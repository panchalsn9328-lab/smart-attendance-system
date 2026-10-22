package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.example.model.AttendanceRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiscoveredBleDevice(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val rssi: Int,
    val isEsp32Candidate: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class BleState {
    object Idle : BleState() // Ready
    object CheckingPermissions : BleState()
    data class BluetoothDisabled(val message: String = "Bluetooth is required for attendance.") : BleState()
    data class PermissionRequired(val permissions: List<String>) : BleState()
    object Scanning : BleState()
    data class DeviceFound(
        val device: BluetoothDevice,
        val deviceName: String,
        val rssi: Int
    ) : BleState()
    data class Connecting(
        val device: BluetoothDevice,
        val deviceName: String
    ) : BleState()
    data class Connected(
        val device: BluetoothDevice,
        val deviceName: String
    ) : BleState()
    object ReadingStatus : BleState() // Checking Session
    data class ActiveSession(
        val deviceId: String,
        val sessionToken: String,
        val status: String,
        val deviceName: String = "BLE_Attendance",
        val rssi: Int = 0
    ) : BleState()
    data class InactiveSession(
        val deviceId: String,
        val status: String = "INACTIVE",
        val deviceName: String = "BLE_Attendance"
    ) : BleState() // No Active Session
    data class AttendanceMarked(val record: AttendanceRecord) : BleState()
    data class AlreadyMarked(val message: String = "Attendance Already Marked") : BleState()
    data class DeviceNotFound(val message: String = "Classroom device not found. Make sure attendance has been started.") : BleState()
    data class Disconnected(val message: String? = null) : BleState()
    data class Error(val message: String) : BleState() // BLE Error
    data class FirebaseError(val message: String) : BleState()
}

class BleManager(private val context: Context) {

    companion object {
        private const val TAG = "BLE_ATTENDANCE"
    }

    private val _state = MutableStateFlow<BleState>(BleState.Idle)
    val state: StateFlow<BleState> = _state.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private var discoveredDevice: BluetoothDevice? = null
    private var lastDiscoveredRssi: Int = 0

    private val connectionCallbacks: BleConnection.ConnectionCallbacks = object : BleConnection.ConnectionCallbacks {
        override fun onConnecting(device: BluetoothDevice) {
            val name = try { device.name } catch (e: SecurityException) { null } ?: "BLE_Attendance"
            _state.value = BleState.Connecting(device, name)
        }

        override fun onConnected(device: BluetoothDevice) {
            val name = try { device.name } catch (e: SecurityException) { null } ?: "BLE_Attendance"
            _state.value = BleState.Connected(device, name)
        }

        override fun onReadingStatus() {
            _state.value = BleState.ReadingStatus
        }

        override fun onActiveSession(deviceId: String, sessionToken: String, status: String) {
            _state.value = BleState.ActiveSession(
                deviceId = deviceId,
                sessionToken = sessionToken,
                status = status,
                deviceName = "BLE_Attendance",
                rssi = lastDiscoveredRssi
            )
        }

        override fun onInactiveSession(deviceId: String, status: String) {
            _state.value = BleState.InactiveSession(
                deviceId = deviceId,
                status = status,
                deviceName = "BLE_Attendance"
            )
        }

        override fun onDisconnected(message: String?) {
            _state.value = BleState.Disconnected(message)
            // If disconnected before acquiring active session, resume scanning
            Log.d(TAG, "[DIAG_BLE_MANAGER] Disconnected event received. Ready for next scan/session.")
        }

        override fun onError(error: String) {
            Log.e(TAG, "[DIAG_BLE_MANAGER] Error reported: $error")
            connection.close()
            scanner.clearDiscoveredDevices()
            if (error.contains("not found", ignoreCase = true) || error.contains("timed out", ignoreCase = true)) {
                _state.value = BleState.DeviceNotFound("Classroom device not found. Make sure attendance has been started.")
            } else {
                _state.value = BleState.Error(error)
            }
        }
    }

    private val connection: BleConnection = BleConnection(context, connectionCallbacks)

    private val scanner: BleScanner = BleScanner(
        context = context,
        onDeviceFound = { device, name, rssi ->
            discoveredDevice = device
            lastDiscoveredRssi = rssi
            _state.value = BleState.DeviceFound(device, name, rssi)
            // Immediately stop scanning and initiate connection to target ESP32
            scanner.stopScan("TARGET_DEVICE_FOUND")
            connectToDevice(device)
        },
        onScanFailed = { _, message ->
            _state.value = BleState.Error(message)
        },
        onScanTimeout = {
            if (_state.value is BleState.Scanning) {
                _state.value = BleState.DeviceNotFound("Classroom device not found. Make sure attendance has been started.")
            }
        },
        onDeviceDiscovered = { device, name, rssi, isEsp32 ->
            val address = device.address ?: return@BleScanner
            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == address }
            val item = DiscoveredBleDevice(
                device = device,
                name = name,
                address = address,
                rssi = rssi,
                isEsp32Candidate = isEsp32
            )
            if (existingIndex >= 0) {
                currentList[existingIndex] = item
            } else {
                if (isEsp32) {
                    currentList.add(0, item)
                } else {
                    currentList.add(item)
                }
            }
            _discoveredDevices.value = currentList
        }
    )

    fun hasPermissions(): Boolean = BluetoothPermissionHelper.hasPermissions(context)

    fun isBluetoothEnabled(): Boolean = BluetoothPermissionHelper.isBluetoothEnabled(context)

    fun isBluetoothSupported(): Boolean = BluetoothPermissionHelper.isBluetoothSupported(context)

    @SuppressLint("MissingPermission")
    fun startScan() {
        _state.value = BleState.CheckingPermissions

        if (!isBluetoothSupported()) {
            _state.value = BleState.Error("Bluetooth is not supported on this device.")
            return
        }

        if (!hasPermissions()) {
            _state.value = BleState.PermissionRequired(BluetoothPermissionHelper.getRequiredPermissions().toList())
            return
        }

        if (!isBluetoothEnabled()) {
            _state.value = BleState.BluetoothDisabled("Bluetooth is required for attendance.")
            return
        }

        _discoveredDevices.value = emptyList()
        _state.value = BleState.Scanning
        scanner.clearDiscoveredDevices()
        scanner.startScan()
    }

    fun stopScan() {
        scanner.stopScan("USER_OR_VIEWMODEL_STOP")
        if (_state.value is BleState.Scanning) {
            _state.value = BleState.Idle
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        scanner.stopScan("CONNECT_TO_DEVICE")
        connection.connect(device)
    }

    fun connectManually(device: BluetoothDevice) {
        val name = try { device.name } catch (e: SecurityException) { null } ?: "BLE_Attendance"
        _state.value = BleState.Connecting(device, name)
        connectToDevice(device)
    }

    fun setAttendanceMarked(record: AttendanceRecord) {
        _state.value = BleState.AttendanceMarked(record)
    }

    fun setAlreadyMarked(message: String = "Attendance Already Marked") {
        _state.value = BleState.AlreadyMarked(message)
    }

    fun setFirebaseError(message: String) {
        _state.value = BleState.FirebaseError(message)
    }

    fun setError(message: String) {
        _state.value = BleState.Error(message)
    }

    fun setDeviceNotFound(message: String = "Classroom device not found. Make sure attendance has been started.") {
        _state.value = BleState.DeviceNotFound(message)
    }

    fun disconnect() {
        scanner.stopScan("DISCONNECT")
        connection.disconnect()
        connection.close()
        _state.value = BleState.Disconnected("Disconnected")
    }

    fun reset() {
        disconnect()
        _state.value = BleState.Idle
    }

    fun cleanup() {
        scanner.stopScan("CLEANUP")
        connection.close()
    }
}

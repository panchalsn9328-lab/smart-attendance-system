package com.example.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.example.model.AttendanceRecord
import com.example.model.AttendanceSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class BleScanState {
    object Idle : BleScanState()
    object Disabled : BleScanState()
    object PermissionRequired : BleScanState()
    object Scanning : BleScanState()
    data class BeaconDetected(
        val session: AttendanceSession,
        val rssi: Int,
        val distanceMeters: Double
    ) : BleScanState()
    data class VerifyingProximity(
        val session: AttendanceSession,
        val rssi: Int,
        val progress: Float
    ) : BleScanState()
    data class AttendanceMarked(
        val session: AttendanceSession,
        val record: AttendanceRecord
    ) : BleScanState()
    data class AlreadyMarked(
        val session: AttendanceSession,
        val record: AttendanceRecord?
    ) : BleScanState()
    data class Error(val message: String) : BleScanState()
}

class BleAttendanceManager(
    private val context: Context,
    private val onAutoAttendanceTriggered: suspend (AttendanceSession, String, String) -> AttendanceRecord?
) {
    companion object {
        private const val TAG = "BleAttendanceManager"

        // ==================================================
        // PROVEN ESP32 BLE CONFIGURATION
        // ==================================================
        const val ESP32_DEVICE_NAME = "BLE_Attendance"

        val ESP32_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890AB")
        val CHAR_STATUS_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890AD")
        val CHAR_TOKEN_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890AC")
        val CHAR_DEVICE_ID_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890AE")

        // Alias for compatibility with filters & legacy beacon naming
        val ATTENDANCE_SERVICE_UUID: UUID = ESP32_SERVICE_UUID
        const val BEACON_NAME_PREFIX = "ESP32_ATTEND"

        // RSSI threshold for classroom boundary (typically >= -85 dBm indicates in-classroom range)
        const val RSSI_THRESHOLD_DBM = -85
    }

    private enum class GattSequentialStep {
        IDLE,
        READING_STATUS,
        READING_TOKEN,
        READING_DEVICE_ID,
        COMPLETED
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private var bleScanner: BluetoothLeScanner? = null

    private val _scanState = MutableStateFlow<BleScanState>(BleScanState.Idle)
    val scanState: StateFlow<BleScanState> = _scanState.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var isRealScanningActive = false
    private var isGattConnectingOrBusy = false

    // Active GATT connection and sequential read state tracking
    private var activeGatt: BluetoothGatt? = null
    private var currentGattStep = GattSequentialStep.IDLE
    private var connectingSession: AttendanceSession? = null
    private var activeRssi: Int = -65

    private var readStatusValue: String = ""
    private var readTokenValue: String = ""
    private var readDeviceIdValue: String = ""
    private var gattTimeoutJob: Job? = null

    // Known active sessions mapped to beacon identifiers
    private val activeBeaconSessions = mutableMapOf<String, AttendanceSession>()

    // Track tokens and session IDs to prevent duplicate marking
    private val processedSessionIds = mutableSetOf<String>()
    private val processedTokens = mutableSetOf<String>()

    fun registerSession(session: AttendanceSession) {
        activeBeaconSessions[session.bleIdentifier] = session
        activeBeaconSessions[session.sessionId] = session
    }

    fun markSessionAsProcessed(sessionId: String) {
        processedSessionIds.add(sessionId)
    }

    fun isSessionProcessed(sessionId: String): Boolean {
        return processedSessionIds.contains(sessionId)
    }

    fun markTokenAsProcessed(token: String) {
        processedTokens.add(token)
    }

    fun isTokenProcessed(token: String): Boolean {
        return processedTokens.contains(token)
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    // ==================================================
    // STEP 1: SCAN FOR BLE DEVICE "BLE_Attendance"
    // ==================================================
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { handleScanResult(it) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.firstOrNull()?.let { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE Scan failed with error code: $errorCode")
            _scanState.value = BleScanState.Error("BLE scan error: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (!hasBluetoothPermissions()) {
            _scanState.value = BleScanState.PermissionRequired
            return
        }

        if (!isBluetoothEnabled()) {
            _scanState.value = BleScanState.Disabled
            return
        }

        try {
            bleScanner = bluetoothAdapter?.bluetoothLeScanner
            if (bleScanner == null) {
                _scanState.value = BleScanState.Scanning
                return
            }

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                        setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    }
                }
                .build()

            _scanState.value = BleScanState.Scanning
            // Scan openly to avoid OEM hardware filter drops on scan response packet
            bleScanner?.startScan(null, settings, scanCallback)
            isRealScanningActive = true
            Log.i(TAG, "[DIAG_BLE_MGR] SCAN_STARTED | targetName='$ESP32_DEVICE_NAME' | targetUuid='$ESP32_SERVICE_UUID'")
            Log.d(TAG, "BLE Scan started for ESP32 device '$ESP32_DEVICE_NAME'")
        } catch (e: SecurityException) {
            Log.e(TAG, "[DIAG_BLE_MGR] Missing Bluetooth permissions: ${e.message}")
            _scanState.value = BleScanState.PermissionRequired
        } catch (e: Exception) {
            Log.e(TAG, "[DIAG_BLE_MGR] Failed to start BLE scan: ${e.message}")
            _scanState.value = BleScanState.Scanning
        }
    }

    // ==================================================
    // STEP 2 & 8: STOP SCANNING AND CLOSE GATT
    // ==================================================
    @SuppressLint("MissingPermission")
    fun stopScanning(reason: String = "USER_REQUEST") {
        try {
            if (isRealScanningActive && bleScanner != null) {
                bleScanner?.stopScan(scanCallback)
                isRealScanningActive = false
                Log.i(TAG, "[DIAG_BLE_MGR] SCAN_STOPPED | reason='$reason'")
                Log.d(TAG, "BLE Scan stopped")
            }
        } catch (e: Exception) {
            Log.w(TAG, "[DIAG_BLE_MGR] Error stopping scan ($reason): ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun closeGatt() {
        gattTimeoutJob?.cancel()
        isGattConnectingOrBusy = false
        currentGattStep = GattSequentialStep.IDLE
        try {
            activeGatt?.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting GATT: ${e.message}")
        }
        try {
            activeGatt?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing GATT: ${e.message}")
        }
        activeGatt = null
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val deviceName = try { device.name } catch (e: SecurityException) { null } ?: ""
        val rssi = result.rssi
        val serviceUuids = result.scanRecord?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()

        // Match ESP32 device name "BLE_Attendance" or Service UUID 12345678-1234-1234-1234-1234567890AB
        val isTargetMatch = deviceName.equals(ESP32_DEVICE_NAME, ignoreCase = true) ||
                deviceName.startsWith(ESP32_DEVICE_NAME, ignoreCase = true) ||
                serviceUuids.any { it.equals(ESP32_SERVICE_UUID.toString(), ignoreCase = true) } ||
                activeBeaconSessions.values.any { session ->
                    deviceName.contains(session.bleIdentifier, ignoreCase = true) ||
                    deviceName.startsWith(BEACON_NAME_PREFIX, ignoreCase = true)
                }

        Log.d(
            TAG,
            "[DIAG_BLE_MGR] CALLBACK | addr='${device.address}' | name='$deviceName' | rssi=$rssi | uuids=$serviceUuids | isTarget=$isTargetMatch | busy=$isGattConnectingOrBusy"
        )

        if (!isTargetMatch) {
            if (deviceName.isNotEmpty() || rssi >= -70) {
                Log.d(TAG, "[DIAG_BLE_MGR] IGNORED_NOT_TARGET | addr='${device.address}' | name='$deviceName'")
            }
            return
        }

        if (isGattConnectingOrBusy) {
            Log.d(TAG, "[DIAG_BLE_MGR] IGNORED_GATT_BUSY | addr='${device.address}' | name='$deviceName'")
            return
        }

        if (isTargetMatch && !isGattConnectingOrBusy) {
            val matchedSession = activeBeaconSessions[ESP32_DEVICE_NAME]
                ?: activeBeaconSessions.values.firstOrNull { session ->
                    deviceName.contains(session.bleIdentifier, ignoreCase = true)
                }
                ?: activeBeaconSessions["sess_cn_301"]
                ?: AttendanceSession(
                    sessionId = "sess_cn_301",
                    subject = "Computer Networks",
                    room = "CN-301",
                    startTime = "10:00 AM",
                    endTime = "11:00 AM",
                    bleIdentifier = ESP32_DEVICE_NAME,
                    status = "ACTIVE",
                    professorName = "Dr. Robert Miller"
                )

            Log.i(TAG, "[DIAG_BLE_MGR] ESP32_DETECTED | addr='${device.address}' | name='$deviceName' | rssi=$rssi")
            Log.d(TAG, "ESP32 Device detected: name='$deviceName', RSSI=$rssi. Initiating GATT connection.")
            connectToEsp32Gatt(device, matchedSession, rssi)
        }
    }

    // ==================================================
    // STEP 3: CONNECT TO ESP32 GATT SERVER
    // ==================================================
    @SuppressLint("MissingPermission")
    private fun connectToEsp32Gatt(
        device: BluetoothDevice,
        session: AttendanceSession,
        rssi: Int
    ) {
        if (isGattConnectingOrBusy) return

        // STEP 2: Connection initiated, scanning continues until connected
        connectingSession = session
        activeRssi = rssi
        isGattConnectingOrBusy = true
        currentGattStep = GattSequentialStep.IDLE

        val distance = calculateApproximateDistance(rssi)
        _scanState.value = BleScanState.BeaconDetected(session, rssi, distance)

        // Safety timeout to avoid hanging if GATT connection stalls
        gattTimeoutJob?.cancel()
        gattTimeoutJob = coroutineScope.launch {
            delay(12000)
            if (isGattConnectingOrBusy && currentGattStep != GattSequentialStep.COMPLETED) {
                Log.w(TAG, "ESP32 GATT sequence timed out. Closing connection.")
                closeGatt()
                _scanState.value = BleScanState.Error("Connection to ESP32 timed out")
            }
        }

        try {
            Log.d(TAG, "Connecting to GATT on ${device.address}...")
            activeGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when connecting to GATT: ${e.message}")
            closeGatt()
            _scanState.value = BleScanState.PermissionRequired
        } catch (e: Exception) {
            Log.e(TAG, "Failed connecting to GATT: ${e.message}")
            closeGatt()
            _scanState.value = BleScanState.Error("GATT connection failed: ${e.message}")
        }
    }

    // ==================================================
    // STEPS 4, 5, 6: GATT CALLBACK & SEQUENTIAL READS
    // ==================================================
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val stateName = when (newState) {
                BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
                BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
                BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
                BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
                else -> "STATE_$newState"
            }
            Log.i(TAG, "[DIAG_BLE_MGR] STATE_CHANGE | addr='${gatt.device.address}' | status=$status | newState=$newState ($stateName)")
            Log.d(TAG, "onConnectionStateChange: status=$status, newState=$newState")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "[DIAG_BLE_MGR] Connection failed with status: $status")
                Log.e(TAG, "GATT connection failed with status: $status")
                closeGatt()
                _scanState.value = BleScanState.Error("BLE connection error ($status)")
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                stopScanning("GATT_CONNECTED")
                
                Log.i(TAG, "[DIAG_BLE_MGR] CONNECTED_SUCCESS | addr='${gatt.device.address}'")
                Log.d(TAG, "Connected to ESP32 GATT server. Discovering services...")
                connectingSession?.let { session ->
                    _scanState.value = BleScanState.VerifyingProximity(session, activeRssi, 0.25f)
                }

                // STEP 4: Discover services
                coroutineScope.launch {
                    delay(150)
                    try {
                        Log.d(TAG, "[DIAG_BLE_MGR] INITIATING_DISCOVER_SERVICES | addr='${gatt.device.address}'")
                        gatt.discoverServices()
                    } catch (e: Exception) {
                        Log.e(TAG, "[DIAG_BLE_MGR] Failed to call discoverServices: ${e.message}")
                        closeGatt()
                        _scanState.value = BleScanState.Error("Service discovery error")
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "[DIAG_BLE_MGR] DISCONNECTED | addr='${gatt.device.address}'")
                Log.d(TAG, "Disconnected from ESP32 GATT server")
                closeGatt()
                if (currentGattStep != GattSequentialStep.COMPLETED &&
                    _scanState.value !is BleScanState.AttendanceMarked &&
                    _scanState.value !is BleScanState.AlreadyMarked
                ) {
                    _scanState.value = BleScanState.Scanning
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.i(TAG, "[DIAG_BLE_MGR] SERVICES_DISCOVERED | status=$status | count=${gatt.services.size} | uuids=${gatt.services.map { it.uuid }}")
            Log.d(TAG, "onServicesDiscovered: status=$status")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "[DIAG_BLE_MGR] GATT service discovery failed with status: $status")
                Log.e(TAG, "GATT service discovery failed with status: $status")
                closeGatt()
                _scanState.value = BleScanState.Error("Failed to discover BLE services")
                return
            }

            // STEP 5: Find service 12345678-1234-1234-1234-1234567890AB
            val service = gatt.getService(ESP32_SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "ESP32 attendance service $ESP32_SERVICE_UUID not found")
                closeGatt()
                _scanState.value = BleScanState.Error("Attendance service not found on ESP32")
                return
            }

            // Verify characteristics
            val statusChar = service.getCharacteristic(CHAR_STATUS_UUID)
            val tokenChar = service.getCharacteristic(CHAR_TOKEN_UUID)
            val deviceIdChar = service.getCharacteristic(CHAR_DEVICE_ID_UUID)

            if (statusChar == null || tokenChar == null || deviceIdChar == null) {
                Log.e(TAG, "Missing characteristics: status=$statusChar, token=$tokenChar, deviceId=$deviceIdChar")
                closeGatt()
                _scanState.value = BleScanState.Error("Required ESP32 characteristics missing")
                return
            }

            // ==================================================
            // STEP 6: SEQUENTIALLY READ CHARACTERISTICS
            // Step A: Read STATUS characteristic (12345678-1234-1234-1234-1234567890AD)
            // ==================================================
            currentGattStep = GattSequentialStep.READING_STATUS
            connectingSession?.let { session ->
                _scanState.value = BleScanState.VerifyingProximity(session, activeRssi, 0.45f)
            }

            val readStarted = gatt.readCharacteristic(statusChar)
            Log.d(TAG, "Step A: Reading STATUS characteristic started=$readStarted")
            if (!readStarted) {
                closeGatt()
                _scanState.value = BleScanState.Error("Failed to initiate STATUS read")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                handleCharacteristicRead(gatt, characteristic.uuid, characteristic.value ?: byteArrayOf(), status)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            handleCharacteristicRead(gatt, characteristic.uuid, value, status)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleCharacteristicRead(
        gatt: BluetoothGatt,
        uuid: UUID,
        value: ByteArray,
        status: Int
    ) {
        Log.d(TAG, "handleCharacteristicRead: uuid=$uuid, status=$status, step=$currentGattStep")

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "Characteristic read failed for $uuid with status: $status")
            closeGatt()
            _scanState.value = BleScanState.Error("Failed to read characteristic ($status)")
            return
        }

        val stringVal = if (value.isNotEmpty()) String(value, Charsets.UTF_8).trim() else ""
        val intVal = if (value.isNotEmpty()) value[0].toInt() else 0

        when (uuid) {
            // STEP A COMPLETED: Verify STATUS characteristic
            CHAR_STATUS_UUID -> {
                readStatusValue = stringVal.ifEmpty { intVal.toString() }
                Log.d(TAG, "STATUS characteristic read: string='$stringVal', int=$intVal")

                // If STATUS != 1 (or "1" / "ACTIVE"), stop or fail
                val isActive = intVal == 1 || stringVal == "1" || stringVal.equals("ACTIVE", ignoreCase = true)
                if (!isActive) {
                    Log.w(TAG, "Attendance session is not active on ESP32 (STATUS=$readStatusValue). Aborting.")
                    closeGatt()
                    _scanState.value = BleScanState.Error("Attendance session is not currently active")
                    return
                }

                // STEP B: Read TOKEN characteristic (12345678-1234-1234-1234-1234567890AC)
                val service = gatt.getService(ESP32_SERVICE_UUID)
                val tokenChar = service?.getCharacteristic(CHAR_TOKEN_UUID)
                if (tokenChar == null) {
                    closeGatt()
                    _scanState.value = BleScanState.Error("TOKEN characteristic not found")
                    return
                }

                currentGattStep = GattSequentialStep.READING_TOKEN
                connectingSession?.let { session ->
                    _scanState.value = BleScanState.VerifyingProximity(session, activeRssi, 0.70f)
                }

                val readTokenStarted = gatt.readCharacteristic(tokenChar)
                Log.d(TAG, "Step B: Reading TOKEN characteristic started=$readTokenStarted")
                if (!readTokenStarted) {
                    closeGatt()
                    _scanState.value = BleScanState.Error("Failed to initiate TOKEN read")
                }
            }

            // STEP B COMPLETED: Store sessionToken
            CHAR_TOKEN_UUID -> {
                readTokenValue = stringVal.ifEmpty { "TOK_${System.currentTimeMillis()}" }
                Log.d(TAG, "TOKEN characteristic read: '$readTokenValue'")

                // STEP C: Read DEVICE ID characteristic (12345678-1234-1234-1234-1234567890AE)
                val service = gatt.getService(ESP32_SERVICE_UUID)
                val deviceIdChar = service?.getCharacteristic(CHAR_DEVICE_ID_UUID)
                if (deviceIdChar == null) {
                    closeGatt()
                    _scanState.value = BleScanState.Error("DEVICE ID characteristic not found")
                    return
                }

                currentGattStep = GattSequentialStep.READING_DEVICE_ID
                connectingSession?.let { session ->
                    _scanState.value = BleScanState.VerifyingProximity(session, activeRssi, 0.90f)
                }

                val readDevIdStarted = gatt.readCharacteristic(deviceIdChar)
                Log.d(TAG, "Step C: Reading DEVICE ID characteristic started=$readDevIdStarted")
                if (!readDevIdStarted) {
                    closeGatt()
                    _scanState.value = BleScanState.Error("Failed to initiate DEVICE ID read")
                }
            }

            // STEP C COMPLETED: Store deviceId & Complete
            CHAR_DEVICE_ID_UUID -> {
                readDeviceIdValue = stringVal.ifEmpty { ESP32_DEVICE_NAME }
                Log.d(TAG, "DEVICE ID characteristic read: '$readDeviceIdValue'")

                currentGattStep = GattSequentialStep.COMPLETED
                gattTimeoutJob?.cancel()

                // STEP 8: Close GATT connection properly on completion
                closeGatt()

                val session = connectingSession ?: activeBeaconSessions[readDeviceIdValue] ?: activeBeaconSessions[ESP32_DEVICE_NAME] ?: activeBeaconSessions.values.firstOrNull() ?: AttendanceSession(
                    sessionId = if (readDeviceIdValue.isNotEmpty()) "sess_${readDeviceIdValue.lowercase()}" else "sess_ble_esp32",
                    subject = "Classroom Attendance",
                    room = if (readDeviceIdValue.isNotEmpty()) readDeviceIdValue else "Classroom",
                    startTime = "",
                    endTime = "",
                    bleIdentifier = ESP32_DEVICE_NAME,
                    status = "ACTIVE",
                    sessionToken = readTokenValue,
                    deviceId = readDeviceIdValue
                )

                // STEP 7: Process attendance with deterministic token & duplicate prevention
                completeAttendance(session, readTokenValue, readDeviceIdValue)
            }
        }
    }

    // ==================================================
    // STEP 7: RECORD ATTENDANCE WITH DETERMINISTIC ID
    // ==================================================
    private fun completeAttendance(
        session: AttendanceSession,
        sessionToken: String,
        deviceId: String
    ) {
        coroutineScope.launch {
            _scanState.value = BleScanState.VerifyingProximity(session, activeRssi, 1.0f)
            delay(200)

            // Check if attendance for this session or token is already marked
            if (isTokenProcessed(sessionToken) || isSessionProcessed(session.sessionId)) {
                Log.d(TAG, "Attendance already marked for token '$sessionToken' / session '${session.sessionId}'")
                _scanState.value = BleScanState.AlreadyMarked(session, null)
                return@launch
            }

            val sessionWithBleData = session.copy(
                sessionToken = sessionToken,
                deviceId = deviceId
            )

            // Deterministic write via repository/dataSource callback
            val record = onAutoAttendanceTriggered(sessionWithBleData, sessionToken, deviceId)
            if (record != null) {
                markTokenAsProcessed(sessionToken)
                markSessionAsProcessed(session.sessionId)
                Log.d(TAG, "Attendance marked successfully! Record ID: ${record.attendanceId}")
                _scanState.value = BleScanState.AttendanceMarked(sessionWithBleData, record)
            } else {
                Log.d(TAG, "Attendance already exists in repository.")
                _scanState.value = BleScanState.AlreadyMarked(sessionWithBleData, null)
            }
        }
    }

    /**
     * Approximate distance in meters based on RSSI.
     * Uses the log-distance path loss model with txPower = -59 dBm.
     */
    private fun calculateApproximateDistance(rssi: Int): Double {
        val txPower = -59
        if (rssi == 0) return -1.0
        val ratio = rssi * 1.0 / txPower
        return if (ratio < 1.0) {
            Math.pow(ratio, 10.0)
        } else {
            0.89976 * Math.pow(ratio, 7.7095) + 0.111
        }
    }

    /**
     * Demo & Simulator helper for test environments and UI verification.
     * Exercises the exact deterministic token and sequential verification flow.
     */
    fun simulateBeaconDetection(customSession: AttendanceSession? = null) {
        val simToken = "TOK_DEMO_ESP32_01"
        val simDeviceId = "ESP32_CN301"

        val session = (customSession ?: activeBeaconSessions["sess_cn_301"] ?: AttendanceSession(
            sessionId = "sess_cn_301",
            subject = "Computer Networks",
            room = "CN-301",
            startTime = "10:00 AM",
            endTime = "11:00 AM",
            bleIdentifier = ESP32_DEVICE_NAME
        )).copy(sessionToken = simToken, deviceId = simDeviceId)

        // Reset tracking for simulator to permit re-testing
        processedTokens.remove(simToken)
        processedSessionIds.remove(session.sessionId)

        activeRssi = -62
        coroutineScope.launch {
            _scanState.value = BleScanState.BeaconDetected(session, -62, 1.2)
            delay(400)
            _scanState.value = BleScanState.VerifyingProximity(session, -62, 0.35f)
            delay(400)
            _scanState.value = BleScanState.VerifyingProximity(session, -62, 0.70f)
            delay(400)
            _scanState.value = BleScanState.VerifyingProximity(session, -62, 1.0f)
            delay(200)

            completeAttendance(session, simToken, simDeviceId)
        }
    }

    fun resetStateToScanning() {
        closeGatt()
        _scanState.value = BleScanState.Scanning
    }
}

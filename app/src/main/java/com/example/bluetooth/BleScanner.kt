package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import java.nio.ByteBuffer
import java.util.UUID

class BleScanner(
    private val context: Context,
    private val onDeviceFound: (device: BluetoothDevice, name: String, rssi: Int) -> Unit,
    private val onScanFailed: (errorCode: Int, message: String) -> Unit,
    private val onScanTimeout: () -> Unit,
    private val onDeviceDiscovered: ((device: BluetoothDevice, name: String, rssi: Int, isEsp32: Boolean) -> Unit)? = null
) {
    companion object {
        private const val TAG = "BLE_ATTENDANCE"
        const val TARGET_DEVICE_NAME = "BLE_Attendance"
        val TARGET_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890AB")
        const val DEFAULT_SCAN_TIMEOUT_MS = 60000L // 60s scan window to discover continuous or late sessions
    }

    private var bleScanner: BluetoothLeScanner? = null
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())
    private val discoveredAddresses = mutableSetOf<String>()
    var autoRestartOnTimeout: Boolean = true

    private val timeoutRunnable = Runnable {
        if (isScanning) {
            Log.i(TAG, "[DIAG_BLE_SCAN] SCAN_TIMEOUT_WINDOW_ELAPSED | autoRestart=$autoRestartOnTimeout")
            if (autoRestartOnTimeout) {
                // Seamlessly restart scan cycle without dropping into stuck Idle state
                try {
                    bleScanner?.stopScan(scanCallback)
                } catch (_: Exception) {}
                isScanning = false
                Log.i(TAG, "[DIAG_BLE_SCAN] Auto-restarting continuous background/foreground BLE scan cycle...")
                handler.postDelayed({
                    startScan(DEFAULT_SCAN_TIMEOUT_MS)
                }, 150)
            } else {
                stopScan("TIMEOUT")
                onScanTimeout()
            }
        }
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { processScanResult(it) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { processScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            handler.removeCallbacks(timeoutRunnable)

            if (errorCode == SCAN_FAILED_ALREADY_STARTED) {
                Log.w(TAG, "[DIAG_BLE_SCAN] SCAN_FAILED_ALREADY_STARTED | Stopping and restarting cleanly...")
                try {
                    bleScanner?.stopScan(this)
                } catch (_: Exception) {}
                isScanning = false
                handler.postDelayed({
                    startScan(DEFAULT_SCAN_TIMEOUT_MS)
                }, 300)
                return
            }

            isScanning = false
            val errorMessage = when (errorCode) {
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Bluetooth registration failed. Please toggle Bluetooth."
                SCAN_FAILED_INTERNAL_ERROR -> "Internal Bluetooth error. Please restart Bluetooth."
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scanning not supported on this device"
                else -> "Scan failed with error code: $errorCode"
            }
            Log.e(TAG, "[DIAG_BLE_SCAN] SCAN_FAILED | errorCode=$errorCode ($errorMessage)")
            onScanFailed(errorCode, errorMessage)
        }
    }

    @SuppressLint("MissingPermission")
    private fun processScanResult(result: ScanResult) {
        val device = result.device
        val deviceAddress = device.address
        if (deviceAddress == null) {
            Log.w(TAG, "[DIAG_BLE_SCAN] EARLY_RETURN_IGNORED | reason=NULL_DEVICE_ADDRESS")
            return
        }
        val scanRecord = result.scanRecord

        // 1. Resolve Advertised Name: check scan record, then cached device name
        val advertisedName = scanRecord?.deviceName?.trim() ?: ""
        val cachedName = try { device.name?.trim() } catch (e: SecurityException) { null } ?: ""
        val rawName = if (advertisedName.isNotEmpty()) advertisedName else cachedName
        val displayName = if (rawName.isNotEmpty()) rawName else "BLE Device (${deviceAddress.takeLast(5)})"

        val rssi = result.rssi
        val serviceUuids = scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()
        val rawBytes = scanRecord?.bytes ?: byteArrayOf()

        // 2. Comprehensive Matcher for target ESP32 hardware:
        // A. Device Name is "BLE_Attendance" or variant containing Attendance/ESP32
        val nameMatches = rawName.equals(TARGET_DEVICE_NAME, ignoreCase = true) ||
                rawName.contains(TARGET_DEVICE_NAME, ignoreCase = true) ||
                rawName.contains("Attendance", ignoreCase = true) ||
                rawName.contains("ESP32", ignoreCase = true)

        // B. Service UUID matches 12345678-1234-1234-1234-1234567890AB
        val uuidMatches = serviceUuids.any {
            it == TARGET_SERVICE_UUID || it.toString().equals(TARGET_SERVICE_UUID.toString(), ignoreCase = true)
        } || (scanRecord?.serviceData?.keys?.any {
            it.uuid == TARGET_SERVICE_UUID || it.uuid.toString().equals(TARGET_SERVICE_UUID.toString(), ignoreCase = true)
        } == true)

        // C. Raw payload bytes contain 128-bit UUID (in little-endian or big-endian order)
        val bytesContainUuid = containsUuidBytes(rawBytes, TARGET_SERVICE_UUID)

        val isTargetEsp32 = nameMatches || uuidMatches || bytesContainUuid

        // Diagnostic log for every scan callback
        Log.d(
            TAG,
            "[DIAG_BLE_SCAN] CALLBACK | addr='$deviceAddress' | name='$rawName' | rssi=$rssi | uuids=$serviceUuids | isTarget=$isTargetEsp32"
        )

        onDeviceDiscovered?.invoke(device, displayName, rssi, isTargetEsp32)

        if (isTargetEsp32) {
            if (!discoveredAddresses.contains(deviceAddress)) {
                discoveredAddresses.add(deviceAddress)
                val finalName = if (rawName.isNotEmpty()) rawName else TARGET_DEVICE_NAME

                val matchReason = when {
                    nameMatches && uuidMatches -> "NAME_AND_UUID"
                    nameMatches -> "NAME_MATCH"
                    uuidMatches -> "UUID_MATCH"
                    else -> "BYTE_PAYLOAD_UUID_MATCH"
                }

                // REQUIRED DEBUG LOGS
                Log.i(TAG, "[DIAG_BLE_SCAN] ESP32_DETECTED | addr='$deviceAddress' | name='$finalName' | rssi=$rssi | matchReason=$matchReason")
                Log.i(TAG, "BLE device found")
                Log.i(TAG, "ESP32 FOUND")
                Log.i(TAG, "Device Name: $finalName")
                Log.i(TAG, "Device Address: $deviceAddress")
                Log.i(TAG, "RSSI: $rssi")

                // Stop scanning immediately upon finding the target ESP32
                stopScan("ESP32_FOUND_CONNECTING")
                onDeviceFound(device, finalName, rssi)
            } else {
                Log.d(TAG, "[DIAG_BLE_SCAN] EARLY_RETURN_IGNORED | addr='$deviceAddress' | reason=ALREADY_IN_DISCOVERED_ADDRESSES")
            }
        } else {
            if (rawName.isNotEmpty() || rssi >= -70) {
                Log.d(TAG, "[DIAG_BLE_SCAN] EARLY_RETURN_IGNORED | addr='$deviceAddress' | name='$rawName' | reason=NOT_TARGET_ESP32")
            }
        }
    }

    private fun containsUuidBytes(bytes: ByteArray, uuid: UUID): Boolean {
        if (bytes.isEmpty()) return false
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        val bigEndian = bb.array()

        val littleEndian = ByteArray(16)
        for (i in 0 until 16) {
            littleEndian[i] = bigEndian[15 - i]
        }

        return containsSubArray(bytes, littleEndian) || containsSubArray(bytes, bigEndian)
    }

    private fun containsSubArray(source: ByteArray, target: ByteArray): Boolean {
        if (target.isEmpty() || source.size < target.size) return false
        for (i in 0..source.size - target.size) {
            var found = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return true
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun startScan(timeoutMs: Long = DEFAULT_SCAN_TIMEOUT_MS): Boolean {
        if (isScanning) {
            Log.d(TAG, "[DIAG_BLE_SCAN] startScan called but scanner is already scanning")
            return true
        }

        if (!BluetoothPermissionHelper.hasPermissions(context)) {
            Log.w(TAG, "[DIAG_BLE_SCAN] Cannot start scan: Bluetooth permissions not granted")
            onScanFailed(-1, "Bluetooth permission required. Please allow Bluetooth permission.")
            return false
        }

        val adapter = BluetoothPermissionHelper.getBluetoothAdapter(context)
        if (adapter == null) {
            Log.w(TAG, "[DIAG_BLE_SCAN] Cannot start scan: Bluetooth is unsupported on this device")
            onScanFailed(-2, "Bluetooth is not supported on this device.")
            return false
        }

        if (!adapter.isEnabled) {
            Log.w(TAG, "[DIAG_BLE_SCAN] Cannot start scan: Bluetooth is disabled")
            onScanFailed(-2, "Bluetooth is required for attendance.")
            return false
        }

        bleScanner = adapter.bluetoothLeScanner
        if (bleScanner == null) {
            Log.e(TAG, "[DIAG_BLE_SCAN] BluetoothLeScanner is null")
            onScanFailed(-3, "BLE scanner unavailable on this device")
            return false
        }

        discoveredAddresses.clear()

        // Low latency aggressive scanning settings for instantaneous discovery
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                }
            }
            .build()

        return try {
            // Using open filter capture to prevent OEM hardware drop of Scan Response device names
            bleScanner?.startScan(null, settings, scanCallback)
            isScanning = true
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed(timeoutRunnable, timeoutMs)
            Log.i(TAG, "[DIAG_BLE_SCAN] SCAN_STARTED | timeoutMs=$timeoutMs")
            Log.i(TAG, "BLE scan started")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "[DIAG_BLE_SCAN] SecurityException starting scan: ${e.message}")
            onScanFailed(-1, "Bluetooth permission required")
            false
        } catch (e: Exception) {
            Log.e(TAG, "[DIAG_BLE_SCAN] Exception starting scan: ${e.message}")
            onScanFailed(-4, e.message ?: "Failed to start scan")
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan(reason: String = "USER_REQUEST") {
        if (!isScanning) return
        handler.removeCallbacks(timeoutRunnable)
        try {
            bleScanner?.stopScan(scanCallback)
            Log.i(TAG, "[DIAG_BLE_SCAN] SCAN_STOPPED | reason='$reason'")
            Log.i(TAG, "BLE scan stopped")
        } catch (e: Exception) {
            Log.w(TAG, "[DIAG_BLE_SCAN] Error stopping scan ($reason): ${e.message}")
        } finally {
            isScanning = false
        }
    }

    fun clearDiscoveredDevices() {
        discoveredAddresses.clear()
        Log.d(TAG, "[DIAG_BLE_SCAN] Cleared discovered device cache")
    }

    fun isScanningNow(): Boolean = isScanning
}

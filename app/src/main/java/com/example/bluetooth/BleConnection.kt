package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.UUID

class BleConnection(
    private val context: Context,
    private val callbacks: ConnectionCallbacks
) {
    interface ConnectionCallbacks {
        fun onConnecting(device: BluetoothDevice)
        fun onConnected(device: BluetoothDevice)
        fun onReadingStatus()
        fun onActiveSession(deviceId: String, sessionToken: String, status: String)
        fun onInactiveSession(deviceId: String, status: String)
        fun onDisconnected(message: String?)
        fun onError(error: String)
    }

    companion object {
        private const val TAG = "BLE_ATTENDANCE"

        val ATTENDANCE_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890AB")
        val CHAR_STATUS_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890AD")
        val CHAR_TOKEN_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890AC")
        val CHAR_DEVICE_ID_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890AE")

        private const val CONNECTION_TIMEOUT_MS = 15000L // 15 seconds timeout
        private const val MAX_RETRIES = 2
    }

    private enum class ReadStep {
        NONE,
        READING_STATUS,
        READING_TOKEN,
        READING_DEVICE_ID,
        DONE
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var currentStep = ReadStep.NONE
    private var currentDevice: BluetoothDevice? = null
    private var connectionRetryCount = 0
    private var readStatusValue: String = ""
    private var readTokenValue: String = ""
    private var readDeviceIdValue: String = ""
    private var cachedTokenChar: BluetoothGattCharacteristic? = null
    private var cachedDeviceIdChar: BluetoothGattCharacteristic? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val timeoutRunnable = Runnable {
        Log.w(TAG, "[DIAG_BLE_GATT] TIMEOUT | step=$currentStep | device=${currentDevice?.address}")
        Log.w(TAG, "GATT connection or read timed out")
        close()
        callbacks.onError("Classroom device not found. Make sure attendance has been started.")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            val stateLabel = when (newState) {
                BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
                BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
                BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
                BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
                else -> "UNKNOWN_$newState"
            }
            Log.i(TAG, "[DIAG_BLE_GATT] STATE_CHANGE | addr='${gatt.device.address}' | status=$status | newState=$newState ($stateLabel)")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "[DIAG_BLE_GATT] CONNECTION_FAILED | addr='${gatt.device.address}' | status=$status | retry=$connectionRetryCount of $MAX_RETRIES")
                Log.e(TAG, "connection failed with status: $status")

                if (connectionRetryCount < MAX_RETRIES && currentDevice != null) {
                    connectionRetryCount++
                    Log.i(TAG, "[DIAG_BLE_GATT] AUTO_RETRY_CONNECT | addr='${currentDevice?.address}' | attempt=$connectionRetryCount in 350ms...")
                    close()
                    mainHandler.postDelayed({
                        currentDevice?.let { dev ->
                            connectInternal(dev, connectionRetryCount)
                        }
                    }, 350)
                    return
                }

                mainHandler.removeCallbacks(timeoutRunnable)
                close()
                mainHandler.post {
                    callbacks.onError("Connection to ESP32 failed (status $status). Ensure ESP32 is advertising.")
                }
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionRetryCount = 0
                Log.i(TAG, "[DIAG_BLE_GATT] CONNECTED_SUCCESS | addr='${gatt.device.address}'")
                Log.i(TAG, "connected")
                mainHandler.post {
                    callbacks.onConnected(gatt.device)
                }
                // Short stabilization delay before discovering services
                mainHandler.postDelayed({
                    try {
                        Log.d(TAG, "[DIAG_BLE_GATT] INITIATING_DISCOVER_SERVICES | addr='${gatt.device.address}'")
                        gatt.discoverServices()
                    } catch (e: Exception) {
                        Log.e(TAG, "[DIAG_BLE_GATT] Error initiating discoverServices: ${e.message}")
                        close()
                        callbacks.onError("Service discovery failed: ${e.message}")
                    }
                }, 200)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "[DIAG_BLE_GATT] DISCONNECTED | addr='${gatt.device.address}' | currentStep=$currentStep")
                Log.i(TAG, "BLE disconnect")
                mainHandler.removeCallbacks(timeoutRunnable)
                close()
                mainHandler.post {
                    callbacks.onDisconnected("Disconnected from classroom device")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)

            Log.i(TAG, "[DIAG_BLE_GATT] SERVICES_DISCOVERED | status=$status | count=${gatt.services.size} | uuids=${gatt.services.map { it.uuid }}")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "[DIAG_BLE_GATT] Services discovery failed with status: $status")
                Log.e(TAG, "services discovery failed with status: $status")
                mainHandler.removeCallbacks(timeoutRunnable)
                close()
                mainHandler.post {
                    callbacks.onError("Failed to discover services ($status)")
                }
                return
            }

            Log.i(TAG, "services discovered")
            val serviceUuids = gatt.services.map { it.uuid }
            Log.i(TAG, "service UUIDs: $serviceUuids")

            // Locate target Attendance Service
            val service = gatt.services.find { s ->
                s.uuid == ATTENDANCE_SERVICE_UUID ||
                s.uuid.toString().equals(ATTENDANCE_SERVICE_UUID.toString(), ignoreCase = true)
            } ?: gatt.getService(ATTENDANCE_SERVICE_UUID)

            if (service == null) {
                Log.e(TAG, "[DIAG_BLE_GATT] Attendance service missing: $ATTENDANCE_SERVICE_UUID")
                Log.e(TAG, "Service missing: $ATTENDANCE_SERVICE_UUID")
                mainHandler.removeCallbacks(timeoutRunnable)
                close()
                mainHandler.post {
                    callbacks.onError("Required service $ATTENDANCE_SERVICE_UUID not found on device")
                }
                return
            }

            // Locate required characteristics
            var statusChar: BluetoothGattCharacteristic? = service.getCharacteristic(CHAR_STATUS_UUID)
            var tokenChar: BluetoothGattCharacteristic? = service.getCharacteristic(CHAR_TOKEN_UUID)
            var deviceIdChar: BluetoothGattCharacteristic? = service.getCharacteristic(CHAR_DEVICE_ID_UUID)

            // Fallback: search across all services if firmware registered characteristics in adjacent handles
            if (statusChar == null || tokenChar == null || deviceIdChar == null) {
                for (s in gatt.services) {
                    if (statusChar == null) {
                        statusChar = s.characteristics.find { it.uuid.toString().equals(CHAR_STATUS_UUID.toString(), ignoreCase = true) }
                    }
                    if (tokenChar == null) {
                        tokenChar = s.characteristics.find { it.uuid.toString().equals(CHAR_TOKEN_UUID.toString(), ignoreCase = true) }
                    }
                    if (deviceIdChar == null) {
                        deviceIdChar = s.characteristics.find { it.uuid.toString().equals(CHAR_DEVICE_ID_UUID.toString(), ignoreCase = true) }
                    }
                }
            }

            // Verify and log characteristics
            if (statusChar == null) {
                Log.e(TAG, "status characteristic missing: $CHAR_STATUS_UUID")
                mainHandler.removeCallbacks(timeoutRunnable)
                close()
                mainHandler.post {
                    callbacks.onError("Status characteristic missing: $CHAR_STATUS_UUID")
                }
                return
            }
            Log.i(TAG, "status characteristic found: ${statusChar.uuid}")

            if (tokenChar == null) {
                Log.e(TAG, "token characteristic missing: $CHAR_TOKEN_UUID")
                mainHandler.removeCallbacks(timeoutRunnable)
                close()
                mainHandler.post {
                    callbacks.onError("Token characteristic missing: $CHAR_TOKEN_UUID")
                }
                return
            }
            Log.i(TAG, "token characteristic found: ${tokenChar.uuid}")

            if (deviceIdChar == null) {
                Log.e(TAG, "device ID characteristic missing: $CHAR_DEVICE_ID_UUID")
                mainHandler.removeCallbacks(timeoutRunnable)
                close()
                mainHandler.post {
                    callbacks.onError("Device ID characteristic missing: $CHAR_DEVICE_ID_UUID")
                }
                return
            }
            Log.i(TAG, "device ID characteristic found: ${deviceIdChar.uuid}")

            cachedTokenChar = tokenChar
            cachedDeviceIdChar = deviceIdChar

            // STEP 7: Read characteristics sequentially!
            // First read: STATUS characteristic
            currentStep = ReadStep.READING_STATUS
            mainHandler.post {
                callbacks.onReadingStatus()
            }

            Log.i(TAG, "reading STATUS")
            val started = gatt.readCharacteristic(statusChar)
            if (!started) {
                Log.e(TAG, "Failed to initiate reading STATUS")
                mainHandler.removeCallbacks(timeoutRunnable)
                close()
                mainHandler.post {
                    callbacks.onError("Failed to read STATUS from ESP32")
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            // Android 12 and below (API < 33) invokes this 3-parameter callback.
            // On Android 13+ (API >= 33), the 4-parameter overload below is invoked.
            // We guard here so handleCharacteristicRead is never invoked twice on API >= 33.
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
            // On API 33+, this callback receives the value byte array directly.
            // We DO NOT call super.onCharacteristicRead because the default framework implementation
            // delegates to the deprecated 3-parameter method, which would cause duplicate reads!
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
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "Failed reading characteristic $uuid with status: $status")
            mainHandler.removeCallbacks(timeoutRunnable)
            close()
            mainHandler.post {
                callbacks.onError("Error reading data from ESP32 ($status)")
            }
            return
        }

        // Clean UTF-8 decoded string with trimmed whitespace
        val stringVal = if (value.isNotEmpty()) String(value, Charsets.UTF_8).trim() else ""
        val intVal = if (value.isNotEmpty()) value[0].toInt() else -1

        when (uuid) {
            CHAR_STATUS_UUID -> {
                val isActive = intVal == 1 || stringVal == "1" || stringVal.equals("ACTIVE", ignoreCase = true)
                val isExplicitlyInactive = intVal == 0 || stringVal == "0" || stringVal.equals("INACTIVE", ignoreCase = true) || stringVal.equals("STOP", ignoreCase = true)
                readStatusValue = if (isActive) "ACTIVE" else if (isExplicitlyInactive) "INACTIVE" else stringVal.ifEmpty { intVal.toString() }
                Log.i(TAG, "[DIAG_BLE_GATT] STATUS characteristic read: string='$stringVal' intVal=$intVal isActive=$isActive finalStatus='$readStatusValue'")

                if (!isActive) {
                    Log.w(TAG, "[DIAG_BLE_GATT] STATUS is not ACTIVE (raw string='$stringVal', intVal=$intVal, final='$readStatusValue'). Stopping session read.")
                    mainHandler.removeCallbacks(timeoutRunnable)
                    close()
                    Log.i(TAG, "BLE disconnect")
                    mainHandler.post {
                        callbacks.onInactiveSession(
                            deviceId = readDeviceIdValue.ifEmpty { "CLASSROOM_ESP32_01" },
                            status = if (readStatusValue.isNotEmpty()) readStatusValue else "INACTIVE"
                        )
                    }
                    return
                }

                // Step 2: Read TOKEN characteristic with a small dispatch delay to ensure GATT readiness
                val tokenChar = cachedTokenChar
                if (tokenChar == null) {
                    Log.e(TAG, "Token characteristic missing: $CHAR_TOKEN_UUID")
                    mainHandler.removeCallbacks(timeoutRunnable)
                    close()
                    mainHandler.post {
                        callbacks.onError("Token characteristic missing")
                    }
                    return
                }

                currentStep = ReadStep.READING_TOKEN
                mainHandler.postDelayed({
                    val currentGatt = bluetoothGatt ?: return@postDelayed
                    Log.i(TAG, "reading TOKEN")
                    val started = currentGatt.readCharacteristic(tokenChar)
                    if (!started) {
                        Log.e(TAG, "Failed to initiate reading TOKEN")
                        mainHandler.removeCallbacks(timeoutRunnable)
                        close()
                        mainHandler.post {
                            callbacks.onError("Failed to read TOKEN from ESP32")
                        }
                    }
                }, 50)
            }

            CHAR_TOKEN_UUID -> {
                readTokenValue = stringVal
                Log.i(TAG, "TOKEN value: $readTokenValue")

                if (readTokenValue.isEmpty() || readTokenValue.equals("NO_SESSION", ignoreCase = true)) {
                    Log.e(TAG, "TOKEN is empty or NO_SESSION! Attendance failed.")
                    mainHandler.removeCallbacks(timeoutRunnable)
                    close()
                    mainHandler.post {
                        callbacks.onError("No active attendance session token found on ESP32.")
                    }
                    return
                }

                // Step 3: Read DEVICE ID characteristic with a small dispatch delay
                val deviceIdChar = cachedDeviceIdChar
                if (deviceIdChar == null) {
                    Log.e(TAG, "Device ID characteristic missing: $CHAR_DEVICE_ID_UUID")
                    mainHandler.removeCallbacks(timeoutRunnable)
                    close()
                    mainHandler.post {
                        callbacks.onError("Device ID characteristic missing")
                    }
                    return
                }

                currentStep = ReadStep.READING_DEVICE_ID
                mainHandler.postDelayed({
                    val currentGatt = bluetoothGatt ?: return@postDelayed
                    Log.i(TAG, "reading DEVICE ID")
                    val started = currentGatt.readCharacteristic(deviceIdChar)
                    if (!started) {
                        Log.e(TAG, "Failed to initiate reading DEVICE ID")
                        mainHandler.removeCallbacks(timeoutRunnable)
                        close()
                        mainHandler.post {
                            callbacks.onError("Failed to read DEVICE ID from ESP32")
                        }
                    }
                }, 50)
            }

            CHAR_DEVICE_ID_UUID -> {
                readDeviceIdValue = stringVal.ifEmpty { "CLASSROOM_ESP32_01" }
                Log.i(TAG, "DEVICE ID value: $readDeviceIdValue")
                Log.i(TAG, "[DIAG_BLE_GATT] SESSION_PAYLOAD_RECEIVED | deviceId='$readDeviceIdValue' | token='$readTokenValue' | status='$readStatusValue'")

                currentStep = ReadStep.DONE
                mainHandler.removeCallbacks(timeoutRunnable)

                // Safe GATT cleanup after all reads are completed successfully
                close()
                Log.i(TAG, "BLE disconnect")

                mainHandler.post {
                    callbacks.onActiveSession(
                        deviceId = readDeviceIdValue,
                        sessionToken = readTokenValue,
                        status = "ACTIVE"
                    )
                }
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        currentDevice = device
        connectionRetryCount = 0
        connectInternal(device, 0)
    }

    @SuppressLint("MissingPermission")
    private fun connectInternal(device: BluetoothDevice, attempt: Int) {
        close()
        currentDevice = device
        connectionRetryCount = attempt
        currentStep = ReadStep.NONE
        readStatusValue = ""
        readTokenValue = ""
        readDeviceIdValue = ""

        callbacks.onConnecting(device)
        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, CONNECTION_TIMEOUT_MS)

        try {
            Log.i(TAG, "[DIAG_BLE_GATT] INITIATING_CONNECT | addr='${device.address}' | attempt=$attempt")
            Log.i(TAG, "connecting to ${device.address}...")
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
        } catch (e: SecurityException) {
            mainHandler.removeCallbacks(timeoutRunnable)
            Log.e(TAG, "[DIAG_BLE_GATT] SecurityException connecting to GATT: ${e.message}")
            callbacks.onError("Bluetooth permission required")
        } catch (e: Exception) {
            mainHandler.removeCallbacks(timeoutRunnable)
            Log.e(TAG, "[DIAG_BLE_GATT] Exception connecting to GATT: ${e.message}")
            callbacks.onError("Connection failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        mainHandler.removeCallbacks(timeoutRunnable)
        try {
            bluetoothGatt?.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting GATT: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun close() {
        mainHandler.removeCallbacks(timeoutRunnable)
        try {
            bluetoothGatt?.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Error in disconnect: ${e.message}")
        }
        try {
            bluetoothGatt?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing GATT: ${e.message}")
        }
        bluetoothGatt = null
        cachedTokenChar = null
        cachedDeviceIdChar = null
        currentStep = ReadStep.NONE
    }
}

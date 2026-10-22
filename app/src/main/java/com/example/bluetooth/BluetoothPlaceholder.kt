package com.example.bluetooth

/**
 * Placeholder previously used before Stage 6.
 * Replaced by real ESP32 BLE integration via [BleScanner], [BleConnection], and [BleManager].
 */
object BluetoothPlaceholder {
    const val IS_MOCK = false
    const val ESP32_DEVICE_NAME = BleScanner.TARGET_DEVICE_NAME
    val ESP32_SERVICE_UUID = BleScanner.TARGET_SERVICE_UUID
}

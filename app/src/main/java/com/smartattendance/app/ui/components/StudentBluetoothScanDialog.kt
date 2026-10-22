package com.smartattendance.app.ui.components

import androidx.compose.runtime.Composable
import com.example.bluetooth.BleState

@Composable
fun StudentBluetoothScanDialog(
    bleState: BleState,
    onDismissRequest: () -> Unit,
    onStartScan: () -> Unit,
    onRecordLocalAttendance: ((deviceId: String, token: String) -> Unit)? = null
) {
    com.example.ui.components.StudentBluetoothScanDialog(
        bleState = bleState,
        onDismissRequest = onDismissRequest,
        onStartScan = onStartScan,
        onRecordLocalAttendance = onRecordLocalAttendance
    )
}

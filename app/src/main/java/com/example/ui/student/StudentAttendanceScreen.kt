package com.example.ui.student

import androidx.compose.runtime.Composable
import com.example.bluetooth.BleState
import com.example.model.Student
import com.example.ui.components.StudentBluetoothScanDialog

@Composable
fun StudentAttendanceScreen(
    student: Student?,
    bleState: BleState,
    onBack: () -> Unit,
    onStartScan: () -> Unit,
    onRecordAttendance: ((String, String) -> Unit)? = null
) {
    StudentBluetoothScanDialog(
        bleState = bleState,
        onDismissRequest = onBack,
        onStartScan = onStartScan,
        onRecordLocalAttendance = onRecordAttendance
    )
}

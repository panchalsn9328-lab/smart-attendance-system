package com.smartattendance.app.ui.student

import androidx.compose.runtime.Composable
import com.example.ble.BleScanState
import com.example.bluetooth.BleState
import com.example.model.AttendanceRecord
import com.example.model.Student

@Composable
fun StudentHomeScreen(
    student: Student?,
    scanState: BleScanState = BleScanState.Idle,
    bleState: BleState = BleState.Idle,
    unreadNotificationCount: Int = 0,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onAttendanceMarkedAuto: (AttendanceRecord) -> Unit = {},
    onStartScanning: () -> Unit = {},
    onRecordLocalAttendance: ((String, String) -> Unit)? = null
) {
    com.example.ui.student.StudentHomeScreen(
        student = student,
        scanState = scanState,
        bleState = bleState,
        unreadNotificationCount = unreadNotificationCount,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToProfile = onNavigateToProfile,
        onAttendanceMarkedAuto = onAttendanceMarkedAuto,
        onStartScanning = onStartScanning,
        onRecordLocalAttendance = onRecordLocalAttendance
    )
}

@Composable
fun StudentAttendanceScreen(
    student: Student?,
    bleState: BleState,
    onBack: () -> Unit,
    onStartScan: () -> Unit,
    onRecordAttendance: ((String, String) -> Unit)? = null
) {
    com.example.ui.student.StudentAttendanceScreen(
        student = student,
        bleState = bleState,
        onBack = onBack,
        onStartScan = onStartScan,
        onRecordAttendance = onRecordAttendance
    )
}

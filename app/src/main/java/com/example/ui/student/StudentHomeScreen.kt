package com.example.ui.student

import androidx.compose.runtime.Composable
import com.example.ble.BleScanState
import com.example.bluetooth.BleState
import com.example.model.AttendanceRecord
import com.example.model.Student
import com.example.ui.screens.HomeScreen

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
    onRecordLocalAttendance: ((String, String) -> Unit)? = null,
    onNavigateToFaceVerification: (String, String) -> Unit = { _, _ -> },
    onNavigateToFaceRegistration: () -> Unit = {}
) {
    HomeScreen(
        student = student,
        scanState = scanState,
        bleState = bleState,
        unreadNotificationCount = unreadNotificationCount,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToProfile = onNavigateToProfile,
        onAttendanceMarkedAuto = onAttendanceMarkedAuto,
        onStartScanning = onStartScanning,
        onRecordLocalAttendance = onRecordLocalAttendance,
        onNavigateToFaceVerification = onNavigateToFaceVerification,
        onNavigateToFaceRegistration = onNavigateToFaceRegistration
    )
}

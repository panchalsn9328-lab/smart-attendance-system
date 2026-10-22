package com.example.model

enum class UserRole {
    STUDENT,
    FACULTY,
    ADMIN
}

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "STUDENT", // "ADMIN", "FACULTY", "STUDENT"
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

data class Student(
    val id: String = "",
    val name: String = "",
    val fullName: String = name,
    val email: String = "",
    val enrollmentNumber: String = "",
    val phone: String = "",
    val course: String = "B.Tech",
    val department: String = "Computer Science",
    val semester: String = "6th Semester",
    val academicYear: String = "2026-2027",
    val classId: String = "",
    val className: String = "",
    val classIds: List<String> = if (classId.isNotBlank()) listOf(classId) else emptyList(),
    val division: String = "A",
    val createdBy: String = "",
    val role: String = "student",
    val isActive: Boolean = true,
    val status: String = "ACTIVE",
    val faceRegistered: Boolean = false,
    val faceEmbedding: List<Float> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
) {
    val uid: String get() = id
    val studentId: String get() = id
    val enrollmentNo: String get() = enrollmentNumber
    val isStudentActive: Boolean get() = isActive && !status.equals("INACTIVE", ignoreCase = true)
    val effectiveClassIds: List<String> get() = if (classIds.isNotEmpty()) classIds else if (classId.isNotBlank()) listOf(classId) else emptyList()
}

data class Faculty(
    val id: String = "",
    val name: String = "",
    val facultyId: String = "",
    val email: String = "",
    val department: String = "Computer Science & Engineering",
    val designation: String = "Assistant Professor",
    val phone: String = "",
    val assignedClasses: List<String> = emptyList(),
    val assignedClassIds: List<String> = assignedClasses,
    val assignedDeviceId: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
) {
    val uid: String get() = id
    val active: Boolean get() = status.equals("ACTIVE", ignoreCase = true)
}

data class ClassItem(
    val classId: String = "",
    val className: String = "",
    val subject: String = "",
    val room: String = "",
    val facultyId: String = "",
    val department: String = "",
    val semester: String = "",
    val status: String = "ACTIVE",
    val enrolledCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Esp32Device(
    val deviceId: String = "CLASSROOM_ESP32_01",
    val deviceName: String = "BLE_Attendance",
    val facultyId: String = "",
    val facultyName: String = "",
    val serviceUuid: String = "12345678-1234-1234-1234-1234567890AB",
    val statusCharUuid: String = "12345678-1234-1234-1234-1234567890AD",
    val tokenCharUuid: String = "12345678-1234-1234-1234-1234567890AC",
    val deviceIdCharUuid: String = "12345678-1234-1234-1234-1234567890AE",
    val room: String = "CN-301",
    val status: String = "ACTIVE",
    val lastSeen: Long = System.currentTimeMillis()
)

data class ClassScheduleItem(
    val id: String,
    val classCode: String,
    val subject: String,
    val room: String,
    val timeSlot: String,
    val status: String = "Upcoming" // "Upcoming", "Active", "Completed"
)

data class StudentAttendanceStatus(
    val studentId: String = "",
    val name: String = "",
    val enrollment: String = "",
    val initials: String = "",
    val isPresent: Boolean = false,
    val markedTime: String = "--:--",
    val status: String = if (isPresent) "PRESENT" else "ABSENT"
) {
    val id: String get() = studentId
    val rollNo: String get() = enrollment
    val enrollmentNo: String get() = enrollment
    val time: String get() = markedTime
    val studentEnrollmentNo: String get() = enrollment
}

data class ClassReportData(
    val classCode: String = "CN-301",
    val className: String = "Computer Networks",
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val totalCount: Int = 0,
    val attendanceRatePercent: Int = 0,
    val dailyAttendance: List<Pair<Int, Int>> = emptyList()
) {
    val dailyRates: List<Int> get() = dailyAttendance.map { if (totalCount > 0) (it.second * 100) / totalCount else 0 }
}

data class AttendanceSession(
    val sessionId: String = "",
    val sessionToken: String = "",
    val deviceId: String = "",
    val deviceName: String = "BLE_Attendance",
    val classId: String = "",
    val className: String = "",
    val subject: String = "",
    val facultyId: String = "",
    val room: String = "",
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, CANCELLED
    val startTime: String = "",
    val endTime: String = "",
    val totalStudents: Int = 30,
    val presentCount: Int = 0,
    val date: String = "",
    val bleIdentifier: String = "BLE_Attendance",
    val professorName: String = "Faculty",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val esp32DeviceId: String get() = deviceId.ifBlank { bleIdentifier }
    val absentCount: Int get() = maxOf(0, totalStudents - presentCount)
    val attendancePercentage: Int get() = if (totalStudents > 0) (presentCount * 100) / totalStudents else 0
}

data class AttendanceRecord(
    val attendanceId: String = "",
    val sessionId: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val enrollmentNo: String = "",
    val classId: String = "",
    val subject: String = "",
    val facultyId: String = "",
    val deviceId: String = "",
    val sessionToken: String = "",
    val bleVerified: Boolean = true,
    val faceVerified: Boolean = true,
    val livenessVerified: Boolean = true,
    val status: String = "Present", // "Present" or "Absent"
    val timestamp: Long = System.currentTimeMillis(),
    val date: String = "",
    val time: String = "",
    val room: String = ""
) {
    val id: String get() = attendanceId.ifBlank { "${studentUid}_${sessionToken}" }
    val studentId: String get() = studentUid
    val studentEnrollment: String get() = enrollmentNo
}

data class AdminStats(
    val totalStudents: Int = 0,
    val totalFaculty: Int = 0,
    val totalClasses: Int = 0,
    val activeSessions: Int = 0,
    val todayAttendance: Int = 0
)

enum class NotificationType {
    ATTENDANCE_MARKED,
    SESSION_DETECTED,
    REMINDER,
    BLUETOOTH_DISABLED
}

data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val type: NotificationType,
    val isRead: Boolean = false
)

data class AppSettings(
    val bluetoothEnabled: Boolean = true,
    val backgroundScanningEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val themeMode: String = "System Default" // "System Default", "Light", "Dark"
)

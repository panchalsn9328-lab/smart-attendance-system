package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AttendanceRepository
import com.example.model.AttendanceSession
import com.example.model.Student
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.example")
                    .setApiKey("AIzaTestKeyForSmartAttendanceUnitTesting001")
                    .setProjectId("smart-attendance12-1353f")
                    .build()
            )
        }
    }

    private fun createTestRepo(): AttendanceRepository {
        val repo = AttendanceRepository()
        repo.setStudent(
            Student(
                id = "std_22bcs123",
                name = "Sujal Panchal",
                email = "sujal@college.edu",
                enrollmentNumber = "22BCS123",
                department = "B.Tech Computer Science"
            )
        )
        return repo
    }

    @Test
    fun testAttendanceRecordingAndDuplicatePrevention() {
        val repo = createTestRepo()

        val initialCount = repo.attendanceHistory.value.size

        val testSession = AttendanceSession(
            sessionId = "test_sess_001",
            subject = "Computer Networks",
            room = "CN-301",
            startTime = "10:00 AM",
            endTime = "11:00 AM",
            bleIdentifier = "ESP32_ATTEND_CN301"
        )

        // Verify session not yet marked
        assertFalse(repo.hasMarkedAttendance("test_sess_001"))

        // Record attendance
        val record = repo.recordAttendance(
            session = testSession,
            bleVerified = true,
            faceVerified = true,
            livenessVerified = true
        )
        assertNotNull(record)
        assertEquals("Computer Networks", record?.subject)
        assertEquals("CN-301", record?.room)
        assertTrue(record?.status.equals("PRESENT", ignoreCase = true))

        // Verify marked count incremented by 1
        assertEquals(initialCount + 1, repo.attendanceHistory.value.size)
        assertTrue(repo.hasMarkedAttendance("test_sess_001"))

        // Duplicate attendance attempt for same session should not duplicate entry
        val duplicateRecord = repo.recordAttendance(
            session = testSession,
            bleVerified = true,
            faceVerified = true,
            livenessVerified = true
        )
        assertNotNull(duplicateRecord)
        assertEquals(initialCount + 1, repo.attendanceHistory.value.size)
    }

    @Test
    fun testDeterministicAttendanceIdAndDuplicatePreventionWithSessionToken() {
        val repo = createTestRepo()
        val student = repo.currentStudent.value
        assertNotNull(student)
        val studentId = student!!.id

        val session = AttendanceSession(
            sessionId = "sess_esp32_cn301",
            subject = "Computer Networks",
            room = "CN-301",
            startTime = "10:00 AM",
            endTime = "11:00 AM",
            bleIdentifier = "BLE_Attendance"
        )
        val token = "SESSION_TOKEN_ABC123"
        val deviceId = "ESP32_ATTEND_CN301"

        // Record attendance with token
        val record = repo.recordAttendance(
            session = session,
            sessionToken = token,
            deviceId = deviceId,
            bleVerified = true,
            faceVerified = true,
            livenessVerified = true
        )
        assertNotNull(record)
        // Deterministic ID format: studentId + "_" + sessionToken
        assertEquals("${studentId}_$token", record?.attendanceId)
        assertEquals(token, record?.sessionToken)
        assertEquals(deviceId, record?.deviceId)
        assertTrue(record?.status.equals("PRESENT", ignoreCase = true))

        // Duplicate check should return true
        assertTrue(repo.hasMarkedAttendance("sess_esp32_cn301", token))

        // Re-recording with the same token must NOT create duplicate
        val historyCountBefore = repo.attendanceHistory.value.size
        val duplicateRecord = repo.recordAttendance(
            session = session,
            sessionToken = token,
            deviceId = deviceId,
            bleVerified = true,
            faceVerified = true,
            livenessVerified = true
        )
        assertEquals(record?.attendanceId, duplicateRecord?.attendanceId)
        assertEquals(historyCountBefore, repo.attendanceHistory.value.size)
    }

    @Test
    fun testStudentLoginAndProfileUpdate() {
        val repo = createTestRepo()
        assertEquals("22BCS123", repo.currentStudent.value?.enrollmentNumber)

        repo.updateProfile("Sujal Panchal", "22BCS123", "Computer Science")
        assertEquals("Sujal Panchal", repo.currentStudent.value?.name)
        assertEquals("Computer Science", repo.currentStudent.value?.department)
    }

    @Test
    fun testSettingsUpdate() {
        val repo = AttendanceRepository()
        assertTrue(repo.settings.value.bluetoothEnabled)
        repo.updateSettings(bluetooth = false, themeMode = "Dark")
        assertFalse(repo.settings.value.bluetoothEnabled)
        assertEquals("Dark", repo.settings.value.themeMode)
    }
}

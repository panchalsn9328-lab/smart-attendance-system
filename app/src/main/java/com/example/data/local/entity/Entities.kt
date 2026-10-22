package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.AttendanceRecord
import com.example.model.AttendanceSession
import com.example.model.ClassItem
import com.example.model.Faculty
import com.example.model.Student

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val name: String,
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
    val division: String = "A",
    val createdBy: String = "",
    val role: String = "student",
    val isActive: Boolean = true,
    val status: String = "ACTIVE",
    val faceRegistered: Boolean = false,
    val faceEmbeddingJson: String = "", // JSON serialized List<Float>
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toStudent(): Student {
        val embeddings = if (faceEmbeddingJson.isNotBlank()) {
            try {
                faceEmbeddingJson.removeSurrounding("[", "]")
                    .split(",")
                    .mapNotNull { it.trim().toFloatOrNull() }
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        return Student(
            id = id,
            name = name.ifBlank { fullName },
            fullName = fullName.ifBlank { name },
            email = email,
            enrollmentNumber = enrollmentNumber,
            phone = phone,
            course = course,
            department = department,
            semester = semester,
            academicYear = academicYear,
            classId = classId,
            className = className,
            division = division,
            createdBy = createdBy,
            role = role,
            isActive = isActive,
            status = status,
            faceRegistered = faceRegistered || embeddings.isNotEmpty(),
            faceEmbedding = embeddings,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromStudent(student: Student): StudentEntity {
            val embJson = if (student.faceEmbedding.isNotEmpty()) {
                student.faceEmbedding.joinToString(prefix = "[", postfix = "]")
            } else ""

            return StudentEntity(
                id = student.id.ifBlank { "std_${student.enrollmentNumber.trim().lowercase().replace(" ", "_")}" },
                name = student.name.ifBlank { student.fullName },
                fullName = student.fullName.ifBlank { student.name },
                email = student.email.trim(),
                enrollmentNumber = student.enrollmentNumber.trim(),
                phone = student.phone.trim(),
                course = student.course,
                department = student.department,
                semester = student.semester,
                academicYear = student.academicYear,
                classId = student.classId,
                className = student.className,
                division = student.division,
                createdBy = student.createdBy,
                role = "student",
                isActive = student.isActive,
                status = if (student.isActive) "ACTIVE" else "INACTIVE",
                faceRegistered = student.faceRegistered || student.faceEmbedding.isNotEmpty(),
                faceEmbeddingJson = embJson,
                createdAt = student.createdAt,
                updatedAt = student.updatedAt
            )
        }
    }
}

@Entity(tableName = "faculty")
data class FacultyEntity(
    @PrimaryKey val id: String,
    val facultyId: String,
    val name: String,
    val email: String,
    val department: String = "Computer Science & Engineering",
    val designation: String = "Assistant Professor",
    val phone: String = "",
    val assignedClassesJson: String = "", // Comma-separated or JSON list
    val assignedDeviceId: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toFaculty(): Faculty {
        val classes = if (assignedClassesJson.isNotBlank()) {
            assignedClassesJson.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
        } else emptyList()

        return Faculty(
            id = id,
            facultyId = facultyId,
            name = name,
            email = email,
            department = department,
            designation = designation,
            phone = phone,
            assignedClasses = classes,
            assignedDeviceId = assignedDeviceId,
            status = status,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromFaculty(fac: Faculty): FacultyEntity {
            val classesJson = fac.assignedClasses.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
            return FacultyEntity(
                id = fac.id.ifBlank { "fac_${fac.facultyId.trim().lowercase()}" },
                facultyId = fac.facultyId.trim().uppercase(),
                name = fac.name.trim(),
                email = fac.email.trim(),
                department = fac.department,
                designation = fac.designation,
                phone = fac.phone,
                assignedClassesJson = classesJson,
                assignedDeviceId = fac.assignedDeviceId,
                status = fac.status,
                createdAt = fac.createdAt
            )
        }
    }
}

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey val classId: String,
    val className: String,
    val subject: String,
    val room: String,
    val facultyId: String,
    val department: String = "Computer Science",
    val semester: String = "6th Semester",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toClassItem(): ClassItem = ClassItem(
        classId = classId,
        className = className,
        subject = subject,
        room = room,
        facultyId = facultyId,
        department = department,
        semester = semester,
        status = status,
        createdAt = createdAt
    )

    companion object {
        fun fromClassItem(item: ClassItem): ClassEntity = ClassEntity(
            classId = item.classId.trim(),
            className = item.className.trim(),
            subject = item.subject.trim(),
            room = item.room.trim(),
            facultyId = item.facultyId.trim(),
            department = item.department,
            semester = item.semester,
            status = item.status,
            createdAt = item.createdAt
        )
    }
}

@Entity(tableName = "attendance_sessions")
data class AttendanceSessionEntity(
    @PrimaryKey val sessionId: String,
    val sessionToken: String = "",
    val deviceId: String = "",
    val deviceName: String = "BLE_Attendance",
    val classId: String = "",
    val className: String = "",
    val subject: String = "",
    val facultyId: String = "",
    val room: String = "",
    val status: String = "ACTIVE",
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
    fun toAttendanceSession(): AttendanceSession = AttendanceSession(
        sessionId = sessionId,
        sessionToken = sessionToken,
        deviceId = deviceId,
        deviceName = deviceName,
        classId = classId,
        className = className,
        subject = subject,
        facultyId = facultyId,
        room = room,
        status = status,
        startTime = startTime,
        endTime = endTime,
        totalStudents = totalStudents,
        presentCount = presentCount,
        date = date,
        bleIdentifier = bleIdentifier,
        professorName = professorName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromSession(s: AttendanceSession): AttendanceSessionEntity = AttendanceSessionEntity(
            sessionId = s.sessionId,
            sessionToken = s.sessionToken,
            deviceId = s.deviceId,
            deviceName = s.deviceName,
            classId = s.classId,
            className = s.className,
            subject = s.subject,
            facultyId = s.facultyId,
            room = s.room,
            status = s.status,
            startTime = s.startTime,
            endTime = s.endTime,
            totalStudents = s.totalStudents,
            presentCount = s.presentCount,
            date = s.date,
            bleIdentifier = s.bleIdentifier,
            professorName = s.professorName,
            createdAt = s.createdAt,
            updatedAt = s.updatedAt
        )
        fun fromAttendanceSession(s: AttendanceSession): AttendanceSessionEntity = fromSession(s)
    }
}

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
    @PrimaryKey val attendanceId: String,
    val sessionId: String,
    val studentUid: String,
    val studentName: String,
    val enrollmentNo: String,
    val classId: String,
    val subject: String,
    val facultyId: String,
    val deviceId: String,
    val sessionToken: String,
    val bleVerified: Boolean = true,
    val faceVerified: Boolean = true,
    val livenessVerified: Boolean = true,
    val status: String = "Present",
    val timestamp: Long = System.currentTimeMillis(),
    val date: String = "",
    val time: String = "",
    val room: String = ""
) {
    fun toAttendanceRecord(): AttendanceRecord = AttendanceRecord(
        attendanceId = attendanceId,
        sessionId = sessionId,
        studentUid = studentUid,
        studentName = studentName,
        enrollmentNo = enrollmentNo,
        classId = classId,
        subject = subject,
        facultyId = facultyId,
        deviceId = deviceId,
        sessionToken = sessionToken,
        bleVerified = bleVerified,
        faceVerified = faceVerified,
        livenessVerified = livenessVerified,
        status = status,
        timestamp = timestamp,
        date = date,
        time = time,
        room = room
    )

    companion object {
        fun fromRecord(r: AttendanceRecord): AttendanceRecordEntity = AttendanceRecordEntity(
            attendanceId = r.attendanceId.ifBlank { "${r.studentUid}_${r.sessionToken}" },
            sessionId = r.sessionId,
            studentUid = r.studentUid,
            studentName = r.studentName,
            enrollmentNo = r.enrollmentNo,
            classId = r.classId,
            subject = r.subject,
            facultyId = r.facultyId,
            deviceId = r.deviceId,
            sessionToken = r.sessionToken,
            bleVerified = r.bleVerified,
            faceVerified = r.faceVerified,
            livenessVerified = r.livenessVerified,
            status = r.status,
            timestamp = r.timestamp,
            date = r.date,
            time = r.time,
            room = r.room
        )
    }
}

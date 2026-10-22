package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.entity.ClassEntity
import com.example.data.local.entity.FacultyEntity
import com.example.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudentsFlow(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY name ASC")
    suspend fun getAllStudents(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: String): StudentEntity?

    @Query("SELECT * FROM students WHERE LOWER(enrollmentNumber) = LOWER(:identifier) OR LOWER(email) = LOWER(:identifier) OR id = :identifier LIMIT 1")
    suspend fun getStudentByEnrollmentOrEmail(identifier: String): StudentEntity?

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY name ASC")
    suspend fun getStudentsByClass(classId: String): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("UPDATE students SET faceRegistered = 1, faceEmbeddingJson = :embeddingJson, updatedAt = :updatedAt WHERE id = :studentId")
    suspend fun updateFaceEmbedding(studentId: String, embeddingJson: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudentById(id: String)
}

@Dao
interface FacultyDao {
    @Query("SELECT * FROM faculty ORDER BY name ASC")
    fun getAllFacultyFlow(): Flow<List<FacultyEntity>>

    @Query("SELECT * FROM faculty ORDER BY name ASC")
    suspend fun getAllFaculty(): List<FacultyEntity>

    @Query("SELECT * FROM faculty WHERE id = :id LIMIT 1")
    suspend fun getFacultyById(id: String): FacultyEntity?

    @Query("SELECT * FROM faculty WHERE UPPER(facultyId) = UPPER(:identifier) OR LOWER(email) = LOWER(:identifier) OR id = :identifier LIMIT 1")
    suspend fun getFacultyByIdOrEmail(identifier: String): FacultyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaculty(faculty: FacultyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacultyList(facultyList: List<FacultyEntity>)

    @Query("DELETE FROM faculty WHERE id = :id")
    suspend fun deleteFacultyById(id: String)
}

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes ORDER BY classId ASC")
    fun getAllClassesFlow(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes ORDER BY classId ASC")
    suspend fun getAllClasses(): List<ClassEntity>

    @Query("SELECT * FROM classes WHERE classId = :classId LIMIT 1")
    suspend fun getClassById(classId: String): ClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<ClassEntity>)
}

@Dao
interface AttendanceSessionDao {
    @Query("SELECT * FROM attendance_sessions ORDER BY createdAt DESC")
    fun getAllSessionsFlow(): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions ORDER BY createdAt DESC")
    suspend fun getAllSessions(): List<AttendanceSessionEntity>

    @Query("SELECT * FROM attendance_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_sessions WHERE facultyId = :facultyId ORDER BY createdAt DESC")
    suspend fun getSessionsForFaculty(facultyId: String): List<AttendanceSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AttendanceSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<AttendanceSessionEntity>)
}

@Dao
interface AttendanceRecordDao {
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE studentUid = :studentUid ORDER BY timestamp DESC")
    suspend fun getRecordsForStudent(studentUid: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE studentUid = :studentUid ORDER BY timestamp DESC")
    fun getRecordsForStudentFlow(studentUid: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getRecordsForSession(sessionId: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getRecordsForSessionFlow(sessionId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE attendanceId = :attendanceId LIMIT 1")
    suspend fun getRecordById(attendanceId: String): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE studentUid = :studentUid AND sessionToken = :sessionToken LIMIT 1")
    suspend fun getRecordByStudentAndToken(studentUid: String, sessionToken: String): AttendanceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)
}

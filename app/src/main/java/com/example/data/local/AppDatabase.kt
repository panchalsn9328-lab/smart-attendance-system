package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AttendanceRecordDao
import com.example.data.local.dao.AttendanceSessionDao
import com.example.data.local.dao.ClassDao
import com.example.data.local.dao.FacultyDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.entity.ClassEntity
import com.example.data.local.entity.FacultyEntity
import com.example.data.local.entity.StudentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        StudentEntity::class,
        FacultyEntity::class,
        ClassEntity::class,
        AttendanceSessionEntity::class,
        AttendanceRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun facultyDao(): FacultyDao
    abstract fun classDao(): ClassDao
    abstract fun attendanceSessionDao(): AttendanceSessionDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao

    companion object {
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_attendance_v3.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                INSTANCE = instance
                // Pre-populate default seed data if database is new
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        seedDefaultDataIfEmpty(instance)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Database seed skipped: ${e.message}")
                    }
                }
                instance
            }
        }

        private suspend fun seedDefaultDataIfEmpty(db: AppDatabase) {
            try {
                // 1. Seed Default Classes if empty
                val existingClasses = db.classDao().getAllClasses()
                if (existingClasses.isEmpty()) {
                    db.classDao().insertClasses(
                        listOf(
                            ClassEntity(
                                classId = "CN-301",
                                className = "Computer Networks",
                                subject = "Computer Networks",
                                room = "Room 301 - Lab B",
                                facultyId = "FAC001",
                                department = "Computer Science",
                                semester = "6th Semester"
                            ),
                            ClassEntity(
                                classId = "CS-401",
                                className = "Operating Systems",
                                subject = "Operating Systems",
                                room = "Room 402 - Hall A",
                                facultyId = "FAC001",
                                department = "Computer Science",
                                semester = "6th Semester"
                            ),
                            ClassEntity(
                                classId = "CS-501",
                                className = "Database Management Systems",
                                subject = "DBMS & Cloud SQL",
                                room = "Room 205 - Lab C",
                                facultyId = "FAC002",
                                department = "Information Technology",
                                semester = "6th Semester"
                            ),
                            ClassEntity(
                                classId = "CS-601",
                                className = "Artificial Intelligence",
                                subject = "AI & Machine Learning",
                                room = "Seminar Hall 1",
                                facultyId = "FAC001",
                                department = "Computer Science",
                                semester = "6th Semester"
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error seeding database: ${e.message}")
            }
        }
    }
}

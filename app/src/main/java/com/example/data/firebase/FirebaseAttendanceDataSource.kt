package com.example.data.firebase

import android.util.Log
import com.example.data.awaitTask
import com.example.model.AdminStats
import com.example.model.AttendanceRecord
import com.example.model.AttendanceSession
import com.example.model.ClassItem
import com.example.model.Esp32Device
import com.example.model.Faculty
import com.example.model.Student
import com.example.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Firebase Firestore Single Source of Truth with Full Runtime Instrumentation
 */
class FirebaseAttendanceDataSource {

    companion object {
        private const val TAG = "FirestoreDataSource"

        const val COLL_USERS = "users"
        const val COLL_STUDENTS = "students"
        const val COLL_FACULTY = "faculty"
        const val COLL_CLASSES = "classes"
        const val COLL_DEVICES = "devices"
        const val COLL_SESSIONS = "attendance_sessions"
        const val COLL_ATTENDANCE = "attendance_records"
        const val COLL_FACE_PROFILES = "face_profiles"

        fun logFirebaseDebugStatus(userRole: String = "UNKNOWN") {
            try {
                val app = FirebaseApp.getInstance()
                val projectId = app.options.projectId
                val appName = app.name
                val currentUser = FirebaseAuth.getInstance().currentUser
                val firestoreApp = FirebaseFirestore.getInstance().app.name

                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] =========================================")
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] projectId=$projectId")
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] appName=$appName")
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] firestoreAppName=$firestoreApp")
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] authUid=${currentUser?.uid ?: "null"}")
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] authEmail=${currentUser?.email ?: "null"}")
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] isAnonymous=${currentUser?.isAnonymous ?: false}")
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] userRole=$userRole")
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] currentUserIsNull=${currentUser == null}")
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] firestoreInitialized=true")
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] =========================================")
            } catch (e: Exception) {
                Log.e("FIREBASE_DEBUG", "[FIREBASE_DEBUG] firestoreInitialized=false error=${e.message}")
            }
        }
    }

    private val firestore: FirebaseFirestore by lazy {
        try {
            val app = FirebaseApp.getInstance()
            val resId = app.applicationContext.resources.getIdentifier(
                "firestore_database_id", "string", app.applicationContext.packageName
            )
            val dbId = if (resId != 0) app.applicationContext.getString(resId) else null
            if (!dbId.isNullOrBlank()) {
                Log.i(TAG, "Connecting to custom Firestore database: $dbId")
                FirebaseFirestore.getInstance(dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Connecting to default Firestore instance: ${e.message}")
            FirebaseFirestore.getInstance()
        }
    }

    // ==========================================
    // RUNTIME DIRECT FIRESTORE DIAGNOSTICS
    // ==========================================
    suspend fun runDirectFirestoreTest(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        try {
            val studentsSnap = firestore.collection(COLL_STUDENTS).get().awaitTask()
            val facultySnap = firestore.collection(COLL_FACULTY).get().awaitTask()
            val classesSnap = firestore.collection(COLL_CLASSES).get().awaitTask()

            val sCount = studentsSnap.size()
            val fCount = facultySnap.size()
            val cCount = classesSnap.size()
            val sIds = studentsSnap.documents.map { it.id }

            Log.i("FIRESTORE_DIRECT_TEST", "[FIRESTORE_DIRECT_TEST] =========================================")
            Log.i("FIRESTORE_DIRECT_TEST", "[FIRESTORE_DIRECT_TEST] students count = $sCount (fromCache=${studentsSnap.metadata.isFromCache})")
            Log.i("FIRESTORE_DIRECT_TEST", "[FIRESTORE_DIRECT_TEST] faculty count = $fCount (fromCache=${facultySnap.metadata.isFromCache})")
            Log.i("FIRESTORE_DIRECT_TEST", "[FIRESTORE_DIRECT_TEST] classes count = $cCount (fromCache=${classesSnap.metadata.isFromCache})")
            Log.i("FIRESTORE_DIRECT_TEST", "[FIRESTORE_DIRECT_TEST] student document IDs = $sIds")
            Log.i("FIRESTORE_DIRECT_TEST", "[FIRESTORE_DIRECT_TEST] =========================================")
            Triple(sCount, fCount, cCount)
        } catch (e: Exception) {
            Log.e("FIRESTORE_DIRECT_TEST", "[FIRESTORE_DIRECT_TEST] FAILED: ${e.message}")
            Triple(0, 0, 0)
        }
    }

    suspend fun createTestStudentInFirestore(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val authUser = FirebaseAuth.getInstance().currentUser
        val ts = System.currentTimeMillis()
        val testUid = authUser?.uid ?: "DEBUG_STUDENT_$ts"
        val testName = "DEBUG_STUDENT_$ts"
        val testEmail = "debug_$ts@college.edu"
        val testEnroll = "DBG$ts"

        val student = Student(
            id = testUid,
            name = testName,
            fullName = testName,
            email = testEmail,
            enrollmentNumber = testEnroll,
            course = "B.Tech",
            department = "Computer Science",
            semester = "6th Semester",
            classId = "CN-301",
            className = "Computer Networks",
            classIds = listOf("CN-301"),
            division = "A",
            role = "student",
            isActive = true,
            status = "ACTIVE",
            createdAt = ts,
            updatedAt = ts
        )

        Log.i("STUDENT_DEBUG", "[STUDENT_DEBUG] Attempting test student write: authUid=$testUid name=$testName")
        val writeOk = saveStudentProfile(student)
        Log.i("STUDENT_DEBUG", "[STUDENT_DEBUG] authUid=$testUid studentWrite=${if (writeOk) "SUCCESS" else "FAIL"}")

        // Immediate direct read test
        val directDoc = try {
            firestore.collection(COLL_STUDENTS).document(testUid).get().awaitTask()
        } catch (e: Exception) {
            null
        }
        val readStatus = if (directDoc != null && directDoc.exists()) "FOUND" else "NOT_FOUND"
        Log.i("STUDENT_DEBUG", "[STUDENT_DEBUG] authUid=$testUid directRead=$readStatus (fromCache=${directDoc?.metadata?.isFromCache})")

        Pair(writeOk, "Write: ${if (writeOk) "SUCCESS" else "FAIL"}, Read: $readStatus, ID: $testUid")
    }

    // ==========================================
    // INITIALIZATION (Ensure Device & Class Config)
    // ==========================================
    suspend fun initializeCollectionsIfEmpty() = withContext(Dispatchers.IO) {
        try {
            logFirebaseDebugStatus("INITIALIZING")
            val deviceDoc = firestore.collection(COLL_DEVICES).document("CLASSROOM_ESP32_01").get().awaitTask()
            if (!deviceDoc.exists()) {
                val defaultDevice = Esp32Device(
                    deviceId = "CLASSROOM_ESP32_01",
                    deviceName = "BLE_Attendance",
                    room = "CN-301",
                    status = "ACTIVE",
                    lastSeen = System.currentTimeMillis()
                )
                firestore.collection(COLL_DEVICES).document(defaultDevice.deviceId)
                    .set(defaultDevice, SetOptions.merge())
            }

            val cn301 = firestore.collection(COLL_CLASSES).document("CN-301").get().awaitTask()
            if (!cn301.exists()) {
                val defaultClass = ClassItem(
                    classId = "CN-301",
                    className = "Computer Networks",
                    subject = "Computer Networks",
                    room = "Room 301 - Lab B",
                    department = "Computer Science",
                    semester = "6th Semester",
                    status = "ACTIVE"
                )
                firestore.collection(COLL_CLASSES).document("CN-301").set(defaultClass, SetOptions.merge())
            }

            val cs401 = firestore.collection(COLL_CLASSES).document("CS-401").get().awaitTask()
            if (!cs401.exists()) {
                val defaultClass = ClassItem(
                    classId = "CS-401",
                    className = "Operating Systems",
                    subject = "Operating Systems",
                    room = "Room 402 - Hall A",
                    department = "Computer Science",
                    semester = "6th Semester",
                    status = "ACTIVE"
                )
                firestore.collection(COLL_CLASSES).document("CS-401").set(defaultClass, SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore initialization note: ${e.message}")
        }
    }

    // ==========================================
    // USER PROFILES & ROLES
    // ==========================================
    suspend fun getUserProfile(uid: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection(COLL_USERS).document(uid).get().awaitTask()
            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=users documentId=$uid exists=${doc.exists()} fromCache=${doc.metadata.isFromCache}")
            if (doc.exists()) {
                UserProfile(
                    uid = doc.getString("uid") ?: uid,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    role = doc.getString("role") ?: "STUDENT",
                    status = doc.getString("status") ?: "ACTIVE",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            Log.e("FIRESTORE_READ", "[FIRESTORE_READ] collection=users documentId=$uid error=${e.message}")
            null
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf<String, Any>(
                "uid" to profile.uid,
                "name" to profile.name,
                "email" to profile.email.trim().lowercase(),
                "role" to profile.role.lowercase(),
                "status" to profile.status,
                "createdAt" to profile.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLL_USERS).document(profile.uid).set(data, SetOptions.merge()).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=users documentId=${profile.uid} operation=SET result=SUCCESS")
            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=users documentId=${profile.uid} operation=SET result=FAIL error=${e.message}")
            false
        }
    }

    // ==========================================
    // STUDENTS (Canonical: students/{uid})
    // ==========================================
    suspend fun getStudentProfile(studentUid: String): Student? = withContext(Dispatchers.IO) {
        val clean = studentUid.trim()
        if (clean.isBlank()) return@withContext null
        try {
            var student: Student? = null
            val doc = firestore.collection(COLL_STUDENTS).document(clean).get().awaitTask()
            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=students documentId=$clean exists=${doc.exists()} fromCache=${doc.metadata.isFromCache}")
            if (doc.exists()) {
                student = mapDocumentToStudent(doc)
            }
            if (student == null) {
                student = getStudentByEnrollmentOrEmail(clean)
            }
            if (student != null) {
                // If face profile exists in canonical face_profiles collection, ensure faceRegistered = true
                if (!student.faceRegistered) {
                    val statusClean = getFaceProfileStatus(clean)
                    val statusId = if (student.id != clean) getFaceProfileStatus(student.id) else statusClean
                    if (statusClean is FaceProfileStatus.Valid || statusId is FaceProfileStatus.Valid) {
                        student = student.copy(faceRegistered = true)
                    }
                }
                return@withContext student
            }
            null
        } catch (e: Exception) {
            Log.e("FIRESTORE_READ", "[FIRESTORE_READ] collection=students documentId=$clean error=${e.message}")
            null
        }
    }

    suspend fun saveStudentProfile(student: Student): Boolean = withContext(Dispatchers.IO) {
        val uid = student.id.trim()
        if (uid.isBlank()) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=students documentId=EMPTY operation=SET result=FAIL error=BlankUID")
            return@withContext false
        }

        try {
            val classIdList = student.effectiveClassIds
            val data = hashMapOf(
                "uid" to uid,
                "studentId" to uid,
                "id" to uid,
                "name" to student.name.ifBlank { student.fullName },
                "fullName" to student.fullName.ifBlank { student.name },
                "enrollmentNumber" to student.enrollmentNumber.trim().uppercase(),
                "enrollmentNo" to student.enrollmentNumber.trim().uppercase(),
                "email" to student.email.trim().lowercase(),
                "phone" to student.phone.trim(),
                "course" to student.course,
                "department" to student.department,
                "semester" to student.semester,
                "academicYear" to student.academicYear,
                "classId" to student.classId,
                "className" to student.className,
                "classIds" to classIdList,
                "division" to student.division,
                "createdBy" to student.createdBy,
                "role" to "student",
                "isActive" to student.isActive,
                "status" to (if (student.isActive) "ACTIVE" else "INACTIVE"),
                "faceRegistered" to student.faceRegistered,
                "createdAt" to student.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )

            // 1. Primary write: students/{uid}
            firestore.collection(COLL_STUDENTS).document(uid).set(data, SetOptions.merge()).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=students documentId=$uid operation=SET result=SUCCESS")

            // 2. Identity mirror: users/{uid}
            val userProfile = hashMapOf<String, Any>(
                "uid" to uid,
                "name" to student.name.ifBlank { student.fullName },
                "email" to student.email.trim().lowercase(),
                "phone" to student.phone.trim(),
                "role" to "student",
                "isActive" to student.isActive,
                "status" to (if (student.isActive) "ACTIVE" else "INACTIVE"),
                "faceRegistered" to student.faceRegistered,
                "createdAt" to student.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLL_USERS).document(uid).set(userProfile, SetOptions.merge()).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=users documentId=$uid operation=SET result=SUCCESS")

            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=students documentId=$uid operation=SET result=FAIL error=${e.message}")
            false
        }
    }

    fun listenToAllStudents(onUpdate: (List<Student>) -> Unit): ListenerRegistration {
        Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=students event=CREATED")
        return firestore.collection(COLL_STUDENTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=students event=ERROR code=${error.code} message=${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val docCount = snapshot.size()
                val fromCache = snapshot.metadata.isFromCache
                val hasPendingWrites = snapshot.metadata.hasPendingWrites()
                val docIds = snapshot.documents.map { it.id }

                Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=students event=SNAPSHOT documents=$docCount fromCache=$fromCache hasPendingWrites=$hasPendingWrites")
                Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=students documentsReceived=$docCount ids=$docIds fromCache=$fromCache hasPendingWrites=$hasPendingWrites")
                Log.i("STUDENT_FLOW", "[STUDENT_FLOW] FIRESTORE count=$docCount")

                val parsedStudents = snapshot.documents.mapNotNull { doc ->
                    mapDocumentToStudent(doc)
                }

                val canonicalList = deduplicateAndNormalizeStudents(parsedStudents)

                Log.i("STUDENT_FLOW", "[STUDENT_FLOW] DATASOURCE count=${canonicalList.size}")
                onUpdate(canonicalList)
            }
    }

    private fun deduplicateAndNormalizeStudents(rawList: List<Student>): List<Student> {
        val studentMap = mutableMapOf<String, Student>()
        val enrollToUid = mutableMapOf<String, String>()
        val emailToUid = mutableMapOf<String, String>()

        for (s in rawList) {
            val uid = s.id.trim()
            val enroll = s.enrollmentNumber.trim().uppercase()
            val email = s.email.trim().lowercase()

            val existing = studentMap[uid]
            if (existing != null) {
                studentMap[uid] = mergeStudentData(existing, s)
                continue
            }

            val uidByEnroll = if (enroll.isNotBlank()) enrollToUid[enroll] else null
            val uidByEmail = if (email.isNotBlank()) emailToUid[email] else null
            val targetUid = uidByEnroll ?: uidByEmail

            if (targetUid != null && targetUid != uid) {
                val canonical = studentMap[targetUid]
                if (canonical != null) {
                    studentMap[targetUid] = mergeStudentData(canonical, s)
                    continue
                }
            }

            val effectiveId = if (uid.isNotBlank()) uid else if (enroll.isNotBlank()) "std_${enroll.lowercase()}" else "std_${s.createdAt}"
            val studentWithId = s.copy(id = effectiveId)
            studentMap[effectiveId] = studentWithId

            if (enroll.isNotBlank()) enrollToUid[enroll] = effectiveId
            if (email.isNotBlank()) emailToUid[email] = effectiveId
        }

        return studentMap.values.toList().sortedBy { it.name.ifBlank { it.fullName } }
    }

    private fun mergeStudentData(existing: Student, incoming: Student): Student {
        val mergedClasses = (existing.effectiveClassIds + incoming.effectiveClassIds).distinct()
        return existing.copy(
            name = existing.name.ifBlank { incoming.name },
            fullName = existing.fullName.ifBlank { incoming.fullName },
            email = existing.email.ifBlank { incoming.email },
            enrollmentNumber = existing.enrollmentNumber.ifBlank { incoming.enrollmentNumber },
            phone = existing.phone.ifBlank { incoming.phone },
            course = existing.course.ifBlank { incoming.course },
            department = existing.department.ifBlank { incoming.department },
            semester = existing.semester.ifBlank { incoming.semester },
            academicYear = existing.academicYear.ifBlank { incoming.academicYear },
            classId = existing.classId.ifBlank { incoming.classId },
            className = existing.className.ifBlank { incoming.className },
            classIds = mergedClasses,
            division = existing.division.ifBlank { incoming.division },
            faceRegistered = existing.faceRegistered || incoming.faceRegistered,
            isActive = existing.isActive && incoming.isActive,
            status = if (existing.isActive && incoming.isActive) "ACTIVE" else "INACTIVE",
            createdAt = minOf(existing.createdAt, incoming.createdAt),
            updatedAt = maxOf(existing.updatedAt, incoming.updatedAt)
        )
    }

    suspend fun getAllStudents(): List<Student> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLL_STUDENTS).get().awaitTask()
            val list = snapshot.documents.mapNotNull { mapDocumentToStudent(it) }
            val result = deduplicateAndNormalizeStudents(list)
            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=students (getAllStudents) count=${result.size} fromCache=${snapshot.metadata.isFromCache}")
            result
        } catch (e: Exception) {
            Log.e("FIRESTORE_READ", "[FIRESTORE_READ] getAllStudents error: ${e.message}")
            emptyList()
        }
    }

    suspend fun deleteOrDeactivateStudent(studentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = hashMapOf<String, Any>(
                "isActive" to false,
                "status" to "INACTIVE",
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLL_STUDENTS).document(studentId).update(updates).awaitTask()
            try {
                firestore.collection(COLL_USERS).document(studentId).update(updates).awaitTask()
            } catch (e: Exception) { /* users sync */ }
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=students documentId=$studentId operation=UPDATE (Deactivate) result=SUCCESS")
            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=students documentId=$studentId operation=UPDATE (Deactivate) result=FAIL error=${e.message}")
            false
        }
    }

    suspend fun getStudentsForClass(classId: String): List<Student> = withContext(Dispatchers.IO) {
        try {
            val all = getAllStudents()
            val cleanClass = classId.trim().replace(" ", "").replace("-", "").uppercase()
            if (cleanClass.isBlank() || cleanClass == "ALL") return@withContext all

            all.filter { s ->
                val sClass = s.classId.trim().replace(" ", "").replace("-", "").uppercase()
                val listMatch = s.effectiveClassIds.any {
                    it.trim().replace(" ", "").replace("-", "").uppercase() == cleanClass
                }
                sClass == cleanClass || listMatch
            }
        } catch (e: Exception) {
            Log.w(TAG, "getStudentsForClass error: ${e.message}")
            emptyList()
        }
    }

    suspend fun getStudentByEnrollmentOrEmail(identifier: String): Student? = withContext(Dispatchers.IO) {
        val clean = identifier.trim()
        if (clean.isBlank()) return@withContext null

        try {
            // 1. Direct document ID in students collection
            val directDoc = firestore.collection(COLL_STUDENTS).document(clean).get().awaitTask()
            if (directDoc.exists()) {
                val student = mapDocumentToStudent(directDoc)
                if (student != null) return@withContext checkAndEnrichFaceStatus(student)
            }

            // 2. Query by enrollmentNumber (uppercase and verbatim)
            val byEnroll = firestore.collection(COLL_STUDENTS)
                .whereEqualTo("enrollmentNumber", clean.uppercase())
                .limit(1)
                .get().awaitTask()
            if (!byEnroll.isEmpty) {
                val student = mapDocumentToStudent(byEnroll.documents[0])
                if (student != null) return@withContext checkAndEnrichFaceStatus(student)
            }

            val byEnrollRaw = firestore.collection(COLL_STUDENTS)
                .whereEqualTo("enrollmentNumber", clean)
                .limit(1)
                .get().awaitTask()
            if (!byEnrollRaw.isEmpty) {
                val student = mapDocumentToStudent(byEnrollRaw.documents[0])
                if (student != null) return@withContext checkAndEnrichFaceStatus(student)
            }

            // 3. Query by enrollmentNo or enrollment
            val byEnrollNo = firestore.collection(COLL_STUDENTS)
                .whereEqualTo("enrollmentNo", clean.uppercase())
                .limit(1)
                .get().awaitTask()
            if (!byEnrollNo.isEmpty) {
                val student = mapDocumentToStudent(byEnrollNo.documents[0])
                if (student != null) return@withContext checkAndEnrichFaceStatus(student)
            }

            // 4. Query by email
            val byEmail = firestore.collection(COLL_STUDENTS)
                .whereEqualTo("email", clean.lowercase())
                .limit(1)
                .get().awaitTask()
            if (!byEmail.isEmpty) {
                val student = mapDocumentToStudent(byEmail.documents[0])
                if (student != null) return@withContext checkAndEnrichFaceStatus(student)
            }

            // 5. Query in users collection
            val userDirect = firestore.collection(COLL_USERS).document(clean).get().awaitTask()
            if (userDirect.exists()) {
                val student = mapDocumentToStudent(userDirect)
                if (student != null) return@withContext checkAndEnrichFaceStatus(student)
            }

            val userByEnroll = firestore.collection(COLL_USERS)
                .whereEqualTo("enrollmentNumber", clean.uppercase())
                .limit(1)
                .get().awaitTask()
            if (!userByEnroll.isEmpty) {
                val student = mapDocumentToStudent(userByEnroll.documents[0])
                if (student != null) return@withContext checkAndEnrichFaceStatus(student)
            }

            val userByEmail = firestore.collection(COLL_USERS)
                .whereEqualTo("email", clean.lowercase())
                .limit(1)
                .get().awaitTask()
            if (!userByEmail.isEmpty) {
                val student = mapDocumentToStudent(userByEmail.documents[0])
                if (student != null) return@withContext checkAndEnrichFaceStatus(student)
            }

            // 6. Check all students in cache / collection
            val all = getAllStudents()
            val matched = all.firstOrNull {
                it.enrollmentNumber.equals(clean, ignoreCase = true) ||
                it.email.equals(clean, ignoreCase = true) ||
                it.id.equals(clean, ignoreCase = true) ||
                it.phone.equals(clean, ignoreCase = true)
            }
            if (matched != null) {
                return@withContext checkAndEnrichFaceStatus(matched)
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "getStudentByEnrollmentOrEmail error: ${e.message}")
            null
        }
    }

    private suspend fun checkAndEnrichFaceStatus(student: Student): Student {
        if (student.faceRegistered) return student
        val status = getFaceProfileStatus(student.id)
        return if (status is FaceProfileStatus.Valid) {
            student.copy(faceRegistered = true)
        } else {
            student
        }
    }

    fun mapDocumentToStudent(doc: com.google.firebase.firestore.DocumentSnapshot): Student? {
        val docId = doc.id
        val role = (doc.getString("role") ?: doc.getString("userRole") ?: "").trim().lowercase()
        if (role.contains("admin") || role.contains("faculty") || role.contains("professor")) {
            return null
        }

        val sId = doc.getString("uid")
            ?: doc.getString("studentId")
            ?: doc.getString("id")
            ?: docId

        val fName = doc.getString("fullName")
            ?: doc.getString("name")
            ?: doc.getString("studentName")
            ?: doc.getString("displayName")
            ?: ""

        val enroll = doc.getString("enrollmentNumber")
            ?: doc.getString("enrollmentNo")
            ?: doc.getString("enrollment")
            ?: doc.getString("rollNo")
            ?: ""

        val mail = doc.getString("email")
            ?: doc.getString("studentEmail")
            ?: ""

        val ph = doc.getString("phone")
            ?: doc.getString("phoneNumber")
            ?: ""

        val cId = doc.getString("classId")
            ?: doc.getString("classCode")
            ?: "CN-301"

        val cName = doc.getString("className")
            ?: doc.getString("subject")
            ?: "Computer Networks"

        val rawClassIds = (doc.get("classIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val effectiveClassIds = if (rawClassIds.isNotEmpty()) rawClassIds else listOf(cId)

        val div = doc.getString("division") ?: "A"
        val cr = doc.getString("course") ?: "B.Tech"
        val dept = doc.getString("department") ?: "Computer Science"
        val sem = doc.getString("semester") ?: "6th Semester"
        val yr = doc.getString("academicYear") ?: "2026-2027"
        val cBy = doc.getString("createdBy") ?: ""
        val stat = doc.getString("status") ?: "ACTIVE"
        val active = doc.getBoolean("isActive") ?: !stat.equals("INACTIVE", ignoreCase = true)

        val faceReg = doc.getBoolean("faceRegistered") ?: false
        val crAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        val upAt = doc.getLong("updatedAt") ?: crAt

        if (fName.isBlank() && enroll.isBlank() && mail.isBlank()) {
            return null
        }

        val effName = fName.ifBlank {
            if (enroll.isNotBlank()) "Student $enroll"
            else if (mail.isNotBlank()) mail.substringBefore("@")
            else "Student"
        }

        return Student(
            id = sId,
            name = effName,
            fullName = effName,
            email = mail,
            enrollmentNumber = enroll.trim().uppercase(),
            phone = ph,
            course = cr,
            department = dept,
            semester = sem,
            academicYear = yr,
            classId = cId,
            className = cName,
            classIds = effectiveClassIds,
            division = div,
            createdBy = cBy,
            role = "student",
            isActive = active,
            status = if (active) "ACTIVE" else "INACTIVE",
            faceRegistered = faceReg,
            createdAt = crAt,
            updatedAt = upAt
        )
    }

    // ==========================================
    // FACE DATA ISOLATION (face_profiles/{uid})
    // ==========================================
    suspend fun saveFaceEmbedding(studentId: String, embedding: List<Float>): Boolean = withContext(Dispatchers.IO) {
        val uid = studentId.trim()
        if (uid.isBlank() || embedding.isEmpty()) return@withContext false
        if (embedding.size != com.example.face.FaceEmbeddingProcessor.EMBEDDING_DIM) {
            Log.e("FIRESTORE_WRITE", "saveFaceEmbedding rejected: embedding dimension ${embedding.size} != expected ${com.example.face.FaceEmbeddingProcessor.EMBEDDING_DIM}")
            return@withContext false
        }

        try {
            val faceDoc = hashMapOf<String, Any>(
                "uid" to uid,
                "embedding" to embedding,
                "embeddingDim" to embedding.size,
                "modelVersion" to com.example.face.FaceEmbeddingProcessor.MODEL_VERSION,
                "registeredAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLL_FACE_PROFILES).document(uid).set(faceDoc, SetOptions.merge()).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=face_profiles documentId=$uid operation=SET (192-d MobileFaceNet) result=SUCCESS")

            val statusUpdate = mapOf<String, Any>(
                "faceRegistered" to true,
                "updatedAt" to System.currentTimeMillis()
            )
            // Use set with merge so it never throws NOT_FOUND
            firestore.collection(COLL_STUDENTS).document(uid).set(statusUpdate, SetOptions.merge()).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=students documentId=$uid operation=SET (faceRegistered=true) result=SUCCESS")

            try {
                // If student document exists by enrollment number, also ensure it is marked
                val byEnroll = firestore.collection(COLL_STUDENTS)
                    .whereEqualTo("enrollmentNumber", uid.uppercase())
                    .limit(1)
                    .get().awaitTask()
                for (doc in byEnroll.documents) {
                    doc.reference.set(statusUpdate, SetOptions.merge())
                }
            } catch (_: Exception) {}

            try {
                firestore.collection(COLL_USERS).document(uid).set(statusUpdate, SetOptions.merge()).awaitTask()
            } catch (e: Exception) { /* users update */ }

            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=face_profiles documentId=$uid operation=SET result=FAIL error=${e.message}")
            false
        }
    }

    suspend fun getFaceEmbedding(studentId: String): List<Float> = withContext(Dispatchers.IO) {
        val status = getFaceProfileStatus(studentId)
        when (status) {
            is FaceProfileStatus.Valid -> status.embedding
            else -> emptyList()
        }
    }

    suspend fun getFaceProfileStatus(studentId: String): FaceProfileStatus = withContext(Dispatchers.IO) {
        val uid = studentId.trim()
        if (uid.isBlank()) return@withContext FaceProfileStatus.Missing

        try {
            var doc = firestore.collection(COLL_FACE_PROFILES).document(uid).get().awaitTask()
            if (!doc.exists()) {
                // Check if stored under uppercase enrollment
                val byEnroll = firestore.collection(COLL_FACE_PROFILES)
                    .whereEqualTo("enrollmentNumber", uid.uppercase())
                    .limit(1)
                    .get().awaitTask()
                if (!byEnroll.isEmpty) {
                    doc = byEnroll.documents[0]
                }
            }

            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=face_profiles documentId=$uid exists=${doc.exists()} fromCache=${doc.metadata.isFromCache}")
            if (doc.exists()) {
                val raw = doc.get("embedding")
                if (raw is List<*>) {
                    val emb = raw.mapNotNull { (it as? Number)?.toFloat() }
                    if (emb.size == com.example.face.FaceEmbeddingProcessor.EMBEDDING_DIM) {
                        return@withContext FaceProfileStatus.Valid(emb)
                    } else if (emb.size == com.example.face.FaceEmbeddingProcessor.LEGACY_HANDCRAFTED_DIM) {
                        Log.w("FACE_DEBUG", "Face profile for $uid contains legacy 128-d handcrafted embedding. Migration required.")
                        return@withContext FaceProfileStatus.LegacyNeedsMigration(emb.size)
                    } else {
                        Log.w("FACE_DEBUG", "Face profile for $uid has unsupported dimension ${emb.size}. Migration required.")
                        return@withContext FaceProfileStatus.LegacyNeedsMigration(emb.size)
                    }
                }
            }
            FaceProfileStatus.Missing
        } catch (e: Exception) {
            Log.w(TAG, "getFaceProfileStatus error: ${e.message}")
            FaceProfileStatus.Error(e.message ?: "Failed to read face profile")
        }
    }

    // ==========================================
    // FACULTY (Canonical: faculty/{uid})
    // ==========================================
    suspend fun getFacultyProfile(facultyUid: String): Faculty? = withContext(Dispatchers.IO) {
        val clean = facultyUid.trim()
        if (clean.isBlank()) return@withContext null
        try {
            val doc = firestore.collection(COLL_FACULTY).document(clean).get().awaitTask()
            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=faculty documentId=$clean exists=${doc.exists()} fromCache=${doc.metadata.isFromCache}")
            if (doc.exists()) {
                return@withContext mapDocumentToFaculty(doc)
            }
            getFacultyByFacultyId(clean)
        } catch (e: Exception) {
            Log.e("FIRESTORE_READ", "[FIRESTORE_READ] collection=faculty documentId=$clean error=${e.message}")
            null
        }
    }

    suspend fun getFacultyByFacultyId(facultyIdOrEmail: String): Faculty? = withContext(Dispatchers.IO) {
        val clean = facultyIdOrEmail.trim()
        if (clean.isBlank()) return@withContext null

        try {
            val byFid = firestore.collection(COLL_FACULTY)
                .whereEqualTo("facultyId", clean.uppercase())
                .limit(1)
                .get().awaitTask()
            if (!byFid.isEmpty) {
                val fac = mapDocumentToFaculty(byFid.documents[0])
                if (fac != null) return@withContext fac
            }

            val byEmail = firestore.collection(COLL_FACULTY)
                .whereEqualTo("email", clean.lowercase())
                .limit(1)
                .get().awaitTask()
            if (!byEmail.isEmpty) {
                val fac = mapDocumentToFaculty(byEmail.documents[0])
                if (fac != null) return@withContext fac
            }

            val all = getAllFaculty()
            all.firstOrNull {
                it.facultyId.equals(clean, ignoreCase = true) ||
                it.email.equals(clean, ignoreCase = true) ||
                it.id.equals(clean, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "getFacultyByFacultyId error: ${e.message}")
            null
        }
    }

    suspend fun saveFacultyProfile(faculty: Faculty): Boolean = withContext(Dispatchers.IO) {
        val uid = faculty.id.trim()
        if (uid.isBlank()) return@withContext false

        try {
            val assignedList = if (faculty.assignedClasses.isNotEmpty()) faculty.assignedClasses else faculty.assignedClassIds
            val data = hashMapOf(
                "uid" to uid,
                "id" to uid,
                "facultyId" to faculty.facultyId.trim().uppercase(),
                "name" to faculty.name.trim(),
                "email" to faculty.email.trim().lowercase(),
                "department" to faculty.department.trim(),
                "designation" to faculty.designation.trim(),
                "phone" to faculty.phone.trim(),
                "assignedClasses" to assignedList,
                "assignedClassIds" to assignedList,
                "assignedDeviceId" to faculty.assignedDeviceId.trim(),
                "role" to "faculty",
                "status" to faculty.status,
                "active" to faculty.active,
                "createdAt" to faculty.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection(COLL_FACULTY).document(uid).set(data, SetOptions.merge()).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=faculty documentId=$uid operation=SET result=SUCCESS")

            val userProfile = hashMapOf<String, Any>(
                "uid" to uid,
                "name" to faculty.name.trim(),
                "email" to faculty.email.trim().lowercase(),
                "role" to "faculty",
                "status" to faculty.status,
                "createdAt" to faculty.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLL_USERS).document(uid).set(userProfile, SetOptions.merge()).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=users documentId=$uid operation=SET result=SUCCESS")

            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=faculty documentId=$uid operation=SET result=FAIL error=${e.message}")
            false
        }
    }

    fun listenToAllFaculty(onUpdate: (List<Faculty>) -> Unit): ListenerRegistration {
        Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=faculty event=CREATED")
        return firestore.collection(COLL_FACULTY)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=faculty event=ERROR code=${error.code} message=${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc -> mapDocumentToFaculty(doc) }
                    .distinctBy { it.facultyId.ifBlank { it.id } }
                    .sortedBy { it.name }

                Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=faculty event=SNAPSHOT documents=${list.size} fromCache=${snapshot.metadata.isFromCache}")
                Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=faculty documentsReceived=${list.size} ids=${list.map { it.id }} fromCache=${snapshot.metadata.isFromCache}")
                onUpdate(list)
            }
    }

    suspend fun getAllFaculty(): List<Faculty> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLL_FACULTY).get().awaitTask()
            val list = snapshot.documents.mapNotNull { mapDocumentToFaculty(it) }
                .distinctBy { it.facultyId.ifBlank { it.id } }
                .sortedBy { it.name }
            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=faculty (getAllFaculty) count=${list.size} fromCache=${snapshot.metadata.isFromCache}")
            list
        } catch (e: Exception) {
            Log.e("FIRESTORE_READ", "[FIRESTORE_READ] getAllFaculty error: ${e.message}")
            emptyList()
        }
    }

    private fun mapDocumentToFaculty(doc: com.google.firebase.firestore.DocumentSnapshot): Faculty? {
        val fid = doc.getString("facultyId") ?: ""
        val uid = doc.getString("uid") ?: doc.id
        val name = doc.getString("name") ?: ""
        val email = doc.getString("email") ?: ""
        if (name.isBlank() && email.isBlank() && fid.isBlank()) return null

        val rawClasses = (doc.get("assignedClasses") as? List<*>)?.mapNotNull { it?.toString() }
            ?: (doc.get("assignedClassIds") as? List<*>)?.mapNotNull { it?.toString() }
            ?: emptyList()

        return Faculty(
            id = uid,
            name = name,
            facultyId = fid,
            email = email,
            department = doc.getString("department") ?: "Computer Science & Engineering",
            designation = doc.getString("designation") ?: "Assistant Professor",
            phone = doc.getString("phone") ?: "",
            assignedClasses = rawClasses,
            assignedClassIds = rawClasses,
            assignedDeviceId = doc.getString("assignedDeviceId") ?: "",
            status = doc.getString("status") ?: "ACTIVE",
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    suspend fun assignDeviceToFaculty(facultyUid: String, deviceId: String, deviceName: String = "ESP32_Attendance"): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanDevId = deviceId.trim().uppercase()
            val fac = getFacultyProfile(facultyUid)
            if (fac != null) {
                saveFacultyProfile(fac.copy(assignedDeviceId = cleanDevId))
            }
            val devDoc = hashMapOf<String, Any>(
                "deviceId" to cleanDevId,
                "name" to deviceName,
                "deviceName" to deviceName,
                "type" to "esp32",
                "assignedFacultyId" to facultyUid,
                "assignedFacultyName" to (fac?.name ?: ""),
                "status" to "ACTIVE",
                "lastSeen" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLL_DEVICES).document(cleanDevId).set(devDoc, SetOptions.merge()).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=devices documentId=$cleanDevId operation=SET result=SUCCESS")
            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=devices documentId=$deviceId operation=SET result=FAIL error=${e.message}")
            false
        }
    }

    // ==========================================
    // CLASSES (Canonical: classes/{classId})
    // ==========================================
    fun listenToAllClasses(onUpdate: (List<ClassItem>) -> Unit): ListenerRegistration {
        Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=classes event=CREATED")
        return firestore.collection(COLL_CLASSES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=classes event=ERROR code=${error.code} message=${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    ClassItem(
                        classId = doc.getString("classId") ?: doc.id,
                        className = doc.getString("className") ?: doc.getString("name") ?: "",
                        subject = doc.getString("subject") ?: doc.getString("className") ?: "",
                        room = doc.getString("room") ?: "",
                        facultyId = doc.getString("facultyId") ?: "",
                        department = doc.getString("department") ?: "",
                        semester = doc.getString("semester") ?: "",
                        status = doc.getString("status") ?: "ACTIVE",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }.sortedBy { it.classId }

                Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=classes event=SNAPSHOT documents=${list.size} fromCache=${snapshot.metadata.isFromCache}")
                onUpdate(list)
            }
    }

    suspend fun getAllClasses(): List<ClassItem> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLL_CLASSES).get().awaitTask()
            val list = snapshot.documents.mapNotNull { doc ->
                ClassItem(
                    classId = doc.getString("classId") ?: doc.id,
                    className = doc.getString("className") ?: doc.getString("name") ?: "",
                    subject = doc.getString("subject") ?: doc.getString("className") ?: "",
                    room = doc.getString("room") ?: "",
                    facultyId = doc.getString("facultyId") ?: "",
                    department = doc.getString("department") ?: "",
                    semester = doc.getString("semester") ?: "",
                    status = doc.getString("status") ?: "ACTIVE",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }.sortedBy { it.classId }
            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=classes (getAllClasses) count=${list.size} fromCache=${snapshot.metadata.isFromCache}")
            list
        } catch (e: Exception) {
            Log.e("FIRESTORE_READ", "[FIRESTORE_READ] getAllClasses error: ${e.message}")
            emptyList()
        }
    }

    suspend fun getClassesForFaculty(facultyIdOrUid: String): List<ClassItem> = withContext(Dispatchers.IO) {
        try {
            val all = getAllClasses()
            if (facultyIdOrUid.isBlank()) all else all.filter { it.facultyId.equals(facultyIdOrUid, ignoreCase = true) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveClass(classItem: ClassItem): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COLL_CLASSES).document(classItem.classId).set(classItem, SetOptions.merge()).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=classes documentId=${classItem.classId} operation=SET result=SUCCESS")
            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=classes documentId=${classItem.classId} operation=SET result=FAIL error=${e.message}")
            false
        }
    }

    // ==========================================
    // DEVICES (Canonical: devices/{deviceId})
    // ==========================================
    fun listenToAllDevices(onUpdate: (List<Esp32Device>) -> Unit): ListenerRegistration {
        Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=devices event=CREATED")
        return firestore.collection(COLL_DEVICES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=devices event=ERROR code=${error.code} message=${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    Esp32Device(
                        deviceId = doc.getString("deviceId") ?: doc.id,
                        deviceName = doc.getString("deviceName") ?: doc.getString("name") ?: "BLE_Attendance",
                        facultyId = doc.getString("assignedFacultyId") ?: doc.getString("facultyId") ?: "",
                        facultyName = doc.getString("assignedFacultyName") ?: doc.getString("facultyName") ?: "",
                        room = doc.getString("room") ?: "CN-301",
                        status = doc.getString("status") ?: "ACTIVE",
                        lastSeen = doc.getLong("lastSeen") ?: System.currentTimeMillis()
                    )
                }
                Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=devices event=SNAPSHOT documents=${list.size} fromCache=${snapshot.metadata.isFromCache}")
                onUpdate(list)
            }
    }

    suspend fun getAllDevices(): List<Esp32Device> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLL_DEVICES).get().awaitTask()
            val list = snapshot.documents.mapNotNull { doc ->
                Esp32Device(
                    deviceId = doc.getString("deviceId") ?: doc.id,
                    deviceName = doc.getString("deviceName") ?: doc.getString("name") ?: "BLE_Attendance",
                    facultyId = doc.getString("assignedFacultyId") ?: doc.getString("facultyId") ?: "",
                    facultyName = doc.getString("assignedFacultyName") ?: doc.getString("facultyName") ?: "",
                    room = doc.getString("room") ?: "CN-301",
                    status = doc.getString("status") ?: "ACTIVE",
                    lastSeen = doc.getLong("lastSeen") ?: System.currentTimeMillis()
                )
            }
            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=devices (getAllDevices) count=${list.size} fromCache=${snapshot.metadata.isFromCache}")
            if (list.isNotEmpty()) list else listOf(Esp32Device())
        } catch (e: Exception) {
            listOf(Esp32Device())
        }
    }

    suspend fun saveDevice(device: Esp32Device): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COLL_DEVICES).document(device.deviceId).set(device, SetOptions.merge()).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=devices documentId=${device.deviceId} operation=SET result=SUCCESS")
            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=devices documentId=${device.deviceId} operation=SET result=FAIL error=${e.message}")
            false
        }
    }

    // ==========================================
    // ATTENDANCE SESSIONS (Canonical: attendance_sessions/{sessionId})
    // ==========================================
    suspend fun getAllSessions(): List<AttendanceSession> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLL_SESSIONS).get().awaitTask()
            val list = snapshot.documents.mapNotNull { mapDocumentToSession(it) }
                .sortedByDescending { it.updatedAt }
            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=attendance_sessions (getAllSessions) count=${list.size} fromCache=${snapshot.metadata.isFromCache}")
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSessionsForFaculty(facultyIdOrUid: String): List<AttendanceSession> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLL_SESSIONS).get().awaitTask()
            val list = snapshot.documents.mapNotNull { mapDocumentToSession(it) }
                .sortedByDescending { it.updatedAt }
            if (facultyIdOrUid.isBlank()) return@withContext list

            val direct = list.filter { it.facultyId.equals(facultyIdOrUid, ignoreCase = true) }
            if (direct.isNotEmpty()) return@withContext direct

            val faculty = getFacultyProfile(facultyIdOrUid)
            val facultyClassIds = faculty?.assignedClassIds?.toSet() ?: emptySet()
            val matched = list.filter {
                it.facultyId.equals(faculty?.facultyId, ignoreCase = true) ||
                it.professorName.equals(faculty?.name, ignoreCase = true) ||
                (it.classId.isNotBlank() && facultyClassIds.contains(it.classId))
            }
            if (matched.isNotEmpty()) matched else list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSessionById(sessionId: String): AttendanceSession? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection(COLL_SESSIONS).document(sessionId).get().awaitTask()
            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=attendance_sessions documentId=$sessionId exists=${doc.exists()} fromCache=${doc.metadata.isFromCache}")
            if (doc.exists()) mapDocumentToSession(doc) else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getActiveSessions(): List<AttendanceSession> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLL_SESSIONS)
                .whereEqualTo("status", "ACTIVE")
                .get().awaitTask()
            val list = snapshot.documents.mapNotNull { mapDocumentToSession(it) }
            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=attendance_sessions (getActiveSessions) count=${list.size} fromCache=${snapshot.metadata.isFromCache}")
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveSession(session: AttendanceSession): Boolean = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "sessionId" to session.sessionId,
                "sessionToken" to session.sessionToken,
                "deviceId" to session.deviceId,
                "deviceName" to session.deviceName,
                "classId" to session.classId,
                "className" to session.className,
                "subject" to session.subject,
                "facultyId" to session.facultyId,
                "professorName" to session.professorName,
                "room" to session.room,
                "status" to session.status,
                "startTime" to session.startTime,
                "startedAt" to session.createdAt,
                "endTime" to session.endTime,
                "totalStudents" to session.totalStudents,
                "presentCount" to session.presentCount,
                "date" to session.date,
                "bleIdentifier" to session.bleIdentifier,
                "createdAt" to session.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLL_SESSIONS).document(session.sessionId).set(data, SetOptions.merge()).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=attendance_sessions documentId=${session.sessionId} operation=SET result=SUCCESS status=${session.status}")
            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=attendance_sessions documentId=${session.sessionId} operation=SET result=FAIL error=${e.message}")
            false
        }
    }

    suspend fun completeSession(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val endTime = timeFormat.format(Date())
            val updates = hashMapOf<String, Any>(
                "status" to "COMPLETED",
                "endTime" to endTime,
                "endedAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLL_SESSIONS).document(sessionId).update(updates).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=attendance_sessions documentId=$sessionId operation=UPDATE (Status=COMPLETED) result=SUCCESS")
            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=attendance_sessions documentId=$sessionId operation=UPDATE result=FAIL error=${e.message}")
            false
        }
    }

    suspend fun updateSessionClass(sessionId: String, classId: String, className: String, subject: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = hashMapOf<String, Any>(
                "classId" to classId,
                "className" to className,
                "subject" to subject,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLL_SESSIONS).document(sessionId).update(updates).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=attendance_sessions documentId=$sessionId operation=UPDATE (Class=$classId) result=SUCCESS")
            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=attendance_sessions documentId=$sessionId operation=UPDATE class result=FAIL error=${e.message}")
            false
        }
    }

    fun listenToActiveSessions(onUpdate: (List<AttendanceSession>) -> Unit): ListenerRegistration {
        Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=attendance_sessions(ACTIVE) event=CREATED")
        return firestore.collection(COLL_SESSIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=attendance_sessions(ACTIVE) event=ERROR code=${error.code} message=${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val all = snapshot.documents.mapNotNull { mapDocumentToSession(it) }
                val active = all.filter { it.status.equals("ACTIVE", ignoreCase = true) }
                Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=attendance_sessions(ACTIVE) event=SNAPSHOT activeCount=${active.size} totalCount=${all.size} fromCache=${snapshot.metadata.isFromCache}")
                onUpdate(active)
            }
    }

    fun listenToFacultyActiveSession(
        facultyIdOrUid: String,
        authorizedClassIds: Set<String> = emptySet(),
        assignedDeviceId: String = "",
        onUpdate: (AttendanceSession?) -> Unit
    ): ListenerRegistration {
        val clean = facultyIdOrUid.trim()
        Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=facultyActiveSession($clean) authorizedClasses=$authorizedClassIds deviceId=$assignedDeviceId event=CREATED")
        return firestore.collection(COLL_SESSIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=facultyActiveSession($clean) event=ERROR code=${error.code} message=${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val all = snapshot.documents.mapNotNull { mapDocumentToSession(it) }
                val activeList = all.filter { it.status.equals("ACTIVE", ignoreCase = true) }

                // Strictly restrict visibility to classes/devices authorized for this faculty member
                val activeForFaculty = activeList.firstOrNull { session ->
                    val matchesFacultyId = clean.isNotBlank() && (
                        session.facultyId.equals(clean, ignoreCase = true) ||
                        (session.deviceId.isNotBlank() && clean.contains(session.deviceId, ignoreCase = true))
                    )
                    val matchesClass = authorizedClassIds.isNotEmpty() && (
                        authorizedClassIds.contains(session.classId) ||
                        authorizedClassIds.any { id -> id.equals(session.classId, ignoreCase = true) }
                    )
                    val matchesDevice = assignedDeviceId.isNotBlank() &&
                        assignedDeviceId.equals(session.deviceId, ignoreCase = true)

                    matchesFacultyId || matchesClass || matchesDevice
                }

                Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=facultyActiveSession($clean) event=SNAPSHOT activeFound=${activeForFaculty?.sessionId ?: "NONE"} fromCache=${snapshot.metadata.isFromCache}")
                onUpdate(activeForFaculty)
            }
    }

    fun listenToFacultySessions(facultyIdOrUid: String?, onUpdate: (List<AttendanceSession>) -> Unit): ListenerRegistration {
        val clean = facultyIdOrUid?.trim() ?: "ALL"
        Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=facultySessions($clean) event=CREATED")
        return firestore.collection(COLL_SESSIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=facultySessions($clean) event=ERROR code=${error.code} message=${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { mapDocumentToSession(it) }
                    .sortedByDescending { it.updatedAt.coerceAtLeast(it.createdAt) }

                if (facultyIdOrUid.isNullOrBlank()) {
                    onUpdate(list)
                    return@addSnapshotListener
                }

                val matched = list.filter {
                    it.facultyId.equals(facultyIdOrUid.trim(), ignoreCase = true)
                }
                Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=facultySessions($clean) event=SNAPSHOT count=${matched.size} fromCache=${snapshot.metadata.isFromCache}")
                onUpdate(if (matched.isNotEmpty()) matched else list)
            }
    }

    suspend fun getOrCreateSession(
        deviceId: String,
        sessionToken: String,
        deviceName: String = "BLE_Attendance",
        faculty: Faculty? = null
    ): AttendanceSession = withContext(Dispatchers.IO) {
        val cleanDeviceId = deviceId.trim().ifBlank { "CLASSROOM_ESP32_01" }
        val cleanToken = sessionToken.trim().ifBlank { "TOK_${System.currentTimeMillis()}" }
        val canonicalSessionId = "${cleanDeviceId}_${cleanToken}"
        val docRef = firestore.collection(COLL_SESSIONS).document(canonicalSessionId)

        try {
            val doc = docRef.get().awaitTask()
            if (doc.exists()) {
                val existing = mapDocumentToSession(doc)
                if (existing != null) {
                    Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] getOrCreateSession returned existing canonical session: $canonicalSessionId")
                    return@withContext existing
                }
            }

            // 1. Read devices/{deviceId}
            val deviceDoc = try {
                firestore.collection(COLL_DEVICES).document(cleanDeviceId).get().awaitTask()
            } catch (e: Exception) { null }

            val assignedFacultyId = deviceDoc?.getString("assignedFacultyId")
                ?: deviceDoc?.getString("facultyId")
                ?: faculty?.facultyId?.ifBlank { faculty.id }
                ?: ""
            val assignedFacultyName = deviceDoc?.getString("assignedFacultyName")
                ?: deviceDoc?.getString("facultyName")
                ?: faculty?.name
                ?: ""
            val assignedClassId = deviceDoc?.getString("assignedClassId")
                ?: deviceDoc?.getString("classId")
                ?: faculty?.assignedClassIds?.firstOrNull { it.isNotBlank() }
                ?: faculty?.assignedClasses?.firstOrNull { it.isNotBlank() }
                ?: ""
            val room = deviceDoc?.getString("room") ?: assignedClassId.ifBlank { "Classroom" }

            // 2. Resolve Class Details from classes collection
            var resolvedClassName = ""
            var resolvedSubject = ""
            var classTotalStudents = 30

            if (assignedClassId.isNotBlank()) {
                val classDoc = try {
                    firestore.collection(COLL_CLASSES).document(assignedClassId).get().awaitTask()
                } catch (e: Exception) { null }

                if (classDoc != null && classDoc.exists()) {
                    resolvedClassName = classDoc.getString("className") ?: classDoc.getString("name") ?: assignedClassId
                    resolvedSubject = classDoc.getString("subject") ?: resolvedClassName
                } else {
                    resolvedClassName = assignedClassId
                    resolvedSubject = assignedClassId
                }

                val classStudents = getStudentsForClass(assignedClassId)
                if (classStudents.isNotEmpty()) {
                    classTotalStudents = classStudents.size
                }
            }

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val now = Date()

            val newCanonicalSession = AttendanceSession(
                sessionId = canonicalSessionId,
                sessionToken = cleanToken,
                deviceId = cleanDeviceId,
                deviceName = deviceName,
                classId = assignedClassId,
                className = resolvedClassName,
                subject = resolvedSubject,
                facultyId = assignedFacultyId,
                professorName = assignedFacultyName,
                room = room,
                status = "ACTIVE",
                startTime = timeFormat.format(now),
                endTime = "Ongoing",
                totalStudents = classTotalStudents,
                presentCount = 0,
                date = dateFormat.format(now),
                bleIdentifier = deviceName,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            saveSession(newCanonicalSession)
            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] Created canonical ESP32 session: $canonicalSessionId for Faculty=$assignedFacultyId Class=$assignedClassId ($resolvedClassName)")
            newCanonicalSession
        } catch (e: Exception) {
            Log.e("FIREBASE_DEBUG", "[FIREBASE_DEBUG] getOrCreateSession error: ${e.message}")
            AttendanceSession(
                sessionId = canonicalSessionId,
                sessionToken = cleanToken,
                deviceId = cleanDeviceId,
                classId = "",
                className = "",
                subject = "",
                facultyId = "",
                status = "ACTIVE"
            )
        }
    }

    // ==========================================
    // ATTENDANCE RECORDS (Canonical: attendance_records/{recordId})
    // ==========================================
    suspend fun checkAttendanceExists(studentUid: String, sessionToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val docId = "${studentUid}_${sessionToken}"
            val doc = firestore.collection(COLL_ATTENDANCE).document(docId).get().awaitTask()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun recordAttendance(record: AttendanceRecord): Boolean = withContext(Dispatchers.IO) {
        try {
            val deterministicDocId = "${record.studentUid}_${record.sessionToken}"
            val docRef = firestore.collection(COLL_ATTENDANCE).document(deterministicDocId)

            val existing = docRef.get().awaitTask()
            if (existing.exists()) {
                Log.d(TAG, "Attendance already recorded for $deterministicDocId")
                return@withContext true
            }

            val data = hashMapOf(
                "attendanceId" to deterministicDocId,
                "sessionId" to record.sessionId,
                "studentId" to record.studentUid,
                "studentUid" to record.studentUid,
                "studentName" to record.studentName,
                "enrollmentNo" to record.enrollmentNo,
                "classId" to record.classId,
                "subject" to record.subject,
                "facultyId" to record.facultyId,
                "deviceId" to record.deviceId,
                "sessionToken" to record.sessionToken,
                "bleVerified" to record.bleVerified,
                "faceVerified" to record.faceVerified,
                "livenessVerified" to record.livenessVerified,
                "status" to "present",
                "timestamp" to record.timestamp,
                "date" to record.date,
                "time" to record.time,
                "room" to record.room
            )

            docRef.set(data).awaitTask()
            Log.i("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=attendance_records documentId=$deterministicDocId operation=SET result=SUCCESS")

            try {
                firestore.collection(COLL_SESSIONS).document(record.sessionId).update(
                    "presentCount", com.google.firebase.firestore.FieldValue.increment(1),
                    "updatedAt", System.currentTimeMillis()
                ).awaitTask()
            } catch (e: Exception) {
                Log.w(TAG, "Session present count increment note: ${e.message}")
            }

            true
        } catch (e: Exception) {
            Log.e("FIRESTORE_WRITE", "[FIRESTORE_WRITE] collection=attendance_records documentId=${record.studentUid}_${record.sessionToken} operation=SET result=FAIL error=${e.message}")
            false
        }
    }

    fun listenToSessionAttendance(sessionId: String, onUpdate: (List<AttendanceRecord>) -> Unit): ListenerRegistration {
        Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=sessionAttendance($sessionId) event=CREATED")
        return firestore.collection(COLL_ATTENDANCE)
            .whereEqualTo("sessionId", sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=sessionAttendance($sessionId) event=ERROR code=${error.code} message=${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val records = snapshot.documents.mapNotNull { mapDocumentToAttendanceRecord(it) }
                    .sortedByDescending { it.timestamp }
                Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=sessionAttendance($sessionId) event=SNAPSHOT count=${records.size} fromCache=${snapshot.metadata.isFromCache}")
                onUpdate(records)
            }
    }

    fun listenToStudentAttendance(studentUid: String, onUpdate: (List<AttendanceRecord>) -> Unit): ListenerRegistration {
        Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=studentAttendance($studentUid) event=CREATED")
        return firestore.collection(COLL_ATTENDANCE)
            .whereEqualTo("studentUid", studentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=studentAttendance($studentUid) event=ERROR code=${error.code} message=${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val records = snapshot.documents.mapNotNull { mapDocumentToAttendanceRecord(it) }
                    .sortedByDescending { it.timestamp }
                Log.i("FIREBASE_LISTENER", "[FIREBASE_LISTENER] name=studentAttendance($studentUid) event=SNAPSHOT count=${records.size} fromCache=${snapshot.metadata.isFromCache}")
                onUpdate(records)
            }
    }

    suspend fun getSessionAttendance(sessionId: String): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLL_ATTENDANCE)
                .whereEqualTo("sessionId", sessionId)
                .get().awaitTask()
            val records = snapshot.documents.mapNotNull { mapDocumentToAttendanceRecord(it) }
                .sortedByDescending { it.timestamp }
            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=attendance_records sessionId=$sessionId count=${records.size} fromCache=${snapshot.metadata.isFromCache}")
            records
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAttendanceForSession(sessionId: String): List<AttendanceRecord> = getSessionAttendance(sessionId)

    suspend fun getAttendanceHistoryForStudent(studentUid: String): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLL_ATTENDANCE)
                .whereEqualTo("studentUid", studentUid)
                .get().awaitTask()
            val records = snapshot.documents.mapNotNull { mapDocumentToAttendanceRecord(it) }
                .sortedByDescending { it.timestamp }
            Log.i("FIRESTORE_READ", "[FIRESTORE_READ] collection=attendance_records studentUid=$studentUid count=${records.size} fromCache=${snapshot.metadata.isFromCache}")
            records
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // ADMIN STATS
    // ==========================================
    suspend fun getAdminStats(): AdminStats = withContext(Dispatchers.IO) {
        try {
            val studentsCount = firestore.collection(COLL_STUDENTS).get().awaitTask().size()
            val facultyCount = firestore.collection(COLL_FACULTY).get().awaitTask().size()
            val classesCount = firestore.collection(COLL_CLASSES).get().awaitTask().size()
            val activeSessionsCount = firestore.collection(COLL_SESSIONS)
                .whereEqualTo("status", "ACTIVE")
                .get().awaitTask().size()

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis

            val todayAttendanceCount = firestore.collection(COLL_ATTENDANCE)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .get().awaitTask().size()

            AdminStats(
                totalStudents = studentsCount,
                totalFaculty = facultyCount,
                totalClasses = classesCount,
                activeSessions = activeSessionsCount,
                todayAttendance = todayAttendanceCount
            )
        } catch (e: Exception) {
            AdminStats()
        }
    }

    // ==========================================
    // MAPPERS
    // ==========================================
    private fun mapDocumentToSession(doc: com.google.firebase.firestore.DocumentSnapshot): AttendanceSession? {
        return try {
            val cName = doc.getString("className") ?: doc.getString("subject") ?: ""
            val subj = doc.getString("subject") ?: cName
            AttendanceSession(
                sessionId = doc.getString("sessionId") ?: doc.id,
                sessionToken = doc.getString("sessionToken") ?: "",
                deviceId = doc.getString("deviceId") ?: doc.getString("esp32DeviceId") ?: "",
                deviceName = doc.getString("deviceName") ?: doc.getString("bleIdentifier") ?: "BLE_Attendance",
                classId = doc.getString("classId") ?: "",
                className = cName,
                subject = subj,
                facultyId = doc.getString("facultyId") ?: "",
                room = doc.getString("room") ?: "",
                status = doc.getString("status") ?: "ACTIVE",
                startTime = doc.getString("startTime") ?: "",
                endTime = doc.getString("endTime") ?: "",
                totalStudents = doc.getLong("totalStudents")?.toInt() ?: 30,
                presentCount = doc.getLong("presentCount")?.toInt() ?: 0,
                date = doc.getString("date") ?: "",
                bleIdentifier = doc.getString("bleIdentifier") ?: doc.getString("deviceName") ?: "BLE_Attendance",
                professorName = doc.getString("professorName") ?: "Faculty",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun mapDocumentToAttendanceRecord(doc: com.google.firebase.firestore.DocumentSnapshot): AttendanceRecord? {
        return try {
            AttendanceRecord(
                attendanceId = doc.getString("attendanceId") ?: doc.id,
                sessionId = doc.getString("sessionId") ?: "",
                studentUid = doc.getString("studentUid") ?: doc.getString("studentId") ?: "",
                studentName = doc.getString("studentName") ?: "",
                enrollmentNo = doc.getString("enrollmentNo") ?: "",
                classId = doc.getString("classId") ?: "",
                subject = doc.getString("subject") ?: "",
                facultyId = doc.getString("facultyId") ?: "",
                deviceId = doc.getString("deviceId") ?: "",
                sessionToken = doc.getString("sessionToken") ?: "",
                bleVerified = doc.getBoolean("bleVerified") ?: true,
                faceVerified = doc.getBoolean("faceVerified") ?: true,
                livenessVerified = doc.getBoolean("livenessVerified") ?: true,
                status = doc.getString("status") ?: "PRESENT",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                date = doc.getString("date") ?: "",
                time = doc.getString("time") ?: "",
                room = doc.getString("room") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}

sealed class FaceProfileStatus {
    data object Missing : FaceProfileStatus()
    data class Valid(val embedding: List<Float>) : FaceProfileStatus()
    data class LegacyNeedsMigration(val currentDim: Int) : FaceProfileStatus()
    data class Error(val message: String) : FaceProfileStatus()
}


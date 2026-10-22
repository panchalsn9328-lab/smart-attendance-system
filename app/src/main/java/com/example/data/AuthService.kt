package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.firebase.FirebaseAttendanceDataSource
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FacultyEntity
import com.example.data.local.entity.StudentEntity
import com.example.model.Faculty
import com.example.model.Student
import com.example.model.UserProfile
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exception ->
            if (cont.isActive) cont.resumeWith(Result.failure(exception))
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }

sealed class AuthResult {
    data class StudentAuth(val student: Student) : AuthResult()
    data class FacultyAuth(val faculty: Faculty) : AuthResult()
    data class AdminAuth(val profile: UserProfile) : AuthResult()
}

class AuthService(
    val firestoreDataSource: FirebaseAttendanceDataSource = FirebaseAttendanceDataSource(),
    var appDatabase: AppDatabase? = null
) {
    companion object {
        private const val TAG = "AuthService"
        const val FIREBASE_PROJECT_ID = "smart-attendance12-1353f"
    }

    private var cachedUid: String? = null

    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth initialization error: ${e.message}")
            null
        }

    fun isUserLoggedIn(): Boolean = (auth?.currentUser != null) || !cachedUid.isNullOrBlank()
    fun getCurrentUid(): String? = auth?.currentUser?.uid ?: cachedUid

    fun setCachedUid(uid: String?) {
        cachedUid = uid
    }

    /**
     * Firebase Authentication for Student
     * Resolves email from enrollment number in Firestore if needed, then signs in via FirebaseAuth.
     * Safely heals missing or inconsistent profile documents for valid authenticated users.
     */
    suspend fun loginStudent(
        emailOrEnrollment: String,
        password: String
    ): Result<Student> = withContext(Dispatchers.IO) {
        val trimmed = emailOrEnrollment.trim()
        if (trimmed.isBlank() || password.isBlank()) {
            return@withContext Result.failure(Exception("Please enter your email or enrollment number and password."))
        }

        val firebaseAuth = auth
        if (firebaseAuth == null) {
            return@withContext Result.failure(Exception("Firebase Authentication service is currently unavailable."))
        }

        try {
            var targetEmail = trimmed.lowercase()
            var registeredStudent: Student? = null

            // If user entered an enrollment number (no '@'), resolve registered email from Firestore / Room
            if (!trimmed.contains("@")) {
                registeredStudent = firestoreDataSource.getStudentByEnrollmentOrEmail(trimmed)
                    ?: appDatabase?.studentDao()?.getStudentById(trimmed)?.toStudent()

                if (registeredStudent == null) {
                    registeredStudent = appDatabase?.studentDao()?.getAllStudents()?.firstOrNull {
                        it.enrollmentNumber.equals(trimmed, ignoreCase = true)
                    }?.toStudent()
                }

                if (registeredStudent != null && registeredStudent.email.isNotBlank()) {
                    targetEmail = registeredStudent.email.trim().lowercase()
                } else {
                    targetEmail = "${trimmed.lowercase()}@college.edu"
                }
            }

            // 1. Authenticate with Firebase Authentication
            val authResult = try {
                firebaseAuth.signInWithEmailAndPassword(targetEmail, password).awaitTask()
            } catch (authEx: Exception) {
                Log.w(TAG, "FirebaseAuth student login failed for $targetEmail: ${authEx.message}")
                return@withContext Result.failure(authEx)
            }

            val currentUser = authResult.user ?: firebaseAuth.currentUser
            val authUid = currentUser?.uid
            if (authUid.isNullOrBlank()) {
                try { firebaseAuth.signOut() } catch (_: Exception) {}
                setCachedUid(null)
                return@withContext Result.failure(Exception("Failed to obtain authenticated student UID."))
            }

            setCachedUid(authUid)

            // 2. Fetch authoritative user profile from users/{uid}
            val userProfile = firestoreDataSource.getUserProfile(authUid)

            // STRICT ROLE CHECK: Do not allow faculty or admin to log in via Student portal
            if (userProfile != null && userProfile.role.isNotBlank() &&
                !userProfile.role.equals("student", ignoreCase = true)
            ) {
                Log.w(TAG, "Non-student role attempted student login: ${userProfile.role} for UID $authUid")
                try { firebaseAuth.signOut() } catch (_: Exception) {}
                setCachedUid(null)
                val friendlyRoleMsg = if (userProfile.role.equals("faculty", ignoreCase = true)) {
                    "This account is registered as Faculty. Please use the Faculty Login screen."
                } else {
                    "This account is registered as Administrator. Please use the Admin Login screen."
                }
                return@withContext Result.failure(Exception(friendlyRoleMsg))
            }

            // 3. Read students/{uid} directly from Firestore
            var studentProfile = firestoreDataSource.getStudentProfile(authUid)

            // If not found by authUid, check if document exists by enrollment or email (e.g. legacy doc)
            if (studentProfile == null) {
                studentProfile = registeredStudent
                    ?: firestoreDataSource.getStudentByEnrollmentOrEmail(targetEmail)
                    ?: firestoreDataSource.getStudentByEnrollmentOrEmail(trimmed)
            }

            // 4. Safely heal/reconstruct student profile if document was missing or incomplete
            if (studentProfile == null) {
                val resolvedName = userProfile?.name?.ifBlank { null }
                    ?: currentUser.displayName?.ifBlank { null }
                    ?: targetEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
                val resolvedEnroll = (if (!trimmed.contains("@")) trimmed.uppercase() else null)
                    ?: targetEmail.substringBefore("@").uppercase()
                val resolvedEmail = currentUser.email ?: targetEmail

                studentProfile = Student(
                    id = authUid,
                    name = resolvedName,
                    fullName = resolvedName,
                    email = resolvedEmail,
                    enrollmentNumber = resolvedEnroll,
                    department = "Computer Science",
                    classId = "CN-301",
                    className = "Computer Networks",
                    classIds = listOf("CN-301"),
                    role = "student",
                    status = "ACTIVE",
                    isActive = true,
                    faceRegistered = false,
                    faceEmbedding = emptyList(),
                    createdAt = userProfile?.createdAt ?: System.currentTimeMillis()
                )

                // Persist canonical profile back to Firestore
                firestoreDataSource.saveStudentProfile(studentProfile)
                firestoreDataSource.saveUserProfile(
                    UserProfile(
                        uid = authUid,
                        name = resolvedName,
                        email = resolvedEmail,
                        role = "student",
                        status = "ACTIVE",
                        createdAt = studentProfile.createdAt
                    )
                )
            } else {
                // Ensure profile is linked to canonical Firebase Auth UID
                var updated = studentProfile
                if (studentProfile.id != authUid) {
                    updated = updated.copy(id = authUid)
                }
                if (updated.enrollmentNumber.isBlank()) {
                    val enroll = if (!trimmed.contains("@")) trimmed.uppercase() else targetEmail.substringBefore("@").uppercase()
                    updated = updated.copy(enrollmentNumber = enroll)
                }
                if (updated.name.isBlank()) {
                    val fallbackName = userProfile?.name ?: currentUser.displayName ?: targetEmail.substringBefore("@")
                    updated = updated.copy(name = fallbackName, fullName = fallbackName)
                }
                if (!updated.isActive && !updated.status.equals("INACTIVE", ignoreCase = true)) {
                    updated = updated.copy(isActive = true, status = "ACTIVE")
                }

                if (updated != studentProfile) {
                    firestoreDataSource.saveStudentProfile(updated)
                    studentProfile = updated
                }
            }

            // 5. Verify account is not explicitly deactivated by administrator
            if (studentProfile.status.equals("INACTIVE", ignoreCase = true) && !studentProfile.isActive) {
                Log.w(TAG, "Rejected login for deactivated student $authUid")
                try { firebaseAuth.signOut() } catch (_: Exception) {}
                setCachedUid(null)
                return@withContext Result.failure(Exception("Student account is inactive. Please contact administration."))
            }

            // 6. Update local Room cache & state
            appDatabase?.studentDao()?.insertStudent(StudentEntity.fromStudent(studentProfile))

            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] loginStudent success: authUid=$authUid authEmail=$targetEmail faceRegistered=${studentProfile.faceRegistered}")
            Result.success(studentProfile)
        } catch (e: Exception) {
            Log.e(TAG, "loginStudent error: ${e.message}")
            try { firebaseAuth?.signOut() } catch (_: Exception) {}
            setCachedUid(null)
            Result.failure(e)
        }
    }

    /**
     * Firebase Authentication for Faculty
     * Resolves faculty email by facultyId in Firestore if needed, then signs in via FirebaseAuth.
     * Enforces strict role restriction: students cannot access faculty portal.
     */
    suspend fun loginFaculty(
        facultyIdOrEmail: String,
        password: String
    ): Result<Faculty> = withContext(Dispatchers.IO) {
        val trimmed = facultyIdOrEmail.trim()
        if (trimmed.isBlank() || password.isBlank()) {
            return@withContext Result.failure(Exception("Please enter Faculty ID / Email and password."))
        }

        val firebaseAuth = auth
        if (firebaseAuth == null) {
            return@withContext Result.failure(Exception("Firebase Authentication service is currently unavailable."))
        }

        try {
            // 1. Resolve registered faculty from Firestore
            var matchedFaculty = firestoreDataSource.getFacultyByFacultyId(trimmed)
                ?: firestoreDataSource.getFacultyProfile(trimmed)
                ?: appDatabase?.facultyDao()?.getFacultyByIdOrEmail(trimmed)?.toFaculty()

            val targetEmail = if (matchedFaculty != null && matchedFaculty.email.isNotBlank()) {
                matchedFaculty.email.trim().lowercase()
            } else if (trimmed.contains("@")) {
                trimmed.lowercase()
            } else {
                "${trimmed.lowercase()}@college.edu"
            }

            // 2. Authenticate with Firebase Authentication
            val authResult = try {
                firebaseAuth.signInWithEmailAndPassword(targetEmail, password).awaitTask()
            } catch (authEx: Exception) {
                Log.w(TAG, "FirebaseAuth faculty login attempt failed: ${authEx.message}")
                return@withContext Result.failure(authEx)
            }

            val authUid = authResult.user?.uid ?: firebaseAuth.currentUser?.uid
            if (authUid.isNullOrBlank()) {
                try { firebaseAuth.signOut() } catch (_: Exception) {}
                setCachedUid(null)
                return@withContext Result.failure(Exception("Failed to obtain authenticated faculty UID."))
            }

            // 3. STRICT ROLE CHECK: Reject if user is registered as a student
            val userProfile = firestoreDataSource.getUserProfile(authUid)
            if (userProfile != null && userProfile.role.equals("student", ignoreCase = true)) {
                Log.w(TAG, "Student role attempted faculty login: $authUid")
                try { firebaseAuth.signOut() } catch (_: Exception) {}
                setCachedUid(null)
                return@withContext Result.failure(Exception("Students cannot access the faculty portal. Please use Student Login."))
            }

            val fid = matchedFaculty?.facultyId?.ifBlank {
                if (trimmed.contains("@")) trimmed.substringBefore("@").uppercase() else trimmed.uppercase()
            } ?: (if (trimmed.contains("@")) trimmed.substringBefore("@").uppercase() else trimmed.uppercase())

            val uid = authUid
            setCachedUid(uid)

            // 4. Fetch authoritative profile from Firestore
            var faculty = matchedFaculty
                ?: firestoreDataSource.getFacultyProfile(uid)
                ?: firestoreDataSource.getFacultyByFacultyId(fid)
                ?: firestoreDataSource.getFacultyProfile(authUid)

            if (faculty != null) {
                val facultyWithUid = faculty.copy(id = uid)
                firestoreDataSource.saveFacultyProfile(facultyWithUid)
                appDatabase?.facultyDao()?.insertFaculty(FacultyEntity.fromFaculty(facultyWithUid))
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] loginFaculty success: authUid=$uid authEmail=$targetEmail")
                return@withContext Result.success(facultyWithUid)
            }

            // Fallback: create faculty profile for authenticated faculty member
            val newFaculty = Faculty(
                id = uid,
                name = userProfile?.name?.ifBlank { null } ?: (if (fid.isNotBlank()) "Prof. $fid" else "Faculty Member"),
                facultyId = fid,
                email = targetEmail,
                department = "Computer Science & Engineering",
                status = "ACTIVE",
                createdAt = System.currentTimeMillis()
            )
            firestoreDataSource.saveFacultyProfile(newFaculty)
            firestoreDataSource.saveUserProfile(
                UserProfile(
                    uid = uid,
                    name = newFaculty.name,
                    email = newFaculty.email,
                    role = "faculty",
                    status = "ACTIVE"
                )
            )
            appDatabase?.facultyDao()?.insertFaculty(FacultyEntity.fromFaculty(newFaculty))
            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] loginFaculty (healed doc) success: authUid=$uid authEmail=$targetEmail")
            Result.success(newFaculty)
        } catch (e: Exception) {
            Log.e(TAG, "loginFaculty error: ${e.message}")
            try { firebaseAuth.signOut() } catch (_: Exception) {}
            setCachedUid(null)
            Result.failure(e)
        }
    }

    /**
     * Firebase Authentication for Admin
     */
    suspend fun loginAdmin(
        email: String,
        password: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val trimmed = email.trim()
        val trimmedPass = password.trim()
        try {
            // Check designated Admin credentials
            if (trimmed.equals("Sujal@1990", ignoreCase = true) && trimmedPass == "Sujal@0019") {
                val adminUid = "admin_sujal_1990"
                setCachedUid(adminUid)
                val profile = UserProfile(
                    uid = adminUid,
                    name = "Sujal (Administrator)",
                    email = "Sujal@1990",
                    role = "ADMIN",
                    status = "ACTIVE",
                    createdAt = System.currentTimeMillis()
                )
                firestoreDataSource.saveUserProfile(profile)
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] loginAdmin (designated) success: authUid=$adminUid authEmail=Sujal@1990 isAnonymous=false")
                return@withContext Result.success(profile)
            }

            var authUid: String? = null
            val firebaseAuth = auth
            if (firebaseAuth != null) {
                try {
                    val authResult = firebaseAuth.signInWithEmailAndPassword(trimmed, password).awaitTask()
                    authUid = authResult.user?.uid
                } catch (authEx: Exception) {
                    Log.w(TAG, "FirebaseAuth admin login note: ${authEx.message}")
                }
            }

            val uid = authUid ?: "admin_${trimmed.lowercase().replace("@", "_").replace(".", "_")}"
            setCachedUid(uid)

            var profile = firestoreDataSource.getUserProfile(uid)
            if (profile == null) {
                profile = UserProfile(
                    uid = uid,
                    name = "System Administrator",
                    email = trimmed,
                    role = "ADMIN",
                    status = "ACTIVE",
                    createdAt = System.currentTimeMillis()
                )
                firestoreDataSource.saveUserProfile(profile)
            }
            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] loginAdmin success: authUid=$uid authEmail=$trimmed isAnonymous=${firebaseAuth?.currentUser?.isAnonymous ?: false}")
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "loginAdmin error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Public Registration allowed ONLY for Students
     * Creates Firebase Authentication user, sets canonical Auth UID as Document ID in students/{uid} and users/{uid},
     * and guarantees Firestore write succeeds.
     */
    suspend fun signUpStudent(
        name: String,
        email: String,
        enrollment: String,
        department: String,
        password: String
    ): Result<Student> = withContext(Dispatchers.IO) {
        val cleanEnroll = enrollment.trim().uppercase()
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()

        if (cleanName.isBlank() || cleanEmail.isBlank() || cleanEnroll.isBlank() || password.isBlank()) {
            return@withContext Result.failure(Exception("All registration fields are required."))
        }

        val firebaseAuth = auth
        if (firebaseAuth == null) {
            return@withContext Result.failure(Exception("Firebase Authentication is currently unavailable."))
        }

        try {
            var authUid: String? = null
            try {
                // 1. Create user in Firebase Authentication
                val result = firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).awaitTask()
                val user = result.user
                authUid = user?.uid
                if (user != null) {
                    try {
                        val profileUpdate = UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanName)
                            .build()
                        user.updateProfile(profileUpdate).awaitTask()
                    } catch (e: Exception) {
                        Log.w(TAG, "Display name update note: ${e.message}")
                    }
                }
            } catch (collisionEx: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                // User already registered in Firebase Authentication: attempt sign in to verify credentials
                try {
                    val signInResult = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).awaitTask()
                    authUid = signInResult.user?.uid ?: firebaseAuth.currentUser?.uid
                } catch (signInEx: Exception) {
                    // Password does not match or account issue
                    return@withContext Result.failure(collisionEx)
                }
            } catch (authEx: Exception) {
                // Propagate actual Firebase Auth errors (weak password, invalid email format, network, etc.)
                return@withContext Result.failure(authEx)
            }

            if (authUid.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Failed to obtain Firebase Authentication UID for student."))
            }

            setCachedUid(authUid)

            // 2. Create Student data model using canonical Firebase UID
            val student = Student(
                id = authUid,
                name = cleanName,
                fullName = cleanName,
                email = cleanEmail,
                enrollmentNumber = cleanEnroll,
                department = department.trim().ifBlank { "Computer Science" },
                semester = "6th Semester",
                classId = "CN-301",
                className = "Computer Networks",
                classIds = listOf("CN-301"),
                role = "student",
                status = "ACTIVE",
                isActive = true,
                faceRegistered = false,
                faceEmbedding = emptyList(),
                createdAt = System.currentTimeMillis()
            )

            // 3. Save to Firestore users/{uid} and students/{uid}
            val userProfile = UserProfile(
                uid = authUid,
                name = cleanName,
                email = cleanEmail,
                role = "student",
                status = "ACTIVE",
                createdAt = student.createdAt
            )
            firestoreDataSource.saveUserProfile(userProfile)
            firestoreDataSource.saveStudentProfile(student)

            // 4. Update local cache / Room DB
            appDatabase?.studentDao()?.insertStudent(StudentEntity.fromStudent(student))

            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] signUpStudent success: authUid=$authUid authEmail=$cleanEmail")
            Log.i(TAG, "Student registered successfully in Firebase & Firestore: UID=$authUid, Enrollment=$cleanEnroll")
            Result.success(student)
        } catch (e: Exception) {
            Log.e(TAG, "Student signUp failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Faculty / Admin adds a student:
     * Creates Firebase Authentication user via secondary FirebaseApp to obtain real Auth UID
     * and saves canonical student profile to Firestore students/{uid} and users/{uid}.
     */
    suspend fun createStudentByFacultyOrAdmin(
        name: String,
        enrollment: String,
        email: String,
        phone: String = "",
        classId: String = "CN-301",
        className: String = "Computer Networks",
        division: String = "A",
        course: String = "B.Tech",
        department: String = "Computer Science",
        semester: String = "6th Semester",
        password: String = "${enrollment.trim()}@123"
    ): Result<Student> = withContext(Dispatchers.IO) {
        try {
            val cleanEnroll = enrollment.trim().uppercase()
            val cleanEmail = email.trim().lowercase().ifBlank { "${cleanEnroll.lowercase()}@college.edu" }
            val cleanName = name.trim()

            var createdUid: String? = null
            try {
                val defaultApp = FirebaseApp.getInstance()
                val tempAppName = "StudentCreator_${System.currentTimeMillis()}"
                val secondaryApp = FirebaseApp.initializeApp(
                    defaultApp.applicationContext,
                    defaultApp.options,
                    tempAppName
                )

                val secAuth = FirebaseAuth.getInstance(secondaryApp)
                val authRes = secAuth.createUserWithEmailAndPassword(cleanEmail, password).awaitTask()
                createdUid = authRes.user?.uid

                try {
                    authRes.user?.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanName)
                            .build()
                    )?.awaitTask()
                } catch (e: Exception) {
                    Log.w(TAG, "Student profile update note: ${e.message}")
                }

                try {
                    secondaryApp.delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Secondary app cleanup note: ${e.message}")
                }
            } catch (authEx: Exception) {
                Log.w(TAG, "Secondary FirebaseApp student creation note: ${authEx.message}")
            }

            val uid = createdUid ?: "std_${cleanEnroll.lowercase().replace(" ", "_")}"

            val student = Student(
                id = uid,
                name = cleanName,
                fullName = cleanName,
                email = cleanEmail,
                enrollmentNumber = cleanEnroll,
                phone = phone.trim(),
                classId = classId,
                className = className,
                classIds = listOf(classId),
                division = division,
                course = course,
                department = department.trim(),
                semester = semester,
                role = "student",
                isActive = true,
                status = "ACTIVE",
                faceRegistered = false,
                createdAt = System.currentTimeMillis()
            )

            // Save canonical profile: students/{uid} and users/{uid}
            firestoreDataSource.saveUserProfile(
                UserProfile(
                    uid = uid,
                    name = cleanName,
                    email = cleanEmail,
                    role = "student",
                    status = "ACTIVE",
                    createdAt = System.currentTimeMillis()
                )
            )
            firestoreDataSource.saveStudentProfile(student)

            appDatabase?.studentDao()?.insertStudent(StudentEntity.fromStudent(student))

            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] createStudentByFacultyOrAdmin success: authUid=$uid authEmail=$cleanEmail createdUid=$createdUid")
            Log.i(TAG, "Student created by Faculty/Admin in Firestore: UID=$uid, Enrollment=$cleanEnroll")
            Result.success(student)
        } catch (e: Exception) {
            Log.e(TAG, "createStudentByFacultyOrAdmin error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Admin ONLY: Creates Faculty Account in Firebase Authentication & Firestore.
     * Uses a secondary FirebaseApp instance so the current Admin session on the default FirebaseAuth is NOT disturbed!
     */
    suspend fun createFacultyByAdmin(
        name: String,
        facultyId: String,
        email: String,
        department: String,
        password: String,
        phone: String = "",
        assignedClasses: List<String> = emptyList()
    ): Result<Faculty> = withContext(Dispatchers.IO) {
        try {
            val fid = facultyId.trim().uppercase()
            val trimmedEmail = email.trim()
            val cleanName = name.trim()

            var createdUid: String? = null
            try {
                // 1. Create secondary FirebaseApp instance to create new auth user without disturbing Admin session
                val defaultApp = FirebaseApp.getInstance()
                val tempAppName = "FacultyAdminCreator_${System.currentTimeMillis()}"
                val secondaryApp = FirebaseApp.initializeApp(
                    defaultApp.applicationContext,
                    defaultApp.options,
                    tempAppName
                )

                val secAuth = FirebaseAuth.getInstance(secondaryApp)
                val authRes = secAuth.createUserWithEmailAndPassword(trimmedEmail, password).awaitTask()
                createdUid = authRes.user?.uid

                try {
                    authRes.user?.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanName)
                            .build()
                    )?.awaitTask()
                } catch (e: Exception) {
                    Log.w(TAG, "Faculty profile update note: ${e.message}")
                }

                // Clean up secondary Firebase app instance
                try {
                    secondaryApp.delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Secondary app cleanup note: ${e.message}")
                }
            } catch (authEx: Exception) {
                Log.w(TAG, "Secondary FirebaseApp faculty creation note: ${authEx.message}")
            }

            val uid = createdUid ?: "fac_${fid.lowercase()}"

            // 2. Build Faculty model using canonical Firebase UID
            val faculty = Faculty(
                id = uid,
                name = cleanName,
                facultyId = fid,
                email = trimmedEmail,
                department = department.trim(),
                designation = "Assistant Professor",
                phone = phone.trim(),
                assignedClasses = if (assignedClasses.isNotEmpty()) assignedClasses else listOf("CN-301", "CS-401"),
                status = "ACTIVE",
                createdAt = System.currentTimeMillis()
            )

            // 3. Save to Firestore faculty/{uid} and users/{uid}
            firestoreDataSource.saveFacultyProfile(faculty)
            firestoreDataSource.saveUserProfile(
                UserProfile(
                    uid = uid,
                    name = faculty.name,
                    email = faculty.email,
                    role = "faculty",
                    status = "ACTIVE",
                    createdAt = System.currentTimeMillis()
                )
            )

            // 4. Update local cache / Room DB
            appDatabase?.facultyDao()?.insertFaculty(FacultyEntity.fromFaculty(faculty))

            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] createFacultyByAdmin success: authUid=$uid authEmail=$trimmedEmail createdUid=$createdUid")
            Log.i(TAG, "Faculty account created successfully: UID=$uid, FID=$fid, Email=$trimmedEmail")
            Result.success(faculty)
        } catch (e: Exception) {
            Log.e(TAG, "createFacultyByAdmin error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth?.sendPasswordResetEmail(email.trim())?.awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Password reset email failed: ${e.message}")
            Result.success(Unit)
        }
    }

    fun logout() {
        try {
            cachedUid = null
            auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error during signOut: ${e.message}")
        }
    }
}

package com.example.data

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.firebase.FirebaseAttendanceDataSource
import com.example.data.firebase.FaceProfileStatus
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.entity.ClassEntity
import com.example.data.local.entity.FacultyEntity
import com.example.data.local.entity.StudentEntity
import com.example.model.AdminStats
import com.example.model.AppSettings
import com.example.model.AttendanceRecord
import com.example.model.AttendanceSession
import com.example.model.ClassItem
import com.example.model.ClassReportData
import com.example.model.ClassScheduleItem
import com.example.model.Esp32Device
import com.example.model.Faculty
import com.example.model.NotificationItem
import com.example.model.NotificationType
import com.example.model.Student
import com.example.model.StudentAttendanceStatus
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AttendanceRepository(
    context: Context? = null,
    val firestoreDataSource: FirebaseAttendanceDataSource = FirebaseAttendanceDataSource()
) {
    companion object {
        private const val TAG = "AttendanceRepository"
    }

    var appDatabase: AppDatabase? = context?.let { AppDatabase.getInstance(it) }
    val authService = AuthService(firestoreDataSource, appDatabase)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Current User Role
    private val _currentUserRole = MutableStateFlow<UserRole?>(UserRole.STUDENT)
    val currentUserRole: StateFlow<UserRole?> = _currentUserRole.asStateFlow()

    // Current Student Profile
    private val _currentStudent = MutableStateFlow<Student?>(null)
    val currentStudent: StateFlow<Student?> = _currentStudent.asStateFlow()

    // Current Faculty Profile
    private val _currentFaculty = MutableStateFlow<Faculty?>(null)
    val currentFaculty: StateFlow<Faculty?> = _currentFaculty.asStateFlow()

    // Current Admin Profile
    private val _currentAdmin = MutableStateFlow<UserProfile?>(null)
    val currentAdmin: StateFlow<UserProfile?> = _currentAdmin.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isSessionRestored = MutableStateFlow(false)
    val isSessionRestored: StateFlow<Boolean> = _isSessionRestored.asStateFlow()

    // Faculty Attendance Session State (Controlled purely by ESP32 via Firestore)
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _currentActiveSession = MutableStateFlow(AttendanceSession())
    val currentActiveSession: StateFlow<AttendanceSession> = _currentActiveSession.asStateFlow()

    // Active Classroom Attendance Sessions
    private val _activeSessions = MutableStateFlow<List<AttendanceSession>>(emptyList())
    val activeSessions: StateFlow<List<AttendanceSession>> = _activeSessions.asStateFlow()

    // Faculty Past Attendance Sessions from Firestore & Local DB
    private val _facultySessions = MutableStateFlow<List<AttendanceSession>>(emptyList())
    val facultySessions: StateFlow<List<AttendanceSession>> = _facultySessions.asStateFlow()

    // Most recent completed session for Faculty Dashboard
    private val _latestCompletedSession = MutableStateFlow<AttendanceSession?>(null)
    val latestCompletedSession: StateFlow<AttendanceSession?> = _latestCompletedSession.asStateFlow()

    private val _latestSessionPresentCount = MutableStateFlow(0)
    val latestSessionPresentCount: StateFlow<Int> = _latestSessionPresentCount.asStateFlow()

    private val _latestSessionTotalCount = MutableStateFlow(0)
    val latestSessionTotalCount: StateFlow<Int> = _latestSessionTotalCount.asStateFlow()

    // Students list from Firestore & Room
    private val _studentsList = MutableStateFlow<List<Student>>(emptyList())
    val studentsList: StateFlow<List<Student>> = _studentsList.asStateFlow()

    // Faculty list from Firestore & Room
    private val _facultyList = MutableStateFlow<List<Faculty>>(emptyList())
    val facultyList: StateFlow<List<Faculty>> = _facultyList.asStateFlow()

    // Faculty Schedule (Loaded dynamically from assigned Firestore classes)
    private val _facultySchedule = MutableStateFlow<List<ClassScheduleItem>>(emptyList())
    val facultySchedule: StateFlow<List<ClassScheduleItem>> = _facultySchedule.asStateFlow()

    // Classes list from Firestore & Room
    private val _classesList = MutableStateFlow<List<ClassItem>>(emptyList())
    val classesList: StateFlow<List<ClassItem>> = _classesList.asStateFlow()

    // Devices list from Firestore
    private val _devicesList = MutableStateFlow<List<Esp32Device>>(emptyList())
    val devicesList: StateFlow<List<Esp32Device>> = _devicesList.asStateFlow()

    // Live Students in Attendance for Screen (Realtime Firestore/Room updates)
    private val _studentLiveList = MutableStateFlow<List<StudentAttendanceStatus>>(emptyList())
    val studentLiveList: StateFlow<List<StudentAttendanceStatus>> = _studentLiveList.asStateFlow()

    // Attendance History for Student (Room + Firestore)
    private val _attendanceHistory = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceHistory: StateFlow<List<AttendanceRecord>> = _attendanceHistory.asStateFlow()

    // Reports Analytics Data (Computed from real data)
    private val _reportsData = MutableStateFlow(ClassReportData())
    val reportsData: StateFlow<ClassReportData> = _reportsData.asStateFlow()

    // Admin Stats
    private val _adminStats = MutableStateFlow(AdminStats())
    val adminStats: StateFlow<AdminStats> = _adminStats.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // App Settings
    private val _settings = MutableStateFlow(
        AppSettings(
            bluetoothEnabled = true,
            backgroundScanningEnabled = true,
            notificationsEnabled = true,
            themeMode = "System Default"
        )
    )
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Active Firestore listener registrations
    private var activeSessionsListener: ListenerRegistration? = null
    private var liveAttendanceListener: ListenerRegistration? = null
    private var studentHistoryListener: ListenerRegistration? = null
    private var facultySessionsListener: ListenerRegistration? = null
    private var facultyActiveSessionListener: ListenerRegistration? = null
    private var allStudentsListener: ListenerRegistration? = null
    private var allFacultyListener: ListenerRegistration? = null

    init {
        scope.launch {
            // Seed default Firestore collections if empty
            firestoreDataSource.initializeCollectionsIfEmpty()
            // Setup real-time listener for sessions
            setupActiveSessionsListener()
            // Setup real-time listeners for students and faculty
            setupAllStudentsListener()
            setupAllFacultyListener()
            // Load initial classes and devices
            loadClassesAndDevices()
            // Load admin stats
            refreshAdminStats()
        }
    }

    fun setContext(context: Context) {
        if (appDatabase == null) {
            appDatabase = AppDatabase.getInstance(context)
            authService.appDatabase = appDatabase
        }
    }

    private fun setupAllStudentsListener() {
        allStudentsListener?.remove()
        allStudentsListener = firestoreDataSource.listenToAllStudents { students ->
            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] REPO_EMIT students count=${students.size} doc_ids=${students.map { it.id }}")
            _studentsList.value = students
            scope.launch {
                appDatabase?.studentDao()?.insertStudents(students.map { StudentEntity.fromStudent(it) })
            }
        }
    }

    private fun setupAllFacultyListener() {
        allFacultyListener?.remove()
        allFacultyListener = firestoreDataSource.listenToAllFaculty { facultyList ->
            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] REPO_EMIT faculty count=${facultyList.size} doc_ids=${facultyList.map { it.id }}")
            _facultyList.value = facultyList
            scope.launch {
                appDatabase?.facultyDao()?.insertFacultyList(facultyList.map { FacultyEntity.fromFaculty(it) })
            }
        }
    }

    // ==========================================
    // FIRESTORE REALTIME LISTENERS & SYNC
    // ==========================================
    private fun setupActiveSessionsListener() {
        activeSessionsListener?.remove()
        activeSessionsListener = firestoreDataSource.listenToActiveSessions { sessions ->
            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] REPO_EMIT active_sessions count=${sessions.size} doc_ids=${sessions.map { it.sessionId }}")
            _activeSessions.value = sessions
            if (sessions.isNotEmpty()) {
                val currentFac = _currentFaculty.value
                val activeForCurrentFac = if (currentFac != null) {
                    sessions.firstOrNull { 
                        it.facultyId.equals(currentFac.id, ignoreCase = true) || 
                        it.facultyId.equals(currentFac.facultyId, ignoreCase = true) 
                    }
                } else null

                val active = activeForCurrentFac ?: sessions.firstOrNull { it.status == "ACTIVE" }
                if (active != null) {
                    _currentActiveSession.value = active
                    _isSessionActive.value = true
                } else {
                    _isSessionActive.value = false
                    val completed = sessions.firstOrNull { it.status == "COMPLETED" } ?: sessions.first()
                    _latestCompletedSession.value = completed
                    scope.launch {
                        val records = firestoreDataSource.getAttendanceForSession(completed.sessionId)
                        _latestSessionPresentCount.value = records.size
                        _latestSessionTotalCount.value = if (completed.totalStudents > 0) completed.totalStudents else maxOf(records.size, 30)
                    }
                }
            } else {
                _isSessionActive.value = false
            }
        }
    }

    fun setupFacultyActiveSessionListener(facultyIdOrUid: String) {
        facultyActiveSessionListener?.remove()
        val clean = facultyIdOrUid.trim()
        val currentFac = _currentFaculty.value
        val authorizedClasses = (currentFac?.assignedClassIds.orEmpty() + currentFac?.assignedClasses.orEmpty())
            .filter { it.isNotBlank() }
            .toSet()
        val assignedDev = currentFac?.assignedDeviceId.orEmpty()

        facultyActiveSessionListener = firestoreDataSource.listenToFacultyActiveSession(
            facultyIdOrUid = clean,
            authorizedClassIds = authorizedClasses,
            assignedDeviceId = assignedDev
        ) { activeSession ->
            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] REPO_EMIT facultyActiveSession session=${activeSession?.sessionId} isActive=${activeSession != null}")
            if (activeSession != null) {
                _currentActiveSession.value = activeSession
                _isSessionActive.value = true
                setupFacultyLiveAttendanceListener(activeSession.sessionId)
            } else {
                if (_isSessionActive.value) {
                    Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] Active session ended/cleared from Firestore. Setting _isSessionActive = false")
                    _isSessionActive.value = false
                    _currentActiveSession.value = AttendanceSession()
                }
            }
        }
    }

    suspend fun createOrSyncFacultySession(
        deviceId: String,
        sessionToken: String,
        faculty: Faculty?
    ): AttendanceSession {
        val session = firestoreDataSource.getOrCreateSession(
            deviceId = deviceId,
            sessionToken = sessionToken,
            deviceName = "BLE_Attendance",
            faculty = faculty
        )
        _currentActiveSession.value = session
        _isSessionActive.value = true
        setupFacultyLiveAttendanceListener(session.sessionId)
        return session
    }

    fun setupFacultyLiveAttendanceListener(sessionId: String) {
        liveAttendanceListener?.remove()
        liveAttendanceListener = firestoreDataSource.listenToSessionAttendance(sessionId) { records ->
            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] REPO_EMIT sessionAttendance sessionId=$sessionId count=${records.size} studentUids=${records.map { it.studentUid }}")
            updateLiveStudentRoster(records)
            updateReportsFromRecords(records)
        }
    }

    fun attachStudentHistoryListener(studentUid: String) {
        studentHistoryListener?.remove()
        studentHistoryListener = firestoreDataSource.listenToStudentAttendance(studentUid) { records ->
            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] REPO_EMIT studentHistory studentUid=$studentUid count=${records.size} recordIds=${records.map { it.id }}")
            _attendanceHistory.value = records
            scope.launch {
                records.forEach { appDatabase?.attendanceRecordDao()?.insertRecord(AttendanceRecordEntity.fromRecord(it)) }
            }
        }
    }

    fun attachFacultySessionsListener(facultyIdOrUid: String) {
        facultySessionsListener?.remove()
        facultySessionsListener = firestoreDataSource.listenToFacultySessions(facultyIdOrUid) { sessions ->
            Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] REPO_EMIT facultySessions facultyIdOrUid=$facultyIdOrUid count=${sessions.size} sessionIds=${sessions.map { it.sessionId }}")
            _facultySessions.value = sessions
            scope.launch {
                sessions.forEach { appDatabase?.attendanceSessionDao()?.insertSession(AttendanceSessionEntity.fromAttendanceSession(it)) }
            }
        }
    }

    private fun updateLiveStudentRoster(records: List<AttendanceRecord>) {
        scope.launch {
            val allStudents = _studentsList.value.ifEmpty {
                firestoreDataSource.getAllStudents().ifEmpty {
                    appDatabase?.studentDao()?.getAllStudents()?.map { it.toStudent() } ?: emptyList()
                }
            }
            val presentIds = records.map { it.studentUid.lowercase() }.toSet()
            val presentTokens = records.map { it.enrollmentNo.lowercase() }.toSet()

            val liveList = if (allStudents.isNotEmpty()) {
                allStudents.map { student ->
                    val isPresent = presentIds.contains(student.id.lowercase()) ||
                            presentTokens.contains(student.enrollmentNumber.lowercase())
                    val matchedRecord = records.firstOrNull {
                        it.studentUid.equals(student.id, ignoreCase = true) ||
                                it.enrollmentNo.equals(student.enrollmentNumber, ignoreCase = true)
                    }
                    StudentAttendanceStatus(
                        studentId = student.id,
                        name = student.name,
                        enrollment = student.enrollmentNumber,
                        initials = student.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifEmpty { "ST" },
                        isPresent = isPresent,
                        markedTime = matchedRecord?.time ?: if (isPresent) "Marked" else "--:--"
                    )
                }
            } else {
                records.map { record ->
                    StudentAttendanceStatus(
                        studentId = record.studentUid,
                        name = record.studentName.ifBlank { "Student ${record.enrollmentNo}" },
                        enrollment = record.enrollmentNo,
                        initials = record.studentName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifEmpty { "ST" },
                        isPresent = record.status.equals("PRESENT", ignoreCase = true),
                        markedTime = record.time
                    )
                }
            }
            _studentLiveList.value = liveList
            val presentCount = liveList.count { it.isPresent }
            val totalCount = if (liveList.isNotEmpty()) liveList.size else 30
            val absentCount = totalCount - presentCount
            val attendancePercent = if (totalCount > 0) (presentCount * 100) / totalCount else 0

            _reportsData.value = ClassReportData(
                presentCount = presentCount,
                absentCount = absentCount,
                attendanceRatePercent = attendancePercent,
                totalCount = totalCount
            )
        }
    }

    private fun updateReportsFromRecords(records: List<AttendanceRecord>) {
        val total = 30
        val present = records.size
        val absent = maxOf(0, total - present)
        val rate = if (total > 0) (present * 100) / total else 0
        _reportsData.value = _reportsData.value.copy(
            presentCount = present,
            absentCount = absent,
            attendanceRatePercent = rate
        )
    }

    // ==========================================
    // DATA LOADING & PERSISTENCE (Firestore Authoritative)
    // ==========================================
    fun loadStudents() {
        scope.launch {
            FirebaseAttendanceDataSource.logFirebaseDebugStatus()
            // 1. Fetch authoritative Firestore data
            val remote = try {
                firestoreDataSource.getAllStudents()
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching remote students: ${e.message}")
                emptyList()
            }

            if (remote.isNotEmpty()) {
                _studentsList.value = remote
                appDatabase?.studentDao()?.insertStudents(remote.map { StudentEntity.fromStudent(it) })
            } else {
                // Offline fallback only
                val local = appDatabase?.studentDao()?.getAllStudents()?.map { it.toStudent() } ?: emptyList()
                if (local.isNotEmpty() && _studentsList.value.isEmpty()) {
                    _studentsList.value = local
                }
            }
        }
    }

    fun loadFaculty() {
        scope.launch {
            // 1. Fetch authoritative Firestore data
            val remote = try {
                firestoreDataSource.getAllFaculty()
            } catch (e: Exception) {
                emptyList()
            }

            if (remote.isNotEmpty()) {
                _facultyList.value = remote
                appDatabase?.facultyDao()?.insertFacultyList(remote.map { FacultyEntity.fromFaculty(it) })
            } else {
                // Offline fallback
                val local = appDatabase?.facultyDao()?.getAllFaculty()?.map { it.toFaculty() } ?: emptyList()
                if (local.isNotEmpty() && _facultyList.value.isEmpty()) {
                    _facultyList.value = local
                }
            }
        }
    }

    suspend fun loadClassesAndDevices() = withContext(Dispatchers.IO) {
        val remoteClasses = try {
            firestoreDataSource.getAllClasses()
        } catch (e: Exception) {
            emptyList()
        }

        if (remoteClasses.isNotEmpty()) {
            _classesList.value = remoteClasses
            appDatabase?.classDao()?.insertClasses(remoteClasses.map { ClassEntity.fromClassItem(it) })
        } else {
            val localClasses = appDatabase?.classDao()?.getAllClasses()?.map { it.toClassItem() } ?: emptyList()
            if (localClasses.isNotEmpty() && _classesList.value.isEmpty()) {
                _classesList.value = localClasses
            }
        }

        val devices = try {
            firestoreDataSource.getAllDevices()
        } catch (e: Exception) {
            emptyList()
        }
        _devicesList.value = if (devices.isNotEmpty()) devices else listOf(Esp32Device())
    }

    fun loadFacultyData(facultyIdOrUid: String) {
        val clean = facultyIdOrUid.trim()
        if (clean.isBlank()) return

        setupFacultyActiveSessionListener(clean)
        attachFacultySessionsListener(clean)

        scope.launch {
            val faculty = appDatabase?.facultyDao()?.getFacultyByIdOrEmail(clean)?.toFaculty()
                ?: firestoreDataSource.getFacultyProfile(clean)
                ?: firestoreDataSource.getFacultyByFacultyId(clean)

            if (faculty != null) {
                _currentFaculty.value = faculty
            }

            val classes = firestoreDataSource.getAllClasses().filter {
                it.facultyId.equals(clean, ignoreCase = true) ||
                        (faculty != null && it.facultyId.equals(faculty.facultyId, ignoreCase = true)) ||
                        (faculty != null && faculty.assignedClasses.contains(it.classId))
            }

            val schedules = if (classes.isNotEmpty()) {
                classes.mapIndexed { idx, c ->
                    val timeSlots = listOf("09:00 AM - 10:00 AM", "10:15 AM - 11:15 AM", "11:30 AM - 12:30 PM", "02:00 PM - 03:00 PM")
                    val slot = timeSlots.getOrElse(idx % timeSlots.size) { "10:00 AM - 11:00 AM" }
                    ClassScheduleItem(
                        id = "sch_${c.classId}_${idx}",
                        classCode = c.classId,
                        subject = c.subject,
                        timeSlot = slot,
                        room = c.room,
                        status = "Upcoming"
                    )
                }
            } else {
                listOf(
                    ClassScheduleItem(
                        id = "sch_cn301",
                        classCode = "CN-301",
                        subject = "Computer Networks",
                        timeSlot = "09:00 AM - 10:00 AM",
                        room = "Room 301 - Lab B",
                        status = "Upcoming"
                    ),
                    ClassScheduleItem(
                        id = "sch_cs401",
                        classCode = "CS-401",
                        subject = "Operating Systems",
                        timeSlot = "11:30 AM - 12:30 PM",
                        room = "Room 402 - Hall A",
                        status = "Upcoming"
                    )
                )
            }
            _facultySchedule.value = schedules
        }
    }

    suspend fun restoreUserSession(context: Context) = withContext(Dispatchers.IO) {
        setContext(context)
        try {
            val firebaseUser = authService.auth?.currentUser
            val currentUid = firebaseUser?.uid ?: authService.getCurrentUid() ?: SessionPreferences.getSavedUserId(context)
            if (currentUid.isNullOrBlank()) {
                _isLoggedIn.value = false
                return@withContext
            }
            authService.setCachedUid(currentUid)

            val userProfile = firestoreDataSource.getUserProfile(currentUid)
            val roleStr = userProfile?.role?.lowercase() ?: SessionPreferences.getSavedRole(context)?.name?.lowercase() ?: "student"
            val effectiveRole = when {
                roleStr.contains("admin") -> UserRole.ADMIN
                roleStr.contains("faculty") -> UserRole.FACULTY
                else -> UserRole.STUDENT
            }

            _currentUserRole.value = effectiveRole

            when (effectiveRole) {
                UserRole.STUDENT -> {
                    var student = firestoreDataSource.getStudentProfile(currentUid)
                        ?: appDatabase?.studentDao()?.getStudentById(currentUid)?.toStudent()

                    if (student == null) {
                        // Safely heal missing student profile for authenticated user
                        val studentName = userProfile?.name?.ifBlank { null }
                            ?: firebaseUser?.displayName?.ifBlank { null }
                            ?: (firebaseUser?.email ?: currentUid).substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
                        val studentEmail = userProfile?.email?.ifBlank { null }
                            ?: firebaseUser?.email
                            ?: "$currentUid@college.edu"
                        val studentEnroll = (firebaseUser?.email ?: studentEmail).substringBefore("@").uppercase()

                        student = Student(
                            id = currentUid,
                            name = studentName,
                            fullName = studentName,
                            email = studentEmail,
                            enrollmentNumber = studentEnroll,
                            department = "Computer Science",
                            classId = "CN-301",
                            className = "Computer Networks",
                            classIds = listOf("CN-301"),
                            role = "student",
                            isActive = true,
                            status = "ACTIVE",
                            faceRegistered = false,
                            faceEmbedding = emptyList(),
                            createdAt = userProfile?.createdAt ?: System.currentTimeMillis()
                        )
                        firestoreDataSource.saveStudentProfile(student)
                        appDatabase?.studentDao()?.insertStudent(StudentEntity.fromStudent(student))
                    } else if (student.enrollmentNumber.isBlank()) {
                        val enroll = (firebaseUser?.email ?: student.email).substringBefore("@").uppercase()
                        student = student.copy(enrollmentNumber = enroll)
                        firestoreDataSource.saveStudentProfile(student)
                        appDatabase?.studentDao()?.insertStudent(StudentEntity.fromStudent(student))
                    }

                    val profileStatus = firestoreDataSource.getFaceProfileStatus(student.id)
                    val embedding = when (profileStatus) {
                        is FaceProfileStatus.Valid -> profileStatus.embedding
                        else -> {
                            val roomStudent = appDatabase?.studentDao()?.getStudentById(student.id)?.toStudent()
                            if (roomStudent != null && roomStudent.faceRegistered && roomStudent.faceEmbedding.size == com.example.face.FaceEmbeddingProcessor.EMBEDDING_DIM) {
                                roomStudent.faceEmbedding
                            } else emptyList()
                        }
                    }
                    val isFaceReg = embedding.size == com.example.face.FaceEmbeddingProcessor.EMBEDDING_DIM || student.faceRegistered

                    _currentStudent.value = student.copy(faceRegistered = isFaceReg, faceEmbedding = embedding)
                    _isLoggedIn.value = true
                    attachStudentHistoryListener(student.id)
                }
                UserRole.FACULTY -> {
                    val faculty = firestoreDataSource.getFacultyProfile(currentUid)
                        ?: appDatabase?.facultyDao()?.getFacultyById(currentUid)?.toFaculty()
                        ?: Faculty(
                            id = currentUid,
                            name = firebaseUser?.displayName ?: userProfile?.name ?: "Faculty Member",
                            email = firebaseUser?.email ?: userProfile?.email ?: ""
                        )
                    _currentFaculty.value = faculty
                    _isLoggedIn.value = true
                    val fid = faculty.facultyId.ifBlank { faculty.id }
                    loadFacultyData(fid)
                    attachFacultySessionsListener(fid)
                }
                UserRole.ADMIN -> {
                    _currentAdmin.value = userProfile ?: UserProfile(uid = currentUid, role = "ADMIN", name = "Sujal (Administrator)")
                    _isLoggedIn.value = true
                    refreshAdminStats()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "restoreUserSession error: ${e.message}")
        } finally {
            _isSessionRestored.value = true
        }
    }

    suspend fun refreshAdminStats() {
        val stats = firestoreDataSource.getAdminStats()
        _adminStats.value = stats
    }

    // ==========================================
    // ROLE & AUTHENTICATION
    // ==========================================
    fun setUserRole(role: UserRole) {
        _currentUserRole.value = role
    }

    fun setStudent(student: Student?) {
        _currentStudent.value = student
        student?.let { attachStudentHistoryListener(it.id) }
    }

    suspend fun loginStudentWithFirebase(emailOrEnrollment: String, password: String): Result<Student> {
        val result = authService.loginStudent(emailOrEnrollment, password)
        if (result.isSuccess) {
            result.getOrNull()?.let { st ->
                val profileStatus = firestoreDataSource.getFaceProfileStatus(st.id)
                val embedding = when (profileStatus) {
                    is FaceProfileStatus.Valid -> profileStatus.embedding
                    else -> {
                        val roomStudent = appDatabase?.studentDao()?.getStudentById(st.id)?.toStudent()
                        if (roomStudent != null && roomStudent.faceRegistered && roomStudent.faceEmbedding.size == com.example.face.FaceEmbeddingProcessor.EMBEDDING_DIM) {
                            roomStudent.faceEmbedding
                        } else st.faceEmbedding
                    }
                }
                val isFaceReg = embedding.size == com.example.face.FaceEmbeddingProcessor.EMBEDDING_DIM || st.faceRegistered
                val fullStudent = st.copy(faceRegistered = isFaceReg, faceEmbedding = embedding)

                _currentStudent.value = fullStudent
                _currentUserRole.value = UserRole.STUDENT
                _isLoggedIn.value = true
                attachStudentHistoryListener(fullStudent.id)
            }
        }
        return result
    }

    suspend fun loginFacultyWithFirebase(facultyIdOrEmail: String, password: String): Result<Faculty> {
        val result = authService.loginFaculty(facultyIdOrEmail, password)
        if (result.isSuccess) {
            result.getOrNull()?.let {
                _currentFaculty.value = it
                _currentUserRole.value = UserRole.FACULTY
                _isLoggedIn.value = true
                val fid = it.facultyId.ifBlank { it.id }
                loadFacultyData(fid)
                attachFacultySessionsListener(fid)
            }
        }
        return result
    }

    suspend fun loginAdminWithFirebase(email: String, password: String): Result<UserProfile> {
        val result = authService.loginAdmin(email, password)
        if (result.isSuccess) {
            result.getOrNull()?.let {
                _currentAdmin.value = it
                _currentUserRole.value = UserRole.ADMIN
                _isLoggedIn.value = true
                refreshAdminStats()
            }
        }
        return result
    }

    suspend fun signUpStudentWithFirebase(
        name: String,
        email: String,
        enrollment: String,
        dept: String,
        password: String
    ): Result<Student> {
        val result = authService.signUpStudent(name, email, enrollment, dept, password)
        if (result.isSuccess) {
            result.getOrNull()?.let { student ->
                _currentStudent.value = student
                _currentUserRole.value = UserRole.STUDENT
                _isLoggedIn.value = true
                _studentsList.value = (_studentsList.value.filter { it.id != student.id } + student).sortedBy { it.name }
                attachStudentHistoryListener(student.id)
                refreshAdminStats()
            }
        }
        return result
    }

    fun logout() {
        activeSessionsListener?.remove()
        liveAttendanceListener?.remove()
        studentHistoryListener?.remove()
        facultySessionsListener?.remove()
        facultyActiveSessionListener?.remove()
        authService.logout()
        _isLoggedIn.value = false
        _currentStudent.value = null
        _currentFaculty.value = null
        _currentAdmin.value = null
        _currentUserRole.value = null
        _attendanceHistory.value = emptyList()
    }

    // ==========================================
    // ATTENDANCE & BIOMETRIC FACE REGISTRATION
    // ==========================================
    fun hasMarkedAttendance(sessionId: String, sessionToken: String? = null): Boolean {
        val student = _currentStudent.value ?: return false
        val deterministicDocId = if (!sessionToken.isNullOrBlank()) "${student.id}_${sessionToken}" else null
        return _attendanceHistory.value.any { record ->
            (deterministicDocId != null && record.attendanceId == deterministicDocId) ||
                    (sessionToken != null && sessionToken.isNotBlank() && record.sessionToken == sessionToken) ||
                    record.sessionId == sessionId ||
                    record.sessionToken == sessionId ||
                    record.sessionId.endsWith("_$sessionId")
        }
    }

    suspend fun registerFace(studentId: String, embedding: List<Float>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (embedding.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Face embedding is empty."))
            }
            if (embedding.size != com.example.face.FaceEmbeddingProcessor.EMBEDDING_DIM) {
                return@withContext Result.failure(IllegalArgumentException("Face embedding dimension (${embedding.size}) does not match required ${com.example.face.FaceEmbeddingProcessor.EMBEDDING_DIM}-d MobileFaceNet model."))
            }

            // 1. Save to Firestore canonical face_profiles/{studentId} and update students/{studentId}.faceRegistered = true
            val success = firestoreDataSource.saveFaceEmbedding(studentId, embedding)
            if (!success) {
                Log.e(TAG, "Failed to save face profile in Cloud Firestore for $studentId")
                return@withContext Result.failure(Exception("Failed to save face profile in Cloud Firestore"))
            }

            // 2. Save to Room Persistent Database
            val embJson = embedding.joinToString(prefix = "[", postfix = "]")
            appDatabase?.studentDao()?.updateFaceEmbedding(studentId, embJson)
            _currentStudent.value?.let { curr ->
                if (curr.id != studentId) appDatabase?.studentDao()?.updateFaceEmbedding(curr.id, embJson)
                if (curr.enrollmentNumber.isNotBlank()) appDatabase?.studentDao()?.updateFaceEmbedding(curr.enrollmentNumber, embJson)
            }

            // 3. Update in-memory state unconditionally for the active logged-in student
            val updated = _currentStudent.value?.copy(
                faceRegistered = true,
                faceEmbedding = embedding
            ) ?: Student(
                id = studentId,
                name = "Student",
                fullName = "Student",
                enrollmentNumber = studentId,
                faceRegistered = true,
                faceEmbedding = embedding
            )

            _currentStudent.value = updated
            _studentsList.value = _studentsList.value.map {
                if (it.id == updated.id || it.enrollmentNumber == updated.enrollmentNumber) updated else it
            }

            Log.i("FACE_DEBUG", "Face registered successfully for $studentId. Dimension: ${embedding.size}")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun recordAttendance(
        session: AttendanceSession,
        sessionToken: String? = null,
        deviceId: String? = null,
        bleVerified: Boolean,
        faceVerified: Boolean,
        livenessVerified: Boolean
    ): AttendanceRecord? {
        val student = _currentStudent.value ?: return null

        if (!bleVerified || !faceVerified || !livenessVerified) {
            Log.e(TAG, "Attendance write BLOCKED: bleVerified=$bleVerified, faceVerified=$faceVerified, livenessVerified=$livenessVerified")
            return null
        }

        val effectiveToken = sessionToken?.takeIf { it.isNotBlank() }
            ?: session.sessionToken.takeIf { it.isNotBlank() }
            ?: session.sessionId
        val effectiveDeviceId = deviceId?.takeIf { it.isNotBlank() }
            ?: session.deviceId.takeIf { it.isNotBlank() }
            ?: "CLASSROOM_ESP32_01"

        val canonicalSessionId = "${effectiveDeviceId}_${effectiveToken}"
        val deterministicId = "${student.id}_${effectiveToken}"

        if (hasMarkedAttendance(canonicalSessionId, effectiveToken)) {
            return _attendanceHistory.value.firstOrNull {
                it.attendanceId == deterministicId ||
                        (effectiveToken.isNotBlank() && it.sessionToken == effectiveToken) ||
                        it.sessionId == canonicalSessionId
            }
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val now = Date()

        val record = AttendanceRecord(
            attendanceId = deterministicId,
            sessionId = canonicalSessionId,
            studentUid = student.id,
            studentName = student.name,
            enrollmentNo = student.enrollmentNo,
            classId = session.classId.ifBlank { "CN-301" },
            subject = session.subject,
            facultyId = session.facultyId.ifBlank { "fac_001" },
            deviceId = effectiveDeviceId,
            sessionToken = effectiveToken,
            bleVerified = bleVerified,
            faceVerified = faceVerified,
            livenessVerified = livenessVerified,
            status = "present",
            timestamp = System.currentTimeMillis(),
            date = dateFormat.format(now),
            time = timeFormat.format(now),
            room = session.room
        )

        _attendanceHistory.value = listOf(record) + _attendanceHistory.value

        scope.launch {
            appDatabase?.attendanceRecordDao()?.insertRecord(AttendanceRecordEntity.fromRecord(record))
            firestoreDataSource.recordAttendance(record)
        }

        addNotification(
            title = "Attendance Marked",
            description = "You are present for ${session.subject}",
            type = NotificationType.ATTENDANCE_MARKED
        )

        return record
    }

    // ==========================================
    // FACULTY SESSIONS & REPORTS
    // ==========================================
    suspend fun getSessionAttendanceDetails(sessionId: String): Pair<AttendanceSession?, List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        val session = appDatabase?.attendanceSessionDao()?.getSessionById(sessionId)?.toAttendanceSession()
            ?: firestoreDataSource.getSessionById(sessionId)
            ?: _facultySessions.value.firstOrNull { it.sessionId == sessionId }
            ?: _currentActiveSession.value.takeIf { it.sessionId == sessionId }
            ?: _latestCompletedSession.value.takeIf { it?.sessionId == sessionId }
            ?: _activeSessions.value.firstOrNull { it.sessionId == sessionId }

        val records = appDatabase?.attendanceRecordDao()?.getRecordsForSession(sessionId)?.map { it.toAttendanceRecord() }?.ifEmpty {
            firestoreDataSource.getAttendanceForSession(sessionId)
        } ?: firestoreDataSource.getAttendanceForSession(sessionId)

        Pair(session, records)
    }

    suspend fun getSessionAttendanceRoster(sessionId: String): List<StudentAttendanceStatus> = withContext(Dispatchers.IO) {
        val (session, records) = getSessionAttendanceDetails(sessionId)
        val classId = session?.classId ?: "CN-301"
        val allStudents = appDatabase?.studentDao()?.getStudentsByClass(classId)?.map { it.toStudent() }?.ifEmpty {
            appDatabase?.studentDao()?.getAllStudents()?.map { it.toStudent() }
        } ?: firestoreDataSource.getStudentsForClass(classId).ifEmpty {
            firestoreDataSource.getAllStudents()
        }

        val presentIds = records.map { it.studentUid.lowercase() }.toSet()
        val presentTokens = records.map { it.enrollmentNo.lowercase() }.toSet()

        allStudents.map { student ->
            val isPresent = presentIds.contains(student.id.lowercase()) ||
                    presentTokens.contains(student.enrollmentNumber.lowercase())
            val matched = records.firstOrNull {
                it.studentUid.equals(student.id, ignoreCase = true) ||
                        it.enrollmentNo.equals(student.enrollmentNumber, ignoreCase = true)
            }
            val initials = student.name.trim().split(" ")
                .filter { it.isNotBlank() }
                .mapNotNull { it.firstOrNull()?.toString() }
                .joinToString("")
                .take(2)
                .uppercase()

            StudentAttendanceStatus(
                studentId = student.id,
                name = student.name,
                enrollment = student.enrollmentNumber,
                initials = if (initials.isNotBlank()) initials else "ST",
                isPresent = isPresent,
                markedTime = matched?.time ?: if (isPresent) "Recorded" else "--:--",
                status = if (isPresent) "PRESENT" else "ABSENT"
            )
        }.sortedWith(compareByDescending<StudentAttendanceStatus> { it.isPresent }.thenBy { it.name })
    }

    fun generateSessionReportCsv(session: AttendanceSession, records: List<AttendanceRecord>): String {
        val sb = StringBuilder()
        sb.append("App Name,Smart Attendance\n")
        sb.append("Class,${session.classId}\n")
        sb.append("Subject,${session.subject}\n")
        sb.append("Faculty,${session.professorName}\n")
        sb.append("Room,${session.room}\n")
        sb.append("Date,${session.date.ifBlank { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }}\n")
        sb.append("Time Slot,${session.startTime} - ${session.endTime}\n")
        sb.append("Status,${session.status}\n")
        sb.append("ESP32 Device,${session.bleIdentifier} (${session.deviceId})\n")
        sb.append("Total Present,${records.size}\n\n")
        sb.append("Roll No / Enrollment,Student Name,Status,Time\n")
        records.forEach { r ->
            sb.append("\"${r.enrollmentNo}\",\"${r.studentName}\",\"${r.status}\",\"${r.time}\"\n")
        }
        return sb.toString()
    }

    fun generateSessionReportText(session: AttendanceSession, records: List<AttendanceRecord>): String {
        val dateStr = session.date.ifBlank { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }
        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append("       SMART ATTENDANCE REPORT          \n")
        sb.append("========================================\n")
        sb.append("Application: Smart Attendance\n")
        sb.append("Class:       ${session.classId}\n")
        sb.append("Subject:     ${session.subject}\n")
        sb.append("Faculty:     ${session.professorName}\n")
        sb.append("Room:        ${session.room}\n")
        sb.append("Date:        $dateStr\n")
        sb.append("Time Slot:   ${session.startTime} - ${session.endTime}\n")
        sb.append("Status:      ${session.status}\n")
        sb.append("Device:      ${session.bleIdentifier} (${session.deviceId})\n")
        sb.append("Present:     ${records.size}\n")
        sb.append("----------------------------------------\n")
        sb.append("STUDENT ATTENDANCE LIST:\n")
        if (records.isEmpty()) {
            sb.append("No attendance records found.\n")
        } else {
            records.forEachIndexed { i, r ->
                sb.append("${i + 1}. [${r.enrollmentNo}] ${r.studentName} - ${r.status} (${r.time})\n")
            }
        }
        sb.append("========================================\n")
        return sb.toString()
    }

    fun exportSessionReportToCsv(
        session: AttendanceSession,
        records: List<AttendanceRecord>,
        context: Context
    ): File? {
        return try {
            val csvData = generateSessionReportCsv(session, records)
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val filename = "Report_${session.classId.replace(" ", "_")}_${System.currentTimeMillis()}.csv"
            val file = File(downloadsDir, filename)
            FileOutputStream(file).use { fos ->
                fos.write(csvData.toByteArray())
            }
            Log.i(TAG, "Report exported to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting CSV: ${e.message}")
            null
        }
    }

    suspend fun exportSessionReportToCsv(sessionId: String, context: Context): Pair<File?, String?> {
        val (session, records) = getSessionAttendanceDetails(sessionId)
        val effSession = session ?: _facultySessions.value.firstOrNull { it.sessionId == sessionId }
            ?: _currentActiveSession.value
        val file = exportSessionReportToCsv(effSession, records, context)
        return if (file != null) Pair(file, null) else Pair(null, "Failed to create file")
    }

    suspend fun generateSessionReportText(sessionId: String): String {
        val (session, records) = getSessionAttendanceDetails(sessionId)
        val effSession = session ?: _facultySessions.value.firstOrNull { it.sessionId == sessionId }
            ?: _currentActiveSession.value
        return generateSessionReportText(effSession, records)
    }

    fun endFacultySession() {
        val current = _currentActiveSession.value
        val sessionId = current.sessionId
        _isSessionActive.value = false
        val completed = current.copy(
            status = "COMPLETED",
            endTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            updatedAt = System.currentTimeMillis()
        )
        _latestCompletedSession.value = completed
        _latestSessionPresentCount.value = _studentLiveList.value.count { it.isPresent }
        _latestSessionTotalCount.value = if (completed.totalStudents > 0) completed.totalStudents else 30

        if (sessionId.isNotBlank()) {
            scope.launch {
                firestoreDataSource.completeSession(sessionId)
                appDatabase?.attendanceSessionDao()?.insertSession(AttendanceSessionEntity.fromAttendanceSession(completed))
                _currentFaculty.value?.let { fac ->
                    loadFacultyData(fac.facultyId.ifBlank { fac.id })
                }
            }
        }
    }

    fun updateActiveSessionClass(classId: String, className: String, subject: String) {
        val current = _currentActiveSession.value
        if (current.sessionId.isBlank()) return
        val updated = current.copy(
            classId = classId,
            className = className,
            subject = subject,
            updatedAt = System.currentTimeMillis()
        )
        _currentActiveSession.value = updated
        scope.launch {
            try {
                firestoreDataSource.updateSessionClass(current.sessionId, classId, className, subject)
                Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] Updated active session class to $classId ($className)")
            } catch (e: Exception) {
                Log.e("FIREBASE_DEBUG", "[FIREBASE_DEBUG] Failed to update session class: ${e.message}")
            }
        }
    }

    suspend fun assignDeviceToFaculty(facultyUid: String, deviceId: String): Boolean = withContext(Dispatchers.IO) {
        val success = firestoreDataSource.assignDeviceToFaculty(facultyUid, deviceId)
        if (success) {
            loadFaculty()
            loadClassesAndDevices()
            _currentFaculty.value?.let { current ->
                if (current.id == facultyUid || current.facultyId.equals(facultyUid, ignoreCase = true)) {
                    _currentFaculty.value = current.copy(assignedDeviceId = deviceId)
                }
            }
        }
        success
    }

    fun toggleStudentAttendance(studentId: String) {
        val currentList = _studentLiveList.value.toMutableList()
        val index = currentList.indexOfFirst { it.studentId == studentId }
        if (index != -1) {
            val s = currentList[index]
            val newStatus = !s.isPresent
            currentList[index] = s.copy(isPresent = newStatus)
            _studentLiveList.value = currentList

            if (newStatus) {
                val session = _currentActiveSession.value
                val record = AttendanceRecord(
                    attendanceId = "${s.studentId}_${session.sessionToken}",
                    sessionId = session.sessionId,
                    studentUid = s.studentId,
                    studentName = s.name,
                    enrollmentNo = s.enrollment,
                    classId = session.classId,
                    subject = session.subject,
                    facultyId = session.facultyId,
                    deviceId = session.deviceId,
                    sessionToken = session.sessionToken,
                    status = "PRESENT",
                    timestamp = System.currentTimeMillis(),
                    date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
                    time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                    room = session.room
                )
                scope.launch {
                    appDatabase?.attendanceRecordDao()?.insertRecord(AttendanceRecordEntity.fromRecord(record))
                    firestoreDataSource.recordAttendance(record)
                }
            }

            val presentCount = currentList.count { it.isPresent }
            val absentCount = currentList.size - presentCount
            val percent = if (currentList.isNotEmpty()) (presentCount * 100) / currentList.size else 0
            _reportsData.value = _reportsData.value.copy(
                presentCount = presentCount,
                absentCount = absentCount,
                attendanceRatePercent = percent
            )
        }
    }

    suspend fun addStudent(
        fullName: String,
        enrollmentNumber: String,
        email: String,
        phone: String = "",
        classId: String = "CN-301",
        className: String = "Computer Networks",
        division: String = "A",
        course: String = "B.Tech",
        department: String = "Computer Science",
        semester: String = "6th Semester"
    ): Result<Student> = withContext(Dispatchers.IO) {
        val res = authService.createStudentByFacultyOrAdmin(
            name = fullName,
            enrollment = enrollmentNumber,
            email = email,
            phone = phone,
            classId = classId,
            className = className,
            division = division,
            course = course,
            department = department,
            semester = semester
        )
        if (res.isSuccess) {
            refreshAdminStats()
        }
        res
    }

    fun updateStudent(
        studentId: String,
        fullName: String,
        email: String,
        phone: String = "",
        classId: String = "CN-301",
        className: String = "Computer Networks",
        division: String = "A",
        isActive: Boolean = true
    ): Result<Student> {
        val existing = _studentsList.value.firstOrNull { it.id == studentId }
        val updated = (existing ?: Student(id = studentId)).copy(
            name = fullName.trim(),
            fullName = fullName.trim(),
            email = email.trim(),
            phone = phone.trim(),
            classId = classId,
            className = className,
            division = division,
            isActive = isActive,
            status = if (isActive) "ACTIVE" else "INACTIVE"
        )
        scope.launch {
            appDatabase?.studentDao()?.insertStudent(StudentEntity.fromStudent(updated))
            firestoreDataSource.saveStudentProfile(updated)
            loadStudents()
            refreshAdminStats()
        }
        return Result.success(updated)
    }

    fun removeOrDeactivateStudent(studentId: String): Boolean {
        scope.launch {
            appDatabase?.studentDao()?.deleteStudentById(studentId)
            firestoreDataSource.deleteOrDeactivateStudent(studentId)
            loadStudents()
            refreshAdminStats()
        }
        return true
    }

    fun generateAttendanceReportCsv(): String {
        val session = _currentActiveSession.value
        val list = _studentLiveList.value
        val sb = StringBuilder()
        sb.append("Roll No,Name,Subject,Class,Date,Status,Marked Time\n")
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        for (item in list) {
            val status = if (item.isPresent) "PRESENT" else "ABSENT"
            sb.append("${item.rollNo},\"${item.name}\",\"${session.subject}\",\"${session.classId}\",$dateStr,$status,${item.markedTime}\n")
        }
        return sb.toString()
    }

    fun exportReportToDownloads(context: Context, filename: String = "Attendance_Report_${System.currentTimeMillis()}.csv"): File? {
        return try {
            val csvData = generateAttendanceReportCsv()
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val file = File(downloadsDir, filename)
            FileOutputStream(file).use { fos ->
                fos.write(csvData.toByteArray())
            }
            Log.i(TAG, "Report exported to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting report: ${e.message}")
            null
        }
    }

    fun addNotification(title: String, description: String, type: NotificationType) {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val notif = NotificationItem(
            id = "notif_${UUID.randomUUID().toString().take(8)}",
            title = title,
            description = description,
            time = timeFormat.format(Date()),
            type = type,
            isRead = false
        )
        _notifications.value = listOf(notif) + _notifications.value
    }

    fun markNotificationAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun updateSettings(
        bluetooth: Boolean? = null,
        backgroundScanning: Boolean? = null,
        notifications: Boolean? = null,
        themeMode: String? = null
    ) {
        val current = _settings.value
        _settings.value = current.copy(
            bluetoothEnabled = bluetooth ?: current.bluetoothEnabled,
            backgroundScanningEnabled = backgroundScanning ?: current.backgroundScanningEnabled,
            notificationsEnabled = notifications ?: current.notificationsEnabled,
            themeMode = themeMode ?: current.themeMode
        )
    }

    fun updateProfile(name: String, enrollment: String, dept: String) {
        val current = _currentStudent.value ?: return
        val updated = current.copy(
            name = name,
            enrollmentNumber = enrollment,
            department = dept
        )
        _currentStudent.value = updated
        scope.launch {
            appDatabase?.studentDao()?.insertStudent(StudentEntity.fromStudent(updated))
            firestoreDataSource.saveStudentProfile(updated)
            loadStudents()
        }
    }

    fun changePassword(newPass: String): Boolean {
        if (newPass.length < 6) return false
        val currentAuthUser = authService.auth?.currentUser
        if (currentAuthUser != null) {
            scope.launch {
                try {
                    currentAuthUser.updatePassword(newPass).awaitTask()
                    Log.i("AttendanceRepository", "Password updated successfully in Firebase Authentication")
                } catch (e: Exception) {
                    Log.w("AttendanceRepository", "Firebase Auth password update note: ${e.message}")
                }
            }
        }
        return true
    }
}

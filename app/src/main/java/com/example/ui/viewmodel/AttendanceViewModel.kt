package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ble.BleScanState
import com.example.bluetooth.BleManager
import com.example.bluetooth.BleState
import com.example.data.AttendanceRepository
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
import com.example.model.Student
import com.example.model.StudentAttendanceStatus
import com.example.model.UserProfile
import com.example.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.bluetooth.BluetoothPermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AttendanceRepository(context = application.applicationContext)

    val bleManager = BleManager(application.applicationContext)
    val bleState: StateFlow<BleState> = bleManager.state
    val discoveredDevices = bleManager.discoveredDevices

    fun connectToDiscoveredDevice(device: android.bluetooth.BluetoothDevice) {
        bleManager.connectManually(device)
    }

    // Backwards-compatible scanState mapped from real BleState
    val scanState: StateFlow<BleScanState> = bleManager.state.map { state ->
        when (state) {
            is BleState.Idle -> BleScanState.Idle
            is BleState.CheckingPermissions -> BleScanState.Scanning
            is BleState.Scanning -> BleScanState.Scanning
            is BleState.DeviceFound -> BleScanState.BeaconDetected(
                AttendanceSession(
                    sessionId = "sess_esp32",
                    subject = "Classroom Attendance",
                    room = state.deviceName,
                    startTime = "",
                    endTime = "",
                    bleIdentifier = state.deviceName
                ),
                state.rssi,
                1.5
            )
            is BleState.Connecting -> BleScanState.VerifyingProximity(
                AttendanceSession(
                    sessionId = "sess_esp32",
                    subject = "Classroom Attendance",
                    room = state.deviceName,
                    startTime = "",
                    endTime = "",
                    bleIdentifier = state.deviceName
                ),
                -65,
                0.4f
            )
            is BleState.Connected, is BleState.ReadingStatus -> BleScanState.VerifyingProximity(
                AttendanceSession(
                    sessionId = "sess_esp32",
                    subject = "Classroom Attendance",
                    room = "BLE_Attendance",
                    startTime = "",
                    endTime = "",
                    bleIdentifier = "BLE_Attendance"
                ),
                -65,
                0.8f
            )
            is BleState.ActiveSession -> BleScanState.BeaconDetected(
                AttendanceSession(
                    sessionId = "sess_${state.deviceId.lowercase()}",
                    subject = "Classroom Attendance",
                    room = state.deviceId,
                    startTime = "",
                    endTime = "",
                    bleIdentifier = state.deviceName,
                    status = state.status,
                    sessionToken = state.sessionToken,
                    deviceId = state.deviceId
                ),
                state.rssi,
                1.0
            )
            is BleState.InactiveSession -> BleScanState.Error("Attendance Session Not Active")
            is BleState.AttendanceMarked -> BleScanState.AttendanceMarked(
                session = AttendanceSession(
                    sessionId = "sess_${state.record.deviceId.lowercase()}",
                    subject = "Classroom Attendance",
                    room = state.record.deviceId,
                    startTime = "",
                    endTime = "",
                    bleIdentifier = "BLE_Attendance",
                    status = "ACTIVE",
                    sessionToken = state.record.sessionToken,
                    deviceId = state.record.deviceId
                ),
                record = state.record
            )
            is BleState.AlreadyMarked -> BleScanState.AlreadyMarked(
                session = AttendanceSession(
                    sessionId = "sess_classroom",
                    subject = "Classroom Attendance",
                    room = "CLASSROOM_ESP32_01",
                    startTime = "",
                    endTime = "",
                    bleIdentifier = "BLE_Attendance"
                ),
                record = null
            )
            is BleState.DeviceNotFound -> BleScanState.Error(state.message)
            is BleState.BluetoothDisabled -> BleScanState.Error(state.message)
            is BleState.PermissionRequired -> BleScanState.Error("Permissions required")
            is BleState.Disconnected -> BleScanState.Idle
            is BleState.Error -> BleScanState.Error(state.message)
            is BleState.FirebaseError -> BleScanState.Error(state.message)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, BleScanState.Idle)

    val student: StateFlow<Student?> = repository.currentStudent
    val faculty: StateFlow<Faculty?> = repository.currentFaculty
    val admin: StateFlow<UserProfile?> = repository.currentAdmin
    val currentRole: StateFlow<UserRole?> = repository.currentUserRole
    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
    val isSessionRestored: StateFlow<Boolean> = repository.isSessionRestored
    val isSessionActive: StateFlow<Boolean> = repository.isSessionActive
    val currentActiveSession: StateFlow<AttendanceSession> = repository.currentActiveSession
    val activeSessions: StateFlow<List<AttendanceSession>> = repository.activeSessions
    val facultySchedule: StateFlow<List<ClassScheduleItem>> = repository.facultySchedule
    val facultySessions: StateFlow<List<AttendanceSession>> = repository.facultySessions
    val classesList: StateFlow<List<ClassItem>> = repository.classesList
    val devicesList: StateFlow<List<Esp32Device>> = repository.devicesList
    val studentLiveList: StateFlow<List<StudentAttendanceStatus>> = repository.studentLiveList
    val reportsData: StateFlow<ClassReportData> = repository.reportsData
    val adminStats: StateFlow<AdminStats> = repository.adminStats
    val settings: StateFlow<AppSettings> = repository.settings
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications

    val latestCompletedSession: StateFlow<AttendanceSession?> = repository.latestCompletedSession
    val latestSessionPresentCount: StateFlow<Int> = repository.latestSessionPresentCount
    val latestSessionTotalCount: StateFlow<Int> = repository.latestSessionTotalCount
    val studentsList: StateFlow<List<Student>> = repository.studentsList
    val facultyList: StateFlow<List<Faculty>> = repository.facultyList

    suspend fun getSessionAttendanceDetails(sessionId: String): Pair<AttendanceSession?, List<AttendanceRecord>> {
        return repository.getSessionAttendanceDetails(sessionId)
    }

    suspend fun getSessionAttendanceRoster(sessionId: String): List<StudentAttendanceStatus> {
        return repository.getSessionAttendanceRoster(sessionId)
    }

    fun loadStudents() {
        repository.loadStudents()
    }

    fun loadFaculty() {
        repository.loadFaculty()
    }

    fun loadFacultyData(facultyIdOrUid: String) {
        repository.loadFacultyData(facultyIdOrUid)
    }

    fun refreshAdminStats() {
        viewModelScope.launch {
            repository.refreshAdminStats()
        }
    }

    fun loadClassesAndDevices() {
        viewModelScope.launch {
            repository.loadClassesAndDevices()
        }
    }

    fun addStudent(
        fullName: String,
        enrollmentNumber: String,
        email: String,
        phone: String = "",
        classId: String = "CN-301",
        className: String = "Computer Networks",
        division: String = "A",
        course: String = "B.Tech",
        department: String = "Computer Science",
        semester: String = "6th Semester",
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.addStudent(
                fullName = fullName,
                enrollmentNumber = enrollmentNumber,
                email = email,
                phone = phone,
                classId = classId,
                className = className,
                division = division,
                course = course,
                department = department,
                semester = semester
            )
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to add student to Firebase")
            }
        }
    }

    fun updateStudent(
        studentId: String,
        fullName: String,
        email: String,
        phone: String = "",
        classId: String = "CN-301",
        className: String = "Computer Networks",
        division: String = "A",
        isActive: Boolean = true,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateStudent(
                studentId = studentId,
                fullName = fullName,
                email = email,
                phone = phone,
                classId = classId,
                className = className,
                division = division,
                isActive = isActive
            )
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to update student")
            }
        }
    }

    fun removeStudent(
        studentId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.removeOrDeactivateStudent(studentId)
            if (success) {
                onResult(true, null)
            } else {
                onResult(false, "Failed to remove student")
            }
        }
    }

    fun restoreUserSession(context: Context) {
        viewModelScope.launch {
            repository.restoreUserSession(context)
        }
    }

    fun exportSessionReportToCsv(session: AttendanceSession, records: List<AttendanceRecord>, context: Context): File? {
        return repository.exportSessionReportToCsv(session, records, context)
    }

    fun generateSessionReportText(session: AttendanceSession, records: List<AttendanceRecord>): String {
        return repository.generateSessionReportText(session, records)
    }

    fun selectRole(role: UserRole) {
        repository.setUserRole(role)
    }

    fun formatAuthErrorMessage(e: Throwable?): String {
        if (e == null) return "Login failed. Please check credentials or sign up."
        val msg = e.message ?: ""
        return when {
            e is com.google.firebase.auth.FirebaseAuthInvalidUserException ||
            msg.contains("user-not-found", ignoreCase = true) ||
            msg.contains("no user record", ignoreCase = true) ->
                "Account not found. Please tap 'Sign Up' below to register."

            e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ||
            msg.contains("wrong-password", ignoreCase = true) ||
            msg.contains("invalid-credential", ignoreCase = true) ||
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                "Invalid email or password. Please check your credentials."

            e is com.google.firebase.FirebaseNetworkException ||
            msg.contains("network", ignoreCase = true) ||
            msg.contains("unreachable", ignoreCase = true) ->
                "Network connection error. Please check your internet connection and try again."

            e is com.google.firebase.FirebaseTooManyRequestsException ||
            e.javaClass.simpleName.contains("TooManyRequests") ||
            msg.contains("too-many-requests", ignoreCase = true) ->
                "Too many failed attempts. Access temporarily blocked. Please try again later."

            e is com.google.firebase.auth.FirebaseAuthUserCollisionException ||
            msg.contains("email-already-in-use", ignoreCase = true) ->
                "An account already exists with this email address. Please log in."

            msg.isNotBlank() -> msg
            else -> "Authentication failed. Please check your credentials."
        }
    }

    fun loginStudent(
        emailOrEnrollment: String,
        pass: String,
        onResult: (Boolean, Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.loginStudentWithFirebase(emailOrEnrollment, pass)
            if (result.isSuccess) {
                val student = result.getOrNull()
                if (student != null) {
                    val ctx = getApplication<Application>().applicationContext
                    com.example.data.SessionPreferences.setSavedRole(ctx, UserRole.STUDENT)
                    com.example.data.SessionPreferences.setSavedUserId(ctx, student.id)
                    onResult(true, student.faceRegistered, null)
                    startScanning()
                } else {
                    onResult(false, false, "Student profile could not be loaded")
                }
            } else {
                val friendlyError = formatAuthErrorMessage(result.exceptionOrNull())
                onResult(false, false, friendlyError)
            }
        }
    }

    fun loginFaculty(
        idOrEmail: String,
        pass: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.loginFacultyWithFirebase(idOrEmail, pass)
            if (result.isSuccess) {
                result.getOrNull()?.let {
                    val ctx = getApplication<Application>().applicationContext
                    com.example.data.SessionPreferences.setSavedRole(ctx, UserRole.FACULTY)
                    com.example.data.SessionPreferences.setSavedUserId(ctx, it.id)
                }
                onResult(true, null)
            } else {
                val friendlyError = formatAuthErrorMessage(result.exceptionOrNull())
                onResult(false, friendlyError)
            }
        }
    }

    fun loginAdmin(
        email: String,
        pass: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.loginAdminWithFirebase(email, pass)
            if (result.isSuccess) {
                result.getOrNull()?.let {
                    val ctx = getApplication<Application>().applicationContext
                    com.example.data.SessionPreferences.setSavedRole(ctx, UserRole.ADMIN)
                    com.example.data.SessionPreferences.setSavedUserId(ctx, it.uid)
                }
                onResult(true, null)
            } else {
                val friendlyError = formatAuthErrorMessage(result.exceptionOrNull())
                onResult(false, friendlyError)
            }
        }
    }

    fun signUpStudent(
        name: String,
        email: String,
        enrollment: String,
        dept: String,
        pass: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.signUpStudentWithFirebase(name, email, enrollment, dept, pass)
            if (result.isSuccess) {
                result.getOrNull()?.let {
                    val ctx = getApplication<Application>().applicationContext
                    com.example.data.SessionPreferences.setSavedRole(ctx, UserRole.STUDENT)
                    com.example.data.SessionPreferences.setSavedUserId(ctx, it.id)
                }
                onResult(true, null)
                startScanning()
            } else {
                val friendlyError = formatAuthErrorMessage(result.exceptionOrNull())
                onResult(false, friendlyError)
            }
        }
    }

    fun createFacultyByAdmin(
        name: String,
        facultyId: String,
        email: String,
        dept: String,
        password: String,
        phone: String = "",
        assignedClasses: List<String> = emptyList(),
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.authService.createFacultyByAdmin(
                name = name,
                facultyId = facultyId,
                email = email,
                department = dept,
                password = password,
                phone = phone,
                assignedClasses = assignedClasses
            )
            if (result.isSuccess) {
                repository.loadFaculty()
                repository.refreshAdminStats()
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to create faculty account")
            }
        }
    }

    fun createClassByAdmin(
        classItem: ClassItem,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.firestoreDataSource.saveClass(classItem)
            if (success) {
                repository.loadClassesAndDevices()
                repository.refreshAdminStats()
                onResult(true, null)
            } else {
                onResult(false, "Failed to save class to Firestore")
            }
        }
    }

    fun registerDeviceByAdmin(
        device: Esp32Device,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.firestoreDataSource.saveDevice(device)
            if (success) {
                repository.loadClassesAndDevices()
                onResult(true, null)
            } else {
                onResult(false, "Failed to save ESP32 device to Firestore")
            }
        }
    }

    fun exportAttendanceReport(context: Context): File? {
        return repository.exportReportToDownloads(context)
    }

    fun endFacultySession() {
        repository.endFacultySession()
    }

    fun toggleStudentAttendance(studentId: String) {
        repository.toggleStudentAttendance(studentId)
    }

    val unreadNotificationCount: StateFlow<Int> = repository.notifications.combine(
        MutableStateFlow(Unit)
    ) { list, _ ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _historyFilter = MutableStateFlow("All")
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    private val _facultyHistoryTimeFilter = MutableStateFlow("All") // "All", "This Week", "This Month"
    val facultyHistoryTimeFilter: StateFlow<String> = _facultyHistoryTimeFilter.asStateFlow()

    private val _facultyHistorySearchQuery = MutableStateFlow("")
    val facultyHistorySearchQuery: StateFlow<String> = _facultyHistorySearchQuery.asStateFlow()

    private val _facultyHistoryClassFilter = MutableStateFlow<String?>(null)
    val facultyHistoryClassFilter: StateFlow<String?> = _facultyHistoryClassFilter.asStateFlow()

    fun setFacultyHistoryTimeFilter(filter: String) {
        _facultyHistoryTimeFilter.value = filter
    }

    fun setFacultyHistorySearchQuery(query: String) {
        _facultyHistorySearchQuery.value = query
    }

    fun setFacultyHistoryClassFilter(classId: String?) {
        _facultyHistoryClassFilter.value = classId
    }

    val filteredFacultySessions: StateFlow<List<AttendanceSession>> = combine(
        repository.facultySessions,
        _facultyHistoryTimeFilter,
        _facultyHistorySearchQuery,
        _facultyHistoryClassFilter
    ) { sessions, timeFilter, query, classFilter ->
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000)
        val oneMonthAgo = now - (30L * 24 * 60 * 60 * 1000)

        sessions.filter { session ->
            // Time filter
            val matchesTime = when (timeFilter) {
                "This Week" -> session.updatedAt >= oneWeekAgo || session.createdAt >= oneWeekAgo
                "This Month" -> session.updatedAt >= oneMonthAgo || session.createdAt >= oneMonthAgo
                else -> true
            }

            // Class filter
            val matchesClass = classFilter == null || classFilter == "All" ||
                    session.classId.equals(classFilter, ignoreCase = true)

            // Search query filter (Class, Subject, Date)
            val cleanQuery = query.trim()
            val matchesSearch = if (cleanQuery.isBlank()) {
                true
            } else {
                session.classId.contains(cleanQuery, ignoreCase = true) ||
                session.className.contains(cleanQuery, ignoreCase = true) ||
                session.subject.contains(cleanQuery, ignoreCase = true) ||
                session.room.contains(cleanQuery, ignoreCase = true) ||
                session.date.contains(cleanQuery, ignoreCase = true)
            }

            matchesTime && matchesClass && matchesSearch
        }.sortedByDescending { it.updatedAt.coerceAtLeast(it.createdAt) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredHistory: StateFlow<List<AttendanceRecord>> = combine(
        repository.attendanceHistory,
        _historyFilter
    ) { history, filter ->
        when (filter) {
            "Present" -> history.filter { it.status.equals("Present", ignoreCase = true) }
            "Absent" -> history.filter { it.status.equals("Absent", ignoreCase = true) }
            else -> history
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val studentAttendanceHistory: StateFlow<List<AttendanceRecord>> = combine(
        repository.attendanceHistory,
        repository.currentStudent,
        _historyFilter
    ) { history, currentStudent, filter ->
        val studentRecords = if (currentStudent != null) {
            val matched = history.filter {
                it.studentUid.equals(currentStudent.id, ignoreCase = true) ||
                it.studentId.equals(currentStudent.id, ignoreCase = true) ||
                (it.enrollmentNo.isNotBlank() && it.enrollmentNo.equals(currentStudent.enrollmentNumber, ignoreCase = true)) ||
                (it.studentEnrollment.isNotBlank() && it.studentEnrollment.equals(currentStudent.enrollmentNumber, ignoreCase = true)) ||
                it.studentName.equals(currentStudent.name, ignoreCase = true)
            }
            if (matched.isNotEmpty()) matched else history
        } else {
            history
        }
        when (filter) {
            "Present" -> studentRecords.filter { it.status.equals("Present", ignoreCase = true) }
            "Absent" -> studentRecords.filter { it.status.equals("Absent", ignoreCase = true) }
            else -> studentRecords
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _lastMarkedRecord = MutableStateFlow<AttendanceRecord?>(null)
    val lastMarkedRecord: StateFlow<AttendanceRecord?> = _lastMarkedRecord.asStateFlow()

    private val _pendingBleSession = MutableStateFlow<BleState.ActiveSession?>(null)
    val pendingBleSession: StateFlow<BleState.ActiveSession?> = _pendingBleSession.asStateFlow()

    private val _faceVerificationTriggered = MutableStateFlow(false)
    val faceVerificationTriggered: StateFlow<Boolean> = _faceVerificationTriggered.asStateFlow()

    private var facultyPeriodicCheckJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            bleManager.state.collect { state ->
                when (state) {
                    is BleState.ActiveSession -> {
                        val currentFac = repository.currentFaculty.value
                        if (currentFac != null) {
                            Log.i("FACULTY_BLE", "[FACULTY_BLE] ESP32 ACTIVE session detected by faculty client! deviceId=${state.deviceId} token=${state.sessionToken}")
                            handleFacultyActiveBleSession(state)
                        } else {
                            // Student dual verification flow
                            _pendingBleSession.value = state
                            _faceVerificationTriggered.value = true
                        }
                    }
                    is BleState.InactiveSession -> {
                        val currentFac = repository.currentFaculty.value
                        if (currentFac != null) {
                            Log.i("FACULTY_BLE", "[FACULTY_BLE] ESP32 reported INACTIVE status! deviceId=${state.deviceId} status=${state.status}")
                            handleFacultyInactiveBleSession(inactiveState = state)
                        }
                    }
                    is BleState.Disconnected -> {
                        Log.d("FACULTY_BLE", "[FACULTY_BLE] BLE disconnected: ${state.message} (Session state preserved)")
                    }
                    is BleState.Error -> {
                        Log.w("FACULTY_BLE", "[FACULTY_BLE] BLE error: ${state.message}")
                    }
                    else -> {}
                }
            }
        }

        // Automatically start scanning on initialization if Bluetooth is enabled
        if (bleManager.isBluetoothEnabled() && bleManager.hasPermissions()) {
            bleManager.startScan()
        }
    }

    private fun handleFacultyActiveBleSession(activeState: BleState.ActiveSession) {
        viewModelScope.launch {
            try {
                val currentFac = repository.currentFaculty.value
                Log.i("FACULTY_BLE", "[FACULTY_BLE] Persisting active ESP32 session to Firestore: deviceId=${activeState.deviceId} token=${activeState.sessionToken} facId=${currentFac?.facultyId}")
                val session = repository.createOrSyncFacultySession(
                    deviceId = activeState.deviceId,
                    sessionToken = activeState.sessionToken,
                    faculty = currentFac
                )
                Log.i("FACULTY_BLE", "[FACULTY_BLE] Active session synced: id=${session.sessionId} class=${session.className} (${session.classId}) totalStudents=${session.totalStudents}")
            } catch (e: Exception) {
                Log.e("FACULTY_BLE", "[FACULTY_BLE] Failed to sync active session to Firestore: ${e.message}", e)
            }
        }
    }

    private fun handleFacultyInactiveBleSession(inactiveState: BleState.InactiveSession) {
        viewModelScope.launch {
            try {
                Log.i("FACULTY_BLE", "[FACULTY_BLE] Closing faculty session due to ESP32 INACTIVE status: ${inactiveState.deviceId}")
                repository.endFacultySession()
            } catch (e: Exception) {
                Log.e("FACULTY_BLE", "[FACULTY_BLE] Error closing session on inactive signal: ${e.message}", e)
            }
        }
    }

    fun startFacultyBleMonitoring() {
        val context = getApplication<Application>().applicationContext
        val hasPerms = BluetoothPermissionHelper.hasPermissions(context)
        val btEnabled = BluetoothPermissionHelper.isBluetoothEnabled(context)
        val currentFac = repository.currentFaculty.value
        Log.i("FACULTY_BLE_STARTUP", "[FACULTY_BLE_STARTUP] startFacultyBleMonitoring | hasPerms=$hasPerms btEnabled=$btEnabled facId=${currentFac?.facultyId ?: currentFac?.id}")

        if (!hasPerms || !btEnabled) {
            Log.w("FACULTY_BLE_STARTUP", "[FACULTY_BLE_STARTUP] Cannot start scanning: permissionsGranted=$hasPerms btEnabled=$btEnabled")
            return
        }

        // Start background foreground service for persistent beacon monitoring
        try {
            com.example.bluetooth.BleAttendanceForegroundService.start(context)
            Log.i("FACULTY_BLE_STARTUP", "[FACULTY_BLE_STARTUP] BleAttendanceForegroundService started successfully")
        } catch (e: Exception) {
            Log.w("FACULTY_BLE_STARTUP", "[FACULTY_BLE_STARTUP] BleAttendanceForegroundService start note: ${e.message}")
        }

        // Initiate BleManager scan
        Log.i("FACULTY_BLE_STARTUP", "[FACULTY_BLE_STARTUP] Starting BleManager scan for ESP32 beacon...")
        bleManager.startScan()

        // Periodic refresher: ensures scan stays active and detects STOP state changes reliably
        facultyPeriodicCheckJob?.cancel()
        facultyPeriodicCheckJob = viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                if (BluetoothPermissionHelper.hasPermissions(context) && BluetoothPermissionHelper.isBluetoothEnabled(context)) {
                    val currentState = bleManager.state.value
                    val isSessionActive = repository.isSessionActive.value
                    if (isSessionActive || currentState is BleState.Idle || currentState is BleState.Disconnected || currentState is BleState.DeviceNotFound || currentState is BleState.ActiveSession) {
                        Log.d("FACULTY_BLE_STARTUP", "[FACULTY_BLE_STARTUP] Periodic cycle restarting BLE scan to verify ESP32 status...")
                        bleManager.startScan()
                    }
                }
            }
        }
    }

    fun stopFacultyBleMonitoring() {
        Log.i("FACULTY_BLE_STARTUP", "[FACULTY_BLE_STARTUP] Stopping faculty BLE monitoring")
        facultyPeriodicCheckJob?.cancel()
        facultyPeriodicCheckJob = null
        bleManager.stopScan()
    }

    fun clearFaceVerificationTrigger() {
        _faceVerificationTriggered.value = false
    }

    fun setPendingBleSession(session: BleState.ActiveSession?) {
        _pendingBleSession.value = session
    }

    fun registerStudentFace(embedding: List<Float>, onResult: (Boolean, String?) -> Unit) {
        val currentStudent = repository.currentStudent.value
        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: currentStudent?.id
            ?: currentStudent?.enrollmentNumber
        if (authUid.isNullOrBlank()) {
            onResult(false, "Authentication required. Please sign in again.")
            return
        }
        viewModelScope.launch {
            val result = repository.registerFace(authUid, embedding)
            if (result.isSuccess) {
                // Confirm in-memory student is updated
                repository.currentStudent.value?.let { st ->
                    repository.setStudent(st.copy(faceRegistered = true, faceEmbedding = embedding))
                }
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to save face embedding to database.")
            }
        }
    }

    suspend fun verifyAndMarkAttendance(
        deviceId: String,
        sessionToken: String,
        bleVerified: Boolean,
        faceVerified: Boolean,
        livenessVerified: Boolean
    ): Pair<Boolean, String> {
        val student = repository.currentStudent.value ?: return Pair(false, "Student not signed in")
        val currentAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: student.id
        if (currentAuthUid.isBlank()) return Pair(false, "Student not authenticated")

        if (!bleVerified) {
            return Pair(false, "Bluetooth verification failed")
        }
        if (!faceVerified) {
            return Pair(false, "Face does not match registered student")
        }
        if (!livenessVerified) {
            return Pair(false, "Liveness verification failed")
        }

        val sessionId = "${deviceId}_${sessionToken}"

        // Uniqueness check: studentUid + sessionToken
        val cloudDuplicate = try {
            repository.firestoreDataSource.checkAttendanceExists(currentAuthUid, sessionToken)
        } catch (e: Exception) {
            false
        }
        val localDuplicate = repository.hasMarkedAttendance(
            sessionId = sessionId,
            sessionToken = sessionToken
        )

        if (localDuplicate || cloudDuplicate) {
            Log.i("BLE_ATTENDANCE", "session validation result: DUPLICATE (${currentAuthUid}_$sessionToken)")
            bleManager.setAlreadyMarked("Attendance Already Marked")
            return Pair(false, "Attendance already marked for this session")
        }

        Log.i("BLE_ATTENDANCE", "Dual verification passed: BLE=true, Face=true, Liveness=true for authUid=$currentAuthUid")

        // 1. SESSION CREATION: Identify or create active session in Firestore
        val session = repository.firestoreDataSource.getOrCreateSession(
            deviceId = deviceId,
            sessionToken = sessionToken,
            deviceName = "BLE_Attendance"
        )

        // 2. STUDENT ATTENDANCE: Record in Firestore with all 3 verifications
        val record = repository.recordAttendance(
            session = session,
            sessionToken = sessionToken,
            deviceId = deviceId,
            bleVerified = true,
            faceVerified = true,
            livenessVerified = true
        )

        return if (record != null) {
            _lastMarkedRecord.value = record
            _pendingBleSession.value = null
            _faceVerificationTriggered.value = false
            Log.i("BLE_ATTENDANCE", "Attendance successfully recorded with BLE + Face: ${record.attendanceId}")
            bleManager.setAttendanceMarked(record)
            Pair(true, "Attendance marked successfully")
        } else {
            Log.e("BLE_ATTENDANCE", "attendance Firebase write result: FAILED")
            bleManager.setFirebaseError("Failed to save attendance record to database")
            Pair(false, "Failed to record attendance")
        }
    }

    fun validateAndRecordAttendance(deviceId: String, sessionToken: String) {
        viewModelScope.launch {
            verifyAndMarkAttendance(deviceId, sessionToken, bleVerified = true, faceVerified = true, livenessVerified = true)
        }
    }

    fun startScanning() {
        Log.i("BLE_ATTENDANCE", "[DIAG_VIEWMODEL] startScanning() requested by UI")
        bleManager.startScan()
    }

    fun stopScanning() {
        Log.i("BLE_ATTENDANCE", "[DIAG_VIEWMODEL] stopScanning() requested by UI")
        bleManager.stopScan()
        bleManager.disconnect()
    }

    fun updateActiveSessionClass(classId: String, className: String, subject: String) {
        repository.updateActiveSessionClass(classId, className, subject)
    }

    fun resetScannerToScanning() {
        Log.i("BLE_ATTENDANCE", "[DIAG_VIEWMODEL] resetScannerToScanning() requested by UI")
        bleManager.startScan()
        _lastMarkedRecord.value = null
    }

    fun recordLocalAttendance(deviceId: String, sessionToken: String): AttendanceRecord? {
        Log.w("BLE_ATTENDANCE", "Direct local attendance bypass blocked: biometric face verification required.")
        return null
    }

    fun logout() {
        stopScanning()
        _lastMarkedRecord.value = null
        _pendingBleSession.value = null
        _faceVerificationTriggered.value = false
        val ctx = getApplication<Application>().applicationContext
        com.example.data.SessionPreferences.clearSession(ctx)
        repository.logout()
    }

    fun updateProfile(name: String, enrollment: String, dept: String) {
        repository.updateProfile(name, enrollment, dept)
    }

    fun changePassword(newPass: String): Boolean {
        return repository.changePassword(newPass)
    }

    fun updateSettings(
        bluetooth: Boolean? = null,
        backgroundScanning: Boolean? = null,
        notifications: Boolean? = null,
        themeMode: String? = null
    ) {
        repository.updateSettings(bluetooth, backgroundScanning, notifications, themeMode)
        if (bluetooth == false) {
            stopScanning()
        } else if (bluetooth == true) {
            startScanning()
        }
    }

    fun markNotificationRead(id: String) {
        repository.markNotificationAsRead(id)
    }

    fun setLastMarkedRecord(record: AttendanceRecord?) {
        _lastMarkedRecord.value = record
    }

    fun generateSessionReportCsv(session: AttendanceSession, records: List<AttendanceRecord>): String {
        return repository.generateSessionReportCsv(session, records)
    }

    suspend fun generateSessionReportText(sessionId: String): String {
        return repository.generateSessionReportText(sessionId)
    }

    suspend fun exportSessionReportToCsv(sessionId: String): Pair<File?, String?> {
        val appContext = getApplication<Application>().applicationContext
        return repository.exportSessionReportToCsv(sessionId, appContext)
    }

    suspend fun generateAndOpenPdfReport(
        session: AttendanceSession,
        records: List<AttendanceRecord>,
        context: Context
    ): Boolean {
        val file = com.example.util.PdfReportGenerator.generatePdfReport(context, session, records)
        return if (file != null && file.exists()) {
            com.example.util.PdfReportGenerator.openPdf(context, file)
        } else {
            false
        }
    }

    suspend fun sharePdfReport(
        session: AttendanceSession,
        records: List<AttendanceRecord>,
        context: Context
    ): Boolean {
        val file = com.example.util.PdfReportGenerator.generatePdfReport(context, session, records)
        return if (file != null && file.exists()) {
            com.example.util.PdfReportGenerator.sharePdf(context, file, session)
        } else {
            false
        }
    }

    suspend fun generateAndOpenPdfReport(
        sessionId: String,
        context: Context
    ): Boolean {
        val session = repository.facultySessions.value.firstOrNull { it.sessionId == sessionId }
            ?: repository.currentActiveSession.value
        val records = repository.firestoreDataSource.getSessionAttendance(session.sessionId)
        return generateAndOpenPdfReport(session, records, context)
    }

    suspend fun sharePdfReport(
        sessionId: String,
        context: Context
    ): Boolean {
        val session = repository.facultySessions.value.firstOrNull { it.sessionId == sessionId }
            ?: repository.currentActiveSession.value
        val records = repository.firestoreDataSource.getSessionAttendance(session.sessionId)
        return sharePdfReport(session, records, context)
    }
}

package com.example.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.UserRole
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.AttendanceHistoryScreen
import com.example.ui.screens.AttendanceMarkedScreen
import com.example.ui.screens.FaceRegistrationScreen
import com.example.ui.screens.FaceVerificationScreen
import com.example.ui.screens.FacultyClassesScreen
import com.example.ui.screens.FacultyDashboardScreen
import com.example.ui.screens.FacultyLiveAttendanceScreen
import com.example.ui.screens.FacultyLoginScreen
import com.example.ui.screens.FacultyProfileScreen
import com.example.ui.screens.FacultyReportsScreen
import com.example.ui.screens.FacultySessionActiveScreen
import com.example.ui.screens.FacultyStudentsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.viewmodel.AttendanceViewModel

object NavRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val ROLE_SELECTION = "role_selection"
    const val STUDENT_LOGIN = "student_login"
    const val FACULTY_LOGIN = "faculty_login"
    const val ADMIN_LOGIN = "admin_login"
    const val ADMIN_DASHBOARD = "admin_dashboard"

    // Student Routes
    const val HOME = "home"
    const val ATTENDANCE_MARKED = "attendance_marked"
    const val HISTORY = "history"
    const val PROFILE = "profile"
    const val FACE_REGISTRATION = "face_registration"
    const val FACE_VERIFICATION = "face_verification"

    // Faculty Routes
    const val FACULTY_DASHBOARD = "faculty_dashboard"
    const val FACULTY_CLASSES = "faculty_classes"
    const val FACULTY_STUDENTS = "faculty_students"
    const val FACULTY_LIVE_ATTENDANCE = "faculty_live_attendance"
    const val FACULTY_ACTIVE_SESSION = "faculty_active_session"
    const val FACULTY_SESSION_DETAILS = "faculty_session_details/{sessionId}"
    const val FACULTY_REPORTS = "faculty_reports"
    const val FACULTY_PROFILE = "faculty_profile"

    // Common Routes
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val ABOUT = "about"
}

@Composable
fun SmartAttendanceNavGraph(
    viewModel: AttendanceViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> (fullWidth * 0.12f).toInt() },
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(260))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * 0.12f).toInt() },
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(260))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * 0.12f).toInt() },
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(260))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> (fullWidth * 0.12f).toInt() },
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(260))
        }
    ) {
        // Screen 1: Splash Screen with Persistent Login Check
        composable(NavRoutes.SPLASH) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val isSessionRestored by viewModel.isSessionRestored.collectAsState()

            SplashScreen(
                isSessionRestored = isSessionRestored,
                onNavigateNext = {
                    val loggedIn = viewModel.isLoggedIn.value
                    val currentRole = viewModel.currentRole.value
                    val hasSeenOnboarding = com.example.data.SessionPreferences.hasSeenOnboarding(context)

                    if (loggedIn && currentRole != null) {
                        when (currentRole) {
                            UserRole.ADMIN -> {
                                navController.navigate(NavRoutes.ADMIN_DASHBOARD) {
                                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                }
                            }
                            UserRole.FACULTY -> {
                                navController.navigate(NavRoutes.FACULTY_DASHBOARD) {
                                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                }
                            }
                            UserRole.STUDENT -> {
                                val currentStudent = viewModel.student.value
                                if (currentStudent != null && !currentStudent.faceRegistered) {
                                    navController.navigate(NavRoutes.FACE_REGISTRATION) {
                                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(NavRoutes.HOME) {
                                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                    }
                                }
                            }
                        }
                    } else {
                        // User is not authenticated
                        if (hasSeenOnboarding) {
                            // On subsequent launches, skip onboarding and navigate directly to Login screen
                            val savedRole = com.example.data.SessionPreferences.getSavedRole(context)
                            val targetLoginRoute = when (savedRole) {
                                UserRole.FACULTY -> NavRoutes.FACULTY_LOGIN
                                UserRole.ADMIN -> NavRoutes.ADMIN_LOGIN
                                else -> NavRoutes.STUDENT_LOGIN
                            }
                            navController.navigate(targetLoginRoute) {
                                popUpTo(NavRoutes.SPLASH) { inclusive = true }
                            }
                        } else {
                            // First launch: show onboarding
                            navController.navigate(NavRoutes.ONBOARDING) {
                                popUpTo(NavRoutes.SPLASH) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        // Screen 2: Onboarding Screen
        composable(NavRoutes.ONBOARDING) {
            val context = androidx.compose.ui.platform.LocalContext.current
            OnboardingScreen(
                onGetStarted = {
                    // Mark onboarding complete persistently ONLY when user finishes or skips
                    com.example.data.SessionPreferences.setHasSeenOnboarding(context, true)
                    navController.navigate(NavRoutes.ROLE_SELECTION) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // Screen 3: Role Selection Screen (Student vs Faculty vs Admin)
        composable(NavRoutes.ROLE_SELECTION) {
            RoleSelectionScreen(
                onSelectRole = { role ->
                    viewModel.selectRole(role)
                    when (role) {
                        UserRole.STUDENT -> navController.navigate(NavRoutes.STUDENT_LOGIN)
                        UserRole.FACULTY -> navController.navigate(NavRoutes.FACULTY_LOGIN)
                        UserRole.ADMIN -> navController.navigate(NavRoutes.ADMIN_LOGIN)
                    }
                }
            )
        }

        composable(NavRoutes.STUDENT_LOGIN) {
            LoginScreen(
                onLoginSuccess = { _ ->
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.STUDENT_LOGIN) { inclusive = true }
                    }
                },
                onSignUpSuccess = {
                    // Direct first-time registered student to mandatory face registration
                    navController.navigate(NavRoutes.FACE_REGISTRATION) {
                        popUpTo(NavRoutes.STUDENT_LOGIN) { inclusive = true }
                    }
                },
                onBackToRoleSelection = {
                    navController.navigate(NavRoutes.ROLE_SELECTION) {
                        popUpTo(NavRoutes.STUDENT_LOGIN) { inclusive = true }
                    }
                },
                onPerformLogin = { email, pass, cb ->
                    viewModel.loginStudent(email, pass, cb)
                },
                onPerformSignUp = { name, email, enroll, dept, pass, cb ->
                    viewModel.signUpStudent(name, email, enroll, dept, pass, cb)
                }
            )
        }

        // Screen 5: Faculty Login Screen
        composable(NavRoutes.FACULTY_LOGIN) {
            FacultyLoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.FACULTY_DASHBOARD) {
                        popUpTo(NavRoutes.FACULTY_LOGIN) { inclusive = true }
                    }
                },
                onBackToRoleSelection = {
                    navController.navigate(NavRoutes.ROLE_SELECTION) {
                        popUpTo(NavRoutes.FACULTY_LOGIN) { inclusive = true }
                    }
                },
                onPerformFacultyLogin = { idOrEmail, pass, cb ->
                    viewModel.loginFaculty(idOrEmail, pass, cb)
                }
            )
        }

        // Screen 6: Student Home Screen (BLE auto check-in & ESP32 connection)
        composable(NavRoutes.HOME) {
            val student by viewModel.student.collectAsState()
            val scanState by viewModel.scanState.collectAsState()
            val bleState by viewModel.bleState.collectAsState()
            val unreadNotifCount by viewModel.unreadNotificationCount.collectAsState()
            val discoveredDevices by viewModel.discoveredDevices.collectAsState()

            HomeScreen(
                student = student,
                scanState = scanState,
                bleState = bleState,
                unreadNotificationCount = unreadNotifCount,
                onNavigateToNotifications = {
                    navController.navigate(NavRoutes.NOTIFICATIONS)
                },
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY) {
                        popUpTo(NavRoutes.HOME) { saveState = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(NavRoutes.PROFILE) {
                        popUpTo(NavRoutes.HOME) { saveState = true }
                        launchSingleTop = true
                    }
                },
                onAttendanceMarkedAuto = { record ->
                    viewModel.setLastMarkedRecord(record)
                    navController.navigate(NavRoutes.ATTENDANCE_MARKED)
                },
                onStartScanning = {
                    viewModel.startScanning()
                },
                onRecordLocalAttendance = { deviceId, token ->
                    val record = viewModel.recordLocalAttendance(deviceId, token)
                    if (record != null) {
                        navController.navigate(NavRoutes.ATTENDANCE_MARKED)
                    }
                },
                discoveredDevices = discoveredDevices,
                onConnectDiscoveredDevice = { dev ->
                    viewModel.connectToDiscoveredDevice(dev)
                },
                onNavigateToFaceVerification = { deviceId, token ->
                    navController.navigate(NavRoutes.FACE_VERIFICATION)
                },
                onNavigateToFaceRegistration = {
                    navController.navigate(NavRoutes.FACE_REGISTRATION)
                }
            )
        }

        // Screen: Face Registration (Biometric enrollment for students)
        composable(NavRoutes.FACE_REGISTRATION) {
            val student by viewModel.student.collectAsState()

            FaceRegistrationScreen(
                student = student,
                onNavigateBack = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.FACE_REGISTRATION) { inclusive = true }
                    }
                },
                onRegistrationComplete = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.FACE_REGISTRATION) { inclusive = true }
                    }
                },
                onSaveEmbedding = { embedding, cb ->
                    viewModel.registerStudentFace(embedding, cb)
                }
            )
        }

        // Screen: Face Verification (Anti-Proxy Attendance check)
        composable(NavRoutes.FACE_VERIFICATION) {
            val student by viewModel.student.collectAsState()
            val pendingBle by viewModel.pendingBleSession.collectAsState()
            val effectiveDeviceId = pendingBle?.deviceId ?: "CLASSROOM_ESP32_01"
            val effectiveToken = pendingBle?.sessionToken ?: "token_${System.currentTimeMillis()}"

            FaceVerificationScreen(
                student = student,
                deviceId = effectiveDeviceId,
                sessionToken = effectiveToken,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRegistration = {
                    navController.navigate(NavRoutes.FACE_REGISTRATION)
                },
                onAttendanceMarkedSuccess = { record ->
                    viewModel.setLastMarkedRecord(record)
                    navController.navigate(NavRoutes.ATTENDANCE_MARKED) {
                        popUpTo(NavRoutes.HOME) { inclusive = false }
                    }
                },
                onVerifyAndMarkAttendance = { devId, token, bleV, faceV, liveV ->
                    viewModel.verifyAndMarkAttendance(devId, token, bleV, faceV, liveV)
                }
            )
        }

        // Screen 7: Attendance Marked Confirmation Screen
        composable(NavRoutes.ATTENDANCE_MARKED) {
            val lastMarkedRecord by viewModel.lastMarkedRecord.collectAsState()

            AttendanceMarkedScreen(
                record = lastMarkedRecord,
                onDoneClick = {
                    viewModel.resetScannerToScanning()
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        // Screen 8: Attendance History Screen
        composable(NavRoutes.HISTORY) {
            val currentRole by viewModel.currentRole.collectAsState()
            val studentAttendanceHistory by viewModel.studentAttendanceHistory.collectAsState()
            val historyFilter by viewModel.historyFilter.collectAsState()
            val facultySessions by viewModel.filteredFacultySessions.collectAsState()
            val facultyTimeFilter by viewModel.facultyHistoryTimeFilter.collectAsState()
            val facultySearchQuery by viewModel.facultyHistorySearchQuery.collectAsState()
            val facultyClassFilter by viewModel.facultyHistoryClassFilter.collectAsState()
            val classes by viewModel.classesList.collectAsState()

            AttendanceHistoryScreen(
                records = if (currentRole == UserRole.STUDENT) studentAttendanceHistory else emptyList(),
                facultySessions = facultySessions,
                facultyTimeFilter = facultyTimeFilter,
                onFacultyTimeFilterChange = { viewModel.setFacultyHistoryTimeFilter(it) },
                facultySearchQuery = facultySearchQuery,
                onFacultySearchQueryChange = { viewModel.setFacultyHistorySearchQuery(it) },
                facultyClassFilter = facultyClassFilter,
                onFacultyClassFilterChange = { viewModel.setFacultyHistoryClassFilter(it) },
                availableClasses = classes.map { it.classId }.distinct(),
                selectedFilter = historyFilter,
                onFilterChange = { filter ->
                    viewModel.setHistoryFilter(filter)
                },
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    if (currentRole == UserRole.FACULTY) {
                        navController.navigate(NavRoutes.FACULTY_DASHBOARD) {
                            popUpTo(NavRoutes.FACULTY_DASHBOARD) { inclusive = true }
                        }
                    } else {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.HOME) { inclusive = true }
                        }
                    }
                },
                onNavigateToProfile = {
                    val dest = if (currentRole == UserRole.FACULTY) NavRoutes.FACULTY_PROFILE else NavRoutes.PROFILE
                    navController.navigate(dest) {
                        launchSingleTop = true
                    }
                },
                onNavigateToClasses = {
                    navController.navigate(NavRoutes.FACULTY_CLASSES)
                },
                onNavigateToStudents = {
                    navController.navigate(NavRoutes.FACULTY_STUDENTS)
                },
                onSessionClick = { sId ->
                    navController.navigate("faculty_session_details/$sId")
                },
                userRole = currentRole
            )
        }

        // Screen 9: Student Profile Screen
        composable(NavRoutes.PROFILE) {
            val student by viewModel.student.collectAsState()

            ProfileScreen(
                student = student,
                onNavigateToHome = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY) {
                        popUpTo(NavRoutes.HOME) { saveState = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                },
                onNavigateToAbout = {
                    navController.navigate(NavRoutes.ABOUT)
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(NavRoutes.ROLE_SELECTION) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onUpdateProfile = { name, enroll, dept ->
                    viewModel.updateProfile(name, enroll, dept)
                },
                onChangePassword = { newPass ->
                    viewModel.changePassword(newPass)
                },
                onNavigateToFaceRegistration = {
                    navController.navigate(NavRoutes.FACE_REGISTRATION)
                }
            )
        }

        // Screen 11: Faculty Dashboard Screen
        composable(NavRoutes.FACULTY_DASHBOARD) {
            FacultyDashboardScreen(
                viewModel = viewModel,
                onNavigateToClasses = {
                    navController.navigate(NavRoutes.FACULTY_CLASSES)
                },
                onNavigateToStudents = {
                    navController.navigate(NavRoutes.FACULTY_STUDENTS)
                },
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY)
                },
                onNavigateToSessionDetails = { sessionId ->
                    navController.navigate("faculty_session_details/$sessionId")
                },
                onNavigateToReports = {
                    navController.navigate(NavRoutes.FACULTY_REPORTS)
                },
                onNavigateToProfile = {
                    navController.navigate(NavRoutes.FACULTY_PROFILE)
                },
                onNavigateToNotifications = {
                    navController.navigate(NavRoutes.NOTIFICATIONS)
                }
            )
        }

        // Faculty Students Tab Screen
        composable(NavRoutes.FACULTY_STUDENTS) {
            FacultyStudentsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(NavRoutes.FACULTY_DASHBOARD) {
                        popUpTo(NavRoutes.FACULTY_DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToClasses = {
                    navController.navigate(NavRoutes.FACULTY_CLASSES)
                },
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY)
                },
                onNavigateToProfile = {
                    navController.navigate(NavRoutes.FACULTY_PROFILE)
                }
            )
        }

        // Faculty Classes Tab Screen
        composable(NavRoutes.FACULTY_CLASSES) {
            FacultyClassesScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                    navController.navigate(NavRoutes.FACULTY_DASHBOARD) {
                        popUpTo(NavRoutes.FACULTY_DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY)
                },
                onNavigateToStudents = {
                    navController.navigate(NavRoutes.FACULTY_STUDENTS)
                },
                onNavigateToReports = { classId ->
                    navController.navigate(NavRoutes.FACULTY_REPORTS)
                },
                onNavigateToProfile = {
                    navController.navigate(NavRoutes.FACULTY_PROFILE)
                }
            )
        }

        // Faculty Attendance Details Screen
        composable(
            route = "faculty_session_details/{sessionId}",
            arguments = listOf(androidx.navigation.navArgument("sessionId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            com.example.ui.screens.FacultyAttendanceDetailsScreen(
                sessionId = sessionId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(NavRoutes.FACULTY_DASHBOARD) {
                        popUpTo(NavRoutes.FACULTY_DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToClasses = {
                    navController.navigate(NavRoutes.FACULTY_CLASSES)
                },
                onNavigateToStudents = {
                    navController.navigate(NavRoutes.FACULTY_STUDENTS)
                },
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY)
                },
                onNavigateToProfile = {
                    navController.navigate(NavRoutes.FACULTY_PROFILE)
                }
            )
        }

        // Screen 12: Faculty Live Attendance Screen (Redirects to Details or Live fallback)
        composable(NavRoutes.FACULTY_LIVE_ATTENDANCE) {
            com.example.ui.screens.FacultyAttendanceDetailsScreen(
                sessionId = "sess_esp32",
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(NavRoutes.FACULTY_DASHBOARD) {
                        popUpTo(NavRoutes.FACULTY_DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToClasses = {
                    navController.navigate(NavRoutes.FACULTY_CLASSES)
                },
                onNavigateToStudents = {
                    navController.navigate(NavRoutes.FACULTY_STUDENTS)
                },
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY)
                },
                onNavigateToProfile = {
                    navController.navigate(NavRoutes.FACULTY_PROFILE)
                }
            )
        }

        // Screen 13: Faculty Attendance Session Active Screen
        composable(NavRoutes.FACULTY_ACTIVE_SESSION) {
            com.example.ui.screens.FacultyAttendanceDetailsScreen(
                sessionId = "sess_esp32",
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(NavRoutes.FACULTY_DASHBOARD) {
                        popUpTo(NavRoutes.FACULTY_DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToClasses = {
                    navController.navigate(NavRoutes.FACULTY_CLASSES)
                },
                onNavigateToStudents = {
                    navController.navigate(NavRoutes.FACULTY_STUDENTS)
                },
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY)
                },
                onNavigateToProfile = {
                    navController.navigate(NavRoutes.FACULTY_PROFILE)
                }
            )
        }

        // Screen 14: Faculty Reports Screen
        composable(NavRoutes.FACULTY_REPORTS) {
            FacultyReportsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(NavRoutes.FACULTY_DASHBOARD) {
                        popUpTo(NavRoutes.FACULTY_DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToClasses = {
                    navController.navigate(NavRoutes.FACULTY_CLASSES)
                },
                onNavigateToStudents = {
                    navController.navigate(NavRoutes.FACULTY_STUDENTS)
                },
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY)
                },
                onNavigateToProfile = {
                    navController.navigate(NavRoutes.FACULTY_PROFILE)
                }
            )
        }

        // Screen 15: Faculty Profile Screen
        composable(NavRoutes.FACULTY_PROFILE) {
            FacultyProfileScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                    navController.navigate(NavRoutes.FACULTY_DASHBOARD) {
                        popUpTo(NavRoutes.FACULTY_DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToClasses = {
                    navController.navigate(NavRoutes.FACULTY_CLASSES)
                },
                onNavigateToStudents = {
                    navController.navigate(NavRoutes.FACULTY_STUDENTS)
                },
                onNavigateToLiveAttendance = {
                    navController.navigate(NavRoutes.FACULTY_LIVE_ATTENDANCE)
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                },
                onNavigateToNotifications = {
                    navController.navigate(NavRoutes.NOTIFICATIONS)
                },
                onNavigateToAbout = {
                    navController.navigate(NavRoutes.ABOUT)
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(NavRoutes.ROLE_SELECTION) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Screen 10: App Settings Screen
        composable(NavRoutes.SETTINGS) {
            val settings by viewModel.settings.collectAsState()

            SettingsScreen(
                settings = settings,
                onUpdateSettings = { bt, bg, notif, theme ->
                    viewModel.updateSettings(bt, bg, notif, theme)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Notifications Screen
        composable(NavRoutes.NOTIFICATIONS) {
            val notifications by viewModel.notifications.collectAsState()

            NotificationsScreen(
                notifications = notifications,
                onNotificationClick = { id ->
                    viewModel.markNotificationRead(id)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // About Screen
        composable(NavRoutes.ABOUT) {
            AboutScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Admin Login Screen
        composable(NavRoutes.ADMIN_LOGIN) {
            AdminLoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.ADMIN_DASHBOARD) {
                        popUpTo(NavRoutes.ADMIN_LOGIN) { inclusive = true }
                    }
                },
                onBackToRoleSelection = {
                    navController.navigate(NavRoutes.ROLE_SELECTION) {
                        popUpTo(NavRoutes.ADMIN_LOGIN) { inclusive = true }
                    }
                },
                onPerformAdminLogin = { email, pass, cb ->
                    viewModel.loginAdmin(email, pass, cb)
                }
            )
        }

        // Admin Dashboard Screen
        composable(NavRoutes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                viewModel = viewModel,
                onLogout = {
                    navController.navigate(NavRoutes.ROLE_SELECTION) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

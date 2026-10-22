package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import com.example.bluetooth.BluetoothPermissionHelper
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceSession
import com.example.model.ClassScheduleItem
import com.example.ui.components.FacultyBottomNavigationBar
import com.example.ui.components.FacultyBottomNavTab
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.PaletteGray
import com.example.ui.theme.PaletteLightBlue
import com.example.ui.theme.StatusPresentGreenBg
import com.example.ui.theme.StatusPresentGreenText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AttendanceViewModel

@Composable
fun FacultyDashboardScreen(
    viewModel: AttendanceViewModel,
    onNavigateToClasses: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSessionDetails: (String) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val faculty by viewModel.faculty.collectAsState()
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val currentActiveSession by viewModel.currentActiveSession.collectAsState()
    val studentLiveList by viewModel.studentLiveList.collectAsState()
    val facultySessions by viewModel.facultySessions.collectAsState()
    val schedule by viewModel.facultySchedule.collectAsState()
    val classes by viewModel.classesList.collectAsState()
    val students by viewModel.studentsList.collectAsState()
    val latestCompletedSession by viewModel.latestCompletedSession.collectAsState()
    val latestPresentCount by viewModel.latestSessionPresentCount.collectAsState()
    val latestTotalCount by viewModel.latestSessionTotalCount.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        android.util.Log.i("FACULTY_BLE_STARTUP", "[FACULTY_BLE_STARTUP] Permission result: allGranted=$allGranted perms=$permissions")
        if (allGranted) {
            viewModel.startFacultyBleMonitoring()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadStudents()
        viewModel.loadClassesAndDevices()
        viewModel.faculty.value?.let { fac ->
            val fid = fac.facultyId.ifBlank { fac.id }
            viewModel.loadFacultyData(fid)
        }

        if (!BluetoothPermissionHelper.hasPermissions(context)) {
            android.util.Log.i("FACULTY_BLE_STARTUP", "[FACULTY_BLE_STARTUP] Requesting Bluetooth permissions for Faculty Dashboard...")
            permissionLauncher.launch(BluetoothPermissionHelper.getRequiredPermissions())
        } else {
            android.util.Log.i("FACULTY_BLE_STARTUP", "[FACULTY_BLE_STARTUP] Permissions already granted. Starting BLE monitoring...")
            viewModel.startFacultyBleMonitoring()
        }
    }

    androidx.compose.runtime.LaunchedEffect(faculty) {
        faculty?.let { fac ->
            val fid = fac.facultyId.ifBlank { fac.id }
            viewModel.loadFacultyData(fid)
            if (BluetoothPermissionHelper.hasPermissions(context) && BluetoothPermissionHelper.isBluetoothEnabled(context)) {
                viewModel.startFacultyBleMonitoring()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopFacultyBleMonitoring()
        }
    }

    androidx.compose.runtime.LaunchedEffect(students, classes, facultySessions) {
        android.util.Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] UI_RENDER screen=FacultyDashboardScreen students_count=${students.size} classes_count=${classes.size} sessions_count=${facultySessions.size}")
    }

    Scaffold(
        bottomBar = {
            FacultyBottomNavigationBar(
                currentTab = FacultyBottomNavTab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        FacultyBottomNavTab.HOME -> {}
                        FacultyBottomNavTab.CLASSES -> onNavigateToClasses()
                        FacultyBottomNavTab.STUDENTS -> onNavigateToStudents()
                        FacultyBottomNavTab.HISTORY -> onNavigateToHistory()
                        FacultyBottomNavTab.PROFILE -> onNavigateToProfile()
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceWhite)
                .statusBarsPadding()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Header: "Hello, Prof. Priya Sharma" and Notification Bell
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello,",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextSecondary,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = faculty?.name ?: "Faculty Member",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 22.sp
                            )
                        )
                    }

                    Box {
                        IconButton(
                            onClick = onNavigateToNotifications,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PaletteLightBlue)
                                .testTag("faculty_dashboard_notif_btn")
                        ) {
                            Icon(
                                imageVector = if (unreadCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = BrandPrimaryBlue
                            )
                        }
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }

            // Stat Cards Row with real data
            item {
                val todaysClassesCount = if (schedule.isNotEmpty()) schedule.size else (faculty?.assignedClassIds?.size?.takeIf { it > 0 } ?: classes.size)
                val completedLecturesCount = facultySessions.count { it.status.equals("COMPLETED", ignoreCase = true) || it.status.equals("ACTIVE", ignoreCase = true) }
                val totalStudentsCount = students.size

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Today's Classes
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                            .testTag("stat_todays_classes"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$todaysClassesCount",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimaryBlue,
                                    fontSize = 22.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Today's Classes",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // 2. Sessions Conducted / Completed Lectures
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                            .testTag("stat_sessions_conducted"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$completedLecturesCount",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusPresentGreenText,
                                    fontSize = 22.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Sessions Conducted",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // 3. Total Students
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                            .testTag("stat_total_students"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$totalStudentsCount",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 22.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Total Students",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // Current Class / Active ESP32 Attendance Card or Latest Attendance
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                        .testTag("faculty_current_class_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        if (isSessionActive) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Attendance In Progress",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = StatusPresentGreenText,
                                        fontSize = 13.sp
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(StatusPresentGreenBg)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = StatusPresentGreenText
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val presentStudents = studentLiveList.filter { it.isPresent }
                            val totalStudents = if (currentActiveSession.totalStudents > 0) currentActiveSession.totalStudents else 30
                            val presentCount = presentStudents.size

                            val displaySubject = currentActiveSession.subject.ifBlank {
                                currentActiveSession.className.ifBlank { "" }
                            }
                            val displayClass = if (currentActiveSession.className.isNotBlank() && currentActiveSession.classId.isNotBlank()) {
                                "${currentActiveSession.className} • ${currentActiveSession.classId}"
                            } else if (currentActiveSession.classId.isNotBlank()) {
                                currentActiveSession.classId
                            } else {
                                currentActiveSession.className
                            }

                            val hasClassMapping = currentActiveSession.classId.isNotBlank() || currentActiveSession.className.isNotBlank() || currentActiveSession.subject.isNotBlank()

                            if (!hasClassMapping) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFF3E0))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Active device detected (${currentActiveSession.deviceId.ifBlank { "CLASSROOM_ESP32_01" }}), but no class is assigned in Firebase. Please assign a class in settings.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFE65100),
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            } else {
                                Text(
                                    text = displaySubject.ifBlank { displayClass },
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 20.sp
                                    )
                                )

                                Text(
                                    text = displayClass,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandPrimaryBlue,
                                        fontSize = 14.sp
                                    )
                                )
                            }

                            if (classes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Assigned Class Mapping:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(classes, key = { it.classId }) { cls ->
                                        val isSelected = currentActiveSession.classId.equals(cls.classId, ignoreCase = true)
                                        androidx.compose.material3.FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.updateActiveSessionClass(cls.classId, cls.className, cls.subject)
                                            },
                                            label = { Text("${cls.className} (${cls.classId})") }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Session Info Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Started", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = currentActiveSession.startTime.ifBlank { "Ongoing" },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }
                                Column {
                                    Text("Device", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = currentActiveSession.deviceName.ifBlank { currentActiveSession.bleIdentifier.ifBlank { "BLE_Attendance" } },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }
                                Column {
                                    Text("Present", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = "$presentCount / $totalStudents",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = StatusPresentGreenText
                                    )
                                }
                                Column {
                                    Text("Status", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = "ACTIVE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = StatusPresentGreenText
                                    )
                                }
                            }

                            // Attendance Progress list of verified students from Firestore
                            if (presentStudents.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = SurfaceBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Live Verified Students ($presentCount Present)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    presentStudents.take(4).forEach { student ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = StatusPresentGreenText,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = student.name,
                                                fontSize = 12.sp,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (student.enrollmentNo.isNotBlank()) {
                                                Text(
                                                    text = "(${student.enrollmentNo})",
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }
                                    if (presentStudents.size > 4) {
                                        Text(
                                            text = "+ ${presentStudents.size - 4} more students verified",
                                            fontSize = 11.sp,
                                            color = BrandPrimaryBlue,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.endFacultySession()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_end_session_from_dashboard"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("End Session", fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        onNavigateToSessionDetails(currentActiveSession.sessionId)
                                    },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp)
                                        .testTag("btn_view_active_session_details"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View Attendance", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else if (latestCompletedSession != null) {
                            val lastSession = latestCompletedSession!!
                            val present = latestPresentCount
                            val total = if (latestTotalCount > 0) latestTotalCount else 30
                            val pct = if (total > 0) (present * 100) / total else 0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Latest Attendance",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimaryBlue,
                                        fontSize = 13.sp
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PaletteLightBlue)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "COMPLETED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPrimaryBlue
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = lastSession.subject.ifBlank { lastSession.className.ifBlank { "Class Session" } },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 20.sp
                                )
                            )

                            Text(
                                text = if (lastSession.className.isNotBlank() && lastSession.classId.isNotBlank()) "${lastSession.className} • ${lastSession.classId}" else lastSession.classId.ifBlank { lastSession.className },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandPrimaryBlue,
                                    fontSize = 14.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "${lastSession.date}  •  ${lastSession.startTime} – ${lastSession.endTime}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Stats Grid: Present, Percentage, Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Present", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = "$present / $total Present",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = StatusPresentGreenText
                                    )
                                }
                                Column {
                                    Text("Attendance", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = "$pct%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BrandPrimaryBlue
                                    )
                                }
                                Column {
                                    Text("Status", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = "Completed",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    onNavigateToSessionDetails(lastSession.sessionId)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_view_last_attendance"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Attendance", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEBF5FF))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Classroom BLE Scanner Active • Listening for ESP32 beacon",
                                    fontSize = 12.sp,
                                    color = BrandPrimaryBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Classroom Attendance",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 19.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "No active session detected. Press START on the classroom ESP32 device to start live attendance session.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            OutlinedButton(
                                onClick = onNavigateToHistory,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_view_history_from_card"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = BrandPrimaryBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Attendance History", fontWeight = FontWeight.SemiBold, color = BrandPrimaryBlue)
                            }
                        }
                    }
                }
            }

            // Quick Actions 3-Column: My Classes / Students / Reports
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // My Classes Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                            .clickable(onClick = onNavigateToClasses)
                            .testTag("quick_action_my_classes"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PaletteLightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "My Classes",
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Classes",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${classes.size.coerceAtLeast(schedule.size)} Enrolled",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Students Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                            .clickable(onClick = onNavigateToStudents)
                            .testTag("quick_action_students"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PaletteLightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = "Students",
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Students",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${students.size} Active",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Reports Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                            .clickable(onClick = onNavigateToReports)
                            .testTag("quick_action_reports"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PaletteLightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "Reports",
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Reports",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Analytics",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Attendance Sessions / Today's Schedule Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (facultySessions.isNotEmpty()) "Recent Sessions" else "Assigned Schedule",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    )

                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BrandPrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )
                }
            }

            if (facultySessions.isNotEmpty()) {
                items(facultySessions, key = { it.sessionId }) { session ->
                    SessionRowItem(
                        session = session,
                        onClick = { onNavigateToSessionDetails(session.sessionId) }
                    )
                }
            } else if (schedule.isNotEmpty()) {
                items(schedule) { item ->
                    ScheduleRowItem(
                        item = item,
                        onClick = onNavigateToClasses
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No attendance records found.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SessionRowItem(
    session: AttendanceSession,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("session_item_${session.sessionId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.classId,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimaryBlue,
                            fontSize = 15.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${session.subject}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${session.room}  •  ${session.startTime}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                )
            }

            val isActive = session.status.equals("ACTIVE", ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) StatusPresentGreenBg else PaletteLightBlue)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = session.status,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) StatusPresentGreenText else BrandPrimaryBlue,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ScheduleRowItem(
    item: ClassScheduleItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("schedule_item_${item.classCode}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.classCode,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimaryBlue,
                            fontSize = 15.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${item.subject}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${item.room}  •  ${item.timeSlot}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaletteLightBlue)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = item.status,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = BrandPrimaryBlue,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceRecord
import com.example.model.AttendanceSession
import com.example.ui.components.FacultyBottomNavigationBar
import com.example.ui.components.FacultyBottomNavTab
import com.example.ui.components.bouncingClickable
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.PaletteGray
import com.example.ui.theme.PaletteLightBlue
import com.example.ui.theme.StatusAbsentRedBg
import com.example.ui.theme.StatusAbsentRedText
import com.example.ui.theme.StatusPresentGreenBg
import com.example.ui.theme.StatusPresentGreenText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch

@Composable
fun FacultyAttendanceDetailsScreen(
    sessionId: String,
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: (() -> Unit)? = null,
    onNavigateToClasses: (() -> Unit)? = null,
    onNavigateToHistory: (() -> Unit)? = null,
    onNavigateToProfile: (() -> Unit)? = null,
    onNavigateToStudents: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var session by remember { mutableStateOf<AttendanceSession?>(null) }
    var records by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(sessionId) {
        isLoading = true
        val result = viewModel.getSessionAttendanceDetails(sessionId)
        session = result.first
        records = result.second
        isLoading = false
    }

    Scaffold(
        bottomBar = {
            if (onNavigateToHome != null && onNavigateToClasses != null && onNavigateToHistory != null && onNavigateToProfile != null) {
                FacultyBottomNavigationBar(
                    currentTab = FacultyBottomNavTab.HISTORY,
                    onTabSelected = { tab ->
                        when (tab) {
                            FacultyBottomNavTab.HOME -> onNavigateToHome()
                            FacultyBottomNavTab.CLASSES -> onNavigateToClasses()
                            FacultyBottomNavTab.STUDENTS -> onNavigateToStudents?.invoke() ?: onNavigateToHome()
                            FacultyBottomNavTab.HISTORY -> onNavigateToHistory()
                            FacultyBottomNavTab.PROFILE -> onNavigateToProfile()
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("details_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attendance Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = session?.subject?.ifBlank { "Class Session" } ?: "Class Session",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        containerColor = SurfaceWhite
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = BrandPrimaryBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading session details...", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            val currentSession = session ?: AttendanceSession(
                sessionId = sessionId,
                classId = "CN-301",
                subject = "Classroom Attendance",
                room = "CN-301",
                startTime = "10:00 AM",
                endTime = "11:00 AM",
                bleIdentifier = "BLE_Attendance",
                status = "COMPLETED",
                deviceId = "CLASSROOM_ESP32_01"
            )

            val totalExpected = if (currentSession.totalStudents > 0) currentSession.totalStudents else maxOf(records.size, 1)
            val presentCount = records.count { it.status.equals("PRESENT", ignoreCase = true) }
            val absentCount = maxOf(0, totalExpected - presentCount)
            val rate = if (totalExpected > 0) (presentCount * 100) / totalExpected else 0

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Overview Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                            .testTag("details_overview_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentSession.classId.ifBlank { "Class" },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimaryBlue,
                                        fontSize = 16.sp
                                    )
                                )

                                val isActive = currentSession.status.equals("ACTIVE", ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isActive) StatusPresentGreenBg else PaletteLightBlue)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isActive) "LIVE ACTIVE" else "COMPLETED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) StatusPresentGreenText else BrandPrimaryBlue,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = currentSession.subject.ifBlank { "Classroom Attendance" },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 19.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = SurfaceBorder)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Metadata Grid
                            InfoRow(
                                icon = Icons.Default.Person,
                                label = "Faculty",
                                value = currentSession.professorName.ifBlank { "Assigned Faculty" }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            InfoRow(
                                icon = Icons.Default.LocationOn,
                                label = "Room",
                                value = currentSession.room.ifBlank { "Classroom" }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            InfoRow(
                                icon = Icons.Default.CalendarToday,
                                label = "Date",
                                value = currentSession.date.ifBlank { "Today" }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            InfoRow(
                                icon = Icons.Default.Schedule,
                                label = "Time Slot",
                                value = "${currentSession.startTime} – ${currentSession.endTime}"
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            InfoRow(
                                icon = Icons.Default.Bluetooth,
                                label = "Device",
                                value = "${currentSession.bleIdentifier} (${currentSession.deviceId})"
                            )
                        }
                    }
                }

                // Attendance Stats Card (Clean 4-column metrics that NEVER wrap)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Total",
                            value = "$totalExpected",
                            valueColor = TextPrimary,
                            bgColor = PaletteGray,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Present",
                            value = "$presentCount",
                            valueColor = StatusPresentGreenText,
                            bgColor = StatusPresentGreenBg,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Absent",
                            value = "$absentCount",
                            valueColor = StatusAbsentRedText,
                            bgColor = StatusAbsentRedBg,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Rate",
                            value = "$rate%",
                            valueColor = BrandPrimaryBlue,
                            bgColor = PaletteLightBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Action Buttons: Export PDF, Share PDF & Export CSV
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Exporting PDF report...")
                                        val opened = viewModel.generateAndOpenPdfReport(currentSession, records, context)
                                        if (opened) {
                                            snackbarHostState.showSnackbar("Opening PDF report...")
                                        } else {
                                            snackbarHostState.showSnackbar("PDF report exported successfully!")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_export_pdf"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Export PDF",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val shared = viewModel.sharePdfReport(currentSession, records, context)
                                        if (!shared) {
                                            snackbarHostState.showSnackbar("Failed to prepare PDF for sharing")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_share_pdf_report"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Share PDF",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = BrandPrimaryBlue,
                                    maxLines = 1
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val file = viewModel.exportSessionReportToCsv(currentSession, records, context)
                                scope.launch {
                                    if (file != null) {
                                        snackbarHostState.showSnackbar("CSV saved: ${file.name}")
                                    } else {
                                        snackbarHostState.showSnackbar("CSV report exported successfully!")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_export_csv"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export Spreadsheet (CSV)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Student Attendance List Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Student Attendance",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 17.sp
                            )
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PaletteLightBlue)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${records.size} Recorded",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BrandPrimaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                if (records.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No student attendance recorded yet.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(records, key = { it.attendanceId }) { record ->
                        StudentRecordCard(record = record)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandPrimaryBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                fontSize = 13.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = valueColor,
                        fontSize = if (value.length > 3) 15.sp else 18.sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Modern, beautifully aligned Student Record Card.
 * Eliminates cramped table columns and wraps all data cleanly.
 */
@Composable
private fun StudentRecordCard(record: AttendanceRecord) {
    val isPresent = record.status.equals("PRESENT", ignoreCase = true)
    val initials = record.studentName.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "ST" }
        .uppercase()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .testTag("record_item_${record.attendanceId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Student Initials Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isPresent) StatusPresentGreenBg else PaletteGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isPresent) StatusPresentGreenText else TextMuted
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Student Information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.studentName.ifBlank { "Student ${record.enrollmentNo}" },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Enrollment: ${record.enrollmentNo.ifBlank { record.studentUid }}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (record.faceVerified || record.bleVerified) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (record.bleVerified) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Text("BLE Verified", fontSize = 10.sp, color = BrandPrimaryBlue)
                        }
                        if (record.faceVerified) {
                            if (record.bleVerified) Text("•", fontSize = 10.sp, color = TextMuted)
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = StatusPresentGreenText,
                                modifier = Modifier.size(12.dp)
                            )
                            Text("Face Match", fontSize = 10.sp, color = StatusPresentGreenText)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Badge & Time
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isPresent) StatusPresentGreenBg else StatusAbsentRedBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isPresent) "PRESENT" else "ABSENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isPresent) StatusPresentGreenText else StatusAbsentRedText,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }

                if (record.time.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = record.time,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

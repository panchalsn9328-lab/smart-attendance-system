package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FacultyBottomNavigationBar
import com.example.ui.components.FacultyBottomNavTab
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.StatusAbsentRedBg
import com.example.ui.theme.StatusAbsentRedText
import com.example.ui.theme.StatusPresentGreenBg
import com.example.ui.theme.StatusPresentGreenText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AttendanceViewModel

@Composable
fun FacultySessionActiveScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToClasses: () -> Unit,
    onNavigateToLiveAttendance: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val session by viewModel.currentActiveSession.collectAsState()
    val studentList by viewModel.studentLiveList.collectAsState()
    val presentCount = studentList.count { it.isPresent }

    var showConfirmEndDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            FacultyBottomNavigationBar(
                currentTab = FacultyBottomNavTab.HISTORY,
                onTabSelected = { tab ->
                    when (tab) {
                        FacultyBottomNavTab.HOME -> onNavigateToHome()
                        FacultyBottomNavTab.CLASSES -> onNavigateToClasses()
                        FacultyBottomNavTab.STUDENTS -> onNavigateToClasses()
                        FacultyBottomNavTab.HISTORY -> onNavigateToLiveAttendance()
                        FacultyBottomNavTab.PROFILE -> onNavigateToProfile()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceWhite)
                .statusBarsPadding()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("faculty_session_active_screen")
        ) {
            // Header: Back Arrow & Title (Screen 13)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("session_active_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Attendance Session",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Banner: Session is ACTIVE (Exact matching Screen 13)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("session_active_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSessionActive) StatusPresentGreenBg else StatusAbsentRedBg
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSessionActive) StatusPresentGreenText else StatusAbsentRedText),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSessionActive) Icons.Default.CheckCircle else Icons.Default.StopCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = if (isSessionActive) "Session is ACTIVE" else "Session is COMPLETED",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSessionActive) StatusPresentGreenText else StatusAbsentRedText,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSessionActive)
                                "Students can now mark attendance."
                            else
                                "Attendance window is closed.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isSessionActive) StatusPresentGreenText else StatusAbsentRedText,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Details Card (Screen 13)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Session Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 17.sp
                        )
                    )

                    HorizontalDivider(color = SurfaceBorder)

                    val classLabel = if (session.classId.isNotBlank() && session.className.isNotBlank()) {
                        "${session.className} (${session.classId})"
                    } else if (session.classId.isNotBlank()) {
                        session.classId
                    } else {
                        session.subject.ifBlank { "Assigned Class" }
                    }
                    DetailRow(label = "Class", value = classLabel)
                    DetailRow(label = "Room", value = session.room.ifBlank { "Classroom" })
                    DetailRow(label = "Device", value = session.deviceName.ifBlank { session.bleIdentifier.ifBlank { "BLE_Attendance" } })
                    DetailRow(label = "Device ID", value = session.deviceId.ifBlank { "CLASSROOM_ESP32_01" }, isMono = true)
                    DetailRow(label = "Session Token", value = session.sessionToken.ifBlank { "--" }, isMono = true)
                    val timeDisplay = if (session.startTime.isNotBlank()) session.startTime else "Ongoing"
                    DetailRow(label = "Time", value = timeDisplay)
                    val totalExpected = if (session.totalStudents > 0) session.totalStudents else studentList.size.coerceAtLeast(1)
                    DetailRow(label = "Marked Present", value = "$presentCount / $totalExpected students")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Attendance Shortcut Button
            OutlinedButton(
                onClick = onNavigateToLiveAttendance,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_session_view_live_list"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = BrandPrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Live Attendance ($presentCount Present)",
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // End Session Red Button (Screen 13)
            if (isSessionActive) {
                Button(
                    onClick = { showConfirmEndDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_end_session"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626) // Red button matching wireframe
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "End Session",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onNavigateToLiveAttendance,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_view_history_session"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrandPrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Attendance Completed",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimaryBlue,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showConfirmEndDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmEndDialog = false },
                title = { Text("End Attendance Session?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to end this attendance session? Students will no longer be able to check in via BLE.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmEndDialog = false
                            viewModel.endFacultySession()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("End Session", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmEndDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                fontSize = 14.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontSize = 14.sp,
                fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default
            )
        )
    }
}

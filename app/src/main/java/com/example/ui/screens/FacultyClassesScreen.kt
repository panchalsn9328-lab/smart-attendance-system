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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClassItem
import com.example.ui.components.FacultyBottomNavigationBar
import com.example.ui.components.FacultyBottomNavTab
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.PaletteLightBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AttendanceViewModel

@Composable
fun FacultyClassesScreen(
    viewModel: AttendanceViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToReports: (String?) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToStudents: (() -> Unit)? = null
) {
    val classes by viewModel.classesList.collectAsState()
    val schedule by viewModel.facultySchedule.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadClassesAndDevices()
        viewModel.faculty.value?.let { fac ->
            val fid = fac.facultyId.ifBlank { fac.id }
            viewModel.loadFacultyData(fid)
        }
    }

    Scaffold(
        bottomBar = {
            FacultyBottomNavigationBar(
                currentTab = FacultyBottomNavTab.CLASSES,
                onTabSelected = { tab ->
                    when (tab) {
                        FacultyBottomNavTab.HOME -> onNavigateToHome()
                        FacultyBottomNavTab.CLASSES -> {}
                        FacultyBottomNavTab.STUDENTS -> onNavigateToStudents?.invoke() ?: onNavigateToHome()
                        FacultyBottomNavTab.HISTORY -> onNavigateToHistory()
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
                .testTag("faculty_classes_screen")
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "My Classes",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Assigned classes and attendance records (ESP32 device controlled)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (classes.isEmpty() && schedule.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No classes assigned.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (classes.isNotEmpty()) {
                        items(classes, key = { it.classId }) { cls ->
                            ClassItemCard(
                                cls = cls,
                                onViewReports = { onNavigateToReports(cls.classId) },
                                onViewHistory = onNavigateToHistory
                            )
                        }
                    } else {
                        items(schedule) { s ->
                            val clsItem = ClassItem(
                                classId = s.classCode,
                                className = s.subject,
                                subject = s.subject,
                                room = s.room,
                                enrolledCount = 30
                            )
                            ClassItemCard(
                                cls = clsItem,
                                onViewReports = { onNavigateToReports(clsItem.classId) },
                                onViewHistory = onNavigateToHistory
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassItemCard(
    cls: ClassItem,
    onViewReports: () -> Unit,
    onViewHistory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .testTag("class_card_${cls.classId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PaletteLightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = cls.classId,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimaryBlue,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = cls.subject.ifBlank { cls.className },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                        )
                    }
                }

                if (cls.enrolledCount > 0) {
                    Text(
                        text = "${cls.enrolledCount} Students",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Room ${cls.room.ifBlank { "CN-301" }}  •  Dept: ${cls.department.ifBlank { "CSE" }}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onViewReports,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_class_reports_${cls.classId}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Reports", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_class_history_${cls.classId}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BrandPrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("History", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrandPrimaryBlue)
                }
            }
        }
    }
}

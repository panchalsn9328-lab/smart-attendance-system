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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StudentAttendanceStatus
import com.example.ui.components.FacultyBottomNavigationBar
import com.example.ui.components.FacultyBottomNavTab
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.PaletteGray
import com.example.ui.theme.PaletteLightBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AttendanceViewModel

@Composable
fun FacultyLiveAttendanceScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToClasses: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val studentList by viewModel.studentLiveList.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Present, 1 = Absent
    var searchQuery by remember { mutableStateOf("") }

    val presentStudents = studentList.filter { it.isPresent }
    val absentStudents = studentList.filter { !it.isPresent }

    val displayedStudents = (if (selectedTab == 0) presentStudents else absentStudents).filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.rollNo.contains(searchQuery, ignoreCase = true)
    }

    val progressFraction = if (studentList.isNotEmpty()) {
        presentStudents.size.toFloat() / studentList.size.toFloat()
    } else 0f

    Scaffold(
        bottomBar = {
            FacultyBottomNavigationBar(
                currentTab = FacultyBottomNavTab.HISTORY,
                onTabSelected = { tab ->
                    when (tab) {
                        FacultyBottomNavTab.HOME -> onNavigateToHome()
                        FacultyBottomNavTab.CLASSES -> onNavigateToClasses()
                        FacultyBottomNavTab.STUDENTS -> onNavigateToClasses()
                        FacultyBottomNavTab.HISTORY -> {}
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
                .testTag("faculty_live_attendance_screen")
        ) {
            // Header: Back Arrow, "Live Attendance", Filter Icon (Screen 12)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("live_attendance_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Live Attendance",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 20.sp
                        )
                    )
                }

                IconButton(
                    onClick = { /* Filter */ },
                    modifier = Modifier.testTag("live_attendance_filter_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gauge Card: "24 / 30 Present" (Screen 12)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CN-301 Computer Networks",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${presentStudents.size}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp,
                                    color = BrandPrimaryBlue
                                )
                            )
                            Text(
                                text = " / ${studentList.size}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = TextSecondary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Students Present",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        )
                    }

                    // Circular Progress Dial
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(76.dp),
                            color = PaletteLightBlue,
                            strokeWidth = 7.dp
                        )
                        CircularProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.size(76.dp),
                            color = BrandPrimaryBlue,
                            strokeWidth = 7.dp
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BrandPrimaryBlue
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Tab Selector: [ Present (24) ] [ Absent (6) ]
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceWhite,
                contentColor = BrandPrimaryBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BrandPrimaryBlue
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Present (${presentStudents.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Absent (${absentStudents.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Student Box (Screen 12)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search student by name or roll no...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_student_search"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = PaletteGray,
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Student List Rows (Screen 12)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedStudents, key = { it.studentId }) { student ->
                    StudentLiveRowItem(
                        student = student,
                        onToggle = { viewModel.toggleStudentAttendance(student.studentId) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StudentLiveRowItem(
    student: StudentAttendanceStatus,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .testTag("student_row_${student.rollNo}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circle Avatar with Initials (e.g. SP, AR, KM)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (student.isPresent) PaletteLightBlue else PaletteGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (student.isPresent) BrandPrimaryBlue else TextSecondary,
                            fontSize = 15.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = student.rollNo,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            // Status Badge
            StatusBadge(status = if (student.isPresent) "Present" else "Absent")
        }
    }
}

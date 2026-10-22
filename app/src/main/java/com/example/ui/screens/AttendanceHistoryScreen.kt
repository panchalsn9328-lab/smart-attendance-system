package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceRecord
import com.example.model.AttendanceSession
import com.example.model.UserRole
import com.example.ui.components.BottomNavTab
import com.example.ui.components.FacultyBottomNavigationBar
import com.example.ui.components.FacultyBottomNavTab
import com.example.ui.components.SmartBottomNavigationBar
import com.example.ui.components.SmartTopBar
import com.example.ui.components.StatusBadge
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

@Composable
fun AttendanceHistoryScreen(
    records: List<AttendanceRecord> = emptyList(),
    facultySessions: List<AttendanceSession> = emptyList(),
    facultyTimeFilter: String = "All",
    onFacultyTimeFilterChange: (String) -> Unit = {},
    facultySearchQuery: String = "",
    onFacultySearchQueryChange: (String) -> Unit = {},
    facultyClassFilter: String? = null,
    onFacultyClassFilterChange: (String?) -> Unit = {},
    availableClasses: List<String> = emptyList(),
    selectedFilter: String = "All",
    onFilterChange: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToClasses: (() -> Unit)? = null,
    onNavigateToStudents: (() -> Unit)? = null,
    onSessionClick: ((String) -> Unit)? = null,
    userRole: UserRole? = UserRole.STUDENT
) {
    val isFaculty = userRole == UserRole.FACULTY

    Scaffold(
        topBar = {
            if (isFaculty) {
                // Faculty Top Bar with title & Search
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceWhite)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("history_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Conducted Lectures",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Lectures & Attendance Sessions Conducted",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search bar: Search by Class, Subject, Date
                    OutlinedTextField(
                        value = facultySearchQuery,
                        onValueChange = onFacultySearchQueryChange,
                        placeholder = { Text("Search by Class, Subject, Date...", fontSize = 14.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
                        },
                        trailingIcon = {
                            if (facultySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { onFacultySearchQueryChange("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("history_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Time Filters: All, This Week, This Month
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "This Week", "This Month").forEach { timeOption ->
                            val isSelected = facultyTimeFilter.equals(timeOption, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) BrandPrimaryBlue else PaletteLightBlue)
                                    .clickable { onFacultyTimeFilterChange(timeOption) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .testTag("filter_time_$timeOption"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = timeOption,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else BrandPrimaryBlue
                                )
                            }
                        }
                    }

                    // Optional Class Filter Chips
                    if (availableClasses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val classList = listOf("All") + availableClasses
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(classList) { classCode ->
                                val isSelected = (facultyClassFilter == null && classCode == "All") ||
                                        (facultyClassFilter.equals(classCode, ignoreCase = true))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) PaletteGray else SurfaceCard)
                                        .border(1.dp, if (isSelected) BrandPrimaryBlue else SurfaceBorder, RoundedCornerShape(16.dp))
                                        .clickable {
                                            if (classCode == "All") onFacultyClassFilterChange(null)
                                            else onFacultyClassFilterChange(classCode)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = classCode,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) BrandPrimaryBlue else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                SmartTopBar(
                    title = "My Attendance History",
                    onBackClick = onBackClick,
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        bottomBar = {
            if (isFaculty) {
                FacultyBottomNavigationBar(
                    currentTab = FacultyBottomNavTab.HISTORY,
                    onTabSelected = { tab ->
                        when (tab) {
                            FacultyBottomNavTab.HOME -> onNavigateToHome()
                            FacultyBottomNavTab.CLASSES -> onNavigateToClasses?.invoke() ?: onNavigateToHome()
                            FacultyBottomNavTab.STUDENTS -> onNavigateToStudents?.invoke() ?: onNavigateToHome()
                            FacultyBottomNavTab.HISTORY -> {}
                            FacultyBottomNavTab.PROFILE -> onNavigateToProfile()
                        }
                    }
                )
            } else {
                SmartBottomNavigationBar(
                    currentTab = BottomNavTab.HISTORY,
                    onTabSelected = { tab ->
                        when (tab) {
                            BottomNavTab.HOME -> onNavigateToHome()
                            BottomNavTab.HISTORY -> { /* already here */ }
                            BottomNavTab.PROFILE -> onNavigateToProfile()
                        }
                    },
                    modifier = Modifier.testTag("history_bottom_nav")
                )
            }
        },
        containerColor = SurfaceWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isFaculty) {
                // FACULTY HISTORY: CONDUCTED LECTURES / SESSIONS
                if (facultySessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.EventBusy,
                                contentDescription = "No Conducted Lectures",
                                tint = TextMuted,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No conducted lecture sessions found.",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Completed ESP32 classroom attendance sessions will appear here automatically from Firebase.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(facultySessions, key = { it.sessionId }) { session ->
                            FacultyConductedLectureCard(
                                session = session,
                                onViewDetails = {
                                    onSessionClick?.invoke(session.sessionId)
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            } else {
                // STUDENT HISTORY: Strictly Personal Attendance Records
                val totalLectures = records.size
                val presentLectures = records.count { it.status.equals("PRESENT", ignoreCase = true) }
                val attendancePct = if (totalLectures > 0) (presentLectures * 100) / totalLectures else 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("student_attendance_summary_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaletteLightBlue)
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
                            Column {
                                Text(
                                    text = "Your Overall Attendance",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandPrimaryBlue
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$presentLectures of $totalLectures Lectures",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Text(
                                text = "$attendancePct%",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (attendancePct >= 75) StatusPresentGreenText else StatusAbsentRedText
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { if (totalLectures > 0) presentLectures.toFloat() / totalLectures else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (attendancePct >= 75) StatusPresentGreenText else StatusAbsentRedText,
                            trackColor = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Filter Pills Row (All, Present, Absent)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("All", "Present", "Absent").forEach { filter ->
                        val isSelected = selectedFilter.equals(filter, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) BrandPrimaryBlue else SurfaceCard)
                                .clickable { onFilterChange(filter) }
                                .padding(horizontal = 22.dp, vertical = 8.dp)
                                .testTag("filter_chip_$filter"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (records.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.EventBusy,
                                contentDescription = "No Records",
                                tint = TextMuted,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No personal attendance records found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(records, key = { it.attendanceId }) { record ->
                            StudentPersonalAttendanceCard(record = record)
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Faculty Conducted Lecture Card matching Requirement 4:
 *
 * Example:
 * 19 Aug 2026
 * Computer Networks
 * CN-301
 * 10:02 AM – 10:48 AM
 * Students Present: 24
 * Students Absent: 6
 * Total Students: 30
 * [View Details]
 */
@Composable
fun FacultyConductedLectureCard(
    session: AttendanceSession,
    onViewDetails: () -> Unit
) {
    val totalStudents = if (session.totalStudents > 0) session.totalStudents else 30
    val presentCount = session.presentCount
    val absentCount = if (session.absentCount > 0) session.absentCount else (totalStudents - presentCount).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .testTag("session_card_${session.sessionId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Date & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = BrandPrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = session.date.ifBlank { "19 Aug 2026" },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandPrimaryBlue
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (session.status.equals("COMPLETED", ignoreCase = true)) PaletteLightBlue else StatusPresentGreenBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = session.status.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (session.status.equals("COMPLETED", ignoreCase = true)) BrandPrimaryBlue else StatusPresentGreenText
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subject & Class
            Text(
                text = session.subject.ifBlank { "Computer Networks" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${session.classId}  •  Room: ${session.room}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Time Slot
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${session.startTime} – ${session.endTime}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SurfaceBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Metrics row: Students Present: 24 | Students Absent: 6 | Total: 30
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Students Present: $presentCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusPresentGreenText
                    )
                    Text(
                        text = "Students Absent: $absentCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusAbsentRedText
                    )
                    Text(
                        text = "Total Students: $totalStudents",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_view_details_${session.sessionId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun StudentPersonalAttendanceCard(
    record: AttendanceRecord,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("student_history_record_${record.attendanceId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.subject.ifBlank { "Classroom Attendance" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${record.date}  •  ${record.time}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            StatusBadge(status = record.status)
        }
    }
}

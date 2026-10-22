package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClassItem
import com.example.model.Esp32Device
import com.example.ui.theme.BleBlue
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
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    viewModel: AttendanceViewModel,
    onLogout: () -> Unit
) {
    val adminStats by viewModel.adminStats.collectAsState()
    val classes by viewModel.classesList.collectAsState()
    val devices by viewModel.devicesList.collectAsState()
    val students by viewModel.studentsList.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Students", "Faculty", "Classes", "ESP32 Devices", "Export Reports")

    var showAddFacultyDialog by remember { mutableStateOf(false) }
    var showAddClassDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadStudents()
        viewModel.loadFaculty()
        viewModel.loadClassesAndDevices()
        viewModel.refreshAdminStats()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PaletteLightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin",
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Admin Dashboard",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "System Management & Rosters",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BrandPrimaryBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                IconButton(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.testTag("btn_admin_logout")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = TextSecondary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceCard)
                .padding(paddingValues)
                .navigationBarsPadding()
        ) {
            // Tab Row with smooth animated pill indicator
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceWhite,
                contentColor = BrandPrimaryBlue,
                edgePadding = 16.dp,
                divider = {
                    androidx.compose.material3.HorizontalDivider(
                        color = PaletteLightBlue,
                        thickness = 1.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val tabScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isSelected) 1.04f else 1.0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                        ),
                        label = "tab_scale_$index"
                    )
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = if (isSelected) 14.5.sp else 13.5.sp,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = tabScale
                                    scaleY = tabScale
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> (width * 0.25f).toInt() } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally { width -> -(width * 0.25f).toInt() } + fadeOut(tween(180)))
                    } else {
                        (slideInHorizontally { width -> -(width * 0.25f).toInt() } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally { width -> (width * 0.25f).toInt() } + fadeOut(tween(180)))
                    }
                },
                label = "admin_tab_content",
                modifier = Modifier.fillMaxSize()
            ) { currentTab ->
                when (currentTab) {
                    0 -> AdminOverviewContent(
                        stats = adminStats,
                        onManageStudents = { selectedTab = 1 },
                        onManageFaculty = { selectedTab = 2 },
                        onManageClasses = { selectedTab = 3 }
                    )
                    1 -> AdminStudentsManagementContent(
                        students = students
                    )
                    2 -> AdminFacultyManagementContent(
                        viewModel = viewModel,
                        onOpenAddDialog = { showAddFacultyDialog = true }
                    )
                    3 -> AdminClassesContent(
                        classes = classes,
                        onOpenAddClass = { showAddClassDialog = true }
                    )
                    4 -> AdminDevicesContent(
                        devices = devices
                    )
                    5 -> AdminReportsContent(
                        onExportReport = {
                            val file = viewModel.exportAttendanceReport(context)
                            scope.launch {
                                if (file != null) {
                                    snackbarHostState.showSnackbar("Attendance report exported to ${file.name}")
                                } else {
                                    snackbarHostState.showSnackbar("Attendance report generated from Firestore data")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Dialog: Admin Create Faculty Account
    if (showAddFacultyDialog) {
        var facName by remember { mutableStateOf("") }
        var facId by remember { mutableStateOf("") }
        var facEmail by remember { mutableStateOf("") }
        var facDept by remember { mutableStateOf("Computer Science & Engineering") }
        var facPassword by remember { mutableStateOf("") }
        var isFacPassVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddFacultyDialog = false },
            title = {
                Text(
                    text = "Add Faculty Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Faculty accounts are created ONLY by Admin.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    OutlinedTextField(
                        value = facName,
                        onValueChange = { facName = it },
                        label = { Text("Faculty Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = facId,
                        onValueChange = { facId = it },
                        label = { Text("Faculty ID (e.g. TCH002)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = facEmail,
                        onValueChange = { facEmail = it },
                        label = { Text("College Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = facDept,
                        onValueChange = { facDept = it },
                        label = { Text("Department") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = facPassword,
                        onValueChange = { facPassword = it },
                        label = { Text("Initial Login Password") },
                        placeholder = { Text("At least 6 characters") },
                        singleLine = true,
                        visualTransformation = if (isFacPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { isFacPassVisible = !isFacPassVisible }) {
                                Icon(
                                    imageVector = if (isFacPassVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanPass = facPassword.trim().ifBlank { "Pass@123" }
                        if (facName.isNotBlank() && facId.isNotBlank() && facEmail.isNotBlank()) {
                            viewModel.createFacultyByAdmin(facName, facId, facEmail, facDept, cleanPass) { success, msg ->
                                if (success) {
                                    showAddFacultyDialog = false
                                    scope.launch { snackbarHostState.showSnackbar("Faculty account created in Firestore!") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar(msg ?: "Error creating faculty") }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
                ) {
                    Text("Create Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFacultyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Add Class
    if (showAddClassDialog) {
        var clsCode by remember { mutableStateOf("") }
        var clsName by remember { mutableStateOf("") }
        var clsRoom by remember { mutableStateOf("") }
        var clsFacultyId by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddClassDialog = false },
            title = {
                Text(
                    text = "Add Classroom Class",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = clsCode,
                        onValueChange = { clsCode = it },
                        label = { Text("Class Code (e.g. IT-401)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clsName,
                        onValueChange = { clsName = it },
                        label = { Text("Subject / Class Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clsRoom,
                        onValueChange = { clsRoom = it },
                        label = { Text("Room No (e.g. IT-204)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clsFacultyId,
                        onValueChange = { clsFacultyId = it },
                        label = { Text("Assigned Faculty ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clsCode.isNotBlank() && clsName.isNotBlank()) {
                            val newClass = ClassItem(
                                classId = clsCode.trim(),
                                className = clsName.trim(),
                                subject = clsName.trim(),
                                room = clsRoom.trim(),
                                facultyId = clsFacultyId.trim(),
                                department = "Computer Science",
                                semester = "6th Semester"
                            )
                            viewModel.createClassByAdmin(newClass) { success, msg ->
                                if (success) {
                                    showAddClassDialog = false
                                    scope.launch { snackbarHostState.showSnackbar("Class created in Firestore!") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar(msg ?: "Error") }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
                ) {
                    Text("Save Class")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClassDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminOverviewContent(
    stats: com.example.model.AdminStats,
    onManageStudents: () -> Unit,
    onManageFaculty: () -> Unit,
    onManageClasses: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "System Realtime Metrics",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Row 1: Students & Faculty
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminStatCard(
                title = "Total Students",
                value = stats.totalStudents.toString(),
                icon = Icons.Default.School,
                modifier = Modifier.weight(1f)
            )
            AdminStatCard(
                title = "Total Faculty",
                value = stats.totalFaculty.toString(),
                icon = Icons.Default.Person,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: Classes & Active Sessions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminStatCard(
                title = "Total Classes",
                value = stats.totalClasses.toString(),
                icon = Icons.Default.Class,
                modifier = Modifier.weight(1f)
            )
            AdminStatCard(
                title = "Active Sessions",
                value = stats.activeSessions.toString(),
                icon = Icons.Default.Bluetooth,
                accentColor = BleBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 3: Today's Attendance
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Today's Attendance Records",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stats.todayAttendance} Students Verified",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimaryBlue
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusPresentGreenBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Live Firestore",
                        color = StatusPresentGreenText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions
        Text(
            text = "Quick Management",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onManageStudents,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
        ) {
            Icon(imageVector = Icons.Default.School, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Manage Registered Students")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onManageFaculty,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PaletteLightBlue)
        ) {
            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = BrandPrimaryBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Manage Faculty Accounts (Admin Only)", color = BrandPrimaryBlue)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onManageClasses,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PaletteLightBlue)
        ) {
            Icon(imageVector = Icons.Default.Class, contentDescription = null, tint = BrandPrimaryBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Manage Classes & Rosters", color = BrandPrimaryBlue)
        }
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color = BrandPrimaryBlue,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PaletteLightBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 24.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun AdminStudentsManagementContent(
    students: List<com.example.model.Student>
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else students.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.enrollmentNumber.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Registered Students (${students.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Students who register via the app appear here and in the faculty roster.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, enrollment or email...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (students.isEmpty()) "No students registered yet." else "No matching students found.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    )
                    if (students.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "New registrations from the login screen will be listed here automatically.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { st ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
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
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PaletteLightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = st.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                                Text(
                                    text = initials.ifBlank { "ST" }.uppercase(),
                                    color = BrandPrimaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = st.name.ifBlank { st.fullName }.ifBlank { "Student ${st.enrollmentNumber}" },
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    if (st.faceRegistered) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(StatusPresentGreenBg)
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "✓ Face Enrolled",
                                                color = StatusPresentGreenText,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PaletteLightBlue)
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "No Face",
                                                color = BrandPrimaryBlue,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "Enrollment: ${st.enrollmentNumber}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                if (st.email.isNotBlank()) {
                                    Text(
                                        text = st.email,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                if (st.department.isNotBlank()) {
                                    Text(
                                        text = st.department,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BrandPrimaryBlue,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminFacultyManagementContent(
    viewModel: AttendanceViewModel,
    onOpenAddDialog: () -> Unit
) {
    val facultyList by viewModel.facultyList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Faculty Accounts (${facultyList.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Button(
                onClick = onOpenAddDialog,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Faculty")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Faculty accounts are created ONLY by Admin. There is NO public Faculty registration.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (facultyList.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No faculty accounts created yet.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Add Faculty' above to create a faculty account.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(facultyList) { fac ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(PaletteLightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = fac.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                                Text(
                                    text = initials.ifBlank { "FC" }.uppercase(),
                                    color = BrandPrimaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fac.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ID: ${fac.facultyId} • ${fac.email}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${fac.department} • ${fac.designation}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = BrandPrimaryBlue, fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminClassesContent(
    classes: List<ClassItem>,
    onOpenAddClass: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Classrooms & Subjects (${classes.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Button(
                onClick = onOpenAddClass,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Class")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (classes.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Class,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No classes created yet.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Add Class' above to create a new classroom and subject.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(classes) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.classId}: ${item.subject}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (item.room.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PaletteLightBlue)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.room,
                                            color = BrandPrimaryBlue,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Faculty ID: ${item.facultyId.ifBlank { "Unassigned" }} • Department: ${item.department.ifBlank { "General" }} • Semester: ${item.semester.ifBlank { "Current" }}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminDevicesContent(
    devices: List<Esp32Device>
) {
    val displayDevice = devices.firstOrNull() ?: Esp32Device()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "ESP32 Bluetooth Low Energy Hardware",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Configured ESP32 BLE GATT parameters stored in devices/{deviceId}",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
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
                    Text(
                        text = displayDevice.deviceName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimaryBlue
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusPresentGreenBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = StatusPresentGreenText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                DeviceParamRow(label = "Device ID", value = displayDevice.deviceId)
                DeviceParamRow(label = "Assigned Room", value = displayDevice.room)
                DeviceParamRow(label = "Service UUID", value = displayDevice.serviceUuid)
                DeviceParamRow(label = "Status Char UUID", value = displayDevice.statusCharUuid)
                DeviceParamRow(label = "Token Char UUID", value = displayDevice.tokenCharUuid)
                DeviceParamRow(label = "Device ID Char UUID", value = displayDevice.deviceIdCharUuid)
            }
        }
    }
}

@Composable
private fun DeviceParamRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun AdminReportsContent(
    onExportReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Export System Attendance Reports",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Generate and export verified attendance records directly from Firestore single source of truth.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onExportReport,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
        ) {
            Icon(imageVector = Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Attendance Report (CSV / PDF)", fontWeight = FontWeight.SemiBold)
        }
    }
}

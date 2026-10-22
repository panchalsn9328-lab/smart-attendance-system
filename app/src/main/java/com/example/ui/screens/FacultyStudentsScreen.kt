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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Student
import com.example.ui.components.FacultyBottomNavigationBar
import com.example.ui.components.FacultyBottomNavTab
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.PaletteGray
import com.example.ui.theme.PaletteLightBlue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyStudentsScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToClasses: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val students by viewModel.studentsList.collectAsState()
    val classes by viewModel.classesList.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf("ALL") }
    var showAddStudentSheet by remember { mutableStateOf(false) }
    var studentToView by remember { mutableStateOf<Student?>(null) }
    var studentToEdit by remember { mutableStateOf<Student?>(null) }
    var studentToRemove by remember { mutableStateOf<Student?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadStudents()
    }

    // Filter students: Name, Enrollment Number, Email, and selected class (Normalized matching)
    val filteredStudents = remember(students, searchQuery, selectedClassFilter) {
        val cleanFilter = selectedClassFilter.trim().replace(" ", "").replace("-", "").uppercase()
        students.filter { student ->
            val matchesSearch = searchQuery.isBlank() ||
                    student.fullName.contains(searchQuery, ignoreCase = true) ||
                    student.name.contains(searchQuery, ignoreCase = true) ||
                    student.enrollmentNumber.contains(searchQuery, ignoreCase = true) ||
                    student.email.contains(searchQuery, ignoreCase = true)

            val studentClassClean = student.classId.trim().replace(" ", "").replace("-", "").uppercase()
            val matchesClass = cleanFilter == "ALL" ||
                    cleanFilter.isBlank() ||
                    studentClassClean == cleanFilter ||
                    student.effectiveClassIds.any { it.trim().replace(" ", "").replace("-", "").uppercase() == cleanFilter } ||
                    (studentClassClean.isBlank() && cleanFilter == "ALL")

            matchesSearch && matchesClass
        }
    }

    LaunchedEffect(students, filteredStudents) {
        android.util.Log.i("FIREBASE_DEBUG", "[FIREBASE_DEBUG] UI_RENDER screen=FacultyStudentsScreen total_students=${students.size} filtered_count=${filteredStudents.size} displayed_ids=${filteredStudents.map { it.id }}")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            FacultyBottomNavigationBar(
                currentTab = FacultyBottomNavTab.STUDENTS,
                onTabSelected = { tab ->
                    when (tab) {
                        FacultyBottomNavTab.HOME -> onNavigateToHome()
                        FacultyBottomNavTab.CLASSES -> onNavigateToClasses()
                        FacultyBottomNavTab.STUDENTS -> {}
                        FacultyBottomNavTab.HISTORY -> onNavigateToHistory()
                        FacultyBottomNavTab.PROFILE -> onNavigateToProfile()
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddStudentSheet = true },
                containerColor = BrandPrimaryBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_student")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Student")
            }
        },
        topBar = {
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
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("students_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Student Directory",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${filteredStudents.size} enrolled students",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar (Name, Enrollment Number, Email)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, enrollment, email...", fontSize = 13.5.sp, color = TextMuted) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("student_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Class Filter Chips
                val classList = (listOf("ALL") + classes.map { it.classId }.distinct().filter { it.isNotBlank() })
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(classList) { classCode ->
                        val isSelected = selectedClassFilter.equals(classCode, ignoreCase = true)
                        val chipBgColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) BrandPrimaryBlue else PaletteLightBlue,
                            animationSpec = androidx.compose.animation.core.tween(220),
                            label = "chipBg_$classCode"
                        )
                        val chipTextColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) Color.White else BrandPrimaryBlue,
                            animationSpec = androidx.compose.animation.core.tween(220),
                            label = "chipText_$classCode"
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipBgColor)
                                .clickable { selectedClassFilter = classCode }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("filter_class_$classCode"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (classCode == "ALL") "All Classes" else classCode,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = chipTextColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        containerColor = SurfaceWhite
    ) { innerPadding ->
        if (filteredStudents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PaletteLightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "No students enrolled yet" else "No matching students found",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Click '+ Add Student' button below to add students.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredStudents,
                    key = { it.id.ifBlank { it.enrollmentNumber.ifBlank { "${it.name}_${it.email}" } } }
                ) { student ->
                    StudentItemCard(
                        student = student,
                        onView = { studentToView = student },
                        onEdit = { studentToEdit = student },
                        onRemove = { studentToRemove = student }
                    )
                }
            }
        }
    }

    // 1. View Student Dialog
    if (studentToView != null) {
        val s = studentToView!!
        AlertDialog(
            onDismissRequest = { studentToView = null },
            title = {
                Text("Student Details", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow("Full Name", s.fullName.ifBlank { s.name })
                    DetailRow("Enrollment No", s.enrollmentNumber)
                    DetailRow("Email", s.email)
                    DetailRow("Phone", s.phone.ifBlank { "Not provided" })
                    DetailRow("Class / Course", "${s.classId} (${s.className})")
                    DetailRow("Division", s.division.ifBlank { "A" })
                    DetailRow("Degree / Course", s.course)
                    DetailRow("Department", s.department)
                    DetailRow("Semester", s.semester)
                    DetailRow("Status", if (s.isStudentActive) "Active" else "Inactive")
                    DetailRow("Role", s.role)
                }
            },
            confirmButton = {
                Button(onClick = { studentToView = null }) {
                    Text("Close")
                }
            }
        )
    }

    // 2. Edit Student Dialog
    if (studentToEdit != null) {
        val s = studentToEdit!!
        EditStudentDialog(
            student = s,
            onDismiss = { studentToEdit = null },
            onSave = { name, email, phone, classId, className, division, isActive ->
                studentToEdit = null
                viewModel.updateStudent(
                    studentId = s.id,
                    fullName = name,
                    email = email,
                    phone = phone,
                    classId = classId,
                    className = className,
                    division = division,
                    isActive = isActive
                ) { success, error ->
                    scope.launch {
                        if (success) {
                            snackbarHostState.showSnackbar("Student information updated successfully.")
                        } else {
                            snackbarHostState.showSnackbar(error ?: "Failed to update student.")
                        }
                    }
                }
            }
        )
    }

    // 3. Remove Student Confirmation Dialog
    if (studentToRemove != null) {
        val target = studentToRemove!!
        AlertDialog(
            onDismissRequest = { studentToRemove = null },
            title = {
                Text("Remove this student from the class?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to remove ${target.fullName.ifBlank { target.name }} (${target.enrollmentNumber}) from ${target.classId}?",
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Note: Historical attendance records for this student will be preserved. The student will simply be marked inactive.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sid = target.id
                        studentToRemove = null
                        viewModel.removeStudent(sid) { success, error ->
                            scope.launch {
                                if (success) {
                                    snackbarHostState.showSnackbar("Student removed from class. Attendance history preserved.")
                                } else {
                                    snackbarHostState.showSnackbar(error ?: "Failed to remove student.")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAbsentRedText),
                    modifier = Modifier.testTag("confirm_remove_student_btn")
                ) {
                    Text("Remove Student")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Add Student Bottom Sheet / Form
    if (showAddStudentSheet) {
        AddStudentModalSheet(
            availableClasses = classes.map { it.classId }.distinct().filter { it.isNotBlank() },
            isSubmitting = isSubmitting,
            onDismiss = { showAddStudentSheet = false },
            onSubmit = { fullName, enrollment, email, phone, classCourse, division ->
                isSubmitting = true
                viewModel.addStudent(
                    fullName = fullName,
                    enrollmentNumber = enrollment,
                    email = email,
                    phone = phone,
                    classId = classCourse,
                    className = classCourse,
                    division = division
                ) { success, error ->
                    isSubmitting = false
                    if (success) {
                        showAddStudentSheet = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Student added successfully.")
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(error ?: "Failed to add student to Firebase.")
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun StudentItemCard(
    student: Student,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isActive = student.isStudentActive
    val displayName = student.fullName.ifBlank { student.name }.ifBlank { "Student ${student.enrollmentNumber}" }
    val initials = displayName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "ST" }.uppercase()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .testTag("student_card_${student.enrollmentNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isActive) PaletteLightBlue else PaletteGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isActive) BrandPrimaryBlue else TextMuted
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Student Info: Name, Enrollment Number, Email, Class, Status
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) StatusPresentGreenBg else PaletteGray)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isActive) "ACTIVE" else "INACTIVE",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) StatusPresentGreenText else TextMuted,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val classText = if (student.classId.isNotBlank()) " • Class: ${student.classId}" else ""
                Text(
                    text = "Enrollment: ${student.enrollmentNumber}$classText",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (student.email.isNotBlank()) {
                    Text(
                        text = student.email,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // More Options Menu (View, Edit, Remove)
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("btn_student_menu_${student.enrollmentNumber}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextSecondary
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = BrandPrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Details", fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onView()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Student", fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = StatusAbsentRedText, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Remove from Class", color = StatusAbsentRedText, fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStudentModalSheet(
    availableClasses: List<String>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (fullName: String, enrollment: String, email: String, phone: String, classCourse: String, division: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var fullName by remember { mutableStateOf("") }
    var enrollmentNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var classCourse by remember { mutableStateOf(availableClasses.firstOrNull() ?: "") }
    var division by remember { mutableStateOf("A") }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Add Student",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )
            )

            Text(
                text = "Add student record directly to Firebase Firestore database.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = StatusAbsentRedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Full Name (Required)
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    validationError = null
                },
                label = { Text("Full Name *") },
                placeholder = { Text("e.g. Sujal Panchal") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_student_name"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Enrollment Number (Required)
            OutlinedTextField(
                value = enrollmentNumber,
                onValueChange = {
                    enrollmentNumber = it.uppercase()
                    validationError = null
                },
                label = { Text("Enrollment Number *") },
                placeholder = { Text("e.g. 22BCS123") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_student_enrollment"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Email (Required)
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    validationError = null
                },
                label = { Text("Email *") },
                placeholder = { Text("e.g. sujal.panchal@college.edu") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_student_email"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Phone Number (optional)
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number (optional)") },
                placeholder = { Text("e.g. +91 98765 43210") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_student_phone"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Class / Course (Required)
            OutlinedTextField(
                value = classCourse,
                onValueChange = {
                    classCourse = it.uppercase()
                    validationError = null
                },
                label = { Text("Class / Course *") },
                placeholder = { Text("e.g. CN-301") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_student_class"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Division (optional)
            OutlinedTextField(
                value = division,
                onValueChange = { division = it.uppercase() },
                label = { Text("Division (optional)") },
                placeholder = { Text("e.g. A") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_student_division"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val cleanName = fullName.trim()
                    val cleanEnroll = enrollmentNumber.trim()
                    val cleanEmail = email.trim()
                    val cleanClass = classCourse.trim()

                    if (cleanName.isBlank()) {
                        validationError = "Please enter the student's full name."
                        return@Button
                    }
                    if (cleanEnroll.isBlank()) {
                        validationError = "Please enter an enrollment number."
                        return@Button
                    }
                    if (cleanEmail.isBlank()) {
                        validationError = "Please enter an email address."
                        return@Button
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                        validationError = "Please enter a valid email address."
                        return@Button
                    }
                    if (cleanClass.isBlank()) {
                        validationError = "Please enter or select a class / course."
                        return@Button
                    }

                    onSubmit(cleanName, cleanEnroll, cleanEmail, phone.trim(), cleanClass, division.trim().ifBlank { "A" })
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_submit_add_student"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adding to Firebase...")
                } else {
                    Text("Add Student", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun EditStudentDialog(
    student: Student,
    onDismiss: () -> Unit,
    onSave: (fullName: String, email: String, phone: String, classId: String, className: String, division: String, isActive: Boolean) -> Unit
) {
    var fullName by remember { mutableStateOf(student.fullName.ifBlank { student.name }) }
    var email by remember { mutableStateOf(student.email) }
    var phone by remember { mutableStateOf(student.phone) }
    var classId by remember { mutableStateOf(student.classId) }
    var className by remember { mutableStateOf(student.className) }
    var division by remember { mutableStateOf(student.division) }
    var isActive by remember { mutableStateOf(student.isStudentActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Student Information", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Enrollment: ${student.enrollmentNumber} (read-only)",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = classId,
                    onValueChange = { classId = it.uppercase() },
                    label = { Text("Class ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Class Name / Course") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = division,
                    onValueChange = { division = it.uppercase() },
                    label = { Text("Division") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        onSave(fullName.trim(), email.trim(), phone.trim(), classId.trim(), className.trim(), division.trim(), isActive)
                    }
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

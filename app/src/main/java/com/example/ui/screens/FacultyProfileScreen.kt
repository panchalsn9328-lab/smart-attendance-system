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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Faculty
import com.example.ui.components.FacultyBottomNavigationBar
import com.example.ui.components.FacultyBottomNavTab
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.PaletteLightBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch

@Composable
fun FacultyProfileScreen(
    viewModel: AttendanceViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToClasses: () -> Unit,
    onNavigateToLiveAttendance: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToStudents: (() -> Unit)? = null
) {
    val faculty by viewModel.faculty.collectAsState()

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var showPersonalInfoDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            FacultyBottomNavigationBar(
                currentTab = FacultyBottomNavTab.PROFILE,
                onTabSelected = { tab ->
                    when (tab) {
                        FacultyBottomNavTab.HOME -> onNavigateToHome()
                        FacultyBottomNavTab.CLASSES -> onNavigateToClasses()
                        FacultyBottomNavTab.STUDENTS -> onNavigateToStudents?.invoke() ?: onNavigateToHome()
                        FacultyBottomNavTab.HISTORY -> onNavigateToLiveAttendance()
                        FacultyBottomNavTab.PROFILE -> {}
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceWhite)
                .statusBarsPadding()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("faculty_profile_screen")
        ) {
            // Header: Title & Settings gear icon (Screen 15)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 24.sp
                    )
                )

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("faculty_profile_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Profile Card (Screen 15)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(BrandPrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PS",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 22.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = faculty?.name ?: "Prof. Priya Sharma",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Faculty ID: ${faculty?.facultyId ?: "TCH001"}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = faculty?.email ?: "priya.sharma@college.edu",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Menu Section (Screen 15)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FacultyMenuItem(
                        icon = Icons.Outlined.Person,
                        title = "Personal Information",
                        onClick = { showPersonalInfoDialog = true },
                        testTag = "menu_faculty_personal_info"
                    )
                    HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(horizontal = 16.dp))

                    FacultyMenuItem(
                        icon = Icons.Outlined.Lock,
                        title = "Change Password",
                        onClick = { showChangePasswordDialog = true },
                        testTag = "menu_faculty_change_password"
                    )
                    HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(horizontal = 16.dp))

                    FacultyMenuItem(
                        icon = Icons.Outlined.Tune,
                        title = "App Settings",
                        onClick = onNavigateToSettings,
                        testTag = "menu_faculty_settings"
                    )
                    HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(horizontal = 16.dp))

                    FacultyMenuItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        onClick = onNavigateToNotifications,
                        testTag = "menu_faculty_notifications"
                    )
                    HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(horizontal = 16.dp))

                    FacultyMenuItem(
                        icon = Icons.Outlined.HelpOutline,
                        title = "Help & Support",
                        onClick = { showHelpDialog = true },
                        testTag = "menu_faculty_help"
                    )
                    HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(horizontal = 16.dp))

                    FacultyMenuItem(
                        icon = Icons.Outlined.Info,
                        title = "About",
                        onClick = onNavigateToAbout,
                        testTag = "menu_faculty_about"
                    )
                    HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(horizontal = 16.dp))

                    // Logout Button in Red (Screen 15)
                    FacultyMenuItem(
                        icon = Icons.AutoMirrored.Outlined.ExitToApp,
                        title = "Logout",
                        textColor = Color(0xFFDC2626),
                        iconTint = Color(0xFFDC2626),
                        onClick = { showLogoutDialog = true },
                        testTag = "menu_faculty_logout"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Personal Information Dialog
        if (showPersonalInfoDialog) {
            AlertDialog(
                onDismissRequest = { showPersonalInfoDialog = false },
                title = { Text("Faculty Information", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Name: ${faculty?.name}")
                        Text("Faculty ID: ${faculty?.facultyId}")
                        Text("Department: ${faculty?.department}")
                        Text("Designation: ${faculty?.designation}")
                        Text("Email: ${faculty?.email}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPersonalInfoDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Change Password Dialog
        if (showChangePasswordDialog) {
            AlertDialog(
                onDismissRequest = { showChangePasswordDialog = false },
                title = { Text("Change Password", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Enter new secure password (min 6 characters):", color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPassword.length >= 6) {
                                showChangePasswordDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Password updated successfully!")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
                    ) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangePasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Help Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "For system issues, classroom beacon pairing, or roster corrections, please contact campus IT support at support@college.edu or visit the Administrator office.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to log out from Faculty mode?", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout()
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Logout", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun FacultyMenuItem(
    icon: ImageVector,
    title: String,
    textColor: Color = TextPrimary,
    iconTint: Color = BrandPrimaryBlue,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = textColor
                )
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

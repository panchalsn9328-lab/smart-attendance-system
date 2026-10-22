package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ble.BleScanState
import com.example.bluetooth.BleState
import com.example.bluetooth.BluetoothPermissionHelper
import com.example.model.AttendanceRecord
import com.example.model.Student
import com.example.ui.components.BleScanningAnimation
import com.example.ui.components.BottomNavTab
import com.example.ui.components.SmartBottomNavigationBar
import com.example.ui.components.StudentBluetoothScanDialog
import com.example.ui.theme.BleBlue
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.PaletteGray
import com.example.ui.theme.PaletteLightBlue
import com.example.ui.theme.PaletteLightGray
import com.example.ui.theme.StatusAbsentRedBg
import com.example.ui.theme.StatusAbsentRedText
import com.example.ui.theme.StatusPresentGreenBg
import com.example.ui.theme.StatusPresentGreenText
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import androidx.compose.material.icons.filled.Face
import com.example.bluetooth.DiscoveredBleDevice

@Composable
fun HomeScreen(
    student: Student?,
    scanState: BleScanState = BleScanState.Idle,
    bleState: BleState = BleState.Idle,
    unreadNotificationCount: Int = 0,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onAttendanceMarkedAuto: (AttendanceRecord) -> Unit = {},
    onStartScanning: () -> Unit = {},
    onRecordLocalAttendance: ((String, String) -> Unit)? = null,
    discoveredDevices: List<DiscoveredBleDevice> = emptyList(),
    onConnectDiscoveredDevice: ((android.bluetooth.BluetoothDevice) -> Unit)? = null,
    onNavigateToFaceVerification: (String, String) -> Unit = { _, _ -> },
    onNavigateToFaceRegistration: () -> Unit = {}
) {
    val context = LocalContext.current
    var showScanDialog by remember { mutableStateOf(false) }

    // Runtime Permission Launcher for Android 12+ BLE
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onStartScanning()
            showScanDialog = true
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (BluetoothPermissionHelper.isBluetoothEnabled(context)) {
            onStartScanning()
            showScanDialog = true
        }
    }

    // Automatically ensure scanning is started when HomeScreen is presented
    LaunchedEffect(Unit) {
        if (bleState is BleState.Idle || bleState is BleState.Disconnected || bleState is BleState.DeviceNotFound) {
            if (BluetoothPermissionHelper.hasPermissions(context) && BluetoothPermissionHelper.isBluetoothEnabled(context)) {
                onStartScanning()
            }
        }
    }

    // Auto-navigate to Attendance Marked confirmation screen when attendance is recorded
    LaunchedEffect(bleState) {
        if (bleState is BleState.AttendanceMarked) {
            val record = (bleState as BleState.AttendanceMarked).record
            onAttendanceMarkedAuto(record)
        } else if (bleState is BleState.ActiveSession) {
            // Dual verification flow: Active BLE session detected -> launch face verification
            showScanDialog = false
            onNavigateToFaceVerification(bleState.deviceId, bleState.sessionToken)
        }
    }

    if (showScanDialog) {
        StudentBluetoothScanDialog(
            bleState = bleState,
            onDismissRequest = { showScanDialog = false },
            onStartScan = onStartScanning,
            onRecordLocalAttendance = null,
            discoveredDevices = discoveredDevices,
            onSelectDevice = { dev ->
                onConnectDiscoveredDevice?.invoke(dev)
            }
        )
    }

    Scaffold(
        bottomBar = {
            SmartBottomNavigationBar(
                currentTab = BottomNavTab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.HOME -> { /* already here */ }
                        BottomNavTab.HISTORY -> onNavigateToHistory()
                        BottomNavTab.PROFILE -> onNavigateToProfile()
                    }
                },
                modifier = Modifier.testTag("home_bottom_nav")
            )
        },
        containerColor = SurfaceWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Greeting, Student/Teacher Role Switcher, Notification Icon
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello,",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                color = TextSecondary
                            )
                        )
                        Text(
                            text = student?.name ?: "Student",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.testTag("btn_notifications_top")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge(
                                        containerColor = BrandPrimaryBlue,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = unreadNotificationCount.toString(),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = TextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (student != null && !student.faceRegistered) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PaletteLightBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Biometric Setup Required",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Register face for dual BLE attendance",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                        Button(
                            onClick = onNavigateToFaceRegistration,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Register", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ==================== STUDENT ATTENDANCE ====================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                    BleScanningAnimation(
                        isScanning = bleState is BleState.Scanning || bleState is BleState.Connecting || bleState is BleState.ReadingStatus
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Status display with smooth animated transitions
                    AnimatedContent(
                        targetState = bleState,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.96f))
                                .togetherWith(fadeOut(animationSpec = tween(180)))
                        },
                        label = "ble_status_transition",
                        modifier = Modifier.fillMaxWidth()
                    ) { currentBleState ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (currentBleState) {
                                is BleState.Scanning -> {
                                    Text(
                                        text = "Scanning for classroom device...",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = BrandPrimaryBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Looking for BLE_Attendance",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            color = TextSecondary
                                        )
                                    )
                                }

                                is BleState.DeviceFound -> {
                                    Text(
                                        text = "ESP32 Found",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = BrandPrimaryBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Device: ${currentBleState.deviceName}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    )
                                }

                                is BleState.Connecting -> {
                                    Text(
                                        text = "Connecting to classroom device",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = BrandPrimaryBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Connecting to ${currentBleState.deviceName}...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            color = TextSecondary
                                        )
                                    )
                                }

                                is BleState.Connected, is BleState.ReadingStatus -> {
                                    Text(
                                        text = "Connected to classroom device",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = BrandPrimaryBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Reading STATUS, SESSION TOKEN, DEVICE ID...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            color = TextSecondary
                                        )
                                    )
                                }

                                is BleState.ActiveSession -> {
                                    Text(
                                        text = "ESP32 Connected",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = BrandPrimaryBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Attendance Session Active",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        ),
                                        color = StatusPresentGreenText
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SurfaceCard,
                                        modifier = Modifier.fillMaxWidth(0.92f)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "Device:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                                Text(text = currentBleState.deviceName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "Device ID:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                                Text(text = currentBleState.deviceId, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandPrimaryBlue)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "Session:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                                Text(text = currentBleState.status, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = StatusPresentGreenText)
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    onNavigateToFaceVerification(currentBleState.deviceId, currentBleState.sessionToken)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp)
                                                    .testTag("btn_verify_face_active_session")
                                            ) {
                                                Icon(imageVector = Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Verify Face to Mark Attendance", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }

                                is BleState.InactiveSession -> {
                                    Text(
                                        text = "Attendance Session Not Active",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = StatusAbsentRedText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Device ID: ${currentBleState.deviceId}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "The teacher has not started the session on the ESP32 (GPIO 4 button).",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                is BleState.AttendanceMarked -> {
                                    Text(
                                        text = "Attendance Marked!",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = StatusPresentGreenText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${currentBleState.record.studentName} is marked Present",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Session Token: ${currentBleState.record.sessionToken}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    )
                                }

                                is BleState.AlreadyMarked -> {
                                    Text(
                                        text = "Attendance Already Marked",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = StatusPresentGreenText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "You have already marked attendance for this session token.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            color = TextSecondary
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                is BleState.DeviceNotFound -> {
                                    Text(
                                        text = "ESP32 Not Detected",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = StatusAbsentRedText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Classroom device not found. Ensure the teacher pressed the GPIO 4 button to activate attendance.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                is BleState.BluetoothDisabled -> {
                                    Text(
                                        text = "Bluetooth is Turned Off",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = StatusAbsentRedText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Please enable Bluetooth to scan for the classroom ESP32.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            color = TextSecondary
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                is BleState.FirebaseError -> {
                                    Text(
                                        text = "Attendance Sync Warning",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = StatusAbsentRedText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentBleState.message,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            color = TextSecondary
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                is BleState.Error -> {
                                    Text(
                                        text = "Connection Error",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = StatusAbsentRedText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentBleState.message,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            color = TextSecondary
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                else -> {
                                    Text(
                                        text = "Classroom BLE Scanner",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 17.sp
                                        ),
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Keep Bluetooth on and stay within classroom range",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            color = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (!BluetoothPermissionHelper.hasPermissions(context)) {
                                    permissionLauncher.launch(BluetoothPermissionHelper.getRequiredPermissions())
                                } else if (!BluetoothPermissionHelper.isBluetoothEnabled(context)) {
                                    try {
                                        enableBluetoothLauncher.launch(BluetoothPermissionHelper.getEnableBluetoothIntent())
                                    } catch (e: Exception) {
                                        try {
                                            context.startActivity(BluetoothPermissionHelper.getOpenBluetoothSettingsIntent())
                                        } catch (_: Exception) {}
                                    }
                                } else {
                                    onStartScanning()
                                    showScanDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_find_esp32")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = "Scan",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Find ESP32")
                        }

                        OutlinedButton(
                            onClick = { showScanDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_open_scan_dialog")
                        ) {
                            Text("Scanner Modal")
                        }
                    }
                }

            Spacer(modifier = Modifier.height(20.dp))

            // Information Card: Auto Check-in matching Screen 4 in UI Reference
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_auto_checkin"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PaletteLightBlue.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Radar,
                            contentDescription = "Beacon Icon",
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Auto Check-in",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "When in range of active ESP32 beacon, session is verified automatically.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

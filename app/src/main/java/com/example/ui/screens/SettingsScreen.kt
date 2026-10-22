package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.bluetooth.BleAttendanceForegroundService
import com.example.bluetooth.BluetoothPermissionHelper
import com.example.model.AppSettings
import com.example.ui.components.SmartTopBar
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.PaletteLightBlue
import com.example.ui.theme.PaletteLightGray
import com.example.ui.theme.StatusAbsentRedBg
import com.example.ui.theme.StatusAbsentRedText
import com.example.ui.theme.StatusPresentGreenBg
import com.example.ui.theme.StatusPresentGreenText
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: (bluetooth: Boolean?, backgroundScanning: Boolean?, notifications: Boolean?, themeMode: String?) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    // State for live permission and service status
    var isBtEnabled by remember { mutableStateOf(BluetoothPermissionHelper.isBluetoothEnabled(context)) }
    var hasBtPermissions by remember { mutableStateOf(BluetoothPermissionHelper.hasPermissions(context)) }
    var isBatteryOptimizedIgnored by remember { mutableStateOf(BluetoothPermissionHelper.isIgnoringBatteryOptimizations(context)) }
    var hasNotificationPermission by remember { mutableStateOf(BluetoothPermissionHelper.hasNotificationPermission(context)) }
    var hasLocationPermission by remember { mutableStateOf(BluetoothPermissionHelper.hasLocationPermission(context)) }
    var isServiceRunning by remember { mutableStateOf(BleAttendanceForegroundService.isRunning) }

    fun refreshStatus() {
        isBtEnabled = BluetoothPermissionHelper.isBluetoothEnabled(context)
        hasBtPermissions = BluetoothPermissionHelper.hasPermissions(context)
        isBatteryOptimizedIgnored = BluetoothPermissionHelper.isIgnoringBatteryOptimizations(context)
        hasNotificationPermission = BluetoothPermissionHelper.hasNotificationPermission(context)
        hasLocationPermission = BluetoothPermissionHelper.hasLocationPermission(context)
        isServiceRunning = BleAttendanceForegroundService.isRunning
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Launchers
    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshStatus()
    }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshStatus()
    }

    val requestNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshStatus()
    }

    Scaffold(
        topBar = {
            SmartTopBar(
                title = "App Settings & Permissions",
                onBackClick = onBackClick,
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = SurfaceWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ==================== PERMISSIONS & SYSTEM STATUS ====================
            Text(
                text = "PERMISSIONS & HARDWARE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                ),
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. Bluetooth Adapter On/Off
                    PermissionStatusItem(
                        icon = Icons.Outlined.Bluetooth,
                        title = "Bluetooth Adapter",
                        subtitle = if (isBtEnabled) "Turned on and ready" else "Bluetooth is turned off",
                        isGranted = isBtEnabled,
                        grantedText = "ON",
                        deniedText = "Turn ON",
                        onActionClick = {
                            try {
                                enableBtLauncher.launch(BluetoothPermissionHelper.getEnableBluetoothIntent())
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(BluetoothPermissionHelper.getOpenBluetoothSettingsIntent())
                                } catch (_: Exception) {}
                            }
                        },
                        testTag = "btn_perm_bluetooth"
                    )

                    HorizontalDivider(color = PaletteLightGray, thickness = 1.dp)

                    // 2. Bluetooth Scan & Connect Permissions
                    PermissionStatusItem(
                        icon = Icons.Outlined.Security,
                        title = "Bluetooth Permissions",
                        subtitle = if (hasBtPermissions) "Scan and connect granted" else "Nearby devices access required",
                        isGranted = hasBtPermissions,
                        grantedText = "Granted",
                        deniedText = "Grant",
                        onActionClick = {
                            requestPermissionsLauncher.launch(BluetoothPermissionHelper.getRequiredPermissions())
                        },
                        testTag = "btn_perm_ble_scan"
                    )

                    HorizontalDivider(color = PaletteLightGray, thickness = 1.dp)

                    // 3. Location Permissions
                    PermissionStatusItem(
                        icon = Icons.Outlined.LocationOn,
                        title = "Location Permission",
                        subtitle = if (hasLocationPermission) "Beacon detection allowed" else "Needed for BLE beacon detection",
                        isGranted = hasLocationPermission,
                        grantedText = "Granted",
                        deniedText = "Grant",
                        onActionClick = {
                            requestPermissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        testTag = "btn_perm_location"
                    )

                    HorizontalDivider(color = PaletteLightGray, thickness = 1.dp)

                    // 4. Notifications Permission
                    PermissionStatusItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        subtitle = if (hasNotificationPermission) "Alerts & service notifications active" else "Attendance alerts required",
                        isGranted = hasNotificationPermission,
                        grantedText = "Allowed",
                        deniedText = "Allow",
                        onActionClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                Toast.makeText(context, "Notifications enabled in system", Toast.LENGTH_SHORT).show()
                            }
                        },
                        testTag = "btn_perm_notifications"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==================== BACKGROUND RUNNING ====================
            Text(
                text = "BACKGROUND RUNNING",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                ),
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Battery Optimization Exemption
                    PermissionStatusItem(
                        icon = if (isBatteryOptimizedIgnored) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryAlert,
                        title = "Battery Optimization",
                        subtitle = if (isBatteryOptimizedIgnored) {
                            "Unrestricted background execution active"
                        } else {
                            "Exempt app from battery saver for background check-in"
                        },
                        isGranted = isBatteryOptimizedIgnored,
                        grantedText = "Unrestricted",
                        deniedText = "Allow",
                        onActionClick = {
                            try {
                                context.startActivity(BluetoothPermissionHelper.getRequestIgnoreBatteryOptimizationIntent(context))
                            } catch (_: Exception) {
                                context.startActivity(BluetoothPermissionHelper.getAppSettingsIntent(context))
                            }
                        },
                        testTag = "btn_perm_battery"
                    )

                    HorizontalDivider(color = PaletteLightGray, thickness = 1.dp)

                    // Foreground Service Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PaletteLightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Radar,
                                    contentDescription = "Background Service",
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Background Monitor Service",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isServiceRunning) "Actively scanning for classroom ESP32" else "Run service when app is closed",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = if (isServiceRunning) StatusPresentGreenText else TextSecondary
                                    )
                                )
                            }
                        }
                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (!hasBtPermissions) {
                                        requestPermissionsLauncher.launch(BluetoothPermissionHelper.getRequiredPermissions())
                                    } else {
                                        BleAttendanceForegroundService.start(context)
                                        isServiceRunning = true
                                        onUpdateSettings(null, true, null, null)
                                    }
                                } else {
                                    BleAttendanceForegroundService.stop(context)
                                    isServiceRunning = false
                                    onUpdateSettings(null, false, null, null)
                                }
                            },
                            modifier = Modifier.testTag("switch_background_service"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BrandPrimaryBlue,
                                uncheckedTrackColor = PaletteLightGray
                            )
                        )
                    }

                    HorizontalDivider(color = PaletteLightGray, thickness = 1.dp)

                    // Open Android App Details Settings
                    SettingsNavigationItem(
                        icon = Icons.Outlined.Settings,
                        title = "Open System App Settings",
                        subtitle = "Manage all Android permissions in device settings",
                        onClick = {
                            try {
                                context.startActivity(BluetoothPermissionHelper.getAppSettingsIntent(context))
                            } catch (_: Exception) {}
                        },
                        testTag = "item_setting_app_details"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==================== APP PREFERENCES ====================
            Text(
                text = "GENERAL PREFERENCES",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                ),
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Theme Row
                    SettingsNavigationItem(
                        icon = Icons.Outlined.BrightnessMedium,
                        title = "Theme",
                        subtitle = settings.themeMode,
                        onClick = { showThemeDialog = true },
                        testTag = "item_setting_theme"
                    )
                    HorizontalDivider(color = PaletteLightGray, thickness = 1.dp)

                    // Privacy Policy
                    SettingsNavigationItem(
                        icon = Icons.Outlined.Policy,
                        title = "Privacy Policy",
                        subtitle = "Hardware and beacon proximity rules",
                        onClick = { showPrivacyDialog = true },
                        testTag = "item_setting_privacy"
                    )
                    HorizontalDivider(color = PaletteLightGray, thickness = 1.dp)

                    // Terms & Conditions
                    SettingsNavigationItem(
                        icon = Icons.Outlined.Description,
                        title = "Terms & Conditions",
                        subtitle = "Attendance protocol details",
                        onClick = { showTermsDialog = true },
                        testTag = "item_setting_terms"
                    )
                }
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        val themes = listOf("System Default", "Light", "Dark")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    themes.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateSettings(null, null, null, mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.themeMode == mode,
                                onClick = {
                                    onUpdateSettings(null, null, null, mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandPrimaryBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Smart Attendance Privacy Policy\n\n" +
                                "1. Data Collection: The Smart Attendance application scans for classroom ESP32 BLE beacons to verify student presence in real time.\n\n" +
                                "2. Beacon Proximity: We do not track student geographic GPS coordinates; BLE RSSI signals are used exclusively to determine classroom proximity.\n\n" +
                                "3. Attendance Records: Attendance records containing subject name, timestamp, and verification status are safely synchronized with the institutional backend.\n\n" +
                                "4. Data Security: All network and BLE communications adhere to strict university security protocols.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    // Terms & Conditions Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Smart Attendance Student Agreement\n\n" +
                                "1. Authentic Attendance: Each student must use their assigned device and student account to record attendance.\n\n" +
                                "2. Classroom Presence: Attendance is only validated when the student is physically within classroom boundaries during the scheduled lecture.\n\n" +
                                "3. Hardware Compliance: Tampering with BLE beacon signals or simulating false proximity for unauthorized records is strictly prohibited.\n\n" +
                                "4. System Continuity: Keep Bluetooth enabled during lecture hours for uninterrupted automatic check-ins.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Agree")
                }
            }
        )
    }
}

@Composable
private fun PermissionStatusItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    grantedText: String,
    deniedText: String,
    onActionClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) StatusPresentGreenBg else StatusAbsentRedBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isGranted) StatusPresentGreenText else StatusAbsentRedText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StatusPresentGreenBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = StatusPresentGreenText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = grantedText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = StatusPresentGreenText
                    )
                )
            }
        } else {
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag(testTag)
            ) {
                Text(
                    text = deniedText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    ),
                    color = TextPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}

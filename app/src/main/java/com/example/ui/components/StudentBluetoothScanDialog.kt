package com.example.ui.components

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import com.example.bluetooth.BleState
import com.example.bluetooth.BluetoothPermissionHelper
import com.example.bluetooth.DiscoveredBleDevice
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StudentBluetoothScanDialog(
    bleState: BleState,
    onDismissRequest: () -> Unit,
    onStartScan: () -> Unit,
    onRecordLocalAttendance: ((deviceId: String, token: String) -> Unit)? = null,
    discoveredDevices: List<DiscoveredBleDevice> = emptyList(),
    onSelectDevice: ((BluetoothDevice) -> Unit)? = null
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            onStartScan()
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (BluetoothPermissionHelper.isBluetoothEnabled(context)) {
            onStartScan()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("dialog_student_ble_scan"),
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PaletteLightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth",
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ESP32 Scanner",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Dialog",
                        tint = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (bleState) {
                    is BleState.Idle -> {
                        Icon(
                            imageVector = Icons.Outlined.BluetoothSearching,
                            contentDescription = "Idle",
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Find Classroom ESP32",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Search for device 'BLE_Attendance' in your classroom.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    is BleState.CheckingPermissions -> {
                        CircularProgressIndicator(
                            color = BrandPrimaryBlue,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Checking Bluetooth permissions...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    is BleState.Scanning -> {
                        CircularProgressIndicator(
                            color = BrandPrimaryBlue,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Scanning for classroom device...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Looking for 'BLE_Attendance'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    is BleState.DeviceFound -> {
                        Icon(
                            imageVector = Icons.Outlined.BluetoothConnected,
                            contentDescription = "Found",
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ESP32 Found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimaryBlue
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Device: ${bleState.deviceName}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Signal: ${bleState.rssi} dBm",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    is BleState.Connecting -> {
                        CircularProgressIndicator(
                            color = BrandPrimaryBlue,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connecting...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connecting to classroom device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    is BleState.Connected -> {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Connected",
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connected to classroom device",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = BrandPrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Device: ${bleState.deviceName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    is BleState.ReadingStatus -> {
                        CircularProgressIndicator(
                            color = BrandPrimaryBlue,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connected to classroom device",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = BrandPrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reading session characteristics...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    is BleState.ActiveSession -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(StatusPresentGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Active",
                                tint = StatusPresentGreenText,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Attendance Session Active",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = StatusPresentGreenText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connected to classroom device",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Session details card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceCard,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Device:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = bleState.deviceName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Device ID:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = bleState.deviceId,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Session Status:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = bleState.status,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = StatusPresentGreenText
                                    )
                                }
                                if (bleState.sessionToken.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Token:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = bleState.sessionToken.take(16),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is BleState.AttendanceMarked -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(StatusPresentGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Attendance Marked",
                                tint = StatusPresentGreenText,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Attendance Marked",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = StatusPresentGreenText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Verified successfully via classroom ESP32 beacon",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceCard,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Status:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text(
                                        text = "PRESENT",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = StatusPresentGreenText
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Device ID:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text(
                                        text = bleState.record.deviceId.ifEmpty { "CLASSROOM_ESP32_01" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                }
                                if (bleState.record.sessionToken.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Session Token:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Text(
                                            text = bleState.record.sessionToken.take(16),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is BleState.AlreadyMarked -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PaletteLightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Already Marked",
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Attendance Already Marked",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = BrandPrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You have already marked attendance for this session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    is BleState.DeviceNotFound -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(StatusAbsentRedBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Device Not Found",
                                tint = StatusAbsentRedText,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Classroom Device Not Found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = StatusAbsentRedText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = bleState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    is BleState.FirebaseError -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(StatusAbsentRedBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Firebase Error",
                                tint = StatusAbsentRedText,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Database Error",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = StatusAbsentRedText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = bleState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    is BleState.InactiveSession -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(StatusAbsentRedBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Inactive",
                                tint = StatusAbsentRedText,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No active attendance session.",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = StatusAbsentRedText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Device ID: ${bleState.deviceId}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "The teacher has not started the session on the ESP32 (GPIO 4 button).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    is BleState.Disconnected -> {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Disconnected",
                            tint = TextSecondary,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Disconnected",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bleState.message ?: "Disconnected from classroom device",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    is BleState.BluetoothDisabled -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PaletteLightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bluetooth,
                                contentDescription = "Bluetooth Disabled",
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Bluetooth is required for attendance.",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bluetooth is required to detect and connect to the classroom ESP32 beacon.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    is BleState.PermissionRequired -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PaletteLightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Permissions Required",
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Permissions Required",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Classroom beacon scanning requires Nearby Devices and Location permissions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    is BleState.Error -> {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Error",
                            tint = StatusAbsentRedText,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Connection Error",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = StatusAbsentRedText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bleState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // If Location service is turned OFF, warn user as it affects BLE beacon discovery
                val isLocationOn = remember(bleState) { BluetoothPermissionHelper.isLocationEnabled(context) }
                if (!isLocationOn) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusAbsentRedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Location (GPS) is OFF",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = StatusAbsentRedText
                                )
                                Text(
                                    text = "Some Android phones require GPS to discover BLE beacons.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )
                            }
                            TextButton(
                                onClick = {
                                    try {
                                        context.startActivity(BluetoothPermissionHelper.getOpenLocationSettingsIntent())
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Text("Turn ON", fontWeight = FontWeight.Bold, color = StatusAbsentRedText, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Show Discovered Devices list if any are detected
                if (discoveredDevices.isNotEmpty() && bleState !is BleState.ActiveSession) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nearby Devices (${discoveredDevices.size})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            if (bleState is BleState.Scanning) {
                                Text(
                                    text = "Scanning...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = BrandPrimaryBlue
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                        ) {
                            items(discoveredDevices, key = { it.address }) { dev ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (dev.isEsp32Candidate) PaletteLightBlue else PaletteLightGray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = dev.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (dev.isEsp32Candidate) BrandPrimaryBlue else TextPrimary
                                                )
                                                if (dev.isEsp32Candidate) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = BrandPrimaryBlue
                                                    ) {
                                                        Text(
                                                            text = "ESP32",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${dev.address} • ${dev.rssi} dBm",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                color = TextSecondary
                                            )
                                        }
                                        Button(
                                            onClick = { onSelectDevice?.invoke(dev.device) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (dev.isEsp32Candidate) BrandPrimaryBlue else PaletteGray
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Connect", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (bleState) {
                is BleState.BluetoothDisabled -> {
                    Button(
                        onClick = {
                            try {
                                enableBluetoothLauncher.launch(BluetoothPermissionHelper.getEnableBluetoothIntent())
                            } catch (e: Exception) {
                                try {
                                    context.startActivity(BluetoothPermissionHelper.getOpenBluetoothSettingsIntent())
                                } catch (_: Exception) {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_turn_on_bluetooth")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bluetooth,
                            contentDescription = "Bluetooth",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Turn On Bluetooth")
                    }
                }

                is BleState.PermissionRequired -> {
                    Button(
                        onClick = {
                            permissionLauncher.launch(BluetoothPermissionHelper.getRequiredPermissions())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_grant_permissions")
                    ) {
                        Text("Grant Permissions")
                    }
                }

                is BleState.AttendanceMarked -> {
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusPresentGreenText),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_attendance_done")
                    ) {
                        Text("Done")
                    }
                }

                is BleState.AlreadyMarked -> {
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_already_marked_close")
                    ) {
                        Text("Close")
                    }
                }

                is BleState.DeviceNotFound, is BleState.FirebaseError -> {
                    Button(
                        onClick = { onStartScan() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_retry_scan")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Scan")
                    }
                }

                is BleState.Idle, is BleState.Disconnected, is BleState.Error, is BleState.InactiveSession -> {
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
                                onStartScan()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_dialog_scan")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Scan",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan for ESP32")
                    }
                }

                is BleState.ActiveSession -> {
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_proceed_face_verify")
                    ) {
                        Text("Proceed to Face Verification")
                    }
                }

                is BleState.Scanning, is BleState.Connecting, is BleState.Connected, is BleState.ReadingStatus -> {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                }

                else -> {}
            }
        },
        dismissButton = {
            if (bleState is BleState.ActiveSession) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        }
    )
}

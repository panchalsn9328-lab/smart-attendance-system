package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.ContextCompat
import com.example.face.CameraPreviewWithOverlay
import com.example.face.FaceEmbeddingProcessor
import com.example.face.LivenessDetector
import com.example.model.AttendanceRecord
import com.example.model.Student
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
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated Student Face Verification Screen
 *
 * Requirements:
 * 1. Bluetooth verified status display ("Bluetooth verified ✓")
 * 2. Front camera face capture with bounding box and reticle
 * 3. Basic Liveness verification ("Look at camera" -> "Turn left" -> "Turn right" -> "Look straight")
 * 4. Face embedding extraction and cosine similarity comparison with student's registered embedding
 * 5. Configurable threshold (calibrated DEFAULT_SIMILARITY_THRESHOLD = 0.72f)
 * 6. Dual verification: BLE + Face + Liveness all pass -> Mark attendance as PRESENT.
 * 7. Verification failure handling with precise failure messages.
 */
@Composable
fun FaceVerificationScreen(
    student: Student?,
    deviceId: String,
    sessionToken: String,
    onNavigateBack: () -> Unit,
    onNavigateToRegistration: () -> Unit,
    onAttendanceMarkedSuccess: (AttendanceRecord) -> Unit,
    onVerifyAndMarkAttendance: suspend (deviceId: String, sessionToken: String, bleVerified: Boolean, faceVerified: Boolean, livenessVerified: Boolean) -> Pair<Boolean, String>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val processor = remember { FaceEmbeddingProcessor(context) }
    val livenessDetector = remember { LivenessDetector() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler {
        onNavigateBack()
    }

    // Status tracking states
    var isBleVerified by remember { mutableStateOf(deviceId.isNotBlank() && sessionToken.isNotBlank()) }
    var livenessState by remember { mutableStateOf<String>("Waiting...") }
    var faceState by remember { mutableStateOf<String>("Scanning...") }
    var livenessVerified by remember { mutableStateOf(false) }
    var faceVerified by remember { mutableStateOf(false) }

    var similarityScore by remember { mutableFloatStateOf(0.0f) }
    var reticleColor by remember { mutableStateOf(BleBlue) }

    var isVerifying by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var isFailed by remember { mutableStateOf(false) }
    var failureReason by remember { mutableStateOf<String?>(null) }
    var markedRecord by remember { mutableStateOf<AttendanceRecord?>(null) }

    // Canonical authenticated student UID
    val currentAuthUid = FirebaseAuth.getInstance().currentUser?.uid ?: student?.id
    var registeredEmbedding by remember { mutableStateOf<List<Float>>(emptyList()) }
    var isProfileLoading by remember { mutableStateOf(true) }
    var isProfileMissing by remember { mutableStateOf(false) }
    var isLegacyProfile by remember { mutableStateOf(false) }

    // Explicitly load canonical face profile for the currently authenticated student
    LaunchedEffect(currentAuthUid) {
        if (currentAuthUid.isNullOrBlank()) {
            isProfileMissing = true
            isProfileLoading = false
            return@LaunchedEffect
        }
        val status = withContext(Dispatchers.IO) {
            com.example.data.firebase.FirebaseAttendanceDataSource().getFaceProfileStatus(currentAuthUid)
        }
        when (status) {
            is com.example.data.firebase.FaceProfileStatus.Valid -> {
                Log.d("FACE_DEBUG", "authUid=$currentAuthUid faceProfileUid=$currentAuthUid embeddingDim=${status.embedding.size} (MobileFaceNet 192-d)")
                registeredEmbedding = status.embedding
                isProfileMissing = false
                isLegacyProfile = false
            }
            is com.example.data.firebase.FaceProfileStatus.LegacyNeedsMigration -> {
                Log.w("FACE_DEBUG", "authUid=$currentAuthUid has legacy ${status.currentDim}-d profile. Migration required.")
                registeredEmbedding = emptyList()
                isProfileMissing = false
                isLegacyProfile = true
                failureReason = "Biometric security upgraded to MobileFaceNet neural model. Please re-register your face profile in Profile -> Face Registration."
            }
            is com.example.data.firebase.FaceProfileStatus.Missing -> {
                Log.d("FACE_DEBUG", "authUid=$currentAuthUid profile missing.")
                registeredEmbedding = emptyList()
                isProfileMissing = true
                isLegacyProfile = false
                failureReason = "No registered face profile found for authenticated student ($currentAuthUid)."
            }
            is com.example.data.firebase.FaceProfileStatus.Error -> {
                Log.e("FACE_DEBUG", "authUid=$currentAuthUid error loading face profile: ${status.message}")
                registeredEmbedding = emptyList()
                isProfileMissing = true
                isLegacyProfile = false
                failureReason = "Error loading face profile: ${status.message}"
            }
        }
        isProfileLoading = false
    }

    // Clear all in-memory verification state on destroy / exit
    DisposableEffect(Unit) {
        onDispose {
            livenessDetector.reset()
            registeredEmbedding = emptyList()
            similarityScore = 0.0f
            livenessVerified = false
            faceVerified = false
            isVerifying = false
            isLegacyProfile = false
        }
    }

    // Check if authenticated student has a valid registered face profile
    val isFaceRegistered = !isProfileLoading && !isProfileMissing && !isLegacyProfile && registeredEmbedding.isNotEmpty()

    fun resetVerification() {
        livenessDetector.reset()
        livenessVerified = false
        faceVerified = false
        isFailed = false
        isSuccess = false
        failureReason = null
        similarityScore = 0.0f
        livenessState = "Waiting..."
        faceState = "Scanning..."
        reticleColor = BleBlue
    }

    Scaffold(
        topBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_face_verify")
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
                            text = "FACE VERIFICATION",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Anti-Proxy Attendance System",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        )
                    }
                }
            }
        },
        containerColor = SurfaceWhite
    ) { innerPadding ->
        if (isProfileLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = BrandPrimaryBlue)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading Registered Face Profile...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, color = TextSecondary)
                )
            }
        } else if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PaletteLightBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = BrandPrimaryBlue,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Face verification requires camera access to confirm student presence and match your registered biometric face embedding.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = TextSecondary),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enable Camera")
                }
            }
        } else if (!isFaceRegistered) {
            // Student has not registered face yet
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(StatusAbsentRedBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = StatusAbsentRedText,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Face Registration Required",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You must register your face before you can mark attendance. Please register your biometric face profile first.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = TextSecondary
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToRegistration,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                        .testTag("btn_goto_register_face")
                ) {
                    Text("Register Face Now", fontWeight = FontWeight.Bold)
                }
            }
        } else if (isSuccess) {
            // Attendance marked successfully
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(StatusPresentGreenBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = StatusPresentGreenText,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Attendance Marked Successfully!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dual verification confirmation summary card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bluetooth Detection:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("✓ Verified ($deviceId)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = StatusPresentGreenText))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Liveness Check:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("✓ Liveness verified", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = StatusPresentGreenText))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Biometric Face Match:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("✓ Face verified (${(similarityScore * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = StatusPresentGreenText))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Attendance Status:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("PRESENT", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BrandPrimaryBlue))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        markedRecord?.let { onAttendanceMarkedSuccess(it) } ?: onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(50.dp)
                        .testTag("btn_attendance_success_done")
                ) {
                    Text("View Attendance Receipt", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else if (isFailed) {
            // Verification Failure Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(StatusAbsentRedBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Failed",
                        tint = StatusAbsentRedText,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Face Verification Failed",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = StatusAbsentRedText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = failureReason ?: "Face verification could not be confirmed.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Attendance was NOT marked.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusAbsentRedText
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { resetVerification() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("btn_retry_face_verify")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Live Dual-Verification Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Header Status Card: Bluetooth, Face, Liveness
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "DUAL VERIFICATION STATUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimaryBlue,
                                letterSpacing = 1.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Status 1: Bluetooth
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BluetoothConnected,
                                    contentDescription = null,
                                    tint = if (isBleVerified) StatusPresentGreenText else PaletteGray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bluetooth:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = if (isBleVerified) "✓ Verified" else "Waiting...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBleVerified) StatusPresentGreenText else TextSecondary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Status 2: Face
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Face,
                                    contentDescription = null,
                                    tint = if (faceVerified) StatusPresentGreenText else BrandPrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Face:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = faceState,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (faceVerified) StatusPresentGreenText else BrandPrimaryBlue
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Status 3: Liveness
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.MotionPhotosOn,
                                    contentDescription = null,
                                    tint = if (livenessVerified) StatusPresentGreenText else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Liveness:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = livenessState,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (livenessVerified) StatusPresentGreenText else TextSecondary
                                )
                            )
                        }
                    }
                }

                // Front Camera View with Face Oval Reticle
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(2.dp, PaletteLightGray, RoundedCornerShape(20.dp))
                ) {
                    CameraPreviewWithOverlay(
                        modifier = Modifier.fillMaxSize(),
                        reticleColor = reticleColor,
                        onFrameAnalyzed = { _, faces, bitmap ->
                            if (isVerifying || isSuccess || isFailed) return@CameraPreviewWithOverlay

                            if (faces.isEmpty()) {
                                faceState = "Face not detected"
                                reticleColor = PaletteGray
                                return@CameraPreviewWithOverlay
                            }

                            if (faces.size > 1) {
                                faceState = "Multiple faces detected"
                                reticleColor = StatusAbsentRedText
                                failureReason = "Multiple faces detected. Ensure only one person is in front of the camera."
                                isFailed = true
                                return@CameraPreviewWithOverlay
                            }

                            val face = faces[0]
                            faceState = "Face detected"

                            // Step A: Run basic liveness state machine
                            if (!livenessVerified) {
                                val result = livenessDetector.processFrame(faces)
                                when (result) {
                                    is LivenessDetector.ValidationResult.InProgress -> {
                                        livenessState = result.instruction
                                        reticleColor = BrandPrimaryBlue
                                    }
                                    is LivenessDetector.ValidationResult.LivenessPassed -> {
                                        livenessVerified = true
                                        livenessState = "✓ Liveness verified"
                                        reticleColor = StatusPresentGreenText
                                    }
                                    is LivenessDetector.ValidationResult.MultipleFacesDetected -> {
                                        failureReason = "Multiple faces detected."
                                        isFailed = true
                                    }
                                    is LivenessDetector.ValidationResult.Failed -> {
                                        failureReason = "Liveness verification failed."
                                        isFailed = true
                                    }
                                    else -> {}
                                }
                            }

                            // Step B: Once liveness is verified, perform face embedding comparison
                            if (livenessVerified && !faceVerified && bitmap != null && !isVerifying) {
                                isVerifying = true
                                scope.launch {
                                    faceState = "Comparing MobileFaceNet neural embedding..."
                                    val cropped = processor.cropFaceBitmap(bitmap, face.boundingBox)
                                    if (cropped == null) {
                                        isVerifying = false
                                        return@launch
                                    }

                                    if (isLegacyProfile) {
                                        isVerifying = false
                                        faceVerified = false
                                        faceState = "Legacy profile detected"
                                        failureReason = "Biometric security upgraded to MobileFaceNet neural model. Please re-register your face profile in Profile -> Face Registration."
                                        isFailed = true
                                        return@launch
                                    }

                                    if (registeredEmbedding.isEmpty() || registeredEmbedding.size != FaceEmbeddingProcessor.EMBEDDING_DIM) {
                                        isVerifying = false
                                        faceVerified = false
                                        faceState = "Face profile not found"
                                        failureReason = "No registered 192-d MobileFaceNet profile found for authenticated student ($currentAuthUid)."
                                        isFailed = true
                                        return@launch
                                    }

                                    val liveEmbedding = processor.generateEmbedding(cropped, face)
                                    if (liveEmbedding.isEmpty() || liveEmbedding.size != FaceEmbeddingProcessor.EMBEDDING_DIM) {
                                        isVerifying = false
                                        faceVerified = false
                                        faceState = "Embedding extraction failed"
                                        failureReason = "Failed to extract 192-d live facial embedding from camera frame."
                                        isFailed = true
                                        return@launch
                                    }

                                    val score = processor.cosineSimilarity(liveEmbedding, registeredEmbedding)
                                    similarityScore = score

                                    // Calibration check: compare against calibrated threshold
                                    if (score >= FaceEmbeddingProcessor.DEFAULT_SIMILARITY_THRESHOLD) {
                                        faceVerified = true
                                        faceState = "✓ Face verified (${(score * 100).toInt()}%)"
                                        reticleColor = StatusPresentGreenText

                                        // Mark attendance in Firestore with all 3 verifications
                                        val (success, error) = onVerifyAndMarkAttendance(
                                            deviceId,
                                            sessionToken,
                                            true, // bleVerified
                                            true, // faceVerified
                                            true  // livenessVerified
                                        )

                                        isVerifying = false
                                        if (success) {
                                            isSuccess = true
                                        } else {
                                            isFailed = true
                                            failureReason = error
                                        }
                                    } else {
                                        // Biometric match failed
                                        isVerifying = false
                                        faceVerified = false
                                        faceState = "✗ Face does not match registered student"
                                        failureReason = "Face does not match registered student (Score: ${(score * 100).toInt()}%, Required: ${(FaceEmbeddingProcessor.DEFAULT_SIMILARITY_THRESHOLD * 100).toInt()}%)."
                                        isFailed = true
                                    }
                                }
                            }
                        }
                    )

                    // Floating prompt instruction badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = if (livenessVerified) "Biometric match in progress..." else livenessState,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color.White
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (isVerifying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Matching Face Embedding...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // Bottom disclaimer & instruction card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Security",
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Anti-Proxy Attendance Notice",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Attendance requires BOTH Bluetooth classroom device proximity AND on-device biometric face verification with basic liveness check. Proxy check-ins are rejected.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

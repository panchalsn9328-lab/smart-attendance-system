package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Student Face Registration Screen
 *
 * Implements the registration process:
 * 1. Request CAMERA permission.
 * 2. Open front camera.
 * 3. Detect exactly one face in the frame.
 * 4. Guide the student to look directly at the camera.
 * 5. Capture multiple suitable face samples from slightly different angles (Center, Left, Right).
 * 6. Generate numerical face embedding using on-device biometric processor.
 * 7. Store resulting 128-dimensional embedding in Firebase Firestore under students/{studentId}.
 *
 * Privacy:
 * - On-device processing only.
 * - Raw face photographs are NOT uploaded to Firebase Storage or cloud servers.
 */
@Composable
fun FaceRegistrationScreen(
    student: Student?,
    onNavigateBack: () -> Unit,
    onRegistrationComplete: () -> Unit,
    onSaveEmbedding: (List<Float>, (Boolean, String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val processor = remember { FaceEmbeddingProcessor(context) }

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

    BackHandler {
        onNavigateBack()
    }

    // Step state machine for multi-angle sample capture
    // Sample 1: Look straight ahead (center)
    // Sample 2: Turn head slightly left
    // Sample 3: Turn head slightly right
    var currentSampleStage by remember { mutableIntStateOf(1) } // 1, 2, 3, or 4 (done)
    val collectedEmbeddings = remember { mutableStateListOf<List<Float>>() }

    var feedbackMessage by remember { mutableStateOf("Position your face inside the oval frame") }
    var warningMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var sampleProgress by remember { mutableFloatStateOf(0.0f) }
    var reticleBorderColor by remember { mutableStateOf(BleBlue) }

    // Throttle capture cooldown so samples are not taken from consecutive millisecond frames
    var lastCaptureTime by remember { mutableStateOf(0L) }

    fun captureSample(face: Face, bitmap: Bitmap?) {
        val now = System.currentTimeMillis()
        if (now - lastCaptureTime < 1400) return // Allow 1.4s gap between angle transitions
        if (bitmap == null) return

        val cropped = processor.cropFaceBitmap(bitmap, face.boundingBox) ?: return
        val embedding = processor.generateEmbedding(cropped, face)

        if (embedding.isNotEmpty() && embedding.size == FaceEmbeddingProcessor.EMBEDDING_DIM) {
            collectedEmbeddings.add(embedding)
            lastCaptureTime = now

            when (currentSampleStage) {
                1 -> {
                    currentSampleStage = 2
                    sampleProgress = 0.33f
                    feedbackMessage = "Angle 1 captured! Now turn your head slightly left"
                    reticleBorderColor = StatusPresentGreenText
                }
                2 -> {
                    currentSampleStage = 3
                    sampleProgress = 0.66f
                    feedbackMessage = "Angle 2 captured! Now turn your head slightly right"
                    reticleBorderColor = StatusPresentGreenText
                }
                3 -> {
                    currentSampleStage = 4
                    sampleProgress = 1.0f
                    feedbackMessage = "All 3 angles captured! Generating MobileFaceNet biometric embedding..."
                    reticleBorderColor = StatusPresentGreenText

                    // Process and upload to Firebase
                    isProcessing = true
                    scope.launch {
                        delay(600) // Brief UI animation
                        val averagedEmbedding = processor.computeAveragedEmbedding(collectedEmbeddings.toList())
                        if (averagedEmbedding.size != FaceEmbeddingProcessor.EMBEDDING_DIM) {
                            isProcessing = false
                            errorMessage = "Biometric processor produced invalid embedding dimension. Please try again."
                            return@launch
                        }
                        onSaveEmbedding(averagedEmbedding) { success, error ->
                            isProcessing = false
                            if (success) {
                                isSuccess = true
                            } else {
                                errorMessage = error ?: "Failed to save face profile"
                            }
                        }
                    }
                }
            }
        }
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
                        modifier = Modifier.testTag("btn_back_face_reg")
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
                            text = "Face Registration",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = student?.name ?: "Student Biometric Profile",
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
        if (!hasCameraPermission) {
            // Permission request prompt
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

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Camera access is required to capture your face embedding on-device. Images are processed locally and never stored.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = TextSecondary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                        .testTag("btn_grant_camera_perm")
                ) {
                    Text("Grant Camera Permission", fontWeight = FontWeight.SemiBold)
                }
            }
        } else if (isSuccess) {
            // Success Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(28.dp),
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
                    text = "Face Registered Successfully!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your 192-dimensional MobileFaceNet biometric embedding has been securely linked to your student ID (${student?.enrollmentNumber ?: ""}). You can now complete BLE attendance with dual-verification.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = TextSecondary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onRegistrationComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(50.dp)
                        .testTag("btn_face_reg_done")
                ) {
                    Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Active Registration Camera View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Top Progress and Instruction Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sample ${currentSampleStage.coerceAtMost(3)} of 3",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimaryBlue
                            )
                        )
                        Text(
                            text = "${(sampleProgress * 100).toInt()}% completed",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { sampleProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = BrandPrimaryBlue,
                        trackColor = PaletteLightGray
                    )
                }

                // Camera Preview Box with Oval Guide
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
                        reticleColor = reticleBorderColor,
                        onFrameAnalyzed = { _, faces, bitmap ->
                            if (currentSampleStage > 3 || isProcessing || isSuccess) return@CameraPreviewWithOverlay

                            if (faces.isEmpty()) {
                                warningMessage = "Face not detected. Look at the camera."
                                reticleBorderColor = PaletteGray
                                return@CameraPreviewWithOverlay
                            }

                            if (faces.size > 1) {
                                warningMessage = "Multiple faces detected! Ensure only one person is in view."
                                reticleBorderColor = StatusAbsentRedText
                                return@CameraPreviewWithOverlay
                            }

                            warningMessage = null
                            val face = faces[0]
                            val yaw = face.headEulerAngleY

                            when (currentSampleStage) {
                                1 -> {
                                    // Step 1: Straight ahead
                                    if (abs(yaw) < 10f) {
                                        reticleBorderColor = BrandPrimaryBlue
                                        feedbackMessage = "Looking straight — capturing center sample..."
                                        captureSample(face, bitmap)
                                    } else {
                                        feedbackMessage = "Look straight ahead at the front camera"
                                        reticleBorderColor = BleBlue
                                    }
                                }
                                2 -> {
                                    // Step 2: Turn slightly left
                                    if (yaw < -12f || yaw > 12f) {
                                        reticleBorderColor = BrandPrimaryBlue
                                        feedbackMessage = "Angle detected — capturing left sample..."
                                        captureSample(face, bitmap)
                                    } else {
                                        feedbackMessage = "Turn your head slightly to the left"
                                    }
                                }
                                3 -> {
                                    // Step 3: Turn slightly right
                                    if (abs(yaw) > 12f) {
                                        reticleBorderColor = BrandPrimaryBlue
                                        feedbackMessage = "Angle detected — capturing right sample..."
                                        captureSample(face, bitmap)
                                    } else {
                                        feedbackMessage = "Turn your head slightly to the right"
                                    }
                                }
                            }
                        }
                    )

                    // On-Camera Instruction Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = warningMessage ?: feedbackMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (warningMessage != null) Color(0xFFFFB4AB) else Color.White
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Generating numerical embedding...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // Error alert if saving failed
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusAbsentRedBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = StatusAbsentRedText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = StatusAbsentRedText,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                // Bottom Step Information Card
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
                                contentDescription = "Info",
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Biometric Registration Guide",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Exactly 1 face must be present in the reticle.\n• 3 angle samples are captured automatically to build an invariant 192-dimensional MobileFaceNet embedding.\n• No raw face photos are ever uploaded or stored.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 17.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

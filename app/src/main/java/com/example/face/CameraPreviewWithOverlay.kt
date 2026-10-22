package com.example.face

import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.face.Face
import java.util.concurrent.Executors

/**
 * Jetpack Compose Front Camera Preview with biometric reticle oval overlay
 */
@Composable
fun CameraPreviewWithOverlay(
    modifier: Modifier = Modifier,
    reticleColor: Color = Color(0xFF2563EB), // BleBlue / BrandPrimary
    onFrameAnalyzed: (ImageProxy, List<Face>, Bitmap?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val faceDetectorHelper = remember { FaceDetectionHelper() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            faceDetectorHelper.close()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val task = faceDetectorHelper.detectFaces(imageProxy)
                        if (task != null) {
                            task.addOnSuccessListener { faces ->
                                val bitmap = faceDetectorHelper.imageProxyToBitmap(imageProxy)
                                onFrameAnalyzed(imageProxy, faces, bitmap)
                                imageProxy.close()
                            }.addOnFailureListener {
                                imageProxy.close()
                            }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom biometric face oval guide overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ovalWidth = size.width * 0.70f
            val ovalHeight = size.height * 0.55f
            val left = (size.width - ovalWidth) / 2f
            val top = (size.height - ovalHeight) / 2.3f

            // Translucent dark vignette around the face target
            // Draw face guide oval with smooth rounded borders
            drawRoundRect(
                color = reticleColor,
                topLeft = Offset(left, top),
                size = Size(ovalWidth, ovalHeight),
                cornerRadius = CornerRadius(ovalWidth / 2f, ovalHeight / 2f),
                style = Stroke(
                    width = 4.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), 0f)
                )
            )

            // Four subtle alignment corner tick marks
            val cornerLen = 30f
            val strokeW = 6f

            // Top-left
            drawLine(reticleColor, Offset(left, top + cornerLen), Offset(left, top), strokeW)
            drawLine(reticleColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)

            // Top-right
            drawLine(reticleColor, Offset(left + ovalWidth - cornerLen, top), Offset(left + ovalWidth, top), strokeW)
            drawLine(reticleColor, Offset(left + ovalWidth, top), Offset(left + ovalWidth, top + cornerLen), strokeW)

            // Bottom-left
            drawLine(reticleColor, Offset(left, top + ovalHeight - cornerLen), Offset(left, top + ovalHeight), strokeW)
            drawLine(reticleColor, Offset(left, top + ovalHeight), Offset(left + cornerLen, top + ovalHeight), strokeW)

            // Bottom-right
            drawLine(reticleColor, Offset(left + ovalWidth - cornerLen, top + ovalHeight), Offset(left + ovalWidth, top + ovalHeight), strokeW)
            drawLine(reticleColor, Offset(left + ovalWidth, top + ovalHeight), Offset(left + ovalWidth, top + ovalHeight - cornerLen), strokeW)
        }
    }
}

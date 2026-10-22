package com.example.face

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Helper class for Google ML Kit Face Detection integrated with CameraX ImageProxy
 */
class FaceDetectionHelper {

    private val detector: FaceDetector

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
        detector = FaceDetection.getClient(options)
    }

    @OptIn(ExperimentalGetImage::class)
    fun detectFaces(imageProxy: ImageProxy): Task<List<Face>>? {
        val mediaImage = imageProxy.image ?: return null
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        return detector.process(inputImage)
    }

    /**
     * Converts a CameraX ImageProxy (YUV_420_888 format) to a rotated, aligned Bitmap
     */
    @OptIn(ExperimentalGetImage::class)
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        return try {
            val yBuffer: ByteBuffer = image.planes[0].buffer
            val uBuffer: ByteBuffer = image.planes[1].buffer
            val vBuffer: ByteBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 85, out)
            val imageBytes = out.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            if (rotationDegrees != 0 && bitmap != null) {
                val matrix = Matrix().apply {
                    postRotate(rotationDegrees.toFloat())
                    // Mirror horizontally for front-facing camera selfie view
                    postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        try {
            detector.close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}

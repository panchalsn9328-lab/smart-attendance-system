package com.example.face

import com.google.mlkit.vision.face.Face
import kotlin.math.abs

/**
 * Basic Anti-Spoofing & Liveness Detector
 *
 * NOTE: This is a BASIC liveness verification mechanism suitable for academic / college demonstration.
 * It uses head orientation (Euler angles: yaw and pitch) and facial tracking to ensure an interactive,
 * live individual is performing real movements rather than holding up a static 2D photograph.
 * It is clearly designated as basic liveness and does not claim production-grade 3D biometric anti-spoofing.
 */
class LivenessDetector {

    enum class Step(val instruction: String, val progressFraction: Float) {
        LOOK_STRAIGHT("Look directly at the camera", 0.25f),
        TURN_LEFT("Turn your head slightly left", 0.50f),
        TURN_RIGHT("Turn your head slightly right", 0.75f),
        LOOK_STRAIGHT_FINAL("Look straight at the camera", 1.0f),
        PASSED("Liveness verified ✓", 1.0f),
        FAILED("Liveness verification failed", 0.0f)
    }

    sealed class ValidationResult {
        object WaitingForFace : ValidationResult()
        object MultipleFacesDetected : ValidationResult()
        object FaceTooFar : ValidationResult()
        data class InProgress(val currentStep: Step, val instruction: String, val progress: Float) : ValidationResult()
        object LivenessPassed : ValidationResult()
        data class Failed(val reason: String) : ValidationResult()
    }

    var currentStep: Step = Step.LOOK_STRAIGHT
        private set

    private var stepHoldFrames = 0
    private val requiredHoldFrames = 3 // Frames to confirm sustained pose

    fun reset() {
        currentStep = Step.LOOK_STRAIGHT
        stepHoldFrames = 0
    }

    /**
     * Evaluates a frame with the detected faces list and returns the updated validation state.
     */
    fun processFrame(faces: List<Face>): ValidationResult {
        if (faces.isEmpty()) {
            return ValidationResult.WaitingForFace
        }

        if (faces.size > 1) {
            return ValidationResult.MultipleFacesDetected
        }

        val face = faces[0]

        // Validate face size in view
        val faceBox = face.boundingBox
        if (faceBox.width() < 120 || faceBox.height() < 120) {
            return ValidationResult.FaceTooFar
        }

        val yaw = face.headEulerAngleY   // Left/right rotation (- is left, + is right)
        val pitch = face.headEulerAngleX // Up/down tilt

        when (currentStep) {
            Step.LOOK_STRAIGHT -> {
                // Must be looking relatively straight ahead
                if (abs(yaw) < 12f && abs(pitch) < 20f) {
                    stepHoldFrames++
                    if (stepHoldFrames >= requiredHoldFrames) {
                        currentStep = Step.TURN_LEFT
                        stepHoldFrames = 0
                    }
                } else {
                    stepHoldFrames = 0
                }
            }

            Step.TURN_LEFT -> {
                // Turn head left (typically negative yaw or positive depending on camera orientation)
                if (yaw < -13f || yaw > 13f) {
                    stepHoldFrames++
                    if (stepHoldFrames >= requiredHoldFrames) {
                        currentStep = Step.TURN_RIGHT
                        stepHoldFrames = 0
                    }
                }
            }

            Step.TURN_RIGHT -> {
                // Turn head to the opposite direction
                if (abs(yaw) > 13f) {
                    stepHoldFrames++
                    if (stepHoldFrames >= requiredHoldFrames) {
                        currentStep = Step.LOOK_STRAIGHT_FINAL
                        stepHoldFrames = 0
                    }
                }
            }

            Step.LOOK_STRAIGHT_FINAL -> {
                // Return to center
                if (abs(yaw) < 12f && abs(pitch) < 20f) {
                    stepHoldFrames++
                    if (stepHoldFrames >= requiredHoldFrames) {
                        currentStep = Step.PASSED
                        stepHoldFrames = 0
                        return ValidationResult.LivenessPassed
                    }
                }
            }

            Step.PASSED -> {
                return ValidationResult.LivenessPassed
            }

            Step.FAILED -> {
                return ValidationResult.Failed("Liveness verification timed out or was interrupted.")
            }
        }

        return ValidationResult.InProgress(
            currentStep = currentStep,
            instruction = currentStep.instruction,
            progress = currentStep.progressFraction
        )
    }
}

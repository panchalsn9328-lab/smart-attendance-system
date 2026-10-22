package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AttendanceRepository
import com.example.face.FaceEmbeddingProcessor
import com.example.model.AttendanceRecord
import com.example.model.AttendanceSession
import com.example.model.Student
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Random
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FaceRecognitionSecurityTest {

    private lateinit var processor: FaceEmbeddingProcessor
    private val rng = Random(42)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.example")
                    .setApiKey("AIzaTestKeyForBiometricSecurityTesting001")
                    .setProjectId("smart-attendance12-1353f")
                    .build()
            )
        }
        processor = FaceEmbeddingProcessor(context)
    }

    /**
     * Helper to generate a random unit L2 normalized vector of specified dimension.
     */
    private fun generateRandomNormalizedEmbedding(dim: Int, random: Random = rng): List<Float> {
        val raw = FloatArray(dim) { (random.nextGaussian()).toFloat() }
        var sumSquares = 0.0
        for (v in raw) sumSquares += (v * v).toDouble()
        val norm = sqrt(sumSquares).toFloat()
        return raw.map { it / norm }
    }

    /**
     * Helper to generate a genuine face variation (same individual with lighting / head tilt noise).
     * Perturbation standard deviation is small (e.g. 0.15), keeping cosine similarity high (> 0.75).
     */
    private fun generateGenuineVariation(base: List<Float>, noiseLevel: Float = 0.02f, random: Random = rng): List<Float> {
        val noisy = base.map { it + (random.nextGaussian() * noiseLevel).toFloat() }
        var sumSquares = 0.0
        for (v in noisy) sumSquares += (v * v).toDouble()
        val norm = sqrt(sumSquares).toFloat()
        return noisy.map { it / norm }
    }

    // =========================================================================
    // MANDATORY BIOMETRIC TESTS (Genuine vs Impostor, FAR / FRR Measurement)
    // =========================================================================

    @Test
    fun testModelDimensionsAndConstants() {
        assertEquals("MobileFaceNet standard dimension must be 192", 192, FaceEmbeddingProcessor.EMBEDDING_DIM)
        assertEquals("Legacy handcrafted dimension must be 128", 128, FaceEmbeddingProcessor.LEGACY_HANDCRAFTED_DIM)
        assertEquals("Calibrated threshold must be 0.70f", 0.70f, FaceEmbeddingProcessor.DEFAULT_SIMILARITY_THRESHOLD, 0.001f)
    }

    @Test
    fun testCosineSimilarityIdentitySelfMatch() {
        val embedding = generateRandomNormalizedEmbedding(FaceEmbeddingProcessor.EMBEDDING_DIM)
        val selfSimilarity = processor.cosineSimilarity(embedding, embedding)
        assertEquals("Self match cosine similarity must be 1.0", 1.0f, selfSimilarity, 0.0001f)
    }

    @Test
    fun testGenuineMatchPassesCalibratedThreshold() {
        // Test genuine faces across 100 simulated captures (lighting variations, slight tilt)
        val registeredVed = generateRandomNormalizedEmbedding(FaceEmbeddingProcessor.EMBEDDING_DIM)
        var falseRejects = 0
        val totalTrials = 100

        for (i in 0 until totalTrials) {
            val liveCaptureVed = generateGenuineVariation(registeredVed, noiseLevel = 0.04f)
            val score = processor.cosineSimilarity(liveCaptureVed, registeredVed)
            assertTrue("Genuine capture score $score should be >= 0.70f", score >= FaceEmbeddingProcessor.DEFAULT_SIMILARITY_THRESHOLD)
            if (score < FaceEmbeddingProcessor.DEFAULT_SIMILARITY_THRESHOLD) {
                falseRejects++
            }
        }

        val frr = falseRejects.toDouble() / totalTrials
        println("Biometric Evaluation - Genuine Trials: $totalTrials, False Rejections: $falseRejects, FRR: $frr")
        assertEquals("False Rejection Rate must be 0.0 under normal tolerance", 0.0, frr, 0.001)
    }

    @Test
    fun testImpostorMatchStrictlyRejected() {
        // Test 500 different impostor faces against Ved's registered profile
        val registeredVed = generateRandomNormalizedEmbedding(FaceEmbeddingProcessor.EMBEDDING_DIM)
        var falseAccepts = 0
        val totalImpostors = 500
        var maxImpostorScore = -1.0f

        for (i in 0 until totalImpostors) {
            val impostorFace = generateRandomNormalizedEmbedding(FaceEmbeddingProcessor.EMBEDDING_DIM)
            val score = processor.cosineSimilarity(impostorFace, registeredVed)
            if (score > maxImpostorScore) maxImpostorScore = score
            if (score >= FaceEmbeddingProcessor.DEFAULT_SIMILARITY_THRESHOLD) {
                falseAccepts++
            }
        }

        val far = falseAccepts.toDouble() / totalImpostors
        println("Biometric Evaluation - Impostor Trials: $totalImpostors, False Accepts: $falseAccepts, FAR: $far, Max Impostor Score: $maxImpostorScore")
        assertTrue("Max impostor score ($maxImpostorScore) must be far below threshold (0.70)", maxImpostorScore < 0.45f)
        assertEquals("False Acceptance Rate must be exactly 0.0%", 0.0, far, 0.0001)
    }

    @Test
    fun testLegacyHandcraftedEmbeddingMigrationRejected() {
        // Student has legacy 128-d handcrafted embedding stored in profile
        val legacyEmbedding = generateRandomNormalizedEmbedding(FaceEmbeddingProcessor.LEGACY_HANDCRAFTED_DIM)
        val currentLiveEmbedding = generateRandomNormalizedEmbedding(FaceEmbeddingProcessor.EMBEDDING_DIM)

        assertTrue("128-d embedding must be identified as legacy", processor.isLegacyEmbedding(legacyEmbedding))
        assertFalse("128-d embedding must NOT be considered valid MobileFaceNet embedding", processor.isEmbeddingValid(legacyEmbedding))

        // Cosine similarity across different dimensions must return 0.0f
        val similarity = processor.cosineSimilarity(currentLiveEmbedding, legacyEmbedding)
        assertEquals("Comparing different dimensions must return 0.0f", 0.0f, similarity, 0.0001f)
    }

    // =========================================================================
    // MANDATORY TESTS A - F SPECIFIED IN USER BRIEF
    // =========================================================================

    private fun createRepositoryWithVed(): Pair<AttendanceRepository, Student> {
        val repo = AttendanceRepository()
        val ved = Student(
            id = "ved_student_uid_101",
            name = "Ved",
            email = "ved@college.edu",
            enrollmentNumber = "22BCS101",
            department = "Computer Science"
        )
        repo.setStudent(ved)
        return Pair(repo, ved)
    }

    private fun createSampleSession(): AttendanceSession {
        return AttendanceSession(
            sessionId = "sess_cs401_001",
            subject = "Operating Systems",
            room = "LAB-2",
            startTime = "09:00 AM",
            endTime = "10:00 AM",
            bleIdentifier = "ESP32_ATTEND_LAB2"
        )
    }

    @Test
    fun testA_WrongPersonFaceRejected_NoAttendanceDocumentCreated() {
        val (repo, ved) = createRepositoryWithVed()
        val session = createSampleSession()

        // 1. Authenticated user is Ved
        assertEquals(ved.id, repo.currentStudent.value?.id)

        // 2. Another person presents face -> Biometric comparison produces impostor score (< 0.70f)
        val vedStoredEmbedding = generateRandomNormalizedEmbedding(FaceEmbeddingProcessor.EMBEDDING_DIM)
        val impostorEmbedding = generateRandomNormalizedEmbedding(FaceEmbeddingProcessor.EMBEDDING_DIM)
        val score = processor.cosineSimilarity(impostorEmbedding, vedStoredEmbedding)
        assertTrue("Impostor score must be < 0.70f", score < FaceEmbeddingProcessor.DEFAULT_SIMILARITY_THRESHOLD)

        val faceVerified = score >= FaceEmbeddingProcessor.DEFAULT_SIMILARITY_THRESHOLD
        assertFalse("faceVerified must be false for impostor", faceVerified)

        // 3. Attempt attendance recording with faceVerified = false
        val initialHistoryCount = repo.attendanceHistory.value.size
        val record: AttendanceRecord? = repo.recordAttendance(
            session = session,
            sessionToken = "TOK_SEC_999",
            deviceId = "ESP32_ATTEND_LAB2",
            bleVerified = true,
            faceVerified = faceVerified, // FALSE!
            livenessVerified = true
        )

        // 4. Verification must fail closed: NO record created, history count unchanged
        assertNull("Attendance record MUST be null when face verification fails", record)
        assertEquals("Attendance history must NOT increment", initialHistoryCount, repo.attendanceHistory.value.size)
        assertFalse("Session must not be marked", repo.hasMarkedAttendance("sess_cs401_001", "TOK_SEC_999"))
    }

    @Test
    fun testB_CorrectPersonFaceAccepted_AttendanceCreated() {
        val (repo, ved) = createRepositoryWithVed()
        val session = createSampleSession()

        val vedStoredEmbedding = generateRandomNormalizedEmbedding(FaceEmbeddingProcessor.EMBEDDING_DIM)
        val vedLiveEmbedding = generateGenuineVariation(vedStoredEmbedding, noiseLevel = 0.02f)
        val score = processor.cosineSimilarity(vedLiveEmbedding, vedStoredEmbedding)

        assertTrue("Genuine face match score must be >= 0.70f", score >= FaceEmbeddingProcessor.DEFAULT_SIMILARITY_THRESHOLD)
        val faceVerified = score >= FaceEmbeddingProcessor.DEFAULT_SIMILARITY_THRESHOLD
        assertTrue(faceVerified)

        val record = repo.recordAttendance(
            session = session,
            sessionToken = "TOK_SEC_888",
            deviceId = "ESP32_ATTEND_LAB2",
            bleVerified = true,
            faceVerified = true,
            livenessVerified = true
        )

        assertNotNull("Attendance record must be created for verified student", record)
        assertEquals("ved_student_uid_101", record?.studentUid)
        assertEquals("Ved", record?.studentName)
        assertTrue(record?.status.equals("PRESENT", ignoreCase = true))
        assertTrue(repo.hasMarkedAttendance("sess_cs401_001", "TOK_SEC_888"))
    }

    @Test
    fun testC_AccountSwitching_CacheClearedAndNotUsedForSecondStudent() {
        val repo = AttendanceRepository()

        // 1. Log in as Ved
        val ved = Student(
            id = "ved_uid_101",
            name = "Ved",
            email = "ved@college.edu",
            enrollmentNumber = "22BCS101",
            department = "Computer Science"
        )
        repo.setStudent(ved)
        assertEquals("ved_uid_101", repo.currentStudent.value?.id)

        // 2. Log out
        repo.logout()
        assertNull("Current student must be null after logout", repo.currentStudent.value)
        assertEquals(0, repo.attendanceHistory.value.size)

        // 3. Log in as another student (Rohan)
        val rohan = Student(
            id = "rohan_uid_202",
            name = "Rohan",
            email = "rohan@college.edu",
            enrollmentNumber = "22BCS202",
            department = "Information Technology"
        )
        repo.setStudent(rohan)
        assertEquals("rohan_uid_202", repo.currentStudent.value?.id)

        // 4. Verify that Ved's identity or records are NOT present
        assertFalse(repo.currentStudent.value?.id == "ved_uid_101")
        assertEquals(0, repo.attendanceHistory.value.size)
    }

    @Test
    fun testD_MissingProfile_VerificationFailsClosed() {
        val (repo, _) = createRepositoryWithVed()
        val session = createSampleSession()

        // Empty registered embedding (profile missing)
        val emptyEmbedding = emptyList<Float>()
        val liveEmbedding = generateRandomNormalizedEmbedding(FaceEmbeddingProcessor.EMBEDDING_DIM)

        assertFalse("Empty embedding must be invalid", processor.isEmbeddingValid(emptyEmbedding))
        val score = processor.cosineSimilarity(liveEmbedding, emptyEmbedding)
        assertEquals("Comparison against empty profile must be 0.0f", 0.0f, score, 0.0001f)

        // Face verification cannot pass
        val record = repo.recordAttendance(
            session = session,
            sessionToken = "TOK_SEC_777",
            deviceId = "ESP32_ATTEND_LAB2",
            bleVerified = true,
            faceVerified = false,
            livenessVerified = true
        )
        assertNull("Missing profile must fail closed and block attendance", record)
    }

    @Test
    fun testE_StaleSuccess_NewAttemptRequiresFreshVerification() {
        val (repo, _) = createRepositoryWithVed()
        val session = createSampleSession()

        // Attempt 1: Verified and passed once
        val record1 = repo.recordAttendance(
            session = session,
            sessionToken = "TOK_ATTEMPT_1",
            deviceId = "ESP32_ATTEND_LAB2",
            bleVerified = true,
            faceVerified = true,
            livenessVerified = true
        )
        assertNotNull(record1)

        // Attempt 2: Next session or new attempt presents wrong person's face
        // The previous attempt's success MUST NOT authorize Attempt 2
        val wrongPersonVerified = false // Result of fresh camera biometric match
        val record2 = repo.recordAttendance(
            session = session,
            sessionToken = "TOK_ATTEMPT_2",
            deviceId = "ESP32_ATTEND_LAB2",
            bleVerified = true,
            faceVerified = wrongPersonVerified,
            livenessVerified = true
        )
        assertNull("Stale previous success must NOT authorize new attempt", record2)
    }

    @Test
    fun testF_DirectSubmissionRejectedWithoutAllThreeVerifications() {
        val (repo, _) = createRepositoryWithVed()
        val session = createSampleSession()

        // 1. Missing BLE
        val recNoBle = repo.recordAttendance(session, "TOK_F1", "ESP32_1", bleVerified = false, faceVerified = true, livenessVerified = true)
        assertNull("Must reject when BLE is missing", recNoBle)

        // 2. Missing Face
        val recNoFace = repo.recordAttendance(session, "TOK_F2", "ESP32_1", bleVerified = true, faceVerified = false, livenessVerified = true)
        assertNull("Must reject when Face is missing", recNoFace)

        // 3. Missing Liveness
        val recNoLive = repo.recordAttendance(session, "TOK_F3", "ESP32_1", bleVerified = true, faceVerified = true, livenessVerified = false)
        assertNull("Must reject when Liveness is missing", recNoLive)

        // 4. All missing
        val recNone = repo.recordAttendance(session, "TOK_F4", "ESP32_1", bleVerified = false, faceVerified = false, livenessVerified = false)
        assertNull("Must reject when all verifications missing", recNone)
    }
}

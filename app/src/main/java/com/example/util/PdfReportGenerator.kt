package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.AttendanceRecord
import com.example.model.AttendanceSession
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val TAG = "PdfReportGenerator"
    private const val PAGE_WIDTH = 595 // Standard A4 width in points
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points

    fun generatePdfReport(
        context: Context,
        session: AttendanceSession,
        records: List<AttendanceRecord>
    ): File? {
        val pdfDoc = PdfDocument()

        return try {
            val totalStudents = if (session.totalStudents > 0) session.totalStudents else maxOf(records.size, 30)
            val presentCount = records.count { it.status.equals("PRESENT", ignoreCase = true) }
            val absentCount = maxOf(0, totalStudents - presentCount)
            val percentage = if (totalStudents > 0) (presentCount * 100) / totalStudents else 0

            val titlePaint = Paint().apply {
                color = Color.rgb(27, 85, 226) // Deep Brand Blue
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val sectionTitlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val labelPaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val valuePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
                isAntiAlias = true
            }

            val tableHeaderBgPaint = Paint().apply {
                color = Color.rgb(241, 245, 249)
                style = Paint.Style.FILL
            }

            val tableRowBgPaint = Paint().apply {
                color = Color.rgb(248, 250, 252)
                style = Paint.Style.FILL
            }

            val presentBadgeBgPaint = Paint().apply {
                color = Color.rgb(220, 252, 231) // Light green
                style = Paint.Style.FILL
            }

            val presentBadgeTextPaint = Paint().apply {
                color = Color.rgb(22, 101, 52) // Dark green
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val absentBadgeBgPaint = Paint().apply {
                color = Color.rgb(254, 226, 226) // Light red
                style = Paint.Style.FILL
            }

            val absentBadgeTextPaint = Paint().apply {
                color = Color.rgb(153, 27, 27) // Dark red
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdfDoc.startPage(pageInfo)
            var canvas = page.canvas

            // Left & Right Margins
            val marginX = 40f
            var currentY = 45f

            // Header Banner
            canvas.drawText("SMART ATTENDANCE", marginX, currentY, titlePaint)
            currentY += 18f
            canvas.drawText("Automated BLE & ESP32 Attendance Report", marginX, currentY, subtitlePaint)
            currentY += 15f
            canvas.drawLine(marginX, currentY, PAGE_WIDTH - marginX, currentY, linePaint)
            currentY += 20f

            // Session Metadata (2 columns)
            canvas.drawText("SESSION DETAILS", marginX, currentY, sectionTitlePaint)
            currentY += 16f

            val col2X = 300f
            // Row 1
            canvas.drawText("Class:", marginX, currentY, labelPaint)
            canvas.drawText(session.classId.ifBlank { "CN-301" }, marginX + 60f, currentY, valuePaint)
            canvas.drawText("Date:", col2X, currentY, labelPaint)
            val effDate = session.date.ifBlank {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            }
            canvas.drawText(effDate, col2X + 65f, currentY, valuePaint)
            currentY += 14f

            // Row 2
            canvas.drawText("Subject:", marginX, currentY, labelPaint)
            canvas.drawText(session.subject.ifBlank { "Computer Networks" }, marginX + 60f, currentY, valuePaint)
            canvas.drawText("Start Time:", col2X, currentY, labelPaint)
            canvas.drawText(session.startTime.ifBlank { "N/A" }, col2X + 65f, currentY, valuePaint)
            currentY += 14f

            // Row 3
            canvas.drawText("Faculty:", marginX, currentY, labelPaint)
            canvas.drawText(session.professorName.ifBlank { "Prof. Priya Sharma" }, marginX + 60f, currentY, valuePaint)
            canvas.drawText("End Time:", col2X, currentY, labelPaint)
            canvas.drawText(session.endTime.ifBlank { "Ongoing" }, col2X + 65f, currentY, valuePaint)
            currentY += 14f

            // Row 4
            canvas.drawText("Room:", marginX, currentY, labelPaint)
            canvas.drawText(session.room.ifBlank { "CN-301" }, marginX + 60f, currentY, valuePaint)
            canvas.drawText("ESP32 Device:", col2X, currentY, labelPaint)
            val effDevice = session.deviceName.ifBlank { session.bleIdentifier.ifBlank { "BLE_Attendance" } }
            canvas.drawText(effDevice, col2X + 65f, currentY, valuePaint)
            currentY += 14f

            // Row 5
            canvas.drawText("Device ID:", marginX, currentY, labelPaint)
            canvas.drawText(session.deviceId.ifBlank { "CLASSROOM_ESP32_01" }, marginX + 60f, currentY, valuePaint)
            canvas.drawText("Session Status:", col2X, currentY, labelPaint)
            canvas.drawText(session.status.uppercase(), col2X + 65f, currentY, valuePaint)
            currentY += 18f

            canvas.drawLine(marginX, currentY, PAGE_WIDTH - marginX, currentY, linePaint)
            currentY += 18f

            // Attendance Summary Box
            canvas.drawText("ATTENDANCE SUMMARY", marginX, currentY, sectionTitlePaint)
            currentY += 12f

            val boxWidth = (PAGE_WIDTH - (marginX * 2)) / 4f
            val boxHeight = 44f
            val summaryMetrics = listOf(
                Pair("Total Students", "$totalStudents"),
                Pair("Present", "$presentCount"),
                Pair("Absent", "$absentCount"),
                Pair("Percentage", "$percentage%")
            )

            for (i in summaryMetrics.indices) {
                val boxX = marginX + (i * boxWidth)
                val rect = RectF(boxX + 2f, currentY, boxX + boxWidth - 2f, currentY + boxHeight)
                val statBgPaint = Paint().apply {
                    color = when (i) {
                        1 -> Color.rgb(240, 253, 244)
                        2 -> Color.rgb(254, 242, 242)
                        3 -> Color.rgb(239, 246, 255)
                        else -> Color.rgb(248, 250, 252)
                    }
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rect, 6f, 6f, statBgPaint)

                val statValPaint = Paint().apply {
                    color = when (i) {
                        1 -> Color.rgb(22, 101, 52)
                        2 -> Color.rgb(153, 27, 27)
                        3 -> Color.rgb(29, 78, 216)
                        else -> Color.rgb(15, 23, 42)
                    }
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                canvas.drawText(summaryMetrics[i].first, boxX + 8f, currentY + 16f, labelPaint)
                canvas.drawText(summaryMetrics[i].second, boxX + 8f, currentY + 34f, statValPaint)
            }
            currentY += boxHeight + 20f

            canvas.drawLine(marginX, currentY, PAGE_WIDTH - marginX, currentY, linePaint)
            currentY += 18f

            // Student Attendance Table
            canvas.drawText("STUDENT ATTENDANCE", marginX, currentY, sectionTitlePaint)
            currentY += 12f

            // Table Header Row
            val colNoX = marginX + 6f
            val colNameX = marginX + 35f
            val colRollX = marginX + 220f
            val colStatusX = marginX + 350f
            val colTimeX = marginX + 430f

            canvas.drawRect(marginX, currentY, PAGE_WIDTH - marginX, currentY + 22f, tableHeaderBgPaint)
            canvas.drawText("No.", colNoX, currentY + 15f, labelPaint)
            canvas.drawText("Student Name", colNameX, currentY + 15f, labelPaint)
            canvas.drawText("Enrollment No.", colRollX, currentY + 15f, labelPaint)
            canvas.drawText("Status", colStatusX, currentY + 15f, labelPaint)
            canvas.drawText("Time", colTimeX, currentY + 15f, labelPaint)
            currentY += 24f

            if (records.isEmpty()) {
                canvas.drawText("No attendance records registered for this session.", marginX + 10f, currentY + 20f, valuePaint)
                currentY += 35f
            } else {
                for (index in records.indices) {
                    // Check if we need a new page
                    if (currentY > PAGE_HEIGHT - 60f) {
                        // Draw footer for current page
                        drawFooter(canvas, marginX, pageNumber)
                        pdfDoc.finishPage(page)

                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = pdfDoc.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = 45f

                        // Redraw table header on new page
                        canvas.drawText("STUDENT ATTENDANCE (CONT.)", marginX, currentY, sectionTitlePaint)
                        currentY += 15f
                        canvas.drawRect(marginX, currentY, PAGE_WIDTH - marginX, currentY + 22f, tableHeaderBgPaint)
                        canvas.drawText("No.", colNoX, currentY + 15f, labelPaint)
                        canvas.drawText("Student Name", colNameX, currentY + 15f, labelPaint)
                        canvas.drawText("Enrollment No.", colRollX, currentY + 15f, labelPaint)
                        canvas.drawText("Status", colStatusX, currentY + 15f, labelPaint)
                        canvas.drawText("Time", colTimeX, currentY + 15f, labelPaint)
                        currentY += 24f
                    }

                    val rec = records[index]
                    val isEven = index % 2 == 0
                    if (isEven) {
                        canvas.drawRect(marginX, currentY, PAGE_WIDTH - marginX, currentY + 20f, tableRowBgPaint)
                    }

                    canvas.drawText("${index + 1}.", colNoX, currentY + 14f, valuePaint)
                    val sName = if (rec.studentName.length > 28) rec.studentName.take(25) + "..." else rec.studentName
                    canvas.drawText(sName, colNameX, currentY + 14f, valuePaint)
                    canvas.drawText(rec.enrollmentNo.ifBlank { rec.studentUid.take(12) }, colRollX, currentY + 14f, valuePaint)

                    val isPresent = rec.status.equals("PRESENT", ignoreCase = true)
                    val badgeRect = RectF(colStatusX - 4f, currentY + 2f, colStatusX + 50f, currentY + 18f)
                    if (isPresent) {
                        canvas.drawRoundRect(badgeRect, 4f, 4f, presentBadgeBgPaint)
                        canvas.drawText("PRESENT", colStatusX + 4f, currentY + 13f, presentBadgeTextPaint)
                    } else {
                        canvas.drawRoundRect(badgeRect, 4f, 4f, absentBadgeBgPaint)
                        canvas.drawText("ABSENT", colStatusX + 6f, currentY + 13f, absentBadgeTextPaint)
                    }

                    canvas.drawText(rec.time.ifBlank { "--:--" }, colTimeX, currentY + 14f, valuePaint)
                    currentY += 20f
                }
            }

            // Footer
            drawFooter(canvas, marginX, pageNumber)
            pdfDoc.finishPage(page)

            // Save PDF to cacheDir/reports
            val reportsDir = File(context.cacheDir, "reports").apply {
                if (!exists()) mkdirs()
            }
            val sanitizedClass = session.classId.replace("[^a-zA-Z0-9]".toRegex(), "_").ifBlank { "CN301" }
            val sanitizedToken = session.sessionToken.take(6).ifBlank { "sess" }
            val fileName = "Attendance_${sanitizedClass}_${sanitizedToken}_${System.currentTimeMillis()}.pdf"
            val file = File(reportsDir, fileName)

            val outputStream = FileOutputStream(file)
            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDoc.close()

            Log.i(TAG, "Generated PDF at: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF report: ${e.message}", e)
            try { pdfDoc.close() } catch (_: Exception) {}
            null
        }
    }

    private fun drawFooter(canvas: Canvas, marginX: Float, pageNumber: Int) {
        val footerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val genTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $genTime  •  Smart Attendance Faculty Portal", marginX, PAGE_HEIGHT - 25f, footerPaint)
        canvas.drawText("Page $pageNumber", PAGE_WIDTH - marginX - 35f, PAGE_HEIGHT - 25f, footerPaint)
    }

    /**
     * Opens the generated PDF directly on Android with appropriate FileProvider and Intent.
     */
    fun openPdf(context: Context, pdfFile: File): Boolean {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(viewIntent, "Open Attendance Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open PDF intent: ${e.message}", e)
            false
        }
    }

    /**
     * Shares the generated PDF via Android system share sheet.
     */
    fun sharePdf(context: Context, pdfFile: File, session: AttendanceSession): Boolean {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Attendance Report - ${session.subject} (${session.classId})")
                putExtra(Intent.EXTRA_TEXT, "Attendance Report for ${session.subject} on ${session.date}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Attendance PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share PDF: ${e.message}", e)
            false
        }
    }
}

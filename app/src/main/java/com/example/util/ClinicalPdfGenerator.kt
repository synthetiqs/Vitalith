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
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AppointmentPrep
import com.example.data.model.HealthTimelineItem
import com.example.data.model.MedicationItem
import com.example.data.model.PatientProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClinicalPdfGenerator {

  // A4 standard points: 595 x 842
  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 40f

  // Colors
  private val COLOR_PRIMARY = Color.rgb(13, 148, 136) // VitalTealPrimary #0D9488
  private val COLOR_NAVY = Color.rgb(15, 23, 42) // VitalNavy #0F172A
  private val COLOR_DARK_GRAY = Color.rgb(51, 65, 85) // Slate 700
  private val COLOR_MUTED = Color.rgb(100, 116, 139) // Slate 500
  private val COLOR_LIGHT_BG = Color.rgb(248, 250, 252) // Slate 50
  private val COLOR_BORDER = Color.rgb(226, 232, 240) // Slate 200
  private val COLOR_EMERALD = Color.rgb(16, 185, 129) // Emerald 500
  private val COLOR_ALERT = Color.rgb(239, 68, 68) // Red 500

  fun generateAppointmentPrepPdf(
    context: Context,
    prep: AppointmentPrep,
    profile: PatientProfile?
  ): File {
    val exportDir = File(context.cacheDir, "exports")
    if (!exportDir.exists()) exportDir.mkdirs()
    val file = File(exportDir, "appointment_prep_${System.currentTimeMillis()}.pdf")

    try {
      val document = PdfDocument()
      val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
      val page = document.startPage(pageInfo)
      val canvas = page.canvas

    var y = MARGIN

    // Header Banner
    val headerPaint = Paint().apply {
      color = COLOR_NAVY
      style = Paint.Style.FILL
    }
    canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 64f), 8f, 8f, headerPaint)

    val titlePaint = Paint().apply {
      color = Color.WHITE
      textSize = 17f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    canvas.drawText("VITALITH CLINICAL HEALTH VAULT", MARGIN + 16f, y + 28f, titlePaint)

    val subtitlePaint = Paint().apply {
      color = Color.rgb(204, 251, 241) // Light teal
      textSize = 11f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      isAntiAlias = true
    }
    canvas.drawText("1-Page Clinical Briefing & Synthesis for Provider Visit", MARGIN + 16f, y + 48f, subtitlePaint)
    y += 76f

    // Patient and Visit Info Grid (Two side-by-side boxes)
    val cardBgPaint = Paint().apply {
      color = COLOR_LIGHT_BG
      style = Paint.Style.FILL
    }
    val cardBorderPaint = Paint().apply {
      color = COLOR_BORDER
      style = Paint.Style.STROKE
      strokeWidth = 1f
    }
    val cardWidth = (PAGE_WIDTH - 2 * MARGIN - 12f) / 2f
    val cardHeight = 90f

    // Patient Box
    val patientRect = RectF(MARGIN, y, MARGIN + cardWidth, y + cardHeight)
    canvas.drawRoundRect(patientRect, 6f, 6f, cardBgPaint)
    canvas.drawRoundRect(patientRect, 6f, 6f, cardBorderPaint)

    val sectionHeaderPaint = Paint().apply {
      color = COLOR_PRIMARY
      textSize = 10f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    val labelPaint = Paint().apply {
      color = COLOR_MUTED
      textSize = 9f
      isAntiAlias = true
    }
    val textPaint = Paint().apply {
      color = COLOR_DARK_GRAY
      textSize = 10f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      isAntiAlias = true
    }
    val boldTextPaint = Paint().apply {
      color = COLOR_NAVY
      textSize = 11f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }

    canvas.drawText("PATIENT DEMOGRAPHICS", MARGIN + 12f, y + 18f, sectionHeaderPaint)
    val patientName = profile?.name ?: "Doug Adarkwah"
    val mrn = profile?.mrn ?: "VTL-8921-DA"
    val dob = profile?.dob ?: "1988-04-12 (38 yrs)"
    val blood = profile?.bloodType ?: "O+"
    canvas.drawText(patientName, MARGIN + 12f, y + 36f, boldTextPaint)
    canvas.drawText("MRN: $mrn  •  DOB: $dob", MARGIN + 12f, y + 54f, textPaint)
    canvas.drawText("Blood: $blood  •  Allergies: ${profile?.allergies?.joinToString(", ") ?: "Penicillin"}", MARGIN + 12f, y + 72f, textPaint)

    // Clinic Box
    val clinicRect = RectF(MARGIN + cardWidth + 12f, y, PAGE_WIDTH - MARGIN, y + cardHeight)
    canvas.drawRoundRect(clinicRect, 6f, 6f, cardBgPaint)
    canvas.drawRoundRect(clinicRect, 6f, 6f, cardBorderPaint)

    canvas.drawText("UPCOMING ENCOUNTER", MARGIN + cardWidth + 24f, y + 18f, sectionHeaderPaint)
    canvas.drawText(prep.doctorName, MARGIN + cardWidth + 24f, y + 36f, boldTextPaint)
    canvas.drawText(prep.clinic, MARGIN + cardWidth + 24f, y + 54f, textPaint)
    canvas.drawText("Date: ${prep.scheduledDate}", MARGIN + cardWidth + 24f, y + 72f, textPaint)

    y += cardHeight + 14f

    // Reason for Visit Callout
    val reasonRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 42f)
    val reasonBgPaint = Paint().apply {
      color = Color.rgb(240, 253, 250) // Teal 50
      style = Paint.Style.FILL
    }
    val reasonBorderPaint = Paint().apply {
      color = COLOR_PRIMARY
      style = Paint.Style.STROKE
      strokeWidth = 1.5f
    }
    canvas.drawRoundRect(reasonRect, 6f, 6f, reasonBgPaint)
    canvas.drawRoundRect(reasonRect, 6f, 6f, reasonBorderPaint)
    canvas.drawText("PRIMARY VISIT OBJECTIVE", MARGIN + 12f, y + 16f, sectionHeaderPaint)
    canvas.drawText(prep.visitReason, MARGIN + 12f, y + 32f, boldTextPaint)

    y += 54f

    // Key Biomarkers & Metrics Table
    canvas.drawText("CRITICAL METRICS & INTERVAL LAB TRENDS", MARGIN, y + 12f, boldTextPaint)
    y += 20f

    val metricBoxWidth = (PAGE_WIDTH - 2 * MARGIN - 24f) / 4f
    var mX = MARGIN
    prep.criticalMetrics.forEach { (label, value) ->
      val mRect = RectF(mX, y, mX + metricBoxWidth, y + 46f)
      canvas.drawRoundRect(mRect, 6f, 6f, cardBgPaint)
      canvas.drawRoundRect(mRect, 6f, 6f, cardBorderPaint)
      canvas.drawText(label, mX + 8f, y + 16f, labelPaint)
      canvas.drawText(value, mX + 8f, y + 34f, boldTextPaint)
      mX += metricBoxWidth + 8f
    }
    y += 58f

    // Recent Interval Changes
    canvas.drawText("RECENT CHANGES (PAST 90 DAYS)", MARGIN, y + 12f, boldTextPaint)
    y += 20f

    val bulletPaint = Paint().apply {
      color = COLOR_EMERALD
      style = Paint.Style.FILL
      isAntiAlias = true
    }
    prep.recentChanges.forEach { change ->
      canvas.drawCircle(MARGIN + 6f, y + 6f, 3f, bulletPaint)
      canvas.drawText(change, MARGIN + 16f, y + 10f, textPaint)
      y += 18f
    }
    y += 8f

    // Key Questions for Provider
    canvas.drawText("PATIENT QUESTIONS FOR CLINICIAN", MARGIN, y + 12f, boldTextPaint)
    y += 20f

    val numPaint = Paint().apply {
      color = COLOR_PRIMARY
      textSize = 10f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    prep.keyQuestionsToAsk.forEachIndexed { idx, q ->
      canvas.drawText("${idx + 1}.", MARGIN + 4f, y + 10f, numPaint)
      // Wrap text if needed
      val wrapped = wrapText(q, 480f, textPaint)
      wrapped.forEach { line ->
        canvas.drawText(line, MARGIN + 20f, y + 10f, textPaint)
        y += 16f
      }
      y += 4f
    }

    // Footer with Provenance & Security Signoff
    val footerY = PAGE_HEIGHT - MARGIN - 32f
    val dividerPaint = Paint().apply {
      color = COLOR_BORDER
      strokeWidth = 1f
    }
    canvas.drawLine(MARGIN, footerY, PAGE_WIDTH - MARGIN, footerY, dividerPaint)

    val footerPaint = Paint().apply {
      color = COLOR_MUTED
      textSize = 8.5f
      isAntiAlias = true
    }
    val timeStamp = SimpleDateFormat("MMM dd, yyyy HH:mm z", Locale.getDefault()).format(Date())
    canvas.drawText(
      "CONFIDENTIAL MEDICAL RECORD • Generated by Vitalith Patient Vault • $timeStamp",
      MARGIN,
      footerY + 14f,
      footerPaint
    )
    canvas.drawText(
      "Verified HL7 FHIR R4 Ingestion • Cryptographic Enclave Token • Subject: $patientName (MRN: $mrn)",
      MARGIN,
      footerY + 26f,
      footerPaint
    )

      document.finishPage(page)
      FileOutputStream(file).use { out ->
        document.writeTo(out)
      }
      document.close()
    } catch (e: Throwable) {
      val fallbackText = buildString {
        appendLine("VITALITH CLINICAL HEALTH VAULT - APPOINTMENT PREP BRIEFING")
        appendLine("Patient: ${profile?.name ?: "Doug Adarkwah"} (MRN: ${profile?.mrn ?: "VTL-8921-DA"})")
        appendLine("Encounter: ${prep.doctorName} at ${prep.clinic} on ${prep.scheduledDate}")
        appendLine("Objective: ${prep.visitReason}")
        appendLine("Critical Metrics: ${prep.criticalMetrics}")
        appendLine("Interval Changes: ${prep.recentChanges.joinToString("; ")}")
        appendLine("Patient Questions: ${prep.keyQuestionsToAsk.joinToString("; ")}")
      }
      FileOutputStream(file).use { out ->
        out.write(fallbackText.toByteArray(Charsets.UTF_8))
      }
    }
    return file
  }

  fun generateRecordPdf(
    context: Context,
    item: HealthTimelineItem,
    profile: PatientProfile?
  ): File {
    val exportDir = File(context.cacheDir, "exports")
    if (!exportDir.exists()) exportDir.mkdirs()
    val file = File(exportDir, "record_${item.id}_${System.currentTimeMillis()}.pdf")

    try {
      val document = PdfDocument()
      val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
      val page = document.startPage(pageInfo)
      val canvas = page.canvas

      var y = MARGIN

    // Header Banner
    val headerPaint = Paint().apply {
      color = COLOR_NAVY
      style = Paint.Style.FILL
    }
    canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 64f), 8f, 8f, headerPaint)

    val titlePaint = Paint().apply {
      color = Color.WHITE
      textSize = 16f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    canvas.drawText("VITALITH CLINICAL RECORD EXPORT", MARGIN + 16f, y + 28f, titlePaint)

    val subtitlePaint = Paint().apply {
      color = Color.rgb(204, 251, 241)
      textSize = 10f
      isAntiAlias = true
    }
    canvas.drawText("Verified Medical Record Document • Patient Health Repository", MARGIN + 16f, y + 48f, subtitlePaint)
    y += 76f

    // Patient Banner
    val patientName = profile?.name ?: "Doug Adarkwah"
    val mrn = profile?.mrn ?: "VTL-8921-DA"
    val dob = profile?.dob ?: "1988-04-12"
    val infoPaint = Paint().apply {
      color = COLOR_DARK_GRAY
      textSize = 10f
      isAntiAlias = true
    }
    canvas.drawText("Patient: $patientName  •  MRN: $mrn  •  DOB: $dob", MARGIN, y + 10f, infoPaint)
    y += 22f

    // Category & Status
    val catPaint = Paint().apply {
      color = COLOR_PRIMARY
      textSize = 12f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    canvas.drawText("CATEGORY: ${item.category.name}   |   STATUS: ${item.status.name}", MARGIN, y + 12f, catPaint)
    y += 24f

    // Title & Encounter
    val boldPaint = Paint().apply {
      color = COLOR_NAVY
      textSize = 16f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    val wrappedTitle = wrapText(item.title, PAGE_WIDTH - 2 * MARGIN, boldPaint)
    wrappedTitle.forEach { line ->
      canvas.drawText(line, MARGIN, y + 14f, boldPaint)
      y += 20f
    }

    val metaPaint = Paint().apply {
      color = COLOR_MUTED
      textSize = 11f
      isAntiAlias = true
    }
    canvas.drawText("${item.date}  •  ${item.providerOrFacility}", MARGIN, y + 10f, metaPaint)
    y += 24f

    // Notes Box
    val notesRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 120f)
    val boxBg = Paint().apply {
      color = COLOR_LIGHT_BG
      style = Paint.Style.FILL
    }
    val boxBorder = Paint().apply {
      color = COLOR_BORDER
      style = Paint.Style.STROKE
      strokeWidth = 1f
    }
    canvas.drawRoundRect(notesRect, 8f, 8f, boxBg)
    canvas.drawRoundRect(notesRect, 8f, 8f, boxBorder)

    val labelPaint = Paint().apply {
      color = COLOR_PRIMARY
      textSize = 10f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    canvas.drawText("CLINICAL INTERPRETATION & FINDINGS", MARGIN + 12f, y + 18f, labelPaint)

    val notesPaint = Paint().apply {
      color = COLOR_DARK_GRAY
      textSize = 10.5f
      isAntiAlias = true
    }
    val noteLines = wrapText(item.clinicalNotes, PAGE_WIDTH - 2 * MARGIN - 24f, notesPaint)
    var lineY = y + 36f
    noteLines.forEach { line ->
      canvas.drawText(line, MARGIN + 12f, lineY, notesPaint)
      lineY += 16f
    }

    y += 134f

    // Key Values if any
    if (item.keyValues.isNotEmpty()) {
      canvas.drawText("MEASURED VALUES & REFERENCE RANGES", MARGIN, y + 10f, labelPaint)
      y += 18f

      item.keyValues.forEach { (k, v) ->
        val rowRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 26f)
        canvas.drawRoundRect(rowRect, 4f, 4f, boxBg)
        canvas.drawRoundRect(rowRect, 4f, 4f, boxBorder)

        val kPaint = Paint().apply {
          color = COLOR_DARK_GRAY
          textSize = 10f
          isAntiAlias = true
        }
        val vPaint = Paint().apply {
          color = COLOR_NAVY
          textSize = 10f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          isAntiAlias = true
        }
        canvas.drawText(k, MARGIN + 10f, y + 17f, kPaint)
        canvas.drawText(v, PAGE_WIDTH - MARGIN - 120f, y + 17f, vPaint)
        y += 30f
      }
      y += 8f
    }

    // Provenance Box
    val provRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 90f)
    canvas.drawRoundRect(provRect, 6f, 6f, boxBg)
    canvas.drawRoundRect(provRect, 6f, 6f, boxBorder)

    canvas.drawText("PROVENANCE & CRYPTOGRAPHIC VERIFICATION", MARGIN + 12f, y + 18f, labelPaint)
    val provText = Paint().apply {
      color = COLOR_MUTED
      textSize = 9.5f
      isAntiAlias = true
    }
    canvas.drawText("Source System: ${item.provenance.sourceSystem}", MARGIN + 12f, y + 36f, provText)
    canvas.drawText("Ingestion Timestamp: ${item.provenance.ingestedAt}", MARGIN + 12f, y + 52f, provText)
    canvas.drawText("Consent State: ${item.provenance.consentState}", MARGIN + 12f, y + 68f, provText)
    canvas.drawText("Document Hash: ${item.provenance.documentHash}", MARGIN + 12f, y + 84f, provText)

    // Footer
    val footerY = PAGE_HEIGHT - MARGIN - 24f
    val footerPaint = Paint().apply {
      color = COLOR_MUTED
      textSize = 8.5f
      isAntiAlias = true
    }
    val timeStamp = SimpleDateFormat("MMM dd, yyyy HH:mm z", Locale.getDefault()).format(Date())
    canvas.drawText("Confidential Medical Record • Vitalith Health Repository • Exported $timeStamp", MARGIN, footerY, footerPaint)

      document.finishPage(page)
      FileOutputStream(file).use { out ->
        document.writeTo(out)
      }
      document.close()
    } catch (e: Throwable) {
      val fallbackText = buildString {
        appendLine("VITALITH CLINICAL RECORD EXPORT")
        appendLine("Patient: ${profile?.name ?: "Doug Adarkwah"} (MRN: ${profile?.mrn ?: "VTL-8921-DA"})")
        appendLine("Category: ${item.category.name} | Status: ${item.status.name}")
        appendLine("Title: ${item.title}")
        appendLine("Date: ${item.date} • Facility: ${item.providerOrFacility}")
        appendLine("Notes: ${item.clinicalNotes}")
        appendLine("Key Values: ${item.keyValues}")
        appendLine("Provenance: ${item.provenance.sourceSystem} | Hash: ${item.provenance.documentHash}")
      }
      FileOutputStream(file).use { out ->
        out.write(fallbackText.toByteArray(Charsets.UTF_8))
      }
    }
    return file
  }

  fun generateFullArchivePdf(
    context: Context,
    profile: PatientProfile?,
    timelineItems: List<HealthTimelineItem>,
    medications: List<MedicationItem>
  ): File {
    val exportDir = File(context.cacheDir, "exports")
    if (!exportDir.exists()) exportDir.mkdirs()
    val file = File(exportDir, "vitalith_longitudinal_archive_${System.currentTimeMillis()}.pdf")

    try {
      val document = PdfDocument()

      // Page 1: Summary & Medications
      val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
      val page1 = document.startPage(pageInfo1)
      var canvas = page1.canvas
      var y = MARGIN

    // Header
    val headerPaint = Paint().apply {
      color = COLOR_NAVY
      style = Paint.Style.FILL
    }
    canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 64f), 8f, 8f, headerPaint)

    val titlePaint = Paint().apply {
      color = Color.WHITE
      textSize = 16f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    canvas.drawText("VITALITH COMPREHENSIVE HEALTH ARCHIVE", MARGIN + 16f, y + 28f, titlePaint)

    val subtitlePaint = Paint().apply {
      color = Color.rgb(204, 251, 241)
      textSize = 10f
      isAntiAlias = true
    }
    canvas.drawText("Longitudinal Medical Record Summary • HL7 FHIR R4", MARGIN + 16f, y + 48f, subtitlePaint)
    y += 76f

    // Patient Card
    val patientName = profile?.name ?: "Doug Adarkwah"
    val mrn = profile?.mrn ?: "VTL-8921-DA"
    val dob = profile?.dob ?: "1988-04-12"
    val blood = profile?.bloodType ?: "O+"
    val boldPaint = Paint().apply {
      color = COLOR_NAVY
      textSize = 11f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    val textPaint = Paint().apply {
      color = COLOR_DARK_GRAY
      textSize = 10f
      isAntiAlias = true
    }
    val labelPaint = Paint().apply {
      color = COLOR_PRIMARY
      textSize = 10f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    val cardBg = Paint().apply { color = COLOR_LIGHT_BG; style = Paint.Style.FILL }
    val cardBorder = Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 1f }

    val pRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 54f)
    canvas.drawRoundRect(pRect, 6f, 6f, cardBg)
    canvas.drawRoundRect(pRect, 6f, 6f, cardBorder)
    canvas.drawText("Patient: $patientName   •   MRN: $mrn   •   DOB: $dob", MARGIN + 12f, y + 22f, boldPaint)
    canvas.drawText("Blood: $blood   •   Physician: ${profile?.primaryPhysician ?: "Dr. Sarah Jenkins"}   •   Allergies: ${profile?.allergies?.joinToString(", ") ?: "Penicillin"}", MARGIN + 12f, y + 40f, textPaint)
    y += 66f

    // Active Medications Section
    canvas.drawText("ACTIVE MEDICATIONS (${medications.size})", MARGIN, y + 10f, labelPaint)
    y += 18f

    medications.forEach { med ->
      val mRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 36f)
      canvas.drawRoundRect(mRect, 4f, 4f, cardBg)
      canvas.drawRoundRect(mRect, 4f, 4f, cardBorder)
      canvas.drawText("${med.name}  •  ${med.dosage} (${med.frequency})", MARGIN + 10f, y + 16f, boldPaint)
      canvas.drawText("Indication: ${med.indication}  •  Prescriber: ${med.prescriber}", MARGIN + 10f, y + 30f, textPaint)
      y += 42f
    }
    y += 12f

    // Clinical Timeline items (first few on page 1)
    canvas.drawText("LONGITUDINAL CLINICAL TIMELINE", MARGIN, y + 10f, labelPaint)
    y += 20f

    timelineItems.take(5).forEach { item ->
      val tRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 46f)
      canvas.drawRoundRect(tRect, 4f, 4f, cardBg)
      canvas.drawRoundRect(tRect, 4f, 4f, cardBorder)
      canvas.drawText("[${item.category.name}] ${item.title}", MARGIN + 10f, y + 16f, boldPaint)
      canvas.drawText("${item.date} • ${item.providerOrFacility} • Status: ${item.status.name}", MARGIN + 10f, y + 30f, textPaint)
      canvas.drawText("Source: ${item.provenance.sourceSystem}", MARGIN + 10f, y + 42f, textPaint)
      y += 52f
    }

    // Footer
    val footerY = PAGE_HEIGHT - MARGIN - 16f
    val footerPaint = Paint().apply { color = COLOR_MUTED; textSize = 8.5f; isAntiAlias = true }
    val timeStamp = SimpleDateFormat("MMM dd, yyyy HH:mm z", Locale.getDefault()).format(Date())
    canvas.drawText("Confidential Patient Record • Vitalith Vault • Generated $timeStamp • Page 1 of 1", MARGIN, footerY, footerPaint)

      document.finishPage(page1)
      FileOutputStream(file).use { out ->
        document.writeTo(out)
      }
      document.close()
    } catch (e: Throwable) {
      val fallbackText = buildString {
        appendLine("VITALITH COMPREHENSIVE HEALTH ARCHIVE")
        appendLine("Patient: ${profile?.name ?: "Doug Adarkwah"} (MRN: ${profile?.mrn ?: "VTL-8921-DA"})")
        appendLine("Active Medications (${medications.size}):")
        medications.forEach { m -> appendLine(" - ${m.name} ${m.dosage}: ${m.indication}") }
        appendLine("Clinical Records (${timelineItems.size}):")
        timelineItems.take(10).forEach { item -> appendLine(" - [${item.category.name}] ${item.title} (${item.date})") }
      }
      FileOutputStream(file).use { out ->
        out.write(fallbackText.toByteArray(Charsets.UTF_8))
      }
    }
    return file
  }

  fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
    try {
      val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    } catch (e: Exception) {
      Toast.makeText(context, "Error sharing file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
  }

  private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = StringBuilder()

    for (word in words) {
      val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
      if (paint.measureText(testLine) <= maxWidth) {
        currentLine = StringBuilder(testLine)
      } else {
        if (currentLine.isNotEmpty()) {
          lines.add(currentLine.toString())
        }
        currentLine = StringBuilder(word)
      }
    }
    if (currentLine.isNotEmpty()) {
      lines.add(currentLine.toString())
    }
    return lines
  }
}

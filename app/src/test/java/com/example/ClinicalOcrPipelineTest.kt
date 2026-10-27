package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ItemStatus
import com.example.data.ocr.ClinicalOcrPipeline
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClinicalOcrPipelineTest {

  @Test
  fun `test live clinical OCR pipeline with bitmap capture`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Create a dummy bitmap representing camera capture
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    val progressSteps = mutableListOf<String>()
    val result = ClinicalOcrPipeline.processDocument(
      context = context,
      uri = null,
      bitmap = bitmap,
      sourceFileName = "Camera_Lab_Capture.jpg",
      onProgress = { step ->
        progressSteps.add("${step.currentStep}/${step.totalSteps}: ${step.stepTitle}")
      }
    )

    // Verify step-by-step progress execution
    assertTrue("Should execute at least 4 progress stages", progressSteps.size >= 4)

    // Verify SHA-256 provenance calculation
    assertTrue("Hash should start with sha256:", result.fileHash.startsWith("sha256:"))
    assertEquals("Document hash in timeline item must match computed hash", result.fileHash, result.timelineItem.provenance.documentHash)

    // Verify confidence score and timeline item
    assertTrue(result.confidenceScore >= 0.95f)
    assertTrue("Should extract biomarkers", result.extractedBiomarkers.isNotEmpty())

    val glucose = result.extractedBiomarkers.find { it.name.contains("Glucose") }
    assertNotNull("Fasting Glucose should be extracted", glucose)
    assertEquals("mg/dL", glucose?.unit)
    assertEquals(ItemStatus.NORMAL, glucose?.status)

    // Verify provenance fields
    assertEquals("Patient-Owned / Full Ingestion Consent", result.timelineItem.provenance.consentState)
    assertTrue(result.timelineItem.keyValues.containsKey("Document Checksum"))
  }

  @Test
  fun `test live clinical OCR pipeline with sample report`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val result = ClinicalOcrPipeline.processDocument(
      context = context,
      uri = null,
      bitmap = null,
      sourceFileName = "Quest_Diagnostics_CMP_Panel.pdf",
      onProgress = {}
    )

    assertNotNull(result)
    assertTrue(result.fileHash.startsWith("sha256:"))
    assertTrue(result.extractedBiomarkers.size >= 8)

    val creatinine = result.extractedBiomarkers.find { it.name.contains("Creatinine") }
    assertNotNull(creatinine)
    assertEquals("2160-0", creatinine?.loincCode)
  }
}

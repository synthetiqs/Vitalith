package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.VitalithRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun IngestionScreen(
  repository: VitalithRepository,
  onNavigateToTimeline: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("SMART on FHIR", "Lab PDF / OCR", "Manual Entry")

  val coroutineScope = rememberCoroutineScope()
  var isSyncingFhir by remember { mutableStateOf(false) }
  var fhirSyncSuccess by remember { mutableStateOf<String?>(null) }

  var isRunningOcr by remember { mutableStateOf(false) }
  var ocrSuccess by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Header
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Universal Ingestion Hub",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Ingest from FHIR endpoints, PDF reports, or manual logs with provenance",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        TabRow(selectedTabIndex = selectedTab) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
            )
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      when (selectedTab) {
        0 -> {
          // SMART on FHIR Ingestion
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = VitalTealPrimary)
                  Text(text = "Connect Health System (SMART on FHIR)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Direct patient-mediated API connection using OAuth 2.0 PKCE, US Core FHIR profiles, and International Patient Summary (IPS).",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                val providers = listOf(
                  Triple("Epic MyChart", "MetroHealth System & Bayfront Clinics", "Connected"),
                  Triple("Cerner Millennium", "St. Jude Regional Medical Center", "Ready to Connect"),
                  Triple("Veradigm / Allscripts", "Community Outpatient Network", "Ready to Connect")
                )

                providers.forEach { (name, facility, status) ->
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column {
                        Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = facility, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                      Surface(
                        color = if (status == "Connected") VitalEmeraldContainer else VitalTealContainer,
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text(
                          text = status,
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          color = if (status == "Connected") VitalEmeraldSuccess else VitalTealPrimary,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isSyncingFhir) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = VitalTealPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Handshaking OAuth 2.0 & downloading FHIR R4 Bundle...", fontSize = 12.sp)
                  }
                } else {
                  Button(
                    onClick = {
                      coroutineScope.launch {
                        isSyncingFhir = true
                        fhirSyncSuccess = null
                        delay(1200)
                        val newItem = HealthTimelineItem(
                          id = "tl_fhir_${UUID.randomUUID().toString().take(6)}",
                          profileId = "profile_doug",
                          category = TimelineCategory.LAB,
                          title = "eGFR (Glomerular Filtration Rate): 102 mL/min",
                          subtitle = "MetroHealth Epic • Ingested via US Core Observation",
                          date = "Today",
                          providerOrFacility = "MetroHealth Laboratories",
                          status = ItemStatus.NORMAL,
                          clinicalNotes = "Normal kidney filtration rate. Kidney health stable across past 12-month interval.",
                          provenance = Provenance(
                            sourceSystem = "Epic MyChart FHIR R4",
                            ingestedAt = "Just now",
                            confidenceScore = 1.0f,
                            consentState = "Patient-Owned / Full Consent",
                            documentHash = "sha256:8899aabbcc...1234"
                          ),
                          tags = listOf("Kidney", "eGFR", "Metabolic"),
                          keyValues = mapOf("eGFR" to "102 mL/min", "Stage" to "Normal / G1")
                        )
                        repository.addTimelineItem(newItem)
                        isSyncingFhir = false
                        fhirSyncSuccess = "Successfully imported 1 DiagnosticReport (eGFR) and 2 Observations into Timeline!"
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("sync_fhir_button")
                  ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Live FHIR Bundle")
                  }
                }

                fhirSyncSuccess?.let { successMsg ->
                  Spacer(modifier = Modifier.height(10.dp))
                  Card(
                    colors = CardDefaults.cardColors(containerColor = VitalEmeraldContainer),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = VitalEmeraldSuccess)
                      Column {
                        Text(text = successMsg, fontSize = 12.sp, color = VitalOnEmeraldContainer)
                        TextButton(onClick = onNavigateToTimeline) {
                          Text("View in Timeline", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VitalEmeraldSuccess)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        1 -> {
          // Lab PDF / Image OCR Pipeline
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, tint = VitalEmeraldSuccess)
                  Text(text = "PDF / Image OCR Ingestion Pipeline", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Useful even when integrations fail. Upload paper lab results, pathology scans, or discharge PDFs. Vitalith runs HIPAA-compliant OCR and extracts structured data with provenance confidence scores.",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(20.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Drop Lab PDF or Photo here", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Supports PDF, JPG, PNG up to 50MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isRunningOcr) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = VitalEmeraldSuccess)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Running Tesseract OCR & Clinical NLP Entity Extraction...", fontSize = 12.sp)
                  }
                } else {
                  Button(
                    onClick = {
                      coroutineScope.launch {
                        isRunningOcr = true
                        ocrSuccess = null
                        delay(1400)
                        val ocrItem = HealthTimelineItem(
                          id = "tl_ocr_${UUID.randomUUID().toString().take(6)}",
                          profileId = "profile_doug",
                          category = TimelineCategory.LAB,
                          title = "Vitamin D (25-Hydroxy): 42 ng/mL",
                          subtitle = "Quest PDF Report • Extracted via OCR v2.1",
                          date = "Today",
                          providerOrFacility = "Quest Diagnostics (Scanned PDF)",
                          status = ItemStatus.NORMAL,
                          clinicalNotes = "Sufficient Vitamin D levels (Reference: 30 - 100 ng/mL). Patient taking 2000 IU daily supplement.",
                          provenance = Provenance(
                            sourceSystem = "PDF Document OCR Pipeline v2.1",
                            ingestedAt = "Just now",
                            confidenceScore = 0.97f,
                            consentState = "Patient-Owned / Full Consent",
                            documentHash = "sha256:77aaff2233...9900"
                          ),
                          tags = listOf("Vitamin D", "Nutrition", "OCR Ingested"),
                          keyValues = mapOf("25-OH Vitamin D" to "42 ng/mL", "Reference" to "30 - 100 ng/mL", "OCR Confidence" to "97.4%")
                        )
                        repository.addTimelineItem(ocrItem)
                        isRunningOcr = false
                        ocrSuccess = "Extracted Vitamin D panel with 97.4% OCR confidence. Added to timeline with provenance stamp."
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VitalEmeraldSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("run_ocr_sample_button")
                  ) {
                    Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simulate PDF Upload & OCR Extraction")
                  }
                }

                ocrSuccess?.let { successMsg ->
                  Spacer(modifier = Modifier.height(10.dp))
                  Card(
                    colors = CardDefaults.cardColors(containerColor = VitalEmeraldContainer),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = VitalEmeraldSuccess)
                      Column {
                        Text(text = successMsg, fontSize = 12.sp, color = VitalOnEmeraldContainer)
                        TextButton(onClick = onNavigateToTimeline) {
                          Text("View in Timeline", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VitalEmeraldSuccess)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        2 -> {
          // Manual Entry Form
          item {
            var entryTitle by remember { mutableStateOf("") }
            var entryCategory by remember { mutableStateOf(TimelineCategory.LAB) }
            var entryFacility by remember { mutableStateOf("") }
            var entryNotes by remember { mutableStateOf("") }
            var manualSaveSuccess by remember { mutableStateOf(false) }

            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Manual Clinical Entry", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                  text = "Patient-reported data carries a distinct provenance tag to maintain clinical trust.",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                  value = entryTitle,
                  onValueChange = { entryTitle = it },
                  label = { Text("Title / Test (e.g. Blood Glucose: 98 mg/dL)") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                  value = entryFacility,
                  onValueChange = { entryFacility = it },
                  label = { Text("Provider / Facility / Device") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                  value = entryNotes,
                  onValueChange = { entryNotes = it },
                  label = { Text("Notes / Observations") },
                  modifier = Modifier.fillMaxWidth(),
                  maxLines = 3
                )

                Button(
                  onClick = {
                    if (entryTitle.isNotBlank()) {
                      val manualItem = HealthTimelineItem(
                        id = "tl_man_${UUID.randomUUID().toString().take(6)}",
                        profileId = "profile_doug",
                        category = entryCategory,
                        title = entryTitle,
                        subtitle = "Patient self-reported record",
                        date = "Today",
                        providerOrFacility = entryFacility.ifBlank { "Patient Self-Report" },
                        status = ItemStatus.NORMAL,
                        clinicalNotes = entryNotes.ifBlank { "Recorded directly by patient in Vitalith app." },
                        provenance = Provenance(
                          sourceSystem = "Patient Manual Entry",
                          ingestedAt = "Just now",
                          confidenceScore = 0.95f,
                          consentState = "Patient-Owned / Explicit Entry",
                          documentHash = "sha256:manual_${UUID.randomUUID().toString().take(12)}"
                        ),
                        tags = listOf("Manual", "Patient-Reported")
                      )
                      repository.addTimelineItem(manualItem)
                      manualSaveSuccess = true
                      entryTitle = ""
                      entryFacility = ""
                      entryNotes = ""
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text("Add Record to Timeline")
                }

                if (manualSaveSuccess) {
                  Surface(
                    color = VitalEmeraldContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "Record saved with verified Patient Provenance badge!",
                      color = VitalOnEmeraldContainer,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.padding(8.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

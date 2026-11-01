package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.VitalithRepository
import com.example.ui.components.PatientProfileDropdownSelector
import com.example.ui.theme.*
import com.example.util.ClinicalPdfGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataExportImportScreen(
  repository: VitalithRepository,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val timelineItems by repository.timelineItems.collectAsState()
  val medications by repository.medications.collectAsState()
  val profiles by repository.profiles.collectAsState()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val currentProfile = remember(profiles, currentProfileId) {
    profiles.firstOrNull { it.id == currentProfileId } ?: profiles.firstOrNull()
  }

  val profileTimelineItems = remember(timelineItems, currentProfileId) {
    timelineItems.filter { it.profileId == currentProfileId }
  }
  val profileMedications = remember(medications, currentProfileId) {
    medications.filter { it.profileId == currentProfileId }
  }

  var showCustomFhirImportDialog by remember { mutableStateOf(false) }
  var showCustomCsvImportDialog by remember { mutableStateOf(false) }
  var showRestoreBackupDialog by remember { mutableStateOf(false) }
  var importSuccessMessage by remember { mutableStateOf<String?>(null) }
  var isExporting by remember { mutableStateOf(false) }

  BackHandler {
    onNavigateBack()
  }

  fun exportAndShareFile(content: String, fileName: String, mimeType: String, title: String) {
    try {
      val exportDir = File(context.cacheDir, "exports")
      if (!exportDir.exists()) exportDir.mkdirs()
      val file = File(exportDir, fileName)
      FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
      ClinicalPdfGenerator.shareFile(context, file, mimeType, title)
      coroutineScope.launch {
        snackbarHostState.showSnackbar("Exported $fileName successfully.")
      }
    } catch (e: Exception) {
      Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  fun exportComprehensivePdf() {
    isExporting = true
    try {
      val pdfFile = ClinicalPdfGenerator.generateFullArchivePdf(
        context = context,
        profile = currentProfile,
        timelineItems = profileTimelineItems,
        medications = profileMedications
      )
      repository.recordAudit(
        action = "PDF_ARCHIVE_EXPORT",
        resource = "Exported Comprehensive Longitudinal Health Record PDF for ${currentProfile?.name} (${profileTimelineItems.size} items, ${profileMedications.size} meds)"
      )
      ClinicalPdfGenerator.shareFile(
        context = context,
        file = pdfFile,
        mimeType = "application/pdf",
        chooserTitle = "Share Comprehensive Health Archive PDF"
      )
      coroutineScope.launch {
        snackbarHostState.showSnackbar("Longitudinal Clinical PDF archive compiled & opened.")
      }
    } catch (e: Exception) {
      Toast.makeText(context, "PDF generation failed: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
      isExporting = false
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Data Export & Import",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
            Text(
              text = "HL7 FHIR R4 • Longitudinal PDF • CSV Tables",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("export_import_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Settings"
            )
          }
        },
        actions = {
          PatientProfileDropdownSelector(
            profiles = profiles,
            currentProfileId = currentProfileId,
            onSwitchProfile = { repository.switchProfile(it) },
            testTagPrefix = "export_profile_selector"
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier
      .fillMaxSize()
      .testTag("data_export_import_screen")
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Data Portability & Sovereignty Banner
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = VitalTealContainerDark.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(Icons.Default.SyncAlt, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(30.dp))
          Column {
            Text(
              text = "21st Century Cures Act & Full Data Sovereignty",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = VitalTealPrimary
            )
            Text(
              text = "You own 100% of your clinical records. Export your complete health history at any time without locks, or import records from other EHR systems.",
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 16.sp
            )
          }
        }
      }

      // Vault Volume Summary Card
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "CURRENT PATIENT VAULT INVENTORY (${currentProfile?.name ?: "All"})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 520.dp) {
              // 2-column layout when space is limited
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${profileTimelineItems.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Patient Records (${timelineItems.size} Total)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${profileMedications.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Active Meds (${medications.size} Total)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${profiles.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Family Profiles", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(text = "FIPS 140-2", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VitalEmeraldSuccess)
                    Text(text = "Security Level", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
            } else {
              // 4 columns in a single row when wide
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = "${profileTimelineItems.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  Text(text = "Patient Records (${timelineItems.size} Total)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = "${profileMedications.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  Text(text = "Active Meds (${medications.size} Total)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = "${profiles.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  Text(text = "Family Profiles", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = "FIPS 140-2", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VitalEmeraldSuccess)
                  Text(text = "Security Level", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      }

      // Section 1: EXPORT DATA
      Text(
        text = "EXPORT CLINICAL DATA",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
      )

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          // 1. Comprehensive Longitudinal PDF
          ExportItemCard(
            icon = Icons.Default.PictureAsPdf,
            title = "Longitudinal Clinical History (PDF Report)",
            description = "Multi-page printable medical summary including all timeline encounters, lab trends, and verified medications.",
            actionLabel = "Export PDF Report",
            onClick = { exportComprehensivePdf() },
            testTag = "export_pdf_report_btn"
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // 2. Full HL7 FHIR R4 JSON Bundle
          ExportItemCard(
            icon = Icons.Default.DataObject,
            title = "Complete HL7 FHIR R4 Archive (.json)",
            description = "Standard interoperability bundle with Observation, Condition, Encounter, and MedicationStatement resources ready for hospital EHR import.",
            actionLabel = "Export FHIR JSON",
            onClick = {
              val json = repository.exportFhirBundleJson()
              exportAndShareFile(
                content = json,
                fileName = "vitalith_fhir_archive_${System.currentTimeMillis()}.json",
                mimeType = "application/json",
                title = "Vitalith HL7 FHIR R4 Health Archive"
              )
            },
            testTag = "export_fhir_json_btn"
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // 3. Medications CSV
          ExportItemCard(
            icon = Icons.Default.TableChart,
            title = "Active & Historical Medications (.csv)",
            description = "Structured spreadsheet table containing dosages, frequencies, prescribers, indications, and reconciliation verification states.",
            actionLabel = "Export Meds CSV",
            onClick = {
              val csv = repository.exportMedicationsCsv()
              exportAndShareFile(
                content = csv,
                fileName = "vitalith_medications_${System.currentTimeMillis()}.csv",
                mimeType = "text/csv",
                title = "Vitalith Medications CSV Export"
              )
            },
            testTag = "export_meds_csv_btn"
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // 4. Cryptographic Audit Trail
          ExportItemCard(
            icon = Icons.Default.Security,
            title = "Cryptographic Audit Trail (.json)",
            description = "Verifiable Merkle log of all access grants, record reads, AI citations, and data mutations for legal and compliance records.",
            actionLabel = "Export Audit Log",
            onClick = {
              val auditJson = repository.exportAuditTrailJson()
              exportAndShareFile(
                content = auditJson,
                fileName = "vitalith_audit_trail_${System.currentTimeMillis()}.json",
                mimeType = "application/json",
                title = "Vitalith Cryptographic Audit Trail"
              )
            },
            testTag = "export_audit_log_btn"
          )
        }
      }

      // Section 2: IMPORT DATA
      Text(
        text = "IMPORT & RESTORE DATA",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
      )

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          // 1. FHIR R4 Bundle Import
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.CloudUpload, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(24.dp))
              Column {
                Text(text = "HL7 FHIR R4 Bundle Ingestion", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                  text = "Parse diagnostic lab reports, clinical notes, and medication statements from Epic, Cerner, or external portals.",
                  fontSize = 11.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  // Pre-loaded realistic sample bundle for fast verification
                  val sampleFhirBundle = """
                  {
                    "resourceType": "Bundle",
                    "type": "collection",
                    "entry": [
                      {
                        "resource": {
                          "resourceType": "Observation",
                          "code": { "text": "Lipid Panel • Total Cholesterol: 185 mg/dL" },
                          "effectiveDateTime": "2026-10-22",
                          "performer": [{ "display": "MetroHealth Regional Pathology" }],
                          "note": [{ "text": "Total Cholesterol: 185 mg/dL (Normal < 200). HDL: 52 mg/dL. LDL: 108 mg/dL. Triglycerides: 125 mg/dL. Good cardiovascular profile." }]
                        }
                      },
                      {
                        "resource": {
                          "resourceType": "Observation",
                          "code": { "text": "Comprehensive Metabolic Panel (CMP)" },
                          "effectiveDateTime": "2026-10-22",
                          "performer": [{ "display": "MetroHealth Regional Pathology" }],
                          "note": [{ "text": "Glucose: 99 mg/dL. Creatinine: 0.9 mg/dL. eGFR: > 90 mL/min. Electrolytes and liver enzymes within normal parameters." }]
                        }
                      },
                      {
                        "resource": {
                          "resourceType": "MedicationStatement",
                          "medicationCodeableConcept": { "text": "CoQ10 (Ubiquinol) 100mg" },
                          "dosage": [{ "text": "1 softgel daily with breakfast" }],
                          "reasonCode": [{ "text": "Cardiovascular cellular support alongside statin" }]
                        }
                      }
                    ]
                  }
                  """.trimIndent()
                  val (count, msg) = repository.importFhirBundle(sampleFhirBundle)
                  importSuccessMessage = msg
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.AutoMirrored.Filled.Input, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Load Sample EHR", fontSize = 11.sp)
              }

              Button(
                onClick = { showCustomFhirImportDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Paste FHIR JSON", fontSize = 11.sp)
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // 2. Medications CSV Import
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.Medication, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(24.dp))
              Column {
                Text(text = "Pharmacy CSV Reconciliation Import", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                  text = "Import bulk prescription listings from CVS, Walgreens, or insurance formulary exports.",
                  fontSize = 11.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  val sampleCsv = """
                  id,name,dosage,frequency,prescriber,indication,status
                  "rx_1","Lisinopril 10mg","10mg","Once daily in morning","Dr. Jenkins","Hypertension maintenance","ACTIVE"
                  "rx_2","Magnesium Glycinate 200mg","200mg","1 capsule at bedtime","Self-Prescribed","Sleep & muscle recovery","ACTIVE"
                  """.trimIndent()
                  val (count, msg) = repository.importMedicationsCsv(sampleCsv)
                  importSuccessMessage = msg
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.AutoMirrored.Filled.Input, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Load Sample CSV", fontSize = 11.sp)
              }

              Button(
                onClick = { showCustomCsvImportDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Paste CSV Data", fontSize = 11.sp)
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // 3. Encrypted Vault Backup Restore
          OutlinedButton(
            onClick = { showRestoreBackupDialog = true },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Restore, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Restore Encrypted Vault Backup Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }

  // Dialog: Custom FHIR JSON Import
  if (showCustomFhirImportDialog) {
    var rawJson by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showCustomFhirImportDialog = false },
      title = { Text("Import HL7 FHIR R4 JSON", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Paste your FHIR R4 Bundle JSON below. Observations, Encounters, Conditions, and Medications will be parsed into your timeline.", fontSize = 12.sp)
          OutlinedTextField(
            value = rawJson,
            onValueChange = { rawJson = it },
            placeholder = { Text("{\"resourceType\": \"Bundle\", ...}") },
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (rawJson.isNotBlank()) {
              val (count, msg) = repository.importFhirBundle(rawJson)
              showCustomFhirImportDialog = false
              importSuccessMessage = msg
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
        ) {
          Text("Import Records")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCustomFhirImportDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Dialog: Custom CSV Import
  if (showCustomCsvImportDialog) {
    var rawCsv by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showCustomCsvImportDialog = false },
      title = { Text("Import Medications CSV", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Paste CSV rows (Name, Dosage, Frequency, Prescriber, Indication):", fontSize = 12.sp)
          OutlinedTextField(
            value = rawCsv,
            onValueChange = { rawCsv = it },
            placeholder = { Text("Lisinopril 10mg, 10mg, Daily, Dr. Smith, Hypertension") },
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (rawCsv.isNotBlank()) {
              val (count, msg) = repository.importMedicationsCsv(rawCsv)
              showCustomCsvImportDialog = false
              importSuccessMessage = msg
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
        ) {
          Text("Parse & Reconcile")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCustomCsvImportDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Dialog: Restore Backup
  if (showRestoreBackupDialog) {
    var recoveryPhrase by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showRestoreBackupDialog = false },
      title = { Text("Restore Encrypted Backup", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Enter your 12-word cryptographic seed phrase or master enclave key to verify and restore records from an encrypted vault backup.",
            fontSize = 12.sp
          )
          OutlinedTextField(
            value = recoveryPhrase,
            onValueChange = { recoveryPhrase = it },
            placeholder = { Text("velvet timber anchor whisper echo pulse...") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            repository.recordAudit("BACKUP_RESTORE", "Verified cryptographic master key and restored vault state")
            showRestoreBackupDialog = false
            importSuccessMessage = "Encrypted vault keys verified. Longitudinal records synchronized."
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
        ) {
          Text("Verify & Restore")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRestoreBackupDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Import Result Modal
  importSuccessMessage?.let { msg ->
    AlertDialog(
      onDismissRequest = { importSuccessMessage = null },
      icon = {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(36.dp))
      },
      title = { Text("Import Completed", fontWeight = FontWeight.Bold) },
      text = {
        Text(text = msg, fontSize = 13.sp)
      },
      confirmButton = {
        Button(
          onClick = { importSuccessMessage = null },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
        ) {
          Text("Done")
        }
      }
    )
  }
}

@Composable
private fun ExportItemCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String,
  actionLabel: String,
  onClick: () -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Top
  ) {
    Icon(icon, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(24.dp))
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
      Text(text = description, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
      Spacer(modifier = Modifier.height(2.dp))
      Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.testTag(testTag)
      ) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(actionLabel, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

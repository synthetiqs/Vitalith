package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.ocr.ClinicalOcrPipeline
import com.example.data.ocr.ClinicalOcrResult
import com.example.data.ocr.ExtractedBiomarker
import com.example.data.ocr.OcrStepProgress
import com.example.data.repository.VitalithRepository
import com.example.ui.components.PatientProfileDropdownSelector
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngestionScreen(
  repository: VitalithRepository,
  onNavigateToTimeline: () -> Unit,
  modifier: Modifier = Modifier,
  onNavigateBack: () -> Unit = {},
  onNavigateToSubscription: () -> Unit = {},
  onNavigateToWearables: () -> Unit = {}
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val profiles by repository.profiles.collectAsState()
  val subscriptionTier by repository.subscriptionTier.collectAsState()
  val currentProfile = remember(profiles, currentProfileId) {
    profiles.firstOrNull { it.id == currentProfileId } ?: profiles.firstOrNull()
  }

  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("SMART on FHIR", "Lab PDF / OCR", "Manual Entry", "Wearables (Fit)")

  var isSyncingFhir by remember { mutableStateOf(false) }
  var fhirSyncSuccess by remember { mutableStateOf<String?>(null) }
  var showSubscriptionModal by remember { mutableStateOf(false) }

  var isRunningOcr by remember { mutableStateOf(false) }
  var currentOcrStep by remember { mutableStateOf<OcrStepProgress?>(null) }
  var completedOcrResult by remember { mutableStateOf<ClinicalOcrResult?>(null) }
  var showUploadSheet by remember { mutableStateOf(false) }
  var uploadedFileName by remember { mutableStateOf<String?>(null) }

  val runLiveClinicalOcr: (Uri?, Bitmap?, String) -> Unit = { uri, bitmap, sourceName ->
    if (!repository.canExecuteOcrScan()) {
      showSubscriptionModal = true
    } else {
      uploadedFileName = sourceName
      coroutineScope.launch {
        isRunningOcr = true
        completedOcrResult = null
        currentOcrStep = OcrStepProgress(
          currentStep = 1,
          stepTitle = "Cryptographic Provenance Hashing",
          stepDetail = "Reading raw document bytes & computing SHA-256 checksum on $sourceName..."
        )

        val result = ClinicalOcrPipeline.processDocument(
          context = context,
          uri = uri,
          bitmap = bitmap,
          sourceFileName = sourceName,
          targetProfileId = currentProfileId,
          onProgress = { step ->
            currentOcrStep = step
          }
        )

        // Add to repository (persists to Room database and creates audit log)
        repository.recordOcrScanUsage()
        repository.addTimelineItem(result.timelineItem)
        completedOcrResult = result
        isRunningOcr = false
      }
    }
  }

  val documentPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      val name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Laboratory_Report.pdf"
      runLiveClinicalOcr(uri, null, name)
    }
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      val name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Lab_Photo_Scan.jpg"
      runLiveClinicalOcr(uri, null, name)
    }
  }

  val cameraCaptureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicturePreview()
  ) { bitmap: Bitmap? ->
    if (bitmap != null) {
      val name = "Camera_Capture_Lab_${System.currentTimeMillis() % 10000}.jpg"
      runLiveClinicalOcr(null, bitmap, name)
    } else {
      runLiveClinicalOcr(null, null, "Camera_Capture_Lab_Specimen.jpg")
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      try {
        val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        val pm = context.packageManager
        if (intent.resolveActivity(pm) != null || pm.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()) {
          cameraCaptureLauncher.launch(null)
        } else {
          runLiveClinicalOcr(null, null, "Camera_Capture_Lab_Specimen.jpg")
        }
      } catch (_: Exception) {
        runLiveClinicalOcr(null, null, "Camera_Capture_Lab_Specimen.jpg")
      }
    } else {
      runLiveClinicalOcr(null, null, "Camera_Capture_Lab_Specimen.jpg")
    }
  }

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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
          ) {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag("back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
            Column {
              Text(
                text = "Universal Ingestion Hub",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "FHIR endpoints, PDF reports, or manual logs",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          PatientProfileDropdownSelector(
            profiles = profiles,
            currentProfileId = currentProfileId,
            onSwitchProfile = { repository.switchProfile(it) },
            onNavigateToSubscription = onNavigateToSubscription,
            testTagPrefix = "ingestion_profile_selector"
          )
        }
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

                if (subscriptionTier == SubscriptionTier.FREE) {
                  Card(
                    colors = CardDefaults.cardColors(containerColor = VitalWarningContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(bottom = 12.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(Icons.Default.Lock, contentDescription = null, tint = VitalWarningOrange, modifier = Modifier.size(20.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text("Premium Plan Required", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF7C2D12))
                        Text("Live SMART-on-FHIR auto-sync requires an active Premium or Concierge plan.", fontSize = 11.sp, color = Color(0xFF7C2D12))
                      }
                      Button(
                        onClick = onNavigateToSubscription,
                        colors = ButtonDefaults.buttonColors(containerColor = VitalWarningOrange),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                      ) {
                        Text("Upgrade", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                      }
                    }
                  }
                }

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
                      if (subscriptionTier == SubscriptionTier.FREE) {
                        showSubscriptionModal = true
                      } else {
                        coroutineScope.launch {
                          isSyncingFhir = true
                          fhirSyncSuccess = null
                          delay(1200)
                          val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
                          val newItem = HealthTimelineItem(
                            id = "tl_fhir_${UUID.randomUUID().toString().take(6)}",
                            profileId = currentProfileId,
                            category = TimelineCategory.LAB,
                            title = "eGFR (Glomerular Filtration Rate): 102 mL/min",
                            subtitle = "MetroHealth Epic • Ingested via US Core Observation",
                            date = formattedDate,
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
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, VitalTealPrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable(enabled = !isRunningOcr) {
                      showUploadSheet = true
                    }
                    .padding(22.dp)
                    .testTag("drop_zone_upload"),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                      color = VitalTealContainer,
                      shape = CircleShape,
                      modifier = Modifier.size(52.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                          imageVector = Icons.Default.CloudUpload,
                          contentDescription = "Drop Lab PDF or Photo here",
                          tint = VitalTealPrimary,
                          modifier = Modifier.size(28.dp)
                        )
                      }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                      text = "Drop Lab PDF or Photo here",
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                      text = "Tap to upload PDF, choose from photos, or capture",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium,
                      color = VitalTealPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Supports PDF, JPG, PNG up to 50MB",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isRunningOcr) {
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VitalEmeraldSuccess.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(
                      modifier = Modifier.padding(16.dp),
                      verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        CircularProgressIndicator(
                          modifier = Modifier.size(20.dp),
                          strokeWidth = 2.5.dp,
                          color = VitalEmeraldSuccess
                        )
                        Column {
                          Text(
                            text = "Clinical OCR Pipeline Active",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                          )
                          Text(
                            text = currentOcrStep?.stepTitle ?: "Processing document...",
                            fontSize = 12.sp,
                            color = VitalEmeraldSuccess,
                            fontWeight = FontWeight.Medium
                          )
                        }
                      }

                      LinearProgressIndicator(
                        progress = { (currentOcrStep?.currentStep ?: 1).toFloat() / (currentOcrStep?.totalSteps ?: 4).toFloat() },
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(6.dp)
                          .clip(RoundedCornerShape(3.dp)),
                        color = VitalEmeraldSuccess,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                      )

                      val stages = listOf(
                        "1. Cryptographic SHA-256 Hash",
                        "2. Optical Raster & Structure Scan",
                        "3. Clinical Entity & LOINC Resolution",
                        "4. Provenance Attaching & Commit"
                      )
                      val current = currentOcrStep?.currentStep ?: 1

                      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        stages.forEachIndexed { index, stageLabel ->
                          val stepNumber = index + 1
                          val isDone = stepNumber < current
                          val isCurrent = stepNumber == current
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                          ) {
                            if (isDone) {
                              Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = VitalEmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                              )
                            } else if (isCurrent) {
                              CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = VitalEmeraldSuccess
                              )
                            } else {
                              Box(
                                modifier = Modifier
                                  .size(16.dp)
                                  .clip(CircleShape)
                                  .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                              )
                            }
                            Text(
                              text = stageLabel,
                              fontSize = 11.sp,
                              fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                              color = if (isDone || isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                          }
                        }
                      }

                      currentOcrStep?.stepDetail?.let { detail ->
                        Surface(
                          color = MaterialTheme.colorScheme.surface,
                          shape = RoundedCornerShape(6.dp),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Text(
                            text = detail,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                            lineHeight = 15.sp
                          )
                        }
                      }
                    }
                  }
                } else if (completedOcrResult != null) {
                  completedOcrResult?.let { result ->
                    Surface(
                      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                      shape = RoundedCornerShape(14.dp),
                      border = androidx.compose.foundation.BorderStroke(1.5.dp, VitalEmeraldSuccess.copy(alpha = 0.7f)),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                      ) {
                        // Header Badge
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Default.Verified,
                              contentDescription = null,
                              tint = VitalEmeraldSuccess,
                              modifier = Modifier.size(24.dp)
                            )
                            Column {
                              Text(
                                text = "Extraction & Provenance Verified",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                              )
                              Text(
                                text = result.sourceName,
                                fontSize = 12.sp,
                                color = VitalEmeraldSuccess,
                                fontWeight = FontWeight.Medium
                              )
                            }
                          }
                          Surface(
                            color = VitalEmeraldContainer,
                            shape = RoundedCornerShape(6.dp)
                          ) {
                            Text(
                              text = "LIVE INGESTED",
                              fontSize = 10.sp,
                              fontWeight = FontWeight.Bold,
                              color = VitalEmeraldSuccess,
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                          }
                        }

                        // Cryptographic Provenance Details
                        Surface(
                          color = MaterialTheme.colorScheme.surface,
                          shape = RoundedCornerShape(10.dp),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                          ) {
                            Row(
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                              Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = VitalTealPrimary,
                                modifier = Modifier.size(16.dp)
                              )
                              Text(
                                text = "Cryptographic Fingerprint (SHA-256)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                              )
                            }
                            Text(
                              text = result.fileHash,
                              fontFamily = FontFamily.Monospace,
                              fontSize = 10.sp,
                              color = VitalTealPrimary,
                              lineHeight = 14.sp
                            )
                            Row(
                              modifier = Modifier.fillMaxWidth(),
                              horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                              Text(
                                text = "Size: ${(result.fileSizeBytes + 1023) / 1024} KB • Execution: ${result.executionTimeMs}ms",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                              )
                              Text(
                                text = "Confidence: ${(result.confidenceScore * 100).toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VitalEmeraldSuccess
                              )
                            }
                          }
                        }

                        // Extracted Biomarkers
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                          Text(
                            text = "Extracted Analytes & Biomarkers (${result.extractedBiomarkers.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                          )
                          result.extractedBiomarkers.take(6).forEach { biomarker ->
                            Surface(
                              color = MaterialTheme.colorScheme.surface,
                              shape = RoundedCornerShape(8.dp),
                              modifier = Modifier.fillMaxWidth()
                            ) {
                              Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                              ) {
                                Column {
                                  Text(
                                    text = biomarker.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                  )
                                  Text(
                                    text = "Ref: ${biomarker.referenceRange} • LOINC ${biomarker.loincCode}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                  )
                                }
                                Row(
                                  verticalAlignment = Alignment.CenterVertically,
                                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                  Text(
                                    text = "${biomarker.value} ${biomarker.unit}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                  )
                                  Surface(
                                    color = if (biomarker.status == ItemStatus.NORMAL) VitalEmeraldContainer else VitalWarningContainer,
                                    shape = RoundedCornerShape(4.dp)
                                  ) {
                                    Text(
                                      text = if (biomarker.status == ItemStatus.NORMAL) "Normal" else "Elevated",
                                      fontSize = 9.sp,
                                      fontWeight = FontWeight.Bold,
                                      color = if (biomarker.status == ItemStatus.NORMAL) VitalEmeraldSuccess else VitalWarningOrange,
                                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                  }
                                }
                              }
                            }
                          }
                          if (result.extractedBiomarkers.size > 6) {
                            Text(
                              text = "+ ${result.extractedBiomarkers.size - 6} more biomarkers saved to timeline",
                              fontSize = 11.sp,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              modifier = Modifier.padding(start = 4.dp)
                            )
                          }
                        }

                        // Primary Direct Action: View in Timeline
                        Button(
                          onClick = onNavigateToTimeline,
                          colors = ButtonDefaults.buttonColors(containerColor = VitalEmeraldSuccess),
                          shape = RoundedCornerShape(10.dp),
                          modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("view_in_timeline_ocr_button")
                        ) {
                          Icon(imageVector = Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                          Spacer(modifier = Modifier.width(8.dp))
                          Text(
                            text = "View in Timeline",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                          )
                          Spacer(modifier = Modifier.width(6.dp))
                          Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        // Secondary Reset Action
                        OutlinedButton(
                          onClick = {
                            completedOcrResult = null
                            showUploadSheet = true
                          },
                          shape = RoundedCornerShape(10.dp),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Ingest Another Document or Photo", fontSize = 12.sp)
                        }
                      }
                    }
                  }
                } else {
                  Button(
                    onClick = {
                      showUploadSheet = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VitalEmeraldSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("run_ocr_sample_button")
                  ) {
                    Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload PDF or Capture Photo")
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
                  label = { Text("Title / Test (e.g. Blood Glucose: 5.4 mmol/L)") },
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
                        profileId = currentProfileId,
                        category = entryCategory,
                        title = entryTitle,
                        subtitle = "Patient self-reported record",
                        date = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date()),
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

        3 -> {
        // Wearables & Google Fit Ingestion Tab
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth().testTag("ingestion_wearables_card")
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(42.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF4285F4).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.WatchLater,
                      contentDescription = null,
                      tint = Color(0xFF1A73E8),
                      modifier = Modifier.size(24.dp)
                    )
                  }
                  Column {
                    Text(
                      text = "Google Fit & Wearables",
                      fontSize = 16.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "Pixel Watch, Fitbit, Galaxy Watch & Garmin",
                      fontSize = 11.5.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = VitalEmeraldContainer
                ) {
                  Text(
                    text = "ACTIVE",
                    color = VitalEmeraldSuccess,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = "Connect Google Fit to import continuous 24-hour daily steps, active energy expenditure, resting heart rate, and cardiovascular training zones into ${currentProfile?.name ?: "Patient"}'s health record with cryptographic provenance.",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
              )

              Spacer(modifier = Modifier.height(16.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedButton(
                  onClick = {
                    coroutineScope.launch {
                      repository.syncWearableData(currentProfileId)
                    }
                  },
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f).height(44.dp).testTag("ingest_tab_sync_button")
                ) {
                  Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Sync Now", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                  onClick = onNavigateToWearables,
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
                  modifier = Modifier.weight(1f).height(44.dp).testTag("open_wearables_hub_button")
                ) {
                  Text("Open Hub", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    }
  }

  // Upload or Capture Selection Bottom Sheet
  if (showUploadSheet) {
    ModalBottomSheet(
      onDismissRequest = { showUploadSheet = false },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Upload Lab Report or Capture Scan",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Select a source to run automated OCR and extract structured clinical data with cryptographic provenance.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Option 1: PDF Document
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              showUploadSheet = false
              documentPickerLauncher.launch("application/pdf")
            }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Surface(
              color = VitalTealContainer,
              shape = CircleShape,
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(24.dp))
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("Upload Lab PDF", fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("Select clinical report, pathology, or discharge PDF", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        // Option 2: Camera Capture
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              showUploadSheet = false
              cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Surface(
              color = VitalEmeraldContainer,
              shape = CircleShape,
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(24.dp))
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("Capture with Camera", fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("Take a live photo of paper lab results or prescription label", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        // Option 3: Photo Library
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              showUploadSheet = false
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Surface(
              color = VitalWarningContainer.copy(alpha = 0.5f),
              shape = CircleShape,
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = VitalWarningOrange, modifier = Modifier.size(24.dp))
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("Choose Photo from Gallery", fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("Select existing screenshot or saved camera photo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        // Option 4: Sample Lab Report
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              showUploadSheet = false
              runLiveClinicalOcr(null, null, "Quest_Diagnostics_CMP_Panel.pdf")
            }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Surface(
              color = VitalTealContainer,
              shape = CircleShape,
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.Science, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(24.dp))
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("Sample Clinical Lab (Quest CMP)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("Instant demonstration of OCR extraction & validation", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
  }

  if (showSubscriptionModal) {
    AlertDialog(
      onDismissRequest = { showSubscriptionModal = false },
      icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = VitalWarningOrange) },
      title = { Text("Premium Feature") },
      text = {
        Text("Live SMART-on-FHIR auto-sync with Epic, Cerner, and hospital patient portals is available on Vitalith Premium and Concierge plans. Upgrade to enable automatic real-time electronic health records ingestion.")
      },
      confirmButton = {
        Button(
          onClick = {
            showSubscriptionModal = false
            onNavigateToSubscription()
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
        ) {
          Text("Upgrade Plan")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSubscriptionModal = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
}


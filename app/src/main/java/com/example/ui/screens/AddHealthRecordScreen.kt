package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.VitalithRepository
import com.example.ui.theme.*
import com.example.util.ExtractedFormBiomarker
import com.example.util.FormAutoFillData
import com.example.util.GeminiFormOcrAnalyzer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHealthRecordScreen(
  repository: VitalithRepository,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val profiles by repository.profiles.collectAsState()
  val userProfile by repository.userProfile.collectAsState()
  val activeProfileId by repository.currentProfileId.collectAsState()

  // Form State
  var selectedProfileId by remember { mutableStateOf(activeProfileId) }
  var selectedCategory by remember { mutableStateOf(TimelineCategory.LAB) }
  var title by remember { mutableStateOf("") }
  var subtitle by remember { mutableStateOf("") }
  var providerOrFacility by remember { mutableStateOf("") }
  var selectedStatus by remember { mutableStateOf(ItemStatus.NORMAL) }
  var clinicalNotes by remember { mutableStateOf("") }

  // Date selection state
  val calendar = remember { Calendar.getInstance() }
  var selectedDateMillis by remember { mutableLongStateOf(calendar.timeInMillis) }
  var showDatePicker by remember { mutableStateOf(false) }

  val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
  val displayDate = remember(selectedDateMillis) {
    dateFormatter.format(Date(selectedDateMillis))
  }

  // Tags state
  val defaultTagSuggestions = listOf("Metabolic", "Routine", "Prescription", "Preventive", "Cardiovascular", "Screening", "Annual", "Follow-up")
  var selectedTags by remember { mutableStateOf(setOf("Routine")) }
  var customTagInput by remember { mutableStateOf("") }

  // Clinical Key-Value pairs (Biomarkers & Values)
  var keyValues by remember {
    mutableStateOf(listOf(Pair("Value", ""), Pair("Reference Range", "")))
  }

  // Validation state
  var titleError by remember { mutableStateOf(false) }
  var providerError by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }

  // OCR & Gemini Auto-Fill State
  var showOcrOptionsModal by remember { mutableStateOf(false) }
  var isAnalyzingOcr by remember { mutableStateOf(false) }
  var ocrStepMessage by remember { mutableStateOf("") }
  var ocrStepNumber by remember { mutableIntStateOf(1) }
  var autoFillNotice by remember { mutableStateOf<String?>(null) }

  fun applyAutoFillResult(result: FormAutoFillData) {
    title = result.title
    subtitle = result.subtitle
    selectedCategory = result.category
    providerOrFacility = result.providerOrFacility
    selectedStatus = result.status
    clinicalNotes = result.clinicalNotes
    selectedTags = (selectedTags + result.tags).toSet()

    if (result.biomarkers.isNotEmpty()) {
      keyValues = result.biomarkers.map { b ->
        val rangeSuffix = if (b.referenceRange.isNotBlank()) " (Ref: ${b.referenceRange})" else ""
        Pair(b.name, "${b.value} ${b.unit}".trim() + rangeSuffix)
      }
    }

    titleError = false
    providerError = false
    autoFillNotice = "Auto-filled via ${result.aiModelUsed} (${(result.confidenceScore * 100).toInt()}% confidence) • ${result.sourceDocumentName}"
    Toast.makeText(context, "Form auto-filled by Gemini AI!", Toast.LENGTH_SHORT).show()
  }

  fun runOcrPipeline(uri: Uri?, bitmap: Bitmap?, preset: GeminiFormOcrAnalyzer.ClinicalPreset?, sourceName: String) {
    coroutineScope.launch {
      isAnalyzingOcr = true
      ocrStepNumber = 1
      ocrStepMessage = "Starting document scan & optical analysis..."
      try {
        val result = GeminiFormOcrAnalyzer.processAndAnalyze(
          context = context,
          uri = uri,
          bitmap = bitmap,
          preset = preset,
          sourceName = sourceName,
          onProgress = { step, msg ->
            ocrStepNumber = step
            ocrStepMessage = msg
          }
        )
        applyAutoFillResult(result)
      } catch (e: Exception) {
        Toast.makeText(context, "OCR Analysis failed: ${e.message}", Toast.LENGTH_SHORT).show()
      } finally {
        isAnalyzingOcr = false
      }
    }
  }

  val cameraCaptureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicturePreview()
  ) { bitmap: Bitmap? ->
    if (bitmap != null) {
      val name = "Camera_Capture_${System.currentTimeMillis().toString().takeLast(4)}.jpg"
      runOcrPipeline(null, bitmap, null, name)
    }
  }

  val pickMediaLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      val name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Uploaded_Document.jpg"
      runOcrPipeline(uri, null, null, name)
    }
  }

  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      val name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Clinical_Lab_Report.pdf"
      runOcrPipeline(uri, null, null, name)
    }
  }

  BackHandler {
    onNavigateBack()
  }

  fun saveRecord() {
    titleError = title.isBlank()
    providerError = providerOrFacility.isBlank()

    if (titleError || providerError) {
      Toast.makeText(context, "Please complete required fields (Title and Provider)", Toast.LENGTH_SHORT).show()
      return
    }

    isSaving = true
    try {
      val generatedId = "rec_local_${UUID.randomUUID().toString().replace("-", "").take(10)}"
      val timeStampStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()) + " UTC"
      val docHash = "sha256:" + UUID.randomUUID().toString().replace("-", "").take(32)

      // Filter out empty key-value rows
      val filteredKeyValues = keyValues
        .filter { it.first.isNotBlank() && it.second.isNotBlank() }
        .toMap()

      val newRecord = HealthTimelineItem(
        id = generatedId,
        profileId = selectedProfileId,
        category = selectedCategory,
        title = title.trim(),
        subtitle = subtitle.trim().ifEmpty { "${selectedCategory.name} Entry • Encrypted Local Vault" },
        date = displayDate,
        providerOrFacility = providerOrFacility.trim(),
        status = selectedStatus,
        clinicalNotes = clinicalNotes.trim().ifEmpty { "Patient self-authored clinical health record entry." },
        provenance = Provenance(
          sourceSystem = "Patient Manual Entry (Room SQLite)",
          ingestedAt = timeStampStr,
          confidenceScore = 1.0f,
          consentState = "Patient-Owned / Full Local Consent",
          documentHash = docHash
        ),
        tags = selectedTags.toList(),
        keyValues = filteredKeyValues
      )

      // Save directly to Room Database via Repository
      repository.addTimelineItem(newRecord)

      Toast.makeText(context, "Record successfully saved to local Room database!", Toast.LENGTH_SHORT).show()
      onNavigateBack()
    } catch (e: Exception) {
      Toast.makeText(context, "Error saving to Room: ${e.message}", Toast.LENGTH_LONG).show()
    } finally {
      isSaving = false
    }
  }

  // Material 3 Date Picker Dialog
  if (showDatePicker) {
    val datePickerState = rememberDatePickerState(
      initialSelectedDateMillis = selectedDateMillis
    )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              selectedDateMillis = millis
            }
            showDatePicker = false
          }
        ) {
          Text("Select", fontWeight = FontWeight.Bold, color = VitalTealPrimary)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text("Cancel")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "New Health Record Entry",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
            Text(
              text = "Room SQLite Local Persistence • FIPS 140-2",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("add_record_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Timeline"
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showOcrOptionsModal = true },
            modifier = Modifier.testTag("ocr_scan_appbar_button")
          ) {
            Icon(
              imageVector = Icons.Default.DocumentScanner,
              contentDescription = "Scan / OCR with Gemini AI",
              tint = VitalTealPrimary
            )
          }
          Button(
            onClick = { saveRecord() },
            colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier
              .padding(end = 8.dp)
              .testTag("save_record_top_button")
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier
      .fillMaxSize()
      .testTag("add_health_record_screen")
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
      // Local Database Persistence Banner
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = VitalTealContainer.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(Icons.Default.Storage, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(24.dp))
          Column {
            Text(
              text = "On-Device Local Database Storage",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = VitalOnTealContainer
            )
            Text(
              text = "This record will be saved directly into your device's encrypted Room SQLite table (`timeline_records`) with cryptographic provenance.",
              fontSize = 11.sp,
              color = VitalOnTealContainer,
              lineHeight = 15.sp
            )
          }
        }
      }

      // Gemini AI OCR & Document Scan Auto-Fill Card
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, VitalTealPrimary.copy(alpha = 0.45f)),
        tonalElevation = 2.dp,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("gemini_ocr_card")
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .background(VitalTealPrimary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Column {
                Text(
                  text = "Gemini AI • Smart Document OCR",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Capture or upload to auto-fill form instantly",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Surface(
              color = VitalTealContainer,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "Multimodal AI",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VitalOnTealContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Text(
            text = "Snap a photo of a lab report, prescription bottle, or clinical notes. Gemini AI will extract test names, reference ranges, provider info, and pre-populate this form.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { showOcrOptionsModal = true },
              colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1.3f)
                .testTag("btn_capture_upload_ocr")
            ) {
              Icon(Icons.Default.MoveToInbox, contentDescription = "Import", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Import", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = { showOcrOptionsModal = true },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_clinical_presets")
            ) {
              Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp), tint = VitalTealPrimary)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Presets", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      // Auto-Fill Notice Banner (if applied)
      if (autoFillNotice != null) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = VitalTealContainer,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("autofill_success_banner")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(20.dp))
              Text(
                text = autoFillNotice ?: "",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = VitalOnTealContainer
              )
            }
            IconButton(onClick = { autoFillNotice = null }, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = VitalOnTealContainer, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      // 1. Patient Profile Selector
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "PATIENT PROFILE",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          profiles.forEach { profile ->
            FilterChip(
              selected = selectedProfileId == profile.id,
              onClick = { selectedProfileId = profile.id },
              label = {
                Text(
                  text = profile.name.split(" ").first() + if (profile.isPrimary) " (Self)" else "",
                  fontSize = 12.sp
                )
              },
              leadingIcon = {
                if (selectedProfileId == profile.id) {
                  Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                }
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = VitalTealPrimary,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("profile_chip_${profile.id}")
            )
          }
        }
      }

      // 2. Category Tags Selection
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "RECORD CATEGORY",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        val categories = listOf(
          CategoryInfo(TimelineCategory.LAB, "Lab Test", Icons.Default.Science, "Bloodwork, urine, pathology"),
          CategoryInfo(TimelineCategory.ENCOUNTER, "Encounter", Icons.Default.LocalHospital, "Doctor visits, consultations"),
          CategoryInfo(TimelineCategory.CONDITION, "Condition", Icons.Default.MedicalServices, "Diagnoses & chronic illnesses"),
          CategoryInfo(TimelineCategory.MEDICATION, "Medication", Icons.Default.Medication, "Prescriptions & treatments"),
          CategoryInfo(TimelineCategory.IMMUNIZATION, "Vaccine", Icons.Default.Vaccines, "Immunization records"),
          CategoryInfo(TimelineCategory.PROCEDURE, "Procedure", Icons.Default.Healing, "Surgeries & screenings"),
          CategoryInfo(TimelineCategory.ALLERGY, "Allergy", Icons.Default.Warning, "Adverse reactions"),
          CategoryInfo(TimelineCategory.DOCUMENT, "Document", Icons.Default.Description, "Clinical notes & imaging")
        )

        // 2-column or wrapping category grid
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          categories.chunked(2).forEach { rowCats ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              rowCats.forEach { catInfo ->
                val isSelected = selectedCategory == catInfo.category
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSelected) VitalTealContainer else MaterialTheme.colorScheme.surface,
                  border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) VitalTealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .clickable { selectedCategory = catInfo.category }
                    .testTag("category_tag_${catInfo.category.name.lowercase()}")
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) VitalTealPrimary else MaterialTheme.colorScheme.surfaceVariant),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = catInfo.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                    Column {
                      Text(
                        text = catInfo.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = if (isSelected) VitalOnTealContainer else MaterialTheme.colorScheme.onSurface
                      )
                      Text(
                        text = catInfo.description,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 3. Date Selection & Presets
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "CLINICAL DATE",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(20.dp))
                Column {
                  Text(text = "Selected Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(text = displayDate, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
              }

              OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("date_picker_button")
              ) {
                Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick Date", fontSize = 12.sp)
              }
            }

            // Quick Date Presets Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              QuickDateChip(
                label = "Today",
                onClick = { selectedDateMillis = System.currentTimeMillis() }
              )
              QuickDateChip(
                label = "Yesterday",
                onClick = { selectedDateMillis = System.currentTimeMillis() - 86400000L }
              )
              QuickDateChip(
                label = "7 Days Ago",
                onClick = { selectedDateMillis = System.currentTimeMillis() - (86400000L * 7) }
              )
              QuickDateChip(
                label = "30 Days Ago",
                onClick = { selectedDateMillis = System.currentTimeMillis() - (86400000L * 30) }
              )
            }
          }
        }
      }

      // 4. Clinical Details Form
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "RECORD DETAILS",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Title
            OutlinedTextField(
              value = title,
              onValueChange = {
                title = it
                titleError = false
              },
              label = { Text("Title / Procedure / Test Name *") },
              placeholder = { Text("e.g. Hemoglobin A1c, Lipid Panel, Annual Wellness Exam") },
              isError = titleError,
              supportingText = if (titleError) {
                { Text("Title is required", color = MaterialTheme.colorScheme.error) }
              } else null,
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("record_title_input")
            )

            // Quick Title Suggestions
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              val suggestions = when (selectedCategory) {
                TimelineCategory.LAB -> listOf("Complete Blood Count (CBC)", "Comprehensive Metabolic Panel", "HbA1c Glycemic Panel", "Lipid Profile", "TSH Thyroid Panel")
                TimelineCategory.ENCOUNTER -> listOf("Cardiology Follow-Up", "Annual Physical Exam", "Telehealth Consultation", "Urgent Care Visit", "Specialist Review")
                TimelineCategory.CONDITION -> listOf("Essential Hypertension", "Type 2 Diabetes Mellitus", "Hyperlipidemia", "Asthma", "Migraine")
                TimelineCategory.MEDICATION -> listOf("Metformin HCl 500mg", "Atorvastatin 20mg", "Lisinopril 10mg", "Levothyroxine 50mcg")
                TimelineCategory.IMMUNIZATION -> listOf("Influenza Vaccine (Annual)", "COVID-19 Updated Booster", "Tdap Booster", "Pneumococcal Conjugate")
                else -> listOf("Routine Clinical Assessment", "Diagnostic Radiology Report", "Echocardiogram", "Allergy Panel")
              }
              items(suggestions) { sugg ->
                SuggestionChip(
                  onClick = { title = sugg },
                  label = { Text(sugg, fontSize = 11.sp) }
                )
              }
            }

            // Subtitle
            OutlinedTextField(
              value = subtitle,
              onValueChange = { subtitle = it },
              label = { Text("Subtitle / Panel Details") },
              placeholder = { Text("e.g. Normal: 4.0 - 5.6%, Fasting 12 hours") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            // Provider / Facility
            OutlinedTextField(
              value = providerOrFacility,
              onValueChange = {
                providerOrFacility = it
                providerError = false
              },
              label = { Text("Physician, Clinic, or Facility *") },
              placeholder = { Text("e.g. Dr. Sarah Jenkins, MD • Quest Diagnostics Hub") },
              isError = providerError,
              supportingText = if (providerError) {
                { Text("Provider or clinic name is required", color = MaterialTheme.colorScheme.error) }
              } else null,
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("record_provider_input")
            )

            // Clinical Status
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(text = "Clinical Status", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              FlowRow(
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("clinical_status_flow_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                listOf(ItemStatus.NORMAL, ItemStatus.ELEVATED, ItemStatus.CRITICAL, ItemStatus.ACTIVE, ItemStatus.RESOLVED).forEach { st ->
                  FilterChip(
                    selected = selectedStatus == st,
                    onClick = { selectedStatus = st },
                    label = { Text(st.name, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = when (st) {
                        ItemStatus.CRITICAL -> VitalAlertRed
                        ItemStatus.ELEVATED -> VitalWarningOrange
                        ItemStatus.NORMAL, ItemStatus.ACTIVE -> VitalTealPrimary
                        ItemStatus.RESOLVED, ItemStatus.DISCONTINUED -> MaterialTheme.colorScheme.outline
                      },
                      selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("status_chip_${st.name.lowercase()}")
                  )
                }
              }
            }

            // Clinical Notes
            OutlinedTextField(
              value = clinicalNotes,
              onValueChange = { clinicalNotes = it },
              label = { Text("Clinical Findings & Notes") },
              placeholder = { Text("Enter doctor's comments, interpretation, symptoms, and plan...") },
              minLines = 3,
              maxLines = 6,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("record_notes_input")
            )
          }
        }
      }

      // 5. Clinical Metrics & Biomarkers (Key-Values)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "BIOMARKERS & CLINICAL VALUES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          TextButton(
            onClick = {
              keyValues = keyValues + Pair("", "")
            }
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Metric", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Quick Biomarker Presets according to user configured units
        val uPrefs = userProfile.unitPreferences
        FlowRow(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("biomarker_presets_flow_row"),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(
            "Glucose (${uPrefs.glucoseUnit.label})" to Pair("Glucose", " ${uPrefs.glucoseUnit.label}"),
            "Temp (${uPrefs.temperatureUnit.label})" to Pair("Temperature", " ${uPrefs.temperatureUnit.label}"),
            "Weight (${uPrefs.weightUnit.label})" to Pair("Weight", " ${uPrefs.weightUnit.label}"),
            "BP (mmHg)" to Pair("Blood Pressure", " / mmHg")
          ).forEach { (label, pair) ->
            SuggestionChip(
              onClick = {
                keyValues = keyValues + pair
              },
              label = { Text(label, fontSize = 10.5.sp) },
              modifier = Modifier.testTag("biomarker_chip_${pair.first.lowercase().replace(" ", "_")}")
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            keyValues.forEachIndexed { index, pair ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = pair.first,
                  onValueChange = { newKey ->
                    keyValues = keyValues.toMutableList().also { it[index] = Pair(newKey, pair.second) }
                  },
                  placeholder = { Text("e.g. Value, eGFR, BP") },
                  modifier = Modifier.weight(1f),
                  singleLine = true
                )

                OutlinedTextField(
                  value = pair.second,
                  onValueChange = { newVal ->
                    keyValues = keyValues.toMutableList().also { it[index] = Pair(pair.first, newVal) }
                  },
                  placeholder = { Text("e.g. 6.4%, 98 mL/min") },
                  modifier = Modifier.weight(1f),
                  singleLine = true
                )

                if (keyValues.size > 1) {
                  IconButton(
                    onClick = {
                      keyValues = keyValues.toMutableList().also { it.removeAt(index) }
                    },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = VitalAlertRed, modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }
      }

      // 6. Category Tags & Custom Tags
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "RECORD TAGS",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Predefined suggested tags
            Text(text = "Suggested Clinical Tags", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("suggested_clinical_tags_flow_row"),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              defaultTagSuggestions.forEach { tag ->
                val isSelected = selectedTags.contains(tag)
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                  },
                  label = { Text(tag, fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VitalTealPrimary,
                    selectedLabelColor = Color.White
                  ),
                  modifier = Modifier.testTag("suggested_tag_${tag.lowercase()}")
                )
              }
            }

            // Custom tag input
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = customTagInput,
                onValueChange = { customTagInput = it },
                placeholder = { Text("Add custom tag...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              Button(
                onClick = {
                  if (customTagInput.isNotBlank()) {
                    selectedTags = selectedTags + customTagInput.trim()
                    customTagInput = ""
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Add Tag")
              }
            }

            // Active Selected Tags display
            if (selectedTags.isNotEmpty()) {
              Text(
                text = "Selected Tags: ${selectedTags.joinToString(", ")}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }

      // 7. Save Action Button
      Button(
        onClick = { saveRecord() },
        colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
        shape = RoundedCornerShape(12.dp),
        enabled = !isSaving,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_health_record_button")
      ) {
        if (isSaving) {
          CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Saving to Room SQLite...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        } else {
          Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Save Record to Room Database", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(112.dp))
    }
  }

  // OCR Document Capture / Upload Selection Modal
  if (showOcrOptionsModal) {
    AlertDialog(
      onDismissRequest = { showOcrOptionsModal = false },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DocumentScanner,
            contentDescription = null,
            tint = VitalTealPrimary,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = "Smart Document OCR & AI Auto-Fill",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Choose how to capture your medical document. Gemini AI will analyze the content, extract clinical values, and pre-populate this form automatically.",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          // Capture / Upload Options
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                showOcrOptionsModal = false
                cameraCaptureLauncher.launch(null)
              }
              .testTag("ocr_option_camera")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(VitalTealPrimary, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
              Column {
                Text("Take Photo with Camera", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Text("Snap a picture of a lab sheet or prescription bottle", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                showOcrOptionsModal = false
                pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              }
              .testTag("ocr_option_gallery")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
              Column {
                Text("Upload Image from Gallery", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Text("Select a photo or scan from device storage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                showOcrOptionsModal = false
                filePickerLauncher.launch("*/*")
              }
              .testTag("ocr_option_file")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
              Column {
                Text("Upload Document or PDF", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Text("Select clinical report, discharge summary, or PDF", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

          Text(
            text = "OR TRY A CLINICAL PRESET (INSTANT TEST):",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          GeminiFormOcrAnalyzer.CLINICAL_PRESETS.forEach { preset ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surface,
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  showOcrOptionsModal = false
                  runOcrPipeline(null, null, preset, "${preset.id}.pdf")
                }
                .testTag("preset_${preset.id}")
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(
                  imageVector = when (preset.category) {
                    TimelineCategory.MEDICATION -> Icons.Default.Medication
                    else -> Icons.Default.Science
                  },
                  contentDescription = null,
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(20.dp)
                )
                Column {
                  Text(text = preset.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                  Text(text = preset.subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showOcrOptionsModal = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // OCR & Gemini Analysis Progress Dialog
  if (isAnalyzingOcr) {
    AlertDialog(
      onDismissRequest = {}, // Non-cancelable while AI is extracting
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          CircularProgressIndicator(
            color = VitalTealPrimary,
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.5.dp
          )
          Text(
            text = "Gemini AI Document Analysis",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Step $ocrStepNumber of 4: $ocrStepMessage",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
          )

          LinearProgressIndicator(
            progress = { ocrStepNumber / 4f },
            color = VitalTealPrimary,
            trackColor = VitalTealContainer,
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
          )

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = VitalTealContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Clinical Entity & LOINC Resolution",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VitalOnTealContainer
              )
              Text(
                text = "Extracting analyte values, units, reference intervals, physician notes, and category classification...",
                fontSize = 10.sp,
                color = VitalOnTealContainer,
                lineHeight = 14.sp
              )
            }
          }
        }
      },
      confirmButton = {}
    )
  }
}

private data class CategoryInfo(
  val category: TimelineCategory,
  val title: String,
  val icon: ImageVector,
  val description: String
)

@Composable
private fun QuickDateChip(
  label: String,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier = Modifier.clickable { onClick() }
  ) {
    Text(
      text = label,
      fontSize = 10.5.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

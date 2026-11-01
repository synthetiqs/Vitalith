package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthTimelineItem
import com.example.data.model.TimelineCategory
import com.example.data.repository.VitalithRepository
import com.example.ui.components.GeminiHealthSummaryCard
import com.example.ui.components.MedicalTermExplainerModal
import com.example.ui.components.PatientProfileDropdownSelector
import com.example.ui.components.ProvenanceBadge
import com.example.ui.components.TimelineSpineItem
import com.example.ui.theme.*
import com.example.util.ClinicalPdfGenerator
import com.example.util.GeminiMedicalExplainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
  repository: VitalithRepository,
  onNavigateToIngest: () -> Unit,
  onNavigateToAddRecord: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val timelineItems by repository.timelineItems.collectAsState()
  val profiles by repository.profiles.collectAsState()
  val userProfile by repository.userProfile.collectAsState()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val currentProfile = remember(profiles, currentProfileId) {
    profiles.firstOrNull { it.id == currentProfileId } ?: profiles.firstOrNull()
  }
  var selectedCategory by remember { mutableStateOf(TimelineCategory.ALL) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedItemForDetail by remember { mutableStateOf<HealthTimelineItem?>(null) }
  var selectedTermForAiExplainer by remember { mutableStateOf<Pair<String, String>?>(null) }

  val profileTimelineItems = remember(timelineItems, currentProfileId) {
    timelineItems.filter { it.profileId == currentProfileId }
  }

  val filteredItems = remember(profileTimelineItems, selectedCategory, searchQuery) {
    profileTimelineItems.filter { item ->
      val matchesCategory = (selectedCategory == TimelineCategory.ALL || item.category == selectedCategory)
      val matchesSearch = searchQuery.isBlank() ||
        item.title.contains(searchQuery, ignoreCase = true) ||
        item.subtitle.contains(searchQuery, ignoreCase = true) ||
        item.clinicalNotes.contains(searchQuery, ignoreCase = true) ||
        item.tags.any { it.contains(searchQuery, ignoreCase = true) }
      matchesCategory && matchesSearch
    }
  }

  Box(
    modifier = modifier.fillMaxSize()
  ) {
    Column(
      modifier = Modifier
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
            Column {
              Text(
                text = "Longitudinal Timeline",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Unified patient-owned record across providers & years",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              IconButton(
                onClick = onNavigateToAddRecord,
                modifier = Modifier.testTag("add_record_button")
              ) {
                Icon(
                  imageVector = Icons.Default.AddCircle,
                  contentDescription = "New Health Record",
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(28.dp)
                )
              }
              IconButton(
                onClick = onNavigateToIngest,
                modifier = Modifier.testTag("ingest_sync_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Sync,
                  contentDescription = "Sync EHR / OCR",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(24.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Active Patient Profile Selector Dropdown
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Patient Record:",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PatientProfileDropdownSelector(
              profiles = profiles,
              currentProfileId = currentProfileId,
              onSwitchProfile = { repository.switchProfile(it) },
              modifier = Modifier.testTag("timeline_patient_dropdown")
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search labs, meds, conditions, encounters...", fontSize = 13.sp) },
          leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
          val categories = listOf(
            TimelineCategory.ALL,
            TimelineCategory.LAB,
            TimelineCategory.MEDICATION,
            TimelineCategory.ENCOUNTER,
            TimelineCategory.CONDITION,
            TimelineCategory.PROCEDURE,
            TimelineCategory.ALLERGY,
            TimelineCategory.IMMUNIZATION,
            TimelineCategory.DOCUMENT
          )
          items(categories) { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = {
                Text(
                  text = if (cat == TimelineCategory.ALL) "All (${profileTimelineItems.size})" else cat.name,
                  fontSize = 11.sp,
                  fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = VitalTealPrimary,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("filter_${cat.name.lowercase()}")
            )
          }
        }
      }
    }

    // Timeline List
    if (filteredItems.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "No clinical records found",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Try adjusting your search query or filter category.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
      ) {
        item {
          GeminiHealthSummaryCard(
            timelineItems = filteredItems,
            patientName = currentProfile?.name ?: userProfile.fullName,
            unitPreferences = userProfile.unitPreferences,
            modifier = Modifier.padding(bottom = 12.dp)
          )
        }

        itemsIndexed(filteredItems, key = { _, item -> item.id }) { index, item ->
          TimelineSpineItem(
            item = item,
            isFirst = index == 0,
            isLast = index == filteredItems.size - 1,
            onClick = { selectedItemForDetail = item }
          )
        }
      }
    }
  }

  // Floating Action Button for New Entry
  FloatingActionButton(
    onClick = onNavigateToAddRecord,
    containerColor = VitalTealPrimary,
    contentColor = Color.White,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .align(Alignment.BottomEnd)
      .padding(bottom = 84.dp, end = 20.dp)
      .testTag("timeline_add_record_fab")
  ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = "New Entry", modifier = Modifier.size(20.dp))
        Text("New Entry", fontWeight = FontWeight.Bold, fontSize = 13.sp)
      }
    }
  }

  // Material 3 Modal Bottom Sheet for Clinical Details
  selectedItemForDetail?.let { item ->
    ModalBottomSheet(
      onDismissRequest = { selectedItemForDetail = null },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          CategoryChip(category = item.category)
          StatusBadge(status = item.status)
        }

        Text(
          text = item.title,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          letterSpacing = (-0.3).sp
        )

        Text(
          text = "${item.date} • ${item.providerOrFacility}",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Clinical Interpretation & Notes",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = item.clinicalNotes,
              fontSize = 14.sp,
              lineHeight = 22.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        // Gemini AI Medical Term Explainer Section
        val medicalTerms = remember(item) {
          GeminiMedicalExplainer.extractMedicalTerms(
            title = item.title,
            subtitle = item.subtitle,
            notes = item.clinicalNotes,
            keyValues = item.keyValues,
            tags = item.tags
          )
        }

        if (medicalTerms.isNotEmpty()) {
          Card(
            colors = CardDefaults.cardColors(containerColor = VitalTealContainer.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimary.copy(alpha = 0.3f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("medical_terms_ai_section")
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "Medical Terms (Tap for Vitalith AI Explanation)",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = VitalOnTealContainer
                )
              }

              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                items(medicalTerms) { term ->
                  SuggestionChip(
                    onClick = {
                      selectedTermForAiExplainer = Pair(term, item.clinicalNotes)
                    },
                    label = {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Icon(
                          Icons.AutoMirrored.Filled.HelpOutline,
                          contentDescription = null,
                          modifier = Modifier.size(12.dp),
                          tint = VitalTealPrimary
                        )
                        Text(
                          text = term,
                          fontSize = 11.5.sp,
                          fontWeight = FontWeight.Bold,
                          color = VitalTealPrimary
                        )
                      }
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                      containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                      borderColor = VitalTealPrimary.copy(alpha = 0.4f),
                      enabled = true
                    ),
                    modifier = Modifier.testTag("term_chip_${term.lowercase().replace(" ", "_")}")
                  )
                }
              }
            }
          }
        }

        if (item.keyValues.isNotEmpty()) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(text = "Clinical Values & Ranges", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              item.keyValues.forEach { (k, v) ->
                val localized = com.example.util.UcumUnitConverter.formatLocalizedKeyValue(k, v, userProfile.unitPreferences)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = k, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (localized.wasConverted) {
                      Surface(
                        color = VitalTealContainerDark.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text(
                          text = "UCUM: ${localized.ucumCode}",
                          fontSize = 9.sp,
                          color = VitalTealPrimary,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }
                  Text(text = localized.displayString, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        Text(text = "Provenance & Cryptographic Verification", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        ProvenanceBadge(provenance = item.provenance, showFullDetails = true)

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = {
              try {
                val pdfFile = ClinicalPdfGenerator.generateRecordPdf(
                  context = context,
                  item = item,
                  profile = currentProfile
                )
                repository.recordAudit(
                  action = "PDF_RECORD_EXPORT",
                  resource = "Exported clinical document PDF for '${item.title}'"
                )
                ClinicalPdfGenerator.shareFile(
                  context = context,
                  file = pdfFile,
                  mimeType = "application/pdf",
                  chooserTitle = "Export & Share Clinical Record PDF"
                )
              } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("export_record_pdf_button")
          ) {
            Icon(
              imageVector = Icons.Default.PictureAsPdf,
              contentDescription = null,
              tint = VitalTealPrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = { selectedItemForDetail = null },
            colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Done")
          }
        }
      }
    }
  }

  // Gemini AI Medical Term Explainer Modal
  selectedTermForAiExplainer?.let { (term, ctx) ->
    MedicalTermExplainerModal(
      term = term,
      clinicalContext = ctx,
      onDismiss = { selectedTermForAiExplainer = null }
    )
  }
}

@Composable
fun TimelineCard(
  item: HealthTimelineItem,
  onClick: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .testTag("timeline_item_${item.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          CategoryChip(category = item.category)
          Text(
            text = item.date,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        StatusBadge(status = item.status)
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = item.title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = item.subtitle,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2
      )

      Spacer(modifier = Modifier.height(10.dp))

      ProvenanceBadge(provenance = item.provenance)
    }
  }
}

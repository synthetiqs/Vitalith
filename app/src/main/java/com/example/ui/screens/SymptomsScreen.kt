package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.data.repository.VitalithRepository
import com.example.ui.components.PatientProfileDropdownSelector
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Calm, non-alarmist severity color palette: Sage -> Ochre -> Terracotta Plum
private val SeveritySage = Color(0xFF4A7C59)       // Mild (0-3)
private val SeverityOchre = Color(0xFFC67D00)      // Moderate (4-6)
private val SeverityTerracotta = Color(0xFFA83E3E) // Severe (7-10)

private fun getSeverityColor(sev: Int): Color {
  return when {
    sev <= 3 -> SeveritySage
    sev <= 6 -> SeverityOchre
    else -> SeverityTerracotta
  }
}

private fun getSeverityLabel(sev: Int): String {
  return when {
    sev == 0 -> "Subtle / Trace (0/10)"
    sev <= 3 -> "Mild ($sev/10)"
    sev <= 6 -> "Moderate ($sev/10)"
    sev <= 8 -> "Severe ($sev/10)"
    else -> "Very Severe ($sev/10)"
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomsScreen(
  repository: VitalithRepository,
  onNavigateBack: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  BackHandler { onNavigateBack() }

  val context = LocalContext.current
  val profiles by repository.profiles.collectAsState()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val allSymptoms by repository.symptoms.collectAsState()
  val allMeds by repository.medications.collectAsState()

  val currentProfile = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.first()
  val profileSymptoms = remember(allSymptoms, currentProfileId) {
    allSymptoms.filter { it.profileId == currentProfileId }
  }
  val profileMeds = remember(allMeds, currentProfileId) {
    allMeds.filter { it.profileId == currentProfileId && it.status == ItemStatus.ACTIVE }
  }

  var selectedTab by remember { mutableStateOf(0) }
  var showQuickAddDialog by remember { mutableStateOf(false) }
  var activeRedFlagAlert by remember { mutableStateOf<RedFlagAlert?>(null) }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .testTag("symptoms_screen"),
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("symptoms_back_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Navigate Back",
                  tint = MaterialTheme.colorScheme.onSurface
                )
              }
              Column {
                Text(
                  text = "Symptoms & Wellness",
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Longitudinal Pattern Observation",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            PatientProfileDropdownSelector(
              profiles = profiles,
              currentProfileId = currentProfileId,
              onSwitchProfile = { repository.switchProfile(it) },
              testTagPrefix = "symptoms_profile"
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              modifier = Modifier.testTag("symptoms_tab_timeline"),
              text = {
                Text("Timeline & Strip", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
              },
              icon = {
                Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
              }
            )
            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              modifier = Modifier.testTag("symptoms_tab_export"),
              text = {
                Text("Doctor Report", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
              },
              icon = {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
              }
            )
          }
        }
      }
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showQuickAddDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("quick_log_symptom_fab")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = "Quick Log")
          Text("Quick Log", fontWeight = FontWeight.Bold)
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background)
    ) {
      if (selectedTab == 0) {
        SymptomsTimelineView(
          profile = currentProfile,
          symptoms = profileSymptoms,
          activeMedications = profileMeds,
          onQuickAdd = { showQuickAddDialog = true },
          onCloseLoop = { id -> repository.closeSymptom(id) },
          onDeleteSymptom = { id -> repository.deleteSymptom(id) },
          repository = repository
        )
      } else {
        DoctorReadyExportView(
          profile = currentProfile,
          repository = repository
        )
      }

      // Quick Add Dialog
      if (showQuickAddDialog) {
        QuickAddSymptomDialog(
          profileId = currentProfileId,
          repository = repository,
          onDismiss = { showQuickAddDialog = false },
          onSymptomLogged = { item, alert ->
            showQuickAddDialog = false
            if (alert != null) {
              activeRedFlagAlert = alert
            } else {
              Toast.makeText(context, "Logged: ${item.symptomName} (${item.severityLabel})", Toast.LENGTH_SHORT).show()
            }
          }
        )
      }

      // Red Flag Emergency Banner Dialog
      activeRedFlagAlert?.let { alert ->
        EmergencyRedFlagDialog(
          alert = alert,
          onDismiss = { activeRedFlagAlert = null }
        )
      }
    }
  }
}

/**
 * Responsive header layout that displays a title block and a badge side-by-side if there is sufficient
 * horizontal space; otherwise, shifts the badge gracefully to the next line below the title.
 */
@Composable
private fun ResponsiveHeaderWithBadge(
  titleContent: @Composable () -> Unit,
  badgeContent: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  horizontalSpacing: androidx.compose.ui.unit.Dp = 10.dp,
  verticalSpacing: androidx.compose.ui.unit.Dp = 8.dp
) {
  Layout(
    content = {
      Box(modifier = Modifier.wrapContentSize()) {
        titleContent()
      }
      Box(modifier = Modifier.wrapContentSize()) {
        badgeContent()
      }
    },
    modifier = modifier.fillMaxWidth()
  ) { measurables, constraints ->
    val horizontalSpacingPx = horizontalSpacing.roundToPx()
    val verticalSpacingPx = verticalSpacing.roundToPx()
    val maxAvailableWidth = constraints.maxWidth

    val badgeMeasurable = measurables[1]
    val badgePlaceable = badgeMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val badgeWidth = badgePlaceable.width

    val titleMeasurable = measurables[0]
    val titleIntrinsicWidth = titleMeasurable.maxIntrinsicWidth(constraints.maxHeight)

    val canFitTogether = (titleIntrinsicWidth + badgeWidth + horizontalSpacingPx) <= maxAvailableWidth

    if (canFitTogether) {
      val titlePlaceable = titleMeasurable.measure(
        constraints.copy(
          minWidth = 0,
          maxWidth = maxAvailableWidth - badgeWidth - horizontalSpacingPx
        )
      )
      val rowHeight = maxOf(titlePlaceable.height, badgePlaceable.height)
      layout(maxAvailableWidth, rowHeight) {
        titlePlaceable.placeRelative(0, (rowHeight - titlePlaceable.height) / 2)
        badgePlaceable.placeRelative(maxAvailableWidth - badgeWidth, (rowHeight - badgePlaceable.height) / 2)
      }
    } else {
      val titlePlaceable = titleMeasurable.measure(
        constraints.copy(minWidth = 0, maxWidth = maxAvailableWidth)
      )
      val totalHeight = titlePlaceable.height + verticalSpacingPx + badgePlaceable.height
      layout(maxAvailableWidth, totalHeight) {
        titlePlaceable.placeRelative(0, 0)
        badgePlaceable.placeRelative(0, titlePlaceable.height + verticalSpacingPx)
      }
    }
  }
}

@Composable
private fun SymptomsTimelineView(
  profile: PatientProfile,
  symptoms: List<SymptomItem>,
  activeMedications: List<MedicationItem>,
  onQuickAdd: () -> Unit,
  onCloseLoop: (String) -> Unit,
  onDeleteSymptom: (String) -> Unit,
  repository: VitalithRepository
) {
  val ongoingCount = symptoms.count { it.isOngoing }
  val uniqueSymptomNames = remember(symptoms) { symptoms.map { it.symptomName }.distinct() }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Clinical Context Strip
    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          ResponsiveHeaderWithBadge(
            titleContent = {
              Column {
                Text(
                  text = "${profile.name}'s Wellness Strip",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Parallel tracking of symptoms, active meds, and context",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            },
            badgeContent = {
              if (ongoingCount > 0) {
                Surface(
                  color = SeverityOchre.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("symptoms_active_alert_badge")
                ) {
                  Text(
                    text = "$ongoingCount Active",
                    color = SeverityOchre,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              } else {
                Surface(
                  color = SeveritySage.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("symptoms_all_resolved_badge")
                ) {
                  Text(
                    text = "All Resolved",
                    color = SeveritySage,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Longitudinal Visual Lane
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Symptoms:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(75.dp)
              )
              if (symptoms.isEmpty()) {
                Text("None recorded", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              } else {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                  symptoms.take(4).forEach { item ->
                    val color = getSeverityColor(item.severity)
                    Surface(
                      color = color.copy(alpha = 0.15f),
                      shape = RoundedCornerShape(8.dp),
                      border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Box(
                          modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(color)
                        )
                        Text(
                          text = "${item.symptomName.split("/").first().trim()} (${item.severity}/10)",
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          color = color
                        )
                      }
                    }
                  }
                }
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Active Meds:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(75.dp)
              )
              if (activeMedications.isEmpty()) {
                Text("None active", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              } else {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                  activeMedications.take(3).forEach { med ->
                    Surface(
                      color = VitalTealContainer.copy(alpha = 0.7f),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text(
                        text = med.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VitalOnTealContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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

    // Pattern & Recurrence Observation Cards
    if (uniqueSymptomNames.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Longitudinal Pattern Observations",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          uniqueSymptomNames.take(2).forEach { sName ->
            val stats = repository.calculateSymptomRecurrence(profile.id, sName)
            if (stats.coOccurrenceObservation != null) {
              Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.Top,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.AutoGraph,
                    contentDescription = null,
                    tint = VitalTealPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(
                        text = sName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Text(
                        text = "${stats.totalEpisodesThisMonth} episode(s)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    // CLINICAL_REVIEW_REQUIRED: Descriptive pattern observation without causal verbs
                    Text(
                      text = stats.coOccurrenceObservation,
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      lineHeight = 16.sp
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Episode Cards Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Logged Episodes (${symptoms.size})",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = onQuickAdd) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Quick Log", fontSize = 12.sp)
        }
      }
    }

    if (symptoms.isEmpty()) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.HealthAndSafety,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No Symptoms Recorded Yet",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Use the 10-second Quick Log to record episodes, severity, and context factors.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = onQuickAdd,
              modifier = Modifier.testTag("empty_state_quick_log_button")
            ) {
              Text("Log First Symptom")
            }
          }
        }
      }
    } else {
      items(symptoms, key = { it.id }) { item ->
        SymptomEpisodeCard(
          item = item,
          onCloseLoop = { onCloseLoop(item.id) },
          onDelete = { onDeleteSymptom(item.id) }
        )
      }
    }
  }
}

@Composable
private fun SymptomEpisodeCard(
  item: SymptomItem,
  onCloseLoop: () -> Unit,
  onDelete: () -> Unit
) {
  val severityColor = getSeverityColor(item.severity)

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp),
    tonalElevation = 2.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("symptom_card_${item.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(severityColor)
          )
          Column {
            Text(
              text = item.symptomName,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Onset: ${item.onsetDate} • Site: ${item.bodySite ?: "Unspecified"}",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          color = severityColor.copy(alpha = 0.12f),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = "${item.severity}/10 ${item.severityLabel}",
            color = severityColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Duration & Status Loop
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = if (item.isOngoing) Icons.Default.Timelapse else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (item.isOngoing) SeverityOchre else SeveritySage,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = if (item.isOngoing) "Ongoing Episode" else "Duration: ${item.durationDisplay}",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (item.isOngoing) SeverityOchre else SeveritySage
          )
        }

        if (item.isOngoing) {
          OutlinedButton(
            onClick = onCloseLoop,
            modifier = Modifier
              .height(30.dp)
              .testTag("resolve_symptom_${item.id}"),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Mark Resolved", fontSize = 11.sp)
          }
        }
      }

      // Context Tags & Impact Chips
      if (item.contextTags.isNotEmpty() || item.impact.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
          item.contextTags.forEach { tag ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = tag,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          item.impact.forEach { imp ->
            Surface(
              color = SeverityTerracotta.copy(alpha = 0.1f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "Impact: $imp",
                fontSize = 10.sp,
                color = SeverityTerracotta,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }

      if (item.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "\"${item.notes}\"",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 15.sp
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "SNOMED-CT: ${item.snomedCode}",
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          fontFamily = FontFamily.Monospace
        )
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(24.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete Symptom Entry",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddSymptomDialog(
  profileId: String,
  repository: VitalithRepository,
  onDismiss: () -> Unit,
  onSymptomLogged: (SymptomItem, RedFlagAlert?) -> Unit
) {
  var naturalLanguageInput by remember { mutableStateOf("") }
  var selectedSymptom by remember { mutableStateOf<SymptomDefinition?>(SymptomCatalog.definitions.first()) }
  var customSymptomName by remember { mutableStateOf("") }
  var severity by remember { mutableStateOf(5) }
  var selectedBodySite by remember { mutableStateOf(SymptomCatalog.definitions.first().defaultBodySite ?: "General") }
  val selectedContextTags = remember { mutableStateListOf<String>() }
  val selectedImpact = remember { mutableStateListOf<String>() }
  var notes by remember { mutableStateOf("") }
  var showProgressiveDisclosure by remember { mutableStateOf(false) }

  // Available context options
  val contextOptions = listOf(
    "Poor Sleep (<6h)", "High Stress", "Skipped Meal", "Luteal Phase", "Post-Exercise", "Medication Taken", "Cold Weather"
  )
  val impactOptions = listOf(
    "Work / Focus Limited", "Sleep Interrupted", "Movement Limited", "Bedridden"
  )
  val bodySites = listOf(
    "Right Temple", "Forehead", "Epigastric", "Lower Abdomen", "Lumbar Spine", "Bilateral Knees", "Arms / Torso", "Whole Body"
  )

  // Real-time red flag evaluation
  val liveSymptomName = if (customSymptomName.isNotBlank()) customSymptomName else selectedSymptom?.name ?: "Symptom"
  val redFlagAlert = remember(liveSymptomName, severity, notes, selectedContextTags) {
    repository.evaluateRedFlags(liveSymptomName, severity, notes, selectedContextTags.toList())
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.90f)
        .testTag("quick_add_symptom_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "10-Second Quick Log",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Fast capture with depth on demand",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close Dialog")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // 1. Natural Language Parse Quick Entry
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Natural Language Quick-Entry",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = naturalLanguageInput,
                  onValueChange = { naturalLanguageInput = it },
                  placeholder = { Text("e.g. headache, 6 out of 10, poor sleep", fontSize = 12.sp) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("nlp_symptom_input"),
                  singleLine = true
                )
                Button(
                  onClick = {
                    if (naturalLanguageInput.isNotBlank()) {
                      val (parsed, _) = repository.parseNaturalLanguageSymptom(naturalLanguageInput)
                      customSymptomName = parsed.symptomName
                      severity = parsed.severity
                      notes = parsed.notes
                      selectedContextTags.clear()
                      selectedContextTags.addAll(parsed.contextTags)
                      if (parsed.bodySite != null) selectedBodySite = parsed.bodySite
                    }
                  },
                  modifier = Modifier.testTag("nlp_parse_button"),
                  contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                  Text("Parse", fontSize = 12.sp)
                }
              }
            }
          }

          // 2. Curated 1-Tap Symptom Chips
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Select Symptom:",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              SymptomCatalog.definitions.take(6).forEach { def ->
                val isSelected = selectedSymptom?.id == def.id && customSymptomName.isBlank()
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    selectedSymptom = def
                    customSymptomName = ""
                    if (def.defaultBodySite != null) selectedBodySite = def.defaultBodySite
                  },
                  label = { Text(def.name.split("/").first().trim(), fontSize = 11.sp) },
                  modifier = Modifier.testTag("symptom_chip_${def.id}")
                )
              }
            }

            OutlinedTextField(
              value = customSymptomName,
              onValueChange = { customSymptomName = it },
              placeholder = { Text("Or enter other symptom...", fontSize = 12.sp) },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_symptom_name_input"),
              singleLine = true
            )
          }

          // 3. Continuous Severity Slider with Calm Verbal Anchors
          val severityColor = getSeverityColor(severity)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Severity (0-10):",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Surface(
                color = severityColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = getSeverityLabel(severity),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = severityColor,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }

            Slider(
              value = severity.toFloat(),
              onValueChange = { severity = it.toInt() },
              valueRange = 0f..10f,
              steps = 9,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("severity_slider")
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("0 (Trace)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("5 (Moderate)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("10 (Severe)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          // Real-time Red-Flag Warning Banner
          redFlagAlert?.let { alert ->
            Surface(
              color = SeverityTerracotta.copy(alpha = 0.12f),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, SeverityTerracotta.copy(alpha = 0.6f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_add_red_flag_banner")
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.WarningAmber,
                  contentDescription = null,
                  tint = SeverityTerracotta,
                  modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = alert.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = SeverityTerracotta
                  )
                  Text(
                    text = alert.message,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 15.sp
                  )
                }
              }
            }
          }

          // 4. Progressive Disclosure Toggle (Depth on Demand)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showProgressiveDisclosure = !showProgressiveDisclosure }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = if (showProgressiveDisclosure) "Hide Advanced Context" else "+ More Details (Body Site, Impact, Context)",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
            Icon(
              imageVector = if (showProgressiveDisclosure) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
          }

          if (showProgressiveDisclosure) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              // Body Site Selection
              Text("Body Map / Region:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                bodySites.forEach { site ->
                  val isSelected = selectedBodySite == site
                  FilterChip(
                    selected = isSelected,
                    onClick = { selectedBodySite = site },
                    label = { Text(site, fontSize = 10.sp) }
                  )
                }
              }

              // Context Factors
              Text("Context Factors (What was happening?):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                contextOptions.forEach { tag ->
                  val isSelected = selectedContextTags.contains(tag)
                  FilterChip(
                    selected = isSelected,
                    onClick = {
                      if (isSelected) selectedContextTags.remove(tag) else selectedContextTags.add(tag)
                    },
                    label = { Text(tag, fontSize = 10.sp) }
                  )
                }
              }

              // Daily Life Impact
              Text("Daily Life Impact:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                impactOptions.forEach { imp ->
                  val isSelected = selectedImpact.contains(imp)
                  FilterChip(
                    selected = isSelected,
                    onClick = {
                      if (isSelected) selectedImpact.remove(imp) else selectedImpact.add(imp)
                    },
                    label = { Text(imp, fontSize = 10.sp) }
                  )
                }
              }

              // Optional Notes
              OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Observations", fontSize = 11.sp) },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("symptom_notes_input"),
                minLines = 2
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Save Button
        Button(
          onClick = {
            val sName = if (customSymptomName.isNotBlank()) customSymptomName else selectedSymptom?.name ?: "Symptom"
            val snomed = selectedSymptom?.snomedCode ?: "418290006"
            val sevLabel = when {
              severity <= 3 -> "Mild"
              severity <= 6 -> "Moderate"
              else -> "Severe"
            }
            val currentDateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())

            val item = SymptomItem(
              id = "sym_${UUID.randomUUID().toString().take(8)}",
              profileId = profileId,
              symptomName = sName,
              snomedCode = snomed,
              severity = severity,
              severityLabel = sevLabel,
              onsetDate = currentDateStr,
              onsetTimestamp = System.currentTimeMillis(),
              isOngoing = true,
              bodySite = selectedBodySite,
              contextTags = selectedContextTags.toList(),
              impact = selectedImpact.toList(),
              notes = notes,
              isRedFlag = redFlagAlert != null,
              redFlagMessage = redFlagAlert?.message
            )

            repository.addSymptom(item)
            onSymptomLogged(item, redFlagAlert)
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("save_symptom_button"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Save Symptom Entry", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun DoctorReadyExportView(
  profile: PatientProfile,
  repository: VitalithRepository
) {
  val context = LocalContext.current
  val exportText = remember(profile.id) { repository.generateDoctorSymptomExportText(profile.id) }
  val allSymptoms by repository.symptoms.collectAsState()
  val allMeds by repository.medications.collectAsState()
  val profileSymptoms = remember(allSymptoms, profile.id) {
    allSymptoms.filter { it.profileId == profile.id }
  }
  val profileMeds = remember(allMeds, profile.id) {
    allMeds.filter { it.profileId == profile.id && it.status == ItemStatus.ACTIVE }
  }
  val uniqueSymptomNames = remember(profileSymptoms) {
    profileSymptoms.map { it.symptomName }.distinct()
  }
  var showRawExport by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          ResponsiveHeaderWithBadge(
            titleContent = {
              Column {
                Text(
                  text = "Clinical Consultation Summary",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Structured for doctor review • Interoperable FHIR format",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            },
            badgeContent = {
              Surface(
                color = VitalTealContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("doctor_report_fhir_badge")
              ) {
                Text(
                  text = "FHIR R4",
                  color = VitalOnTealContainer,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          )

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Vitalith Doctor Report", exportText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied Clinical Summary to clipboard!", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier
                .weight(1f)
                .testTag("copy_doctor_report_button"),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Copy Report", fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = {
                val sendIntent = Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, exportText)
                  type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Clinical Summary"))
              },
              modifier = Modifier
                .weight(1f)
                .testTag("export_fhir_pdf_button"),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Share", fontSize = 12.sp)
            }
          }
        }
      }
    }

    // Professional & Appealing Clinical Document Preview
    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("doctor_report_preview_card")
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Document Header Banner
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
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(VitalTealContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.LocalHospital,
                  contentDescription = null,
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(22.dp)
                )
              }
              Column {
                Text(
                  text = "VITALITH CLINICAL REPORT",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = VitalTealPrimary,
                  letterSpacing = 0.8.sp
                )
                Text(
                  text = "Longitudinal Symptom Brief",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            Surface(
              color = VitalTealContainerDark.copy(alpha = 0.15f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "CONFIDENTIAL",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = VitalTealPrimary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

          // Patient Demographics & Encounter Metadata (Grid)
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(12.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("PATIENT NAME", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(profile.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text("DOB • MRN", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text("${profile.dob} • ${profile.mrn}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("PRIMARY PHYSICIAN", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(profile.primaryPhysician, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text("REPORT DATE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
              }
            }
          }

          // Clinical Disclaimer Note
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            shape = RoundedCornerShape(8.dp)
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(16.dp))
              Text(
                text = "Descriptive observational log prepared for clinician review. Does not constitute a clinical diagnosis.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 15.sp
              )
            }
          }

          // Section 1: Active Medications
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.Medication, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(16.dp))
              Text(
                text = "CURRENT ACTIVE MEDICATIONS (${profileMeds.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            if (profileMeds.isEmpty()) {
              Text("No active medications documented.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                profileMeds.forEach { med ->
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column {
                        Text(
                          text = "${med.name} ${med.dosage}",
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 12.sp,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                          text = med.frequency,
                          fontSize = 10.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                      Surface(
                        color = VitalTealContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                      ) {
                        Text(
                          text = med.indication,
                          fontSize = 9.5.sp,
                          color = VitalOnTealContainer,
                          fontWeight = FontWeight.Medium,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          // Section 2: Recorded Symptom Episodes
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(16.dp))
              Text(
                text = "RECORDED SYMPTOM EPISODES (${profileSymptoms.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            if (profileSymptoms.isEmpty()) {
              Text("No symptom episodes recorded for this reporting period.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                profileSymptoms.forEachIndexed { index, sym ->
                  val color = getSeverityColor(sym.severity)
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(
                      modifier = Modifier.padding(12.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp),
                          modifier = Modifier.weight(1f)
                        ) {
                          Text(
                            text = "[${index + 1}] ${sym.symptomName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                          )
                          Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                          ) {
                            Text(
                              text = "SNOMED ${sym.snomedCode}",
                              fontSize = 9.sp,
                              fontFamily = FontFamily.Monospace,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                          }
                        }

                        Surface(
                          color = color.copy(alpha = 0.15f),
                          shape = RoundedCornerShape(6.dp)
                        ) {
                          Text(
                            text = "${sym.severity}/10 ${sym.severityLabel}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "Onset: ${sym.onsetDate} • ${if (sym.isOngoing) "Ongoing" else "Resolved (${sym.durationDisplay})"}",
                          fontSize = 11.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        sym.bodySite?.let { site ->
                          Text(
                            text = "Site: $site",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                        }
                      }

                      if (sym.contextTags.isNotEmpty() || sym.impact.isNotEmpty()) {
                        Row(
                          horizontalArrangement = Arrangement.spacedBy(4.dp),
                          modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                          sym.contextTags.forEach { tag ->
                            Surface(
                              color = VitalTealContainer.copy(alpha = 0.5f),
                              shape = RoundedCornerShape(4.dp)
                            ) {
                              Text(
                                text = tag,
                                fontSize = 9.5.sp,
                                color = VitalOnTealContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                              )
                            }
                          }
                          sym.impact.forEach { imp ->
                            Surface(
                              color = SeverityTerracotta.copy(alpha = 0.1f),
                              shape = RoundedCornerShape(4.dp)
                            ) {
                              Text(
                                text = "Impact: $imp",
                                fontSize = 9.5.sp,
                                color = SeverityTerracotta,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                              )
                            }
                          }
                        }
                      }

                      if (sym.notes.isNotBlank()) {
                        Surface(
                          color = MaterialTheme.colorScheme.surface,
                          shape = RoundedCornerShape(6.dp),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Text(
                            text = "“${sym.notes}”",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(6.dp),
                            lineHeight = 14.sp
                          )
                        }
                      }

                      Text(
                        text = "FHIR: Observation?code=http://snomed.info/sct|${sym.snomedCode}&value=${sym.severity}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                      )
                    }
                  }
                }
              }
            }
          }

          // Section 3: Longitudinal Recurrence & Patterns
          if (uniqueSymptomNames.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(16.dp))
                Text(
                  text = "LONGITUDINAL PATTERN OBSERVATIONS",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              uniqueSymptomNames.forEach { sName ->
                val stats = repository.calculateSymptomRecurrence(profile.id, sName)
                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(sName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                      Text("${stats.totalEpisodesThisMonth} episode(s) this month", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = VitalTealPrimary)
                    }
                    Text(
                      text = "Avg Severity: ${"%.1f".format(stats.averageSeverity)}/10 • Peak Severity: ${stats.peakSeverity}/10",
                      fontSize = 10.5.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (stats.coOccurrenceObservation != null) {
                      Text(
                        text = stats.coOccurrenceObservation,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                      )
                    }
                  }
                }
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

          // Security seal & FHIR compliance footer
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(14.dp))
              Text(
                text = "Vitalith Health Enclave • SMART-on-FHIR R4",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            TextButton(
              onClick = { showRawExport = !showRawExport },
              contentPadding = PaddingValues(0.dp)
            ) {
              Text(
                text = if (showRawExport) "Hide Raw Text" else "View Raw Text",
                fontSize = 10.5.sp,
                color = VitalTealPrimary
              )
            }
          }

          // Optional Monospace Raw Text view when toggled
          if (showRawExport) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = exportText,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 14.sp,
                modifier = Modifier.padding(10.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun EmergencyRedFlagDialog(
  alert: RedFlagAlert,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Emergency,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = alert.title,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = alert.message,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Vitalith is an observational tracking tool, not a diagnostic engine or emergency responder.",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${alert.emergencyPhone}")
          }
          context.startActivity(dialIntent)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("emergency_dial_button")
      ) {
        Text(alert.actionLabel, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("emergency_dismiss_button")
      ) {
        Text("I Understand / Dismiss")
      }
    }
  )
}

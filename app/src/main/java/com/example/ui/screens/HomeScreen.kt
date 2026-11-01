package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.*
import com.example.data.repository.VitalithRepository
import kotlinx.coroutines.delay
import com.example.ui.components.ClinicalAlertBanner
import com.example.ui.components.GeminiHealthSummaryCard
import com.example.ui.components.GlassHeroBanner
import com.example.ui.components.MonthlyHealthTrendsDashboard
import com.example.ui.components.PatientProfileDropdownSelector
import com.example.ui.components.ProvenanceBadge
import com.example.ui.components.QuickActionDock
import com.example.ui.components.RangeBar
import com.example.ui.components.Sparkline
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  repository: VitalithRepository,
  onNavigateToTimeline: () -> Unit,
  onNavigateToAi: () -> Unit,
  onNavigateToMeds: () -> Unit,
  onNavigateToSharing: () -> Unit,
  onNavigateToEmergency: () -> Unit,
  onNavigateToPrep: () -> Unit,
  onNavigateToIngest: () -> Unit,
  onNavigateToPrivacy: () -> Unit,
  onNavigateToProfile: () -> Unit = {},
  onNavigateToSettings: () -> Unit = {},
  onNavigateToAddRecord: () -> Unit = {},
  onNavigateToSubscription: () -> Unit = {},
  onNavigateToSymptoms: () -> Unit = {},
  onNavigateToProviderSearch: () -> Unit = {},
  onNavigateToWearables: () -> Unit = {},
  onLogOut: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val profiles by repository.profiles.collectAsState()
  val userProfile by repository.userProfile.collectAsState()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val timelineItems by repository.timelineItems.collectAsState()
  val medications by repository.medications.collectAsState()
  val symptoms by repository.symptoms.collectAsState()
  val subscriptionTier by repository.subscriptionTier.collectAsState()
  val subscriptionDetails by repository.subscriptionDetails.collectAsState()
  val wearableActivities by repository.wearableActivities.collectAsState()
  val wearableHeartRates by repository.wearableHeartRates.collectAsState()
  val wearableDevices by repository.wearableDevices.collectAsState()

  val currentWearableActivity = wearableActivities[currentProfileId] ?: repository.wearableService.getDailyActivityData(currentProfileId)
  val currentWearableHeartRate = wearableHeartRates[currentProfileId] ?: repository.wearableService.getHeartRateData(currentProfileId)
  val connectedWearableDevice = wearableDevices.firstOrNull { it.isConnected } ?: wearableDevices.firstOrNull()

  val currentProfile = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.first()
  val filteredTimelineItems = remember(timelineItems, currentProfileId) {
    timelineItems.filter { it.profileId == currentProfileId }
  }
  val filteredMedications = remember(medications, currentProfileId) {
    medications.filter { it.profileId == currentProfileId }
  }
  val profileSymptoms = remember(symptoms, currentProfileId) {
    symptoms.filter { it.profileId == currentProfileId }
  }
  val ongoingSymptoms = remember(profileSymptoms) {
    profileSymptoms.filter { it.isOngoing }
  }
  val currentPrep = remember(userProfile.unitPreferences, currentProfileId) {
    repository.getAppointmentPrep(userProfile.unitPreferences)
  }
  var showUserDropdown by remember { mutableStateOf(false) }
  var showLogOutConfirm by remember { mutableStateOf(false) }
  var isSpeedDialOpen by remember { mutableStateOf(false) }
  var showQuickLogSymptom by remember { mutableStateOf(false) }
  var showQuickLogMedication by remember { mutableStateOf(false) }
  var activeRedFlagAlert by remember { mutableStateOf<RedFlagAlert?>(null) }
  var quickLogFeedbackMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(quickLogFeedbackMessage) {
    if (quickLogFeedbackMessage != null) {
      delay(3500)
      quickLogFeedbackMessage = null
    }
  }

  val activeThemeKey by ThemeManager.currentThemeKey.collectAsState()
  val themeTokens = LocalVitalithThemeTokens.current

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 112.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
    // Header & Profile Switcher
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Vitalith",
              fontSize = 22.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary
            )
            Surface(
              color = VitalTealContainer,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = "SynthetiQs",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VitalOnTealContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Text(
            text = "Your health history, in your hands.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Profile Selector & Account Actions
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          PatientProfileDropdownSelector(
            profiles = profiles,
            currentProfileId = currentProfileId,
            onSwitchProfile = { repository.switchProfile(it) },
            onNavigateToSubscription = onNavigateToSubscription,
            onNavigateToManageProfiles = onNavigateToProfile,
            testTagPrefix = "profile_selector"
          )

          IconButton(
            onClick = { showUserDropdown = true },
            modifier = Modifier.testTag("header_profile_button")
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(VitalTealContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "User Account & Profiles",
                tint = VitalTealPrimary,
                modifier = Modifier.size(26.dp)
              )
            }
          }
        }
      }
    }

    // Clinical Disclaimer Banner
    item {
      ClinicalAlertBanner()
    }

    // Glassmorphic Hero Banner (Replacing Emergency Banner)
    item {
      GlassHeroBanner(
        profile = currentProfile,
        onOpenEmergency = onNavigateToEmergency
      )
    }

    // Horizontal Quick-Action Pill Dock (Replacing 2x2 Grid)
    item {
      QuickActionDock(
        onConnectFhir = onNavigateToIngest,
        onUploadOcr = onNavigateToIngest,
        onAskAi = onNavigateToAi,
        onShareRecord = onNavigateToSharing
      )
    }

    // Wearable & Daily Activity Health Snapshot (Google Fit API)
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToWearables() }
          .testTag("home_wearable_snapshot_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(VitalTealContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.WatchLater,
                  contentDescription = null,
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(18.dp)
                )
              }
              Column {
                Text(
                  text = "Connected Wearable Telemetry",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "${connectedWearableDevice?.name ?: "Pixel Watch / Fitbit"} • Google Fit API",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = VitalEmeraldContainer
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Sync,
                  contentDescription = null,
                  tint = VitalEmeraldSuccess,
                  modifier = Modifier.size(11.dp)
                )
                Text(
                  text = "LIVE SYNC",
                  color = VitalEmeraldSuccess,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 3 Metric Blocks: Steps, Resting HR, Active Mins
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Steps Block
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(text = "Daily Steps", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${currentWearableActivity.stepsCount}",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                  progress = { currentWearableActivity.stepProgressPercent.coerceIn(0f, 1f) },
                  color = VitalTealPrimary,
                  trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                  modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${(currentWearableActivity.stepProgressPercent * 100).toInt()}% of ${currentWearableActivity.stepGoal}",
                  fontSize = 9.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Resting HR Block
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(text = "Resting HR", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "${currentWearableHeartRate.restingHeartRateBpm}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(text = "bpm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = VitalEmeraldContainer
                ) {
                  Text(
                    text = "Optimal",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = VitalEmeraldSuccess,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }
            }

            // Active Minutes / Energy Block
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(text = "Active Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${currentWearableActivity.activeMinutes}m",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${currentWearableActivity.caloriesBurnedKcal} kcal",
                  fontSize = 9.5.sp,
                  color = VitalWarningOrange
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Open Wearables & Google Fit Hub",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = VitalTealPrimary
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = VitalTealPrimary,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }

    // Gemini AI Natural Language Health Record Summary & Anomaly Radar
    item {
      GeminiHealthSummaryCard(
        timelineItems = filteredTimelineItems,
        patientName = currentProfile.name,
        unitPreferences = userProfile.unitPreferences
      )
    }

    // Appointment Prep Card (Layer B / Coordinator)
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToPrep() }
          .testTag("appointment_prep_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(VitalTealContainerDark.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.EventNote,
                  contentDescription = null,
                  tint = VitalTealPrimaryDark,
                  modifier = Modifier.size(18.dp)
                )
              }
              Text(
                text = "Upcoming Appointment Prep",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Surface(
              color = VitalEmeraldContainer,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "Ready",
                color = VitalEmeraldSuccess,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "${currentPrep.doctorName} • ${currentPrep.clinic} (${currentPrep.scheduledDate.substringBefore(" •")})",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Auto-generated 1-page visit briefing for ${currentProfile.name}: ${currentPrep.visitReason}. ${currentPrep.recentChanges.firstOrNull() ?: ""}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Open Visit Briefing",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = VitalTealPrimary
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = VitalTealPrimary,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }

    // Find Nearby Healthcare Providers & Specialists (Google Search)
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToProviderSearch() }
          .testTag("home_provider_search_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF4285F4).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.LocalHospital,
                  contentDescription = null,
                  tint = Color(0xFF1A73E8),
                  modifier = Modifier.size(18.dp)
                )
              }
              Text(
                text = "Find Nearby Specialists",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Surface(
              color = Color(0xFF4285F4).copy(alpha = 0.15f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = null,
                  tint = Color(0xFF1A73E8),
                  modifier = Modifier.size(11.dp)
                )
                Text(
                  text = "Google Search",
                  color = Color(0xFF1A73E8),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Search accredited doctors, clinics, and diagnostic labs based on ${currentProfile.name}'s active clinical profile.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 17.sp
          )

          Spacer(modifier = Modifier.height(10.dp))

          val homePills = when (currentProfile.id) {
            "profile_maya" -> listOf("Pediatricians", "Peanut Allergy Clinic", "Pediatric Urgent Care")
            "profile_kwame" -> listOf("Interventional Cardiology", "Cardiac Rehab", "Geriatric Care")
            else -> listOf("Endocrinologists", "Primary Care / Internal Med", "Quest / LabCorp")
          }
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            homePills.forEach { pill ->
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = pill,
                  fontSize = 10.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Search Nearby Providers",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = VitalTealPrimary
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = VitalTealPrimary,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }

    // Symptoms & Wellness Tracking Card
    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToSymptoms() }
          .testTag("home_symptoms_tracking_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(VitalTealContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.MonitorHeart,
                  contentDescription = "Symptoms",
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Column {
                Text(
                  text = "Symptoms & Wellness Tracking",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${currentProfile.name} • Longitudinal Wellness",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            if (ongoingSymptoms.isNotEmpty()) {
              Surface(
                color = Color(0xFFC67D00).copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "${ongoingSymptoms.size} Active",
                  color = Color(0xFFC67D00),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            } else {
              Surface(
                color = Color(0xFF4A7C59).copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "Stable / Logged",
                  color = Color(0xFF4A7C59),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (profileSymptoms.isNotEmpty()) {
            val latest = profileSymptoms.first()
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Latest: ${latest.symptomName} (${latest.severity}/10 ${latest.severityLabel})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = latest.onsetDate,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            Text(
              text = "No symptoms recorded. Track recurring pain, reflux, or fatigue in 10 seconds.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "View Longitudinal Strip",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VitalTealPrimary
              )
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = VitalTealPrimary,
                modifier = Modifier.size(14.dp)
              )
            }

            Surface(
              color = VitalTealPrimary.copy(alpha = 0.1f),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.clickable { onNavigateToSymptoms() }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(12.dp))
                Text("+ Quick Log", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VitalTealPrimary)
              }
            }
          }
        }
      }
    }

    // Interactive Monthly Health Trends Summary Dashboard (Recharts-style)
    item {
      MonthlyHealthTrendsDashboard(
        profileId = currentProfileId,
        unitPreferences = userProfile.unitPreferences,
        onLogReadingClick = onNavigateToAddRecord
      )
    }

    // Health Trends Spotlight with Bezier Sparklines + Range Bars
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Longitudinal Biomarker Trends",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        when (currentProfileId) {
          "profile_maya" -> {
            // Pediatric Growth & Lead Surveillance
            val weightDisplay = if (userProfile.unitPreferences.weightUnit == com.example.data.model.WeightUnit.LBS) "51.8 lbs" else "23.5 kg"
            val weightUnitLabel = userProfile.unitPreferences.weightUnit.label
            val weightNumeric = if (userProfile.unitPreferences.weightUnit == com.example.data.model.WeightUnit.LBS) 51.8f else 23.5f

            BiomarkerDetailCard(
              title = "Pediatric Body Weight",
              currentValue = weightDisplay,
              changeText = "Tracking 65th Percentile Curve",
              isPositive = true,
              date = "Oct 14, 2026 • Metro Pediatric Clinic",
              sparklinePoints = listOf(22.8f, 23.0f, 23.2f, 23.4f, 23.5f),
              lineColor = themeTokens.spark1,
              numericValue = weightNumeric,
              minVal = if (userProfile.unitPreferences.weightUnit == com.example.data.model.WeightUnit.LBS) 35f else 16f,
              maxVal = if (userProfile.unitPreferences.weightUnit == com.example.data.model.WeightUnit.LBS) 70f else 32f,
              lowNormal = if (userProfile.unitPreferences.weightUnit == com.example.data.model.WeightUnit.LBS) 44f else 20f,
              highNormal = if (userProfile.unitPreferences.weightUnit == com.example.data.model.WeightUnit.LBS) 62f else 28f,
              unit = weightUnitLabel
            )

            BiomarkerDetailCard(
              title = "Capillary Blood Lead Level",
              currentValue = "< 1.0 ug/dL",
              changeText = "Optimal Zero-Risk Tier",
              isPositive = true,
              date = "Aug 20, 2026 • Children's Health Center",
              sparklinePoints = listOf(1.2f, 1.1f, 1.0f, 0.9f, 0.8f),
              lineColor = themeTokens.spark2,
              numericValue = 0.8f,
              minVal = 0.0f,
              maxVal = 10.0f,
              lowNormal = 0.0f,
              highNormal = 3.5f,
              unit = "ug/dL"
            )
          }
          "profile_kwame" -> {
            // Cardiac & Renal Biomarker Cards
            BiomarkerDetailCard(
              title = "eGFR (Kidney Function)",
              currentValue = "52 mL/min",
              changeText = "CKD Stage 3a Monitored",
              isPositive = false,
              date = "Oct 14, 2026 • MetroHealth Nephrology",
              sparklinePoints = listOf(48f, 49f, 50f, 51f, 52f),
              lineColor = themeTokens.spark1,
              numericValue = 52f,
              minVal = 15f,
              maxVal = 100f,
              lowNormal = 60f,
              highNormal = 100f,
              unit = "mL/min"
            )

            BiomarkerDetailCard(
              title = "High-Sensitivity Troponin I",
              currentValue = "< 0.01 ng/mL",
              changeText = "Negative for Myocardial Injury",
              isPositive = true,
              date = "Sep 15, 2026 • Cardiac Care Unit",
              sparklinePoints = listOf(0.04f, 0.02f, 0.01f, 0.01f, 0.01f),
              lineColor = themeTokens.spark2,
              numericValue = 0.01f,
              minVal = 0.0f,
              maxVal = 0.10f,
              lowNormal = 0.0f,
              highNormal = 0.04f,
              unit = "ng/mL"
            )
          }
          else -> {
            // Doug / Adult Primary Profile
            BiomarkerDetailCard(
              title = "Hemoglobin A1c",
              currentValue = "6.4%",
              changeText = "-0.4% from Jul 2026",
              isPositive = true,
              date = "Oct 14, 2026 • Quest Diagnostics",
              sparklinePoints = listOf(7.2f, 7.0f, 6.8f, 6.6f, 6.4f),
              lineColor = themeTokens.spark1,
              numericValue = 6.4f,
              minVal = 4.0f,
              maxVal = 9.0f,
              lowNormal = 4.0f,
              highNormal = 5.6f,
              unit = "%"
            )

            BiomarkerDetailCard(
              title = "eGFR (Kidney Function)",
              currentValue = "98 mL/min",
              changeText = "Stable Filtration Rate",
              isPositive = true,
              date = "Aug 15, 2026 • MetroHealth",
              sparklinePoints = listOf(92f, 95f, 94f, 96f, 98f),
              lineColor = themeTokens.spark2,
              numericValue = 98f,
              minVal = 30f,
              maxVal = 120f,
              lowNormal = 60f,
              highNormal = 120f,
              unit = "mL/min"
            )
          }
        }
      }
    }

    // Recent Timeline Records Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Longitudinal Timeline",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onNavigateToTimeline) {
          Text("View All (${filteredTimelineItems.size})", color = VitalTealPrimary, fontSize = 12.sp)
        }
      }
    }

    items(filteredTimelineItems.take(3)) { item ->
      TimelineSummaryCard(item = item, onClick = onNavigateToTimeline)
    }

    // Patient Rights & Privacy Footer Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToPrivacy() }
          .testTag("privacy_audit_footer")
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Security",
            tint = VitalEmeraldSuccess,
            modifier = Modifier.size(20.dp)
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Patient-Owned • Zero Data Sale",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "AES-256 encrypted • Granular consent • Download FHIR R4",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }

  // Log Out Confirmation Dialog
  if (showLogOutConfirm) {
    AlertDialog(
      onDismissRequest = { showLogOutConfirm = false },
      icon = {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = null,
          tint = VitalAlertRed,
          modifier = Modifier.size(28.dp)
        )
      },
      title = {
        Text(
          text = "Sign Out of Vitalith?",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "You are signing out of your account (${userProfile.email}).",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "All patient records and clinical provenance remain encrypted in the local SQLite vault protected by your hardware keystore.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showLogOutConfirm = false
            onLogOut()
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalAlertRed),
          modifier = Modifier.testTag("confirm_logout_button")
        ) {
          Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showLogOutConfirm = false },
          modifier = Modifier.testTag("cancel_logout_button")
        ) {
          Text("Cancel")
        }
      },
      modifier = Modifier.testTag("logout_confirmation_dialog")
    )
  }

  // =========================================================================
  // Full-Height User Menu Drawer (occupies 100% full height of device screen)
  // =========================================================================
  AnimatedVisibility(
    visible = showUserDropdown,
    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(180)) + slideInHorizontally(
      initialOffsetX = { fullWidth -> fullWidth },
      animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
    ),
    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) + slideOutHorizontally(
      targetOffsetX = { fullWidth -> fullWidth },
      animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
    ),
    modifier = Modifier.zIndex(100f)
  ) {
    BackHandler(enabled = showUserDropdown) { showUserDropdown = false }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.55f))
        .clickable { showUserDropdown = false }
    ) {
      Surface(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(0.88f)
          .widthIn(min = 320.dp, max = 420.dp)
          .align(Alignment.CenterEnd)
          .clickable(enabled = false) {} // Prevent backdrop dismiss when tapping inside drawer
          .testTag("user_dropdown_menu"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 24.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxHeight()
            .navigationBarsPadding()
        ) {
          // Top Header Bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(VitalTealContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = null,
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(18.dp)
                )
              }
              Column {
                Text(
                  text = "Account & Profiles",
                  fontWeight = FontWeight.Bold,
                  fontSize = 17.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Cryptographic Security Enclave",
                  fontSize = 11.sp,
                  color = Color(0xFF94A3B8)
                )
              }
            }
            IconButton(
              onClick = { showUserDropdown = false },
              modifier = Modifier
                .size(40.dp)
                .testTag("user_menu_close_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Menu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

          // Scrollable Drawer Content
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            // User Identity Header Card
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(52.dp)
                      .clip(CircleShape)
                      .background(VitalTealPrimary),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = userProfile.fullName.take(1).ifEmpty { "U" },
                      color = Color.White,
                      fontSize = 22.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = userProfile.fullName,
                      fontSize = 16.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = userProfile.email,
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = userProfile.phone,
                      fontSize = 11.5.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                  }
                }

                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    color = VitalTealContainer,
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Text(
                      text = "Account Owner",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = VitalOnTealContainer,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                  }
                  Surface(
                    color = VitalEmeraldContainer,
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                      Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(VitalEmeraldSuccess))
                      Text(
                        text = "Hardware Enclave Active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = VitalEmeraldSuccess
                      )
                    }
                  }
                }
              }
            }

            // Membership & Subscription
            Surface(
              onClick = {
                showUserDropdown = false
                onNavigateToSubscription()
              },
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surface,
              border = androidx.compose.foundation.BorderStroke(1.5.dp, VitalTealPrimary.copy(alpha = 0.45f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("user_menu_subscription_entry")
            ) {
              Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VitalTealContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = VitalTealPrimary,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                  ) {
                    Text(
                      text = "MEMBERSHIP & PLAN",
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 1.sp,
                      color = VitalTealPrimary,
                      modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    val isSubActive = subscriptionDetails.status == "ACTIVE" || subscriptionDetails.autoRenew
                    Surface(
                      color = if (isSubActive) VitalEmeraldContainer else VitalAlertContainer,
                      shape = RoundedCornerShape(4.dp),
                      modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                      Text(
                        text = if (isSubActive) "ACTIVE" else "CANCELED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSubActive) VitalOnEmeraldContainer else VitalAlertRed,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = subscriptionTier.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Manage tiers, feature access & invoices",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Icon(
                  imageVector = Icons.Default.ChevronRight,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            // Manage Profiles Action
            Surface(
              onClick = {
                showUserDropdown = false
                onNavigateToProfile()
              },
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surface,
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("user_menu_profiles_account")
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VitalTealContainer.copy(alpha = 0.5f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.ManageAccounts,
                    contentDescription = null,
                    tint = VitalTealPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Manage Profiles & Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = VitalTealPrimary
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "Add family members, edit MRN, notes & personal info",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Icon(
                  imageVector = Icons.Default.ChevronRight,
                  contentDescription = null,
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            HorizontalDivider(
              modifier = Modifier.padding(top = 10.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )

            // Section 2: Security & Privacy Governance
            Column(
              modifier = Modifier.padding(top = 16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                text = "SECURITY & COMPLIANCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
              )

              // Privacy, Security & Audit
              Surface(
                onClick = {
                  showUserDropdown = false
                  onNavigateToPrivacy()
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_privacy_audit")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(VitalTealContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Security,
                      contentDescription = null,
                      tint = VitalTealPrimary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Privacy, Security & Audit",
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Zero-knowledge keys, audit logs & FHIR export",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }

              // Emergency Medical ID
              Surface(
                onClick = {
                  showUserDropdown = false
                  onNavigateToEmergency()
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_emergency_id")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(VitalTealContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.HealthAndSafety,
                      contentDescription = null,
                      tint = VitalTealPrimary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Emergency Medical ID",
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Lockscreen snapshot, blood type & contacts",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }

              // Find Care & Specialists (Google Search)
              Surface(
                onClick = {
                  showUserDropdown = false
                  onNavigateToProviderSearch()
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_provider_search")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(Color(0xFF4285F4).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.LocalHospital,
                      contentDescription = null,
                      tint = Color(0xFF1A73E8),
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                      Text(
                        text = "Find Care & Specialists",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                      )
                      Surface(
                        color = Color(0xFF4285F4).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                      ) {
                        Text(
                          text = "Google",
                          color = Color(0xFF1A73E8),
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Nearby doctors, clinics & diagnostic labs",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }

            HorizontalDivider(
              modifier = Modifier.padding(top = 10.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )

            // Section 3: Preferences & Enclave
            Column(
              modifier = Modifier.padding(top = 16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                text = "PREFERENCES & ENCLAVE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
              )

              // Settings Row
              Surface(
                onClick = {
                  showUserDropdown = false
                  onNavigateToSettings()
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_settings")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(VitalTealContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Settings,
                      contentDescription = null,
                      tint = VitalTealPrimary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Settings",
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Biometrics, security & app preferences",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }

              // Connected Wearables (Google Fit) Row
              Surface(
                onClick = {
                  showUserDropdown = false
                  onNavigateToWearables()
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_wearables")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(VitalTealContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.WatchLater,
                      contentDescription = null,
                      tint = VitalTealPrimary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Text(
                        text = "Connected Wearables",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                      )
                      Surface(
                        color = VitalTealContainer,
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text(
                          text = "Google Fit",
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          color = VitalTealPrimary,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                      }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Pixel Watch, Fitbit, Galaxy Watch & Garmin",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }

              // Inline Biometric Vault Lock Toggle Card
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VitalTealContainer.copy(alpha = 0.4f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = VitalTealPrimary,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                    Column {
                      Text(
                        text = "Biometric Vault Lock",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = "Require fingerprint or biometric scan",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                  Switch(
                    checked = userProfile.biometricLockEnabled,
                    onCheckedChange = { repository.setBiometricLock(it) },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.White,
                      checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("drawer_biometric_toggle")
                  )
                }
              }
            }

            HorizontalDivider(
              modifier = Modifier.padding(top = 10.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )

            // Section 3.5: Appearance & Theme (Collapsible)
            var isDrawerThemesExpanded by remember { mutableStateOf(false) }

            Column(
              modifier = Modifier
                .padding(top = 14.dp)
                .testTag("user_menu_appearance_section"),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Surface(
                onClick = { isDrawerThemesExpanded = !isDrawerThemesExpanded },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_theme_collapse_toggle")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(VitalTealContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Palette,
                      contentDescription = null,
                      tint = VitalTealPrimary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Appearance & Themes",
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val currentDesc = ALL_THEMES.firstOrNull { it.key == activeThemeKey } ?: ALL_THEMES.first()
                    Text(
                      text = "${currentDesc.label} • 9 themes available",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Icon(
                    imageVector = if (isDrawerThemesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isDrawerThemesExpanded) "Collapse Theme List" else "Expand Theme List",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }

              AnimatedVisibility(
                visible = isDrawerThemesExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_menu_themes_radiogroup"),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Text(
                    text = "Select from 9 medical palettes. Changes apply instantly across the entire application and persist in local storage.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                  )
                  ALL_THEMES.forEach { themeDesc ->
                    val isThemeSelected = activeThemeKey == themeDesc.key
                    Surface(
                      onClick = {
                        ThemeManager.setTheme(themeDesc.key)
                      },
                      shape = RoundedCornerShape(12.dp),
                      color = if (isThemeSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                      border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isThemeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                      ),
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_option_${themeDesc.key}")
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                      ) {
                        // Color Swatch Circle with accent & bg
                        Box(
                          modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(themeDesc.swatchBg)
                            .border(
                              width = 1.5.dp,
                              color = themeDesc.swatchAccent.copy(alpha = 0.8f),
                              shape = CircleShape
                            ),
                          contentAlignment = Alignment.Center
                        ) {
                          Box(
                            modifier = Modifier
                              .size(10.dp)
                              .clip(CircleShape)
                              .background(themeDesc.swatchAccent)
                          )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                          ) {
                            Text(
                              text = themeDesc.label,
                              fontWeight = if (isThemeSelected) FontWeight.Bold else FontWeight.SemiBold,
                              fontSize = 13.sp,
                              color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                              color = if (themeDesc.isDark) Color(0x33000000) else Color(0x22000000),
                              shape = RoundedCornerShape(4.dp)
                            ) {
                              Text(
                                text = if (themeDesc.key == "contrast") "AAA" else if (themeDesc.isDark) "Dark" else "Light",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                              )
                            }
                          }
                          Text(
                            text = when (themeDesc.key) {
                              "original" -> "Standard clinical obsidian navy & cyan"
                              "plum" -> "Deep plum with rose accent & sage ranges"
                              "forest" -> "Dark pine green with emerald telemetry"
                              "mono" -> "Neutral minimalist slate & pure white"
                              "indigo" -> "Deep royal indigo & vibrant violet"
                              "sandstone" -> "Warm earth tone with clay & amber"
                              "arctic" -> "Crisp high-readability daylight ice"
                              "rose" -> "Soft pastel daytime clinical aesthetic"
                              "contrast" -> "WCAG AAA black background & luminous yellow"
                              else -> "Design token palette"
                            },
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                          )
                        }

                        if (isThemeSelected) {
                          Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(20.dp)
                          ) {
                            Box(contentAlignment = Alignment.Center) {
                              Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
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

            HorizontalDivider(
              modifier = Modifier.padding(top = 10.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )

            // Section 4: Log Out & Session Management
            Column(
              modifier = Modifier.padding(top = 16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                text = "SESSION & VAULT ACCESS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
              )

              Surface(
                onClick = {
                  showUserDropdown = false
                  showLogOutConfirm = true
                },
                shape = RoundedCornerShape(14.dp),
                color = VitalAlertRed.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, VitalAlertRed.copy(alpha = 0.30f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("user_menu_logout")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(VitalAlertRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Logout,
                      contentDescription = null,
                      tint = VitalAlertRed,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Log Out",
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = VitalAlertRed
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Sign out and securely lock your data",
                      fontSize = 11.sp,
                      color = VitalAlertRed.copy(alpha = 0.85f)
                    )
                  }
                  Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = VitalAlertRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(32.dp))
          }
        }
      }
    }
  }

  // Handle hardware back button to dismiss Speed-Dial if open
  BackHandler(enabled = isSpeedDialOpen) {
    isSpeedDialOpen = false
  }

  // Speed-Dial Scrim Overlay (dismisses on background tap)
  AnimatedVisibility(
    visible = isSpeedDialOpen,
    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(150)),
    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(150)),
    modifier = Modifier.zIndex(40f)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.45f))
        .clickable { isSpeedDialOpen = false }
    )
  }

  // Floating Action Button with Speed-Dial Quick-Log Actions
  Column(
    modifier = Modifier
      .align(Alignment.BottomEnd)
      .padding(end = 18.dp, bottom = 18.dp)
      .zIndex(50f),
    horizontalAlignment = Alignment.End,
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    AnimatedVisibility(
      visible = isSpeedDialOpen,
      enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(180)) + slideInVertically(
        initialOffsetY = { 40 },
        animationSpec = androidx.compose.animation.core.spring(
          dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
          stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        )
      ),
      exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) + slideOutVertically(
        targetOffsetY = { 40 }
      )
    ) {
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Action 1: Log Symptom
        Surface(
          onClick = {
            isSpeedDialOpen = false
            showQuickLogSymptom = true
          },
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 8.dp,
          shadowElevation = 10.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)),
          modifier = Modifier.testTag("fab_quick_log_symptom")
        ) {
          Row(
            modifier = Modifier.padding(start = 14.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(VitalWarningOrange.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.MonitorHeart,
                contentDescription = null,
                tint = VitalWarningOrange,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(
                text = "Log Symptom",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Severity, triggers & body site",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Action 2: Log Medication
        Surface(
          onClick = {
            isSpeedDialOpen = false
            showQuickLogMedication = true
          },
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 8.dp,
          shadowElevation = 10.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)),
          modifier = Modifier.testTag("fab_quick_log_medication")
        ) {
          Row(
            modifier = Modifier.padding(start = 14.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(VitalTealPrimary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = null,
                tint = VitalTealPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(
                text = "Log Medication",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Dosage, frequency & indication",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    // Primary FAB
    FloatingActionButton(
      onClick = { isSpeedDialOpen = !isSpeedDialOpen },
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      shape = CircleShape,
      elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 10.dp),
      modifier = Modifier
        .size(56.dp)
        .testTag("home_quick_log_fab")
    ) {
      val rotation by animateFloatAsState(
        targetValue = if (isSpeedDialOpen) 135f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
          dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
          stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "fab_rotation"
      )
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = if (isSpeedDialOpen) "Close Quick Log Menu" else "Quick Log Symptom or Medication",
        modifier = Modifier
          .size(26.dp)
          .rotate(rotation)
      )
    }
  }

  // Floating Quick-Log Feedback Banner
  AnimatedVisibility(
    visible = quickLogFeedbackMessage != null,
    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) + slideInVertically(initialOffsetY = { 60 }),
    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) + slideOutVertically(targetOffsetY = { 60 }),
    modifier = Modifier
      .align(Alignment.BottomCenter)
      .padding(bottom = 20.dp, start = 20.dp, end = 20.dp)
      .zIndex(60f)
  ) {
    quickLogFeedbackMessage?.let { msg ->
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier = Modifier.testTag("quick_log_success_feedback")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = VitalEmeraldSuccess,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = msg,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.inverseOnSurface
          )
        }
      }
    }
  }

  // In-Place Quick Log Symptom Dialog
  if (showQuickLogSymptom) {
    QuickAddSymptomDialog(
      profileId = currentProfileId,
      repository = repository,
      onDismiss = { showQuickLogSymptom = false },
      onSymptomLogged = { item, alert ->
        showQuickLogSymptom = false
        activeRedFlagAlert = alert
        quickLogFeedbackMessage = "Logged ${item.symptomName} (${item.severityLabel}, ${item.severity}/10)"
      }
    )
  }

  // In-Place Quick Log Medication Dialog
  if (showQuickLogMedication) {
    AddMedicationDialog(
      profileId = currentProfileId,
      onDismiss = { showQuickLogMedication = false },
      onSave = { med ->
        repository.addMedication(med)
        showQuickLogMedication = false
        quickLogFeedbackMessage = "Added ${med.name} (${med.dosage})"
      }
    )
  }

  // Emergency Red Flag Dialog if symptom triggers clinical alert
  activeRedFlagAlert?.let { alert ->
    EmergencyRedFlagDialog(
      alert = alert,
      onDismiss = { activeRedFlagAlert = null }
    )
  }
}
}




@Composable
fun ActionCard(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .testTag("action_${title.lowercase().replace(" ", "_")}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
      Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun BiomarkerDetailCard(
  title: String,
  currentValue: String,
  changeText: String,
  isPositive: Boolean,
  date: String,
  sparklinePoints: List<Float>,
  lineColor: Color,
  numericValue: Float,
  minVal: Float,
  maxVal: Float,
  lowNormal: Float,
  highNormal: Float,
  unit: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(18.dp),
    shadowElevation = 2.dp,
    tonalElevation = 2.dp,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column {
          Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = currentValue,
            style = MetricDisplayLarge,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Surface(
          color = if (isPositive) VitalEmeraldContainer else VitalAlertContainer,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = changeText,
            color = if (isPositive) VitalEmeraldSuccess else VitalAlertRed,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Sparkline Canvas with Bezier curve & gradient fill
      Sparkline(
        points = sparklinePoints,
        lineColor = lineColor,
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Clinical 3-zone range bar with glowing indicator dot
      RangeBar(
        currentValue = numericValue,
        minVal = minVal,
        maxVal = maxVal,
        lowNormal = lowNormal,
        highNormal = highNormal,
        unit = unit
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = date,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )
    }
  }
}

@Composable
fun TrendTile(
  label: String,
  value: String,
  change: String,
  isPositive: Boolean,
  date: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    shape = RoundedCornerShape(8.dp),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = change,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isPositive) VitalEmeraldSuccess else VitalAlertRed
      )
      Text(text = date, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun TimelineSummaryCard(
  item: HealthTimelineItem,
  onClick: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          CategoryChip(category = item.category)
          Text(text = item.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusBadge(status = item.status)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(text = item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
      Text(text = item.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(8.dp))
      ProvenanceBadge(provenance = item.provenance)
    }
  }
}

@Composable
fun CategoryChip(category: TimelineCategory) {
  Surface(
    color = when (category) {
      TimelineCategory.LAB -> VitalTealContainer
      TimelineCategory.MEDICATION -> VitalGoldContainer
      TimelineCategory.CONDITION -> VitalAlertContainer
      TimelineCategory.ALLERGY -> VitalAlertContainer
      TimelineCategory.ENCOUNTER -> Color(0xFFE0E7FF)
      TimelineCategory.IMMUNIZATION -> VitalEmeraldContainer
      else -> Color(0xFFF1F5F9)
    },
    shape = RoundedCornerShape(4.dp)
  ) {
    Text(
      text = category.name,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      color = when (category) {
        TimelineCategory.LAB -> VitalOnTealContainer
        TimelineCategory.MEDICATION -> VitalGoldAccent
        TimelineCategory.CONDITION -> VitalOnAlertContainer
        TimelineCategory.ALLERGY -> VitalOnAlertContainer
        TimelineCategory.ENCOUNTER -> Color(0xFF3730A3)
        TimelineCategory.IMMUNIZATION -> VitalOnEmeraldContainer
        else -> Color(0xFF334155)
      },
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}

@Composable
fun StatusBadge(status: ItemStatus) {
  Surface(
    color = when (status) {
      ItemStatus.NORMAL, ItemStatus.ACTIVE, ItemStatus.RESOLVED -> VitalEmeraldContainer
      ItemStatus.ELEVATED -> VitalWarningContainer
      ItemStatus.CRITICAL -> VitalAlertContainer
      ItemStatus.DISCONTINUED -> Color(0xFFF1F5F9)
    },
    shape = RoundedCornerShape(4.dp)
  ) {
    Text(
      text = status.name,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      color = when (status) {
        ItemStatus.NORMAL, ItemStatus.ACTIVE, ItemStatus.RESOLVED -> VitalEmeraldSuccess
        ItemStatus.ELEVATED -> VitalWarningOrange
        ItemStatus.CRITICAL -> VitalAlertRed
        ItemStatus.DISCONTINUED -> Color(0xFF64748B)
      },
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}

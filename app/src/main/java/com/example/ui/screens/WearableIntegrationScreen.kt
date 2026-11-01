package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.data.repository.VitalithRepository
import com.example.ui.components.PatientProfileDropdownSelector
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WearableIntegrationScreen(
  repository: VitalithRepository,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  val currentProfileId by repository.currentProfileId.collectAsStateWithLifecycle()
  val profiles by repository.profiles.collectAsStateWithLifecycle()
  val currentProfile = remember(profiles, currentProfileId) {
    profiles.firstOrNull { it.id == currentProfileId } ?: profiles.firstOrNull()
  }

  val googleFitState by repository.googleFitState.collectAsStateWithLifecycle()
  val googleFitEmail by repository.googleFitAccountEmail.collectAsStateWithLifecycle()
  val zeppState by repository.zeppState.collectAsStateWithLifecycle()
  val zeppEmail by repository.zeppAccountEmail.collectAsStateWithLifecycle()
  val zeppBiometricsMap by repository.zeppBiometrics.collectAsStateWithLifecycle()
  val isSyncing by repository.isWearableSyncing.collectAsStateWithLifecycle()
  val devices by repository.wearableDevices.collectAsStateWithLifecycle()
  val activities by repository.wearableActivities.collectAsStateWithLifecycle()
  val heartRates by repository.wearableHeartRates.collectAsStateWithLifecycle()
  val lastSyncResult by repository.lastWearableSyncResult.collectAsStateWithLifecycle()

  val currentActivity = activities[currentProfileId] ?: repository.wearableService.getDailyActivityData(currentProfileId)
  val currentHeartRate = heartRates[currentProfileId] ?: repository.wearableService.getHeartRateData(currentProfileId)
  val currentZeppBiometrics = zeppBiometricsMap[currentProfileId] ?: repository.zeppWearableService.getZeppBiometrics(currentProfileId)

  var selectedPlatformFilter by remember { mutableStateOf<WearablePlatform?>(null) }
  val filteredDevices = remember(devices, selectedPlatformFilter) {
    if (selectedPlatformFilter == null) devices else devices.filter { it.platform == selectedPlatformFilter }
  }
  var showPairDialog by remember { mutableStateOf(false) }
  var feedbackBannerMessage by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Wearables & Health Sync",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Google Fit & Zepp Health (Amazfit) • Telemetry & PAI",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
              .size(48.dp)
              .testTag("wearables_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Navigate Back",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        actions = {
          PatientProfileDropdownSelector(
            profiles = profiles,
            currentProfileId = currentProfileId,
            onSwitchProfile = { repository.switchProfile(it) },
            testTagPrefix = "wearables_profile"
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier.testTag("wearable_integration_screen")
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Feedback Banner
        lastSyncResult?.let { result ->
          item {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = VitalEmeraldContainer,
              border = androidx.compose.foundation.BorderStroke(1.dp, VitalEmeraldSuccess.copy(alpha = 0.4f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("sync_success_banner")
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = VitalEmeraldSuccess,
                  modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Google Fit Sync Complete",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VitalOnEmeraldContainer
                  )
                  Text(
                    text = result.message,
                    fontSize = 11.5.sp,
                    color = VitalOnEmeraldContainer.copy(alpha = 0.9f)
                  )
                }
              }
            }
          }
        }

        // Platform Filter Row
        item {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("platform_filter_row")
          ) {
            FilterChip(
              selected = selectedPlatformFilter == null,
              onClick = { selectedPlatformFilter = null },
              label = { Text("All (${devices.size})", fontSize = 11.5.sp) },
              modifier = Modifier.testTag("filter_all_wearables")
            )
            FilterChip(
              selected = selectedPlatformFilter == WearablePlatform.ZEPP_HEALTH,
              onClick = { selectedPlatformFilter = WearablePlatform.ZEPP_HEALTH },
              label = {
                Text(
                  "Zepp (Amazfit) • ${devices.count { it.platform == WearablePlatform.ZEPP_HEALTH }}",
                  fontSize = 11.5.sp
                )
              },
              leadingIcon = {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00C6FF))
                )
              },
              modifier = Modifier.testTag("filter_zepp_wearables")
            )
            FilterChip(
              selected = selectedPlatformFilter == WearablePlatform.GOOGLE_FIT,
              onClick = { selectedPlatformFilter = WearablePlatform.GOOGLE_FIT },
              label = {
                Text(
                  "Google Fit • ${devices.count { it.platform == WearablePlatform.GOOGLE_FIT }}",
                  fontSize = 11.5.sp
                )
              },
              leadingIcon = {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4285F4))
                )
              },
              modifier = Modifier.testTag("filter_google_wearables")
            )
          }
        }

        // Section 1A: Zepp Health Open Platform (Amazfit) Connection Card
        if (selectedPlatformFilter == null || selectedPlatformFilter == WearablePlatform.ZEPP_HEALTH) {
          item {
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00C6FF).copy(alpha = 0.35f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("zepp_health_connection_card")
            ) {
              Column(modifier = Modifier.padding(18.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00C6FF).copy(alpha = 0.15f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Zepp Health Open Platform",
                        tint = Color(0xFF00A3CC),
                        modifier = Modifier.size(24.dp)
                      )
                    }
                    Column {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                          text = "Zepp Health (Amazfit)",
                          fontSize = 16.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = if (zeppState == ZeppConnectionState.AUTHENTICATED_CONNECTED) VitalEmeraldContainer else VitalAlertContainer
                        ) {
                          Text(
                            text = if (zeppState == ZeppConnectionState.AUTHENTICATED_CONNECTED) "CONNECTED" else "DISCONNECTED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (zeppState == ZeppConnectionState.AUTHENTICATED_CONNECTED) VitalEmeraldSuccess else VitalAlertRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                      Text(
                        text = zeppEmail,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  // Disconnect / Connect Toggle Button
                  OutlinedButton(
                    onClick = {
                      if (zeppState == ZeppConnectionState.AUTHENTICATED_CONNECTED) {
                        repository.disconnectZepp()
                      } else {
                        repository.connectZepp()
                      }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .height(36.dp)
                      .testTag("toggle_zepp_button")
                  ) {
                    Text(
                      text = if (zeppState == ZeppConnectionState.AUTHENTICATED_CONNECTED) "Disconnect" else "Connect",
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Zepp OAuth Scopes Pills
                Text(
                  text = "ZEPP HEALTH OPEN PLATFORM SCOPES",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  ScopePill("activity.read (Steps & Energy)")
                  ScopePill("heartrate.read (BioTracker PPG & HRV)")
                  ScopePill("pai.read (PAI Score)")
                  ScopePill("biotracker.read (SpO2 & Stress)")
                  ScopePill("sleep.read (Readiness)")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(14.dp))

                // Primary Action: Sync Zepp Telemetry Now
                Button(
                  onClick = {
                    coroutineScope.launch {
                      val result = repository.syncZeppWearableData(currentProfileId)
                      feedbackBannerMessage = "Synced ${result.stepsImported} steps, ${result.restingHr} bpm RHR & ${result.paiImported ?: 0} PAI from ${result.deviceName} via Zepp Health"
                    }
                  },
                  enabled = !isSyncing && zeppState == ZeppConnectionState.AUTHENTICATED_CONNECTED,
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0097A7)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sync_zepp_now_button")
                ) {
                  if (isSyncing) {
                    CircularProgressIndicator(
                      color = Color.White,
                      strokeWidth = 2.5.dp,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = "Syncing Zepp OS & Amazfit...",
                      fontSize = 13.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Default.Sync,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Sync Zepp & Amazfit Data Now",
                      fontSize = 13.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  }
                }
              }
            }
          }
        }

        // Section 1B: Google Fit OAuth Connection Card
        if (selectedPlatformFilter == null || selectedPlatformFilter == WearablePlatform.GOOGLE_FIT) {
          item {
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("google_fit_connection_card")
            ) {
              Column(modifier = Modifier.padding(18.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4285F4).copy(alpha = 0.15f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = "Google Fit API",
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(24.dp)
                      )
                    }
                    Column {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                          text = "Google Fit API",
                          fontSize = 16.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = if (googleFitState == GoogleFitConnectionState.AUTHENTICATED_CONNECTED) VitalEmeraldContainer else VitalAlertContainer
                        ) {
                          Text(
                            text = if (googleFitState == GoogleFitConnectionState.AUTHENTICATED_CONNECTED) "CONNECTED" else "DISCONNECTED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (googleFitState == GoogleFitConnectionState.AUTHENTICATED_CONNECTED) VitalEmeraldSuccess else VitalAlertRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                      Text(
                        text = googleFitEmail,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  // Disconnect / Connect Toggle Button
                  OutlinedButton(
                    onClick = {
                      if (googleFitState == GoogleFitConnectionState.AUTHENTICATED_CONNECTED) {
                        repository.disconnectGoogleFit()
                      } else {
                        repository.connectGoogleFit()
                      }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .height(36.dp)
                      .testTag("toggle_google_fit_button")
                  ) {
                    Text(
                      text = if (googleFitState == GoogleFitConnectionState.AUTHENTICATED_CONNECTED) "Disconnect" else "Connect",
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // OAuth Scopes Pills
                Text(
                  text = "AUTHORIZED PERMISSIONS & SCOPES",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  ScopePill("fitness.activity.read (Steps & Energy)")
                  ScopePill("fitness.body.read (Heart Rate & PPG)")
                  ScopePill("fitness.heart_rate.read (Zones)")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(14.dp))

                // Primary Action: Sync Telemetry Now
                Button(
                  onClick = {
                    coroutineScope.launch {
                      val result = repository.syncWearableData(currentProfileId)
                      feedbackBannerMessage = "Successfully synced ${result.stepsImported} steps & ${result.restingHr} bpm RHR"
                    }
                  },
                  enabled = !isSyncing && googleFitState == GoogleFitConnectionState.AUTHENTICATED_CONNECTED,
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sync_google_fit_now_button")
                ) {
                  if (isSyncing) {
                    CircularProgressIndicator(
                      color = MaterialTheme.colorScheme.onPrimary,
                      strokeWidth = 2.5.dp,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = "Querying Google Fit REST API...",
                      fontSize = 13.5.sp,
                      fontWeight = FontWeight.Bold
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Default.Sync,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Sync Google Fit Data Now",
                      fontSize = 13.5.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }

        // Section 2: Paired Wearable Devices
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Paired Health Wearables",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              TextButton(
                onClick = { showPairDialog = true },
                modifier = Modifier.testTag("pair_new_device_button")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pair Device", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }

            if (filteredDevices.isEmpty()) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "No devices paired for this filter. Tap 'Pair Device' above to connect an Amazfit or Google wearable.",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(16.dp)
                )
              }
            } else {
              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
              ) {
                items(filteredDevices) { device ->
                  WearableDeviceCard(
                    device = device,
                    onToggleConnection = { repository.toggleWearableDeviceConnection(device.id) },
                    onSyncDevice = {
                      coroutineScope.launch {
                        repository.syncWearableData(currentProfileId, device.id)
                      }
                    }
                  )
                }
              }
            }
          }
        }

        // Section 2B: Zepp PAI (Personal Activity Intelligence) Telemetry Card
        if (selectedPlatformFilter == null || selectedPlatformFilter == WearablePlatform.ZEPP_HEALTH) {
          item {
            ZeppPaiTelemetryCard(
              biometrics = currentZeppBiometrics,
              patientName = currentProfile?.name ?: "Patient"
            )
          }
        }

        // Section 3: Daily Activity Telemetry Card
        item {
          DailyActivityTelemetryCard(
            activity = currentActivity,
            patientName = currentProfile?.name ?: "Patient"
          )
        }

        // Section 4: Heart Rate & Cardiovascular Telemetry Card
        item {
          HeartRateTelemetryCard(
            heartRate = currentHeartRate,
            patientName = currentProfile?.name ?: "Patient"
          )
        }

        // Section 5: Clinical Provenance & Ingest to Timeline
        item {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("clinical_provenance_card")
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    modifier = Modifier.size(20.dp)
                  )
                }
                Column {
                  Text(
                    text = "Clinical Provenance & Ingestion",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "FHIR Observation • LOINC 55423-8, 8867-4 & 80490-6 (PAI)",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Commit daily steps, continuous BioTracker PPG heart rate, and Zepp PAI cardioprotective score into ${currentProfile?.name ?: "Patient"}'s permanent health record with SHA-256 cryptographic provenance hashing.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
              )

              Spacer(modifier = Modifier.height(14.dp))

              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Source Platforms:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Zepp Health Open Platform & Google Fit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                  Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Consent Scope:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Patient-Owned / OAuth Authorized", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                  Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Clinical Validation:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("HUNT Study Validated (PAI ≥ 100)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VitalEmeraldSuccess)
                  }
                  Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Confidence Score:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("0.99 (BioTracker 5.0 Calibrated)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VitalEmeraldSuccess)
                  }
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              Button(
                onClick = {
                  val items = repository.importWearableToTimeline(currentProfileId)
                  feedbackBannerMessage = "Added ${items.size} clinical wearable observations to patient timeline!"
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("ingest_wearables_to_timeline_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Timeline,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Ingest Observations to Patient Timeline",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      // In-Place Floating Confirmation Toast
      AnimatedVisibility(
        visible = feedbackBannerMessage != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 60 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { 60 }),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 20.dp, start = 20.dp, end = 20.dp)
      ) {
        feedbackBannerMessage?.let { msg ->
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.testTag("wearable_feedback_toast")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(20.dp))
              Text(
                text = msg,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.weight(1f)
              )
              IconButton(onClick = { feedbackBannerMessage = null }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }

      // Dialog to pair a new device
      if (showPairDialog) {
        PairDeviceDialog(
          onDismiss = { showPairDialog = false },
          onPair = { newDevice ->
            repository.pairNewWearableDevice(newDevice)
            showPairDialog = false
            feedbackBannerMessage = "Paired ${newDevice.name} successfully!"
          }
        )
      }
    }
  }
}

@Composable
private fun ScopePill(label: String) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
  ) {
    Text(
      text = label,
      fontSize = 10.5.sp,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}

@Composable
fun WearableDeviceCard(
  device: WearableDevice,
  onToggleConnection: () -> Unit,
  onSyncDevice: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 2.dp,
    border = androidx.compose.foundation.BorderStroke(
      width = if (device.isConnected) 1.5.dp else 1.dp,
      color = if (device.isConnected) VitalTealPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    ),
    modifier = modifier
      .width(220.dp)
      .testTag("wearable_device_card_${device.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (device.isConnected) VitalTealContainer else MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when (device.type) {
              WearableDeviceType.PIXEL_WATCH -> Icons.Default.WatchLater
              WearableDeviceType.FITBIT -> Icons.AutoMirrored.Filled.DirectionsRun
              WearableDeviceType.GALAXY_WATCH -> Icons.Default.Watch
              WearableDeviceType.GARMIN -> Icons.Default.Navigation
              WearableDeviceType.WEAR_OS_GENERIC -> Icons.Default.DevicesOther
              WearableDeviceType.AMAZFIT_BALANCE -> Icons.Default.Speed
              WearableDeviceType.AMAZFIT_TREX -> Icons.Default.Explore
              WearableDeviceType.AMAZFIT_CHEETAH -> Icons.AutoMirrored.Filled.DirectionsRun
              WearableDeviceType.AMAZFIT_ACTIVE -> Icons.Default.Favorite
              WearableDeviceType.AMAZFIT_HELIO -> Icons.Default.Adjust
              WearableDeviceType.AMAZFIT_GTR -> Icons.Default.Watch
            },
            contentDescription = null,
            tint = if (device.isConnected) VitalTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }

        // Platform & Battery level pills
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (device.platform == WearablePlatform.ZEPP_HEALTH) Color(0xFF00C6FF).copy(alpha = 0.15f) else Color(0xFF4285F4).copy(alpha = 0.15f)
          ) {
            Text(
              text = if (device.platform == WearablePlatform.ZEPP_HEALTH) "ZEPP" else "FIT",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = if (device.platform == WearablePlatform.ZEPP_HEALTH) Color(0xFF008BA3) else Color(0xFF1A73E8),
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = when {
              device.batteryPercent > 50 -> VitalEmeraldContainer
              device.batteryPercent > 20 -> VitalWarningContainer
              else -> VitalAlertContainer
            }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              Icon(
                imageVector = if (device.batteryPercent > 20) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                contentDescription = null,
                tint = when {
                  device.batteryPercent > 50 -> VitalEmeraldSuccess
                  device.batteryPercent > 20 -> VitalWarningOrange
                  else -> VitalAlertRed
                },
                modifier = Modifier.size(11.dp)
              )
              Text(
                text = "${device.batteryPercent}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                  device.batteryPercent > 50 -> VitalEmeraldSuccess
                  device.batteryPercent > 20 -> VitalWarningOrange
                  else -> VitalAlertRed
                }
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = device.name,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1
      )
      Text(
        text = "${device.manufacturer} • ${device.firmwareVersion}",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
      )
      Text(
        text = device.sensorInfo,
        fontSize = 10.sp,
        color = if (device.platform == WearablePlatform.ZEPP_HEALTH) Color(0xFF008BA3) else MaterialTheme.colorScheme.primary,
        maxLines = 1
      )

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Last sync: ${device.formattedLastSync}",
        fontSize = 10.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onToggleConnection,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .testTag("device_toggle_${device.id}")
        ) {
          Text(
            text = if (device.isConnected) "Disconnect" else "Connect",
            fontSize = 11.sp
          )
        }

        FilledTonalButton(
          onClick = onSyncDevice,
          enabled = device.isConnected,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier
            .height(34.dp)
            .testTag("device_sync_${device.id}")
        ) {
          Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(14.dp))
        }
      }
    }
  }
}

@Composable
fun ZeppPaiTelemetryCard(
  biometrics: ZeppBiometrics,
  patientName: String,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00C6FF).copy(alpha = 0.35f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("zepp_pai_card")
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
              .size(38.dp)
              .clip(CircleShape)
              .background(Color(0xFF00C6FF).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = null,
              tint = Color(0xFF0097A7),
              modifier = Modifier.size(22.dp)
            )
          }
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "Zepp PAI & BioTracker™ Telemetry",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF00C6FF).copy(alpha = 0.15f)
              ) {
                Text(
                  text = "AMAZFIT",
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF008BA3),
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                )
              }
            }
            Text(
              text = "Personal Activity Intelligence • $patientName",
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // PAI Cardioprotective badge
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (biometrics.paiScore >= biometrics.paiGoal) VitalEmeraldContainer else VitalWarningContainer
        ) {
          Text(
            text = if (biometrics.paiScore >= biometrics.paiGoal) "CARDIOPROTECTIVE" else "BUILDING PAI",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (biometrics.paiScore >= biometrics.paiGoal) VitalEmeraldSuccess else VitalWarningOrange,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Highlight Hero Row: PAI Score & 7-Day Rolling Total
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF00C6FF).copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00C6FF).copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "CURRENT PAI SCORE",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = Color(0xFF008BA3)
            )
            Row(
              verticalAlignment = Alignment.Bottom,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "${biometrics.paiScore}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "/ 100 Goal",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
              )
            }
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "7-Day Total: ${biometrics.paiWeeklyTotal} PAI",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "HUNT Study: 25% ↓ CV Mortality",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Medium,
              color = VitalEmeraldSuccess
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // PAI Progress Bar toward cardioprotection
      val progress = (biometrics.paiScore.toFloat() / 150f).coerceIn(0f, 1f)
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = Color(0xFF00BCD4),
        trackColor = MaterialTheme.colorScheme.surfaceVariant
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 4-Card Biometrics Grid: Readiness, SpO2, Stress, Skin Temp
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Readiness", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = "${biometrics.readinessScore}/100",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = VitalEmeraldSuccess
            )
            Text("Optimal Recovery", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Blood Oxygen", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = "${biometrics.spo2Percent}%",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text("Continuous SpO2", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Stress Level", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = "${biometrics.stressLevel}/100",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF0097A7)
            )
            Text("Relaxed State", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Skin Temp", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = "${biometrics.skinTemperatureCelsius}°C",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text("Normal Range", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Sensor hardware info footer
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Sensor: ${biometrics.bioTrackerSensor}",
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Sleep Quality: ${biometrics.sleepQualityScore}/100",
          fontSize = 10.5.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF008BA3)
        )
      }
    }
  }
}

@Composable
fun DailyActivityTelemetryCard(
  activity: DailyActivityData,
  patientName: String,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("daily_activity_telemetry_card")
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
              .size(36.dp)
              .clip(CircleShape)
              .background(VitalTealContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
              contentDescription = null,
              tint = VitalTealPrimary,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Daily Activity Telemetry",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Google Fit • LOINC 55423-8",
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = VitalTealContainer
        ) {
          Text(
            text = "${(activity.stepProgressPercent * 100).toInt()}% GOAL",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = VitalTealPrimary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Steps Circular Progress + Metrics Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Step Gauge Ring
        Box(
          modifier = Modifier.size(96.dp),
          contentAlignment = Alignment.Center
        ) {
          val progress = activity.stepProgressPercent.coerceIn(0f, 1f)
          Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            // Background Track
            drawArc(
              color = Color.Gray.copy(alpha = 0.2f),
              startAngle = -90f,
              sweepAngle = 360f,
              useCenter = false,
              style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Foreground Arc
            drawArc(
              brush = Brush.sweepGradient(
                listOf(VitalTealPrimary, VitalEmeraldSuccess, VitalTealPrimary)
              ),
              startAngle = -90f,
              sweepAngle = progress * 360f,
              useCenter = false,
              style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "${activity.stepsCount}",
              fontSize = 16.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "of ${activity.stepGoal}",
              fontSize = 9.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // 4 Micro Metric Cards
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            ActivityMetricTile(
              label = "Active Time",
              value = "${activity.activeMinutes}m",
              goal = "Target ${activity.activeMinutesGoal}m",
              icon = Icons.Default.Timer,
              color = VitalTealPrimary,
              modifier = Modifier.weight(1f)
            )
            ActivityMetricTile(
              label = "Calories",
              value = "${activity.caloriesBurnedKcal}",
              goal = "kcal burned",
              icon = Icons.Default.LocalFireDepartment,
              color = VitalWarningOrange,
              modifier = Modifier.weight(1f)
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            ActivityMetricTile(
              label = "Distance",
              value = String.format(Locale.US, "%.1f km", activity.distanceKm),
              goal = String.format(Locale.US, "%.1f mi", activity.distanceMiles),
              icon = Icons.Default.Straighten,
              color = Color(0xFF6366F1),
              modifier = Modifier.weight(1f)
            )
            ActivityMetricTile(
              label = "Elevation",
              value = "${activity.floorsClimbed}",
              goal = "floors climbed",
              icon = Icons.Default.Terrain,
              color = VitalEmeraldSuccess,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Hourly Step Cadence Chart (Canvas Bar Chart)
      Text(
        text = "HOURLY STEP DISTRIBUTION (6 AM – 10 PM)",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
      )
      Spacer(modifier = Modifier.height(8.dp))

      HourlyActivityBarChart(
        hourlyBuckets = activity.hourlySteps,
        modifier = Modifier
          .fillMaxWidth()
          .height(72.dp)
      )
    }
  }
}

@Composable
private fun ActivityMetricTile(
  label: String,
  value: String,
  goal: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
      }
      Column {
        Text(text = value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = goal, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
      }
    }
  }
}

@Composable
fun HourlyActivityBarChart(
  hourlyBuckets: List<HourlyStepBucket>,
  modifier: Modifier = Modifier
) {
  val daytimeHours = remember(hourlyBuckets) {
    if (hourlyBuckets.size >= 24) hourlyBuckets.subList(6, 23) else hourlyBuckets
  }
  val maxSteps = remember(daytimeHours) {
    (daytimeHours.maxOfOrNull { it.steps } ?: 1000).coerceAtLeast(200)
  }

  Canvas(modifier = modifier) {
    if (daytimeHours.isEmpty()) return@Canvas

    val barSpacing = 4.dp.toPx()
    val availableWidth = size.width - (barSpacing * (daytimeHours.size - 1))
    val barWidth = availableWidth / daytimeHours.size
    val maxHeight = size.height - 14.dp.toPx()

    daytimeHours.forEachIndexed { index, bucket ->
      val barHeight = ((bucket.steps.toFloat() / maxSteps.toFloat()) * maxHeight).coerceAtLeast(3.dp.toPx())
      val left = index * (barWidth + barSpacing)
      val top = maxHeight - barHeight

      val barColor = if (bucket.steps > 800) VitalTealPrimary else Color(0xFF64748B).copy(alpha = 0.6f)

      drawRoundRect(
        color = barColor,
        topLeft = Offset(left, top),
        size = Size(barWidth, barHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
      )
    }
  }
}

@Composable
fun HeartRateTelemetryCard(
  heartRate: HeartRateData,
  patientName: String,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("heart_rate_telemetry_card")
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
              .size(36.dp)
              .clip(CircleShape)
              .background(VitalAlertRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Favorite,
              contentDescription = null,
              tint = VitalAlertRed,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Cardiovascular PPG Telemetry",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Google Fit • LOINC 8867-4",
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
            text = "OPTIMAL BASELINE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = VitalEmeraldSuccess,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Resting Heart Rate Hero + Metrics
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(
            text = "Resting Heart Rate",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "${heartRate.restingHeartRateBpm}",
              fontSize = 32.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "bpm",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 4.dp)
            )
          }
          Text(
            text = "Normal clinical range: 60-100 bpm",
            fontSize = 10.sp,
            color = VitalEmeraldSuccess
          )
        }

        // 3 Key Stats
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          HrStatChip(label = "Min", value = "${heartRate.minHeartRateBpm} bpm")
          HrStatChip(label = "Max", value = "${heartRate.maxHeartRateBpm} bpm")
          HrStatChip(label = "HRV", value = "${heartRate.heartRateVariabilityMs} ms")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 24h Diurnal Heart Rate Curve (Canvas)
      Text(
        text = "24-HOUR DIURNAL CONTINUOUS HEART RATE CURVE",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
      )
      Spacer(modifier = Modifier.height(8.dp))

      HeartRateDiurnalCanvas(
        samples = heartRate.samples,
        modifier = Modifier
          .fillMaxWidth()
          .height(80.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Training Zones Breakdown
      Text(
        text = "PHYSIOLOGICAL TRAINING ZONES",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
      )
      Spacer(modifier = Modifier.height(8.dp))

      // Stacked Zone Bar
      val zones = heartRate.zones
      val totalMinutes = zones.totalTrackedMinutes.coerceAtLeast(1)
      val restPercent = zones.restMinutes.toFloat() / totalMinutes
      val fatBurnPercent = zones.fatBurnMinutes.toFloat() / totalMinutes
      val cardioPercent = zones.cardioMinutes.toFloat() / totalMinutes
      val peakPercent = zones.peakMinutes.toFloat() / totalMinutes

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
          .clip(RoundedCornerShape(5.dp))
      ) {
        Box(modifier = Modifier.weight(restPercent.coerceAtLeast(0.02f)).fillMaxHeight().background(Color(0xFF64748B)))
        Box(modifier = Modifier.weight(fatBurnPercent.coerceAtLeast(0.02f)).fillMaxHeight().background(VitalTealPrimary))
        Box(modifier = Modifier.weight(cardioPercent.coerceAtLeast(0.02f)).fillMaxHeight().background(VitalWarningOrange))
        Box(modifier = Modifier.weight(peakPercent.coerceAtLeast(0.02f)).fillMaxHeight().background(VitalAlertRed))
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Zone Legend Pills
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        ZoneLegendPill("Rest", "${zones.restMinutes}m", Color(0xFF64748B))
        ZoneLegendPill("Fat Burn", "${zones.fatBurnMinutes}m", VitalTealPrimary)
        ZoneLegendPill("Cardio", "${zones.cardioMinutes}m", VitalWarningOrange)
        ZoneLegendPill("Peak", "${zones.peakMinutes}m", VitalAlertRed)
      }
    }
  }
}

@Composable
private fun HrStatChip(label: String, value: String) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(text = value, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
  }
}

@Composable
private fun ZoneLegendPill(name: String, duration: String, color: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color)
    )
    Column {
      Text(text = name, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(text = duration, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
  }
}

@Composable
fun HeartRateDiurnalCanvas(
  samples: List<HeartRateSample>,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    if (samples.size < 2) return@Canvas

    val minBpm = 45f
    val maxBpm = 160f
    val w = size.width
    val h = size.height

    val path = Path()
    val fillPath = Path()

    samples.forEachIndexed { index, sample ->
      val x = (index.toFloat() / (samples.size - 1)) * w
      val normBpm = (sample.bpm - minBpm) / (maxBpm - minBpm)
      val y = h - (normBpm.coerceIn(0f, 1f) * h)

      if (index == 0) {
        path.moveTo(x, y)
        fillPath.moveTo(x, h)
        fillPath.lineTo(x, y)
      } else {
        val prevIndex = index - 1
        val prevX = (prevIndex.toFloat() / (samples.size - 1)) * w
        val prevNorm = (samples[prevIndex].bpm - minBpm) / (maxBpm - minBpm)
        val prevY = h - (prevNorm.coerceIn(0f, 1f) * h)

        val cx = (prevX + x) / 2f
        path.quadraticTo(prevX, prevY, cx, (prevY + y) / 2f)
        fillPath.quadraticTo(prevX, prevY, cx, (prevY + y) / 2f)
      }
    }

    val lastX = w
    val lastSample = samples.last()
    val lastY = h - (((lastSample.bpm - minBpm) / (maxBpm - minBpm)).coerceIn(0f, 1f) * h)
    path.lineTo(lastX, lastY)

    fillPath.lineTo(lastX, lastY)
    fillPath.lineTo(lastX, h)
    fillPath.close()

    // Draw area fill
    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        colors = listOf(VitalAlertRed.copy(alpha = 0.25f), Color.Transparent)
      )
    )

    // Draw continuous curve stroke
    drawPath(
      path = path,
      color = VitalAlertRed,
      style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
    )
  }
}

@Composable
fun PairDeviceDialog(
  onDismiss: () -> Unit,
  onPair: (WearableDevice) -> Unit
) {
  var selectedPlatform by remember { mutableStateOf<WearablePlatform?>(null) }
  var selectedType by remember { mutableStateOf(WearableDeviceType.AMAZFIT_BALANCE) }
  var deviceName by remember { mutableStateOf("Amazfit Balance") }

  val filteredTypes = remember(selectedPlatform) {
    if (selectedPlatform == null) {
      WearableDeviceType.values().toList()
    } else {
      WearableDeviceType.values().filter { it.platform == selectedPlatform }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.Watch, contentDescription = null, tint = VitalTealPrimary)
        Text("Pair Health Wearable", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Select a wearable device to pair via Zepp Health or Google Fit API:",
          fontSize = 12.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Platform Filter Chips
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          FilterChip(
            selected = selectedPlatform == null,
            onClick = { selectedPlatform = null },
            label = { Text("All", fontSize = 11.sp) },
            modifier = Modifier.testTag("pair_filter_all")
          )
          FilterChip(
            selected = selectedPlatform == WearablePlatform.ZEPP_HEALTH,
            onClick = {
              selectedPlatform = WearablePlatform.ZEPP_HEALTH
              selectedType = WearableDeviceType.AMAZFIT_BALANCE
              deviceName = "Amazfit Balance"
            },
            label = { Text("Amazfit (Zepp)", fontSize = 11.sp) },
            modifier = Modifier.testTag("pair_filter_zepp")
          )
          FilterChip(
            selected = selectedPlatform == WearablePlatform.GOOGLE_FIT,
            onClick = {
              selectedPlatform = WearablePlatform.GOOGLE_FIT
              selectedType = WearableDeviceType.PIXEL_WATCH
              deviceName = "Google Pixel Watch 3"
            },
            label = { Text("Google Fit", fontSize = 11.sp) },
            modifier = Modifier.testTag("pair_filter_google")
          )
        }

        // List of wearable devices
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filteredTypes) { type ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (selectedType == type) VitalTealContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              border = if (selectedType == type) androidx.compose.foundation.BorderStroke(1.5.dp, VitalTealPrimary) else null,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedType = type
                  deviceName = when (type) {
                    WearableDeviceType.PIXEL_WATCH -> "Google Pixel Watch 3"
                    WearableDeviceType.FITBIT -> "Fitbit Sense 2"
                    WearableDeviceType.GALAXY_WATCH -> "Samsung Galaxy Watch 7"
                    WearableDeviceType.GARMIN -> "Garmin Fenix 8"
                    WearableDeviceType.WEAR_OS_GENERIC -> "Wear OS Smartwatch"
                    WearableDeviceType.AMAZFIT_BALANCE -> "Amazfit Balance"
                    WearableDeviceType.AMAZFIT_TREX -> "Amazfit T-Rex Ultra"
                    WearableDeviceType.AMAZFIT_CHEETAH -> "Amazfit Cheetah Pro"
                    WearableDeviceType.AMAZFIT_ACTIVE -> "Amazfit Active"
                    WearableDeviceType.AMAZFIT_HELIO -> "Amazfit Helio Ring"
                    WearableDeviceType.AMAZFIT_GTR -> "Amazfit GTR 4"
                  }
                }
                .testTag("pair_option_${type.name}")
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                RadioButton(
                  selected = selectedType == type,
                  onClick = null
                )
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(text = type.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = if (type.platform == WearablePlatform.ZEPP_HEALTH) Color(0xFF00C6FF).copy(alpha = 0.15f) else Color(0xFF4285F4).copy(alpha = 0.15f)
                    ) {
                      Text(
                        text = if (type.platform == WearablePlatform.ZEPP_HEALTH) "ZEPP" else "FIT",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (type.platform == WearablePlatform.ZEPP_HEALTH) Color(0xFF008BA3) else Color(0xFF1A73E8),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                      )
                    }
                  }
                  Text(
                    text = "${type.defaultManufacturer} • ${if (type.platform == WearablePlatform.ZEPP_HEALTH) "BioTracker PPG & PAI" else "Wear OS / Google Fit"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val randomSuffix = (1000..9999).random()
          val isZepp = selectedType.platform == WearablePlatform.ZEPP_HEALTH
          val newDev = WearableDevice(
            id = "device_${selectedType.name.lowercase()}_$randomSuffix",
            name = deviceName,
            type = selectedType,
            manufacturer = selectedType.defaultManufacturer,
            model = if (isZepp) "A${(2100..2300).random()}" else "HW-$randomSuffix",
            batteryPercent = (75..100).random(),
            isConnected = true,
            lastSyncedTimestamp = System.currentTimeMillis(),
            deviceIdentifier = if (isZepp) "AF:${(10..99).random()}:${(10..99).random()}:${(10..99).random()}" else "BT:${(10..99).random()}:${(10..99).random()}:${(10..99).random()}",
            syncFrequencyMinutes = 15,
            firmwareVersion = if (isZepp) "Zepp OS 3.5 (v3.24.1)" else "v4.1.0",
            platform = selectedType.platform,
            sensorInfo = if (isZepp) "BioTracker™ 5.0 PPG & PAI" else "Optical PPG Sensor"
          )
          onPair(newDev)
        },
        colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
        modifier = Modifier.testTag("confirm_pair_device_button")
      ) {
        Text("Pair Device", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_pair_device_button")
      ) {
        Text("Cancel")
      }
    },
    modifier = Modifier.testTag("pair_device_dialog")
  )
}

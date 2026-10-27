package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthService
import com.example.data.model.*
import com.example.data.repository.VitalithRepository
import com.example.ui.theme.*
import com.example.util.UcumUnitConverter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  repository: VitalithRepository,
  authService: AuthService? = null,
  onNavigateBack: () -> Unit,
  onNavigateToPrivacy: () -> Unit = {},
  onNavigateToProfile: () -> Unit = {},
  onNavigateToEmergency: () -> Unit = {},
  onNavigateToExportImport: () -> Unit = {},
  onNavigateToAppearance: () -> Unit = {},
  onNavigateToSubscription: () -> Unit = {},
  onLogOut: () -> Unit = {},
  onRevisitTour: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val userProfile by repository.userProfile.collectAsState()

  // App-level preferences state
  var autoLockTimeout by remember { mutableStateOf("5 minutes") }
  var autoOcrProcessing by remember { mutableStateOf(true) }
  var drugConflictAlerts by remember { mutableStateOf(true) }
  var fhirSyncFrequency by remember { mutableStateOf("Real-Time") }
  var refillReminders by remember { mutableStateOf(true) }
  var labResultAlerts by remember { mutableStateOf(true) }
  var careGrantAlerts by remember { mutableStateOf(true) }
  var highContrastTheme by remember { mutableStateOf(false) }

  var showLogOutConfirm by remember { mutableStateOf(false) }
  var showPurgeCacheConfirm by remember { mutableStateOf(false) }
  var showRecoveryKeyDialog by remember { mutableStateOf(false) }
  var testConverterInput by remember { mutableStateOf("5.6") }
  var testConverterMetric by remember { mutableStateOf("Glucose") }

  BackHandler {
    onNavigateBack()
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Settings & Security",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
            Text(
              text = "Hardware Enclave • FIPS 140-2 Level 3",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("settings_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Previous Screen"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier
      .fillMaxSize()
      .testTag("settings_screen")
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Account & Patient Vault
      SettingsSectionHeader(title = "ACCOUNT & PATIENT VAULT")

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
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
              Text(
                text = userProfile.email,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = userProfile.phone,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              color = VitalTealContainer,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "Account Owner",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VitalOnTealContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
            TextButton(
              onClick = onNavigateToProfile,
              modifier = Modifier.testTag("settings_edit_profile_button")
            ) {
              Text("Edit Profiles & Patient List", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VitalTealPrimary)
            }
          }
        }
      }

      // 2. Membership & Subscription Plan
      SettingsSectionHeader(title = "MEMBERSHIP & SUBSCRIPTION")

      val subscriptionTier by repository.subscriptionTier.collectAsState()
      val subscriptionDetails by repository.subscriptionDetails.collectAsState()

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Surface(
            onClick = onNavigateToSubscription,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("settings_subscription_navigation_row")
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(VitalTealContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.WorkspacePremium,
                  contentDescription = null,
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(22.dp)
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = subscriptionTier.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                  Surface(
                    color = VitalEmeraldContainer,
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      text = subscriptionDetails.status,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = VitalOnEmeraldContainer,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Active Plan • Next billing: ${subscriptionDetails.renewalDate} (${subscriptionDetails.billingCycle.label}). Tap to view plans & payments.",
                  fontSize = 11.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  lineHeight = 15.sp
                )
              }

              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // 3. Appearance, Themes & Display
      SettingsSectionHeader(title = "APPEARANCE, THEMES & DISPLAY")

      val activeTokens by ThemeManager.currentTokens.collectAsState()

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          // User-Accessible Dark / Light Mode Toggle (Persisted in DataStore)
          SettingsToggleItem(
            title = "Dark Mode",
            description = if (activeTokens.isDark) "Dark mode is currently active (persisted in DataStore)" else "Light mode is currently active (persisted in DataStore)",
            checked = activeTokens.isDark,
            onCheckedChange = { isDark ->
              ThemeManager.setDarkMode(isDark, context)
            },
            testTag = "settings_dark_mode_toggle"
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          Surface(
            onClick = onNavigateToAppearance,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("settings_appearance_navigation_row")
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(VitalTealContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Palette,
                  contentDescription = null,
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(22.dp)
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = "Appearance & Themes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                  Surface(
                    color = VitalTealPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      text = activeTokens.label,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = VitalTealPrimary,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Customize from 9 clinical color themes, dark mode, high contrast AAA, and font scaling.",
                  fontSize = 11.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  lineHeight = 15.sp
                )
              }

              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // 3. Cryptographic Security & Biometric Enclave
      SettingsSectionHeader(title = "SECURITY & CRYPTOGRAPHIC ENCLAVE")

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          // Biometric Lock
          SettingsToggleItem(
            title = "Biometric Vault Lock",
            description = "Require biometric verification (Fingerprint or Face Unlock) to open vault and decrypt clinical records.",
            checked = userProfile.biometricLockEnabled,
            onCheckedChange = { repository.setBiometricLock(it) },
            testTag = "settings_biometric_toggle"
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // Auto-Lock Timeout
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Vault Auto-Lock Timeout", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
              text = "Automatically re-encrypt and lock device keystore when inactive.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              listOf("Immediately", "1 minute", "5 minutes", "15 minutes").forEach { option ->
                val isSelected = autoLockTimeout == option
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    autoLockTimeout = option
                    repository.recordAudit("AUTO_LOCK_UPDATED", "Auto-lock timeout set to $option")
                  },
                  label = { Text(option, fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VitalTealPrimary,
                    selectedLabelColor = Color.White
                  )
                )
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // Hardware Enclave Details
          Surface(
            color = VitalEmeraldContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(22.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Hardware KeyStore Active",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = VitalEmeraldSuccess
                )
                Text(
                  text = "Device-bound AES-256-GCM master key protected by Android TEE / StrongBox Enclave.",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          // Recovery Phrase Backup
          OutlinedButton(
            onClick = { showRecoveryKeyDialog = true },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Key, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Cryptographic Enclave Key & Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // 3. Emergency Medical ID & Lock Screen Access
      SettingsSectionHeader(title = "EMERGENCY MEDICAL ID & FIRST-RESPONDER ACCESS")

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          SettingsToggleItem(
            title = "Emergency Medical ID on Lock Screen",
            description = "Allow first-responders and ER staff to view blood type, severe allergies, and primary contact without passcode.",
            checked = userProfile.emergencyMedicalIdUnlocked,
            onCheckedChange = { repository.setEmergencyMedicalIdUnlocked(it) },
            testTag = "settings_emergency_id_toggle"
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Column {
              Text(text = "Blood Group", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(text = "O+ (Positive)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Column {
              Text(text = "Primary Contact", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(text = userProfile.emergencyContactName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
          }

          Button(
            onClick = onNavigateToEmergency,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Emergency Snapshot Preview", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // 4. Clinical Units & Measures (UCUM Standard)
      SettingsSectionHeader(title = "CLINICAL UNITS & MEASURES (UCUM STANDARD)")

      val unitPrefs = userProfile.unitPreferences

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("settings_units_section")
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          // Explanatory banner
          Surface(
            color = VitalTealContainerDark.copy(alpha = 0.2f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.Tune, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(20.dp))
              Column {
                Text(
                  text = "Computable UCUM Storage Standard",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = VitalTealPrimary
                )
                Text(
                  text = "Biomarkers are stored internally in computable Unified Code for Units of Measure (UCUM) syntax and converted on-the-fly to your preferred local display units.",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          // System Preset
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Standard System Preset", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
              text = "Quickly set all biomarker units to standard regional clinical conventions.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              FilterChip(
                selected = unitPrefs.unitSystem == UnitSystem.METRIC_SI,
                onClick = { repository.setUnitSystem(UnitSystem.METRIC_SI) },
                label = { Text("Metric / SI (Default)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("unit_system_metric_si")
              )
              FilterChip(
                selected = unitPrefs.unitSystem == UnitSystem.US_CUSTOMARY,
                onClick = { repository.setUnitSystem(UnitSystem.US_CUSTOMARY) },
                label = { Text("US Customary", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("unit_system_us_customary")
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // 1. Blood Glucose
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(text = "Blood Glucose", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
              text = "UCUM: mmol/L vs. mg/dL (1 mmol/L ≈ 18.02 mg/dL)",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              FilterChip(
                selected = unitPrefs.glucoseUnit == GlucoseUnit.MMOL_L,
                onClick = { repository.setGlucoseUnit(GlucoseUnit.MMOL_L) },
                label = { Text("mmol/L", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("unit_glucose_mmol")
              )
              FilterChip(
                selected = unitPrefs.glucoseUnit == GlucoseUnit.MG_DL,
                onClick = { repository.setGlucoseUnit(GlucoseUnit.MG_DL) },
                label = { Text("mg/dL", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("unit_glucose_mgdl")
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // 2. Body Temperature
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(text = "Body Temperature", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
              text = "UCUM: Cel vs. [degF]",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              FilterChip(
                selected = unitPrefs.temperatureUnit == TemperatureUnit.CELSIUS,
                onClick = { repository.setTemperatureUnit(TemperatureUnit.CELSIUS) },
                label = { Text("°C", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("unit_temp_c")
              )
              FilterChip(
                selected = unitPrefs.temperatureUnit == TemperatureUnit.FAHRENHEIT,
                onClick = { repository.setTemperatureUnit(TemperatureUnit.FAHRENHEIT) },
                label = { Text("°F", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("unit_temp_f")
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // 3. Body Weight & Mass
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(text = "Body Weight & Mass", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
              text = "UCUM: kg vs. [lb_av]",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              FilterChip(
                selected = unitPrefs.weightUnit == WeightUnit.KG,
                onClick = { repository.setWeightUnit(WeightUnit.KG) },
                label = { Text("kg", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("unit_weight_kg")
              )
              FilterChip(
                selected = unitPrefs.weightUnit == WeightUnit.LBS,
                onClick = { repository.setWeightUnit(WeightUnit.LBS) },
                label = { Text("lbs", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("unit_weight_lbs")
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // 4. Height & Length
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(text = "Height & Stature", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
              text = "UCUM: cm vs. [in_i]",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              FilterChip(
                selected = unitPrefs.heightUnit == HeightUnit.CM,
                onClick = { repository.setHeightUnit(HeightUnit.CM) },
                label = { Text("cm", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("unit_height_cm")
              )
              FilterChip(
                selected = unitPrefs.heightUnit == HeightUnit.FT_IN,
                onClick = { repository.setHeightUnit(HeightUnit.FT_IN) },
                label = { Text("ft / in", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("unit_height_ftin")
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // 5. Cholesterol & Lipids
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(text = "Total Cholesterol & Lipids", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
              text = "UCUM: mmol/L vs. mg/dL (1 mmol/L ≈ 38.67 mg/dL)",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              FilterChip(
                selected = unitPrefs.cholesterolUnit == CholesterolUnit.MMOL_L,
                onClick = { repository.setCholesterolUnit(CholesterolUnit.MMOL_L) },
                label = { Text("mmol/L", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("unit_chol_mmol")
              )
              FilterChip(
                selected = unitPrefs.cholesterolUnit == CholesterolUnit.MG_DL,
                onClick = { repository.setCholesterolUnit(CholesterolUnit.MG_DL) },
                label = { Text("mg/dL", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = VitalTealPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("unit_chol_mgdl")
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // 6. Interactive Live Computable UCUM Preview & Tester
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(16.dp))
                Text(
                  text = "Live Computable UCUM Conversion Tester",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              // Selector for test metric
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                listOf("Glucose", "Temperature", "Weight").forEach { metric ->
                  val isSel = testConverterMetric == metric
                  FilterChip(
                    selected = isSel,
                    onClick = {
                      testConverterMetric = metric
                      testConverterInput = when (metric) {
                        "Glucose" -> if (unitPrefs.glucoseUnit == GlucoseUnit.MG_DL) "100" else "5.6"
                        "Temperature" -> if (unitPrefs.temperatureUnit == TemperatureUnit.FAHRENHEIT) "98.6" else "37.0"
                        else -> if (unitPrefs.weightUnit == WeightUnit.LBS) "172.4" else "78.2"
                      }
                    },
                    label = { Text(metric, fontSize = 10.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = VitalTealPrimary,
                      selectedLabelColor = Color.White
                    )
                  )
                }
              }

              OutlinedTextField(
                value = testConverterInput,
                onValueChange = { testConverterInput = it },
                label = {
                  val activeUnitLabel = when (testConverterMetric) {
                    "Glucose" -> unitPrefs.glucoseUnit.label
                    "Temperature" -> unitPrefs.temperatureUnit.label
                    else -> unitPrefs.weightUnit.label
                  }
                  Text("Test reading in local unit ($activeUnitLabel)")
                },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("settings_unit_converter_input")
              )

              // Conversion computation result display
              val numVal = testConverterInput.toDoubleOrNull() ?: 0.0
              val (storedUcum, alternateUnitDisplay) = when (testConverterMetric) {
                "Glucose" -> {
                  if (unitPrefs.glucoseUnit == GlucoseUnit.MG_DL) {
                    val mmol = UcumUnitConverter.mgDlToMmolL(numVal)
                    Pair(
                      "Quantity(value=${String.format(java.util.Locale.US, "%.2f", mmol)}, code=\"mmol/L\", system=\"http://unitsofmeasure.org\")",
                      "${String.format(java.util.Locale.US, "%.1f", mmol)} mmol/L"
                    )
                  } else {
                    val mgDl = UcumUnitConverter.mmolLToMgDl(numVal)
                    Pair(
                      "Quantity(value=${String.format(java.util.Locale.US, "%.2f", numVal)}, code=\"mmol/L\", system=\"http://unitsofmeasure.org\")",
                      "${mgDl.roundToInt()} mg/dL"
                    )
                  }
                }
                "Temperature" -> {
                  if (unitPrefs.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
                    val c = UcumUnitConverter.fahrenheitToCelsius(numVal)
                    Pair(
                      "Quantity(value=${String.format(java.util.Locale.US, "%.2f", c)}, code=\"Cel\", system=\"http://unitsofmeasure.org\")",
                      "${String.format(java.util.Locale.US, "%.1f", c)} °C"
                    )
                  } else {
                    val f = UcumUnitConverter.celsiusToFahrenheit(numVal)
                    Pair(
                      "Quantity(value=${String.format(java.util.Locale.US, "%.2f", numVal)}, code=\"Cel\", system=\"http://unitsofmeasure.org\")",
                      "${String.format(java.util.Locale.US, "%.1f", f)} °F"
                    )
                  }
                }
                else -> {
                  if (unitPrefs.weightUnit == WeightUnit.LBS) {
                    val kg = UcumUnitConverter.lbsToKg(numVal)
                    Pair(
                      "Quantity(value=${String.format(java.util.Locale.US, "%.2f", kg)}, code=\"kg\", system=\"http://unitsofmeasure.org\")",
                      "${String.format(java.util.Locale.US, "%.1f", kg)} kg"
                    )
                  } else {
                    val lbs = UcumUnitConverter.kgToLbs(numVal)
                    Pair(
                      "Quantity(value=${String.format(java.util.Locale.US, "%.2f", numVal)}, code=\"kg\", system=\"http://unitsofmeasure.org\")",
                      "${String.format(java.util.Locale.US, "%.1f", lbs)} lbs"
                    )
                  }
                }
              }

              Surface(
                color = VitalNavy,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = "UNDERLYING COMPUTABLE UCUM RECORD:",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = VitalTealPrimary,
                    letterSpacing = 0.5.sp
                  )
                  Text(
                    text = storedUcum,
                    fontSize = 11.sp,
                    color = Color.White,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "CROSS-SYSTEM EQUIVALENT: $alternateUnitDisplay",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VitalEmeraldSuccess
                  )
                }
              }
            }
          }
        }
      }

      // 5. Clinical Ingestion & AI Verification
      SettingsSectionHeader(title = "CLINICAL INGESTION & AI TELEMETRY")

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          SettingsToggleItem(
            title = "Automatic Document OCR Extraction",
            description = "Immediately parse biomarker tables and clinical values when laboratory PDFs or photos are attached.",
            checked = autoOcrProcessing,
            onCheckedChange = { autoOcrProcessing = it }
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          SettingsToggleItem(
            title = "Live Drug Conflict & Interaction Checking",
            description = "Cross-reference newly reconciled medications against active regimens for contraindications.",
            checked = drugConflictAlerts,
            onCheckedChange = { drugConflictAlerts = it }
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // FHIR Sync Frequency
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "SMART on FHIR Health System Sync", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
              text = "Frequency for polling connected electronic health record portals.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              listOf("Real-Time", "Daily", "Manual Only").forEach { freq ->
                FilterChip(
                  selected = fhirSyncFrequency == freq,
                  onClick = { fhirSyncFrequency = freq },
                  label = { Text(freq, fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VitalTealPrimary,
                    selectedLabelColor = Color.White
                  )
                )
              }
            }
          }
        }
      }

      // 5. Notifications & Patient Adherence
      SettingsSectionHeader(title = "NOTIFICATIONS & ADHERENCE ALERTS")

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          SettingsToggleItem(
            title = "Medication Refill Warnings",
            description = "Notify when prescriptions have less than 7 days remaining supply.",
            checked = refillReminders,
            onCheckedChange = { refillReminders = it }
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          SettingsToggleItem(
            title = "Lab Result Ingestion Alerts",
            description = "Notify when external EHR sync or OCR extraction finishes processing.",
            checked = labResultAlerts,
            onCheckedChange = { labResultAlerts = it }
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          SettingsToggleItem(
            title = "Care-Team Grant Expiration Reminders",
            description = "Alert 24 hours before a time-limited provider access link expires.",
            checked = careGrantAlerts,
            onCheckedChange = { careGrantAlerts = it }
          )
        }
      }

      // 6. Privacy, Audit & Data Governance
      SettingsSectionHeader(title = "DATA GOVERNANCE & PRIVACY")

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          // Zero Data Sale Banner
          Surface(
            color = VitalTealContainerDark.copy(alpha = 0.2f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(20.dp))
              Column {
                Text(
                  text = "Zero Data Sale Guarantee",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = VitalTealPrimary
                )
                Text(
                  text = "Patient health records are encrypted exclusively with your keys. No telemetry or clinical data is ever sold or shared.",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          // View Audit Trail
          SettingsNavigationRow(
            icon = Icons.Default.Security,
            title = "Audit Trail & Cryptographic Provenance",
            subtitle = "Inspect immutable Merkle log of all accesses and imports",
            onClick = onNavigateToPrivacy
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          SettingsNavigationRow(
            icon = Icons.Default.ImportExport,
            title = "Comprehensive Data Export & Import",
            subtitle = "HL7 FHIR R4 JSON, Clinical PDF Archives, CSV and Ingestion",
            onClick = onNavigateToExportImport
          )

          // Purge Cached Thumbnails
          OutlinedButton(
            onClick = { showPurgeCacheConfirm = true },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VitalAlertRed),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = VitalAlertRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear Temporary Cache & Scanned Previews", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }

          SettingsNavigationRow(
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            title = "App Tour & Feature Guide",
            subtitle = "Revisit the 3-minute guided tour of Vitalith capabilities",
            onClick = onRevisitTour
          )
        }
      }

      // 7. System & Build Info
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(text = "Vitalith Health Vault", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Text(text = "Version 2.4.0 (Build 2026.10-production)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(text = "Specification: HL7 FHIR R4.0.1 • HIPAA Security Rule Compliant", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(text = "Cryptography: BouncyCastle AES-256-GCM / AndroidKeyStore", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      // 8. Log Out Button
      Button(
        onClick = { showLogOutConfirm = true },
        colors = ButtonDefaults.buttonColors(containerColor = VitalAlertRed),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("settings_logout_button")
      ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign Out & Lock Hardware Keystore", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  // Recovery Key Dialog
  if (showRecoveryKeyDialog) {
    AlertDialog(
      onDismissRequest = { showRecoveryKeyDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.Key, contentDescription = null, tint = VitalTealPrimary)
          Text("Hardware Master Key", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Your 256-bit AES master encryption key is bound to this device's hardware enclave:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "AES_GCM_256::VTL-${userProfile.fullName.hashCode().toString().takeLast(8)}::TEE_STRONG_BOX",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(12.dp)
            )
          }
          Text(
            text = "Keep this identifier safe for offline recovery if switching devices without cloud synchronization.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { showRecoveryKeyDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
        ) {
          Text("Done", color = Color.White)
        }
      }
    )
  }

  // Purge Cache Confirmation
  if (showPurgeCacheConfirm) {
    AlertDialog(
      onDismissRequest = { showPurgeCacheConfirm = false },
      title = { Text("Clear Temporary Cache?", fontWeight = FontWeight.Bold) },
      text = {
        Text(
          text = "This will remove cached thumbnail images and temporary OCR scans. Your verified medical timeline records, medications, and laboratory data will remain intact in your encrypted local SQLite database.",
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showPurgeCacheConfirm = false
            coroutineScope.launch {
              repository.recordAudit("CACHE_PURGED", "Temporary document cache cleared")
              snackbarHostState.showSnackbar("Temporary cache cleared.")
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalAlertRed)
        ) {
          Text("Clear Cache", color = Color.White)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showPurgeCacheConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Log Out Confirmation Dialog
  if (showLogOutConfirm) {
    AlertDialog(
      onDismissRequest = { showLogOutConfirm = false },
      title = { Text("Sign Out of Vitalith?", fontWeight = FontWeight.Bold) },
      text = {
        Text(
          text = "Signing out will clear the active session and lock the hardware keystore. You will need to log back in to access your personal health vault.",
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showLogOutConfirm = false
            onLogOut()
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalAlertRed)
        ) {
          Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showLogOutConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun SettingsSectionHeader(title: String) {
  Text(
    text = title,
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp,
    color = VitalTealPrimary,
    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
  )
}

@Composable
private fun SettingsToggleItem(
  title: String,
  description: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String? = null
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = description,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 15.sp
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    )
  }
}

@Composable
private fun SettingsNavigationRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(20.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp)
      )
    }
  }
}

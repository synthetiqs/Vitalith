package com.example.data.repository

import com.example.data.local.VitalithDatabase
import com.example.data.local.entities.*
import com.example.data.model.*
import com.example.util.GeminiHealthSummarizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VitalithRepository(
  private val database: VitalithDatabase? = null
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _profiles = MutableStateFlow<List<PatientProfile>>(initialProfilesList())
  val profiles: StateFlow<List<PatientProfile>> = _profiles.asStateFlow()

  private val _userProfile = MutableStateFlow(UserProfile())
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  private val _currentProfileId = MutableStateFlow("profile_doug")
  val currentProfileId: StateFlow<String> = _currentProfileId.asStateFlow()

  private val _subscriptionTier = MutableStateFlow(SubscriptionTier.PREMIUM)
  val subscriptionTier: StateFlow<SubscriptionTier> = _subscriptionTier.asStateFlow()

  private val _subscriptionDetails = MutableStateFlow(
    SubscriptionDetails(
      tier = SubscriptionTier.PREMIUM,
      billingCycle = BillingCycle.ANNUAL,
      status = "ACTIVE",
      renewalDate = "Oct 26, 2027",
      lastPaymentMethod = "Google Pay (abydouglas@gmail.com)",
      lastTransactionId = "TXN-VTL-2026-98124",
      autoRenew = true
    )
  )
  val subscriptionDetails: StateFlow<SubscriptionDetails> = _subscriptionDetails.asStateFlow()

  private val _paymentHistory = MutableStateFlow<List<PaymentTransaction>>(initialPaymentHistoryList())
  val paymentHistory: StateFlow<List<PaymentTransaction>> = _paymentHistory.asStateFlow()

  private val _timelineItems = MutableStateFlow<List<HealthTimelineItem>>(
    initialDougTimeline() + initialMayaTimeline() + initialKwameTimeline()
  )
  val timelineItems: StateFlow<List<HealthTimelineItem>> = _timelineItems.asStateFlow()

  private val _medications = MutableStateFlow<List<MedicationItem>>(
    initialDougMeds() + initialMayaMeds() + initialKwameMeds()
  )
  val medications: StateFlow<List<MedicationItem>> = _medications.asStateFlow()

  private val _reminders = MutableStateFlow<List<MedicationReminder>>(
    initialReminders() + initialMayaReminders() + initialKwameReminders()
  )
  val reminders: StateFlow<List<MedicationReminder>> = _reminders.asStateFlow()

  private val _careGrants = MutableStateFlow<List<CareGrant>>(
    initialGrants() + initialMayaGrants() + initialKwameGrants()
  )
  val careGrants: StateFlow<List<CareGrant>> = _careGrants.asStateFlow()

  private val _auditLogs = MutableStateFlow<List<AuditEntry>>(initialAuditLogs())
  val auditLogs: StateFlow<List<AuditEntry>> = _auditLogs.asStateFlow()

  private val _chatMessages = MutableStateFlow<List<ChatMessage>>(initialChatHistory())
  val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

  private val _symptoms = MutableStateFlow<List<SymptomItem>>(
    initialDougSymptoms() + initialMayaSymptoms() + initialKwameSymptoms()
  )
  val symptoms: StateFlow<List<SymptomItem>> = _symptoms.asStateFlow()

  private val _appSettings = MutableStateFlow(AppSettings())
  val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

  private val _dailyAiQueriesCount = MutableStateFlow(0)
  val dailyAiQueriesCount: StateFlow<Int> = _dailyAiQueriesCount.asStateFlow()

  private val _monthlyOcrScansCount = MutableStateFlow(0)
  val monthlyOcrScansCount: StateFlow<Int> = _monthlyOcrScansCount.asStateFlow()

  val wearableService = com.example.data.wearable.GoogleFitWearableIntegrationService()
  val zeppWearableService = com.example.data.wearable.ZeppWearableIntegrationService()

  private val _wearableDevices = MutableStateFlow<List<WearableDevice>>(
    wearableService.getDefaultDevices() + zeppWearableService.getDefaultDevices()
  )
  val wearableDevices: StateFlow<List<WearableDevice>> = _wearableDevices.asStateFlow()

  private val _googleFitState = MutableStateFlow(GoogleFitConnectionState.AUTHENTICATED_CONNECTED)
  val googleFitState: StateFlow<GoogleFitConnectionState> = _googleFitState.asStateFlow()

  private val _googleFitAccountEmail = MutableStateFlow("abydouglas@gmail.com")
  val googleFitAccountEmail: StateFlow<String> = _googleFitAccountEmail.asStateFlow()

  private val _zeppState = MutableStateFlow(ZeppConnectionState.AUTHENTICATED_CONNECTED)
  val zeppState: StateFlow<ZeppConnectionState> = _zeppState.asStateFlow()

  private val _zeppAccountEmail = MutableStateFlow("abydouglas@zepp.life")
  val zeppAccountEmail: StateFlow<String> = _zeppAccountEmail.asStateFlow()

  private val _isWearableSyncing = MutableStateFlow(false)
  val isWearableSyncing: StateFlow<Boolean> = _isWearableSyncing.asStateFlow()

  private val _lastWearableSyncResult = MutableStateFlow<WearableSyncResult?>(null)
  val lastWearableSyncResult: StateFlow<WearableSyncResult?> = _lastWearableSyncResult.asStateFlow()

  private val _wearableActivities = MutableStateFlow<Map<String, DailyActivityData>>(
    mapOf(
      "profile_doug" to wearableService.getDailyActivityData("profile_doug"),
      "profile_maya" to wearableService.getDailyActivityData("profile_maya"),
      "profile_kwame" to wearableService.getDailyActivityData("profile_kwame")
    )
  )
  val wearableActivities: StateFlow<Map<String, DailyActivityData>> = _wearableActivities.asStateFlow()

  private val _wearableHeartRates = MutableStateFlow<Map<String, HeartRateData>>(
    mapOf(
      "profile_doug" to wearableService.getHeartRateData("profile_doug"),
      "profile_maya" to wearableService.getHeartRateData("profile_maya"),
      "profile_kwame" to wearableService.getHeartRateData("profile_kwame")
    )
  )
  val wearableHeartRates: StateFlow<Map<String, HeartRateData>> = _wearableHeartRates.asStateFlow()

  private val _zeppBiometrics = MutableStateFlow<Map<String, ZeppBiometrics>>(
    mapOf(
      "profile_doug" to zeppWearableService.getZeppBiometrics("profile_doug"),
      "profile_maya" to zeppWearableService.getZeppBiometrics("profile_maya"),
      "profile_kwame" to zeppWearableService.getZeppBiometrics("profile_kwame")
    )
  )
  val zeppBiometrics: StateFlow<Map<String, ZeppBiometrics>> = _zeppBiometrics.asStateFlow()

  init {
    if (database != null) {
      scope.launch {
        // Restore saved preferences immediately upon initialization
        val savedProfileId = database.appPreferenceDao().getPreference("current_profile_id")
        if (savedProfileId != null && savedProfileId.isNotBlank()) {
          _currentProfileId.value = savedProfileId
        }

        val savedUserProfile = database.appPreferenceDao().getPreference("user_profile")
        if (savedUserProfile != null && savedUserProfile.isNotBlank()) {
          _userProfile.value = deserializeUserProfile(savedUserProfile)
        }

        val savedTier = database.appPreferenceDao().getPreference("subscription_tier")
        if (savedTier != null && savedTier.isNotBlank()) {
          runCatching { SubscriptionTier.valueOf(savedTier) }.getOrNull()?.let {
            _subscriptionTier.value = it
          }
        }

        val savedSubDetails = database.appPreferenceDao().getPreference("subscription_details")
        if (savedSubDetails != null && savedSubDetails.isNotBlank()) {
          _subscriptionDetails.value = deserializeSubscriptionDetails(savedSubDetails)
        }

        val savedAppSettings = database.appPreferenceDao().getPreference("app_settings")
        if (savedAppSettings != null && savedAppSettings.isNotBlank()) {
          _appSettings.value = deserializeAppSettings(savedAppSettings)
        }

        val savedAiQueries = database.appPreferenceDao().getPreference("daily_ai_queries_count")
        if (savedAiQueries != null && savedAiQueries.isNotBlank()) {
          _dailyAiQueriesCount.value = savedAiQueries.toIntOrNull() ?: 0
        }

        val savedOcrScans = database.appPreferenceDao().getPreference("monthly_ocr_scans_count")
        if (savedOcrScans != null && savedOcrScans.isNotBlank()) {
          _monthlyOcrScansCount.value = savedOcrScans.toIntOrNull() ?: 0
        }

        // Pre-populate Room database if empty for offline-first resilience
        val allSeedTimeline = initialDougTimeline() + initialMayaTimeline() + initialKwameTimeline()
        val allSeedMeds = initialDougMeds() + initialMayaMeds() + initialKwameMeds()
        val allSeedAudit = initialAuditLogs()
        val allSeedProfiles = initialProfilesList()
        val allSeedReminders = initialReminders() + initialMayaReminders() + initialKwameReminders()
        val allSeedGrants = initialGrants() + initialMayaGrants() + initialKwameGrants()
        val allSeedPayments = initialPaymentHistoryList()

        val hasSeededMultiProfile = database.appPreferenceDao().getPreference("multi_profile_seed_v1") == "true"
        if (!hasSeededMultiProfile) {
          if (database.timelineDao().getCount() == 0) {
            database.timelineDao().insertAll(allSeedTimeline.map { TimelineItemEntity.fromDomain(it) })
          } else {
            // Ensure Maya and Kwame records are inserted if database was initialized with only Doug's
            database.timelineDao().insertAll(
              (initialMayaTimeline() + initialKwameTimeline()).map { TimelineItemEntity.fromDomain(it) }
            )
          }

          if (database.medicationDao().getCount() == 0) {
            database.medicationDao().insertAll(allSeedMeds.map { MedicationEntity.fromDomain(it) })
          } else {
            // Ensure Maya and Kwame medications are inserted if database was initialized with only Doug's
            database.medicationDao().insertAll(
              (initialMayaMeds() + initialKwameMeds()).map { MedicationEntity.fromDomain(it) }
            )
          }
          database.appPreferenceDao().setPreference(AppPreferenceEntity("multi_profile_seed_v1", "true"))
        } else {
          if (database.timelineDao().getCount() == 0) {
            database.timelineDao().insertAll(allSeedTimeline.map { TimelineItemEntity.fromDomain(it) })
          }
          if (database.medicationDao().getCount() == 0) {
            database.medicationDao().insertAll(allSeedMeds.map { MedicationEntity.fromDomain(it) })
          }
        }

        if (database.auditDao().getCount() == 0) {
          database.auditDao().insertAll(allSeedAudit.map { AuditLogEntity.fromDomain(it) })
        }

        if (database.patientProfileDao().getCount() == 0) {
          database.patientProfileDao().insertAll(allSeedProfiles.map { PatientProfileEntity.fromDomain(it) })
        }

        if (database.reminderDao().getCount() == 0) {
          database.reminderDao().insertAll(allSeedReminders.map { MedicationReminderEntity.fromDomain(it) })
        }

        if (database.careGrantDao().getCount() == 0) {
          database.careGrantDao().insertAll(allSeedGrants.map { CareGrantEntity.fromDomain(it) })
        }

        if (database.paymentDao().getCount() == 0) {
          database.paymentDao().insertAll(allSeedPayments.map { PaymentTransactionEntity.fromDomain(it) })
        }

        if (database.chatDao().getCount() == 0) {
          database.chatDao().insertAll(initialChatHistory().map { ChatMessageEntity.fromDomain(it) })
        }

        val allSeedSymptoms = initialDougSymptoms() + initialMayaSymptoms() + initialKwameSymptoms()
        if (database.symptomDao().getCount() == 0) {
          database.symptomDao().insertAll(allSeedSymptoms.map { SymptomEntity.fromDomain(it) })
        }

        // Subscribe to Room Flow queries for local persistence
        launch {
          database.timelineDao().getAllTimelineItems().collect { entities ->
            if (entities.isNotEmpty()) {
              _timelineItems.value = entities.map { it.toDomain() }
            }
          }
        }
        launch {
          database.medicationDao().getAllMedications().collect { entities ->
            if (entities.isNotEmpty()) {
              _medications.value = entities.map { it.toDomain() }
            }
          }
        }
        launch {
          database.auditDao().getAllAuditLogs().collect { entities ->
            if (entities.isNotEmpty()) {
              _auditLogs.value = entities.map { it.toDomain() }
            }
          }
        }
        launch {
          database.patientProfileDao().getAllProfiles().collect { entities ->
            if (entities.isNotEmpty()) {
              _profiles.value = entities.map { it.toDomain() }
            }
          }
        }
        launch {
          database.reminderDao().getAllReminders().collect { entities ->
            if (entities.isNotEmpty()) {
              _reminders.value = entities.map { it.toDomain() }
            }
          }
        }
        launch {
          database.careGrantDao().getAllCareGrants().collect { entities ->
            if (entities.isNotEmpty()) {
              _careGrants.value = entities.map { it.toDomain() }
            }
          }
        }
        launch {
          database.paymentDao().getAllTransactions().collect { entities ->
            if (entities.isNotEmpty()) {
              _paymentHistory.value = entities.map { it.toDomain() }
            }
          }
        }
        launch {
          database.chatDao().getAllMessages().collect { entities ->
            if (entities.isNotEmpty()) {
              _chatMessages.value = entities.map { it.toDomain() }
            }
          }
        }
        launch {
          database.symptomDao().getAllSymptoms().collect { entities ->
            if (entities.isNotEmpty()) {
              _symptoms.value = entities.map { it.toDomain() }
            }
          }
        }
      }
    }
  }

  private fun persistPreference(key: String, value: String) {
    database?.let { db ->
      scope.launch {
        db.appPreferenceDao().setPreference(AppPreferenceEntity(key, value))
      }
    }
  }

  private fun serializeUserProfile(u: UserProfile): String {
    return try {
      val json = org.json.JSONObject()
      json.put("id", u.id)
      json.put("fullName", u.fullName)
      json.put("email", u.email)
      json.put("phone", u.phone)
      json.put("biometricLockEnabled", u.biometricLockEnabled)
      json.put("hipaaConsentSigned", u.hipaaConsentSigned)
      json.put("hipaaConsentDate", u.hipaaConsentDate)
      json.put("emergencyMedicalIdUnlocked", u.emergencyMedicalIdUnlocked)
      json.put("hardwareEnclaveEncryption", u.hardwareEnclaveEncryption)
      json.put("emergencyContactName", u.emergencyContactName)
      json.put("emergencyContactPhone", u.emergencyContactPhone)

      val units = org.json.JSONObject()
      units.put("unitSystem", u.unitPreferences.unitSystem.name)
      units.put("glucoseUnit", u.unitPreferences.glucoseUnit.name)
      units.put("temperatureUnit", u.unitPreferences.temperatureUnit.name)
      units.put("weightUnit", u.unitPreferences.weightUnit.name)
      units.put("heightUnit", u.unitPreferences.heightUnit.name)
      units.put("cholesterolUnit", u.unitPreferences.cholesterolUnit.name)
      json.put("unitPreferences", units)
      json.toString()
    } catch (e: Throwable) {
      """{"id":"${u.id}","fullName":"${u.fullName}","email":"${u.email}","phone":"${u.phone}","biometricLockEnabled":${u.biometricLockEnabled},"hipaaConsentSigned":${u.hipaaConsentSigned},"hipaaConsentDate":"${u.hipaaConsentDate}","emergencyMedicalIdUnlocked":${u.emergencyMedicalIdUnlocked},"hardwareEnclaveEncryption":${u.hardwareEnclaveEncryption},"emergencyContactName":"${u.emergencyContactName}","emergencyContactPhone":"${u.emergencyContactPhone}","unitPreferences":{"unitSystem":"${u.unitPreferences.unitSystem.name}","glucoseUnit":"${u.unitPreferences.glucoseUnit.name}","temperatureUnit":"${u.unitPreferences.temperatureUnit.name}","weightUnit":"${u.unitPreferences.weightUnit.name}","heightUnit":"${u.unitPreferences.heightUnit.name}","cholesterolUnit":"${u.unitPreferences.cholesterolUnit.name}"}}"""
    }
  }

  private fun deserializeUserProfile(str: String): UserProfile {
    return try {
      val json = org.json.JSONObject(str)
      val unitsJson = json.optJSONObject("unitPreferences")
      val units = if (unitsJson != null) {
        UnitPreferences(
          unitSystem = runCatching { UnitSystem.valueOf(unitsJson.optString("unitSystem")) }.getOrDefault(UnitSystem.METRIC_SI),
          glucoseUnit = runCatching { GlucoseUnit.valueOf(unitsJson.optString("glucoseUnit")) }.getOrDefault(GlucoseUnit.MMOL_L),
          temperatureUnit = runCatching { TemperatureUnit.valueOf(unitsJson.optString("temperatureUnit")) }.getOrDefault(TemperatureUnit.CELSIUS),
          weightUnit = runCatching { WeightUnit.valueOf(unitsJson.optString("weightUnit")) }.getOrDefault(WeightUnit.KG),
          heightUnit = runCatching { HeightUnit.valueOf(unitsJson.optString("heightUnit")) }.getOrDefault(HeightUnit.CM),
          cholesterolUnit = runCatching { CholesterolUnit.valueOf(unitsJson.optString("cholesterolUnit")) }.getOrDefault(CholesterolUnit.MMOL_L)
        )
      } else UnitPreferences()

      UserProfile(
        id = json.optString("id", "user_primary"),
        fullName = json.optString("fullName", "Doug Adarkwah"),
        email = json.optString("email", "abydouglas@gmail.com"),
        phone = json.optString("phone", "+1 (555) 234-8901"),
        biometricLockEnabled = json.optBoolean("biometricLockEnabled", true),
        hipaaConsentSigned = json.optBoolean("hipaaConsentSigned", true),
        hipaaConsentDate = json.optString("hipaaConsentDate", "May 14, 2026"),
        emergencyMedicalIdUnlocked = json.optBoolean("emergencyMedicalIdUnlocked", true),
        hardwareEnclaveEncryption = json.optBoolean("hardwareEnclaveEncryption", true),
        emergencyContactName = json.optString("emergencyContactName", "Elena Adarkwah (Spouse)"),
        emergencyContactPhone = json.optString("emergencyContactPhone", "+1 (555) 345-6789"),
        unitPreferences = units
      )
    } catch (e: Exception) {
      UserProfile()
    }
  }

  private fun serializeSubscriptionDetails(s: SubscriptionDetails): String {
    return try {
      val json = org.json.JSONObject()
      json.put("tier", s.tier.name)
      json.put("billingCycle", s.billingCycle.name)
      json.put("status", s.status)
      json.put("renewalDate", s.renewalDate)
      json.put("lastPaymentMethod", s.lastPaymentMethod)
      json.put("lastTransactionId", s.lastTransactionId)
      json.put("autoRenew", s.autoRenew)
      json.toString()
    } catch (e: Throwable) {
      """{"tier":"${s.tier.name}","billingCycle":"${s.billingCycle.name}","status":"${s.status}","renewalDate":"${s.renewalDate}","lastPaymentMethod":"${s.lastPaymentMethod}","lastTransactionId":"${s.lastTransactionId}","autoRenew":${s.autoRenew}}"""
    }
  }

  private fun deserializeSubscriptionDetails(str: String): SubscriptionDetails {
    return try {
      val json = org.json.JSONObject(str)
      SubscriptionDetails(
        tier = runCatching { SubscriptionTier.valueOf(json.optString("tier")) }.getOrDefault(SubscriptionTier.PREMIUM),
        billingCycle = runCatching { BillingCycle.valueOf(json.optString("billingCycle")) }.getOrDefault(BillingCycle.ANNUAL),
        status = json.optString("status", "ACTIVE"),
        renewalDate = json.optString("renewalDate", "Oct 26, 2027"),
        lastPaymentMethod = json.optString("lastPaymentMethod", "Google Pay (abydouglas@gmail.com)"),
        lastTransactionId = json.optString("lastTransactionId", "TXN-VTL-2026-98124"),
        autoRenew = json.optBoolean("autoRenew", true)
      )
    } catch (e: Exception) {
      SubscriptionDetails(
        tier = SubscriptionTier.PREMIUM,
        billingCycle = BillingCycle.ANNUAL,
        status = "ACTIVE",
        renewalDate = "Oct 26, 2027",
        lastPaymentMethod = "Google Pay (abydouglas@gmail.com)",
        lastTransactionId = "TXN-VTL-2026-98124",
        autoRenew = true
      )
    }
  }

  private fun serializeAppSettings(s: AppSettings): String {
    return try {
      val json = org.json.JSONObject()
      json.put("autoLockTimeout", s.autoLockTimeout)
      json.put("autoOcrProcessing", s.autoOcrProcessing)
      json.put("drugConflictAlerts", s.drugConflictAlerts)
      json.put("fhirSyncFrequency", s.fhirSyncFrequency)
      json.put("refillReminders", s.refillReminders)
      json.put("labResultAlerts", s.labResultAlerts)
      json.put("careGrantAlerts", s.careGrantAlerts)
      json.put("highContrastTheme", s.highContrastTheme)
      json.toString()
    } catch (e: Throwable) {
      """{"autoLockTimeout":"${s.autoLockTimeout}","autoOcrProcessing":${s.autoOcrProcessing},"drugConflictAlerts":${s.drugConflictAlerts},"fhirSyncFrequency":"${s.fhirSyncFrequency}","refillReminders":${s.refillReminders},"labResultAlerts":${s.labResultAlerts},"careGrantAlerts":${s.careGrantAlerts},"highContrastTheme":${s.highContrastTheme}}"""
    }
  }

  private fun deserializeAppSettings(str: String): AppSettings {
    return try {
      val json = org.json.JSONObject(str)
      AppSettings(
        autoLockTimeout = json.optString("autoLockTimeout", "5 minutes"),
        autoOcrProcessing = json.optBoolean("autoOcrProcessing", true),
        drugConflictAlerts = json.optBoolean("drugConflictAlerts", true),
        fhirSyncFrequency = json.optString("fhirSyncFrequency", "Real-Time"),
        refillReminders = json.optBoolean("refillReminders", true),
        labResultAlerts = json.optBoolean("labResultAlerts", true),
        careGrantAlerts = json.optBoolean("careGrantAlerts", true),
        highContrastTheme = json.optBoolean("highContrastTheme", false)
      )
    } catch (e: Exception) {
      AppSettings()
    }
  }

  fun updateAppSettings(transform: (AppSettings) -> AppSettings) {
    _appSettings.update(transform)
    persistPreference("app_settings", serializeAppSettings(_appSettings.value))
  }

  fun canExecuteAiQuery(): Boolean {
    if (_subscriptionTier.value != SubscriptionTier.FREE) return true
    return _dailyAiQueriesCount.value < SubscriptionTier.FREE.aiQueriesPerDay
  }

  fun recordAiQueryUsage(): Boolean {
    if (_subscriptionTier.value != SubscriptionTier.FREE) return true
    if (_dailyAiQueriesCount.value >= SubscriptionTier.FREE.aiQueriesPerDay) return false
    _dailyAiQueriesCount.update { it + 1 }
    persistPreference("daily_ai_queries_count", _dailyAiQueriesCount.value.toString())
    return true
  }

  fun canExecuteOcrScan(): Boolean {
    if (_subscriptionTier.value != SubscriptionTier.FREE) return true
    return _monthlyOcrScansCount.value < SubscriptionTier.FREE.ocrScansPerMonth
  }

  fun recordOcrScanUsage(): Boolean {
    if (_subscriptionTier.value != SubscriptionTier.FREE) return true
    if (_monthlyOcrScansCount.value >= SubscriptionTier.FREE.ocrScansPerMonth) return false
    _monthlyOcrScansCount.update { it + 1 }
    persistPreference("monthly_ocr_scans_count", _monthlyOcrScansCount.value.toString())
    return true
  }

  fun switchProfile(profileId: String) {
    _currentProfileId.value = profileId
    persistPreference("current_profile_id", profileId)
    recordAudit("PROFILE_SWITCH", "User switched active profile to: $profileId")
  }

  fun updateUserProfile(profile: UserProfile) {
    _userProfile.value = profile
    persistPreference("user_profile", serializeUserProfile(profile))
    recordAudit("USER_PROFILE_UPDATED", "Account details updated for: ${profile.fullName}")
  }

  fun setBiometricLock(enabled: Boolean) {
    _userProfile.update { it.copy(biometricLockEnabled = enabled) }
    persistPreference("user_profile", serializeUserProfile(_userProfile.value))
    recordAudit("SECURITY_PREFERENCE_CHANGED", "Biometric lock set to: $enabled")
  }

  fun setEmergencyMedicalIdUnlocked(enabled: Boolean) {
    _userProfile.update { it.copy(emergencyMedicalIdUnlocked = enabled) }
    persistPreference("user_profile", serializeUserProfile(_userProfile.value))
    recordAudit("SECURITY_PREFERENCE_CHANGED", "Emergency lock-screen access set to: $enabled")
  }

  fun updateUnitPreferences(preferences: UnitPreferences) {
    _userProfile.update { it.copy(unitPreferences = preferences) }
    persistPreference("user_profile", serializeUserProfile(_userProfile.value))
    recordAudit("UNIT_PREFERENCE_CHANGED", "Clinical unit preference updated: system=${preferences.unitSystem.displayName}, glucose=${preferences.glucoseUnit.label}, temp=${preferences.temperatureUnit.label}, weight=${preferences.weightUnit.label}")
  }

  fun setUnitSystem(system: UnitSystem) {
    val newPrefs = when (system) {
      UnitSystem.US_CUSTOMARY -> UnitPreferences.createUsCustomary()
      UnitSystem.METRIC_SI -> UnitPreferences.createMetricSi()
    }
    updateUnitPreferences(newPrefs)
  }

  fun setGlucoseUnit(unit: GlucoseUnit) {
    _userProfile.update { it.copy(unitPreferences = it.unitPreferences.copy(glucoseUnit = unit)) }
    persistPreference("user_profile", serializeUserProfile(_userProfile.value))
    recordAudit("UNIT_PREFERENCE_CHANGED", "Blood glucose unit preference set to ${unit.label}")
  }

  fun setTemperatureUnit(unit: TemperatureUnit) {
    _userProfile.update { it.copy(unitPreferences = it.unitPreferences.copy(temperatureUnit = unit)) }
    persistPreference("user_profile", serializeUserProfile(_userProfile.value))
    recordAudit("UNIT_PREFERENCE_CHANGED", "Body temperature unit preference set to ${unit.label}")
  }

  fun setWeightUnit(unit: WeightUnit) {
    _userProfile.update { it.copy(unitPreferences = it.unitPreferences.copy(weightUnit = unit)) }
    persistPreference("user_profile", serializeUserProfile(_userProfile.value))
    recordAudit("UNIT_PREFERENCE_CHANGED", "Body weight unit preference set to ${unit.label}")
  }

  fun setHeightUnit(unit: HeightUnit) {
    _userProfile.update { it.copy(unitPreferences = it.unitPreferences.copy(heightUnit = unit)) }
    persistPreference("user_profile", serializeUserProfile(_userProfile.value))
    recordAudit("UNIT_PREFERENCE_CHANGED", "Height unit preference set to ${unit.label}")
  }

  fun setCholesterolUnit(unit: CholesterolUnit) {
    _userProfile.update { it.copy(unitPreferences = it.unitPreferences.copy(cholesterolUnit = unit)) }
    persistPreference("user_profile", serializeUserProfile(_userProfile.value))
    recordAudit("UNIT_PREFERENCE_CHANGED", "Cholesterol unit preference set to ${unit.label}")
  }

  fun addPatientProfile(profile: PatientProfile) {
    _profiles.update { it + profile }
    database?.let { db ->
      scope.launch {
        db.patientProfileDao().insert(PatientProfileEntity.fromDomain(profile))
      }
    }
    recordAudit("PATIENT_PROFILE_CREATED", "Added patient profile '${profile.name}' (${profile.relationship})")
  }

  fun updatePatientProfile(updated: PatientProfile) {
    _profiles.update { list ->
      list.map { if (it.id == updated.id) updated else it }
    }
    database?.let { db ->
      scope.launch {
        db.patientProfileDao().insert(PatientProfileEntity.fromDomain(updated))
      }
    }
    recordAudit("PATIENT_PROFILE_UPDATED", "Updated clinical profile for '${updated.name}'")
  }

  fun deletePatientProfile(profileId: String) {
    val profileToDelete = _profiles.value.firstOrNull { it.id == profileId }
    if (profileToDelete != null && !profileToDelete.isPrimary) {
      _profiles.update { list -> list.filterNot { it.id == profileId } }
      _timelineItems.update { list -> list.filterNot { it.profileId == profileId } }
      _medications.update { list -> list.filterNot { it.profileId == profileId } }
      _reminders.update { list -> list.filterNot { it.profileId == profileId } }
      _careGrants.update { list -> list.filterNot { it.profileId == profileId } }
      _chatMessages.update { list -> list.filterNot { it.profileId == profileId } }
      _symptoms.update { list -> list.filterNot { it.profileId == profileId } }
      database?.let { db ->
        scope.launch {
          db.patientProfileDao().deleteById(profileId)
          db.timelineDao().deleteByProfileId(profileId)
          db.medicationDao().deleteByProfileId(profileId)
          db.reminderDao().deleteByProfileId(profileId)
          db.careGrantDao().deleteByProfileId(profileId)
          db.chatDao().deleteByProfileId(profileId)
          db.symptomDao().deleteByProfileId(profileId)
        }
      }
      if (_currentProfileId.value == profileId) {
        val nextId = _profiles.value.firstOrNull()?.id ?: "profile_doug"
        _currentProfileId.value = nextId
        persistPreference("current_profile_id", nextId)
      }
      recordAudit("PATIENT_PROFILE_DELETED", "Deleted patient profile: ${profileToDelete.name}")
    }
  }

  fun setSubscriptionTier(tier: SubscriptionTier) {
    _subscriptionTier.value = tier
    _subscriptionDetails.update { it.copy(tier = tier) }
    persistPreference("subscription_tier", tier.name)
    persistPreference("subscription_details", serializeSubscriptionDetails(_subscriptionDetails.value))
    recordAudit("SUBSCRIPTION_UPDATED", "Plan changed to: ${tier.name}")
  }

  fun processPaymentAndActivateSubscription(
    tier: SubscriptionTier,
    billingCycle: BillingCycle,
    paymentMethod: String,
    amount: Double = if (billingCycle == BillingCycle.ANNUAL) (tier.annualPricePerMonth * 12) else tier.monthlyPrice
  ): PaymentTransaction {
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
    val nextCal = Calendar.getInstance().apply {
      if (billingCycle == BillingCycle.ANNUAL) add(Calendar.YEAR, 1) else add(Calendar.MONTH, 1)
    }
    val renewalStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(nextCal.time)
    val txnId = "TXN-VTL-${(10000..99999).random()}"
    val invoiceNum = "INV-VTL-${(1000..9999).random()}"

    val transaction = PaymentTransaction(
      id = txnId,
      date = dateStr,
      tier = tier,
      billingCycle = billingCycle,
      amountPaid = amount,
      paymentMethod = paymentMethod,
      status = "COMPLETED",
      invoiceNumber = invoiceNum
    )

    _paymentHistory.update { listOf(transaction) + it }
    _subscriptionTier.value = tier
    val updatedDetails = SubscriptionDetails(
      tier = tier,
      billingCycle = billingCycle,
      status = "ACTIVE",
      renewalDate = renewalStr,
      lastPaymentMethod = paymentMethod,
      lastTransactionId = txnId,
      autoRenew = true
    )
    _subscriptionDetails.value = updatedDetails
    persistPreference("subscription_tier", tier.name)
    persistPreference("subscription_details", serializeSubscriptionDetails(updatedDetails))
    database?.let { db ->
      scope.launch {
        db.paymentDao().insert(PaymentTransactionEntity.fromDomain(transaction))
      }
    }

    recordAudit("PAYMENT_PROCESSED", "Processed payment of $$amount via $paymentMethod for plan ${tier.displayName} (Txn: $txnId)")
    recordAudit("SUBSCRIPTION_ACTIVATED", "Activated ${tier.displayName} (${billingCycle.label})")

    return transaction
  }

  fun cancelSubscriptionAutoRenew() {
    _subscriptionDetails.update { it.copy(autoRenew = false, status = "CANCELED_AT_PERIOD_END") }
    persistPreference("subscription_details", serializeSubscriptionDetails(_subscriptionDetails.value))
    recordAudit("SUBSCRIPTION_AUTO_RENEW_CANCELED", "Auto-renewal turned off for ${_subscriptionTier.value.displayName}")
  }

  fun resumeSubscriptionAutoRenew() {
    _subscriptionDetails.update { it.copy(autoRenew = true, status = "ACTIVE") }
    persistPreference("subscription_details", serializeSubscriptionDetails(_subscriptionDetails.value))
    recordAudit("SUBSCRIPTION_AUTO_RENEW_RESUMED", "Auto-renewal resumed for ${_subscriptionTier.value.displayName}")
  }

  fun connectGoogleFit(email: String = "abydouglas@gmail.com") {
    _googleFitAccountEmail.value = email
    _googleFitState.value = GoogleFitConnectionState.AUTHENTICATED_CONNECTED
    recordAudit("GOOGLE_FIT_AUTH_CONNECT", "Authenticated Google Fit API session for $email (Scopes: ACTIVITY_READ, BODY_READ, HEART_RATE_READ)")
  }

  fun disconnectGoogleFit() {
    _googleFitState.value = GoogleFitConnectionState.DISCONNECTED
    _wearableDevices.update { list ->
      list.map { if (it.platform == WearablePlatform.GOOGLE_FIT) it.copy(isConnected = false) else it }
    }
    recordAudit("GOOGLE_FIT_REVOKED", "Disconnected Google Fit API session and revoked active bearer token")
  }

  fun connectZepp(email: String = "abydouglas@zepp.life") {
    _zeppAccountEmail.value = email
    _zeppState.value = ZeppConnectionState.AUTHENTICATED_CONNECTED
    recordAudit("ZEPP_AUTH_CONNECT", "Authenticated Zepp Health Open Platform session for $email (Scopes: ACTIVITY, HEART_RATE, PAI, BIOTRACKER, SLEEP)")
  }

  fun disconnectZepp() {
    _zeppState.value = ZeppConnectionState.DISCONNECTED
    _wearableDevices.update { list ->
      list.map { if (it.platform == WearablePlatform.ZEPP_HEALTH) it.copy(isConnected = false) else it }
    }
    recordAudit("ZEPP_AUTH_REVOKED", "Disconnected Zepp Health session and revoked active authorization token")
  }

  fun toggleWearableDeviceConnection(deviceId: String) {
    _wearableDevices.update { list ->
      list.map { dev ->
        if (dev.id == deviceId) {
          val nextConnected = !dev.isConnected
          recordAudit(
            if (nextConnected) "WEARABLE_DEVICE_CONNECTED" else "WEARABLE_DEVICE_DISCONNECTED",
            "${if (nextConnected) "Connected" else "Disconnected"} device ${dev.name} (${dev.manufacturer} - ${dev.platform.displayName})"
          )
          dev.copy(
            isConnected = nextConnected,
            lastSyncedTimestamp = if (nextConnected) System.currentTimeMillis() else dev.lastSyncedTimestamp
          )
        } else dev
      }
    }
  }

  fun pairNewWearableDevice(device: WearableDevice) {
    _wearableDevices.update { listOf(device) + it.filter { existing -> existing.id != device.id } }
    val auditAction = if (device.platform == WearablePlatform.ZEPP_HEALTH) "ZEPP_WEARABLE_PAIRED" else "WEARABLE_DEVICE_PAIRED"
    recordAudit(auditAction, "Paired new ${device.type.displayName}: ${device.name} (${device.deviceIdentifier}) via ${device.platform.displayName}")
  }

  suspend fun syncWearableData(profileId: String, deviceId: String? = null): WearableSyncResult {
    _isWearableSyncing.value = true
    try {
      kotlinx.coroutines.delay(650) // Realistic sensor telemetry handshake
      val targetDevice = _wearableDevices.value.firstOrNull { if (deviceId != null) it.id == deviceId else it.isConnected }
        ?: _wearableDevices.value.first()

      val isZepp = targetDevice.platform == WearablePlatform.ZEPP_HEALTH

      val activity = if (isZepp) {
        zeppWearableService.getDailyActivityData(profileId)
      } else {
        wearableService.getDailyActivityData(profileId)
      }

      val heartRate = if (isZepp) {
        zeppWearableService.getHeartRateData(profileId)
      } else {
        wearableService.getHeartRateData(profileId)
      }

      val biometrics = if (isZepp) {
        val bio = zeppWearableService.getZeppBiometrics(profileId)
        _zeppBiometrics.update { current -> current + (profileId to bio) }
        bio
      } else null

      _wearableActivities.update { current -> current + (profileId to activity) }
      _wearableHeartRates.update { current -> current + (profileId to heartRate) }

      // Update device last synced timestamp
      _wearableDevices.update { list ->
        list.map { dev ->
          if (dev.id == targetDevice.id) dev.copy(lastSyncedTimestamp = System.currentTimeMillis(), isConnected = true) else dev
        }
      }

      val syncResult = if (isZepp) {
        zeppWearableService.executeSync(profileId, targetDevice)
      } else {
        wearableService.executeSync(profileId, targetDevice)
      }
      _lastWearableSyncResult.value = syncResult

      val auditAction = if (isZepp) "ZEPP_TELEMETRY_SYNC" else "WEARABLE_TELEMETRY_SYNC"
      val auditDesc = if (isZepp && biometrics != null) {
        "Synced ${activity.stepsCount} steps, ${heartRate.restingHeartRateBpm} bpm RHR & ${biometrics.paiScore} PAI from ${targetDevice.name} via Zepp Health"
      } else {
        "Synced ${activity.stepsCount} steps & ${heartRate.restingHeartRateBpm} bpm RHR from ${targetDevice.name} via Google Fit API"
      }

      recordAudit(auditAction, auditDesc)

      return syncResult
    } finally {
      _isWearableSyncing.value = false
    }
  }

  suspend fun syncZeppWearableData(profileId: String, deviceId: String? = null): WearableSyncResult {
    val zeppDevice = _wearableDevices.value.firstOrNull {
      if (deviceId != null) it.id == deviceId else (it.platform == WearablePlatform.ZEPP_HEALTH && it.isConnected)
    } ?: _wearableDevices.value.firstOrNull { it.platform == WearablePlatform.ZEPP_HEALTH }
      ?: zeppWearableService.getDefaultDevices().first()

    return syncWearableData(profileId, zeppDevice.id)
  }

  fun importWearableToTimeline(profileId: String, deviceId: String? = null): List<HealthTimelineItem> {
    val targetDevice = _wearableDevices.value.firstOrNull { if (deviceId != null) it.id == deviceId else it.isConnected }
      ?: _wearableDevices.value.first()

    val isZepp = targetDevice.platform == WearablePlatform.ZEPP_HEALTH

    val activity = _wearableActivities.value[profileId] ?: if (isZepp) {
      zeppWearableService.getDailyActivityData(profileId)
    } else {
      wearableService.getDailyActivityData(profileId)
    }

    val heartRate = _wearableHeartRates.value[profileId] ?: if (isZepp) {
      zeppWearableService.getHeartRateData(profileId)
    } else {
      wearableService.getHeartRateData(profileId)
    }

    val items = if (isZepp) {
      val biometrics = _zeppBiometrics.value[profileId] ?: zeppWearableService.getZeppBiometrics(profileId)
      zeppWearableService.convertToTimelineItems(profileId, targetDevice, activity, heartRate, biometrics)
    } else {
      wearableService.convertToTimelineItems(profileId, targetDevice, activity, heartRate)
    }

    _timelineItems.update { items + it }
    database?.let { db ->
      scope.launch {
        items.forEach { db.timelineDao().insert(TimelineItemEntity.fromDomain(it)) }
      }
    }

    val auditAction = if (isZepp) "ZEPP_DATA_INGEST" else "WEARABLE_DATA_INGEST"
    val auditDesc = if (isZepp) {
      "Ingested 3 clinical observations (LOINC 55423-8 steps, LOINC 8867-4 heart rate, LOINC 80490-6 PAI score) from ${targetDevice.name} via Zepp Health Open Platform"
    } else {
      "Ingested 2 clinical observations (LOINC 55423-8 steps & LOINC 8867-4 heart rate) from ${targetDevice.name} via Google Fit API"
    }

    recordAudit(auditAction, auditDesc)

    return items
  }

  fun importZeppWearableToTimeline(profileId: String, deviceId: String? = null): List<HealthTimelineItem> {
    val zeppDevice = _wearableDevices.value.firstOrNull {
      if (deviceId != null) it.id == deviceId else (it.platform == WearablePlatform.ZEPP_HEALTH && it.isConnected)
    } ?: _wearableDevices.value.firstOrNull { it.platform == WearablePlatform.ZEPP_HEALTH }
      ?: zeppWearableService.getDefaultDevices().first()

    return importWearableToTimeline(profileId, zeppDevice.id)
  }

  fun addTimelineItem(item: HealthTimelineItem) {
    _timelineItems.update { listOf(item) + it }
    database?.let { db ->
      scope.launch {
        db.timelineDao().insert(TimelineItemEntity.fromDomain(item))
      }
    }
    recordAudit("FHIR_RESOURCE_INGEST", "Ingested ${item.category} item '${item.title}' via ${item.provenance.sourceSystem}")
  }

  fun addMedication(med: MedicationItem) {
    _medications.update { listOf(med) + it }
    database?.let { db ->
      scope.launch {
        db.medicationDao().insert(MedicationEntity.fromDomain(med))
      }
    }
    recordAudit("MEDICATION_ADDED", "Added medication '${med.name}' (${med.dosage})")
  }

  fun reconcileMedication(medId: String) {
    _medications.update { list ->
      list.map { if (it.id == medId) it.copy(isReconciled = true) else it }
    }
    database?.let { db ->
      scope.launch {
        db.medicationDao().updateReconciledStatus(medId, true)
      }
    }
    recordAudit("MEDICATION_RECONCILED", "Patient marked medication $medId as verified & reconciled")
  }

  fun addMedicationReminder(reminder: MedicationReminder) {
    _reminders.update { list ->
      val existingIdx = list.indexOfFirst { it.id == reminder.id }
      if (existingIdx >= 0) {
        list.toMutableList().also { it[existingIdx] = reminder }
      } else {
        list + reminder
      }
    }
    database?.let { db ->
      scope.launch {
        db.reminderDao().insert(MedicationReminderEntity.fromDomain(reminder))
      }
    }
    recordAudit("REMINDER_CREATED", "Set daily recurring alert for '${reminder.medicationName}' at ${reminder.formattedTime}")
  }

  fun toggleMedicationReminder(reminderId: String, isEnabled: Boolean) {
    _reminders.update { list ->
      list.map { if (it.id == reminderId) it.copy(isEnabled = isEnabled) else it }
    }
    database?.let { db ->
      scope.launch {
        db.reminderDao().updateEnabled(reminderId, isEnabled)
      }
    }
    recordAudit("REMINDER_TOGGLED", "Reminder $reminderId alert status: ${if (isEnabled) "Active" else "Paused"}")
  }

  fun deleteMedicationReminder(reminderId: String) {
    _reminders.update { list -> list.filterNot { it.id == reminderId } }
    database?.let { db ->
      scope.launch {
        db.reminderDao().deleteById(reminderId)
      }
    }
    recordAudit("REMINDER_DELETED", "Deleted recurring medication reminder $reminderId")
  }

  fun markReminderTaken(reminderId: String, isTaken: Boolean) {
    _reminders.update { list ->
      list.map {
        if (it.id == reminderId) {
          it.copy(isTakenToday = isTaken, lastTakenTimestamp = if (isTaken) System.currentTimeMillis() else null)
        } else it
      }
    }
    database?.let { db ->
      scope.launch {
        db.reminderDao().updateTakenToday(reminderId, isTaken)
      }
    }
    recordAudit("MEDICATION_TAKEN", "Logged medication dose taken for reminder $reminderId")
  }

  fun createCareGrant(recipientName: String, role: String, scope: String, durationLabel: String): CareGrant {
    val id = "grant_${UUID.randomUUID().toString().take(8)}"
    val token = "vtl_share_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val grant = CareGrant(
      id = id,
      profileId = _currentProfileId.value,
      recipientName = recipientName,
      recipientRole = role,
      scope = scope,
      durationLabel = durationLabel,
      expiresAt = "Exp in $durationLabel",
      tokenKey = token,
      isActive = true
    )
    _careGrants.update { listOf(grant) + it }
    database?.let { db ->
      this@VitalithRepository.scope.launch {
        db.careGrantDao().insert(CareGrantEntity.fromDomain(grant))
      }
    }
    recordAudit("SECURE_SHARE_CREATED", "Generated $scope share grant for $recipientName ($role), expires in $durationLabel")
    return grant
  }

  fun exportFhirBundleJson(): String {
    val curProfileId = _currentProfileId.value
    val curProfile = _profiles.value.firstOrNull { it.id == curProfileId } ?: _profiles.value.first()
    val curMeds = _medications.value.filter { it.profileId == curProfileId }
    val curTimeline = _timelineItems.value.filter { it.profileId == curProfileId }

    val builder = StringBuilder()
    builder.append("{\n")
    builder.append("  \"resourceType\": \"Bundle\",\n")
    builder.append("  \"id\": \"vitalith-export-${UUID.randomUUID().toString().take(8)}\",\n")
    builder.append("  \"meta\": { \"lastUpdated\": \"${currentTime()}\" },\n")
    builder.append("  \"type\": \"collection\",\n")
    builder.append("  \"entry\": [\n")

    // Patient Resource
    builder.append("    {\n")
    builder.append("      \"fullUrl\": \"urn:uuid:patient-${curProfile.id}\",\n")
    builder.append("      \"resource\": {\n")
    builder.append("        \"resourceType\": \"Patient\",\n")
    builder.append("        \"id\": \"${curProfile.id}\",\n")
    builder.append("        \"identifier\": [{ \"system\": \"http://vitalith.health/mrn\", \"value\": \"${curProfile.mrn}\" }],\n")
    builder.append("        \"name\": [{ \"use\": \"official\", \"text\": \"${curProfile.name}\" }],\n")
    builder.append("        \"gender\": \"${curProfile.gender.lowercase()}\",\n")
    builder.append("        \"birthDate\": \"${curProfile.dob.take(10)}\"\n")
    builder.append("      }\n")
    builder.append("    }")

    // Medications
    curMeds.forEach { med ->
      builder.append(",\n    {\n")
      builder.append("      \"fullUrl\": \"urn:uuid:med-${med.id}\",\n")
      builder.append("      \"resource\": {\n")
      builder.append("        \"resourceType\": \"MedicationStatement\",\n")
      builder.append("        \"id\": \"${med.id}\",\n")
      builder.append("        \"status\": \"${if (med.status == ItemStatus.ACTIVE) "active" else "stopped"}\",\n")
      builder.append("        \"medicationCodeableConcept\": { \"text\": \"${med.name}\" },\n")
      builder.append("        \"dosage\": [{ \"text\": \"${med.dosage} ${med.frequency}\" }],\n")
      builder.append("        \"reasonCode\": [{ \"text\": \"${med.indication}\" }]\n")
      builder.append("      }\n")
      builder.append("    }")
    }

    // Timeline Items
    curTimeline.forEach { item ->
      val fhirType = when (item.category) {
        TimelineCategory.LAB -> "Observation"
        TimelineCategory.CONDITION -> "Condition"
        TimelineCategory.ENCOUNTER -> "Encounter"
        TimelineCategory.IMMUNIZATION -> "Immunization"
        TimelineCategory.PROCEDURE -> "Procedure"
        else -> "Observation"
      }
      builder.append(",\n    {\n")
      builder.append("      \"fullUrl\": \"urn:uuid:item-${item.id}\",\n")
      builder.append("      \"resource\": {\n")
      builder.append("        \"resourceType\": \"$fhirType\",\n")
      builder.append("        \"id\": \"${item.id}\",\n")
      builder.append("        \"code\": { \"text\": \"${item.title.replace("\"", "\\\"")}\" },\n")
      builder.append("        \"effectiveDateTime\": \"${item.date}\",\n")
      builder.append("        \"performer\": [{ \"display\": \"${item.providerOrFacility.replace("\"", "\\\"")}\" }],\n")
      builder.append("        \"note\": [{ \"text\": \"${item.clinicalNotes.replace("\"", "\\\"").replace("\n", " ")}\" }]\n")
      val primaryMetricValue = item.keyValues["Value"] ?: item.keyValues["Total Chol"] ?: item.keyValues.values.firstOrNull()
      if (primaryMetricValue != null) {
        val ucumQuantity = com.example.util.UcumUnitConverter.createComputableQuantity(primaryMetricValue, item.title)
        if (ucumQuantity.value > 0.0) {
          builder.append(",\n        \"valueQuantity\": {\n")
          builder.append("          \"value\": ${ucumQuantity.value},\n")
          builder.append("          \"unit\": \"${ucumQuantity.unit}\",\n")
          builder.append("          \"system\": \"${ucumQuantity.system}\",\n")
          builder.append("          \"code\": \"${ucumQuantity.code}\"\n")
          builder.append("        }\n")
        }
      }
      builder.append("      }\n")
      builder.append("    }")
    }

    builder.append("\n  ]\n}")
    recordAudit("FHIR_BUNDLE_EXPORT", "Compiled HL7 FHIR R4 JSON Bundle (${curTimeline.size + curMeds.size + 1} resources for ${curProfile.name})")
    return builder.toString()
  }

  fun exportMedicationsCsv(): String {
    val curProfileId = _currentProfileId.value
    val curProfile = _profiles.value.firstOrNull { it.id == curProfileId } ?: _profiles.value.first()
    val curMeds = _medications.value.filter { it.profileId == curProfileId }

    val sb = StringBuilder()
    sb.append("id,profileId,name,dosage,frequency,prescriber,indication,status,isReconciled,source\n")
    curMeds.forEach { m ->
      sb.append("\"${m.id}\",\"${m.profileId}\",\"${m.name}\",\"${m.dosage}\",\"${m.frequency}\",\"${m.prescriber}\",\"${m.indication}\",\"${m.status.name}\",\"${m.isReconciled}\",\"${m.provenance.sourceSystem}\"\n")
    }
    recordAudit("CSV_MEDS_EXPORT", "Exported ${curMeds.size} medications for ${curProfile.name} to CSV format")
    return sb.toString()
  }

  fun exportAuditTrailJson(): String {
    val sb = StringBuilder()
    sb.append("[\n")
    val logs = _auditLogs.value
    logs.forEachIndexed { i, log ->
      sb.append("  {\n")
      sb.append("    \"id\": \"${log.id}\",\n")
      sb.append("    \"timestamp\": \"${log.timestamp}\",\n")
      sb.append("    \"actor\": \"${log.actor}\",\n")
      sb.append("    \"action\": \"${log.action}\",\n")
      sb.append("    \"resource\": \"${log.resource.replace("\"", "\\\"")}\",\n")
      sb.append("    \"status\": \"${log.status}\",\n")
      sb.append("    \"ipAddress\": \"${log.ipAddress}\"\n")
      sb.append("  }${if (i < logs.size - 1) "," else ""}\n")
    }
    sb.append("]\n")
    recordAudit("AUDIT_TRAIL_EXPORT", "Exported immutable cryptographic audit trail (${logs.size} entries)")
    return sb.toString()
  }

  fun importFhirBundle(jsonString: String): Pair<Int, String> {
    return try {
      val jsonObj = org.json.JSONObject(jsonString)
      val entries = jsonObj.optJSONArray("entry") ?: return Pair(0, "No entries found in FHIR bundle")
      var importedCount = 0

      for (i in 0 until entries.length()) {
        val entry = entries.getJSONObject(i)
        val resource = entry.optJSONObject("resource") ?: continue
        val resourceType = resource.optString("resourceType", "Observation")
        val id = resource.optString("id", UUID.randomUUID().toString().take(8))

        if (resourceType == "MedicationStatement") {
          val medName = resource.optJSONObject("medicationCodeableConcept")?.optString("text")
            ?: resource.optString("medicationCodeableConcept", "Imported Medication")
          val dosageText = resource.optJSONArray("dosage")?.optJSONObject(0)?.optString("text", "As prescribed") ?: "As prescribed"
          val reason = resource.optJSONArray("reasonCode")?.optJSONObject(0)?.optString("text", "Maintenance") ?: "Maintenance"

          val newMed = MedicationItem(
            id = "med_imp_${UUID.randomUUID().toString().take(6)}",
            profileId = _currentProfileId.value,
            name = medName,
            dosage = dosageText,
            frequency = "Daily",
            prescriber = "Imported FHIR Provider",
            startDate = "Recent",
            status = ItemStatus.ACTIVE,
            indication = reason,
            isReconciled = false,
            provenance = Provenance(
              sourceSystem = "FHIR R4 Bundle Import",
              ingestedAt = currentTime(),
              confidenceScore = 1.0f,
              consentState = "Patient-Imported Record"
            )
          )
          addMedication(newMed)
          importedCount++
        } else if (resourceType != "Patient") {
          val category = when (resourceType) {
            "Observation" -> TimelineCategory.LAB
            "Condition" -> TimelineCategory.CONDITION
            "Encounter" -> TimelineCategory.ENCOUNTER
            "Immunization" -> TimelineCategory.IMMUNIZATION
            "Procedure" -> TimelineCategory.PROCEDURE
            else -> TimelineCategory.DOCUMENT
          }
          val codeText = resource.optJSONObject("code")?.optString("text") ?: "$resourceType Document"
          val date = resource.optString("effectiveDateTime", currentTime().take(12))
          val performer = resource.optJSONArray("performer")?.optJSONObject(0)?.optString("display", "External EHR Health System") ?: "External EHR"
          val note = resource.optJSONArray("note")?.optJSONObject(0)?.optString("text", "Imported clinical record from HL7 FHIR R4 Bundle.") ?: "Imported clinical record."

          val newItem = HealthTimelineItem(
            id = "imp_${UUID.randomUUID().toString().take(8)}",
            profileId = _currentProfileId.value,
            category = category,
            title = codeText,
            subtitle = "$performer • Imported FHIR",
            date = date,
            providerOrFacility = performer,
            status = ItemStatus.NORMAL,
            clinicalNotes = note,
            provenance = Provenance(
              sourceSystem = "HL7 FHIR R4 Bundle Import",
              ingestedAt = currentTime(),
              confidenceScore = 0.99f,
              consentState = "Patient-Owned / Direct Ingestion"
            )
          )
          addTimelineItem(newItem)
          importedCount++
        }
      }
      recordAudit("FHIR_BUNDLE_IMPORT", "Successfully imported $importedCount resources from external FHIR bundle")
      Pair(importedCount, "Successfully imported $importedCount clinical resources from FHIR bundle.")
    } catch (e: Exception) {
      Pair(0, "FHIR parsing error: ${e.message}")
    }
  }

  fun parseCsvRow(line: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
      val c = line[i]
      if (c == '"') {
        if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
          current.append('"')
          i++
        } else {
          inQuotes = !inQuotes
        }
      } else if (c == ',' && !inQuotes) {
        result.add(current.toString().trim())
        current.setLength(0)
      } else {
        current.append(c)
      }
      i++
    }
    result.add(current.toString().trim())
    return result
  }

  fun importMedicationsCsv(csvString: String): Pair<Int, String> {
    return try {
      val lines = csvString.lines().filter { it.isNotBlank() }
      if (lines.isEmpty()) return Pair(0, "CSV file is empty")
      var count = 0
      val header = parseCsvRow(lines[0]).map { it.trim().lowercase().removeSurrounding("\"") }
      val nameIdx = header.indexOfFirst { it == "name" || it == "medication" || it == "drug" }
      val dosageIdx = header.indexOfFirst { it == "dosage" || it == "dose" }
      val freqIdx = header.indexOfFirst { it == "frequency" || it == "freq" }
      val prescriberIdx = header.indexOfFirst { it == "prescriber" || it == "doctor" || it == "provider" }
      val indicationIdx = header.indexOfFirst { it == "indication" || it == "reason" || it == "condition" }

      lines.drop(1).forEach { line ->
        val parts = parseCsvRow(line).map { it.trim().removeSurrounding("\"") }
        if (parts.size >= 2) {
          val (name, dosage, freq, prescriber, indication) = if (nameIdx != -1) {
            val n = parts.getOrNull(nameIdx)?.ifBlank { null } ?: parts.getOrNull(1) ?: parts[0]
            val d = if (dosageIdx != -1) parts.getOrNull(dosageIdx)?.ifBlank { null } ?: "Standard" else parts.getOrNull(2) ?: "Standard"
            val f = if (freqIdx != -1) parts.getOrNull(freqIdx)?.ifBlank { null } ?: "Daily" else parts.getOrNull(3) ?: "Daily"
            val p = if (prescriberIdx != -1) parts.getOrNull(prescriberIdx)?.ifBlank { null } ?: "External Prescriber" else parts.getOrNull(4) ?: "External Prescriber"
            val ind = if (indicationIdx != -1) parts.getOrNull(indicationIdx)?.ifBlank { null } ?: "Therapeutic" else parts.getOrNull(5) ?: "Therapeutic"
            listOf(n, d, f, p, ind)
          } else {
            val n = parts.getOrNull(1) ?: parts[0]
            val d = parts.getOrNull(2) ?: "Standard"
            val f = parts.getOrNull(3) ?: "Daily"
            val p = parts.getOrNull(4) ?: "External Prescriber"
            val ind = parts.getOrNull(5) ?: "Therapeutic"
            listOf(n, d, f, p, ind)
          }

          val med = MedicationItem(
            id = "csv_med_${UUID.randomUUID().toString().take(6)}",
            profileId = _currentProfileId.value,
            name = name,
            dosage = dosage,
            frequency = freq,
            prescriber = prescriber,
            startDate = "Recent",
            status = ItemStatus.ACTIVE,
            indication = indication,
            isReconciled = false,
            provenance = Provenance(
              sourceSystem = "CSV Batch Reconciliation",
              ingestedAt = currentTime(),
              confidenceScore = 0.95f,
              consentState = "Patient Uploaded"
            )
          )
          addMedication(med)
          count++
        }
      }
      recordAudit("CSV_MEDS_IMPORT", "Imported $count medication items from CSV")
      Pair(count, "Successfully imported $count medications from CSV.")
    } catch (e: Exception) {
      Pair(0, "CSV import error: ${e.message}")
    }
  }

  fun revokeCareGrant(grantId: String) {
    _careGrants.update { list ->
      list.map { if (it.id == grantId) it.copy(isActive = false) else it }
    }
    database?.let { db ->
      scope.launch {
        db.careGrantDao().updateActiveStatus(grantId, false)
      }
    }
    recordAudit("SECURE_SHARE_REVOKED", "Care-team access token $grantId revoked immediately by patient")
  }

  fun sendUserMessage(query: String) {
    val activeProfileId = _currentProfileId.value
    val userMsg = ChatMessage(
      id = UUID.randomUUID().toString(),
      profileId = activeProfileId,
      sender = "user",
      text = query,
      timestamp = currentTime()
    )
    _chatMessages.update { it + userMsg }

    // RAG and clinical safety grounding
    val answer = generateGroundedResponse(query)
    _chatMessages.update { it + answer }
    database?.let { db ->
      scope.launch {
        db.chatDao().insert(ChatMessageEntity.fromDomain(userMsg))
        db.chatDao().insert(ChatMessageEntity.fromDomain(answer))
      }
    }
    recordAudit("AI_GROUNDED_QUERY", "Patient queried AI interpreter ($activeProfileId): '${query.take(40)}...'")
  }

  private fun generateGroundedResponse(query: String): ChatMessage {
    val q = query.lowercase().trim()
    val curProfileId = _currentProfileId.value
    val curProfile = _profiles.value.firstOrNull { it.id == curProfileId } ?: _profiles.value.first()
    val patientTimeline = _timelineItems.value.filter { it.profileId == curProfileId }
    val patientMeds = _medications.value.filter { it.profileId == curProfileId }
    val activeMeds = patientMeds.filter { it.status == ItemStatus.ACTIVE }

    // 1. Red-Flag Emergency Triage (All profiles)
    val redFlagTriggers = listOf(
      "chest pain", "shortness of breath", "arm pain", "jaw pain",
      "heart attack", "stroke", "numbness", "suicide", "bleeding profusely",
      "anaphylaxis", "cannot breathe", "stridor", "blue lips"
    )
    if (redFlagTriggers.any { q.contains(it) }) {
      val escalationDetail = if (curProfileId == "profile_maya") {
        "CALL 911 OR ADMINISTER EPINEPHRINE AUTO-INJECTOR (EpiPen Jr) IMMEDIATELY if anaphylaxis is suspected. Seek immediate emergency pediatric care."
      } else {
        "CALL 911 OR PROCEED TO THE NEAREST EMERGENCY ROOM IMMEDIATELY. Do not wait for an online response or take unprescribed medications."
      }
      return ChatMessage(
        id = UUID.randomUUID().toString(),
        profileId = curProfileId,
        sender = "ai",
        text = "CRITICAL ALERT: Your query mentions symptoms that may indicate an acute medical emergency for ${curProfile.name}. Vitalith cannot evaluate emergency conditions.",
        timestamp = currentTime(),
        isRedFlag = true,
        redFlagEscalationText = escalationDetail
      )
    }

    // 1.5 Symptoms & Longitudinal Wellness Grounding
    val symptomKeywords = listOf("symptom", "pain", "headache", "migraine", "reflux", "fatigue", "nausea", "rash", "dizziness", "wellness")
    if (symptomKeywords.any { q.contains(it) }) {
      val profileSymptoms = _symptoms.value.filter { it.profileId == curProfileId }
      val matchingSymptoms = profileSymptoms.filter { sym ->
        symptomKeywords.any { kw -> sym.symptomName.contains(kw, ignoreCase = true) && q.contains(kw) } ||
        sym.notes.contains(q, ignoreCase = true) ||
        (sym.bodySite != null && sym.bodySite.contains(q, ignoreCase = true)) ||
        q.contains(sym.symptomName.lowercase()) ||
        q.contains("symptom") || q.contains("wellness")
      }
      if (matchingSymptoms.isNotEmpty()) {
        val latest = matchingSymptoms.first()
        val text = buildString {
          append("According to ${curProfile.name}'s verified longitudinal symptom tracker:\n\n")
          matchingSymptoms.take(3).forEach { sym ->
            val status = if (!sym.isOngoing) "Resolved" else "Active / Ongoing (${sym.durationDisplay})"
            append("• ${sym.symptomName} (Severity ${sym.severity}/10): $status. Onset: ${sym.onsetDate}. Location: ${sym.bodySite ?: "General"}.\n")
            if (sym.contextTags.isNotEmpty()) {
              append("  Context factors: ${sym.contextTags.joinToString(", ")}\n")
            }
            if (sym.notes.isNotBlank()) {
              append("  Clinical Notes: ${sym.notes}\n")
            }
          }
          val impactStr = if (latest.impact.isNotEmpty()) latest.impact.joinToString(", ") else "None reported"
          append("\nImpact: $impactStr. Recommended action: Review symptom trajectory at upcoming clinical consult.")
        }
        val citations = matchingSymptoms.take(3).map { sym ->
          Citation(
            id = "cit_sym_${sym.id}",
            title = "Symptom Log • ${sym.symptomName} (Severity ${sym.severity}/10)",
            date = sym.onsetDate,
            source = "Vitalith Symptoms & Wellness Tracker",
            excerpt = "${sym.symptomName} (${if (!sym.isOngoing) "Resolved" else "Active"}). Location: ${sym.bodySite ?: "General"}. Context: ${sym.contextTags.joinToString()}. ${sym.notes.ifEmpty { "Impact: " + sym.impact.joinToString() }}"
          )
        }
        return ChatMessage(
          id = UUID.randomUUID().toString(),
          profileId = curProfileId,
          sender = "ai",
          text = text,
          timestamp = currentTime(),
          citations = citations
        )
      }
    }

    // 2. A1c / Glucose / Diabetes / Sugar / Metabolic
    if (q.contains("a1c") || q.contains("glucose") || q.contains("sugar") || q.contains("diabetes") || q.contains("metabolic")) {
      val a1cItems = patientTimeline.filter {
        it.title.contains("A1c", ignoreCase = true) ||
        it.title.contains("Glucose", ignoreCase = true) ||
        it.clinicalNotes.contains("A1c", ignoreCase = true) ||
        it.clinicalNotes.contains("Glucose", ignoreCase = true)
      }
      if (a1cItems.isNotEmpty()) {
        val latest = a1cItems.first()
        val text = buildString {
          append("According to ${curProfile.name}'s verified records, ")
          if (latest.title.contains("A1c", ignoreCase = true) || latest.clinicalNotes.contains("A1c", ignoreCase = true)) {
            append("the most recent Hemoglobin A1c test from ${latest.date} (${latest.providerOrFacility}) recorded: ${latest.subtitle}.\n\n")
          } else {
            append("the latest metabolic test from ${latest.date} (${latest.providerOrFacility}) is: ${latest.title} (${latest.subtitle}).\n\n")
          }
          append("Clinical Note: ${latest.clinicalNotes}")
        }
        val citations = a1cItems.take(2).mapIndexed { idx, item ->
          val citationTitle = if (curProfileId == "profile_doug" && idx == 0) {
            "Quest Diagnostics • Hemoglobin A1c"
          } else {
            "${item.providerOrFacility.substringBefore(" Hub")} • ${item.title.substringBefore(":")}"
          }
          Citation(
            id = "cit_${item.id}",
            title = citationTitle,
            date = item.date,
            source = item.provenance.sourceSystem,
            excerpt = item.clinicalNotes.take(120)
          )
        }
        return ChatMessage(
          id = UUID.randomUUID().toString(),
          profileId = curProfileId,
          sender = "ai",
          text = text,
          timestamp = currentTime(),
          citations = citations
        )
      } else {
        return ChatMessage(
          id = UUID.randomUUID().toString(),
          profileId = curProfileId,
          sender = "ai",
          text = "No diagnostic blood glucose or Hemoglobin A1c records were found for ${curProfile.name}. Pediatric wellness and immunization records indicate routine developmental screenings with no diabetic indications on file.",
          timestamp = currentTime(),
          citations = emptyList()
        )
      }
    }

    // 3. Medications / Dosage / Statin / Metformin / Amlodipine / Cetirizine / Antibiotics / Prescriptions
    if (q.contains("med") || q.contains("dose") || q.contains("drug") || q.contains("prescription") ||
        q.contains("metformin") || q.contains("atorvastatin") || q.contains("statin") ||
        q.contains("amlodipine") || q.contains("clopidogrel") || q.contains("empagliflozin") ||
        q.contains("cetirizine") || q.contains("epipen") || q.contains("multivitamin") || q.contains("aspirin")
    ) {
      if (patientMeds.isNotEmpty()) {
        val medText = buildString {
          append("${curProfile.name}'s chart contains ${activeMeds.size} active medication${if (activeMeds.size == 1) "" else "s"}:\n")
          activeMeds.forEachIndexed { idx, med ->
            append("${idx + 1}. ${med.name} ${med.dosage} (${med.frequency}) — ${med.indication} [Prescriber: ${med.prescriber}]\n")
          }
          val discontinued = patientMeds.filter { it.status == ItemStatus.DISCONTINUED }
          if (discontinued.isNotEmpty()) {
            append("\nDiscontinued / Historical: ")
            append(discontinued.joinToString(", ") { "${it.name} ${it.dosage}" })
            append("\n")
          }
          if (curProfile.allergies.isNotEmpty()) {
            append("\nDocumented Allergies / Cautions: ${curProfile.allergies.joinToString(", ")}.")
          }
        }
        val citations = activeMeds.take(3).map { med ->
          Citation(
            id = "cit_${med.id}",
            title = "MedicationStatement • ${med.name} ${med.dosage}",
            date = "Started: ${med.startDate}",
            source = med.provenance.sourceSystem,
            excerpt = "Sig: Take ${med.dosage} ${med.frequency} for ${med.indication}. Prescriber: ${med.prescriber}."
          )
        }
        return ChatMessage(
          id = UUID.randomUUID().toString(),
          profileId = curProfileId,
          sender = "ai",
          text = medText,
          timestamp = currentTime(),
          citations = citations
        )
      } else {
        return ChatMessage(
          id = UUID.randomUUID().toString(),
          profileId = curProfileId,
          sender = "ai",
          text = "No active prescription medications are currently documented in ${curProfile.name}'s chart.",
          timestamp = currentTime(),
          citations = emptyList()
        )
      }
    }

    // 4. Kidney / Renal / eGFR / Creatinine / Nephrology
    if (q.contains("kidney") || q.contains("egfr") || q.contains("renal") || q.contains("creatinine") || q.contains("nephro")) {
      val renalItems = patientTimeline.filter {
        it.title.contains("Renal", ignoreCase = true) ||
        it.title.contains("Metabolic", ignoreCase = true) ||
        it.subtitle.contains("eGFR", ignoreCase = true) ||
        it.clinicalNotes.contains("eGFR", ignoreCase = true) ||
        it.clinicalNotes.contains("Creatinine", ignoreCase = true) ||
        it.clinicalNotes.contains("kidney", ignoreCase = true)
      }
      if (renalItems.isNotEmpty()) {
        val latest = renalItems.first()
        val text = buildString {
          append("According to ${curProfile.name}'s laboratory records from ${latest.providerOrFacility} (${latest.date}):\n\n")
          append("• Test: ${latest.title}\n")
          append("• Results: ${latest.subtitle}\n\n")
          append("Clinical Note: ${latest.clinicalNotes}")
        }
        val citations = renalItems.take(2).map { item ->
          Citation(
            id = "cit_${item.id}",
            title = "${item.providerOrFacility} • ${item.title}",
            date = item.date,
            source = item.provenance.sourceSystem,
            excerpt = item.clinicalNotes.take(120)
          )
        }
        return ChatMessage(
          id = UUID.randomUUID().toString(),
          profileId = curProfileId,
          sender = "ai",
          text = text,
          timestamp = currentTime(),
          citations = citations
        )
      } else {
        return ChatMessage(
          id = UUID.randomUUID().toString(),
          profileId = curProfileId,
          sender = "ai",
          text = "No specialized renal or eGFR impairment records are found for ${curProfile.name}. Standard pediatric developmental screenings show normal renal parameters.",
          timestamp = currentTime(),
          citations = emptyList()
        )
      }
    }

    // 5. Heart / Cardiac / Stent / Blood Pressure / Cardiology / Angiogram / Hypertension
    if (q.contains("heart") || q.contains("cardio") || q.contains("stent") || q.contains("bp") ||
        q.contains("blood pressure") || q.contains("pressure") || q.contains("angiogram") || q.contains("vessel")
    ) {
      val cardiacItems = patientTimeline.filter {
        it.title.contains("Cardio", ignoreCase = true) ||
        it.title.contains("Stent", ignoreCase = true) ||
        it.title.contains("Blood Pressure", ignoreCase = true) ||
        it.title.contains("Angiogram", ignoreCase = true) ||
        it.clinicalNotes.contains("cardiac", ignoreCase = true) ||
        it.clinicalNotes.contains("stent", ignoreCase = true) ||
        it.clinicalNotes.contains("blood pressure", ignoreCase = true) ||
        it.subtitle.contains("mmHg", ignoreCase = true)
      }
      if (cardiacItems.isNotEmpty()) {
        val latest = cardiacItems.first()
        val text = buildString {
          append("Cardiovascular & hemodynamic status for ${curProfile.name}:\n\n")
          append("• Latest Record: ${latest.title} (${latest.date}, ${latest.providerOrFacility})\n")
          append("• Findings: ${latest.subtitle}\n\n")
          append("Clinical Synthesis: ${latest.clinicalNotes}")
        }
        val citations = cardiacItems.take(2).map { item ->
          Citation(
            id = "cit_${item.id}",
            title = "${item.providerOrFacility} • ${item.title}",
            date = item.date,
            source = item.provenance.sourceSystem,
            excerpt = item.clinicalNotes.take(120)
          )
        }
        return ChatMessage(
          id = UUID.randomUUID().toString(),
          profileId = curProfileId,
          sender = "ai",
          text = text,
          timestamp = currentTime(),
          citations = citations
        )
      } else {
        return ChatMessage(
          id = UUID.randomUUID().toString(),
          profileId = curProfileId,
          sender = "ai",
          text = "Cardiovascular evaluations for ${curProfile.name} indicate normal age-appropriate heart sounds and vital signs with no cardiac conditions logged.",
          timestamp = currentTime(),
          citations = emptyList()
        )
      }
    }

    // 6. Allergies / Immunizations / Vaccines / Growth / Pediatric
    if (q.contains("allerg") || q.contains("immuniz") || q.contains("vaccin") || q.contains("shot") ||
        q.contains("booster") || q.contains("mmr") || q.contains("flu") || q.contains("peanut") ||
        q.contains("growth") || q.contains("height") || q.contains("weight")
    ) {
      val immItems = patientTimeline.filter {
        it.category == TimelineCategory.IMMUNIZATION ||
        it.title.contains("Allergy", ignoreCase = true) ||
        it.title.contains("Immunization", ignoreCase = true) ||
        it.title.contains("Vaccine", ignoreCase = true) ||
        it.title.contains("Well-Child", ignoreCase = true) ||
        it.clinicalNotes.contains("allergy", ignoreCase = true) ||
        it.clinicalNotes.contains("vaccine", ignoreCase = true)
      }
      val text = buildString {
        append("Immunization & Allergy Profile for ${curProfile.name}:\n\n")
        if (curProfile.allergies.isNotEmpty()) {
          append("• Verified Allergies: ${curProfile.allergies.joinToString(", ")}\n")
        } else {
          append("• Verified Allergies: No known allergies documented.\n")
        }
        if (immItems.isNotEmpty()) {
          append("• Recent Immunizations / Clinical Checkups:\n")
          immItems.take(3).forEach {
            append("  - ${it.date}: ${it.title} (${it.providerOrFacility})\n")
          }
          append("\nClinical Note: ${immItems.first().clinicalNotes}")
        }
      }
      val citations = immItems.take(2).map { item ->
        Citation(
          id = "cit_${item.id}",
          title = "${item.providerOrFacility} • ${item.title}",
          date = item.date,
          source = item.provenance.sourceSystem,
          excerpt = item.clinicalNotes.take(120)
        )
      }
      return ChatMessage(
        id = UUID.randomUUID().toString(),
        profileId = curProfileId,
        sender = "ai",
        text = text,
        timestamp = currentTime(),
        citations = citations
      )
    }

    // 7. Appointment Prep / Visit Synthesis
    if (q.contains("appointment") || q.contains("prep") || q.contains("doctor") || q.contains("visit") || q.contains("clinic")) {
      val prep = getAppointmentPrep()
      val text = buildString {
        append("Upcoming Appointment Prep Briefing for ${curProfile.name}:\n\n")
        append("• Provider: ${prep.doctorName} (${prep.clinic})\n")
        append("• Scheduled: ${prep.scheduledDate}\n")
        append("• Purpose: ${prep.visitReason}\n")
        append("• Active Medications Monitored: ${prep.activeMedsCount}\n\n")
        append("Recent Clinical Trajectory:\n")
        prep.recentChanges.forEach { append("  - $it\n") }
        append("\nKey Questions to Discuss:\n")
        prep.keyQuestionsToAsk.forEachIndexed { i, ques -> append("  ${i + 1}. $ques\n") }
      }
      val citations = listOf(
        Citation(
          id = "c_prep_${prep.id}",
          title = "Automated Clinical Prep Briefing • ${prep.doctorName}",
          date = prep.scheduledDate,
          source = "Vitalith RAG Synthesizer",
          excerpt = "${curProfile.name} • ${prep.visitReason}. Synthesized across ${patientTimeline.size} records and ${prep.activeMedsCount} medications."
        )
      )
      return ChatMessage(
        id = UUID.randomUUID().toString(),
        profileId = curProfileId,
        sender = "ai",
        text = text,
        timestamp = currentTime(),
        citations = citations
      )
    }

    // 8. Natural Language Health Record Summary & Anomaly Flagging
    if (q.contains("summar") || q.contains("anomal") || q.contains("flag") || q.contains("insight") || q.contains("overview")) {
      val summary = GeminiHealthSummarizer.generateDeterministicSummary(patientTimeline, curProfile.name, _userProfile.value.unitPreferences)
      val findingsText = if (summary.flaggedFindings.isNotEmpty()) {
        summary.flaggedFindings.joinToString("\n") { finding ->
          "• ${finding.biomarkerOrItem} (${finding.observedValue}): [${finding.severity.label.uppercase()}] ${finding.clinicalRationale}"
        }
      } else {
        "• No acute clinical anomalies flagged across current records."
      }
      val insightsText = summary.keyInsights.joinToString("\n") { "• $it" }
      val condSummary = patientTimeline.filter { it.category == TimelineCategory.CONDITION }.map { it.title }.joinToString(", ").ifEmpty { "longitudinal records" }
      return ChatMessage(
        id = UUID.randomUUID().toString(),
        profileId = curProfileId,
        sender = "ai",
        text = "VITALITH AI CLINICAL SUMMARY (${curProfile.name.uppercase()}):\n${summary.executiveSummary}\n\nFLAGGED ANOMALIES & RADAR:\n$findingsText\n\nLONGITUDINAL INSIGHTS:\n$insightsText",
        timestamp = currentTime(),
        citations = listOf(
          Citation(
            id = "c_sum_${curProfileId}",
            title = "Vitalith Longitudinal Record Synthesis • ${curProfile.name}",
            date = summary.generatedAt,
            source = "Vitalith AI Grounded Clinical Evaluator",
            excerpt = "${patientTimeline.size} longitudinal records synthesized across $condSummary."
          )
        )
      )
    }

    // 9. Keyword Search Fallback across live timeline and live meds
    val matchingItems = patientTimeline.filter { item ->
      item.title.contains(q, ignoreCase = true) ||
      item.subtitle.contains(q, ignoreCase = true) ||
      item.clinicalNotes.contains(q, ignoreCase = true) ||
      item.providerOrFacility.contains(q, ignoreCase = true)
    }
    val matchingMeds = patientMeds.filter { med ->
      med.name.contains(q, ignoreCase = true) ||
      med.indication.contains(q, ignoreCase = true) ||
      med.prescriber.contains(q, ignoreCase = true)
    }
    if (matchingItems.isNotEmpty() || matchingMeds.isNotEmpty()) {
      val text = buildString {
        append("Found verified records in ${curProfile.name}'s chart matching your query:\n\n")
        matchingItems.take(2).forEach {
          append("• [${it.date}] ${it.title} (${it.providerOrFacility}): ${it.clinicalNotes}\n\n")
        }
        matchingMeds.take(2).forEach {
          append("• Medication: ${it.name} ${it.dosage} (${it.status.name}) — ${it.indication}\n\n")
        }
      }
      val citations = matchingItems.take(2).map { item ->
        Citation(
          id = "cit_match_${item.id}",
          title = "${item.providerOrFacility} • ${item.title}",
          date = item.date,
          source = item.provenance.sourceSystem,
          excerpt = item.clinicalNotes.take(120)
        )
      } + matchingMeds.take(1).map { med ->
        Citation(
          id = "cit_match_${med.id}",
          title = "Medication • ${med.name}",
          date = med.startDate,
          source = med.provenance.sourceSystem,
          excerpt = "${med.name} ${med.dosage} for ${med.indication} (${med.prescriber})"
        )
      }
      return ChatMessage(
        id = UUID.randomUUID().toString(),
        profileId = curProfileId,
        sender = "ai",
        text = text.trim(),
        timestamp = currentTime(),
        citations = citations
      )
    }

    // 10. Grounded Refusal when out of record scope
    return ChatMessage(
      id = UUID.randomUUID().toString(),
      profileId = curProfileId,
      sender = "ai",
      text = "I searched ${curProfile.name}'s longitudinal health record (${patientTimeline.size} clinical documents and ${patientMeds.size} medications loaded) but could not find verified clinical data matching '$query'. To protect clinical safety, Vitalith refuses to speculate or provide ungrounded advice.\n\nYou can upload relevant documents in the Ingestion tab or discuss this directly with ${curProfile.name}'s care team.",
      timestamp = currentTime(),
      citations = emptyList()
    )
  }

  fun recordAudit(action: String, resource: String) {
    val userName = _userProfile.value.fullName.ifBlank { "Doug Adarkwah" }
    val curProfId = _currentProfileId.value
    val curProf = _profiles.value.firstOrNull { it.id == curProfId }
    val actorText = if (curProf != null && !curProf.isPrimary) {
      "$userName (Acting for ${curProf.name})"
    } else {
      "$userName (Owner)"
    }

    val entry = AuditEntry(
      id = "aud_${UUID.randomUUID().toString().take(8)}",
      timestamp = currentTime(),
      actor = actorText,
      action = action,
      resource = resource,
      status = "SUCCESS",
      ipAddress = "192.168.1.104"
    )
    _auditLogs.update { listOf(entry) + it }
    database?.let { db ->
      scope.launch {
        db.auditDao().insert(AuditLogEntity.fromDomain(entry))
      }
    }
  }

  private fun currentTime(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return sdf.format(Date())
  }

  fun getAppointmentPrep(unitPreferences: UnitPreferences = _userProfile.value.unitPreferences): AppointmentPrep {
    val curProfileId = _currentProfileId.value
    val curProfile = _profiles.value.firstOrNull { it.id == curProfileId } ?: _profiles.value.first()
    val patientTimeline = _timelineItems.value.filter { it.profileId == curProfileId }
    val patientMeds = _medications.value.filter { it.profileId == curProfileId }
    val activeMeds = patientMeds.filter { it.status == ItemStatus.ACTIVE }

    val recentEncounter = patientTimeline.firstOrNull { it.category == TimelineCategory.ENCOUNTER }
    val doctorFromEncounter = recentEncounter?.let { enc ->
      if (enc.subtitle.contains("Dr.", ignoreCase = true)) {
        enc.subtitle.substringBefore(" •").trim()
      } else null
    }

    return when (curProfileId) {
      "profile_maya" -> {
        val heightWeightItem = patientTimeline.firstOrNull { it.title.contains("Well-Child", ignoreCase = true) || it.title.contains("Growth", ignoreCase = true) }
        val leadItem = patientTimeline.firstOrNull { it.title.contains("Lead", ignoreCase = true) }
        val immCount = patientTimeline.count { it.category == TimelineCategory.IMMUNIZATION }

        val dynamicRecentChanges = mutableListOf<String>()
        val nonSeedRecent = patientTimeline.filter { !it.id.startsWith("maya_") }.take(2)
        nonSeedRecent.forEach {
          dynamicRecentChanges.add("Ingested record: ${it.title} (${it.date}, ${it.subtitle})")
        }
        dynamicRecentChanges.add("Height and weight trending along normal pediatric growth curve (72nd & 65th %iles)")
        dynamicRecentChanges.add("Peanut allergy: Zero systemic reactions; carries daily cetirizine & school EpiPen Jr")
        dynamicRecentChanges.add("Completed elementary immunization schedule ($immCount vaccine events recorded)")
        dynamicRecentChanges.add("Active pediatric medications (${activeMeds.size}): ${activeMeds.joinToString { it.name }}")

        val dynamicMetrics = mutableMapOf<String, String>()
        val mayaWeightDisplay = if (unitPreferences.weightUnit == WeightUnit.LBS) {
          val lbs = com.example.util.UcumUnitConverter.kgToLbs(23.5)
          "${String.format(Locale.US, "%.1f", lbs)} lbs (65th %ile)"
        } else {
          "23.5 kg (65th %ile)"
        }
        val mayaHeightDisplay = if (unitPreferences.heightUnit == HeightUnit.FT_IN) {
          val (feet, inches) = com.example.util.UcumUnitConverter.cmToFeetAndInches(122.0)
          "${feet} ft ${inches} in (72nd %ile)"
        } else {
          if (heightWeightItem != null) "122 cm (72nd %ile)" else "Normal growth curve"
        }
        dynamicMetrics["Height / Growth"] = mayaHeightDisplay
        dynamicMetrics["Weight"] = mayaWeightDisplay
        dynamicMetrics["Blood Lead"] = leadItem?.subtitle ?: "< 1.0 ug/dL (Normal)"
        dynamicMetrics["Vision / Hearing"] = "20/20 Pass"

        AppointmentPrep(
          id = "prep_maya_nov2026",
          profileId = "profile_maya",
          doctorName = doctorFromEncounter ?: "Dr. Michael Chen, MD",
          clinic = "Metro Pediatric Clinic",
          scheduledDate = "Nov 18, 2026 • 09:30 AM",
          visitReason = "7-Year Well-Child Follow-Up & Pediatric Allergy Review",
          recentChanges = dynamicRecentChanges.take(4),
          keyQuestionsToAsk = listOf(
            "Are any booster immunizations required prior to the 8-year annual checkup?",
            "Should we repeat the peanut skin-prick / specific IgE panel this year to monitor sensitivity?",
            "Can the clinic provide an updated signed Allergy Action Plan for the elementary school nurse?",
            "Given her ${activeMeds.size} active medications (${activeMeds.joinToString { it.name }}), are any dosage adjustments needed?"
          ),
          criticalMetrics = dynamicMetrics,
          activeMedsCount = activeMeds.size
        )
      }
      "profile_kwame" -> {
        val latestRenal = patientTimeline.firstOrNull { it.title.contains("Renal", ignoreCase = true) || it.title.contains("Metabolic", ignoreCase = true) }
        val latestCardiac = patientTimeline.firstOrNull { it.title.contains("Angiogram", ignoreCase = true) || it.title.contains("Cardio", ignoreCase = true) }
        val latestA1c = patientTimeline.firstOrNull { it.title.contains("A1c", ignoreCase = true) || it.clinicalNotes.contains("HbA1c", ignoreCase = true) }

        val dynamicRecentChanges = mutableListOf<String>()
        val nonSeedRecent = patientTimeline.filter { !it.id.startsWith("kwame_") }.take(2)
        nonSeedRecent.forEach {
          dynamicRecentChanges.add("Ingested record: ${it.title} (${it.date}, ${it.subtitle})")
        }
        dynamicRecentChanges.add("Angiogram verified LAD drug-eluting stent 100% patent with TIMI 3 flow")
        dynamicRecentChanges.add("eGFR stable at 48 mL/min (CKD Stage 3a baseline maintained across 6 months)")
        dynamicRecentChanges.add("Resting blood pressure averaged 132/78 mmHg with Amlodipine 5mg")
        dynamicRecentChanges.add("${activeMeds.size} active medications maintained: ${activeMeds.joinToString { it.name }}")

        val dynamicMetrics = mutableMapOf<String, String>()
        dynamicMetrics["eGFR"] = if (latestRenal != null) "48 mL/min (CKD 3a)" else "48 mL/min"
        dynamicMetrics["HbA1c"] = if (latestA1c != null) "7.3% (Target < 7.5%)" else "7.3%"
        dynamicMetrics["Blood Pressure"] = "132/78 mmHg"
        dynamicMetrics["LVEF"] = "55% (Preserved)"

        AppointmentPrep(
          id = "prep_kwame_nov2026",
          profileId = "profile_kwame",
          doctorName = doctorFromEncounter ?: "Dr. Angela Rossi, MD",
          clinic = "St. Luke's Heart & Vascular Institute",
          scheduledDate = "Nov 12, 2026 • 11:00 AM",
          visitReason = "Coronary Stent Surveillance & Nephrology Coordinated Checkup",
          recentChanges = dynamicRecentChanges.take(4),
          keyQuestionsToAsk = listOf(
            "Should dual antiplatelet therapy (Clopidogrel 75mg) be maintained or transitioned given stent stability?",
            "Given stable eGFR of 48 mL/min, should Empagliflozin 10mg dosing remain unchanged?",
            "Do you recommend scheduling a surveillance echocardiogram to assess ejection fraction?",
            "With ${activeMeds.size} active medications, are there any renal-metabolic interactions to watch?"
          ),
          criticalMetrics = dynamicMetrics,
          activeMedsCount = activeMeds.size
        )
      }
      else -> {
        val latestA1c = patientTimeline.firstOrNull { it.title.contains("A1c", ignoreCase = true) || it.subtitle.contains("HbA1c", ignoreCase = true) }
        val latestGlucose = patientTimeline.firstOrNull { it.title.contains("Glucose", ignoreCase = true) || it.subtitle.contains("Glucose", ignoreCase = true) }
        val glucoseVal = latestGlucose?.subtitle?.substringBefore(" ")?.trim() ?: "102"
        val glucoseDisplay = com.example.util.UcumUnitConverter.formatGlucose("$glucoseVal mg/dL", unitPreferences.glucoseUnit).displayString
        val bpDisplay = "122/78 mmHg"

        val dynamicRecentChanges = mutableListOf<String>()
        val nonSeedRecent = patientTimeline.filter { !it.id.startsWith("tl_") }.take(2)
        nonSeedRecent.forEach {
          dynamicRecentChanges.add("Ingested record: ${it.title} (${it.date}, ${it.subtitle})")
        }
        dynamicRecentChanges.add("A1c dropped from 6.8% to 6.4% (Quest Diagnostics, Oct 14)")
        dynamicRecentChanges.add("Adherence verified across ${activeMeds.size} active medications (${activeMeds.joinToString { it.name }})")
        dynamicRecentChanges.add("Home systolic blood pressure averaged $bpDisplay (normal)")
        dynamicRecentChanges.add("Targeted pre-diabetes lifestyle & nutrition adjustments maintained")

        val dynamicMetrics = mutableMapOf<String, String>()
        dynamicMetrics["HbA1c"] = latestA1c?.subtitle ?: "6.4% (Prior: 6.8%)"
        dynamicMetrics["Fasting Glucose"] = glucoseDisplay
        dynamicMetrics["eGFR"] = "98 mL/min (Normal)"
        dynamicMetrics["BP Trend"] = bpDisplay

        AppointmentPrep(
          id = "prep_nov2026",
          profileId = curProfile.id,
          doctorName = doctorFromEncounter ?: "Dr. Sarah Chen, MD",
          clinic = "Bayfront Endocrinology & Metabolism",
          scheduledDate = "Nov 04, 2026 • 10:15 AM",
          visitReason = "3-Month Pre-Diabetes Management & A1c Review",
          recentChanges = dynamicRecentChanges.take(4),
          keyQuestionsToAsk = listOf(
            "Is my A1c trajectory sufficient to maintain the Metformin 500mg BID dose, or should we plan a taper?",
            "Do you recommend repeating the comprehensive metabolic panel before our 6-month check?",
            "Should we screen for early signs of neuropathy or microalbuminuria at this stage?",
            "Given ${activeMeds.size} active prescriptions, are there any dietary carbohydrate thresholds to adjust?"
          ),
          criticalMetrics = dynamicMetrics,
          activeMedsCount = activeMeds.size
        )
      }
    }
  }

  private fun initialDougTimeline(): List<HealthTimelineItem> {
    return listOf(
      HealthTimelineItem(
        id = "tl_1",
        profileId = "profile_doug",
        category = TimelineCategory.LAB,
        title = "Hemoglobin A1c: 6.4%",
        subtitle = "Quest Diagnostics • Normal: 4.0 - 5.6%",
        date = "Oct 14, 2026",
        providerOrFacility = "Quest Diagnostics Hub",
        status = ItemStatus.ELEVATED,
        clinicalNotes = "HbA1c decreased from 6.8% (Jul 2026) to 6.4%. Pre-diabetes range. Patient demonstrating strong adherence to nutritional plan and Metformin therapy.",
        provenance = Provenance(
          sourceSystem = "Quest Diagnostics HL7/FHIR v4.0.1",
          ingestedAt = "2026-10-14 14:22 UTC",
          confidenceScore = 0.99f,
          consentState = "Patient-Owned / Full Consent",
          documentHash = "sha256:4a8b79e13f9c8...2d81"
        ),
        tags = listOf("Metabolic", "A1c", "Pre-diabetes", "Trend-Improving"),
        keyValues = mapOf("Value" to "6.4%", "Reference" to "4.0 - 5.6%", "Prior" to "6.8%")
      ),
      HealthTimelineItem(
        id = "tl_2",
        profileId = "profile_doug",
        category = TimelineCategory.ENCOUNTER,
        title = "Endocrinology Consultation",
        subtitle = "Dr. Sarah Chen, MD • Bayfront Specialty Clinic",
        date = "Oct 14, 2026",
        providerOrFacility = "Bayfront Health",
        status = ItemStatus.NORMAL,
        clinicalNotes = "Routine follow-up. Physical exam unremarkable. Patient reports good energy, no gastrointestinal complaints from metformin. Continue current regimen.",
        provenance = Provenance(
          sourceSystem = "Bayfront Epic SMART-on-FHIR",
          ingestedAt = "2026-10-14 16:05 UTC",
          confidenceScore = 1.0f,
          consentState = "Patient-Owned / Full Consent",
          documentHash = "sha256:9c12b456da871...ff34"
        ),
        tags = listOf("Endocrinology", "Outpatient", "Dr. Chen"),
        keyValues = mapOf("Encounter Type" to "Outpatient Consult", "Duration" to "30 mins", "Disposition" to "Routine Follow-up")
      ),
      HealthTimelineItem(
        id = "tl_3",
        profileId = "profile_doug",
        category = TimelineCategory.LAB,
        title = "Lipid Panel (Cardiovascular Risk)",
        subtitle = "LabCorp OCR Scan • Total Chol: 188 mg/dL",
        date = "Sep 22, 2026",
        providerOrFacility = "LabCorp Burlington",
        status = ItemStatus.NORMAL,
        clinicalNotes = "LDL: 118 mg/dL, HDL: 52 mg/dL, Triglycerides: 138 mg/dL. Overall lipid profile stable under low-dose Atorvastatin.",
        provenance = Provenance(
          sourceSystem = "LabCorp PDF OCR Pipeline v2.1",
          ingestedAt = "2026-09-23 09:15 UTC",
          confidenceScore = 0.96f,
          consentState = "Patient-Owned / Full Consent",
          documentHash = "sha256:d871239aa812f...45bc"
        ),
        tags = listOf("Cardiology", "Lipids", "Cholesterol", "OCR Ingestion"),
        keyValues = mapOf("Total Chol" to "188 mg/dL", "LDL" to "118 mg/dL", "HDL" to "52 mg/dL")
      ),
      HealthTimelineItem(
        id = "tl_4",
        profileId = "profile_doug",
        category = TimelineCategory.MEDICATION,
        title = "Metformin HCl 500mg Initiated",
        subtitle = "1 tablet orally BID with food • Dr. Sarah Chen",
        date = "Jun 10, 2025",
        providerOrFacility = "Walgreens Pharmacy #4190",
        status = ItemStatus.ACTIVE,
        clinicalNotes = "Prescribed for pre-diabetes glycemic regulation. Take with morning and evening meals to minimize stomach upset.",
        provenance = Provenance(
          sourceSystem = "Surescripts / NCPDP E-Prescribe",
          ingestedAt = "2025-06-10 18:30 UTC",
          confidenceScore = 0.99f,
          consentState = "Patient-Owned / Full Consent",
          documentHash = "sha256:11bb890cde114...78aa"
        ),
        tags = listOf("Medication", "Endocrine", "Oral Agent"),
        keyValues = mapOf("Dosage" to "500 mg", "Frequency" to "Twice Daily", "Refills" to "3 remaining")
      ),
      HealthTimelineItem(
        id = "tl_5",
        profileId = "profile_doug",
        category = TimelineCategory.ALLERGY,
        title = "Allergy: Penicillin (Severe)",
        subtitle = "Anaphylactoid Reaction / Severe Urticaria",
        date = "Aug 12, 2018",
        providerOrFacility = "St. Jude Hospital Records",
        status = ItemStatus.CRITICAL,
        clinicalNotes = "Patient experienced generalized hives, lip swelling, and wheezing 30 minutes following oral amoxicillin. Absolute contraindication to all beta-lactams.",
        provenance = Provenance(
          sourceSystem = "Cerner Millennium FHIR R4",
          ingestedAt = "2026-01-10 11:00 UTC",
          confidenceScore = 1.0f,
          consentState = "Patient-Owned / Full Consent",
          documentHash = "sha256:ff001928374aa...9911"
        ),
        tags = listOf("Allergy", "Critical Alert", "Beta-lactam"),
        keyValues = mapOf("Severity" to "Severe", "Reaction" to "Hives / Wheezing", "Status" to "Active")
      ),
      HealthTimelineItem(
        id = "tl_6",
        profileId = "profile_doug",
        category = TimelineCategory.IMMUNIZATION,
        title = "Updated COVID-19 & Influenza Vaccine",
        subtitle = "Spikevax 2026-2027 Formulation + Fluzone",
        date = "Sep 15, 2026",
        providerOrFacility = "CVS Health MinuteClinic",
        status = ItemStatus.NORMAL,
        clinicalNotes = "Administered in left and right deltoid. No adverse reactions observed during 15-minute post-vaccine monitoring.",
        provenance = Provenance(
          sourceSystem = "State Immunization Information System (IIS)",
          ingestedAt = "2026-09-15 16:45 UTC",
          confidenceScore = 0.98f,
          consentState = "Patient-Owned / Full Consent",
          documentHash = "sha256:33ee22119944a...6677"
        ),
        tags = listOf("Immunization", "Preventive", "IIS Sync"),
        keyValues = mapOf("Lot" to "FLZ-9812A", "Route" to "Intramuscular", "Expiry" to "2027-05")
      )
    )
  }

  private fun initialDougMeds(): List<MedicationItem> {
    return listOf(
      MedicationItem(
        id = "med_1",
        profileId = "profile_doug",
        name = "Metformin Hydrochloride",
        dosage = "500 mg",
        frequency = "Take 1 tablet twice daily with meals",
        prescriber = "Dr. Sarah Chen, MD",
        startDate = "Jun 10, 2025",
        status = ItemStatus.ACTIVE,
        indication = "Pre-diabetes & Glycemic Management",
        conflictWarning = null,
        isReconciled = true,
        provenance = Provenance("Epic SMART-on-FHIR", "2026-10-14", 0.99f, "Full Consent")
      ),
      MedicationItem(
        id = "med_2",
        profileId = "profile_doug",
        name = "Atorvastatin Calcium",
        dosage = "10 mg",
        frequency = "Take 1 tablet once daily at bedtime",
        prescriber = "Dr. Marcus Vance, MD",
        startDate = "Sep 25, 2025",
        status = ItemStatus.ACTIVE,
        indication = "Cardiovascular Prevention & Lipids",
        conflictWarning = "Interaction Watch: Periodic LFT and renal function screening advised during concurrent metformin therapy.",
        isReconciled = true,
        provenance = Provenance("MetroHealth FHIR R4", "2026-09-22", 0.98f, "Full Consent")
      ),
      MedicationItem(
        id = "med_3",
        profileId = "profile_doug",
        name = "Vitamin D3 (Cholecalciferol)",
        dosage = "2000 IU",
        frequency = "Take 1 capsule daily with food",
        prescriber = "Patient Self-Reported",
        startDate = "Jan 15, 2026",
        status = ItemStatus.ACTIVE,
        indication = "Bone Health & Immune Support",
        conflictWarning = null,
        isReconciled = true,
        provenance = Provenance("Patient Manual Entry", "2026-01-15", 0.95f, "Patient Owned")
      ),
      MedicationItem(
        id = "med_4",
        profileId = "profile_doug",
        name = "Amoxicillin (Penicillin class)",
        dosage = "500 mg",
        frequency = "Discontinued - Allergy Flag",
        prescriber = "Historic Urgent Care Record",
        startDate = "Aug 12, 2018",
        status = ItemStatus.DISCONTINUED,
        indication = "Historic Treatment - Contraindicated",
        conflictWarning = "CRITICAL CONTRAINDICATION: Patient has confirmed severe allergic anaphylactoid response to beta-lactam antibiotics.",
        isReconciled = true,
        provenance = Provenance("Hospital Discharge Summary", "2018-08-12", 1.0f, "Verified EHR")
      )
    )
  }

  private fun initialGrants(): List<CareGrant> {
    return listOf(
      CareGrant(
        id = "grant_1",
        profileId = "profile_doug",
        recipientName = "Dr. Sarah Chen",
        recipientRole = "Endocrinology Specialist",
        scope = "Full Longitudinal Record",
        durationLabel = "30 days",
        expiresAt = "Nov 14, 2026",
        tokenKey = "vtl_share_sec_8912ba09",
        accessCount = 4,
        isActive = true
      ),
      CareGrant(
        id = "grant_2",
        profileId = "profile_doug",
        recipientName = "Dr. Marcus Vance",
        recipientRole = "Preventive Cardiology",
        scope = "Labs & Medications Only",
        durationLabel = "7 days",
        expiresAt = "Oct 28, 2026",
        tokenKey = "vtl_share_sec_4102cd34",
        accessCount = 1,
        isActive = true
      ),
      CareGrant(
        id = "grant_3",
        profileId = "profile_doug",
        recipientName = "Emergency First Responders",
        recipientRole = "Emergency Lock-Screen QR",
        scope = "Emergency Snapshot Only (Allergies + Meds)",
        durationLabel = "Continuous Active",
        expiresAt = "Permanent (Revocable)",
        tokenKey = "vtl_emg_qr_7718de55",
        accessCount = 0,
        isActive = true
      )
    )
  }

  private fun initialAuditLogs(): List<AuditEntry> {
    return listOf(
      AuditEntry("aud_101", "2026-10-21 09:12 UTC", "Doug Adarkwah", "FHIR_BUNDLE_EXPORT", "Complete Patient Record FHIR R4 JSON", "SUCCESS"),
      AuditEntry("aud_102", "2026-10-20 18:45 UTC", "Dr. Sarah Chen (Granted Token)", "SECURE_SHARE_ACCESS", "Viewing Longitudinal Lab Trend (A1c & Lipids)", "SUCCESS"),
      AuditEntry("aud_103", "2026-10-19 08:30 UTC", "Doug Adarkwah", "AI_GROUNDED_QUERY", "Interpretation of HbA1c 6.4% result", "SUCCESS"),
      AuditEntry("aud_104", "2026-10-14 14:23 UTC", "Quest Diagnostics Connector", "OCR_FHIR_INGEST", "DiagnosticReport: Hemoglobin A1c", "SUCCESS"),
      AuditEntry("aud_105", "2026-10-14 14:20 UTC", "Doug Adarkwah", "CONSENT_VERIFY", "SMART-on-FHIR OAuth 2.0 Token Refresh", "SUCCESS")
    )
  }

  private fun initialChatHistory(): List<ChatMessage> {
    return listOf(
      // Doug
      ChatMessage(
        id = "msg_init_doug_1",
        profileId = "profile_doug",
        sender = "system",
        text = "Welcome to Doug's Intelligent Interpreter. I answer questions grounded strictly in Doug's personal health records, with citations to source documents. I explain and coordinate; I do not diagnose.",
        timestamp = "System Active",
        citations = emptyList()
      ),
      ChatMessage(
        id = "msg_init_doug_2",
        profileId = "profile_doug",
        sender = "user",
        text = "How is my A1c trending over the last few months?",
        timestamp = "Oct 19, 2026 • 09:15"
      ),
      ChatMessage(
        id = "msg_init_doug_3",
        profileId = "profile_doug",
        sender = "ai",
        text = "Your Hemoglobin A1c has shown positive improvement. On Oct 14, 2026, Quest Diagnostics reported your A1c at 6.4%, down from 6.8% in July 2026.\n\nWhile 6.4% is still within the pre-diabetes bracket (5.7% – 6.4%), the 0.4% reduction indicates that your combination of Metformin 500mg BID and dietary adjustments is yielding measurable clinical progress.",
        timestamp = "Oct 19, 2026 • 09:15",
        citations = listOf(
          Citation(
            id = "cit_1",
            title = "Quest Diagnostics Lab Panel",
            date = "Oct 14, 2026",
            source = "Quest FHIR Endpoint",
            excerpt = "HbA1c = 6.4% (Pre-diabetes: 5.7 - 6.4%). Prior = 6.8%."
          ),
          Citation(
            id = "cit_2",
            title = "Endocrinology Visit Note • Dr. Sarah Chen",
            date = "Oct 14, 2026",
            source = "Bayfront Health Epic",
            excerpt = "Metformin tolerance verified. Glycemic metrics moving toward target < 6.0%."
          )
        )
      ),

      // Maya
      ChatMessage(
        id = "msg_init_maya_1",
        profileId = "profile_maya",
        sender = "system",
        text = "Welcome to Maya's Pediatric Health Interpreter. Grounded in Maya's pediatric immunizations, growth records, and allergy action plan.",
        timestamp = "System Active",
        citations = emptyList()
      ),
      ChatMessage(
        id = "msg_init_maya_2",
        profileId = "profile_maya",
        sender = "user",
        text = "Are Maya's immunizations up to date for 2nd grade?",
        timestamp = "Oct 20, 2026 • 14:10"
      ),
      ChatMessage(
        id = "msg_init_maya_3",
        profileId = "profile_maya",
        sender = "ai",
        text = "Yes, Maya's immunization records show she has completed all required kindergarten and early elementary boosters. Her 2nd MMR and Varicella doses were administered on Aug 18, 2025, and her seasonal influenza booster was documented on Sep 12, 2026. Her next routine wellness visit is scheduled with Dr. Michael Chen.",
        timestamp = "Oct 20, 2026 • 14:10",
        citations = listOf(
          Citation(
            id = "cit_m1",
            title = "Metro Pediatric Clinic • MMR & Varicella Booster",
            date = "Aug 18, 2025",
            source = "Metro Pediatric Epic",
            excerpt = "Administered dose 2 MMR & Varicella. Well tolerated, no adverse reactions."
          ),
          Citation(
            id = "cit_m2",
            title = "CVS MinuteClinic • Annual Influenza Vaccine",
            date = "Sep 12, 2026",
            source = "CVS Health FHIR Portal",
            excerpt = "Influenza Quadrivalent 0.5 mL IM. School clearance voucher issued."
          )
        )
      ),

      // Kwame
      ChatMessage(
        id = "msg_init_kwame_1",
        profileId = "profile_kwame",
        sender = "system",
        text = "Welcome to Kwame's Senior Health Interpreter. Grounded in Kwame's cardiology, nephrology, and 4 active medications.",
        timestamp = "System Active",
        citations = emptyList()
      ),
      ChatMessage(
        id = "msg_init_kwame_2",
        profileId = "profile_kwame",
        sender = "user",
        text = "What is my latest kidney function and eGFR reading?",
        timestamp = "Oct 22, 2026 • 10:45"
      ),
      ChatMessage(
        id = "msg_init_kwame_3",
        profileId = "profile_kwame",
        sender = "ai",
        text = "Kwame's latest Comprehensive Metabolic Panel from Oct 10, 2026 (St. Luke's Lab) indicates an eGFR of 48 mL/min/1.73m² with Serum Creatinine at 1.58 mg/dL. This is classified as CKD Stage 3a (moderate reduction), but has remained stable compared to his 49 mL/min baseline from April 2026. Blood pressure is well-controlled at 132/78 mmHg on Amlodipine 5mg and Empagliflozin 10mg.",
        timestamp = "Oct 22, 2026 • 10:45",
        citations = listOf(
          Citation(
            id = "cit_k1",
            title = "St. Luke's Regional Lab • Comprehensive Metabolic Panel",
            date = "Oct 10, 2026",
            source = "St. Luke's Cerner FHIR",
            excerpt = "eGFR: 48 mL/min/1.73m² (CKD 3a). Creatinine: 1.58 mg/dL. Baseline stable."
          ),
          Citation(
            id = "cit_k2",
            title = "Cardiology Clinical Encounter • Dr. Angela Rossi",
            date = "Oct 12, 2026",
            source = "St. Luke's Heart Institute",
            excerpt = "LAD stent patent. Blood pressure stable on Amlodipine 5mg. Renal function monitoring aligned with nephrology."
          )
        )
      )
    )
  }

  private fun initialReminders(): List<MedicationReminder> {
    return listOf(
      MedicationReminder(
        id = "rem_metformin",
        profileId = "profile_doug",
        medicationId = "med_1",
        medicationName = "Metformin HCl",
        dosage = "500 mg",
        hour = 8,
        minute = 0,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        isEnabled = true,
        instructions = "Take with morning breakfast & full glass of water",
        isTakenToday = true
      ),
      MedicationReminder(
        id = "rem_lisinopril",
        profileId = "profile_doug",
        medicationId = "med_3",
        medicationName = "Lisinopril",
        dosage = "10 mg",
        hour = 8,
        minute = 0,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        isEnabled = true,
        instructions = "Take every morning for blood pressure control",
        isTakenToday = true
      ),
      MedicationReminder(
        id = "rem_atorvastatin",
        profileId = "profile_doug",
        medicationId = "med_2",
        medicationName = "Atorvastatin Calcium",
        dosage = "20 mg",
        hour = 21,
        minute = 0,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        isEnabled = true,
        instructions = "Take at bedtime after evening meal",
        isTakenToday = false
      )
    )
  }

  private fun initialMayaTimeline(): List<HealthTimelineItem> {
    return listOf(
      HealthTimelineItem(
        id = "tl_maya_1",
        profileId = "profile_maya",
        category = TimelineCategory.IMMUNIZATION,
        title = "MMR (Measles, Mumps, Rubella) Booster",
        subtitle = "Metro Pediatric Clinic • 5-6 Year Booster Dose",
        date = "Aug 15, 2025",
        providerOrFacility = "Metro Pediatric Associates",
        status = ItemStatus.NORMAL,
        clinicalNotes = "Kindergarten entry booster administered subcutaneously in right upper outer triceps. Tolerated well without localized erythema or fever.",
        provenance = Provenance(
          sourceSystem = "State Immunization Information System (IIS)",
          ingestedAt = "2025-08-15 11:20 UTC",
          confidenceScore = 0.99f,
          consentState = "Parental Proxy Consent",
          documentHash = "sha256:bb12fa98310c...55a1"
        ),
        tags = listOf("Immunization", "Pediatric", "MMR", "Booster"),
        keyValues = mapOf("Dose" to "Dose 2 of 2", "Route" to "Subcutaneous", "Lot" to "MMR-5542B")
      ),
      HealthTimelineItem(
        id = "tl_maya_2",
        profileId = "profile_maya",
        category = TimelineCategory.ENCOUNTER,
        title = "7-Year Pediatric Well-Child Checkup",
        subtitle = "Dr. Michael Chen, MD • Comprehensive Wellness",
        date = "Aug 22, 2026",
        providerOrFacility = "Metro Pediatric Associates",
        status = ItemStatus.NORMAL,
        clinicalNotes = "Normal pediatric growth velocity (72nd percentile height, 65th percentile weight). Vision screening 20/20 bilateral. Hearing screening normal. Cognitive, developmental, and social milestones fully met for age 7.",
        provenance = Provenance(
          sourceSystem = "Epic MyChart Pediatrics",
          ingestedAt = "2026-08-22 15:45 UTC",
          confidenceScore = 1.0f,
          consentState = "Parental Proxy Consent",
          documentHash = "sha256:7721cc8912ef...0042"
        ),
        tags = listOf("Well-Child", "Pediatrics", "Growth Chart", "Dr. Chen"),
        keyValues = mapOf("Height" to "122 cm (72nd %ile)", "Weight" to "23.5 kg (65th %ile)", "BMI" to "15.8 (Normal)")
      ),
      HealthTimelineItem(
        id = "tl_maya_3",
        profileId = "profile_maya",
        category = TimelineCategory.ALLERGY,
        title = "Allergy: Peanut (Mild / Localized Urticaria)",
        subtitle = "Pediatric Allergy Assessment • Dr. Michael Chen",
        date = "May 10, 2023",
        providerOrFacility = "Children's Allergy & Asthma Care",
        status = ItemStatus.ELEVATED,
        clinicalNotes = "Localized perioral erythema and mild hives upon trace contact. No respiratory compromise, bronchospasm, or anaphylaxis. Family carries pediatric Cetirizine; EpiPen Jr prescribed as precautionary school rescue.",
        provenance = Provenance(
          sourceSystem = "Children's Specialty Care EHR",
          ingestedAt = "2026-01-15 10:10 UTC",
          confidenceScore = 0.99f,
          consentState = "Parental Proxy Consent",
          documentHash = "sha256:ea33910245fd...6712"
        ),
        tags = listOf("Allergy", "Peanut", "Pediatric Alert", "School Action Plan"),
        keyValues = mapOf("Severity" to "Mild-Moderate", "Reaction" to "Perioral Urticaria", "Rescue" to "Cetirizine / EpiPen Jr")
      ),
      HealthTimelineItem(
        id = "tl_maya_4",
        profileId = "profile_maya",
        category = TimelineCategory.IMMUNIZATION,
        title = "Annual Pediatric Influenza Vaccine",
        subtitle = "Flumist Quadrivalent Intranasal Spray",
        date = "Sep 28, 2026",
        providerOrFacility = "CVS MinuteClinic Pediatrics",
        status = ItemStatus.NORMAL,
        clinicalNotes = "0.2 mL intranasal suspension (0.1 mL per nostril). Patient cooperative, no immediate adverse reactions observed during 15-minute wait.",
        provenance = Provenance(
          sourceSystem = "State Immunization Information System (IIS)",
          ingestedAt = "2026-09-28 17:00 UTC",
          confidenceScore = 0.98f,
          consentState = "Parental Proxy Consent",
          documentHash = "sha256:0981aacc2233...ff88"
        ),
        tags = listOf("Immunization", "Flu Vaccine", "Pediatric", "Preventive"),
        keyValues = mapOf("Vaccine" to "Intranasal Quadrivalent", "Season" to "2026-2027", "Expiry" to "2027-04")
      ),
      HealthTimelineItem(
        id = "tl_maya_5",
        profileId = "profile_maya",
        category = TimelineCategory.LAB,
        title = "Pediatric Lead & Hemoglobin Screening",
        subtitle = "Quest Diagnostics • Blood Lead Undetectable",
        date = "Aug 20, 2025",
        providerOrFacility = "Quest Diagnostics Pediatric Hub",
        status = ItemStatus.NORMAL,
        clinicalNotes = "Capillary blood lead level < 1.0 ug/dL (normal / undetectable). Hemoglobin: 12.8 g/dL (normal for age 6-7). Ferritin: 45 ng/mL.",
        provenance = Provenance(
          sourceSystem = "Quest Diagnostics HL7/FHIR v4.0.1",
          ingestedAt = "2025-08-20 13:00 UTC",
          confidenceScore = 1.0f,
          consentState = "Parental Proxy Consent",
          documentHash = "sha256:4421bb9900ee...3311"
        ),
        tags = listOf("Lab", "Pediatric", "Lead Screening", "Iron"),
        keyValues = mapOf("Blood Lead" to "< 1.0 ug/dL", "Hemoglobin" to "12.8 g/dL", "Ferritin" to "45 ng/mL")
      )
    )
  }

  private fun initialKwameTimeline(): List<HealthTimelineItem> {
    return listOf(
      HealthTimelineItem(
        id = "tl_kwame_1",
        profileId = "profile_kwame",
        category = TimelineCategory.PROCEDURE,
        title = "Coronary Stent Surveillance & Angiogram",
        subtitle = "Dr. Angela Rossi, MD • Interventional Cardiology",
        date = "Sep 18, 2026",
        providerOrFacility = "St. Luke's Heart & Vascular Institute",
        status = ItemStatus.NORMAL,
        clinicalNotes = "Prior drug-eluting stent (mid-LAD placed 2024) remains 100% patent with excellent TIMI 3 distal flow. No restenosis or thrombosis. Left ventricular ejection fraction preserved at 55%. Continue dual antiplatelet and statin therapy as scheduled.",
        provenance = Provenance(
          sourceSystem = "St. Luke's Epic FHIR R4",
          ingestedAt = "2026-09-18 16:30 UTC",
          confidenceScore = 1.0f,
          consentState = "Authorized Family Caregiver",
          documentHash = "sha256:55aa11223344...9988"
        ),
        tags = listOf("Cardiology", "Coronary Stent", "LAD", "Angiogram", "TIMI 3"),
        keyValues = mapOf("Stent Location" to "Mid-LAD (DES)", "Patency" to "100% Patent / TIMI 3", "LVEF" to "55% (Preserved)")
      ),
      HealthTimelineItem(
        id = "tl_kwame_2",
        profileId = "profile_kwame",
        category = TimelineCategory.LAB,
        title = "Comprehensive Renal Function & eGFR Panel",
        subtitle = "MetroHealth Nephrology Lab • CKD Stage 3a Stable",
        date = "Oct 05, 2026",
        providerOrFacility = "MetroHealth Outpatient Laboratories",
        status = ItemStatus.ELEVATED,
        clinicalNotes = "eGFR: 48 mL/min/1.73m² (Chronic Kidney Disease Stage 3a, stable across past 12 months). Serum Creatinine: 1.58 mg/dL. Urine Albumin-to-Creatinine Ratio (UACR): 120 mg/g. Renal parameters stable under cardiorenal SGLT2 inhibitor regimen.",
        provenance = Provenance(
          sourceSystem = "MetroHealth Cerner Millennium FHIR",
          ingestedAt = "2026-10-05 14:15 UTC",
          confidenceScore = 0.99f,
          consentState = "Authorized Family Caregiver",
          documentHash = "sha256:8812dd4411aa...77cc"
        ),
        tags = listOf("Nephrology", "Renal", "eGFR", "CKD Stage 3a", "Creatinine"),
        keyValues = mapOf("eGFR" to "48 mL/min/1.73m²", "Creatinine" to "1.58 mg/dL", "UACR" to "120 mg/g", "Stage" to "CKD 3a (Stable)")
      ),
      HealthTimelineItem(
        id = "tl_kwame_3",
        profileId = "profile_kwame",
        category = TimelineCategory.LAB,
        title = "Hemoglobin A1c (Senior Diabetes Monitoring)",
        subtitle = "Quest Diagnostics • HbA1c: 7.3%",
        date = "Sep 20, 2026",
        providerOrFacility = "Quest Diagnostics Geriatric Center",
        status = ItemStatus.ELEVATED,
        clinicalNotes = "HbA1c: 7.3% (ADA geriatric guideline target < 7.5% for older adults with cardiovascular co-morbidities). Fasting glucose: 138 mg/dL. Glycemic stability maintained on Empagliflozin without episodes of hypoglycemia.",
        provenance = Provenance(
          sourceSystem = "Quest Diagnostics HL7/FHIR v4.0.1",
          ingestedAt = "2026-09-20 12:40 UTC",
          confidenceScore = 0.99f,
          consentState = "Authorized Family Caregiver",
          documentHash = "sha256:11447788aa99...bb22"
        ),
        tags = listOf("Endocrinology", "Diabetes", "A1c", "Geriatric Care"),
        keyValues = mapOf("Value" to "7.3%", "Target" to "< 7.5% (Geriatric)", "Fasting Glucose" to "138 mg/dL")
      ),
      HealthTimelineItem(
        id = "tl_kwame_4",
        profileId = "profile_kwame",
        category = TimelineCategory.ENCOUNTER,
        title = "Geriatric & Cardiology Coordinated Care Visit",
        subtitle = "Dr. Robert Vance, MD • Stent & Kidney Follow-up",
        date = "Oct 12, 2026",
        providerOrFacility = "Metro Geriatric Wellness Center",
        status = ItemStatus.NORMAL,
        clinicalNotes = "Sitting BP: 132/78 mmHg, standing BP: 128/76 mmHg (no orthostatic hypotension). Comprehensive medication reconciliation completed across cardiology, nephrology, and primary care. Stent recovery stable. Renal dosing verified.",
        provenance = Provenance(
          sourceSystem = "MetroHealth Epic SMART-on-FHIR",
          ingestedAt = "2026-10-12 17:10 UTC",
          confidenceScore = 1.0f,
          consentState = "Authorized Family Caregiver",
          documentHash = "sha256:9900aabbccdd...eeff"
        ),
        tags = listOf("Geriatrics", "Care Coordination", "Med Reconciliation", "Dr. Vance"),
        keyValues = mapOf("Sitting BP" to "132/78 mmHg", "Standing BP" to "128/76 mmHg", "Heart Rate" to "68 bpm")
      ),
      HealthTimelineItem(
        id = "tl_kwame_5",
        profileId = "profile_kwame",
        category = TimelineCategory.ALLERGY,
        title = "Allergy: Codeine (Severe Delirium & Neurotoxicity)",
        subtitle = "Adverse Drug Event Contraindication",
        date = "Mar 15, 2021",
        providerOrFacility = "Memorial Hospital Emergency Dept",
        status = ItemStatus.CRITICAL,
        clinicalNotes = "Patient experienced acute confusion, severe delirium, profuse vomiting, and profound postural instability following post-operative oral codeine. Absolute contraindication to all codeine-containing analgesics.",
        provenance = Provenance(
          sourceSystem = "Cerner Millennium FHIR R4",
          ingestedAt = "2026-01-12 09:30 UTC",
          confidenceScore = 1.0f,
          consentState = "Authorized Family Caregiver",
          documentHash = "sha256:665544332211...00aa"
        ),
        tags = listOf("Allergy", "Critical Alert", "Opiate Intolerance", "Contraindication"),
        keyValues = mapOf("Severity" to "Severe", "Reaction" to "Delirium / Neurotoxicity", "Status" to "Active Contraindication")
      )
    )
  }

  private fun initialMayaMeds(): List<MedicationItem> {
    return listOf(
      MedicationItem(
        id = "med_maya_1",
        profileId = "profile_maya",
        name = "Cetirizine HCl Oral Solution",
        dosage = "5 mg (5 mL)",
        frequency = "Take 5 mL once daily as needed for allergy flares",
        prescriber = "Dr. Michael Chen, MD (Pediatrics)",
        startDate = "May 10, 2023",
        status = ItemStatus.ACTIVE,
        indication = "Seasonal Allergies & Mild Urticaria",
        conflictWarning = null,
        isReconciled = true,
        provenance = Provenance("Children's Health EHR", "2026-08-22", 0.99f, "Parental Proxy Consent")
      ),
      MedicationItem(
        id = "med_maya_2",
        profileId = "profile_maya",
        name = "Children's Multivitamin + Iron Chewable",
        dosage = "1 chewable tablet",
        frequency = "Chew 1 tablet daily with breakfast",
        prescriber = "Dr. Michael Chen, MD",
        startDate = "Sep 01, 2024",
        status = ItemStatus.ACTIVE,
        indication = "Pediatric Nutritional & Iron Support",
        conflictWarning = null,
        isReconciled = true,
        provenance = Provenance("Parent Manual Entry", "2026-09-01", 0.95f, "Parental Proxy Consent")
      ),
      MedicationItem(
        id = "med_maya_3",
        profileId = "profile_maya",
        name = "Amoxicillin Pediatric Oral Suspension",
        dosage = "250 mg / 5 mL",
        frequency = "Completed 10-day acute course",
        prescriber = "Pediatric Urgent Care",
        startDate = "Jan 12, 2025",
        status = ItemStatus.DISCONTINUED,
        indication = "Acute Otitis Media (Resolved)",
        conflictWarning = null,
        isReconciled = true,
        provenance = Provenance("Urgent Care Discharge", "2025-01-22", 1.0f, "Verified EHR")
      )
    )
  }

  private fun initialKwameMeds(): List<MedicationItem> {
    return listOf(
      MedicationItem(
        id = "med_kwame_1",
        profileId = "profile_kwame",
        name = "Empagliflozin (Jardiance)",
        dosage = "10 mg",
        frequency = "Take 1 tablet by mouth daily in the morning",
        prescriber = "Dr. Angela Rossi, MD (Cardiology)",
        startDate = "Jan 10, 2024",
        status = ItemStatus.ACTIVE,
        indication = "Type 2 Diabetes & Cardiorenal Protection",
        conflictWarning = "Renal Watch: Monitor eGFR every 3 months; dosage appropriate for eGFR 48 mL/min.",
        isReconciled = true,
        provenance = Provenance("St. Luke's Epic FHIR", "2026-10-12", 0.99f, "Caregiver Authorized")
      ),
      MedicationItem(
        id = "med_kwame_2",
        profileId = "profile_kwame",
        name = "Clopidogrel (Plavix)",
        dosage = "75 mg",
        frequency = "Take 1 tablet daily with morning food",
        prescriber = "Dr. Angela Rossi, MD (Cardiology)",
        startDate = "Sep 18, 2024",
        status = ItemStatus.ACTIVE,
        indication = "Coronary Stent Antiplatelet Protection",
        conflictWarning = "Bleeding Precaution: Avoid OTC aspirin and NSAIDs. Notify dentists and surgeons prior to procedures.",
        isReconciled = true,
        provenance = Provenance("St. Luke's Epic FHIR", "2026-09-18", 1.0f, "Caregiver Authorized")
      ),
      MedicationItem(
        id = "med_kwame_3",
        profileId = "profile_kwame",
        name = "Amlodipine Besylate",
        dosage = "5 mg",
        frequency = "Take 1 tablet once daily in the morning",
        prescriber = "Dr. Robert Vance, MD (Geriatrics)",
        startDate = "Nov 02, 2023",
        status = ItemStatus.ACTIVE,
        indication = "Hypertension & Coronary Vasodilation",
        conflictWarning = null,
        isReconciled = true,
        provenance = Provenance("MetroHealth Cerner FHIR", "2026-10-12", 0.98f, "Caregiver Authorized")
      ),
      MedicationItem(
        id = "med_kwame_4",
        profileId = "profile_kwame",
        name = "Rosuvastatin Calcium",
        dosage = "20 mg",
        frequency = "Take 1 tablet daily at bedtime",
        prescriber = "Dr. Angela Rossi, MD (Cardiology)",
        startDate = "Oct 05, 2024",
        status = ItemStatus.ACTIVE,
        indication = "Coronary Artery Disease & Stent Patency",
        conflictWarning = null,
        isReconciled = true,
        provenance = Provenance("St. Luke's Epic FHIR", "2026-10-12", 0.99f, "Caregiver Authorized")
      )
    )
  }

  private fun initialMayaReminders(): List<MedicationReminder> {
    return listOf(
      MedicationReminder(
        id = "rem_maya_multivitamin",
        profileId = "profile_maya",
        medicationId = "med_maya_2",
        medicationName = "Children's Multivitamin",
        dosage = "1 chewable tablet",
        hour = 7,
        minute = 30,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        isEnabled = true,
        instructions = "Give with breakfast before school",
        isTakenToday = true
      )
    )
  }

  private fun initialKwameReminders(): List<MedicationReminder> {
    return listOf(
      MedicationReminder(
        id = "rem_kwame_jardiance",
        profileId = "profile_kwame",
        medicationId = "med_kwame_1",
        medicationName = "Empagliflozin (Jardiance)",
        dosage = "10 mg",
        hour = 8,
        minute = 0,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        isEnabled = true,
        instructions = "Take with morning water & breakfast",
        isTakenToday = true
      ),
      MedicationReminder(
        id = "rem_kwame_plavix",
        profileId = "profile_kwame",
        medicationId = "med_kwame_2",
        medicationName = "Clopidogrel (Plavix)",
        dosage = "75 mg",
        hour = 8,
        minute = 30,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        isEnabled = true,
        instructions = "Take with breakfast; do not crush",
        isTakenToday = true
      ),
      MedicationReminder(
        id = "rem_kwame_amlodipine",
        profileId = "profile_kwame",
        medicationId = "med_kwame_3",
        medicationName = "Amlodipine Besylate",
        dosage = "5 mg",
        hour = 9,
        minute = 0,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        isEnabled = true,
        instructions = "Take for blood pressure regulation",
        isTakenToday = true
      ),
      MedicationReminder(
        id = "rem_kwame_rosuvastatin",
        profileId = "profile_kwame",
        medicationId = "med_kwame_4",
        medicationName = "Rosuvastatin Calcium",
        dosage = "20 mg",
        hour = 21,
        minute = 30,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        isEnabled = true,
        instructions = "Take at bedtime",
        isTakenToday = false
      )
    )
  }

  private fun initialMayaGrants(): List<CareGrant> {
    return listOf(
      CareGrant(
        id = "grant_maya_1",
        profileId = "profile_maya",
        recipientName = "Dr. Michael Chen",
        recipientRole = "Primary Pediatrician",
        scope = "Full Longitudinal Pediatric Record",
        durationLabel = "30 days",
        expiresAt = "Nov 20, 2026",
        tokenKey = "vtl_share_ped_4412aa01",
        accessCount = 2,
        isActive = true
      ),
      CareGrant(
        id = "grant_maya_2",
        profileId = "profile_maya",
        recipientName = "Oakridge Elementary School Nurse",
        recipientRole = "School Health Office",
        scope = "Immunization & Allergy Records Only",
        durationLabel = "180 days",
        expiresAt = "Apr 30, 2027",
        tokenKey = "vtl_share_sch_9912bb02",
        accessCount = 1,
        isActive = true
      )
    )
  }

  private fun initialKwameGrants(): List<CareGrant> {
    return listOf(
      CareGrant(
        id = "grant_kwame_1",
        profileId = "profile_kwame",
        recipientName = "Dr. Angela Rossi",
        recipientRole = "Cardiologist (St. Luke's)",
        scope = "Full Longitudinal Cardiac & Renal Record",
        durationLabel = "90 days",
        expiresAt = "Jan 10, 2027",
        tokenKey = "vtl_share_crd_7712cc03",
        accessCount = 5,
        isActive = true
      ),
      CareGrant(
        id = "grant_kwame_2",
        profileId = "profile_kwame",
        recipientName = "Dr. Robert Vance",
        recipientRole = "Geriatric Primary Care",
        scope = "Full Longitudinal Record",
        durationLabel = "365 days",
        expiresAt = "Oct 12, 2027",
        tokenKey = "vtl_share_ger_8822dd04",
        accessCount = 3,
        isActive = true
      ),
      CareGrant(
        id = "grant_kwame_3",
        profileId = "profile_kwame",
        recipientName = "Senior Living Caregiver Team",
        recipientRole = "Daily Medication Administration",
        scope = "Medications & Reminders Only",
        durationLabel = "30 days",
        expiresAt = "Nov 28, 2026",
        tokenKey = "vtl_share_cgv_3311ee05",
        accessCount = 12,
        isActive = true
      )
    )
  }

  private fun initialProfilesList(): List<PatientProfile> {
    return listOf(
      PatientProfile(
        id = "profile_doug",
        name = "Doug Adarkwah",
        relationship = "Primary Patient",
        dob = "1988-04-12",
        bloodType = "O+",
        mrn = "VTL-8921-DA",
        isPrimary = true,
        activeConditionsCount = 2,
        activeMedicationsCount = 3,
        pendingTasksCount = 1,
        primaryPhysician = "Dr. Sarah Chen, MD",
        allergies = listOf("Penicillin / Beta-lactams"),
        emergencyContact = "Elena Adarkwah • +1 (555) 345-6789",
        notes = "Pre-diabetes metabolic optimization program",
        gender = "Male"
      ),
      PatientProfile(
        id = "profile_maya",
        name = "Maya Adarkwah",
        relationship = "Child",
        dob = "2019-03-24",
        bloodType = "O+",
        mrn = "VTL-4410-MA",
        isPrimary = false,
        activeConditionsCount = 1,
        activeMedicationsCount = 2,
        pendingTasksCount = 0,
        primaryPhysician = "Dr. Michael Chen, MD",
        allergies = listOf("Peanut (Mild / Localized Urticaria)"),
        emergencyContact = "Doug Adarkwah • +1 (555) 234-8901",
        notes = "Grade 2 elementary student, annual pediatric wellness",
        gender = "Female"
      ),
      PatientProfile(
        id = "profile_kwame",
        name = "Kwame Adarkwah",
        relationship = "Elderly Parent",
        dob = "1949-11-08",
        bloodType = "B+",
        mrn = "VTL-1109-KA",
        isPrimary = false,
        activeConditionsCount = 3,
        activeMedicationsCount = 4,
        pendingTasksCount = 2,
        primaryPhysician = "Dr. Angela Rossi, MD",
        allergies = listOf("Codeine (Severe Delirium / Neurotoxicity)"),
        emergencyContact = "Doug Adarkwah • +1 (555) 234-8901",
        notes = "Post-LAD stent recovery & CKD Stage 3a management",
        gender = "Male"
      )
    )
  }

  private fun initialPaymentHistoryList(): List<PaymentTransaction> {
    return listOf(
      PaymentTransaction(
        id = "TXN-VTL-2026-98124",
        date = "Oct 26, 2026",
        tier = SubscriptionTier.PREMIUM,
        billingCycle = BillingCycle.ANNUAL,
        amountPaid = 89.88,
        paymentMethod = "Google Pay (abydouglas@gmail.com)",
        status = "COMPLETED",
        invoiceNumber = "INV-VTL-2026-8812"
      ),
      PaymentTransaction(
        id = "TXN-VTL-2025-11042",
        date = "Oct 26, 2025",
        tier = SubscriptionTier.PREMIUM,
        billingCycle = BillingCycle.ANNUAL,
        amountPaid = 89.88,
        paymentMethod = "Google Pay (abydouglas@gmail.com)",
        status = "COMPLETED",
        invoiceNumber = "INV-VTL-2025-4190"
      )
    )
  }

  // --- Symptom Tracking Module ---

  fun addSymptom(item: SymptomItem) {
    _symptoms.update { listOf(item) + it }
    recordAudit("LOG_SYMPTOM", "Observation (Symptom: ${item.symptomName})")
    if (database != null) {
      scope.launch {
        database.symptomDao().insert(SymptomEntity.fromDomain(item))
      }
    }
  }

  fun updateSymptom(item: SymptomItem) {
    _symptoms.update { list ->
      list.map { if (it.id == item.id) item else it }
    }
    if (database != null) {
      scope.launch {
        database.symptomDao().insert(SymptomEntity.fromDomain(item))
      }
    }
  }

  fun closeSymptom(symptomId: String, endedTimestamp: Long = System.currentTimeMillis()) {
    val endedDateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(endedTimestamp))
    _symptoms.update { list ->
      list.map {
        if (it.id == symptomId) {
          it.copy(
            isOngoing = false,
            endedDate = endedDateStr,
            endedTimestamp = endedTimestamp
          )
        } else it
      }
    }
    if (database != null) {
      scope.launch {
        val current = _symptoms.value.find { it.id == symptomId }
        if (current != null) {
          database.symptomDao().insert(SymptomEntity.fromDomain(current))
        }
      }
    }
  }

  fun deleteSymptom(id: String) {
    _symptoms.update { list -> list.filterNot { it.id == id } }
    if (database != null) {
      scope.launch {
        database.symptomDao().deleteById(id)
      }
    }
  }

  // CLINICAL_REVIEW_REQUIRED: Red-flag triage layer rules for high-risk presentations
  fun evaluateRedFlags(
    symptomName: String,
    severity: Int,
    notes: String = "",
    contextTags: List<String> = emptyList()
  ): RedFlagAlert? {
    val nameLower = symptomName.lowercase()
    val notesLower = notes.lowercase()
    val combinedText = "$nameLower $notesLower"

    // Rule 1: Chest pain + shortness of breath / radiation
    // CLINICAL_REVIEW_REQUIRED
    if ((nameLower.contains("chest") || notesLower.contains("chest")) &&
      (nameLower.contains("breath") || notesLower.contains("short of breath") || notesLower.contains("arm") || severity >= 7)) {
      return RedFlagAlert(
        title = "Potential Acute Cardiac / Respiratory Event",
        message = "Chest pain accompanied by severe discomfort, radiation, or breathing difficulty requires immediate medical evaluation. Vitalith cannot diagnose emergencies.",
        actionLabel = "Call Emergency Services (911)",
        emergencyPhone = "911",
        isEmergency = true
      )
    }

    // Rule 2: Sudden worst-ever thunderclap headache
    // CLINICAL_REVIEW_REQUIRED
    if (nameLower.contains("headache") || nameLower.contains("migraine")) {
      if (severity >= 9 || notesLower.contains("worst") || notesLower.contains("thunderclap") || notesLower.contains("sudden explosion")) {
        return RedFlagAlert(
          title = "Sudden Severe Headache (Thunderclap Onset)",
          message = "Sudden, explosive or worst-of-life headaches warrant urgent emergency clinical evaluation to rule out acute vascular pathology.",
          actionLabel = "Seek Emergency Medical Care",
          emergencyPhone = "911",
          isEmergency = true
        )
      }
    }

    // Rule 3: FAST stroke signs
    // CLINICAL_REVIEW_REQUIRED
    if (combinedText.contains("droop") || combinedText.contains("slur") || combinedText.contains("numbness") || combinedText.contains("one side") || combinedText.contains("facial")) {
      return RedFlagAlert(
        title = "Potential Acute Neurological Event (FAST Signs)",
        message = "Sudden facial drooping, arm weakness, or difficulty speaking are time-critical neurological signs.",
        actionLabel = "Call Emergency Services (911)",
        emergencyPhone = "911",
        isEmergency = true
      )
    }

    // Rule 4: Anaphylaxis signs (throat swelling/tightness + rash/hives/wheeze)
    // CLINICAL_REVIEW_REQUIRED
    if (combinedText.contains("throat") && (combinedText.contains("tight") || combinedText.contains("swell") || combinedText.contains("rash") || combinedText.contains("hive"))) {
      return RedFlagAlert(
        title = "Potential Severe Allergic Reaction (Anaphylaxis)",
        message = "Throat constriction or swelling alongside rash or breathing difficulty can rapidly progress. Administer epinephrine if prescribed and seek immediate emergency care.",
        actionLabel = "Call Emergency (911) / Use EpiPen",
        emergencyPhone = "911",
        isEmergency = true
      )
    }

    // Rule 5: High fever + stiff neck
    // CLINICAL_REVIEW_REQUIRED
    if ((combinedText.contains("stiff neck") || combinedText.contains("neck stiffness")) &&
      (combinedText.contains("fever") || severity >= 7)) {
      return RedFlagAlert(
        title = "Urgent: Fever with Neck Stiffness",
        message = "Combination of fever and neck stiffness requires prompt same-day evaluation at an urgent care or emergency room.",
        actionLabel = "Locate Nearest Urgent Care",
        emergencyPhone = "911",
        isEmergency = false
      )
    }

    return null
  }

  // CLINICAL_REVIEW_REQUIRED: Statistical recurrence and temporal co-occurrence calculation
  fun calculateSymptomRecurrence(profileId: String, symptomName: String): SymptomRecurrenceStats {
    val items = _symptoms.value.filter {
      it.profileId == profileId && it.symptomName.equals(symptomName, ignoreCase = true)
    }
    val totalThisMonth = items.size
    val totalLastMonth = maxOf(0, totalThisMonth - 2)
    val avgSev = if (items.isNotEmpty()) items.map { it.severity }.average().toFloat() else 0f
    val peakSev = items.maxOfOrNull { it.severity } ?: 0

    val contextFrequency = mutableMapOf<String, Int>()
    items.forEach { item ->
      item.contextTags.forEach { tag ->
        contextFrequency[tag] = (contextFrequency[tag] ?: 0) + 1
      }
    }
    val topContext = contextFrequency.maxByOrNull { it.value }?.key

    // CLINICAL_REVIEW_REQUIRED: Purely observational co-occurrence copy with zero causal claims
    val observation = if (topContext != null && items.size >= 3) {
      val countWithContext = contextFrequency[topContext] ?: 0
      val pct = (countWithContext * 100) / items.size
      "Observed on $pct% of logged episodes alongside '$topContext' (n=${items.size}). Note this temporal association for clinical review with your doctor."
    } else if (items.size >= 2) {
      "Observed ${items.size} episodes recorded for this profile. Share this frequency log with your care provider."
    } else {
      null
    }

    return SymptomRecurrenceStats(
      symptomName = symptomName,
      totalEpisodesThisMonth = totalThisMonth,
      totalEpisodesLastMonth = totalLastMonth,
      averageSeverity = avgSev,
      peakSeverity = peakSev,
      mostCommonContext = topContext,
      coOccurrenceObservation = observation
    )
  }

  fun parseNaturalLanguageSymptom(input: String): Pair<SymptomItem, RedFlagAlert?> {
    val trimmed = input.trim()
    val lower = trimmed.lowercase()

    // Match symptom name from catalog or fallback to first segment
    val matchedDef = SymptomCatalog.definitions.find { def ->
      lower.contains(def.name.lowercase().split("/").first().trim()) ||
        def.name.lowercase().split("/").any { part -> lower.contains(part.trim()) } ||
        (def.id == "sym_migraine" && (lower.contains("migraine") || lower.contains("headache"))) ||
        (def.id == "sym_acid_reflux" && (lower.contains("reflux") || lower.contains("heartburn") || lower.contains("acid"))) ||
        (def.id == "sym_chest_tightness" && lower.contains("chest")) ||
        (def.id == "sym_lower_back" && lower.contains("back")) ||
        (def.id == "sym_joint_pain" && (lower.contains("joint") || lower.contains("knee") || lower.contains("stiff") || lower.contains("gout"))) ||
        (def.id == "sym_fatigue" && (lower.contains("fatigue") || lower.contains("tired") || lower.contains("exhausted"))) ||
        (def.id == "sym_skin_rash" && (lower.contains("rash") || lower.contains("hive")))
    }

    val symptomName = matchedDef?.name ?: if (trimmed.contains(",")) trimmed.split(",").first().trim().replaceFirstChar { it.uppercase() } else trimmed.take(30).replaceFirstChar { it.uppercase() }
    val snomed = matchedDef?.snomedCode ?: "418290006"

    // Parse severity number
    val severityRegex = """(\b[0-9]|10)\b(?:\s*(?:out of|\/)\s*10)?""".toRegex()
    val match = severityRegex.find(lower)
    var severity = match?.groupValues?.get(1)?.toIntOrNull() ?: 5
    if (lower.contains("mild")) severity = 3
    if (lower.contains("moderate")) severity = 5
    if (lower.contains("severe") || lower.contains("worst")) severity = 8

    val sevLabel = when {
      severity <= 3 -> "Mild"
      severity <= 6 -> "Moderate"
      else -> "Severe"
    }

    // Extract context tags
    val tags = mutableListOf<String>()
    if (lower.contains("sleep") || lower.contains("tired") || lower.contains("exhausted")) tags.add("Poor Sleep (<6h)")
    if (lower.contains("stress") || lower.contains("work") || lower.contains("deadline")) tags.add("High Stress")
    if (lower.contains("meal") || lower.contains("food") || lower.contains("coffee") || lower.contains("fasting")) tags.add("Skipped Meal")
    if (lower.contains("period") || lower.contains("cycle") || lower.contains("luteal") || lower.contains("cramp")) tags.add("Luteal Phase")
    if (lower.contains("exercise") || lower.contains("run") || lower.contains("gym") || lower.contains("lift")) tags.add("Post-Exercise")
    if (lower.contains("med") || lower.contains("pill") || lower.contains("dose")) tags.add("Medication Taken")

    val currentDateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
    val redFlag = evaluateRedFlags(symptomName, severity, input, tags)

    val item = SymptomItem(
      id = "sym_nlp_${UUID.randomUUID().toString().take(8)}",
      profileId = _currentProfileId.value,
      symptomName = symptomName,
      snomedCode = snomed,
      severity = severity,
      severityLabel = sevLabel,
      onsetDate = currentDateStr,
      onsetTimestamp = System.currentTimeMillis(),
      isOngoing = true,
      bodySite = matchedDef?.defaultBodySite ?: "General",
      contextTags = tags,
      notes = input,
      isRedFlag = redFlag != null,
      redFlagMessage = redFlag?.message
    )

    return Pair(item, redFlag)
  }

  fun generateDoctorSymptomExportText(profileId: String): String {
    val curProfile = _profiles.value.find { it.id == profileId } ?: _profiles.value.first()
    val profileSymptoms = _symptoms.value.filter { it.profileId == profileId }
    val profileMeds = _medications.value.filter { it.profileId == profileId && it.status == ItemStatus.ACTIVE }
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())

    val sb = StringBuilder()
    sb.appendLine("=================================================================")
    sb.appendLine("           VITALITH HEALTH HISTORY • CLINICAL SYMPTOM REPORT     ")
    sb.appendLine("=================================================================")
    sb.appendLine("PATIENT: ${curProfile.name} | DOB: ${curProfile.dob} | MRN: ${curProfile.mrn}")
    sb.appendLine("GENERATED: ${SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date())}")
    sb.appendLine("PHYSICIAN: ${curProfile.primaryPhysician}")
    sb.appendLine("PURPOSE: Wellness Tracking & Longitudinal Symptom Observation")
    sb.appendLine("DISCLAIMER: Descriptive observational log for clinical review. Not diagnostic.")
    sb.appendLine("-----------------------------------------------------------------")
    sb.appendLine("CURRENT ACTIVE MEDICATIONS (${profileMeds.size}):")
    profileMeds.forEach { med ->
      sb.appendLine("  • ${med.name} ${med.dosage} (${med.frequency}) - Indication: ${med.indication}")
    }
    sb.appendLine("-----------------------------------------------------------------")
    sb.appendLine("RECORDED SYMPTOM EPISODES (${profileSymptoms.size} Total):")
    if (profileSymptoms.isEmpty()) {
      sb.appendLine("  No symptom episodes recorded for this reporting period.")
    } else {
      profileSymptoms.forEachIndexed { index, sym ->
        val statusStr = if (sym.isOngoing) "ONGOING" else "RESOLVED (Duration: ${sym.durationDisplay})"
        sb.appendLine("  [${index + 1}] ${sym.symptomName} (SNOMED-CT: ${sym.snomedCode})")
        sb.appendLine("      Onset: ${sym.onsetDate} | Status: $statusStr")
        sb.appendLine("      Severity: ${sym.severity}/10 (${sym.severityLabel}) | Site: ${sym.bodySite ?: "Unspecified"}")
        if (sym.contextTags.isNotEmpty()) {
          sb.appendLine("      Context Factors: ${sym.contextTags.joinToString(", ")}")
        }
        if (sym.impact.isNotEmpty()) {
          sb.appendLine("      Daily Life Impact: ${sym.impact.joinToString(", ")}")
        }
        if (sym.notes.isNotBlank()) {
          sb.appendLine("      Patient Notes: \"${sym.notes}\"")
        }
        if (sym.isRedFlag) {
          sb.appendLine("      * Triaged with Red-Flag Alert: ${sym.redFlagMessage}")
        }
        sb.appendLine("      FHIR Mapping: Observation?code=http://snomed.info/sct|${sym.snomedCode}&effectiveDateTime=${formatter.format(Date(sym.onsetTimestamp))}&valueInteger=${sym.severity}")
        sb.appendLine()
      }
    }
    sb.appendLine("-----------------------------------------------------------------")
    sb.appendLine("LONGITUDINAL RECURRENCE OBSERVATIONS:")
    val uniqueSymptomNames = profileSymptoms.map { it.symptomName }.distinct()
    uniqueSymptomNames.forEach { sName ->
      val stats = calculateSymptomRecurrence(profileId, sName)
      sb.appendLine("  • $sName: ${stats.totalEpisodesThisMonth} episode(s) recorded | Avg Severity: ${"%.1f".format(stats.averageSeverity)}/10 | Peak: ${stats.peakSeverity}/10")
      if (stats.coOccurrenceObservation != null) {
        sb.appendLine("    Pattern Observation: ${stats.coOccurrenceObservation}")
      }
    }
    sb.appendLine("=================================================================")
    sb.appendLine("END OF CLINICAL REPORT • VITALITH SECURE HEALTH ENCLAVE")
    return sb.toString()
  }

  private fun initialDougSymptoms(): List<SymptomItem> {
    return listOf(
      SymptomItem(
        id = "sym_doug_1",
        profileId = "profile_doug",
        symptomName = "Joint Stiffness / Ache",
        snomedCode = "57676002",
        severity = 6,
        severityLabel = "Moderate",
        onsetDate = "Oct 28, 2026",
        onsetTimestamp = System.currentTimeMillis() - 86400000L,
        isOngoing = true,
        bodySite = "Right 1st MTP (Big Toe)",
        impact = listOf("Work / Focus Limited", "Walking with limp"),
        contextTags = listOf("Post-Exercise", "Skipped Meal"),
        notes = "Felt sudden sharp twinge following morning jog; taking prescribed Colchicine",
        isRedFlag = false
      ),
      SymptomItem(
        id = "sym_doug_2",
        profileId = "profile_doug",
        symptomName = "Migraine / Headache",
        snomedCode = "37796009",
        severity = 4,
        severityLabel = "Moderate",
        onsetDate = "Oct 24, 2026",
        onsetTimestamp = System.currentTimeMillis() - (86400000L * 5),
        endedDate = "Oct 24, 2026",
        endedTimestamp = System.currentTimeMillis() - (86400000L * 5) + 14400000L,
        isOngoing = false,
        bodySite = "Right Temple",
        impact = listOf("Screen Time Interrupted"),
        contextTags = listOf("Poor Sleep (<6h)", "High Stress"),
        notes = "Dull throbbing right temple during quarterly sprint review",
        isRedFlag = false
      ),
      SymptomItem(
        id = "sym_doug_3",
        profileId = "profile_doug",
        symptomName = "Acid Reflux / Heartburn",
        snomedCode = "698065",
        severity = 3,
        severityLabel = "Mild",
        onsetDate = "Oct 19, 2026",
        onsetTimestamp = System.currentTimeMillis() - (86400000L * 10),
        endedDate = "Oct 19, 2026",
        endedTimestamp = System.currentTimeMillis() - (86400000L * 10) + 7200000L,
        isOngoing = false,
        bodySite = "Epigastric",
        impact = emptyList(),
        contextTags = listOf("Skipped Meal"),
        notes = "Subsided with water and elevation",
        isRedFlag = false
      )
    )
  }

  private fun initialMayaSymptoms(): List<SymptomItem> {
    return listOf(
      SymptomItem(
        id = "sym_maya_1",
        profileId = "profile_maya",
        symptomName = "Skin Rash / Hives",
        snomedCode = "271807003",
        severity = 4,
        severityLabel = "Moderate",
        onsetDate = "Oct 27, 2026",
        onsetTimestamp = System.currentTimeMillis() - (86400000L * 2),
        isOngoing = false,
        endedDate = "Oct 27, 2026",
        endedTimestamp = System.currentTimeMillis() - (86400000L * 2) + 10800000L,
        bodySite = "Arms / Torso",
        impact = listOf("Scratching / Play Distracted"),
        contextTags = listOf("High Stress"),
        notes = "Mild red urticaria patch after birthday cupcake at school. Resolved after oral antihistamine.",
        isRedFlag = false
      ),
      SymptomItem(
        id = "sym_maya_2",
        profileId = "profile_maya",
        symptomName = "Excessive Fatigue",
        snomedCode = "84229001",
        severity = 3,
        severityLabel = "Mild",
        onsetDate = "Oct 21, 2026",
        onsetTimestamp = System.currentTimeMillis() - (86400000L * 8),
        isOngoing = false,
        endedDate = "Oct 21, 2026",
        endedTimestamp = System.currentTimeMillis() - (86400000L * 8) + 21600000L,
        bodySite = "Whole Body",
        impact = listOf("Sleep Interrupted"),
        contextTags = listOf("Poor Sleep (<6h)"),
        notes = "Sleepy after soccer tournament",
        isRedFlag = false
      )
    )
  }

  private fun initialKwameSymptoms(): List<SymptomItem> {
    return listOf(
      SymptomItem(
        id = "sym_kwame_1",
        profileId = "profile_kwame",
        symptomName = "Lower Back Pain",
        snomedCode = "279039007",
        severity = 5,
        severityLabel = "Moderate",
        onsetDate = "Oct 26, 2026",
        onsetTimestamp = System.currentTimeMillis() - (86400000L * 3),
        isOngoing = true,
        bodySite = "Lumbar Spine",
        impact = listOf("Walking limited"),
        contextTags = listOf("Post-Exercise"),
        notes = "Ache after gardening in the morning; using lumbar support pillow",
        isRedFlag = false
      ),
      SymptomItem(
        id = "sym_kwame_2",
        profileId = "profile_kwame",
        symptomName = "Joint Stiffness / Ache",
        snomedCode = "57676002",
        severity = 6,
        severityLabel = "Moderate",
        onsetDate = "Oct 22, 2026",
        onsetTimestamp = System.currentTimeMillis() - (86400000L * 7),
        endedDate = "Oct 23, 2026",
        endedTimestamp = System.currentTimeMillis() - (86400000L * 6),
        isOngoing = false,
        bodySite = "Bilateral Knees",
        impact = listOf("Stairs Difficult"),
        contextTags = listOf("Poor Sleep (<6h)"),
        notes = "Morning stiffness lasting ~45 minutes before warm shower",
        isRedFlag = false
      )
    )
  }
}


package com.example.data.repository

import com.example.data.local.VitalithDatabase
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.MedicationEntity
import com.example.data.local.entities.TimelineItemEntity
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

  private val _profiles = MutableStateFlow<List<PatientProfile>>(
    listOf(
      PatientProfile(
        id = "profile_doug",
        name = "Doug Adarkwah",
        relationship = "Primary Account (Self)",
        dob = "1988-04-12 (38 yrs)",
        bloodType = "O+",
        mrn = "VTL-8921-DA",
        isPrimary = true,
        activeConditionsCount = 2,
        activeMedicationsCount = 3,
        pendingTasksCount = 2,
        primaryPhysician = "Dr. Sarah Jenkins, MD (Cardiology/IM)",
        allergies = listOf("Penicillin", "Sulfa antibiotics"),
        emergencyContact = "Elena Adarkwah • +1 (555) 345-6789",
        notes = "Executive physical completed Oct 2026. Monitoring blood pressure & lipid panels.",
        gender = "Male"
      ),
      PatientProfile(
        id = "profile_maya",
        name = "Maya Adarkwah",
        relationship = "Daughter (Child)",
        dob = "2019-08-20 (7 yrs)",
        bloodType = "A+",
        mrn = "VTL-4410-MA",
        isPrimary = false,
        activeConditionsCount = 1,
        activeMedicationsCount = 1,
        pendingTasksCount = 1,
        primaryPhysician = "Dr. Michael Chen, MD (Pediatrics)",
        allergies = listOf("Peanuts (Mild)"),
        emergencyContact = "Doug Adarkwah • +1 (555) 234-8901",
        notes = "Up to date on pediatric immunization schedule.",
        gender = "Female"
      ),
      PatientProfile(
        id = "profile_kwame",
        name = "Kwame Adarkwah (Senior)",
        relationship = "Father (Aging Parent)",
        dob = "1955-02-04 (71 yrs)",
        bloodType = "O+",
        mrn = "VTL-1109-KA",
        isPrimary = false,
        activeConditionsCount = 3,
        activeMedicationsCount = 4,
        pendingTasksCount = 3,
        primaryPhysician = "Dr. Robert Vance, MD (Geriatrics)",
        allergies = listOf("Codeine", "Aspirin (GI intolerance)"),
        emergencyContact = "Doug Adarkwah • +1 (555) 234-8901",
        notes = "Type 2 diabetes & coronary stent monitoring; regular nephrology follow-ups.",
        gender = "Male"
      )
    )
  )
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

  private val _paymentHistory = MutableStateFlow<List<PaymentTransaction>>(
    listOf(
      PaymentTransaction(
        id = "TXN-VTL-2026-98124",
        date = "Oct 26, 2026",
        tier = SubscriptionTier.PREMIUM,
        billingCycle = BillingCycle.ANNUAL,
        amountPaid = 89.88,
        paymentMethod = "Google Pay (abydouglas@gmail.com)",
        status = "COMPLETED",
        invoiceNumber = "INV-VTL-2026-0089"
      )
    )
  )
  val paymentHistory: StateFlow<List<PaymentTransaction>> = _paymentHistory.asStateFlow()

  private val _timelineItems = MutableStateFlow<List<HealthTimelineItem>>(initialDougTimeline())
  val timelineItems: StateFlow<List<HealthTimelineItem>> = _timelineItems.asStateFlow()

  private val _medications = MutableStateFlow<List<MedicationItem>>(initialDougMeds())
  val medications: StateFlow<List<MedicationItem>> = _medications.asStateFlow()

  private val _reminders = MutableStateFlow<List<MedicationReminder>>(initialReminders())
  val reminders: StateFlow<List<MedicationReminder>> = _reminders.asStateFlow()

  private val _careGrants = MutableStateFlow<List<CareGrant>>(initialGrants())
  val careGrants: StateFlow<List<CareGrant>> = _careGrants.asStateFlow()

  private val _auditLogs = MutableStateFlow<List<AuditEntry>>(initialAuditLogs())
  val auditLogs: StateFlow<List<AuditEntry>> = _auditLogs.asStateFlow()

  private val _chatMessages = MutableStateFlow<List<ChatMessage>>(initialChatHistory())
  val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

  init {
    if (database != null) {
      scope.launch {
        // Pre-populate Room database if empty for offline-first resilience
        if (database.timelineDao().getCount() == 0) {
          database.timelineDao().insertAll(initialDougTimeline().map { TimelineItemEntity.fromDomain(it) })
        }
        if (database.medicationDao().getCount() == 0) {
          database.medicationDao().insertAll(initialDougMeds().map { MedicationEntity.fromDomain(it) })
        }
        if (database.auditDao().getCount() == 0) {
          database.auditDao().insertAll(initialAuditLogs().map { AuditLogEntity.fromDomain(it) })
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
      }
    }
  }

  fun switchProfile(profileId: String) {
    _currentProfileId.value = profileId
    recordAudit("PROFILE_SWITCH", "User switched active profile to: $profileId")
  }

  fun updateUserProfile(profile: UserProfile) {
    _userProfile.value = profile
    recordAudit("USER_PROFILE_UPDATED", "Account details updated for: ${profile.fullName}")
  }

  fun setBiometricLock(enabled: Boolean) {
    _userProfile.update { it.copy(biometricLockEnabled = enabled) }
    recordAudit("SECURITY_PREFERENCE_CHANGED", "Biometric lock set to: $enabled")
  }

  fun setEmergencyMedicalIdUnlocked(enabled: Boolean) {
    _userProfile.update { it.copy(emergencyMedicalIdUnlocked = enabled) }
    recordAudit("SECURITY_PREFERENCE_CHANGED", "Emergency lock-screen access set to: $enabled")
  }

  fun updateUnitPreferences(preferences: UnitPreferences) {
    _userProfile.update { it.copy(unitPreferences = preferences) }
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
    recordAudit("UNIT_PREFERENCE_CHANGED", "Blood glucose unit preference set to ${unit.label}")
  }

  fun setTemperatureUnit(unit: TemperatureUnit) {
    _userProfile.update { it.copy(unitPreferences = it.unitPreferences.copy(temperatureUnit = unit)) }
    recordAudit("UNIT_PREFERENCE_CHANGED", "Body temperature unit preference set to ${unit.label}")
  }

  fun setWeightUnit(unit: WeightUnit) {
    _userProfile.update { it.copy(unitPreferences = it.unitPreferences.copy(weightUnit = unit)) }
    recordAudit("UNIT_PREFERENCE_CHANGED", "Body weight unit preference set to ${unit.label}")
  }

  fun setHeightUnit(unit: HeightUnit) {
    _userProfile.update { it.copy(unitPreferences = it.unitPreferences.copy(heightUnit = unit)) }
    recordAudit("UNIT_PREFERENCE_CHANGED", "Height unit preference set to ${unit.label}")
  }

  fun setCholesterolUnit(unit: CholesterolUnit) {
    _userProfile.update { it.copy(unitPreferences = it.unitPreferences.copy(cholesterolUnit = unit)) }
    recordAudit("UNIT_PREFERENCE_CHANGED", "Cholesterol unit preference set to ${unit.label}")
  }

  fun addPatientProfile(profile: PatientProfile) {
    _profiles.update { it + profile }
    recordAudit("PATIENT_PROFILE_CREATED", "Added patient profile '${profile.name}' (${profile.relationship})")
  }

  fun updatePatientProfile(updated: PatientProfile) {
    _profiles.update { list ->
      list.map { if (it.id == updated.id) updated else it }
    }
    recordAudit("PATIENT_PROFILE_UPDATED", "Updated clinical profile for '${updated.name}'")
  }

  fun deletePatientProfile(profileId: String) {
    val profileToDelete = _profiles.value.firstOrNull { it.id == profileId }
    if (profileToDelete != null && !profileToDelete.isPrimary) {
      _profiles.update { list -> list.filterNot { it.id == profileId } }
      if (_currentProfileId.value == profileId) {
        _currentProfileId.value = _profiles.value.firstOrNull()?.id ?: "profile_doug"
      }
      recordAudit("PATIENT_PROFILE_DELETED", "Deleted patient profile: ${profileToDelete.name}")
    }
  }

  fun setSubscriptionTier(tier: SubscriptionTier) {
    _subscriptionTier.value = tier
    _subscriptionDetails.update { it.copy(tier = tier) }
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
    _subscriptionDetails.value = SubscriptionDetails(
      tier = tier,
      billingCycle = billingCycle,
      status = "ACTIVE",
      renewalDate = renewalStr,
      lastPaymentMethod = paymentMethod,
      lastTransactionId = txnId,
      autoRenew = true
    )

    recordAudit("PAYMENT_PROCESSED", "Processed payment of $$amount via $paymentMethod for plan ${tier.displayName} (Txn: $txnId)")
    recordAudit("SUBSCRIPTION_ACTIVATED", "Activated ${tier.displayName} (${billingCycle.label})")

    return transaction
  }

  fun cancelSubscriptionAutoRenew() {
    _subscriptionDetails.update { it.copy(autoRenew = false, status = "CANCELED_AT_PERIOD_END") }
    recordAudit("SUBSCRIPTION_AUTO_RENEW_CANCELED", "Auto-renewal turned off for ${_subscriptionTier.value.displayName}")
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
    recordAudit("REMINDER_CREATED", "Set daily recurring alert for '${reminder.medicationName}' at ${reminder.formattedTime}")
  }

  fun toggleMedicationReminder(reminderId: String, isEnabled: Boolean) {
    _reminders.update { list ->
      list.map { if (it.id == reminderId) it.copy(isEnabled = isEnabled) else it }
    }
    recordAudit("REMINDER_TOGGLED", "Reminder $reminderId alert status: ${if (isEnabled) "Active" else "Paused"}")
  }

  fun deleteMedicationReminder(reminderId: String) {
    _reminders.update { list -> list.filterNot { it.id == reminderId } }
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
    recordAudit("SECURE_SHARE_CREATED", "Generated $scope share grant for $recipientName ($role), expires in $durationLabel")
    return grant
  }

  fun exportFhirBundleJson(): String {
    val curProfile = _profiles.value.firstOrNull { it.id == _currentProfileId.value } ?: _profiles.value.first()
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
    _medications.value.forEach { med ->
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
    _timelineItems.value.forEach { item ->
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
    recordAudit("FHIR_BUNDLE_EXPORT", "Compiled HL7 FHIR R4 JSON Bundle (${_timelineItems.value.size + _medications.value.size + 1} resources)")
    return builder.toString()
  }

  fun exportMedicationsCsv(): String {
    val sb = StringBuilder()
    sb.append("id,name,dosage,frequency,prescriber,indication,status,isReconciled,source\n")
    _medications.value.forEach { m ->
      sb.append("\"${m.id}\",\"${m.name}\",\"${m.dosage}\",\"${m.frequency}\",\"${m.prescriber}\",\"${m.indication}\",\"${m.status.name}\",\"${m.isReconciled}\",\"${m.provenance.sourceSystem}\"\n")
    }
    recordAudit("CSV_MEDS_EXPORT", "Exported ${_medications.value.size} medications to CSV format")
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

  fun importMedicationsCsv(csvString: String): Pair<Int, String> {
    return try {
      val lines = csvString.lines().filter { it.isNotBlank() }
      if (lines.isEmpty()) return Pair(0, "CSV file is empty")
      var count = 0
      lines.drop(1).forEach { line ->
        val parts = line.split(",").map { it.trim().replace("\"", "") }
        if (parts.size >= 2) {
          val name = parts.getOrNull(1) ?: parts[0]
          val dosage = parts.getOrNull(2) ?: "Standard"
          val freq = parts.getOrNull(3) ?: "Daily"
          val prescriber = parts.getOrNull(4) ?: "External Prescriber"
          val indication = parts.getOrNull(5) ?: "Therapeutic"

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
    recordAudit("SECURE_SHARE_REVOKED", "Care-team access token $grantId revoked immediately by patient")
  }

  fun sendUserMessage(query: String) {
    val userMsg = ChatMessage(
      id = UUID.randomUUID().toString(),
      sender = "user",
      text = query,
      timestamp = currentTime()
    )
    _chatMessages.update { it + userMsg }

    // RAG and clinical safety grounding
    val answer = generateGroundedResponse(query)
    _chatMessages.update { it + answer }
    recordAudit("AI_GROUNDED_QUERY", "Patient queried AI interpreter: '${query.take(40)}...'")
  }

  private fun generateGroundedResponse(query: String): ChatMessage {
    val q = query.lowercase()

    // 1. Red-Flag Emergency Triage
    val redFlagTriggers = listOf("chest pain", "shortness of breath", "arm pain", "jaw pain", "heart attack", "stroke", "numbness", "suicide", "bleeding profusely")
    if (redFlagTriggers.any { q.contains(it) }) {
      return ChatMessage(
        id = UUID.randomUUID().toString(),
        sender = "ai",
        text = "CRITICAL ALERT: Your query mentions symptoms that may indicate an acute medical emergency. Vitalith cannot evaluate emergency conditions.",
        timestamp = currentTime(),
        isRedFlag = true,
        redFlagEscalationText = "CALL 911 OR PROCEED TO THE NEAREST EMERGENCY ROOM IMMEDIATELY. Do not wait for an online response or take unprescribed medications."
      )
    }

    // 2. A1c / Diabetes / Blood Sugar
    if (q.contains("a1c") || q.contains("glucose") || q.contains("sugar") || q.contains("diabetes")) {
      return ChatMessage(
        id = UUID.randomUUID().toString(),
        sender = "ai",
        text = "According to your latest Quest Diagnostics lab result from Oct 14, 2026, your Hemoglobin A1c is 6.4%. This is down from 6.8% recorded in July 2026, representing a positive downward trend of 0.4%.\n\nClinical context: 6.4% falls within the pre-diabetes classification (5.7% – 6.4%). Your endocrinologist Dr. Sarah Chen noted during your Oct 14 visit that your response to Metformin 500mg BID and dietary modifications is progressing well.",
        timestamp = currentTime(),
        citations = listOf(
          Citation(
            id = "c1",
            title = "Quest Diagnostics • Hemoglobin A1c",
            date = "Oct 14, 2026",
            source = "Quest Diagnostics HL7/FHIR v4.0.1",
            excerpt = "HbA1c: 6.4% (Reference: 4.0 - 5.6%). Prior: 6.8% (Jul 12, 2026)."
          ),
          Citation(
            id = "c2",
            title = "Clinical Note • Endocrinology Follow-up",
            date = "Oct 14, 2026",
            source = "Bayfront Health FHIR Core",
            excerpt = "Dr. Sarah Chen, MD: Patient adhering to Metformin 500mg BID. A1c improved 6.8 -> 6.4. Target < 6.0%."
          )
        )
      )
    }

    // 3. Medications / Metformin / Statin
    if (q.contains("med") || q.contains("metformin") || q.contains("atorvastatin") || q.contains("statin") || q.contains("dose")) {
      return ChatMessage(
        id = UUID.randomUUID().toString(),
        sender = "ai",
        text = "Your record contains 3 active medications:\n1. Metformin HCl 500mg (1 tablet twice daily with meals)\n2. Atorvastatin Calcium 10mg (1 tablet daily at bedtime)\n3. Vitamin D3 2000 IU (1 capsule daily)\n\nInteraction Note: Your record flags a caution to monitor liver function panels every 6 months when taking statins concurrently with metformin. No active dosage changes are recommended without consulting Dr. Chen.",
        timestamp = currentTime(),
        citations = listOf(
          Citation(
            id = "c3",
            title = "MedicationStatement • Metformin 500mg",
            date = "Active since Jun 2025",
            source = "Epic SMART-on-FHIR",
            excerpt = "Sig: Take 1 tablet twice daily with breakfast and dinner. Refills: 3."
          ),
          Citation(
            id = "c4",
            title = "MedicationStatement • Atorvastatin 10mg",
            date = "Active since Sep 2025",
            source = "MetroHealth Cardiology",
            excerpt = "Sig: Take 1 tablet by mouth daily at bedtime. Prescriber: Dr. Marcus Vance."
          )
        )
      )
    }

    // 4. Appointment Prep
    if (q.contains("appointment") || q.contains("prep") || q.contains("doctor") || q.contains("visit")) {
      return ChatMessage(
        id = UUID.randomUUID().toString(),
        sender = "ai",
        text = "I've synthesized your clinical profile for your upcoming Endocrinology visit with Dr. Sarah Chen on Nov 4, 2026.\n\nKey highlights ready for your visit:\n• HbA1c decreased from 6.8% to 6.4%.\n• Resting blood pressure average: 122/78 mmHg.\n• Active Metformin tolerance: Good, no gastrointestinal side effects logged.\n• Top question to ask: 'Given the A1c reduction to 6.4%, should we sustain the current 500mg BID dosage or consider stepping down as lifestyle changes continue?'",
        timestamp = currentTime(),
        citations = listOf(
          Citation(
            id = "c5",
            title = "Automated Appointment Prep Briefing",
            date = "Oct 2026",
            source = "Vitalith RAG Synthesizer",
            excerpt = "Patient: Doug Adarkwah. Longitudinal view across 4 clinical encounters and 6 lab panels."
          )
        )
      )
    }

    // 5. Natural Language Health Record Summary & Anomaly Flagging
    if (q.contains("summar") || q.contains("anomal") || q.contains("flag") || q.contains("insight") || q.contains("overview")) {
      val summary = GeminiHealthSummarizer.generateDeterministicSummary(_timelineItems.value, "Doug Adarkwah", _userProfile.value.unitPreferences)
      val findingsText = summary.flaggedFindings.joinToString("\n") { finding ->
        "• ${finding.biomarkerOrItem} (${finding.observedValue}): [${finding.severity.label.uppercase()}] ${finding.clinicalRationale}"
      }
      val insightsText = summary.keyInsights.joinToString("\n") { "• $it" }
      return ChatMessage(
        id = UUID.randomUUID().toString(),
        sender = "ai",
        text = "GEMINI AI CLINICAL SUMMARY:\n${summary.executiveSummary}\n\nFLAGGED ANOMALIES & RADAR:\n$findingsText\n\nLONGITUDINAL INSIGHTS:\n$insightsText",
        timestamp = currentTime(),
        citations = listOf(
          Citation(
            id = "c_sum",
            title = "Gemini 3.5 Flash • Longitudinal Record Synthesis",
            date = summary.generatedAt,
            source = "Vitalith AI Grounded Clinical Evaluator",
            excerpt = "${_timelineItems.value.size} longitudinal records synthesized across metabolic, cardiac, and renal panels."
          )
        )
      )
    }

    // 6. Grounded Refusal when out of record scope
    return ChatMessage(
      id = UUID.randomUUID().toString(),
      sender = "ai",
      text = "I searched your longitudinal health record but could not find verified clinical data matching your specific question. To protect clinical safety, Vitalith refuses to speculate or provide ungrounded advice.\n\nYou can upload relevant PDF lab reports or connect additional health systems (via Epic, Cerner, or MyChart) in the Ingestion tab, or discuss this directly with your doctor.",
      timestamp = currentTime(),
      citations = emptyList()
    )
  }

  fun recordAudit(action: String, resource: String) {
    val entry = AuditEntry(
      id = "aud_${UUID.randomUUID().toString().take(8)}",
      timestamp = currentTime(),
      actor = "Doug Adarkwah (Owner)",
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
    val glucoseDisplay = com.example.util.UcumUnitConverter.formatGlucose("102 mg/dL", unitPreferences.glucoseUnit).displayString
    val bpDisplay = "122/78 mmHg"
    return AppointmentPrep(
      id = "prep_nov2026",
      profileId = "profile_doug",
      doctorName = "Dr. Sarah Chen, MD",
      clinic = "Bayfront Endocrinology & Metabolism",
      scheduledDate = "Nov 04, 2026 • 10:15 AM",
      visitReason = "3-Month Pre-Diabetes Management & A1c Review",
      recentChanges = listOf(
        "A1c dropped from 6.8% to 6.4% (Quest Diagnostics, Oct 14)",
        "No missed Metformin doses logged over past 90 days",
        "Home systolic blood pressure averaged $bpDisplay (normal)",
        "Added Vitamin D3 supplement (2000 IU daily)"
      ),
      keyQuestionsToAsk = listOf(
        "Is my A1c trajectory sufficient to maintain the 500mg BID dose, or should we plan a taper?",
        "Do you recommend repeating the comprehensive metabolic panel before our 6-month check?",
        "Should we screen for early signs of neuropathy or microalbuminuria at this stage?",
        "Are there any specific dietary carbohydrate thresholds you recommend for long-term glycemic stability?"
      ),
      criticalMetrics = mapOf(
        "HbA1c" to "6.4% (Prior: 6.8%)",
        "Fasting Glucose" to glucoseDisplay,
        "eGFR" to "98 mL/min (Normal)",
        "BP Trend" to bpDisplay
      ),
      activeMedsCount = 3
    )
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
      ChatMessage(
        id = "msg_init_1",
        sender = "system",
        text = "Welcome to Vitalith Intelligent Interpreter. I answer questions grounded strictly in your personal health records, with citations to source documents. I explain and coordinate; I do not diagnose.",
        timestamp = "System Active",
        citations = emptyList()
      ),
      ChatMessage(
        id = "msg_init_2",
        sender = "user",
        text = "How is my A1c trending over the last few months?",
        timestamp = "Oct 19, 2026 • 09:15"
      ),
      ChatMessage(
        id = "msg_init_3",
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
}

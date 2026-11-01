package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.*

@Entity(
  tableName = "timeline_records",
  indices = [
    Index(value = ["profileId"]),
    Index(value = ["category"]),
    Index(value = ["clinicalDateMillis"])
  ]
)
data class TimelineItemEntity(
  @PrimaryKey val id: String,
  val profileId: String,
  val category: String, // LAB, MEDICATION, CONDITION, ENCOUNTER, ALLERGY, IMMUNIZATION, PROCEDURE, DOCUMENT
  val title: String,
  val subtitle: String,
  val date: String,
  val clinicalDateMillis: Long = System.currentTimeMillis(),
  val providerOrFacility: String,
  val status: String, // NORMAL, ELEVATED, CRITICAL, ACTIVE, DISCONTINUED, RESOLVED
  val clinicalNotes: String,
  // Provenance fields
  val sourceSystem: String,
  val ingestedAt: String,
  val confidenceScore: Float,
  val consentState: String,
  val documentHash: String,
  // Comma-separated tags
  val tags: String = "",
  // Serialized key-value pairs (e.g. "Value:6.4%|Reference:4.0 - 5.6%")
  val keyValuesRaw: String = ""
) {
  fun toDomain(): HealthTimelineItem {
    val parsedTags = if (tags.isBlank()) {
      emptyList()
    } else {
      try {
        val jsonArray = org.json.JSONArray(tags)
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
          val tag = jsonArray.optString(i, "").trim()
          if (tag.isNotEmpty()) list.add(tag)
        }
        list
      } catch (e: Throwable) {
        if (tags.startsWith("[") && tags.endsWith("]")) {
          tags.substring(1, tags.length - 1)
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() }
        } else {
          // Fallback for legacy comma-separated string
          tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
      }
    }

    val parsedKeyValues = if (keyValuesRaw.isBlank()) {
      emptyMap()
    } else {
      try {
        val jsonObj = org.json.JSONObject(keyValuesRaw)
        val map = mutableMapOf<String, String>()
        val keys = jsonObj.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          map[key] = jsonObj.optString(key, "")
        }
        map
      } catch (e: Throwable) {
        if (keyValuesRaw.startsWith("{") && keyValuesRaw.endsWith("}")) {
          keyValuesRaw.substring(1, keyValuesRaw.length - 1)
            .split(",")
            .mapNotNull { part ->
              val colonIdx = part.indexOf(':')
              if (colonIdx != -1) {
                val k = part.substring(0, colonIdx).trim().removeSurrounding("\"")
                val v = part.substring(colonIdx + 1).trim().removeSurrounding("\"")
                k to v
              } else null
            }.toMap()
        } else {
          // Fallback for legacy delimiter-formatted strings ("k1:v1|k2:v2")
          keyValuesRaw.split("|")
            .mapNotNull {
              val idx = it.indexOf(':')
              if (idx != -1) {
                it.substring(0, idx).trim() to it.substring(idx + 1).trim()
              } else null
            }.toMap()
        }
      }
    }

    return HealthTimelineItem(
      id = id,
      profileId = profileId,
      category = runCatching { TimelineCategory.valueOf(category) }.getOrDefault(TimelineCategory.LAB),
      title = title,
      subtitle = subtitle,
      date = date,
      providerOrFacility = providerOrFacility,
      status = runCatching { ItemStatus.valueOf(status) }.getOrDefault(ItemStatus.NORMAL),
      clinicalNotes = clinicalNotes,
      provenance = Provenance(
        sourceSystem = sourceSystem,
        ingestedAt = ingestedAt,
        confidenceScore = confidenceScore,
        consentState = consentState,
        documentHash = documentHash
      ),
      tags = parsedTags,
      keyValues = parsedKeyValues
    )
  }

  companion object {
    fun fromDomain(item: HealthTimelineItem): TimelineItemEntity {
      val tagsJson = try {
        org.json.JSONArray().apply {
          item.tags.forEach { put(it) }
        }.toString()
      } catch (e: Throwable) {
        "[" + item.tags.joinToString(",") { "\"$it\"" } + "]"
      }

      val keyValuesJson = try {
        org.json.JSONObject().apply {
          item.keyValues.forEach { (k, v) -> put(k, v) }
        }.toString()
      } catch (e: Throwable) {
        "{" + item.keyValues.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" } + "}"
      }

      val parsedDateMillis = runCatching {
        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).parse(item.date)?.time
      }.getOrNull() ?: runCatching {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(item.date)?.time
      }.getOrNull() ?: runCatching {
        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US).parse(item.date)?.time
      }.getOrNull() ?: System.currentTimeMillis()

      return TimelineItemEntity(
        id = item.id,
        profileId = item.profileId,
        category = item.category.name,
        title = item.title,
        subtitle = item.subtitle,
        date = item.date,
        clinicalDateMillis = parsedDateMillis,
        providerOrFacility = item.providerOrFacility,
        status = item.status.name,
        clinicalNotes = item.clinicalNotes,
        sourceSystem = item.provenance.sourceSystem,
        ingestedAt = item.provenance.ingestedAt,
        confidenceScore = item.provenance.confidenceScore,
        consentState = item.provenance.consentState,
        documentHash = item.provenance.documentHash,
        tags = tagsJson,
        keyValuesRaw = keyValuesJson
      )
    }
  }
}

@Entity(
  tableName = "medication_records",
  indices = [Index(value = ["profileId"])]
)
data class MedicationEntity(
  @PrimaryKey val id: String,
  val profileId: String,
  val name: String,
  val dosage: String,
  val frequency: String,
  val prescriber: String,
  val startDate: String,
  val status: String,
  val indication: String,
  val conflictWarning: String? = null,
  val isReconciled: Boolean = true,
  // Provenance
  val sourceSystem: String,
  val ingestedAt: String,
  val confidenceScore: Float,
  val consentState: String
) {
  fun toDomain(): MedicationItem {
    return MedicationItem(
      id = id,
      profileId = profileId,
      name = name,
      dosage = dosage,
      frequency = frequency,
      prescriber = prescriber,
      startDate = startDate,
      status = runCatching { ItemStatus.valueOf(status) }.getOrDefault(ItemStatus.ACTIVE),
      indication = indication,
      conflictWarning = conflictWarning,
      isReconciled = isReconciled,
      provenance = Provenance(sourceSystem, ingestedAt, confidenceScore, consentState)
    )
  }

  companion object {
    fun fromDomain(item: MedicationItem): MedicationEntity {
      return MedicationEntity(
        id = item.id,
        profileId = item.profileId,
        name = item.name,
        dosage = item.dosage,
        frequency = item.frequency,
        prescriber = item.prescriber,
        startDate = item.startDate,
        status = item.status.name,
        indication = item.indication,
        conflictWarning = item.conflictWarning,
        isReconciled = item.isReconciled,
        sourceSystem = item.provenance.sourceSystem,
        ingestedAt = item.provenance.ingestedAt,
        confidenceScore = item.provenance.confidenceScore,
        consentState = item.provenance.consentState
      )
    }
  }
}

@Entity(
  tableName = "audit_records",
  indices = [Index(value = ["timestampMillis"])]
)
data class AuditLogEntity(
  @PrimaryKey val id: String,
  val timestamp: String,
  val timestampMillis: Long = System.currentTimeMillis(),
  val actor: String,
  val action: String,
  val resource: String,
  val status: String = "SUCCESS",
  val ipAddress: String = "127.0.0.1"
) {
  fun toDomain(): AuditEntry {
    return AuditEntry(
      id = id,
      timestamp = timestamp,
      actor = actor,
      action = action,
      resource = resource,
      status = status,
      ipAddress = ipAddress
    )
  }

  companion object {
    fun fromDomain(item: AuditEntry): AuditLogEntity {
      return AuditLogEntity(
        id = item.id,
        timestamp = item.timestamp,
        actor = item.actor,
        action = item.action,
        resource = item.resource,
        status = item.status,
        ipAddress = item.ipAddress
      )
    }
  }
}

@Entity(tableName = "patient_profiles")
data class PatientProfileEntity(
  @PrimaryKey val id: String,
  val name: String,
  val relationship: String,
  val dob: String,
  val bloodType: String,
  val mrn: String,
  val isPrimary: Boolean = false,
  val activeConditionsCount: Int = 0,
  val activeMedicationsCount: Int = 0,
  val pendingTasksCount: Int = 0,
  val primaryPhysician: String,
  val allergiesJson: String = "[]",
  val emergencyContact: String,
  val notes: String = "",
  val gender: String = "Unspecified"
) {
  fun toDomain(): PatientProfile {
    val parsedAllergies = try {
      val arr = org.json.JSONArray(allergiesJson)
      val list = mutableListOf<String>()
      for (i in 0 until arr.length()) {
        val al = arr.optString(i, "").trim()
        if (al.isNotEmpty()) list.add(al)
      }
      list
    } catch (e: Exception) {
      allergiesJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    return PatientProfile(
      id = id,
      name = name,
      relationship = relationship,
      dob = dob,
      bloodType = bloodType,
      mrn = mrn,
      isPrimary = isPrimary,
      activeConditionsCount = activeConditionsCount,
      activeMedicationsCount = activeMedicationsCount,
      pendingTasksCount = pendingTasksCount,
      primaryPhysician = primaryPhysician,
      allergies = parsedAllergies,
      emergencyContact = emergencyContact,
      notes = notes,
      gender = gender
    )
  }

  companion object {
    fun fromDomain(p: PatientProfile): PatientProfileEntity {
      val arr = org.json.JSONArray().apply {
        p.allergies.forEach { put(it) }
      }.toString()
      return PatientProfileEntity(
        id = p.id,
        name = p.name,
        relationship = p.relationship,
        dob = p.dob,
        bloodType = p.bloodType,
        mrn = p.mrn,
        isPrimary = p.isPrimary,
        activeConditionsCount = p.activeConditionsCount,
        activeMedicationsCount = p.activeMedicationsCount,
        pendingTasksCount = p.pendingTasksCount,
        primaryPhysician = p.primaryPhysician,
        allergiesJson = arr,
        emergencyContact = p.emergencyContact,
        notes = p.notes,
        gender = p.gender
      )
    }
  }
}

@Entity(
  tableName = "medication_reminders",
  indices = [Index(value = ["profileId"])]
)
data class MedicationReminderEntity(
  @PrimaryKey val id: String,
  val profileId: String = "profile_doug",
  val medicationId: String,
  val medicationName: String,
  val dosage: String,
  val hour: Int,
  val minute: Int,
  val daysOfWeekJson: String = "[1,2,3,4,5,6,7]",
  val isEnabled: Boolean = true,
  val instructions: String = "",
  val isTakenToday: Boolean = false
) {
  fun toDomain(): MedicationReminder {
    val days = try {
      val arr = org.json.JSONArray(daysOfWeekJson)
      val list = mutableListOf<Int>()
      for (i in 0 until arr.length()) list.add(arr.getInt(i))
      list
    } catch (e: Exception) {
      daysOfWeekJson.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
    return MedicationReminder(
      id = id,
      profileId = profileId,
      medicationId = medicationId,
      medicationName = medicationName,
      dosage = dosage,
      hour = hour,
      minute = minute,
      daysOfWeek = if (days.isEmpty()) listOf(1, 2, 3, 4, 5, 6, 7) else days,
      isEnabled = isEnabled,
      instructions = instructions,
      isTakenToday = isTakenToday
    )
  }

  companion object {
    fun fromDomain(r: MedicationReminder): MedicationReminderEntity {
      val arr = org.json.JSONArray().apply {
        r.daysOfWeek.forEach { put(it) }
      }.toString()
      return MedicationReminderEntity(
        id = r.id,
        profileId = r.profileId,
        medicationId = r.medicationId,
        medicationName = r.medicationName,
        dosage = r.dosage,
        hour = r.hour,
        minute = r.minute,
        daysOfWeekJson = arr,
        isEnabled = r.isEnabled,
        instructions = r.instructions,
        isTakenToday = r.isTakenToday
      )
    }
  }
}

@Entity(
  tableName = "care_grants",
  indices = [Index(value = ["profileId"])]
)
data class CareGrantEntity(
  @PrimaryKey val id: String,
  val profileId: String,
  val recipientName: String,
  val recipientRole: String,
  val scope: String,
  val durationLabel: String,
  val expiresAt: String,
  val tokenKey: String,
  val accessCount: Int = 0,
  val isActive: Boolean = true
) {
  fun toDomain(): CareGrant {
    return CareGrant(
      id = id,
      profileId = profileId,
      recipientName = recipientName,
      recipientRole = recipientRole,
      scope = scope,
      durationLabel = durationLabel,
      expiresAt = expiresAt,
      tokenKey = tokenKey,
      accessCount = accessCount,
      isActive = isActive
    )
  }

  companion object {
    fun fromDomain(g: CareGrant): CareGrantEntity {
      return CareGrantEntity(
        id = g.id,
        profileId = g.profileId,
        recipientName = g.recipientName,
        recipientRole = g.recipientRole,
        scope = g.scope,
        durationLabel = g.durationLabel,
        expiresAt = g.expiresAt,
        tokenKey = g.tokenKey,
        accessCount = g.accessCount,
        isActive = g.isActive
      )
    }
  }
}

@Entity(tableName = "payment_transactions")
data class PaymentTransactionEntity(
  @PrimaryKey val id: String,
  val date: String,
  val tier: String,
  val billingCycle: String,
  val amountPaid: Double,
  val paymentMethod: String,
  val status: String,
  val invoiceNumber: String
) {
  fun toDomain(): PaymentTransaction {
    return PaymentTransaction(
      id = id,
      date = date,
      tier = runCatching { SubscriptionTier.valueOf(tier) }.getOrDefault(SubscriptionTier.PREMIUM),
      billingCycle = runCatching { BillingCycle.valueOf(billingCycle) }.getOrDefault(BillingCycle.ANNUAL),
      amountPaid = amountPaid,
      paymentMethod = paymentMethod,
      status = status,
      invoiceNumber = invoiceNumber
    )
  }

  companion object {
    fun fromDomain(tx: PaymentTransaction): PaymentTransactionEntity {
      return PaymentTransactionEntity(
        id = tx.id,
        date = tx.date,
        tier = tx.tier.name,
        billingCycle = tx.billingCycle.name,
        amountPaid = tx.amountPaid,
        paymentMethod = tx.paymentMethod,
        status = tx.status,
        invoiceNumber = tx.invoiceNumber
      )
    }
  }
}

@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
  @PrimaryKey val prefKey: String,
  val prefValue: String
)

@Entity(
  tableName = "chat_messages",
  indices = [Index(value = ["profileId"])]
)
data class ChatMessageEntity(
  @PrimaryKey val id: String,
  val profileId: String = "profile_doug",
  val sender: String,
  val text: String,
  val timestamp: String,
  val timestampMillis: Long = System.currentTimeMillis(),
  val citationsJson: String = "[]",
  val isRedFlag: Boolean = false,
  val redFlagEscalationText: String? = null
) {
  fun toDomain(): ChatMessage {
    val parsedCitations = try {
      val arr = org.json.JSONArray(citationsJson)
      val list = mutableListOf<Citation>()
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(
          Citation(
            id = obj.optString("id"),
            title = obj.optString("title"),
            date = obj.optString("date"),
            source = obj.optString("source"),
            excerpt = obj.optString("excerpt")
          )
        )
      }
      list
    } catch (e: Throwable) {
      emptyList()
    }
    return ChatMessage(
      id = id,
      profileId = profileId,
      sender = sender,
      text = text,
      timestamp = timestamp,
      citations = parsedCitations,
      isRedFlag = isRedFlag,
      redFlagEscalationText = redFlagEscalationText
    )
  }

  companion object {
    fun fromDomain(msg: ChatMessage): ChatMessageEntity {
      val arrJson = try {
        val arr = org.json.JSONArray()
        msg.citations.forEach { cit ->
          val obj = org.json.JSONObject()
          obj.put("id", cit.id)
          obj.put("title", cit.title)
          obj.put("date", cit.date)
          obj.put("source", cit.source)
          obj.put("excerpt", cit.excerpt)
          arr.put(obj)
        }
        arr.toString()
      } catch (e: Throwable) {
        "[]"
      }
      return ChatMessageEntity(
        id = msg.id,
        profileId = msg.profileId,
        sender = msg.sender,
        text = msg.text,
        timestamp = msg.timestamp,
        citationsJson = arrJson,
        isRedFlag = msg.isRedFlag,
        redFlagEscalationText = msg.redFlagEscalationText
      )
    }
  }
}

@Entity(
  tableName = "symptom_records",
  indices = [
    Index(value = ["profileId"]),
    Index(value = ["onsetTimestamp"])
  ]
)
data class SymptomEntity(
  @PrimaryKey val id: String,
  val profileId: String,
  val symptomName: String,
  val snomedCode: String = "",
  val severity: Int,
  val severityLabel: String,
  val onsetDate: String,
  val onsetTimestamp: Long,
  val endedDate: String? = null,
  val endedTimestamp: Long? = null,
  val isOngoing: Boolean = true,
  val isIntermittent: Boolean = false,
  val bodySite: String? = null,
  val impactJson: String = "[]",
  val contextTagsJson: String = "[]",
  val notes: String = "",
  val mediaUrisJson: String = "[]",
  val episodeId: String? = null,
  val isRedFlag: Boolean = false,
  val redFlagMessage: String? = null,
  val sourceSystem: String = "Patient Self-Report (Quick-Add)",
  val ingestedAt: String = "Just now",
  val confidenceScore: Float = 1.0f,
  val consentState: String = "Patient-Owned / Private Vault"
) {
  fun toDomain(): SymptomItem {
    return SymptomItem(
      id = id,
      profileId = profileId,
      symptomName = symptomName,
      snomedCode = snomedCode,
      severity = severity,
      severityLabel = severityLabel,
      onsetDate = onsetDate,
      onsetTimestamp = onsetTimestamp,
      endedDate = endedDate,
      endedTimestamp = endedTimestamp,
      isOngoing = isOngoing,
      isIntermittent = isIntermittent,
      bodySite = bodySite,
      impact = parseJsonArray(impactJson),
      contextTags = parseJsonArray(contextTagsJson),
      notes = notes,
      mediaUris = parseJsonArray(mediaUrisJson),
      episodeId = episodeId,
      isRedFlag = isRedFlag,
      redFlagMessage = redFlagMessage,
      provenance = Provenance(
        sourceSystem = sourceSystem,
        ingestedAt = ingestedAt,
        confidenceScore = confidenceScore,
        consentState = consentState
      )
    )
  }

  companion object {
    private fun parseJsonArray(json: String): List<String> {
      if (json.isBlank()) return emptyList()
      return try {
        val arr = org.json.JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
          val str = arr.optString(i, "").trim()
          if (str.isNotEmpty()) list.add(str)
        }
        list
      } catch (e: Throwable) {
        if (json.startsWith("[") && json.endsWith("]")) {
          json.substring(1, json.length - 1).split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotEmpty() }
        } else {
          json.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
      }
    }

    private fun toJsonArray(list: List<String>): String {
      return try {
        val arr = org.json.JSONArray()
        list.forEach { arr.put(it) }
        arr.toString()
      } catch (e: Throwable) {
        "[]"
      }
    }

    fun fromDomain(item: SymptomItem): SymptomEntity {
      return SymptomEntity(
        id = item.id,
        profileId = item.profileId,
        symptomName = item.symptomName,
        snomedCode = item.snomedCode,
        severity = item.severity,
        severityLabel = item.severityLabel,
        onsetDate = item.onsetDate,
        onsetTimestamp = item.onsetTimestamp,
        endedDate = item.endedDate,
        endedTimestamp = item.endedTimestamp,
        isOngoing = item.isOngoing,
        isIntermittent = item.isIntermittent,
        bodySite = item.bodySite,
        impactJson = toJsonArray(item.impact),
        contextTagsJson = toJsonArray(item.contextTags),
        notes = item.notes,
        mediaUrisJson = toJsonArray(item.mediaUris),
        episodeId = item.episodeId,
        isRedFlag = item.isRedFlag,
        redFlagMessage = item.redFlagMessage,
        sourceSystem = item.provenance.sourceSystem,
        ingestedAt = item.provenance.ingestedAt,
        confidenceScore = item.provenance.confidenceScore,
        consentState = item.provenance.consentState
      )
    }
  }
}


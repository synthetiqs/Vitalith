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
    val parsedTags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }
    val parsedKeyValues = if (keyValuesRaw.isBlank()) {
      emptyMap()
    } else {
      keyValuesRaw.split("|")
        .mapNotNull {
          val parts = it.split(":")
          if (parts.size >= 2) parts[0].trim() to parts.drop(1).joinToString(":").trim() else null
        }.toMap()
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
      val rawKeyValues = item.keyValues.entries.joinToString("|") { "${it.key}:${it.value}" }
      return TimelineItemEntity(
        id = item.id,
        profileId = item.profileId,
        category = item.category.name,
        title = item.title,
        subtitle = item.subtitle,
        date = item.date,
        providerOrFacility = item.providerOrFacility,
        status = item.status.name,
        clinicalNotes = item.clinicalNotes,
        sourceSystem = item.provenance.sourceSystem,
        ingestedAt = item.provenance.ingestedAt,
        confidenceScore = item.provenance.confidenceScore,
        consentState = item.provenance.consentState,
        documentHash = item.provenance.documentHash,
        tags = item.tags.joinToString(","),
        keyValuesRaw = rawKeyValues
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

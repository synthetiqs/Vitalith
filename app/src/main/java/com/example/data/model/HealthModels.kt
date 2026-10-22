package com.example.data.model

enum class TimelineCategory {
  ALL,
  LAB,
  MEDICATION,
  CONDITION,
  ENCOUNTER,
  ALLERGY,
  IMMUNIZATION,
  PROCEDURE,
  DOCUMENT
}

enum class ItemStatus {
  NORMAL,
  ELEVATED,
  CRITICAL,
  ACTIVE,
  DISCONTINUED,
  RESOLVED
}

data class Provenance(
  val sourceSystem: String, // e.g. "Epic SMART-on-FHIR", "LabCorp OCR v2.1", "Patient Manual Entry"
  val ingestedAt: String,
  val confidenceScore: Float, // 0.0 to 1.0 (e.g. 0.98f)
  val consentState: String, // "Patient-Owned / Full Consent", "Direct Care Ingestion"
  val documentHash: String = "sha256:7f83b1657ff1...48b3"
)

data class HealthTimelineItem(
  val id: String,
  val profileId: String,
  val category: TimelineCategory,
  val title: String,
  val subtitle: String,
  val date: String,
  val providerOrFacility: String,
  val status: ItemStatus,
  val clinicalNotes: String,
  val provenance: Provenance,
  val tags: List<String> = emptyList(),
  val keyValues: Map<String, String> = emptyMap()
)

data class PatientProfile(
  val id: String,
  val name: String,
  val relationship: String, // "Primary Patient", "Child", "Elderly Parent"
  val dob: String,
  val bloodType: String,
  val mrn: String,
  val isPrimary: Boolean = false,
  val activeConditionsCount: Int = 0,
  val activeMedicationsCount: Int = 0,
  val pendingTasksCount: Int = 0
)

data class MedicationItem(
  val id: String,
  val profileId: String,
  val name: String,
  val dosage: String,
  val frequency: String,
  val prescriber: String,
  val startDate: String,
  val status: ItemStatus,
  val indication: String,
  val conflictWarning: String? = null,
  val isReconciled: Boolean = true,
  val provenance: Provenance
)

data class CareGrant(
  val id: String,
  val profileId: String,
  val recipientName: String,
  val recipientRole: String, // "Dr. Martinez (Cardiology)", "Emergency Department", "Caregiver"
  val scope: String, // "Full Record", "Labs & Medications Only", "Emergency Snapshot Only"
  val durationLabel: String, // "24 hours", "7 days", "30 days"
  val expiresAt: String,
  val tokenKey: String,
  val accessCount: Int = 0,
  val isActive: Boolean = true
)

data class AuditEntry(
  val id: String,
  val timestamp: String,
  val actor: String,
  val action: String, // "FHIR_RESOURCE_READ", "SECURE_SHARE_ACCESS", "OCR_DOCUMENT_INGEST", "AI_QUERY_EXECUTE"
  val resource: String,
  val status: String = "SUCCESS",
  val ipAddress: String = "192.168.1.104"
)

data class Citation(
  val id: String,
  val title: String,
  val date: String,
  val source: String,
  val excerpt: String
)

data class ChatMessage(
  val id: String,
  val sender: String, // "user", "ai", "system"
  val text: String,
  val timestamp: String,
  val citations: List<Citation> = emptyList(),
  val isRedFlag: Boolean = false,
  val redFlagEscalationText: String? = null
)

data class AppointmentPrep(
  val id: String,
  val profileId: String,
  val doctorName: String,
  val clinic: String,
  val scheduledDate: String,
  val visitReason: String,
  val recentChanges: List<String>,
  val keyQuestionsToAsk: List<String>,
  val criticalMetrics: Map<String, String>,
  val activeMedsCount: Int
)

enum class SubscriptionTier {
  FREE,
  PREMIUM,
  PREMIUM_PLUS
}

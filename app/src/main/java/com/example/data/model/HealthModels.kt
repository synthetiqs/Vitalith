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
  val relationship: String, // "Primary Patient", "Child", "Elderly Parent", "Spouse", "Dependent"
  val dob: String,
  val bloodType: String,
  val mrn: String,
  val isPrimary: Boolean = false,
  val activeConditionsCount: Int = 0,
  val activeMedicationsCount: Int = 0,
  val pendingTasksCount: Int = 0,
  val primaryPhysician: String = "Dr. Sarah Jenkins, MD",
  val allergies: List<String> = emptyList(),
  val emergencyContact: String = "Elena Adarkwah • +1 (555) 345-6789",
  val notes: String = "",
  val gender: String = "Not Specified"
)

data class UserProfile(
  val id: String = "user_primary",
  val fullName: String = "Doug Adarkwah",
  val email: String = "abydouglas@gmail.com",
  val phone: String = "+1 (555) 234-8901",
  val biometricLockEnabled: Boolean = true,
  val hipaaConsentSigned: Boolean = true,
  val hipaaConsentDate: String = "May 14, 2026",
  val emergencyMedicalIdUnlocked: Boolean = true,
  val hardwareEnclaveEncryption: Boolean = true,
  val emergencyContactName: String = "Elena Adarkwah (Spouse)",
  val emergencyContactPhone: String = "+1 (555) 345-6789",
  val unitPreferences: UnitPreferences = UnitPreferences()
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

data class MedicationReminder(
  val id: String,
  val profileId: String = "profile_doug",
  val medicationId: String,
  val medicationName: String,
  val dosage: String,
  val hour: Int, // 0..23
  val minute: Int, // 0..59
  val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Sun .. 7=Sat
  val isEnabled: Boolean = true,
  val instructions: String = "Take with food and water",
  val isTakenToday: Boolean = false,
  val lastTakenTimestamp: Long? = null
) {
  val formattedTime: String
    get() {
      val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
      val amPm = if (hour < 12) "AM" else "PM"
      val m = if (minute < 10) "0$minute" else "$minute"
      return "$h:$m $amPm"
    }

  val recurrenceLabel: String
    get() = if (daysOfWeek.size == 7) "Every Day" else "Custom Days (${daysOfWeek.size} days/wk)"
}

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
  val profileId: String = "profile_doug",
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

enum class SubscriptionTier(
  val displayName: String,
  val monthlyPrice: Double,
  val annualPricePerMonth: Double,
  val subtitle: String,
  val maxProfiles: Int,
  val aiQueriesPerDay: Int,
  val ocrScansPerMonth: Int,
  val fhirAutoSyncEnabled: Boolean,
  val careTeamLinksAllowed: Int
) {
  FREE(
    displayName = "Vitalith Core",
    monthlyPrice = 0.0,
    annualPricePerMonth = 0.0,
    subtitle = "Essential personal health record vault",
    maxProfiles = 1,
    aiQueriesPerDay = 5,
    ocrScansPerMonth = 2,
    fhirAutoSyncEnabled = false,
    careTeamLinksAllowed = 1
  ),
  PREMIUM(
    displayName = "Vitalith Plus",
    monthlyPrice = 9.99,
    annualPricePerMonth = 7.49,
    subtitle = "Unlimited Vitalith AI, OCR engine & 5 family profiles",
    maxProfiles = 5,
    aiQueriesPerDay = 9999,
    ocrScansPerMonth = 9999,
    fhirAutoSyncEnabled = true,
    careTeamLinksAllowed = 5
  ),
  PREMIUM_PLUS(
    displayName = "Vitalith Concierge",
    monthlyPrice = 19.99,
    annualPricePerMonth = 14.99,
    subtitle = "Unlimited family enclaves, instant OCR & 24/7 care links",
    maxProfiles = 9999,
    aiQueriesPerDay = 9999,
    ocrScansPerMonth = 9999,
    fhirAutoSyncEnabled = true,
    careTeamLinksAllowed = 9999
  )
}

enum class BillingCycle(val label: String, val discountPercent: Int) {
  MONTHLY("Monthly", 0),
  ANNUAL("Annual (Save 25%)", 25)
}

data class SubscriptionDetails(
  val tier: SubscriptionTier = SubscriptionTier.PREMIUM,
  val billingCycle: BillingCycle = BillingCycle.ANNUAL,
  val status: String = "ACTIVE",
  val renewalDate: String = "Oct 26, 2027",
  val lastPaymentMethod: String = "Google Pay (abydouglas@gmail.com)",
  val lastTransactionId: String = "TXN-VTL-2026-98124",
  val autoRenew: Boolean = true
)

data class PaymentTransaction(
  val id: String,
  val date: String,
  val tier: SubscriptionTier,
  val billingCycle: BillingCycle,
  val amountPaid: Double,
  val paymentMethod: String,
  val status: String = "COMPLETED",
  val invoiceNumber: String
) {
  val transactionId: String get() = id
  val amount: Double get() = amountPaid
}

data class SymptomItem(
  val id: String,
  val profileId: String,
  val symptomName: String,
  val snomedCode: String = "",
  val severity: Int, // 0..10
  val severityLabel: String, // "Mild" (0-3), "Moderate" (4-6), "Severe" (7-10)
  val onsetDate: String, // "Oct 29, 2026"
  val onsetTimestamp: Long = System.currentTimeMillis(),
  val endedDate: String? = null,
  val endedTimestamp: Long? = null,
  val isOngoing: Boolean = true,
  val isIntermittent: Boolean = false,
  val bodySite: String? = null,
  val impact: List<String> = emptyList(), // "Work / Focus Limited", "Sleep Interrupted", "Bedridden"
  val contextTags: List<String> = emptyList(), // "Poor Sleep (<6h)", "High Stress", "Skipped Meal", "Luteal Phase", "Post-Exercise", "Medication Taken"
  val notes: String = "",
  val mediaUris: List<String> = emptyList(),
  val episodeId: String? = null,
  val isRedFlag: Boolean = false,
  val redFlagMessage: String? = null,
  val provenance: Provenance = Provenance(
    sourceSystem = "Patient Self-Report (Quick-Add)",
    ingestedAt = "Just now",
    confidenceScore = 1.0f,
    consentState = "Patient-Owned / Private Vault"
  )
) {
  val durationDisplay: String
    get() {
      if (isOngoing) return "Ongoing"
      val end = endedTimestamp ?: return "Ongoing"
      val diffMs = end - onsetTimestamp
      if (diffMs <= 0) return "Point in time"
      val hours = diffMs / (1000 * 60 * 60)
      val mins = (diffMs / (1000 * 60)) % 60
      return when {
        hours >= 24 -> "${hours / 24}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins}m"
      }
    }
}

data class SymptomDefinition(
  val id: String,
  val name: String,
  val snomedCode: String,
  val category: String, // "Neurological", "Digestive", "Musculoskeletal", "Respiratory", "Dermatological", "General"
  val defaultBodySite: String? = null,
  val isRedFlagCapable: Boolean = false
)

object SymptomCatalog {
  val definitions = listOf(
    SymptomDefinition("sym_migraine", "Migraine / Headache", "37796009", "Neurological", "Right Temple", isRedFlagCapable = true),
    SymptomDefinition("sym_acid_reflux", "Acid Reflux / Heartburn", "698065", "Digestive", "Epigastric", isRedFlagCapable = false),
    SymptomDefinition("sym_lower_back", "Lower Back Pain", "279039007", "Musculoskeletal", "Lumbar Spine", isRedFlagCapable = false),
    SymptomDefinition("sym_joint_pain", "Joint Stiffness / Ache", "57676002", "Musculoskeletal", "Bilateral Knees", isRedFlagCapable = false),
    SymptomDefinition("sym_fatigue", "Excessive Fatigue", "84229001", "General", "Whole Body", isRedFlagCapable = false),
    SymptomDefinition("sym_nausea", "Nausea / Queasiness", "422587007", "Digestive", "Stomach", isRedFlagCapable = true),
    SymptomDefinition("sym_skin_rash", "Skin Rash / Hives", "271807003", "Dermatological", "Arms / Torso", isRedFlagCapable = true),
    SymptomDefinition("sym_chest_tightness", "Chest Pain / Tightness", "29857009", "Cardiovascular", "Mid-Chest", isRedFlagCapable = true),
    SymptomDefinition("sym_dyspnea", "Shortness of Breath", "267036007", "Respiratory", "Chest / Lungs", isRedFlagCapable = true),
    SymptomDefinition("sym_throat_tightness", "Throat Tightness / Swelling", "289138009", "Immunological", "Throat", isRedFlagCapable = true),
    SymptomDefinition("sym_abdominal_cramp", "Abdominal Cramping / Pain", "21522001", "Digestive", "Lower Abdomen", isRedFlagCapable = true),
    SymptomDefinition("sym_dizziness", "Dizziness / Lightheadedness", "404640003", "Neurological", "Head", isRedFlagCapable = true)
  )
}

data class SymptomRecurrenceStats(
  val symptomName: String,
  val totalEpisodesThisMonth: Int,
  val totalEpisodesLastMonth: Int,
  val averageSeverity: Float,
  val peakSeverity: Int,
  val mostCommonContext: String?,
  val coOccurrenceObservation: String?
)

data class RedFlagAlert(
  val title: String,
  val message: String,
  val actionLabel: String,
  val emergencyPhone: String = "911",
  val isEmergency: Boolean = true
)

data class AppSettings(
  val autoLockTimeout: String = "5 minutes",
  val autoOcrProcessing: Boolean = true,
  val drugConflictAlerts: Boolean = true,
  val fhirSyncFrequency: String = "Real-Time",
  val refillReminders: Boolean = true,
  val labResultAlerts: Boolean = true,
  val careGrantAlerts: Boolean = true,
  val highContrastTheme: Boolean = false
)



package com.example.data.model

data class HealthcareProvider(
  val id: String,
  val name: String,
  val title: String = "MD",
  val specialty: String,
  val subSpecialty: String = "",
  val facilityName: String,
  val address: String,
  val city: String,
  val state: String,
  val zip: String,
  val distanceMiles: Double,
  val rating: Double,
  val reviewCount: Int,
  val phoneNumber: String,
  val acceptingNewPatients: Boolean = true,
  val telehealthAvailable: Boolean = true,
  val insuranceAccepted: List<String> = emptyList(),
  val hospitalAffiliation: String = "",
  val googleSearchQuery: String,
  val googleMapsQuery: String,
  val verifiedBadge: Boolean = true,
  val bio: String = ""
)

data class UserLocationInfo(
  val latitude: Double? = null,
  val longitude: Double? = null,
  val displayName: String = "Current Location",
  val city: String = "",
  val state: String = "",
  val postalCode: String = "",
  val isGpsAccurate: Boolean = false
)

enum class HealthcareSpecialty(val title: String, val chipLabel: String, val searchQuery: String) {
  ALL("All Healthcare Providers", "All", "healthcare providers doctors"),
  PRIMARY_CARE("Primary Care & Family Medicine", "Primary Care", "primary care physicians internal medicine doctors"),
  PEDIATRICS("Pediatrics & Child Specialists", "Pediatrics", "pediatricians pediatric clinic child health"),
  CARDIOLOGY("Cardiology & Heart Specialists", "Cardiology", "cardiologists heart specialists cardiac clinic"),
  ENDOCRINOLOGY("Endocrinology & Diabetes", "Endocrinology", "endocrinologists diabetes specialists"),
  ALLERGY_IMMUNOLOGY("Allergy & Clinical Immunology", "Allergy & Asthma", "allergists immunologists food allergy clinic"),
  URGENT_CARE("Urgent Care & Walk-in Clinics", "Urgent Care", "urgent care walk in clinic"),
  LAB_DIAGNOSTICS("Diagnostic Labs & Imaging", "Labs & Imaging", "diagnostic lab blood test LabCorp Quest Diagnostics"),
  DERMATOLOGY("Dermatology & Skin Care", "Dermatology", "dermatologists skin clinic"),
  NEUROLOGY("Neurology & Brain Health", "Neurology", "neurologists neurology clinic")
}

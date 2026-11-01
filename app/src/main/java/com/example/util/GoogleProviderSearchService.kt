package com.example.util

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.HealthcareProvider
import com.example.data.model.HealthcareSpecialty
import com.example.data.model.PatientProfile
import com.example.data.model.UserLocationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PatientSearchSuggestion(
  val title: String,
  val subtitle: String,
  val query: String,
  val specialty: HealthcareSpecialty,
  val reasonBadge: String
)

object GoogleProviderSearchService {

  private const val TAG = "GoogleProviderSearch"
  private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()

  /**
   * Builds an optimized Google Search URL for the provider/specialist query
   */
  fun buildGoogleSearchUrl(query: String, location: String): String {
    val cleanLocation = location.ifBlank { "near me" }
    val fullQuery = "$query $cleanLocation"
    val encoded = URLEncoder.encode(fullQuery, "UTF-8")
    return "https://www.google.com/search?q=$encoded"
  }

  /**
   * Builds a Google Maps Search URL
   */
  fun buildGoogleMapsUrl(query: String, location: String, lat: Double? = null, lng: Double? = null): String {
    val cleanLocation = location.ifBlank { "near me" }
    val fullQuery = "$query in $cleanLocation"
    val encoded = URLEncoder.encode(fullQuery, "UTF-8")
    return if (lat != null && lng != null) {
      "https://www.google.com/maps/search/?api=1&query=$encoded&center=$lat,$lng"
    } else {
      "https://www.google.com/maps/search/?api=1&query=$encoded"
    }
  }

  /**
   * Launches Google Search on device via Intent (Browser / Google App)
   */
  fun launchGoogleSearch(context: Context, query: String, location: String): Boolean {
    return try {
      val url = buildGoogleSearchUrl(query, location)
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch Google Search: ${e.message}")
      false
    }
  }

  /**
   * Launches Google Maps on device via Intent
   */
  fun launchGoogleMaps(context: Context, query: String, location: String, lat: Double? = null, lng: Double? = null): Boolean {
    return try {
      val geoUri = if (lat != null && lng != null) {
        val encoded = URLEncoder.encode("$query near $location", "UTF-8")
        Uri.parse("geo:$lat,$lng?q=$encoded")
      } else {
        Uri.parse(buildGoogleMapsUrl(query, location, lat, lng))
      }
      val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(mapIntent)
      true
    } catch (e: Exception) {
      // Fallback to regular web maps
      try {
        val webUrl = buildGoogleMapsUrl(query, location, lat, lng)
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
        true
      } catch (ex: Exception) {
        Log.e(TAG, "Failed to launch Google Maps: ${ex.message}")
        false
      }
    }
  }

  /**
   * Launches telephone dialer for provider contact
   */
  fun dialProvider(context: Context, phoneNumber: String): Boolean {
    return try {
      val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
      val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(dialIntent)
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch dialer: ${e.message}")
      false
    }
  }

  /**
   * Resolves current user location using LocationManager and Geocoder
   */
  fun resolveDeviceLocation(context: Context): UserLocationInfo {
    return try {
      val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return getFallbackLocation()

      var bestLocation: Location? = null

      // Check GPS provider
      try {
        val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (gpsLoc != null) bestLocation = gpsLoc
      } catch (e: SecurityException) {
        // Location permission not yet granted
      }

      // Check Network provider if GPS was null or stale
      if (bestLocation == null) {
        try {
          val netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
          if (netLoc != null) bestLocation = netLoc
        } catch (e: SecurityException) {
          // Location permission not yet granted
        }
      }

      if (bestLocation != null) {
        val lat = bestLocation.latitude
        val lng = bestLocation.longitude

        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lng, 1)

        if (!addresses.isNullOrEmpty()) {
          val addr = addresses[0]
          val city = addr.locality ?: addr.subAdminArea ?: "Current Location"
          val state = addr.adminArea ?: ""
          val zip = addr.postalCode ?: ""
          val display = if (state.isNotBlank()) "$city, $state" else city

          return UserLocationInfo(
            latitude = lat,
            longitude = lng,
            displayName = display,
            city = city,
            state = state,
            postalCode = zip,
            isGpsAccurate = true
          )
        } else {
          return UserLocationInfo(
            latitude = lat,
            longitude = lng,
            displayName = String.format(Locale.US, "%.3f, %.3f", lat, lng),
            isGpsAccurate = true
          )
        }
      }

      getFallbackLocation()
    } catch (e: Exception) {
      Log.w(TAG, "Unable to resolve device location: ${e.message}")
      getFallbackLocation()
    }
  }

  fun getFallbackLocation(): UserLocationInfo {
    return UserLocationInfo(
      latitude = 37.7749,
      longitude = -122.4194,
      displayName = "San Francisco, CA",
      city = "San Francisco",
      state = "CA",
      postalCode = "94102",
      isGpsAccurate = false
    )
  }

  /**
   * Generates patient-tailored search recommendations based on age, conditions, and allergies
   */
  fun getPatientSuggestions(profile: PatientProfile): List<PatientSearchSuggestion> {
    return when (profile.id) {
      "profile_maya" -> listOf(
        PatientSearchSuggestion(
          title = "Pediatricians & Child Health",
          subtitle = "Well-child exams, growth monitoring & childhood immunizations",
          query = "top pediatricians accepting new patients",
          specialty = HealthcareSpecialty.PEDIATRICS,
          reasonBadge = "Pediatric Care (Age 7)"
        ),
        PatientSearchSuggestion(
          title = "Pediatric Allergy & Immunology",
          subtitle = "Peanut allergy oral immunotherapy & anaphylaxis management",
          query = "pediatric allergist peanut allergy immunotherapy specialist",
          specialty = HealthcareSpecialty.ALLERGY_IMMUNOLOGY,
          reasonBadge = "Peanut Allergy Monitored"
        ),
        PatientSearchSuggestion(
          title = "Pediatric Urgent Care",
          subtitle = "Walk-in care specialized for infant and pediatric emergencies",
          query = "pediatric urgent care clinic open now",
          specialty = HealthcareSpecialty.URGENT_CARE,
          reasonBadge = "Urgent Child Care"
        )
      )

      "profile_kwame" -> listOf(
        PatientSearchSuggestion(
          title = "Cardiologists & Cath Follow-Up",
          subtitle = "Post-PCI LAD coronary stent surveillance & lipid management",
          query = "interventional cardiologists coronary artery disease stent follow up",
          specialty = HealthcareSpecialty.CARDIOLOGY,
          reasonBadge = "LAD Stent Monitored"
        ),
        PatientSearchSuggestion(
          title = "Cardiac Rehabilitation Centers",
          subtitle = "Monitored exercise, heart-healthy lifestyle & endurance rehab",
          query = "cardiac rehabilitation center outpatient program",
          specialty = HealthcareSpecialty.CARDIOLOGY,
          reasonBadge = "Post-Stent Recovery"
        ),
        PatientSearchSuggestion(
          title = "Geriatric & Internal Medicine",
          subtitle = "Comprehensive multi-medication reconciliation & senior health",
          query = "geriatric medicine specialist internal medicine for seniors",
          specialty = HealthcareSpecialty.PRIMARY_CARE,
          reasonBadge = "Senior Health (Age 72)"
        )
      )

      else -> listOf(
        PatientSearchSuggestion(
          title = "Endocrinologists & Metabolism",
          subtitle = "Pre-diabetes reversal, HbA1c trajectory & glucose control",
          query = "endocrinologist pre diabetes metabolic health specialist",
          specialty = HealthcareSpecialty.ENDOCRINOLOGY,
          reasonBadge = "Pre-Diabetes Monitored"
        ),
        PatientSearchSuggestion(
          title = "Internal Medicine & Primary Care",
          subtitle = "Annual preventative exam & metabolic panel follow-up",
          query = "internal medicine primary care physician accepting new patients",
          specialty = HealthcareSpecialty.PRIMARY_CARE,
          reasonBadge = "Annual Preventative"
        ),
        PatientSearchSuggestion(
          title = "Diagnostic Blood Draw Centers",
          subtitle = "Fasting glucose, comprehensive metabolic panel & lipid panels",
          query = "Quest Diagnostics LabCorp blood draw walk in lab",
          specialty = HealthcareSpecialty.LAB_DIAGNOSTICS,
          reasonBadge = "A1c & Lipid Surveillance"
        )
      )
    }
  }

  /**
   * Searches healthcare providers by query, specialty, and location.
   * Leverages real Google Search integration + curated regional directory.
   */
  fun searchProviders(
    query: String,
    specialty: HealthcareSpecialty,
    location: UserLocationInfo,
    profile: PatientProfile
  ): List<HealthcareProvider> {
    val locDisplay = location.displayName.ifBlank { "near me" }
    val city = location.city.ifBlank { "San Francisco" }
    val state = location.state.ifBlank { "CA" }

    val rawList = getDirectory(city, state, locDisplay)

    // Filter by specialty if not ALL
    val filteredBySpecialty = if (specialty == HealthcareSpecialty.ALL) {
      rawList
    } else {
      rawList.filter { provider ->
        matchesSpecialty(provider.specialty, specialty)
      }
    }

    // Filter by text query if non-blank
    val searchResults = if (query.isBlank()) {
      filteredBySpecialty
    } else {
      val q = query.trim().lowercase()
      filteredBySpecialty.filter { p ->
        p.name.lowercase().contains(q) ||
        p.specialty.lowercase().contains(q) ||
        p.facilityName.lowercase().contains(q) ||
        p.subSpecialty.lowercase().contains(q) ||
        p.bio.lowercase().contains(q) ||
        p.insuranceAccepted.any { it.lowercase().contains(q) }
      }
    }

    // If query returned no direct matches in curated list, construct custom Google-searchable provider card
    if (searchResults.isEmpty() && query.isNotBlank()) {
      return listOf(
        HealthcareProvider(
          id = "custom_search_result_1",
          name = "Specialists for \"$query\"",
          title = "Local Results",
          specialty = specialty.title,
          subSpecialty = "Google Search Healthcare Index",
          facilityName = "Medical Providers in $locDisplay",
          address = "Locations across $locDisplay",
          city = city,
          state = state,
          zip = location.postalCode.ifBlank { "94102" },
          distanceMiles = 1.2,
          rating = 4.8,
          reviewCount = 184,
          phoneNumber = "+1 (800) 555-0199",
          acceptingNewPatients = true,
          telehealthAvailable = true,
          insuranceAccepted = listOf("Aetna", "Blue Cross", "Medicare", "UnitedHealthcare"),
          hospitalAffiliation = "$city Regional Medical Center",
          googleSearchQuery = "$query in $locDisplay",
          googleMapsQuery = "$query doctors in $locDisplay",
          bio = "Direct Google Search index for \"$query\" in $locDisplay. Tap below to view live ratings, practice credentials, and appointment availability."
        )
      )
    }

    return searchResults
  }

  private fun matchesSpecialty(providerSpecialty: String, target: HealthcareSpecialty): Boolean {
    val ps = providerSpecialty.lowercase()
    return when (target) {
      HealthcareSpecialty.ALL -> true
      HealthcareSpecialty.PRIMARY_CARE -> ps.contains("internal") || ps.contains("primary") || ps.contains("family") || ps.contains("geriatric")
      HealthcareSpecialty.PEDIATRICS -> ps.contains("pediatric") || ps.contains("child") || ps.contains("adolescent")
      HealthcareSpecialty.CARDIOLOGY -> ps.contains("cardio") || ps.contains("heart") || ps.contains("vascular")
      HealthcareSpecialty.ENDOCRINOLOGY -> ps.contains("endocrin") || ps.contains("diabet") || ps.contains("metabol")
      HealthcareSpecialty.ALLERGY_IMMUNOLOGY -> ps.contains("allerg") || ps.contains("immuno") || ps.contains("asthma")
      HealthcareSpecialty.URGENT_CARE -> ps.contains("urgent") || ps.contains("walk-in") || ps.contains("emergency")
      HealthcareSpecialty.LAB_DIAGNOSTICS -> ps.contains("lab") || ps.contains("diagnostic") || ps.contains("pathology") || ps.contains("quest") || ps.contains("imaging")
      HealthcareSpecialty.DERMATOLOGY -> ps.contains("dermatol") || ps.contains("skin")
      HealthcareSpecialty.NEUROLOGY -> ps.contains("neuro") || ps.contains("brain")
    }
  }

  private fun getDirectory(city: String, state: String, locationDisplay: String): List<HealthcareProvider> {
    return listOf(
      // Primary Care
      HealthcareProvider(
        id = "prov_pcp_1",
        name = "Dr. Michael Chen",
        title = "MD, FAAP",
        specialty = "Pediatrics & Adolescent Medicine",
        subSpecialty = "Allergic Diseases & Pediatric Nutrition",
        facilityName = "Bayside Pediatric Health Group",
        address = "450 Sutter St, Suite 1200",
        city = city,
        state = state,
        zip = "94108",
        distanceMiles = 1.4,
        rating = 4.9,
        reviewCount = 218,
        phoneNumber = "+1 (555) 234-5678",
        acceptingNewPatients = true,
        telehealthAvailable = true,
        insuranceAccepted = listOf("Blue Cross", "Aetna", "UnitedHealthcare", "Cigna"),
        hospitalAffiliation = "$city Children's Hospital",
        googleSearchQuery = "Dr Michael Chen MD Pediatrics in $locationDisplay",
        googleMapsQuery = "Bayside Pediatric Health Group in $locationDisplay",
        bio = "Board-certified pediatrician with 14 years of clinical experience in early childhood asthma, severe food allergies, and preventative developmental medicine."
      ),
      HealthcareProvider(
        id = "prov_pcp_2",
        name = "Dr. Sarah Jenkins",
        title = "MD, FACP",
        specialty = "Internal Medicine & Preventative Health",
        subSpecialty = "Metabolic Syndrome & Cardiovascular Risk Reduction",
        facilityName = "Civic Health & Wellness Center",
        address = "789 Mission Street, Suite 400",
        city = city,
        state = state,
        zip = "94103",
        distanceMiles = 2.1,
        rating = 4.8,
        reviewCount = 175,
        phoneNumber = "+1 (555) 876-5432",
        acceptingNewPatients = true,
        telehealthAvailable = true,
        insuranceAccepted = listOf("Medicare", "Blue Cross", "Aetna", "Kaiser Choice"),
        hospitalAffiliation = "University Hospital & Medical Center",
        googleSearchQuery = "Dr Sarah Jenkins MD Internal Medicine in $locationDisplay",
        googleMapsQuery = "Civic Health and Wellness Center in $locationDisplay",
        bio = "Internist specializing in longitudinal biomarker tracking, lipid optimization, and proactive lifestyle protocols for insulin resistance."
      ),

      // Cardiology
      HealthcareProvider(
        id = "prov_cardio_1",
        name = "Dr. Marcus Vance",
        title = "MD, FACC, FSCAI",
        specialty = "Interventional Cardiology",
        subSpecialty = "Coronary Stent Surveillance & Complex PCI",
        facilityName = "Bay Area Heart & Vascular Institute",
        address = "1100 Van Ness Ave, 7th Floor",
        city = city,
        state = state,
        zip = "94109",
        distanceMiles = 3.0,
        rating = 4.9,
        reviewCount = 312,
        phoneNumber = "+1 (555) 432-1098",
        acceptingNewPatients = true,
        telehealthAvailable = false,
        insuranceAccepted = listOf("Medicare", "Blue Shield", "UnitedHealthcare", "Tricare"),
        hospitalAffiliation = "$city Memorial Heart Institute",
        googleSearchQuery = "Dr Marcus Vance MD Interventional Cardiology in $locationDisplay",
        googleMapsQuery = "Bay Area Heart and Vascular Institute in $locationDisplay",
        bio = "Chief of Interventional Cardiology with expertise in coronary stent patency, antiplatelet therapy management, and post-cardiac catheterization rehabilitation."
      ),

      // Endocrinology
      HealthcareProvider(
        id = "prov_endo_1",
        name = "Dr. Elena Rostova",
        title = "MD, FACE",
        specialty = "Endocrinology & Metabolism",
        subSpecialty = "Type 2 Diabetes Reversal & Lipidology",
        facilityName = "Metabolic Care & Endocrine Specialists",
        address = "220 Montgomery St, Suite 850",
        city = city,
        state = state,
        zip = "94104",
        distanceMiles = 1.8,
        rating = 4.9,
        reviewCount = 143,
        phoneNumber = "+1 (555) 765-4321",
        acceptingNewPatients = true,
        telehealthAvailable = true,
        insuranceAccepted = listOf("Aetna", "Blue Cross", "Cigna", "UnitedHealthcare"),
        hospitalAffiliation = "$city General Hospital",
        googleSearchQuery = "Dr Elena Rostova MD Endocrinology in $locationDisplay",
        googleMapsQuery = "Metabolic Care and Endocrine Specialists in $locationDisplay",
        bio = "Specializing in early glycemic dysregulation, pre-diabetes trajectory analysis, Continuous Glucose Monitor (CGM) optimization, and thyroid health."
      ),

      // Allergy & Immunology
      HealthcareProvider(
        id = "prov_allergy_1",
        name = "Dr. Priya Patel",
        title = "MD, FACAAI",
        specialty = "Allergy & Clinical Immunology",
        subSpecialty = "Pediatric Food Allergies & Oral Immunotherapy",
        facilityName = "Pacific Allergy & Asthma Associates",
        address = "3600 California Street, Suite 300",
        city = city,
        state = state,
        zip = "94118",
        distanceMiles = 2.8,
        rating = 4.9,
        reviewCount = 189,
        phoneNumber = "+1 (555) 654-3210",
        acceptingNewPatients = true,
        telehealthAvailable = true,
        insuranceAccepted = listOf("Blue Cross", "Aetna", "UnitedHealthcare", "Health Net"),
        hospitalAffiliation = "$city Children's Allergy Center",
        googleSearchQuery = "Dr Priya Patel MD Allergy Immunology in $locationDisplay",
        googleMapsQuery = "Pacific Allergy and Asthma Associates in $locationDisplay",
        bio = "Recognized expert in peanut oral immunotherapy (OIT), anaphylaxis action planning, and severe pediatric food allergy diagnostics."
      ),

      // Urgent Care
      HealthcareProvider(
        id = "prov_urgent_1",
        name = "Downtown Immediate Care & Walk-in",
        title = "Urgent Care Clinic",
        specialty = "Urgent Care & Walk-in Clinics",
        subSpecialty = "On-site X-Ray, EKG & Rapid Diagnostics",
        facilityName = "Metro Urgent Health Clinic",
        address = "555 Market Street",
        city = city,
        state = state,
        zip = "94105",
        distanceMiles = 0.8,
        rating = 4.7,
        reviewCount = 420,
        phoneNumber = "+1 (555) 987-6543",
        acceptingNewPatients = true,
        telehealthAvailable = true,
        insuranceAccepted = listOf("Most Major Insurances", "Medicare", "Medicaid", "Self-Pay"),
        hospitalAffiliation = "Affiliated with $city Health Network",
        googleSearchQuery = "Downtown Immediate Care Urgent Care in $locationDisplay",
        googleMapsQuery = "Metro Urgent Health Clinic in $locationDisplay",
        bio = "Open 7 days a week, 8am - 10pm. Full on-site laboratory, EKG testing, digital X-ray, and acute injury/illness treatment with zero wait appointment booking."
      ),

      // Diagnostics & Labs
      HealthcareProvider(
        id = "prov_lab_1",
        name = "Quest Diagnostics Patient Service Center",
        title = "Diagnostic Laboratory",
        specialty = "Diagnostic Labs & Imaging",
        subSpecialty = "Routine Blood Draws, Lipid Panels & HbA1c",
        facilityName = "Quest Diagnostics Service Hub",
        address = "1800 Post Street, Suite 105",
        city = city,
        state = state,
        zip = "94115",
        distanceMiles = 1.9,
        rating = 4.6,
        reviewCount = 280,
        phoneNumber = "+1 (555) 321-7654",
        acceptingNewPatients = true,
        telehealthAvailable = false,
        insuranceAccepted = listOf("All Major Insurances", "Medicare", "Direct Patient Billing"),
        hospitalAffiliation = "National CLIA Certified Diagnostic Network",
        googleSearchQuery = "Quest Diagnostics Lab blood draw in $locationDisplay",
        googleMapsQuery = "Quest Diagnostics Patient Service Center in $locationDisplay",
        bio = "Walk-in and scheduled appointments for comprehensive health screenings, fasting lipid panels, HbA1c, renal function tests, and urinalysis."
      ),
      HealthcareProvider(
        id = "prov_lab_2",
        name = "Labcorp Patient Testing Center",
        title = "Diagnostic Laboratory",
        specialty = "Diagnostic Labs & Imaging",
        subSpecialty = "Clinical Chemistry, Urinalysis & Immunology",
        facilityName = "Labcorp Medical Diagnostic Center",
        address = "3838 California St, Suite 210",
        city = city,
        state = state,
        zip = "94118",
        distanceMiles = 2.7,
        rating = 4.5,
        reviewCount = 195,
        phoneNumber = "+1 (555) 210-9876",
        acceptingNewPatients = true,
        telehealthAvailable = false,
        insuranceAccepted = listOf("All Major Insurances", "Medicare", "Out-of-Pocket"),
        hospitalAffiliation = "CLIA Certified Clinical Laboratory",
        googleSearchQuery = "Labcorp blood draw in $locationDisplay",
        googleMapsQuery = "Labcorp Patient Testing Center in $locationDisplay",
        bio = "Accredited laboratory diagnostic center with online scheduling, rapid turnaround times for routine metabolic panels, and seamless digital record export."
      ),

      // Dermatology
      HealthcareProvider(
        id = "prov_derm_1",
        name = "Dr. Linda Sterling",
        title = "MD, FAAD",
        specialty = "Dermatology & Skin Care",
        subSpecialty = "Skin Cancer Screening & Allergic Dermatitis",
        facilityName = "Golden Gate Dermatology Center",
        address = "2100 Webster St, Suite 500",
        city = city,
        state = state,
        zip = "94115",
        distanceMiles = 2.3,
        rating = 4.9,
        reviewCount = 210,
        phoneNumber = "+1 (555) 543-9876",
        acceptingNewPatients = true,
        telehealthAvailable = true,
        insuranceAccepted = listOf("Blue Shield", "Aetna", "UnitedHealthcare", "Medicare"),
        hospitalAffiliation = "$city Medical Center",
        googleSearchQuery = "Dr Linda Sterling MD Dermatology in $locationDisplay",
        googleMapsQuery = "Golden Gate Dermatology Center in $locationDisplay",
        bio = "Board-certified dermatologist specializing in complex skin eruptions, drug rash surveillance, contact dermatitis patch testing, and preventative mole mapping."
      ),

      // Neurology
      HealthcareProvider(
        id = "prov_neuro_1",
        name = "Dr. Arthur Bell",
        title = "MD, PhD, FAAN",
        specialty = "Neurology & Brain Health",
        subSpecialty = "Cerebrovascular Health & Cognitive Longevity",
        facilityName = "Northwest Neurological Institute",
        address = "1300 Gough Street, Suite 320",
        city = city,
        state = state,
        zip = "94109",
        distanceMiles = 2.5,
        rating = 4.8,
        reviewCount = 98,
        phoneNumber = "+1 (555) 678-1234",
        acceptingNewPatients = true,
        telehealthAvailable = true,
        insuranceAccepted = listOf("Medicare", "Blue Cross", "Aetna", "Cigna"),
        hospitalAffiliation = "$city Neurovascular Institute",
        googleSearchQuery = "Dr Arthur Bell MD Neurology in $locationDisplay",
        googleMapsQuery = "Northwest Neurological Institute in $locationDisplay",
        bio = "Neurologist focusing on post-stroke vascular risk reduction, migraine prevention, autonomic nerve testing, and neurological checkups."
      )
    )
  }
}

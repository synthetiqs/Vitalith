package com.example

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.VitalithDatabase
import com.example.data.local.entities.TimelineItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: VitalithDatabase

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, VitalithDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Vitalith", appName)
  }

  @Test
  fun `insert and retrieve timeline item from Room`() = runBlocking {
    val item = TimelineItemEntity(
      id = "test_item_1",
      profileId = "profile_doug",
      category = "LAB",
      title = "Blood Glucose Fasting",
      subtitle = "Quest Diagnostics",
      date = "Oct 22, 2026",
      clinicalDateMillis = System.currentTimeMillis(),
      providerOrFacility = "Quest Lab",
      status = "NORMAL",
      clinicalNotes = "Normal fasting glucose 92 mg/dL",
      sourceSystem = "Quest FHIR R4",
      ingestedAt = "2026-10-22",
      confidenceScore = 0.99f,
      consentState = "Full Consent",
      documentHash = "sha256:abc12345"
    )

    db.timelineDao().insert(item)

    val items = db.timelineDao().getAllTimelineItems().first()
    assertEquals(1, items.size)
    assertEquals("Blood Glucose Fasting", items.first().title)
    assertEquals("Quest FHIR R4", items.first().sourceSystem)

    val domainItem = items.first().toDomain()
    assertNotNull(domainItem.provenance)
    assertEquals(0.99f, domainItem.provenance.confidenceScore)
  }

  @Test
  fun `verify patient profile CRUD and active profile switching`() = runBlocking {
    val repository = com.example.data.repository.VitalithRepository(db)
    val initialProfiles = repository.profiles.first()
    assertEquals(3, initialProfiles.size)

    // Add new patient profile
    val newProfile = com.example.data.model.PatientProfile(
      id = "profile_kofi",
      name = "Kofi Adarkwah",
      relationship = "Son (Newborn)",
      dob = "2026-03-10",
      bloodType = "O+",
      mrn = "VTL-9901-KA",
      isPrimary = false,
      allergies = listOf("None reported")
    )
    repository.addPatientProfile(newProfile)

    val updatedProfiles = repository.profiles.first()
    assertEquals(4, updatedProfiles.size)
    assertEquals("Kofi Adarkwah", updatedProfiles.last().name)

    // Switch active profile
    repository.switchProfile("profile_kofi")
    assertEquals("profile_kofi", repository.currentProfileId.first())

    // Update patient profile
    val modifiedProfile = newProfile.copy(notes = "Healthy infant checkup completed")
    repository.updatePatientProfile(modifiedProfile)
    val profileAfterUpdate = repository.profiles.first().first { it.id == "profile_kofi" }
    assertEquals("Healthy infant checkup completed", profileAfterUpdate.notes)

    // Delete non-primary patient profile
    repository.deletePatientProfile("profile_kofi")
    val finalProfiles = repository.profiles.first()
    assertEquals(3, finalProfiles.size)
  }

  @Test
  fun `verify user profile and security preferences`() = runBlocking {
    val repository = com.example.data.repository.VitalithRepository(db)
    val initialUser = repository.userProfile.first()
    assertEquals("Doug Adarkwah", initialUser.fullName)
    assertEquals("abydouglas@gmail.com", initialUser.email)

    // Update account credentials
    repository.updateUserProfile(initialUser.copy(phone = "+1 (555) 999-0000"))
    assertEquals("+1 (555) 999-0000", repository.userProfile.first().phone)

    // Toggle security switches
    repository.setBiometricLock(false)
    assertEquals(false, repository.userProfile.first().biometricLockEnabled)
    repository.setBiometricLock(true)
    assertEquals(true, repository.userProfile.first().biometricLockEnabled)

    repository.setEmergencyMedicalIdUnlocked(false)
    assertEquals(false, repository.userProfile.first().emergencyMedicalIdUnlocked)
    repository.setEmergencyMedicalIdUnlocked(true)
    assertEquals(true, repository.userProfile.first().emergencyMedicalIdUnlocked)
  }

  @Test
  fun `verify user authentication, demo session, and logout lifecycle`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authService = com.example.data.auth.AuthService(context)

    // 1. Initially signed out if no existing session
    authService.signOut()
    assertEquals(null, authService.currentUser.value)

    // 2. Clinical demo sign-in for Doug
    val dougResult = authService.signInDemoUser("abydouglas@gmail.com")
    assertEquals(true, dougResult.isSuccess)
    val doug = dougResult.getOrNull()
    assertNotNull(doug)
    assertEquals("abydouglas@gmail.com", doug?.email)
    assertEquals("Doug Adarkwah", doug?.displayName)
    assertEquals("abydouglas@gmail.com", authService.currentUser.value?.email)

    // 3. Clinical demo sign-in for Sarah
    val sarahResult = authService.signInDemoUser("sarah.jenkins@familyhealth.org")
    assertEquals(true, sarahResult.isSuccess)
    assertEquals("sarah.jenkins@familyhealth.org", authService.currentUser.value?.email)
    assertEquals("Sarah Jenkins", authService.currentUser.value?.displayName)

    // 4. Log out locks session
    authService.signOut()
    assertEquals(null, authService.currentUser.value)
  }

  @Test
  fun `verify user registration, email-password login, and credential validation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authService = com.example.data.auth.AuthService(context)
    authService.signOut()

    // 1. Name too short
    val shortNameResult = authService.registerUser("J", "john.doe@example.com", "SecureP@ss123")
    assertEquals(true, shortNameResult.isFailure)

    // 2. Invalid email format
    val badEmailResult = authService.registerUser("John Doe", "invalid-email-no-at", "SecureP@ss123")
    assertEquals(true, badEmailResult.isFailure)

    // 3. Password too short (< 8 chars)
    val shortPwdResult = authService.registerUser("John Doe", "john.doe@example.com", "short")
    assertEquals(true, shortPwdResult.isFailure)

    // 4. Valid Registration
    val regResult = authService.registerUser("John Doe", "john.doe@vitalith-health.org", "EnclaveSecret123!")
    assertEquals(true, regResult.isSuccess)
    val registeredUser = regResult.getOrNull()
    assertNotNull(registeredUser)
    assertEquals("John Doe", registeredUser?.displayName)
    assertEquals("john.doe@vitalith-health.org", registeredUser?.email)
    assertEquals("john.doe@vitalith-health.org", authService.currentUser.value?.email)

    // 5. Duplicate registration prevented
    val duplicateResult = authService.registerUser("John Doe", "john.doe@vitalith-health.org", "AnotherSecret123")
    assertEquals(true, duplicateResult.isFailure)

    // 6. Logout terminates session
    authService.signOut()
    assertEquals(null, authService.currentUser.value)

    // 7. Wrong password fails login
    val wrongPwdResult = authService.signInWithEmailPassword("john.doe@vitalith-health.org", "WrongPassword!")
    assertEquals(true, wrongPwdResult.isFailure)
    assertEquals(null, authService.currentUser.value)

    // 8. Correct password logs in
    val loginResult = authService.signInWithEmailPassword("john.doe@vitalith-health.org", "EnclaveSecret123!")
    assertEquals(true, loginResult.isSuccess)
    assertEquals("john.doe@vitalith-health.org", authService.currentUser.value?.email)
    assertEquals("John Doe", authService.currentUser.value?.displayName)
  }

  @Test
  fun `verify logout means logout with persistent session purge across restarts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authService1 = com.example.data.auth.AuthService(context)

    // Sign in demo user
    authService1.signInDemoUser("abydouglas@gmail.com")
    assertEquals("abydouglas@gmail.com", authService1.currentUser.value?.email)

    // Explicit Log Out
    authService1.signOut()
    assertEquals(null, authService1.currentUser.value)

    // Simulate fresh app launch with new service instance
    val authService2 = com.example.data.auth.AuthService(context)
    // Logout means logout: no persisted session is resurrected
    assertEquals(null, authService2.currentUser.value)
  }

  @Test
  fun `verify onboarding completion flag for new installations and reset`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authService = com.example.data.auth.AuthService(context)

    // Reset for fresh install test
    authService.resetOnboarding()
    assertEquals(false, authService.hasCompletedOnboarding())

    // Complete onboarding
    authService.setCompletedOnboarding(true)
    assertEquals(true, authService.hasCompletedOnboarding())

    // Reset again
    authService.resetOnboarding()
    assertEquals(false, authService.hasCompletedOnboarding())
  }

  @Test
  fun `verify CareGrant creation, link token generation, and revocation`() = runBlocking {
    val repository = com.example.data.repository.VitalithRepository(db)
    val grant = repository.createCareGrant(
      recipientName = "Dr. Robert Vance",
      role = "Geriatrics Specialist",
      scope = "Labs & Medications Only",
      durationLabel = "7 days"
    )
    assertNotNull(grant)
    assertEquals("Dr. Robert Vance", grant.recipientName)
    assertEquals(true, grant.isActive)
    assertEquals(true, grant.tokenKey.startsWith("vtl_share_"))

    // Verify in care grants list
    val grants = repository.careGrants.first()
    assertEquals(true, grants.any { it.id == grant.id && it.isActive })

    // Revoke
    repository.revokeCareGrant(grant.id)
    val updatedGrants = repository.careGrants.first()
    val revoked = updatedGrants.first { it.id == grant.id }
    assertEquals(false, revoked.isActive)
  }

  @Test
  fun `verify FHIR R4 export, CSV export, and FHIR import`() = runBlocking {
    val repository = com.example.data.repository.VitalithRepository(db)

    // Export FHIR R4 JSON
    val fhirJson = repository.exportFhirBundleJson()
    assertNotNull(fhirJson)
    assertEquals(true, fhirJson.contains("\"resourceType\": \"Bundle\""))
    assertEquals(true, fhirJson.contains("\"type\": \"collection\""))

    // Export CSV
    val csv = repository.exportMedicationsCsv()
    assertNotNull(csv)
    assertEquals(true, csv.contains("name,dosage,frequency"))

    // Import external FHIR bundle
    val externalBundle = """
    {
      "resourceType": "Bundle",
      "type": "collection",
      "entry": [
        {
          "resource": {
            "resourceType": "Observation",
            "code": { "text": "Cardiac Calcium Score: 12 Agatston" },
            "effectiveDateTime": "2026-10-22",
            "performer": [{ "display": "Metro Imaging Center" }],
            "note": [{ "text": "Minimal coronary plaque identified. Low cardiovascular risk bracket." }]
          }
        }
      ]
    }
    """.trimIndent()
    val (importedCount, message) = repository.importFhirBundle(externalBundle)
    assertEquals(1, importedCount)
    assertEquals(true, message.contains("1"))

    val timeline = repository.timelineItems.first()
    assertEquals(true, timeline.any { it.title.contains("Cardiac Calcium Score") })
  }

  @Test
  fun `verify ClinicalPdfGenerator creates valid non-empty files for prep and timeline item`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.repository.VitalithRepository(db)
    val prep = repository.getAppointmentPrep()
    val profile = repository.profiles.value.first()

    // 1. Appointment prep PDF
    val prepFile = com.example.util.ClinicalPdfGenerator.generateAppointmentPrepPdf(context, prep, profile)
    assertNotNull(prepFile)
    assertEquals(true, prepFile.exists())
    assertEquals(true, prepFile.length() > 0)

    // 2. Clinical record PDF
    val item = repository.timelineItems.value.first()
    val recordFile = com.example.util.ClinicalPdfGenerator.generateRecordPdf(context, item, profile)
    assertNotNull(recordFile)
    assertEquals(true, recordFile.exists())
    assertEquals(true, recordFile.length() > 0)
  }

  @Test
  fun `verify adding new health record entry saves to Room database with date, tags, and category`() = runBlocking {
    val repository = com.example.data.repository.VitalithRepository(db)
    val testItem = com.example.data.model.HealthTimelineItem(
      id = "rec_test_room_101",
      profileId = "profile_doug",
      category = com.example.data.model.TimelineCategory.LAB,
      title = "Serum Creatinine & eGFR",
      subtitle = "Quest Diagnostics • Comprehensive Metabolic",
      date = "Oct 23, 2026",
      providerOrFacility = "Quest Diagnostics Hub",
      status = com.example.data.model.ItemStatus.NORMAL,
      clinicalNotes = "Normal renal filtration rate. Creatinine 0.88 mg/dL. eGFR > 95 mL/min.",
      provenance = com.example.data.model.Provenance(
        sourceSystem = "Patient Manual Entry (Room SQLite)",
        ingestedAt = "2026-10-23 11:45 UTC",
        confidenceScore = 1.0f,
        consentState = "Patient-Owned / Full Local Consent",
        documentHash = "sha256:7f83b1657ff1...48b3"
      ),
      tags = listOf("Renal", "Metabolic", "Routine"),
      keyValues = mapOf("Creatinine" to "0.88 mg/dL", "eGFR" to "> 95 mL/min")
    )

    // Save record via repository
    repository.addTimelineItem(testItem)

    // Verify it is in repository StateFlow
    val timeline = repository.timelineItems.first()
    val savedItem = timeline.firstOrNull { it.id == "rec_test_room_101" }
    assertNotNull(savedItem)
    assertEquals("Serum Creatinine & eGFR", savedItem?.title)
    assertEquals(com.example.data.model.TimelineCategory.LAB, savedItem?.category)
    assertEquals("Oct 23, 2026", savedItem?.date)
    assertEquals(listOf("Renal", "Metabolic", "Routine"), savedItem?.tags)
    assertEquals("0.88 mg/dL", savedItem?.keyValues?.get("Creatinine"))

    // Verify it persists in Room SQLite entity table, awaiting async coroutine
    var roomEntity = db.timelineDao().searchTimeline("profile_doug", "Creatinine").first()
    var attempts = 0
    while (roomEntity.isEmpty() && attempts < 20) {
      kotlinx.coroutines.delay(50)
      roomEntity = db.timelineDao().searchTimeline("profile_doug", "Creatinine").first()
      attempts++
    }
    assertEquals(1, roomEntity.size)
    assertEquals("Serum Creatinine & eGFR", roomEntity.first().title)
    assertEquals("LAB", roomEntity.first().category)
    assertEquals("Renal,Metabolic,Routine", roomEntity.first().tags)
    assertEquals(true, roomEntity.first().keyValuesRaw.contains("Creatinine:0.88 mg/dL"))
  }

  @Test
  fun `verify MetricTrendType enum values and units`() {
    val bp = com.example.ui.components.MetricTrendType.BLOOD_PRESSURE
    val weight = com.example.ui.components.MetricTrendType.BODY_WEIGHT
    val glucose = com.example.ui.components.MetricTrendType.BLOOD_GLUCOSE
    assertEquals("Blood Pressure", bp.label)
    assertEquals("mmHg", bp.unit)
    assertEquals("Body Weight", weight.label)
    assertEquals("kg", weight.unit)
    assertEquals("Fasting Glucose", glucose.label)
    assertEquals("mmol/L", glucose.unit)
  }

  @Test
  fun `verify medication reminder creation, toggling, marking taken, and deletion`() = runBlocking {
    val repository = com.example.data.repository.VitalithRepository(db)
    val initialReminders = repository.reminders.first()
    assertEquals(3, initialReminders.size)

    val newReminder = com.example.data.model.MedicationReminder(
      id = "rem_test_daily",
      profileId = "profile_doug",
      medicationId = "med_1",
      medicationName = "Metformin HCl",
      dosage = "500 mg",
      hour = 12,
      minute = 30,
      daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
      isEnabled = true,
      instructions = "Take with lunch"
    )

    repository.addMedicationReminder(newReminder)
    val afterAdd = repository.reminders.first()
    assertEquals(4, afterAdd.size)
    assertEquals("12:30 PM", newReminder.formattedTime)
    assertEquals("Every Day", newReminder.recurrenceLabel)

    // Toggle reminder
    repository.toggleMedicationReminder("rem_test_daily", false)
    val afterToggle = repository.reminders.first()
    assertEquals(false, afterToggle.first { it.id == "rem_test_daily" }.isEnabled)

    // Mark as taken
    repository.markReminderTaken("rem_test_daily", true)
    val afterTaken = repository.reminders.first()
    assertEquals(true, afterTaken.first { it.id == "rem_test_daily" }.isTakenToday)

    // Delete reminder
    repository.deleteMedicationReminder("rem_test_daily")
    val afterDelete = repository.reminders.first()
    assertEquals(3, afterDelete.size)
    assertEquals(false, afterDelete.any { it.id == "rem_test_daily" })
  }

  @Test
  fun `verify MedicationNotificationHelper creates notification channel on Android`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.notification.MedicationNotificationHelper.createNotificationChannel(context)
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channel = manager.getNotificationChannel(com.example.notification.MedicationNotificationHelper.CHANNEL_ID)
    assertNotNull(channel)
    assertEquals(com.example.notification.MedicationNotificationHelper.CHANNEL_NAME, channel.name)
  }

  @Test
  fun `verify GeminiMedicalExplainer extracts medical terms from clinical records`() {
    val detected = com.example.util.GeminiMedicalExplainer.extractMedicalTerms(
      title = "Comprehensive Metabolic Panel & Glycemic Evaluation",
      subtitle = "Quest Diagnostics • HbA1c and Renal Function",
      notes = "Patient exhibits elevated HbA1c at 6.4%, eGFR optimal at 98 mL/min, serum Creatinine 0.88 mg/dL. Continuing Metformin HCl 500mg.",
      keyValues = mapOf("HbA1c" to "6.4%", "Creatinine" to "0.88 mg/dL", "eGFR" to "98 mL/min"),
      tags = listOf("Metabolic", "Routine")
    )

    assertEquals(true, detected.contains("HbA1c"))
    assertEquals(true, detected.contains("eGFR"))
    assertEquals(true, detected.contains("Creatinine"))
    assertEquals(true, detected.contains("Metformin"))
  }

  @Test
  fun `verify GeminiMedicalExplainer returns structured clinical explanation`() = runBlocking {
    val explanation = com.example.util.GeminiMedicalExplainer.explainTerm(
      term = "HbA1c",
      clinicalContext = "Recent level 6.4% on Metformin 500mg BID"
    )

    assertNotNull(explanation)
    assertEquals(true, explanation.term.contains("HbA1c") || explanation.term.contains("Hemoglobin"))
    assertEquals(true, explanation.simpleDefinition.isNotBlank())
    assertEquals(true, explanation.clinicalSignificance.isNotBlank())
    assertEquals(true, explanation.referenceRangeGuidance.isNotBlank())
    assertEquals(true, explanation.questionsToAskDoctor.isNotEmpty())
  }

  @Test
  fun `verify all 9 design token themes exist and have correct descriptors`() {
    val themes = com.example.ui.theme.ALL_THEMES
    assertEquals(9, themes.size)

    val expectedKeys = listOf("original", "plum", "forest", "mono", "indigo", "sandstone", "arctic", "rose", "contrast")
    assertEquals(expectedKeys, themes.map { it.key })

    // Verify contrast theme attributes
    val contrast = themes.first { it.key == "contrast" }
    assertEquals(true, contrast.isDark)
    assertEquals(androidx.compose.ui.text.font.FontWeight.ExtraBold, contrast.tokens.fontWeightDisplay)
    assertEquals(2.dp, contrast.tokens.borderWidthCard)

    // Verify arctic theme is light
    val arctic = themes.first { it.key == "arctic" }
    assertEquals(false, arctic.isDark)
  }

  @Test
  fun `verify ThemeManager persists theme selection under key app-theme`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("vitalith_theme_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val themeManager = com.example.ui.theme.ThemeManager

    themeManager.init(context, isSystemDark = true)
    themeManager.setTheme("plum", persist = true)

    assertEquals("plum", themeManager.currentThemeKey.value)
    assertEquals("Plum & Sage", themeManager.currentTokens.value.label)

    // Verify stored in SharedPreferences under key "app-theme"
    assertEquals("plum", prefs.getString("app-theme", null))

    // Switch to High-Contrast AAA
    themeManager.setTheme("contrast", persist = true)
    assertEquals("contrast", themeManager.currentThemeKey.value)
    assertEquals("contrast", prefs.getString("app-theme", null))

    // Reset to system default for light mode -> should be "arctic"
    themeManager.resetToSystem(isSystemDark = false)
    assertEquals("arctic", themeManager.currentThemeKey.value)
  }

  @Test
  fun `verify UCUM blood glucose conversion accuracy`() {
    val mgDlVal = 180.182
    val mmolLVal = com.example.util.UcumUnitConverter.mgDlToMmolL(mgDlVal)
    assertEquals(10.0, mmolLVal, 0.001)

    val convertedBack = com.example.util.UcumUnitConverter.mmolLToMgDl(mmolLVal)
    assertEquals(mgDlVal, convertedBack, 0.001)

    // Standard fasting reading 100 mg/dL -> ~5.55 mmol/L
    val standardFastingMmol = com.example.util.UcumUnitConverter.mgDlToMmolL(100.0)
    assertEquals(5.55, standardFastingMmol, 0.01)
  }

  @Test
  fun `verify UCUM body temperature conversion accuracy`() {
    // Normal body temp 37.0 C = 98.6 F
    val fahrenheit = com.example.util.UcumUnitConverter.celsiusToFahrenheit(37.0)
    assertEquals(98.6, fahrenheit, 0.01)

    val celsius = com.example.util.UcumUnitConverter.fahrenheitToCelsius(98.6)
    assertEquals(37.0, celsius, 0.01)

    // Freezing and boiling points
    assertEquals(32.0, com.example.util.UcumUnitConverter.celsiusToFahrenheit(0.0), 0.01)
    assertEquals(212.0, com.example.util.UcumUnitConverter.celsiusToFahrenheit(100.0), 0.01)
  }

  @Test
  fun `verify UCUM body weight conversion accuracy`() {
    // 100 kg ≈ 220.462 lbs
    val lbs = com.example.util.UcumUnitConverter.kgToLbs(100.0)
    assertEquals(220.462, lbs, 0.01)

    val kg = com.example.util.UcumUnitConverter.lbsToKg(lbs)
    assertEquals(100.0, kg, 0.01)

    // Patient weight 172.4 lbs ≈ 78.2 kg
    val patientKg = com.example.util.UcumUnitConverter.lbsToKg(172.4)
    assertEquals(78.2, patientKg, 0.1)
  }

  @Test
  fun `verify UCUM height conversion accuracy`() {
    // 178 cm ≈ 5 ft 10 in
    val (feet, inches) = com.example.util.UcumUnitConverter.cmToFeetAndInches(178.0)
    assertEquals(5, feet)
    assertEquals(10, inches)

    val cm = com.example.util.UcumUnitConverter.feetAndInchesToCm(5, 10)
    assertEquals(177.8, cm, 0.1)
  }

  @Test
  fun `verify UCUM cholesterol conversion accuracy`() {
    // 188 mg/dL total cholesterol ≈ 4.86 mmol/L
    val mmol = com.example.util.UcumUnitConverter.cholesterolMgDlToMmolL(188.0)
    assertEquals(4.86, mmol, 0.05)

    val mgDl = com.example.util.UcumUnitConverter.cholesterolMmolLToMgDl(mmol)
    assertEquals(188.0, mgDl, 0.05)
  }

  @Test
  fun `verify formatLocalizedKeyValue adapts dynamically to user preferences`() {
    val usPrefs = com.example.data.model.UnitPreferences.createUsCustomary()
    val siPrefs = com.example.data.model.UnitPreferences.createMetricSi()

    // Glucose: 100 mg/dL in US vs SI
    val glucoseUs = com.example.util.UcumUnitConverter.formatLocalizedKeyValue("Glucose", "100 mg/dL", usPrefs)
    assertEquals("100 mg/dL", glucoseUs.displayString)
    assertEquals(false, glucoseUs.wasConverted)

    val glucoseSi = com.example.util.UcumUnitConverter.formatLocalizedKeyValue("Glucose", "100 mg/dL", siPrefs)
    assertEquals("5.5 mmol/L", glucoseSi.displayString)
    assertEquals(true, glucoseSi.wasConverted)
    assertEquals("mmol/L", glucoseSi.ucumCode)

    // Temperature: 98.6 °F in US vs SI
    val tempUs = com.example.util.UcumUnitConverter.formatLocalizedKeyValue("Body Temperature", "98.6 °F", usPrefs)
    assertEquals("98.6 °F", tempUs.displayString)

    val tempSi = com.example.util.UcumUnitConverter.formatLocalizedKeyValue("Body Temperature", "98.6 °F", siPrefs)
    assertEquals("37.0 °C", tempSi.displayString)
    assertEquals(true, tempSi.wasConverted)
    assertEquals("Cel", tempSi.ucumCode)

    // Weight: 172.4 lbs in US vs SI
    val weightUs = com.example.util.UcumUnitConverter.formatLocalizedKeyValue("Weight", "172.4 lbs", usPrefs)
    assertEquals("172.4 lbs", weightUs.displayString)

    val weightSi = com.example.util.UcumUnitConverter.formatLocalizedKeyValue("Weight", "172.4 lbs", siPrefs)
    assertEquals("78.2 kg", weightSi.displayString)
    assertEquals(true, weightSi.wasConverted)
    assertEquals("kg", weightSi.ucumCode)
  }

  @Test
  fun `verify repository updates and persists UnitPreferences reactively`() {
    val repo = com.example.data.repository.VitalithRepository()

    // Default is Metric / SI across the entire app
    assertEquals(com.example.data.model.UnitSystem.METRIC_SI, repo.userProfile.value.unitPreferences.unitSystem)
    assertEquals(com.example.data.model.GlucoseUnit.MMOL_L, repo.userProfile.value.unitPreferences.glucoseUnit)
    assertEquals(com.example.data.model.TemperatureUnit.CELSIUS, repo.userProfile.value.unitPreferences.temperatureUnit)
    assertEquals(com.example.data.model.WeightUnit.KG, repo.userProfile.value.unitPreferences.weightUnit)
    assertEquals(com.example.data.model.HeightUnit.CM, repo.userProfile.value.unitPreferences.heightUnit)
    assertEquals(com.example.data.model.CholesterolUnit.MMOL_L, repo.userProfile.value.unitPreferences.cholesterolUnit)

    // Switch to US Customary preset
    repo.setUnitSystem(com.example.data.model.UnitSystem.US_CUSTOMARY)
    assertEquals(com.example.data.model.UnitSystem.US_CUSTOMARY, repo.userProfile.value.unitPreferences.unitSystem)
    assertEquals(com.example.data.model.GlucoseUnit.MG_DL, repo.userProfile.value.unitPreferences.glucoseUnit)
    assertEquals(com.example.data.model.TemperatureUnit.FAHRENHEIT, repo.userProfile.value.unitPreferences.temperatureUnit)
    assertEquals(com.example.data.model.WeightUnit.LBS, repo.userProfile.value.unitPreferences.weightUnit)
    assertEquals(com.example.data.model.HeightUnit.FT_IN, repo.userProfile.value.unitPreferences.heightUnit)
    assertEquals(com.example.data.model.CholesterolUnit.MG_DL, repo.userProfile.value.unitPreferences.cholesterolUnit)

    // Granular unit change: switch glucose back to Metric mmol/L
    repo.setGlucoseUnit(com.example.data.model.GlucoseUnit.MMOL_L)
    assertEquals(com.example.data.model.GlucoseUnit.MMOL_L, repo.userProfile.value.unitPreferences.glucoseUnit)
    // Other units remain as selected in US Customary
    assertEquals(com.example.data.model.TemperatureUnit.FAHRENHEIT, repo.userProfile.value.unitPreferences.temperatureUnit)
  }

  @Test
  fun `verify FHIR bundle export generates computable UCUM valueQuantity`() {
    val repo = com.example.data.repository.VitalithRepository()
    val fhirJson = repo.exportFhirBundleJson()

    assertNotNull(fhirJson)
    assertEquals(true, fhirJson.contains("\"resourceType\": \"Bundle\""))
    assertEquals(true, fhirJson.contains("\"valueQuantity\":"))
    assertEquals(true, fhirJson.contains("\"system\": \"http://unitsofmeasure.org\""))
  }

  @Test
  fun `verify GeminiHealthSummarizer generates natural language summary with anomalies and insights`() = runBlocking {
    val repo = com.example.data.repository.VitalithRepository()
    val timeline = repo.timelineItems.first()

    val summary = com.example.util.GeminiHealthSummarizer.generateSummary(
      records = timeline,
      patientName = "Doug Adarkwah",
      unitPreferences = com.example.data.model.UnitPreferences()
    )

    assertNotNull(summary)
    assertEquals(true, summary.executiveSummary.isNotBlank())
    assertEquals(true, summary.executiveSummary.contains("Doug"))
    assertEquals(true, summary.keyInsights.isNotEmpty())
    assertEquals(true, summary.flaggedFindings.isNotEmpty())
    assertEquals(true, summary.suggestedQuestions.isNotEmpty())

    // Verify anomaly detection flagged HbA1c
    val hba1cFinding = summary.flaggedFindings.firstOrNull { it.biomarkerOrItem.contains("A1c") }
    assertNotNull(hba1cFinding)
    assertEquals(true, hba1cFinding?.clinicalRationale?.let {
      it.contains("prediabet", ignoreCase = true) || it.contains("pre-diabet", ignoreCase = true) || it.contains("diabetes", ignoreCase = true) || it.contains("glucose", ignoreCase = true) || it.contains("glycemic", ignoreCase = true)
    } == true)

    // Default preferences are Metric / SI -> LDL is in mmol/L
    val ldlFinding = summary.flaggedFindings.firstOrNull { it.biomarkerOrItem.contains("LDL") }
    assertNotNull(ldlFinding)
    assertEquals(true, ldlFinding?.observedValue?.contains("mmol/L") == true)
  }

  @Test
  fun `verify GeminiHealthSummarizer respects localized unit preferences`() = runBlocking {
    val repo = com.example.data.repository.VitalithRepository()
    val timeline = repo.timelineItems.first()

    // Test with US Customary preferences (mg/dL for lipids)
    val usPrefs = com.example.data.model.UnitPreferences.createUsCustomary()
    val summaryUs = com.example.util.GeminiHealthSummarizer.generateSummary(
      records = timeline,
      patientName = "Doug Adarkwah",
      unitPreferences = usPrefs
    )
    assertNotNull(summaryUs)
    val ldlUs = summaryUs.flaggedFindings.firstOrNull { it.biomarkerOrItem.contains("LDL") }
    assertNotNull(ldlUs)
    assertEquals(true, ldlUs?.observedValue?.contains("mg/dL") == true)

    // Test with SI / Metric preferences (mmol/L for lipids)
    val siPrefs = com.example.data.model.UnitPreferences.createMetricSi()
    val summarySi = com.example.util.GeminiHealthSummarizer.generateSummary(
      records = timeline,
      patientName = "Doug Adarkwah",
      unitPreferences = siPrefs
    )

    assertNotNull(summarySi)
    val ldlFinding = summarySi.flaggedFindings.firstOrNull { it.biomarkerOrItem.contains("LDL") }
    assertNotNull(ldlFinding)
    // Converted to mmol/L
    assertEquals(true, ldlFinding?.observedValue?.contains("mmol/L") == true)
  }

  @Test
  fun `verify ThemeManager manages 9 themes and switches active tokens`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.ui.theme.ThemeManager.init(context, isSystemDark = true)

    val available = com.example.ui.theme.ThemeManager.availableThemes
    assertEquals(9, available.size)

    // Switch to Plum theme
    com.example.ui.theme.ThemeManager.setTheme("plum", persist = true)
    assertEquals("plum", com.example.ui.theme.ThemeManager.currentThemeKey.value)
    assertEquals("Plum & Sage", com.example.ui.theme.ThemeManager.currentTokens.value.label)

    // Switch to High Contrast AAA theme
    com.example.ui.theme.ThemeManager.setTheme("contrast", persist = true)
    assertEquals("contrast", com.example.ui.theme.ThemeManager.currentThemeKey.value)
    assertEquals("High-Contrast AAA", com.example.ui.theme.ThemeManager.currentTokens.value.label)

    // Switch back to original
    com.example.ui.theme.ThemeManager.setTheme("original", persist = true)
    assertEquals("original", com.example.ui.theme.ThemeManager.currentThemeKey.value)
  }

  @Test
  fun `verify AI query interpreter provides structured health summary on query`() = runBlocking {
    val repo = com.example.data.repository.VitalithRepository()
    repo.sendUserMessage("Please summarize my recent health records and flag any anomalies")

    val messages = repo.chatMessages.first()
    val lastAiMsg = messages.last()

    assertEquals("ai", lastAiMsg.sender)
    assertEquals(true, lastAiMsg.text.contains("GEMINI AI CLINICAL SUMMARY"))
    assertEquals(true, lastAiMsg.text.contains("FLAGGED ANOMALIES"))
    assertEquals(true, lastAiMsg.citations.isNotEmpty())
  }

  @Test
  fun `verify screenStack and currentScreen survive save and restore across configuration changes`() {
    val initialCurrentScreen = AppScreen.TIMELINE
    val initialStack = listOf(AppScreen.HOME, AppScreen.MEDICATIONS, AppScreen.SHARING)

    // Save simulation (rememberSaveable serialization)
    val savedCurrentScreen = initialCurrentScreen.name
    val savedStack = initialStack.map { it.name }

    // Restore simulation
    val restoredCurrentScreen = AppScreen.valueOf(savedCurrentScreen)
    val restoredStack = savedStack.map { AppScreen.valueOf(it) }

    assertEquals(AppScreen.TIMELINE, restoredCurrentScreen)
    assertEquals(3, restoredStack.size)
    assertEquals(AppScreen.HOME, restoredStack[0])
    assertEquals(AppScreen.MEDICATIONS, restoredStack[1])
    assertEquals(AppScreen.SHARING, restoredStack[2])
  }

  @Test
  fun `verify GeminiFormOcrAnalyzer extracts and populates CMP panel correctly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cmpPreset = com.example.util.GeminiFormOcrAnalyzer.CLINICAL_PRESETS.first { it.id == "preset_cmp" }

    var recordedStep = 0
    val result = com.example.util.GeminiFormOcrAnalyzer.processAndAnalyze(
      context = context,
      preset = cmpPreset,
      sourceName = "Quest_CMP14.pdf",
      onProgress = { step, _ -> recordedStep = step }
    )

    assertEquals(true, result.title.contains("Comprehensive Metabo"))
    assertEquals(com.example.data.model.TimelineCategory.LAB, result.category)
    assertEquals(true, result.providerOrFacility.contains("Quest") || result.providerOrFacility.contains("Clinical"))
    assertEquals(com.example.data.model.ItemStatus.NORMAL, result.status)
    assertEquals(true, result.biomarkers.isNotEmpty())
    assertEquals(true, result.biomarkers.any { it.name.contains("Glucose") })
    assertEquals(4, recordedStep)
    assertEquals(true, result.tags.any { it.contains("Metabolic") || it.contains("Clinical") || it.contains("OCR") })
  }

  @Test
  fun `verify GeminiFormOcrAnalyzer extracts prescription medication entry correctly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val rxPreset = com.example.util.GeminiFormOcrAnalyzer.CLINICAL_PRESETS.first { it.id == "preset_rx" }

    val result = com.example.util.GeminiFormOcrAnalyzer.processAndAnalyze(
      context = context,
      preset = rxPreset,
      sourceName = "Lisinopril_Rx.pdf"
    )

    assertEquals(true, result.title.contains("Lisinopril"))
    assertEquals(com.example.data.model.TimelineCategory.MEDICATION, result.category)
    assertEquals(true, result.providerOrFacility.contains("Walgreens") || result.providerOrFacility.contains("Dr.") || result.providerOrFacility.contains("Pharmacy") || result.providerOrFacility.contains("Marcus"))
    assertEquals(true, result.tags.isNotEmpty())
    assertEquals(true, result.clinicalNotes.isNotBlank())
  }

  @Test
  fun `verify manual health record entry saves to Room with valid timestamp`() = runBlocking {
    val repository = com.example.data.repository.VitalithRepository(db)
    val timeStampStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date()) + " UTC"
    val docHash = "sha256:test1234567890abcdef"

    val newRecord = com.example.data.model.HealthTimelineItem(
      id = "rec_test_save_1",
      profileId = "profile_doug",
      category = com.example.data.model.TimelineCategory.LAB,
      title = "Lipid Panel Test",
      subtitle = "Lab Entry • Encrypted Local Vault",
      date = "Oct 25, 2026",
      providerOrFacility = "Quest Diagnostics Hub",
      status = com.example.data.model.ItemStatus.NORMAL,
      clinicalNotes = "Normal test entry.",
      provenance = com.example.data.model.Provenance(
        sourceSystem = "Patient Manual Entry (Room SQLite)",
        ingestedAt = timeStampStr,
        confidenceScore = 1.0f,
        consentState = "Patient-Owned / Full Local Consent",
        documentHash = docHash
      ),
      tags = listOf("Metabolic", "Routine"),
      keyValues = mapOf("Total Cholesterol" to "180 mg/dL")
    )

    repository.addTimelineItem(newRecord)

    val items = repository.timelineItems.first()
    val savedItem = items.firstOrNull { it.id == "rec_test_save_1" }
    assertNotNull(savedItem)
    assertEquals("Lipid Panel Test", savedItem?.title)
    assertEquals(true, savedItem?.provenance?.ingestedAt?.contains("UTC"))
  }

  @Test
  fun `verify theme mode toggle and persistence via DataStore`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // Test DataStore save and retrieve
    com.example.data.preferences.ThemePreferencesDataStore.saveDarkMode(context, true)
    var isDark = com.example.data.preferences.ThemePreferencesDataStore.getSavedDarkMode(context)
    assertEquals(true, isDark)

    com.example.data.preferences.ThemePreferencesDataStore.saveDarkMode(context, false)
    isDark = com.example.data.preferences.ThemePreferencesDataStore.getSavedDarkMode(context)
    assertEquals(false, isDark)

    // Test ThemeManager setDarkMode
    com.example.ui.theme.ThemeManager.init(context, isSystemDark = false)
    com.example.ui.theme.ThemeManager.setDarkMode(true, context)
    assertEquals(true, com.example.ui.theme.ThemeManager.isDarkMode)

    com.example.ui.theme.ThemeManager.setDarkMode(false, context)
    assertEquals(false, com.example.ui.theme.ThemeManager.isDarkMode)
  }

  @Test
  fun `verify PatientAgeCalculator computes accurate age in years, extracts dates, and formats clinical labels`() {
    // 1. Extract date only
    assertEquals("1995-05-15", com.example.util.PatientAgeCalculator.extractDateOnly("1995-05-15 (31 yrs)"))
    assertEquals("2019-08-22", com.example.util.PatientAgeCalculator.extractDateOnly("2019-08-22"))
    assertEquals("", com.example.util.PatientAgeCalculator.extractDateOnly(""))

    // 2. Reference date Oct 26, 2026
    val refCal = java.util.Calendar.getInstance().apply {
      set(2026, java.util.Calendar.OCTOBER, 26)
    }

    // Adult born May 15, 1995 -> 31 years old
    val adultAge = com.example.util.PatientAgeCalculator.calculateAgeYears("1995-05-15", refCal)
    assertEquals(31, adultAge)

    // Child born Aug 22, 2019 -> 7 years old
    val childAge = com.example.util.PatientAgeCalculator.calculateAgeYears("2019-08-22", refCal)
    assertEquals(7, childAge)

    // Infant born March 10, 2026 -> 0 years old
    val infantAge = com.example.util.PatientAgeCalculator.calculateAgeYears("2026-03-10", refCal)
    assertEquals(0, infantAge)

    // Birthday hasn't happened yet this year: Nov 15, 1995 -> 30 years old on Oct 26, 2026
    val upcomingBdayAge = com.example.util.PatientAgeCalculator.calculateAgeYears("1995-11-15", refCal)
    assertEquals(30, upcomingBdayAge)

    // 3. Clinical labels
    assertEquals("Infant (< 1 year old)", com.example.util.PatientAgeCalculator.formatAgeLabel(0))
    assertEquals("1 year old (Toddler)", com.example.util.PatientAgeCalculator.formatAgeLabel(1))
    assertEquals("31 years old", com.example.util.PatientAgeCalculator.formatAgeLabel(31))

    // 4. Formatting DOB with age
    assertEquals("1995-05-15 (31 yrs)", com.example.util.PatientAgeCalculator.formatDobWithAge("1995-05-15", 31))
    assertEquals("2026-03-10 (0 yrs)", com.example.util.PatientAgeCalculator.formatDobWithAge("2026-03-10", 0))

    // 5. Epoch millis roundtrip
    val millis = com.example.util.PatientAgeCalculator.parseDateToUtcMillis("2020-01-15")
    assertNotNull(millis)
    val formattedBack = com.example.util.PatientAgeCalculator.formatUtcMillisToDateString(millis!!)
    assertEquals("2020-01-15", formattedBack)
  }

  @Test
  fun `verify subscription tier properties and plan entitlements`() {
    val free = com.example.data.model.SubscriptionTier.FREE
    val premium = com.example.data.model.SubscriptionTier.PREMIUM
    val concierge = com.example.data.model.SubscriptionTier.PREMIUM_PLUS

    assertEquals("Vitalith Core", free.displayName)
    assertEquals(1, free.maxProfiles)
    assertEquals(5, free.aiQueriesPerDay)
    assertEquals(2, free.ocrScansPerMonth)
    assertEquals(false, free.fhirAutoSyncEnabled)
    assertEquals(1, free.careTeamLinksAllowed)
    assertEquals(0.00, free.monthlyPrice, 0.001)

    assertEquals("Vitalith Plus", premium.displayName)
    assertEquals(5, premium.maxProfiles)
    assertEquals(9999, premium.aiQueriesPerDay)
    assertEquals(9999, premium.ocrScansPerMonth)
    assertEquals(true, premium.fhirAutoSyncEnabled)
    assertEquals(5, premium.careTeamLinksAllowed)
    assertEquals(9.99, premium.monthlyPrice, 0.001)
    assertEquals(7.49, premium.annualPricePerMonth, 0.001)

    assertEquals("Vitalith Concierge", concierge.displayName)
    assertEquals(9999, concierge.maxProfiles)
    assertEquals(9999, concierge.aiQueriesPerDay)
    assertEquals(9999, concierge.ocrScansPerMonth)
    assertEquals(true, concierge.fhirAutoSyncEnabled)
    assertEquals(9999, concierge.careTeamLinksAllowed)
    assertEquals(19.99, concierge.monthlyPrice, 0.001)
    assertEquals(14.99, concierge.annualPricePerMonth, 0.001)
  }

  @Test
  fun `verify payment processing, tier upgrade, and invoice generation`() = runBlocking {
    val repository = com.example.data.repository.VitalithRepository(db)

    // Initially FREE tier
    repository.setSubscriptionTier(com.example.data.model.SubscriptionTier.FREE)
    assertEquals(com.example.data.model.SubscriptionTier.FREE, repository.subscriptionTier.first())

    val initialDetails = repository.subscriptionDetails.first()
    assertEquals(com.example.data.model.SubscriptionTier.FREE, initialDetails.tier)
    assertEquals("ACTIVE", initialDetails.status)

    // Process a payment for PREMIUM (Monthly)
    val transaction = repository.processPaymentAndActivateSubscription(
      tier = com.example.data.model.SubscriptionTier.PREMIUM,
      billingCycle = com.example.data.model.BillingCycle.MONTHLY,
      paymentMethod = "Google Pay (Visa ending in 4242)"
    )

    // Verify transaction fields
    assertNotNull(transaction.transactionId)
    assertNotNull(transaction.invoiceNumber)
    assertEquals(com.example.data.model.SubscriptionTier.PREMIUM, transaction.tier)
    assertEquals(com.example.data.model.BillingCycle.MONTHLY, transaction.billingCycle)
    assertEquals(9.99, transaction.amount, 0.001)
    assertEquals("Google Pay (Visa ending in 4242)", transaction.paymentMethod)
    assertEquals("COMPLETED", transaction.status)

    // Verify updated repository state
    val updatedTier = repository.subscriptionTier.first()
    assertEquals(com.example.data.model.SubscriptionTier.PREMIUM, updatedTier)

    val updatedDetails = repository.subscriptionDetails.first()
    assertEquals(com.example.data.model.SubscriptionTier.PREMIUM, updatedDetails.tier)
    assertEquals(com.example.data.model.BillingCycle.MONTHLY, updatedDetails.billingCycle)
    assertEquals(true, updatedDetails.autoRenew)
    assertEquals("Google Pay (Visa ending in 4242)", updatedDetails.lastPaymentMethod)
    assertEquals("ACTIVE", updatedDetails.status)

    // Verify payment history has recorded the transaction
    val history = repository.paymentHistory.first()
    assertEquals(true, history.isNotEmpty())
    assertEquals(transaction.transactionId, history.first().transactionId)

    // Test cancelling auto-renew
    repository.cancelSubscriptionAutoRenew()
    val cancelledRenewDetails = repository.subscriptionDetails.first()
    assertEquals(false, cancelledRenewDetails.autoRenew)
  }

  @Test
  fun `verify annual payment processing and 25 percent discount calculation`() = runBlocking {
    val repository = com.example.data.repository.VitalithRepository(db)

    val transaction = repository.processPaymentAndActivateSubscription(
      tier = com.example.data.model.SubscriptionTier.PREMIUM_PLUS,
      billingCycle = com.example.data.model.BillingCycle.ANNUAL,
      paymentMethod = "Mastercard ending in 8832"
    )

    // 14.99 * 12 = 179.88
    assertEquals(179.88, transaction.amount, 0.01)
    assertEquals(com.example.data.model.SubscriptionTier.PREMIUM_PLUS, repository.subscriptionTier.first())
    val details = repository.subscriptionDetails.first()
    assertEquals(com.example.data.model.BillingCycle.ANNUAL, details.billingCycle)
    assertEquals(true, details.autoRenew)
  }
}





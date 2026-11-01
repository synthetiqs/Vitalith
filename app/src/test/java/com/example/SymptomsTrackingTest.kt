package com.example

import com.example.data.model.SymptomItem
import com.example.data.repository.VitalithRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SymptomsTrackingTest {

  private lateinit var repository: VitalithRepository

  @Before
  fun setUp() {
    repository = VitalithRepository()
  }

  @Test
  fun testInitialSymptoms_seededAcrossProfiles() {
    val symptoms = repository.symptoms.value
    assertTrue("Symptoms should be pre-seeded", symptoms.isNotEmpty())

    val dougSymptoms = symptoms.filter { it.profileId == "profile_doug" }
    val mayaSymptoms = symptoms.filter { it.profileId == "profile_maya" }
    val kwameSymptoms = symptoms.filter { it.profileId == "profile_kwame" }

    assertTrue("Doug should have seeded symptoms", dougSymptoms.isNotEmpty())
    assertTrue("Maya should have seeded symptoms", mayaSymptoms.isNotEmpty())
    assertTrue("Kwame should have seeded symptoms", kwameSymptoms.isNotEmpty())

    assertTrue(dougSymptoms.any { it.symptomName.contains("Joint") || it.symptomName.contains("Migraine") })
    assertTrue(mayaSymptoms.any { it.symptomName.contains("Rash") })
    assertTrue(kwameSymptoms.any { it.symptomName.contains("Back") })
  }

  @Test
  fun testAddAndCloseSymptom_updatesStateCorrectly() {
    val initialCount = repository.symptoms.value.size
    val newSymptom = SymptomItem(
      id = "sym_test_101",
      profileId = "profile_doug",
      symptomName = "Acid Reflux / Heartburn",
      snomedCode = "698065",
      severity = 4,
      severityLabel = "Moderate",
      onsetDate = "Oct 29, 2026",
      onsetTimestamp = System.currentTimeMillis() - 3600000L,
      isOngoing = true,
      contextTags = listOf("Skipped Meal")
    )

    repository.addSymptom(newSymptom)
    assertEquals(initialCount + 1, repository.symptoms.value.size)
    val logged = repository.symptoms.value.first { it.id == "sym_test_101" }
    assertTrue(logged.isOngoing)

    // Close the loop
    val endTimestamp = System.currentTimeMillis()
    repository.closeSymptom("sym_test_101", endTimestamp)
    val closed = repository.symptoms.value.first { it.id == "sym_test_101" }
    assertFalse(closed.isOngoing)
    assertNotNull(closed.endedDate)
    assertNotNull(closed.endedTimestamp)

    // Delete symptom
    repository.deleteSymptom("sym_test_101")
    assertEquals(initialCount, repository.symptoms.value.size)
  }

  @Test
  fun testNaturalLanguageParsing_extractsSeverityAndSymptom() {
    val input = "headache, 6 out of 10, poor sleep and high stress"
    val (parsed, redFlag) = repository.parseNaturalLanguageSymptom(input)

    assertTrue("Parsed symptom should match headache/migraine", parsed.symptomName.contains("Headache") || parsed.symptomName.contains("Migraine"))
    assertEquals(6, parsed.severity)
    assertEquals("Moderate", parsed.severityLabel)
    assertTrue("Should extract poor sleep context tag", parsed.contextTags.any { it.contains("Sleep") })
    assertTrue("Should extract stress context tag", parsed.contextTags.any { it.contains("Stress") })
    assertNull("Moderate headache without thunderclap should not fire emergency alert", redFlag)
  }

  @Test
  fun testRedFlagEvaluator_chestPainWithDyspneaFiresEmergency() {
    // CLINICAL_REVIEW_REQUIRED
    val alert = repository.evaluateRedFlags(
      symptomName = "Chest Pain / Tightness",
      severity = 8,
      notes = "crushing chest pressure radiating to arm and short of breath"
    )

    assertNotNull("Should detect acute cardiac/respiratory red flag", alert)
    assertTrue(alert!!.isEmergency)
    assertEquals("911", alert.emergencyPhone)
    assertTrue(alert.title.contains("Cardiac") || alert.title.contains("Respiratory"))
  }

  @Test
  fun testRedFlagEvaluator_thunderclapHeadacheFiresEmergency() {
    // CLINICAL_REVIEW_REQUIRED
    val alert = repository.evaluateRedFlags(
      symptomName = "Migraine / Headache",
      severity = 10,
      notes = "sudden explosion worst thunderclap headache of my life"
    )

    assertNotNull("Thunderclap headache should fire emergency alert", alert)
    assertTrue(alert!!.isEmergency)
    assertTrue(alert.title.contains("Thunderclap"))
  }

  @Test
  fun testRedFlagEvaluator_fastStrokeSignsFiresEmergency() {
    // CLINICAL_REVIEW_REQUIRED
    val alert = repository.evaluateRedFlags(
      symptomName = "Dizziness",
      severity = 7,
      notes = "one side of facial droop and slur speech"
    )

    assertNotNull("FAST stroke signs should trigger alert", alert)
    assertTrue(alert!!.isEmergency)
    assertTrue(alert.title.contains("FAST"))
  }

  @Test
  fun testRedFlagEvaluator_benignSymptomDoesNotFire() {
    val alert = repository.evaluateRedFlags(
      symptomName = "Acid Reflux / Heartburn",
      severity = 3,
      notes = "mild heartburn after spicy tacos"
    )

    assertNull("Mild heartburn must not fire red flag", alert)
  }

  @Test
  fun testRecurrenceCalculation_calculatesStatsAndNonCausalCopy() {
    // CLINICAL_REVIEW_REQUIRED
    val stats = repository.calculateSymptomRecurrence("profile_doug", "Joint Stiffness / Ache")
    assertNotNull(stats)
    assertEquals("Joint Stiffness / Ache", stats.symptomName)
    assertTrue(stats.totalEpisodesThisMonth >= 1)
    assertTrue(stats.averageSeverity > 0f)

    if (stats.coOccurrenceObservation != null) {
      assertFalse("Copy must never state causation", stats.coOccurrenceObservation!!.contains("causes"))
      assertFalse("Copy must never state triggers", stats.coOccurrenceObservation!!.contains("triggers"))
      assertFalse("Copy must never state due to", stats.coOccurrenceObservation!!.contains("due to"))
      assertTrue("Copy must use descriptive terminology", stats.coOccurrenceObservation!!.contains("Observed") || stats.coOccurrenceObservation!!.contains("episodes"))
    }
  }

  @Test
  fun testDoctorReadyExport_containsFHIRMappingAndPatientData() {
    val report = repository.generateDoctorSymptomExportText("profile_doug")
    assertNotNull(report)
    assertTrue(report.contains("VITALITH HEALTH HISTORY • CLINICAL SYMPTOM REPORT"))
    assertTrue(report.contains("Doug Adarkwah"))
    assertTrue(report.contains("CURRENT ACTIVE MEDICATIONS"))
    assertTrue(report.contains("SNOMED-CT"))
    assertTrue(report.contains("FHIR Mapping: Observation"))
    assertTrue(report.contains("Not diagnostic"))
  }
}

package com.example

import com.example.data.model.ItemStatus
import com.example.data.model.SubscriptionTier
import com.example.data.repository.VitalithRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VitalithUnitTest {

  private lateinit var repository: VitalithRepository

  @Before
  fun setUp() {
    repository = VitalithRepository()
  }

  @Test
  fun testInitialProfiles_hasPrimaryDougAdarkwah() {
    val profiles = repository.profiles.value
    assertTrue(profiles.isNotEmpty())
    val doug = profiles.first { it.id == "profile_doug" }
    assertEquals("Doug Adarkwah", doug.name)
    assertEquals("O+", doug.bloodType)
    assertTrue(doug.isPrimary)
  }

  @Test
  fun testTimelineItems_allCarryProvenanceAndConfidence() {
    val items = repository.timelineItems.value
    assertTrue("Timeline items should not be empty", items.isNotEmpty())
    for (item in items) {
      assertNotNull("Item must have provenance", item.provenance)
      assertTrue("Source system must not be blank", item.provenance.sourceSystem.isNotBlank())
      assertTrue("Confidence must be positive", item.provenance.confidenceScore > 0f)
      assertTrue("Consent state must be specified", item.provenance.consentState.isNotBlank())
    }
  }

  @Test
  fun testGroundedAi_a1cQueryReturnsCitations() {
    repository.sendUserMessage("What was my latest A1c test?")
    val messages = repository.chatMessages.value
    val aiResponse = messages.last()

    assertEquals("ai", aiResponse.sender)
    assertTrue(aiResponse.text.contains("6.4%"))
    assertTrue(aiResponse.citations.isNotEmpty())
    assertEquals("Quest Diagnostics • Hemoglobin A1c", aiResponse.citations.first().title)
  }

  @Test
  fun testGroundedAi_emergencyRedFlagTriggersEscalation() {
    repository.sendUserMessage("I have sudden chest pain and shortness of breath")
    val messages = repository.chatMessages.value
    val aiResponse = messages.last()

    assertTrue("Red flag must be detected", aiResponse.isRedFlag)
    assertNotNull("Must include escalation instructions", aiResponse.redFlagEscalationText)
    assertTrue(aiResponse.redFlagEscalationText!!.contains("911"))
  }

  @Test
  fun testGroundedAi_ungroundedQueryRefusesSpeculation() {
    repository.sendUserMessage("What is the result of my brain MRI?")
    val messages = repository.chatMessages.value
    val aiResponse = messages.last()

    assertTrue("AI must refuse ungrounded questions", aiResponse.text.contains("could not find verified clinical data"))
    assertTrue("Ungrounded answer must not fabricate citations", aiResponse.citations.isEmpty())
  }

  @Test
  fun testCareGrant_creationAndRevocation() {
    val initialCount = repository.careGrants.value.size
    repository.createCareGrant("Dr. Emily Stone", "Neurology", "Labs Only", "7 days")

    val updatedGrants = repository.careGrants.value
    assertEquals(initialCount + 1, updatedGrants.size)
    val newGrant = updatedGrants.first()
    assertEquals("Dr. Emily Stone", newGrant.recipientName)
    assertTrue(newGrant.isActive)

    repository.revokeCareGrant(newGrant.id)
    val revokedGrant = repository.careGrants.value.first { it.id == newGrant.id }
    assertFalse("Grant must be deactivated upon revocation", revokedGrant.isActive)
  }

  @Test
  fun testMedicationReconciliation() {
    val meds = repository.medications.value
    val metformin = meds.first { it.name.contains("Metformin") }
    assertTrue(metformin.isReconciled)
    assertEquals(ItemStatus.ACTIVE, metformin.status)
  }

  @Test
  fun testSubscriptionTierUpdate() {
    repository.setSubscriptionTier(SubscriptionTier.PREMIUM_PLUS)
    assertEquals(SubscriptionTier.PREMIUM_PLUS, repository.subscriptionTier.value)
  }
}

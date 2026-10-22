package com.example

import android.content.Context
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
}

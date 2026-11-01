package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*

@Database(
  entities = [
    TimelineItemEntity::class,
    MedicationEntity::class,
    AuditLogEntity::class,
    PatientProfileEntity::class,
    MedicationReminderEntity::class,
    CareGrantEntity::class,
    PaymentTransactionEntity::class,
    AppPreferenceEntity::class,
    ChatMessageEntity::class,
    SymptomEntity::class
  ],
  version = 4,
  exportSchema = false
)
abstract class VitalithDatabase : RoomDatabase() {

  abstract fun timelineDao(): TimelineDao
  abstract fun medicationDao(): MedicationDao
  abstract fun auditDao(): AuditDao
  abstract fun patientProfileDao(): PatientProfileDao
  abstract fun reminderDao(): ReminderDao
  abstract fun careGrantDao(): CareGrantDao
  abstract fun paymentDao(): PaymentDao
  abstract fun appPreferenceDao(): AppPreferenceDao
  abstract fun chatDao(): ChatDao
  abstract fun symptomDao(): SymptomDao

  companion object {
    @Volatile
    private var INSTANCE: VitalithDatabase? = null

    fun getDatabase(context: Context): VitalithDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          VitalithDatabase::class.java,
          "vitalith_health_records.db"
        )
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}

package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AuditDao
import com.example.data.local.dao.MedicationDao
import com.example.data.local.dao.TimelineDao
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.MedicationEntity
import com.example.data.local.entities.TimelineItemEntity

@Database(
  entities = [
    TimelineItemEntity::class,
    MedicationEntity::class,
    AuditLogEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class VitalithDatabase : RoomDatabase() {

  abstract fun timelineDao(): TimelineDao
  abstract fun medicationDao(): MedicationDao
  abstract fun auditDao(): AuditDao

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

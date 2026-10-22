package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.MedicationEntity
import com.example.data.local.entities.TimelineItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {
  @Query("SELECT * FROM timeline_records ORDER BY clinicalDateMillis DESC")
  fun getAllTimelineItems(): Flow<List<TimelineItemEntity>>

  @Query("SELECT * FROM timeline_records WHERE profileId = :profileId ORDER BY clinicalDateMillis DESC")
  fun getTimelineForProfile(profileId: String): Flow<List<TimelineItemEntity>>

  @Query("SELECT * FROM timeline_records WHERE profileId = :profileId AND category = :category ORDER BY clinicalDateMillis DESC")
  fun getTimelineByCategory(profileId: String, category: String): Flow<List<TimelineItemEntity>>

  @Query("""
    SELECT * FROM timeline_records 
    WHERE profileId = :profileId 
      AND (title LIKE '%' || :query || '%' 
           OR subtitle LIKE '%' || :query || '%' 
           OR clinicalNotes LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%')
    ORDER BY clinicalDateMillis DESC
  """)
  fun searchTimeline(profileId: String, query: String): Flow<List<TimelineItemEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: TimelineItemEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<TimelineItemEntity>)

  @Query("DELETE FROM timeline_records WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("SELECT COUNT(*) FROM timeline_records")
  suspend fun getCount(): Int

  @Query("DELETE FROM timeline_records")
  suspend fun clearAll()
}

@Dao
interface MedicationDao {
  @Query("SELECT * FROM medication_records WHERE profileId = :profileId")
  fun getMedicationsForProfile(profileId: String): Flow<List<MedicationEntity>>

  @Query("SELECT * FROM medication_records")
  fun getAllMedications(): Flow<List<MedicationEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(medication: MedicationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(medications: List<MedicationEntity>)

  @Query("UPDATE medication_records SET isReconciled = :reconciled WHERE id = :id")
  suspend fun updateReconciledStatus(id: String, reconciled: Boolean)

  @Query("SELECT COUNT(*) FROM medication_records")
  suspend fun getCount(): Int
}

@Dao
interface AuditDao {
  @Query("SELECT * FROM audit_records ORDER BY timestampMillis DESC")
  fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entry: AuditLogEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(entries: List<AuditLogEntity>)

  @Query("SELECT COUNT(*) FROM audit_records")
  suspend fun getCount(): Int
}

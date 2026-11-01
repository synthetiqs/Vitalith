package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
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

  @Query("DELETE FROM timeline_records WHERE profileId = :profileId")
  suspend fun deleteByProfileId(profileId: String)

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

  @Query("DELETE FROM medication_records WHERE profileId = :profileId")
  suspend fun deleteByProfileId(profileId: String)

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

@Dao
interface PatientProfileDao {
  @Query("SELECT * FROM patient_profiles")
  fun getAllProfiles(): Flow<List<PatientProfileEntity>>

  @Query("SELECT COUNT(*) FROM patient_profiles")
  suspend fun getCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(profile: PatientProfileEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(profiles: List<PatientProfileEntity>)

  @Query("DELETE FROM patient_profiles WHERE id = :id")
  suspend fun deleteById(id: String)
}

@Dao
interface ReminderDao {
  @Query("SELECT * FROM medication_reminders")
  fun getAllReminders(): Flow<List<MedicationReminderEntity>>

  @Query("SELECT * FROM medication_reminders WHERE profileId = :profileId")
  fun getRemindersForProfile(profileId: String): Flow<List<MedicationReminderEntity>>

  @Query("SELECT COUNT(*) FROM medication_reminders")
  suspend fun getCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(reminder: MedicationReminderEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(reminders: List<MedicationReminderEntity>)

  @Query("UPDATE medication_reminders SET isEnabled = :enabled WHERE id = :id")
  suspend fun updateEnabled(id: String, enabled: Boolean)

  @Query("UPDATE medication_reminders SET isTakenToday = :taken WHERE id = :id")
  suspend fun updateTakenToday(id: String, taken: Boolean)

  @Query("DELETE FROM medication_reminders WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM medication_reminders WHERE profileId = :profileId")
  suspend fun deleteByProfileId(profileId: String)
}

@Dao
interface CareGrantDao {
  @Query("SELECT * FROM care_grants")
  fun getAllCareGrants(): Flow<List<CareGrantEntity>>

  @Query("SELECT COUNT(*) FROM care_grants")
  suspend fun getCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(grant: CareGrantEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(grants: List<CareGrantEntity>)

  @Query("UPDATE care_grants SET isActive = :active WHERE id = :id")
  suspend fun updateActiveStatus(id: String, active: Boolean)

  @Query("DELETE FROM care_grants WHERE profileId = :profileId")
  suspend fun deleteByProfileId(profileId: String)
}

@Dao
interface PaymentDao {
  @Query("SELECT * FROM payment_transactions")
  fun getAllTransactions(): Flow<List<PaymentTransactionEntity>>

  @Query("SELECT COUNT(*) FROM payment_transactions")
  suspend fun getCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(transaction: PaymentTransactionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(transactions: List<PaymentTransactionEntity>)
}

@Dao
interface AppPreferenceDao {
  @Query("SELECT prefValue FROM app_preferences WHERE prefKey = :key")
  suspend fun getPreference(key: String): String?

  @Query("SELECT * FROM app_preferences")
  fun getAllPreferences(): Flow<List<AppPreferenceEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun setPreference(entry: AppPreferenceEntity)
}

@Dao
interface ChatDao {
  @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC")
  fun getAllMessages(): Flow<List<ChatMessageEntity>>

  @Query("SELECT * FROM chat_messages WHERE profileId = :profileId ORDER BY timestampMillis ASC")
  fun getMessagesForProfile(profileId: String): Flow<List<ChatMessageEntity>>

  @Query("SELECT COUNT(*) FROM chat_messages")
  suspend fun getCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(message: ChatMessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(messages: List<ChatMessageEntity>)

  @Query("DELETE FROM chat_messages WHERE profileId = :profileId")
  suspend fun deleteByProfileId(profileId: String)
}

@Dao
interface SymptomDao {
  @Query("SELECT * FROM symptom_records ORDER BY onsetTimestamp DESC")
  fun getAllSymptoms(): Flow<List<SymptomEntity>>

  @Query("SELECT * FROM symptom_records WHERE profileId = :profileId ORDER BY onsetTimestamp DESC")
  fun getSymptomsForProfile(profileId: String): Flow<List<SymptomEntity>>

  @Query("SELECT * FROM symptom_records WHERE id = :id")
  suspend fun getById(id: String): SymptomEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(symptom: SymptomEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(symptoms: List<SymptomEntity>)

  @Query("DELETE FROM symptom_records WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM symptom_records WHERE profileId = :profileId")
  suspend fun deleteByProfileId(profileId: String)

  @Query("SELECT COUNT(*) FROM symptom_records")
  suspend fun getCount(): Int

  @Query("DELETE FROM symptom_records")
  suspend fun clearAll()
}


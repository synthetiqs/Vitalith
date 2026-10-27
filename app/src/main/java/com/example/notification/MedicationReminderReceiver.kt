package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.data.model.MedicationReminder

class MedicationReminderReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return

    when (action) {
      MedicationNotificationHelper.ACTION_MEDICATION_REMINDER -> {
        val reminderId = intent.getStringExtra(MedicationNotificationHelper.EXTRA_REMINDER_ID) ?: "med_rem_default"
        val medName = intent.getStringExtra(MedicationNotificationHelper.EXTRA_MED_NAME) ?: "Prescribed Treatment"
        val dosage = intent.getStringExtra(MedicationNotificationHelper.EXTRA_DOSAGE) ?: "Prescribed Dose"
        val instructions = intent.getStringExtra(MedicationNotificationHelper.EXTRA_INSTRUCTIONS) ?: "Take with water"

        val reminder = MedicationReminder(
          id = reminderId,
          medicationId = reminderId,
          medicationName = medName,
          dosage = dosage,
          hour = 8,
          minute = 0,
          instructions = instructions
        )

        // Show the high-priority reminder notification
        MedicationNotificationHelper.showMedicationNotification(context, reminder)

        // Reschedule for next day (recurring daily alert)
        MedicationNotificationHelper.scheduleDailyAlarm(context, reminder)
      }

      MedicationNotificationHelper.ACTION_MARK_MEDICATION_TAKEN -> {
        val reminderId = intent.getStringExtra(MedicationNotificationHelper.EXTRA_REMINDER_ID) ?: return
        val medName = intent.getStringExtra(MedicationNotificationHelper.EXTRA_MED_NAME) ?: "Medication"

        // Cancel notification
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(reminderId.hashCode())

        Toast.makeText(context, "Logged: $medName marked as taken!", Toast.LENGTH_SHORT).show()
      }

      Intent.ACTION_BOOT_COMPLETED -> {
        // Device rebooted: AlarmManager alarms are cleared by system, but notifications can be re-established
      }
    }
  }
}

package com.example.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.MedicationReminder
import java.util.Calendar

object MedicationNotificationHelper {

  const val CHANNEL_ID = "vitalith_medication_reminders"
  const val CHANNEL_NAME = "Prescribed Medication Reminders"
  const val CHANNEL_DESC = "High-priority scheduled reminders for prescription medications"

  const val ACTION_MEDICATION_REMINDER = "com.example.ACTION_MEDICATION_REMINDER"
  const val ACTION_MARK_MEDICATION_TAKEN = "com.example.ACTION_MARK_MEDICATION_TAKEN"

  const val EXTRA_REMINDER_ID = "extra_reminder_id"
  const val EXTRA_MED_NAME = "extra_med_name"
  const val EXTRA_DOSAGE = "extra_dosage"
  const val EXTRA_INSTRUCTIONS = "extra_instructions"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
        description = CHANNEL_DESC
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 300, 200, 300)
        setShowBadge(true)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
      }
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      manager?.createNotificationChannel(channel)
    }
  }

  @SuppressLint("MissingPermission")
  fun showMedicationNotification(context: Context, reminder: MedicationReminder) {
    createNotificationChannel(context)

    val notificationId = reminder.id.hashCode()

    // Intent to open app
    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openPendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Intent to mark as taken
    val takenIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
      action = ACTION_MARK_MEDICATION_TAKEN
      putExtra(EXTRA_REMINDER_ID, reminder.id)
      putExtra(EXTRA_MED_NAME, reminder.medicationName)
    }
    val takenPendingIntent = PendingIntent.getBroadcast(
      context,
      notificationId + 1000,
      takenIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("Prescription Due: ${reminder.medicationName} (${reminder.dosage})")
      .setContentText("Scheduled at ${reminder.formattedTime} • ${reminder.instructions}")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText(
            "Time for your prescribed treatment: ${reminder.medicationName} ${reminder.dosage}.\n\n" +
              "Instructions: ${reminder.instructions}\n" +
              "Schedule: Recurring Daily (${reminder.formattedTime})"
          )
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setAutoCancel(true)
      .setContentIntent(openPendingIntent)
      .addAction(
        R.drawable.ic_launcher_foreground,
        "Mark as Taken",
        takenPendingIntent
      )

    val manager = NotificationManagerCompat.from(context)
    try {
      manager.notify(notificationId, notificationBuilder.build())
    } catch (_: SecurityException) {
      // Catch if runtime permission is disabled
    }
  }

  fun scheduleDailyAlarm(context: Context, reminder: MedicationReminder) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
      action = ACTION_MEDICATION_REMINDER
      putExtra(EXTRA_REMINDER_ID, reminder.id)
      putExtra(EXTRA_MED_NAME, reminder.medicationName)
      putExtra(EXTRA_DOSAGE, reminder.dosage)
      putExtra(EXTRA_INSTRUCTIONS, reminder.instructions)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      reminder.id.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
      timeInMillis = System.currentTimeMillis()
      set(Calendar.HOUR_OF_DAY, reminder.hour)
      set(Calendar.MINUTE, reminder.minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)

      // If already passed today, set for tomorrow
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent
        )
      } else {
        alarmManager.set(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent
        )
      }
    } catch (_: SecurityException) {
      // In case exact alarm permission is restricted
      alarmManager.set(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        pendingIntent
      )
    }
  }

  fun cancelAlarm(context: Context, reminderId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
      action = ACTION_MEDICATION_REMINDER
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      reminderId.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
  }
}

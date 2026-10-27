package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.data.model.ItemStatus
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationReminder
import com.example.data.model.Provenance
import com.example.data.repository.VitalithRepository
import com.example.notification.MedicationNotificationHelper
import com.example.ui.components.MedicalTermExplainerModal
import com.example.ui.components.ProvenanceBadge
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
  repository: VitalithRepository,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val medications by repository.medications.collectAsState()
  val reminders by repository.reminders.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Prescriptions, 1: Daily Alerts

  var showAddMedDialog by remember { mutableStateOf(false) }
  var showAddReminderDialog by remember { mutableStateOf(false) }
  var selectedMedForDetail by remember { mutableStateOf<MedicationItem?>(null) }
  var prefilledMedForReminder by remember { mutableStateOf<MedicationItem?>(null) }
  var selectedTermForAiExplainer by remember { mutableStateOf<Pair<String, String>?>(null) }

  // Notification Permission State
  var hasNotificationPermission by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
      } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
      }
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasNotificationPermission = isGranted
    if (isGranted) {
      Toast.makeText(context, "Notification permission enabled! Daily alerts will sound at your scheduled dose times.", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Notifications disabled. Alarms will still be recorded in your dashboard.", Toast.LENGTH_SHORT).show()
    }
  }

  val activeMeds = medications.filter { it.status == ItemStatus.ACTIVE }
  val discontinuedMeds = medications.filter { it.status == ItemStatus.DISCONTINUED }

  val enabledRemindersCount = reminders.count { it.isEnabled }
  val takenTodayCount = reminders.count { it.isTakenToday }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Header
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Medications & Reminders",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Prescription reconciliation & recurring daily alerts",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              IconButton(
                onClick = {
                  if (selectedTab == 0) {
                    showAddMedDialog = true
                  } else {
                    prefilledMedForReminder = activeMeds.firstOrNull()
                    showAddReminderDialog = true
                  }
                },
                modifier = Modifier.testTag("add_header_button")
              ) {
                Icon(
                  imageVector = Icons.Default.AddCircle,
                  contentDescription = if (selectedTab == 0) "Add Prescription" else "Add Reminder",
                  tint = VitalTealPrimary,
                  modifier = Modifier.size(28.dp)
                )
              }
            }
          }

          // Tabs: Prescriptions vs Daily Alerts
          TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = VitalTealPrimary,
            modifier = Modifier.testTag("medication_tabs")
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(16.dp))
                  Text("Prescriptions (${activeMeds.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              },
              modifier = Modifier.testTag("tab_prescriptions")
            )

            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                  Text("Daily Alerts ($enabledRemindersCount)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              },
              modifier = Modifier.testTag("tab_reminders")
            )
          }
        }
      }

      // Tab Content
      if (selectedTab == 0) {
        // --- PRESCRIPTIONS TAB ---
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
          contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Reconciliation Status Banner
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = VitalEmeraldContainer.copy(alpha = 0.5f)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = VitalEmeraldSuccess)
                Column {
                  Text(
                    text = "All Active Regimens Reconciled",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = VitalOnEmeraldContainer
                  )
                  Text(
                    text = "Cross-referenced against Epic & Quest FHIR endpoints.",
                    fontSize = 11.sp,
                    color = VitalOnEmeraldContainer
                  )
                }
              }
            }
          }

          // Clinical Interaction Banner
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = VitalWarningContainer.copy(alpha = 0.4f)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, VitalWarningOrange.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = VitalWarningOrange)
                Column {
                  Text(
                    text = "Clinical Interaction Watch (Atorvastatin + Metformin)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF7C2D12)
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "Routine 6-month hepatic (LFT) panel recommended when co-administering statins with metformin.",
                    fontSize = 11.sp,
                    color = Color(0xFF7C2D12),
                    lineHeight = 15.sp
                  )
                }
              }
            }
          }

          // Active Meds Section
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Active Prescriptions (${activeMeds.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              TextButton(
                onClick = { selectedTab = 1 }
              ) {
                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("View Alerts", fontSize = 12.sp, color = VitalTealPrimary)
              }
            }
          }

          items(activeMeds, key = { it.id }) { med ->
            MedicationCard(
              med = med,
              onReconcile = { repository.reconcileMedication(med.id) },
              onClick = { selectedMedForDetail = med },
              onSetAlertClick = {
                prefilledMedForReminder = med
                showAddReminderDialog = true
              }
            )
          }

          // Discontinued / Historical Meds
          if (discontinuedMeds.isNotEmpty()) {
            item {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Discontinued / Contraindicated (${discontinuedMeds.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            items(discontinuedMeds, key = { it.id }) { med ->
              MedicationCard(
                med = med,
                onReconcile = {},
                onClick = { selectedMedForDetail = med }
              )
            }
          }
        }
      } else {
        // --- DAILY ALERTS & REMINDERS TAB ---
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
          contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Notification Permission Banner
          if (!hasNotificationPermission) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = VitalAlertContainer.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VitalAlertRed.copy(alpha = 0.5f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("notification_permission_banner")
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = VitalAlertRed)
                    Text(
                      text = "Notification Permission Needed",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.5.sp,
                      color = VitalOnAlertContainer
                    )
                  }
                  Text(
                    text = "To receive audible alarms, vibration, and lock-screen reminders when treatments are due, enable Android notification permissions.",
                    fontSize = 11.5.sp,
                    color = VitalOnAlertContainer,
                    lineHeight = 16.sp
                  )
                  Button(
                    onClick = {
                      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                      } else {
                        Toast.makeText(context, "System notifications are active", Toast.LENGTH_SHORT).show()
                        hasNotificationPermission = true
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VitalAlertRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("grant_notification_permission_button")
                  ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enable Notification Alerts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          } else {
            item {
              Surface(
                color = VitalEmeraldContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VitalEmeraldSuccess.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(20.dp))
                  Column {
                    Text(
                      text = "Android Notification Reminders Active",
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.5.sp,
                      color = VitalOnEmeraldContainer
                    )
                    Text(
                      text = "High-priority alarms with vibration and action buttons will sound at scheduled dose times.",
                      fontSize = 11.sp,
                      color = VitalOnEmeraldContainer
                    )
                  }
                }
              }
            }
          }

          // Daily Adherence Card
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(14.dp),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = VitalTealPrimary)
                    Text(text = "Today's Adherence", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                  }
                  Text(
                    text = "$takenTodayCount of ${reminders.size} doses logged",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = VitalTealPrimary
                  )
                }

                val progress = if (reminders.isNotEmpty()) takenTodayCount.toFloat() / reminders.size else 0f
                LinearProgressIndicator(
                  progress = { progress },
                  color = VitalTealPrimary,
                  trackColor = MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                )
              }
            }
          }

          // Alerts List Header
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Recurring Daily Treatment Alerts (${reminders.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )

              Button(
                onClick = {
                  prefilledMedForReminder = activeMeds.firstOrNull()
                  showAddReminderDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_reminder_button")
              ) {
                Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          if (reminders.isEmpty()) {
            item {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 24.dp)
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.AlarmOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                  Text("No recurring reminders scheduled", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                  Text("Set daily alerts so you never miss a prescribed dose.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          } else {
            items(reminders, key = { it.id }) { reminder ->
              ReminderCard(
                reminder = reminder,
                onToggle = { isEnabled ->
                  repository.toggleMedicationReminder(reminder.id, isEnabled)
                  if (isEnabled) {
                    MedicationNotificationHelper.scheduleDailyAlarm(context, reminder)
                    Toast.makeText(context, "Alarm activated for ${reminder.formattedTime}", Toast.LENGTH_SHORT).show()
                  } else {
                    MedicationNotificationHelper.cancelAlarm(context, reminder.id)
                    Toast.makeText(context, "Alarm paused", Toast.LENGTH_SHORT).show()
                  }
                },
                onToggleTaken = { isTaken ->
                  repository.markReminderTaken(reminder.id, isTaken)
                  Toast.makeText(context, if (isTaken) "Dose marked as taken!" else "Dose unmarked", Toast.LENGTH_SHORT).show()
                },
                onTestAlert = {
                  MedicationNotificationHelper.showMedicationNotification(context, reminder)
                  Toast.makeText(context, "Alert dispatched! Check notification shade.", Toast.LENGTH_SHORT).show()
                },
                onDelete = {
                  MedicationNotificationHelper.cancelAlarm(context, reminder.id)
                  repository.deleteMedicationReminder(reminder.id)
                  Toast.makeText(context, "Reminder removed", Toast.LENGTH_SHORT).show()
                }
              )
            }
          }
        }
      }
    }
  }

  // Dialog: Create Recurring Daily Reminder
  if (showAddReminderDialog) {
    AddMedicationReminderDialog(
      activeMeds = activeMeds,
      initialMed = prefilledMedForReminder,
      onDismiss = {
        showAddReminderDialog = false
        prefilledMedForReminder = null
      },
      onSave = { newReminder ->
        repository.addMedicationReminder(newReminder)
        MedicationNotificationHelper.scheduleDailyAlarm(context, newReminder)
        Toast.makeText(context, "Daily alert scheduled for ${newReminder.formattedTime}!", Toast.LENGTH_SHORT).show()
        showAddReminderDialog = false
        prefilledMedForReminder = null
      }
    )
  }

  // Dialog: Add Medication
  if (showAddMedDialog) {
    AddMedicationDialog(
      onDismiss = { showAddMedDialog = false },
      onSave = { med ->
        repository.addMedication(med)
        showAddMedDialog = false
      }
    )
  }

  // Bottom Sheet: Medication Detail
  selectedMedForDetail?.let { med ->
    ModalBottomSheet(
      onDismissRequest = { selectedMedForDetail = null },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(text = med.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "${med.dosage} • ${med.frequency}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          StatusBadge(status = med.status)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        DetailRow(label = "Indication", value = med.indication)
        DetailRow(label = "Prescribing Physician", value = med.prescriber)
        DetailRow(label = "Start Date", value = med.startDate)
        DetailRow(label = "Reconciliation Status", value = if (med.isReconciled) "Verified & Reconciled" else "Pending Review")

        // Gemini AI Medical Term Explainer for this medication
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = VitalTealContainer.copy(alpha = 0.35f),
          border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimary.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth().testTag("med_terms_ai_section")
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(16.dp))
              Text(
                text = "Tap Medical Term for Gemini AI Explanation",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VitalOnTealContainer
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              SuggestionChip(
                onClick = { selectedTermForAiExplainer = Pair(med.name, "Prescribed: ${med.name} ${med.dosage}, Indication: ${med.indication}") },
                label = { Text("✨ ${med.name}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = VitalTealPrimary) }
              )
              SuggestionChip(
                onClick = { selectedTermForAiExplainer = Pair(med.indication, "Prescribed for ${med.name} (${med.dosage})") },
                label = { Text("✨ ${med.indication.split(" ").first()}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = VitalTealPrimary) }
              )
            }
          }
        }

        // Action to schedule reminder directly from detail
        Button(
          onClick = {
            prefilledMedForReminder = med
            selectedMedForDetail = null
            selectedTab = 1
            showAddReminderDialog = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.AlarmAdd, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Set Recurring Daily Alert for ${med.name}", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  // Gemini AI Medical Term Explainer Modal
  selectedTermForAiExplainer?.let { (term, ctx) ->
    MedicalTermExplainerModal(
      term = term,
      clinicalContext = ctx,
      onDismiss = { selectedTermForAiExplainer = null }
    )
  }
}

@Composable
fun ReminderCard(
  reminder: MedicationReminder,
  onToggle: (Boolean) -> Unit,
  onToggleTaken: (Boolean) -> Unit,
  onTestAlert: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (reminder.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ),
    shape = RoundedCornerShape(14.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = if (reminder.isEnabled) 2.dp else 0.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (reminder.isEnabled) VitalTealPrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("reminder_card_${reminder.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Time Badge
          Surface(
            color = if (reminder.isEnabled) VitalTealContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = reminder.formattedTime,
              color = if (reminder.isEnabled) VitalOnTealContainer else MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 15.sp,
              fontWeight = FontWeight.ExtraBold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }

          Column {
            Text(
              text = reminder.medicationName,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = if (reminder.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "${reminder.dosage} • ${reminder.recurrenceLabel}",
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Active / Paused Switch
        Switch(
          checked = reminder.isEnabled,
          onCheckedChange = onToggle,
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = VitalTealPrimary
          ),
          modifier = Modifier.testTag("reminder_switch_${reminder.id}")
        )
      }

      // Instructions
      if (reminder.instructions.isNotBlank()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.Info, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(15.dp))
          Text(
            text = reminder.instructions,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 15.sp
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

      // Bottom Row: Mark Taken Checkbox + Test Alert Button + Delete
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { onToggleTaken(!reminder.isTakenToday) }
        ) {
          Checkbox(
            checked = reminder.isTakenToday,
            onCheckedChange = onToggleTaken,
            colors = CheckboxDefaults.colors(checkedColor = VitalEmeraldSuccess)
          )
          Text(
            text = if (reminder.isTakenToday) "Taken Today" else "Mark as Taken",
            fontSize = 12.sp,
            fontWeight = if (reminder.isTakenToday) FontWeight.Bold else FontWeight.Medium,
            color = if (reminder.isTakenToday) VitalEmeraldSuccess else MaterialTheme.colorScheme.onSurface
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          // Test Alert Button (fires immediate notification)
          OutlinedButton(
            onClick = onTestAlert,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.testTag("test_alert_${reminder.id}")
          ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Test Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = VitalAlertRed, modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationReminderDialog(
  activeMeds: List<MedicationItem>,
  initialMed: MedicationItem?,
  onDismiss: () -> Unit,
  onSave: (MedicationReminder) -> Unit
) {
  var selectedMed by remember { mutableStateOf(initialMed ?: activeMeds.firstOrNull()) }
  var selectedHour by remember { mutableIntStateOf(8) }
  var selectedMinute by remember { mutableIntStateOf(0) }
  var isAm by remember { mutableStateOf(true) }
  var instructions by remember { mutableStateOf(selectedMed?.let { "Take ${it.name} with water" } ?: "Take with breakfast") }
  var isDaily by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Alarm, contentDescription = null, tint = VitalTealPrimary)
        Text("Schedule Recurring Daily Alert", fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Medication selection
        Text(text = "Prescribed Treatment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          activeMeds.forEach { med ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (selectedMed?.id == med.id) VitalTealContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (selectedMed?.id == med.id) VitalTealPrimary else Color.Transparent
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedMed = med
                  instructions = "Take ${med.name} (${med.dosage}) with water"
                }
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(text = med.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text(text = "${med.dosage} • ${med.frequency}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (selectedMed?.id == med.id) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }

        // Time Selection
        Text(text = "Alert Time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Hour Picker
          OutlinedTextField(
            value = if (selectedHour == 0) "12" else selectedHour.toString(),
            onValueChange = { str ->
              str.toIntOrNull()?.let {
                if (it in 1..12) selectedHour = it
              }
            },
            label = { Text("Hour") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )

          Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)

          // Minute Picker
          OutlinedTextField(
            value = if (selectedMinute < 10) "0$selectedMinute" else selectedMinute.toString(),
            onValueChange = { str ->
              str.toIntOrNull()?.let {
                if (it in 0..59) selectedMinute = it
              }
            },
            label = { Text("Minute") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )

          // AM / PM Toggle
          Row {
            FilterChip(
              selected = isAm,
              onClick = { isAm = true },
              label = { Text("AM", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = VitalTealPrimary,
                selectedLabelColor = Color.White
              )
            )
            Spacer(modifier = Modifier.width(4.dp))
            FilterChip(
              selected = !isAm,
              onClick = { isAm = false },
              label = { Text("PM", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = VitalTealPrimary,
                selectedLabelColor = Color.White
              )
            )
          }
        }

        // Recurrence Mode
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Recurrence Schedule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          FilterChip(
            selected = isDaily,
            onClick = { isDaily = true },
            label = { Text("Every Day (Daily)", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = VitalTealPrimary,
              selectedLabelColor = Color.White
            )
          )
        }

        // Special Instructions
        OutlinedTextField(
          value = instructions,
          onValueChange = { instructions = it },
          label = { Text("Dosing Instructions") },
          placeholder = { Text("e.g. Take with breakfast & full glass of water") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val med = selectedMed ?: return@Button
          // Convert 12h to 24h
          val h24 = if (isAm) {
            if (selectedHour == 12) 0 else selectedHour
          } else {
            if (selectedHour == 12) 12 else selectedHour + 12
          }

          val newReminder = MedicationReminder(
            id = "rem_${UUID.randomUUID().toString().take(8)}",
            profileId = med.profileId,
            medicationId = med.id,
            medicationName = med.name,
            dosage = med.dosage,
            hour = h24,
            minute = selectedMinute,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            isEnabled = true,
            instructions = instructions.trim().ifEmpty { "Take with water" },
            isTakenToday = false
          )
          onSave(newReminder)
        },
        colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("confirm_schedule_reminder_button")
      ) {
        Text("Schedule Alert", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun MedicationCard(
  med: MedicationItem,
  onReconcile: () -> Unit,
  onClick: () -> Unit,
  onSetAlertClick: (() -> Unit)? = null
) {
  val isDiscontinued = med.status == ItemStatus.DISCONTINUED

  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (isDiscontinued) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    ),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .testTag("medication_card_${med.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${med.name} ${med.dosage}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDiscontinued) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = med.frequency,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          StatusBadge(status = med.status)
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "View Details",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "For: ${med.indication}",
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Rx: ${med.prescriber}",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      med.conflictWarning?.let { warning ->
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = if (isDiscontinued) VitalAlertContainer else VitalWarningContainer.copy(alpha = 0.5f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = if (isDiscontinued) Icons.Default.Block else Icons.Default.Info,
              contentDescription = null,
              tint = if (isDiscontinued) VitalAlertRed else VitalWarningOrange,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = warning,
              fontSize = 11.sp,
              color = if (isDiscontinued) VitalOnAlertContainer else Color(0xFF7C2D12),
              lineHeight = 14.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        ProvenanceBadge(provenance = med.provenance)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (onSetAlertClick != null && !isDiscontinued) {
            OutlinedButton(
              onClick = onSetAlertClick,
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              shape = RoundedCornerShape(6.dp)
            ) {
              Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(14.dp), tint = VitalTealPrimary)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Set Alert", fontSize = 11.sp, color = VitalTealPrimary, fontWeight = FontWeight.Bold)
            }
          }

          if (!med.isReconciled && !isDiscontinued) {
            TextButton(onClick = onReconcile) {
              Text("Verify / Reconcile", fontSize = 11.sp, color = VitalTealPrimary)
            }
          }
        }
      }
    }
  }
}


@Composable
fun DetailRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
fun AddMedicationDialog(
  onDismiss: () -> Unit,
  onSave: (MedicationItem) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var dosage by remember { mutableStateOf("") }
  var frequency by remember { mutableStateOf("") }
  var prescriber by remember { mutableStateOf("") }
  var indication by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Prescribed Medication", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Medication Name *") },
          placeholder = { Text("e.g. Metformin") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = dosage,
          onValueChange = { dosage = it },
          label = { Text("Dosage *") },
          placeholder = { Text("e.g. 500 mg") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = frequency,
          onValueChange = { frequency = it },
          label = { Text("Frequency *") },
          placeholder = { Text("e.g. Twice Daily with Meals") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = indication,
          onValueChange = { indication = it },
          label = { Text("Indication / Condition") },
          placeholder = { Text("e.g. Blood Sugar / Pre-diabetes") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = prescriber,
          onValueChange = { prescriber = it },
          label = { Text("Prescriber / Clinic") },
          placeholder = { Text("e.g. Dr. Sarah Chen, MD") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank() && dosage.isNotBlank()) {
            val newMed = MedicationItem(
              id = "med_${UUID.randomUUID().toString().take(8)}",
              profileId = "profile_doug",
              name = name.trim(),
              dosage = dosage.trim(),
              frequency = frequency.trim().ifEmpty { "Daily" },
              prescriber = prescriber.trim().ifEmpty { "Patient Self-Reported" },
              startDate = "Oct 2026",
              status = ItemStatus.ACTIVE,
              indication = indication.trim().ifEmpty { "Prescribed Treatment" },
              isReconciled = false,
              provenance = Provenance(
                sourceSystem = "Patient Manual Entry",
                ingestedAt = "2026-10-23",
                confidenceScore = 0.95f,
                consentState = "Patient Owned"
              )
            )
            onSave(newMed)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
      ) {
        Text("Add to List")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

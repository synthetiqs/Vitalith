package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.ItemStatus
import com.example.data.model.TimelineCategory
import com.example.data.repository.VitalithRepository
import com.example.ui.components.PatientProfileDropdownSelector
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmergencySnapshotScreen(
  repository: VitalithRepository,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val profiles by repository.profiles.collectAsState()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val currentProfile = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.first()
  val medications by repository.medications.collectAsState()
  val timelineItems by repository.timelineItems.collectAsState()
  val userProfile by repository.userProfile.collectAsState()

  val activeMedications = remember(medications, currentProfileId) {
    medications.filter { it.profileId == currentProfile.id && it.status == ItemStatus.ACTIVE }
  }

  val profileConditions = remember(timelineItems, currentProfileId) {
    timelineItems.filter { it.profileId == currentProfile.id && it.category == TimelineCategory.CONDITION }
  }

  val contactRaw = if (currentProfile.emergencyContact.isNotBlank()) {
    currentProfile.emergencyContact
  } else {
    "${userProfile.emergencyContactName} • ${userProfile.emergencyContactPhone}"
  }
  val phoneRegex = Regex("""\+?1?[-.\s]?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}""")
  val parsedPhone = phoneRegex.find(contactRaw)?.value ?: userProfile.emergencyContactPhone

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0F172A)) // High-contrast dark emergency theme
  ) {
    // Emergency Top Bar
    Surface(
      color = Color(0xFF1E293B),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          IconButton(onClick = onNavigateBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
          Column(modifier = Modifier.weight(1f)) {
            FlowRow(
              verticalArrangement = Arrangement.spacedBy(2.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "EMERGENCY MEDICAL ID",
                color = VitalAlertRed,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold
              )
              Surface(
                color = VitalAlertRed,
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = "FIRST RESPONDERS",
                  color = Color.White,
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "Lock-screen accessible patient snapshot",
              color = Color(0xFF94A3B8),
              fontSize = 10.5.sp
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Patient Profile Switcher for Emergency Responders
        PatientProfileDropdownSelector(
          profiles = profiles,
          currentProfileId = currentProfileId,
          onSwitchProfile = { repository.switchProfile(it) },
          testTagPrefix = "emergency_profile_selector"
        )
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Patient & Blood Group Hero
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          shape = RoundedCornerShape(14.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = currentProfile.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
              Text(text = "DOB: ${currentProfile.dob} • MRN: ${currentProfile.mrn}", color = Color(0xFF94A3B8), fontSize = 12.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Text(text = "Organ Donor: YES", color = VitalEmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Surface(
              color = VitalAlertRed,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.size(64.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(text = "BLOOD", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  Text(text = currentProfile.bloodType, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
              }
            }
          }
        }
      }

      // Critical Allergies (Highest Priority for ER)
      item {
        val allergies = currentProfile.allergies
        Card(
          colors = CardDefaults.cardColors(containerColor = if (allergies.isNotEmpty()) Color(0xFF450A0A) else Color(0xFF1E293B)),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, if (allergies.isNotEmpty()) VitalAlertRed else Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth().testTag("emergency_allergies_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(
                imageVector = if (allergies.isNotEmpty()) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (allergies.isNotEmpty()) VitalAlertRed else VitalEmeraldSuccess
              )
              Text(
                text = if (allergies.isNotEmpty()) "ALLERGIES & CONTRAINDICATIONS" else "DOCUMENTED ALLERGIES",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (allergies.isEmpty()) {
              Text(
                text = "No Known Drug or Food Allergies Documented",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
              )
            } else {
              allergies.forEach { allergy ->
                val isSevere = allergy.contains("Severe", ignoreCase = true) || allergy.contains("Anaphylaxis", ignoreCase = true) || allergy.contains("Penicillin", ignoreCase = true)
                Surface(
                  color = if (isSevere) Color(0xFF7F1D1D) else Color(0xFF334155),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                      text = "• ${allergy.uppercase()}",
                      color = Color.White,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = if (isSevere) "Contraindication: Clinical hypersensitivity. Review before administering." else "Documented patient allergy.",
                      color = if (isSevere) Color(0xFFFECACA) else Color(0xFF94A3B8),
                      fontSize = 11.sp
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Active Medications
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth().testTag("emergency_medications_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(imageVector = Icons.Default.Medication, contentDescription = null, tint = Color(0xFF38BDF8))
              Text(text = "ACTIVE MEDICATIONS (${activeMedications.size})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (activeMedications.isEmpty()) {
              Text(text = "No active prescription regimens on file", color = Color(0xFF94A3B8), fontSize = 12.sp)
            } else {
              activeMedications.forEachIndexed { idx, med ->
                Text(
                  text = "${idx + 1}. ${med.name} ${med.dosage} — ${med.frequency} (Indication: ${med.indication})",
                  color = Color(0xFFE2E8F0),
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(vertical = 2.dp)
                )
              }
            }
          }
        }
      }

      // Pre-existing Conditions
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth().testTag("emergency_conditions_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "KNOWN MEDICAL CONDITIONS (${profileConditions.size})", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            if (profileConditions.isEmpty()) {
              Text(text = "No chronic conditions documented", color = Color(0xFF94A3B8), fontSize = 12.sp)
            } else {
              profileConditions.forEach { cond ->
                Text(
                  text = "• ${cond.title}: ${cond.subtitle}",
                  color = Color(0xFFE2E8F0),
                  fontSize = 12.sp,
                  modifier = Modifier.padding(vertical = 2.dp)
                )
              }
            }
          }
        }
      }

      // Emergency Contacts
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth().testTag("emergency_contacts_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "EMERGENCY CONTACTS", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(text = "Contact: $contactRaw", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = "$parsedPhone • Emergency Notification", color = Color(0xFF38BDF8), fontSize = 12.sp)
              }
              Spacer(modifier = Modifier.width(8.dp))
              FilledTonalButton(
                onClick = {
                  try {
                    val cleanPhone = parsedPhone.filter { it.isDigit() || it == '+' }
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                    context.startActivity(dialIntent)
                  } catch (e: Exception) {
                    Log.e("EmergencySnapshot", "Unable to launch dialer: ${e.message}")
                  }
                },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF0369A1)),
                modifier = Modifier.testTag("emergency_call_button")
              ) {
                Icon(imageVector = Icons.Default.Phone, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Call", color = Color.White, fontSize = 12.sp)
              }
            }
          }
        }
      }

      // First Responder QR Code Simulation
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(120.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(8.dp),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = "Emergency QR Token",
                tint = Color.Black,
                modifier = Modifier.size(100.dp)
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Scan for Verified Emergency FHIR Bundle",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Token: vtl_emg_qr_${currentProfile.id.takeLast(6)} • Read-Only No Login Required",
              color = Color(0xFF94A3B8),
              fontSize = 10.sp,
              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
          }
        }
      }

      // Emergency Medical ID Lock Screen Visibility Toggle
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Lock Screen Visibility",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (userProfile.emergencyMedicalIdUnlocked) {
                  "Active • Visible to first responders from lockscreen without unlocking device"
                } else {
                  "Disabled • Emergency Medical ID hidden until device is unlocked"
                },
                color = if (userProfile.emergencyMedicalIdUnlocked) VitalEmeraldSuccess else Color(0xFF94A3B8),
                fontSize = 11.sp
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
              checked = userProfile.emergencyMedicalIdUnlocked,
              onCheckedChange = { repository.setEmergencyMedicalIdUnlocked(it) },
              modifier = Modifier.testTag("emergency_screen_lockscreen_toggle"),
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VitalAlertRed,
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF334155)
              )
            )
          }
        }
      }
    }
  }
}


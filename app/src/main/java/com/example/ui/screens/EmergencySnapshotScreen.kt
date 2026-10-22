package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.VitalithRepository
import com.example.ui.theme.*

@Composable
fun EmergencySnapshotScreen(
  repository: VitalithRepository,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val profiles by repository.profiles.collectAsState()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val currentProfile = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.first()

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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          IconButton(onClick = onNavigateBack) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "EMERGENCY MEDICAL ID",
                color = VitalAlertRed,
                fontSize = 14.sp,
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
              fontSize = 11.sp
            )
          }
        }
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
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, VitalAlertRed),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = VitalAlertRed)
              Text(text = "SEVERE ALLERGIES (CONTRAINDICATION)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              color = Color(0xFF7F1D1D),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(text = "• PENICILLIN / AMOXICILLIN", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "Reaction: Anaphylactoid, severe hives, airway compromise. Do not administer beta-lactam class.", color = Color(0xFFFECACA), fontSize = 11.sp)
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              color = Color(0xFF7F1D1D),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(text = "• SHELLFISH (MILD)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Reaction: Oral pruritus / localized rash.", color = Color(0xFFFECACA), fontSize = 11.sp)
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
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(imageVector = Icons.Default.Medication, contentDescription = null, tint = Color(0xFF38BDF8))
              Text(text = "ACTIVE MEDICATIONS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "1. Metformin HCl 500mg — Oral BID with meals", color = Color(0xFFE2E8F0), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = "2. Atorvastatin 10mg — Oral Daily at bedtime", color = Color(0xFFE2E8F0), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = "3. Vitamin D3 2000 IU — Oral Daily", color = Color(0xFF94A3B8), fontSize = 12.sp)
          }
        }
      }

      // Pre-existing Conditions
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "KNOWN MEDICAL CONDITIONS", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "• Pre-Diabetes (ICD-10 R73.09) — Diet & Metformin", color = Color(0xFFE2E8F0), fontSize = 12.sp)
            Text(text = "• Mild Hyperlipidemia (ICD-10 E78.5) — Statin controlled", color = Color(0xFFE2E8F0), fontSize = 12.sp)
          }
        }
      }

      // Emergency Contacts
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "EMERGENCY CONTACTS", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(text = "Primary Contact: Grace Adarkwah (Spouse)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = "+1 (555) 392-1084 • Mobile", color = Color(0xFF38BDF8), fontSize = 12.sp)
              }
              FilledTonalButton(
                onClick = {},
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF0369A1))
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
              text = "Token: vtl_emg_qr_7718de55 • Read-Only No Login Required",
              color = Color(0xFF94A3B8),
              fontSize = 10.sp,
              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
          }
        }
      }
    }
  }
}

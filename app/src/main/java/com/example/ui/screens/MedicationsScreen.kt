package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ItemStatus
import com.example.data.model.MedicationItem
import com.example.data.model.Provenance
import com.example.data.repository.VitalithRepository
import com.example.ui.components.ProvenanceBadge
import com.example.ui.theme.*
import java.util.UUID

@Composable
fun MedicationsScreen(
  repository: VitalithRepository,
  modifier: Modifier = Modifier
) {
  val medications by repository.medications.collectAsState()
  var showAddDialog by remember { mutableStateOf(false) }

  val activeMeds = medications.filter { it.status == ItemStatus.ACTIVE }
  val discontinuedMeds = medications.filter { it.status == ItemStatus.DISCONTINUED }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Header
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Medication Reconciliation",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Single unified list • Conflict & interaction flags",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        FilledTonalButton(
          onClick = { showAddDialog = true },
          colors = ButtonDefaults.filledTonalButtonColors(containerColor = VitalTealContainer),
          modifier = Modifier.testTag("add_medication_button")
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add", color = VitalTealPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                text = "${activeMeds.size} Active Prescriptions Reconciled",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = VitalOnEmeraldContainer
              )
              Text(
                text = "Cross-referenced against Epic EHR and Walgreens fill history",
                fontSize = 11.sp,
                color = VitalOnEmeraldContainer
              )
            }
          }
        }
      }

      // Interaction Watch Banner
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = VitalWarningContainer.copy(alpha = 0.6f)),
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
                text = "Routine 6-month hepatic (LFT) panel recommended when co-administering statins with metformin. No active contraindication present.",
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
        Text(
          text = "Active Prescriptions & Supplements (${activeMeds.size})",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }

      items(activeMeds, key = { it.id }) { med ->
        MedicationCard(med = med, onReconcile = { repository.reconcileMedication(med.id) })
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
          MedicationCard(med = med, onReconcile = {})
        }
      }
    }
  }

  // Add Medication Dialog
  if (showAddDialog) {
    var medName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var prescriber by remember { mutableStateOf("") }
    var indication by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Log Medication / Supplement", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = medName,
            onValueChange = { medName = it },
            label = { Text("Medication Name (e.g. Lisinopril)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = dosage,
            onValueChange = { dosage = it },
            label = { Text("Dosage (e.g. 10 mg)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = frequency,
            onValueChange = { frequency = it },
            label = { Text("Frequency (e.g. 1 tablet once daily in morning)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = prescriber,
            onValueChange = { prescriber = it },
            label = { Text("Prescriber / Source (e.g. Dr. Vance, Patient Reported)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = indication,
            onValueChange = { indication = it },
            label = { Text("Indication / Condition") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (medName.isNotBlank()) {
              val newMed = MedicationItem(
                id = "med_${UUID.randomUUID().toString().take(6)}",
                profileId = "profile_doug",
                name = medName,
                dosage = dosage.ifBlank { "Unspecified" },
                frequency = frequency.ifBlank { "Daily" },
                prescriber = prescriber.ifBlank { "Patient Manual Entry" },
                startDate = "Today",
                status = ItemStatus.ACTIVE,
                indication = indication.ifBlank { "General Health" },
                conflictWarning = null,
                isReconciled = true,
                provenance = Provenance("Patient Manual Entry", "Today", 0.95f, "Patient-Owned")
              )
              repository.addMedication(newMed)
              showAddDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
        ) {
          Text("Save to Vault")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun MedicationCard(
  med: MedicationItem,
  onReconcile: () -> Unit
) {
  val isDiscontinued = med.status == ItemStatus.DISCONTINUED

  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (isDiscontinued) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    ),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
    modifier = Modifier.fillMaxWidth()
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
        StatusBadge(status = med.status)
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
        if (!med.isReconciled && !isDiscontinued) {
          TextButton(onClick = onReconcile) {
            Text("Verify / Reconcile", fontSize = 11.sp, color = VitalTealPrimary)
          }
        }
      }
    }
  }
}

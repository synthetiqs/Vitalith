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
import com.example.data.model.CareGrant
import com.example.data.repository.VitalithRepository
import com.example.ui.theme.*

@Composable
fun SharingScreen(
  repository: VitalithRepository,
  modifier: Modifier = Modifier
) {
  val careGrants by repository.careGrants.collectAsState()
  var showCreateGrantDialog by remember { mutableStateOf(false) }

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
            text = "Care-Team Sharing",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Time-limited, revocable links • Granular scopes",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Button(
          onClick = { showCreateGrantDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("create_share_link_button")
        ) {
          Icon(imageVector = Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Link", fontSize = 12.sp)
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
      // Security Protocol Notice
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = VitalTealContainer.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = VitalTealPrimary)
            Column {
              Text(
                text = "Patient Access Control & Revocation",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = VitalOnTealContainer
              )
              Text(
                text = "Shared links use end-to-end tokenized sessions. When you revoke, access terminates instantly across all provider screens.",
                fontSize = 11.sp,
                color = VitalOnTealContainer,
                lineHeight = 15.sp
              )
            }
          }
        }
      }

      item {
        Text(
          text = "Active Access Grants (${careGrants.count { it.isActive }})",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }

      items(careGrants, key = { it.id }) { grant ->
        CareGrantCard(
          grant = grant,
          onRevoke = { repository.revokeCareGrant(grant.id) }
        )
      }
    }
  }

  // Create Grant Dialog
  if (showCreateGrantDialog) {
    var recipientName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Specialist / Clinician") }
    var selectedScope by remember { mutableStateOf("Full Longitudinal Record") }
    var selectedDuration by remember { mutableStateOf("7 days") }

    AlertDialog(
      onDismissRequest = { showCreateGrantDialog = false },
      title = { Text("Generate Secure Care Link", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = recipientName,
            onValueChange = { recipientName = it },
            label = { Text("Clinician or Facility Name") },
            placeholder = { Text("e.g. Dr. Jennifer Davis") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = role,
            onValueChange = { role = it },
            label = { Text("Role / Specialty") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Text("Access Scope", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          val scopes = listOf("Full Longitudinal Record", "Labs & Medications Only", "Emergency Snapshot Only")
          scopes.forEach { scope ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              RadioButton(
                selected = selectedScope == scope,
                onClick = { selectedScope = scope }
              )
              Text(text = scope, fontSize = 12.sp)
            }
          }

          Text("Link Expiration", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          val durations = listOf("24 hours", "7 days", "30 days")
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            durations.forEach { dur ->
              FilterChip(
                selected = selectedDuration == dur,
                onClick = { selectedDuration = dur },
                label = { Text(dur, fontSize = 11.sp) }
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (recipientName.isNotBlank()) {
              repository.createCareGrant(
                recipientName = recipientName,
                role = role,
                scope = selectedScope,
                durationLabel = selectedDuration
              )
              showCreateGrantDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
        ) {
          Text("Create Secure Token")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateGrantDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun CareGrantCard(
  grant: CareGrant,
  onRevoke: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (grant.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = grant.recipientName,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (grant.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = grant.recipientRole,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Surface(
          color = if (grant.isActive) VitalEmeraldContainer else VitalAlertContainer,
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            text = if (grant.isActive) "ACTIVE" else "REVOKED",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (grant.isActive) VitalEmeraldSuccess else VitalAlertRed,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = "Scope: ${grant.scope}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = grant.expiresAt, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Token: ${grant.tokenKey} • Accessed ${grant.accessCount} times",
            fontSize = 10.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (grant.isActive) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = onRevoke,
            colors = ButtonDefaults.textButtonColors(contentColor = VitalAlertRed)
          ) {
            Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Revoke Immediately", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

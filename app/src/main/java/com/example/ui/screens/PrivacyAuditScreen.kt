package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.data.model.AuditEntry
import com.example.data.repository.VitalithRepository
import com.example.ui.theme.*

import com.example.data.auth.AuthService
import com.example.data.auth.AuthUiState
import kotlinx.coroutines.launch

@Composable
fun PrivacyAuditScreen(
  repository: VitalithRepository,
  authService: AuthService? = null,
  modifier: Modifier = Modifier,
  onNavigateBack: () -> Unit = {}
) {
  val auditLogs by repository.auditLogs.collectAsState()
  val userProfile by repository.userProfile.collectAsState()
  val currentUser = authService?.currentUser?.collectAsState()?.value
  val authState = authService?.uiState?.collectAsState()?.value
  val coroutineScope = rememberCoroutineScope()
  var showExportModal by remember { mutableStateOf(false) }
  var exportFormat by remember { mutableStateOf("FHIR R4 JSON Bundle") }

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
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.weight(1f)
        ) {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
          Column {
            Text(
              text = "Privacy, Security & Audit",
              fontSize = 19.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Patient-Owned • Immutable Logs",
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        FilledTonalButton(
          onClick = { showExportModal = true },
          colors = ButtonDefaults.filledTonalButtonColors(containerColor = VitalTealContainer),
          modifier = Modifier.testTag("export_fhir_bundle_button")
        ) {
          Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Export", color = VitalTealPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
      // Identity & Google Auth Card
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = VitalTealPrimary)
                Column {
                  Text(text = "Patient Account Identity", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                  Text(
                    text = currentUser?.displayName ?: userProfile.fullName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = currentUser?.email ?: userProfile.email,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Surface(
                color = VitalEmeraldContainer,
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = "VERIFIED",
                  color = VitalEmeraldSuccess,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  coroutineScope.launch {
                    authService?.signInWithGoogle()
                  }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Google Sign-In", fontSize = 12.sp)
              }

              FilledTonalButton(
                onClick = { authService?.signOut() },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sign Out", fontSize = 12.sp)
              }
            }
          }
        }
      }

      // Offline-First Local Room Database Status
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = VitalNavy),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = VitalTealPrimaryDark, modifier = Modifier.size(24.dp))
            Column {
              Text(
                text = "Offline-First Local Room Database",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Health records & timeline are persisted locally in Room SQLite with full offline caching.",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                lineHeight = 15.sp
              )
            }
          }
        }
      }

      // Non-Negotiable Privacy Principles
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = VitalEmeraldSuccess)
              Text(text = "SynthetiQs Privacy Principles", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            val principles = listOf(
              "Patient-owned: Export complete raw data & revoke external access at any time.",
              "Privacy-first: Grounded RAG executes without selling or training foundational LLMs on user PHI.",
              "Zero-knowledge encryption: AES-256 at rest, TLS 1.3 in transit.",
              "HIPAA & GDPR Compliant: Full immutable audit trail on every record access."
            )
            principles.forEach { p ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(14.dp))
                Text(text = p, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 15.sp)
              }
            }
          }
        }
      }

      // Immutable Audit Logs Section
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "Security Audit Trail (${auditLogs.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Surface(
            color = VitalEmeraldContainer,
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = "IMMUTABLE LEDGER",
              color = VitalEmeraldSuccess,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      items(auditLogs, key = { it.id }) { log ->
        AuditLogCard(log = log)
      }
    }
  }

  // FHIR Export Modal
  if (showExportModal) {
    AlertDialog(
      onDismissRequest = { showExportModal = false },
      title = { Text("Export Personal Health Record", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Select your export format. Exported bundles include all longitudinal records, provenance hashes, and medication reconciliation files.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          val formats = listOf("FHIR R4 JSON Bundle", "International Patient Summary (IPS)", "Printable Clinical PDF")
          formats.forEach { fmt ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              RadioButton(
                selected = exportFormat == fmt,
                onClick = { exportFormat = fmt }
              )
              Text(text = fmt, fontSize = 13.sp)
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Payload contains: 6 Observations, 4 MedicationStatements, 2 Conditions, 1 AllergyIntolerance.",
              fontSize = 11.sp,
              modifier = Modifier.padding(8.dp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { showExportModal = false },
          colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary)
        ) {
          Text("Download $exportFormat")
        }
      },
      dismissButton = {
        TextButton(onClick = { showExportModal = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun AuditLogCard(log: AuditEntry) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(8.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = log.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VitalTealPrimary)
        Text(text = log.timestamp, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = log.resource, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(text = "Actor: ${log.actor}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "IP: ${log.ipAddress}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

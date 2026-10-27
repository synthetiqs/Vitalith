package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import com.example.data.repository.VitalithRepository
import com.example.ui.theme.*
import com.example.util.ClinicalPdfGenerator

@Composable
fun AppointmentPrepScreen(
  repository: VitalithRepository,
  onNavigateBack: () -> Unit,
  onShareWithDoctor: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val userProfile by repository.userProfile.collectAsState()
  val prep = remember(userProfile.unitPreferences) { repository.getAppointmentPrep(userProfile.unitPreferences) }
  val profiles by repository.profiles.collectAsState()
  val currentProfileId by repository.currentProfileId.collectAsState()
  val currentProfile = remember(profiles, currentProfileId) {
    profiles.firstOrNull { it.id == currentProfileId } ?: profiles.firstOrNull()
  }
  var showExportSnackbar by remember { mutableStateOf(false) }

  fun exportAndSharePdf() {
    try {
      val pdfFile = ClinicalPdfGenerator.generateAppointmentPrepPdf(
        context = context,
        prep = prep,
        profile = currentProfile
      )
      repository.recordAudit(
        action = "PDF_APPOINTMENT_PREP_EXPORT",
        resource = "Exported 1-page pre-visit clinical briefing for ${prep.doctorName}"
      )
      showExportSnackbar = true
      ClinicalPdfGenerator.shareFile(
        context = context,
        file = pdfFile,
        mimeType = "application/pdf",
        chooserTitle = "Share Appointment Prep Briefing PDF"
      )
    } catch (e: Exception) {
      android.widget.Toast.makeText(context, "Export error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
  }

  Scaffold(
    snackbarHost = {
      if (showExportSnackbar) {
        Snackbar(
          action = {
            TextButton(onClick = { showExportSnackbar = false }) {
              Text("Dismiss", color = Color.White)
            }
          },
          modifier = Modifier.padding(16.dp)
        ) {
          Text("1-Page Clinical Briefing exported as PDF and opened in Share Sheet.")
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background)
    ) {
      // Top Bar
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
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
              Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
              Text(
                text = "Appointment Prep Briefing",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Automated 1-page visit synthesis",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Row {
            IconButton(
              onClick = { exportAndSharePdf() },
              modifier = Modifier.testTag("export_prep_pdf_button")
            ) {
              Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = VitalTealPrimary)
            }
            IconButton(
              onClick = onShareWithDoctor,
              modifier = Modifier.testTag("share_prep_doctor_button")
            ) {
              Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = VitalTealPrimary)
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
        // Clinic Header Card
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = VitalNavy),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Column {
                  Text(text = prep.doctorName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                  Text(text = prep.clinic, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                }
                Surface(
                  color = VitalTealPrimary,
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    text = "Endocrinology",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
              HorizontalDivider(color = Color(0xFF334E68))
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text(text = "Scheduled Date & Time", color = Color(0xFF94A3B8), fontSize = 10.sp)
                  Text(text = prep.scheduledDate, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text(text = "Visit Focus", color = Color(0xFF94A3B8), fontSize = 10.sp)
                  Text(text = prep.visitReason, color = Color(0xFF55D6F7), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }

        // Longitudinal Metrics Summary
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "Key Biomarkers for this Encounter",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(10.dp))
              prep.criticalMetrics.forEach { (metric, value) ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = metric, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      text = value,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                  }
                }
              }
            }
          }
        }

        // Key Questions to Ask Your Doctor
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(18.dp))
                Text(
                  text = "Recommended Questions for Dr. Chen",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              prep.keyQuestionsToAsk.forEachIndexed { idx, q ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                  verticalAlignment = Alignment.Top,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Surface(
                    color = VitalTealContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(20.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(text = "${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VitalOnTealContainer)
                    }
                  }
                  Text(
                    text = q,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                  )
                }
              }
            }
          }
        }

        // Recent Changes Since Last Visit
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "Recent Changes Since Last Visit",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(8.dp))
              prep.recentChanges.forEach { change ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = VitalEmeraldSuccess, modifier = Modifier.size(14.dp))
                  Text(text = change, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
              }
            }
          }
        }

        // Action Buttons
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { exportAndSharePdf() },
              colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Export PDF Brief")
            }
            OutlinedButton(
              onClick = onShareWithDoctor,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Create 24h Link")
            }
          }
        }
      }
    }
  }
}

package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthTimelineItem
import com.example.data.model.UnitPreferences
import com.example.ui.theme.*
import com.example.util.FindingSeverity
import com.example.util.GeminiHealthSummarizer
import com.example.util.HealthRecordSummary
import kotlinx.coroutines.launch

@Composable
fun GeminiHealthSummaryCard(
  timelineItems: List<HealthTimelineItem>,
  patientName: String = "Doug Adarkwah",
  unitPreferences: UnitPreferences = UnitPreferences(),
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  var isLoading by remember { mutableStateOf(false) }
  var summary by remember { mutableStateOf<HealthRecordSummary?>(null) }
  var isExpanded by remember { mutableStateOf(true) }

  // Initial load
  LaunchedEffect(timelineItems.size, unitPreferences) {
    if (summary == null && timelineItems.isNotEmpty()) {
      isLoading = true
      summary = GeminiHealthSummarizer.generateSummary(
        records = timelineItems,
        patientName = patientName,
        unitPreferences = unitPreferences
      )
      isLoading = false
    }
  }

  fun refreshSummary() {
    coroutineScope.launch {
      isLoading = true
      summary = GeminiHealthSummarizer.generateSummary(
        records = timelineItems,
        patientName = patientName,
        unitPreferences = unitPreferences
      )
      isLoading = false
    }
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimary.copy(alpha = 0.35f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("gemini_health_summary_card")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Header: Gemini Badge & Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(VitalTealContainerDark.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = VitalTealPrimary,
              modifier = Modifier.size(20.dp)
            )
          }

          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Gemini Clinical Synthesis",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Surface(
                color = VitalEmeraldContainer,
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = "AI INSIGHTS",
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = VitalEmeraldSuccess,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "Multi-Record Synthesis & Anomaly Detection",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier
                .size(20.dp)
                .padding(end = 4.dp),
              strokeWidth = 2.dp,
              color = VitalTealPrimary
            )
          } else {
            IconButton(
              onClick = { refreshSummary() },
              modifier = Modifier.testTag("refresh_gemini_summary_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh Gemini Summary",
                tint = VitalTealPrimary,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          IconButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.testTag("toggle_gemini_summary_expansion")
          ) {
            Icon(
              imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = "Expand or Collapse",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // Summary Content Body
      val currentSummary = summary
      if (currentSummary != null) {
        // Natural Language Executive Summary
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Insights, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(16.dp))
              Text(
                text = "EXECUTIVE SUMMARY",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = VitalTealPrimary,
                letterSpacing = 0.5.sp
              )
            }
            Text(
              text = currentSummary.executiveSummary,
              fontSize = 13.sp,
              lineHeight = 19.sp,
              fontWeight = FontWeight.Normal,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        AnimatedVisibility(visible = isExpanded) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Flagged Anomalies & Clinical Watchlist
            if (currentSummary.flaggedFindings.isNotEmpty()) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "BIOMARKER ANOMALY & SURVEILLANCE RADAR",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                  )
                  Text(
                    text = "${currentSummary.flaggedFindings.size} items monitored",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                currentSummary.flaggedFindings.forEach { finding ->
                  val (badgeColor, textColor) = when (finding.severity) {
                    FindingSeverity.OPTIMAL -> Pair(VitalEmeraldContainer, VitalEmeraldSuccess)
                    FindingSeverity.STABLE -> Pair(Color(0xFFE0F2FE), Color(0xFF0284C7))
                    FindingSeverity.WATCHLIST -> Pair(VitalGoldContainer, VitalGoldAccent)
                    FindingSeverity.ATTENTION -> Pair(VitalAlertContainer, VitalAlertRed)
                  }

                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = finding.biomarkerOrItem,
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.5.sp,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                          color = badgeColor,
                          shape = RoundedCornerShape(4.dp)
                        ) {
                          Text(
                            text = "${finding.observedValue} • ${finding.severity.label.uppercase()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                      Text(
                        text = finding.clinicalRationale,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                      )
                    }
                  }
                }
              }
            }

            // Key Longitudinal Insights
            if (currentSummary.keyInsights.isNotEmpty()) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "LONGITUDINAL CLINICAL TRAJECTORY",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  letterSpacing = 0.5.sp
                )
                currentSummary.keyInsights.forEach { insight ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    Icon(
                      imageVector = Icons.Default.CheckCircleOutline,
                      contentDescription = null,
                      tint = VitalTealPrimary,
                      modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                    )
                    Text(
                      text = insight,
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurface,
                      lineHeight = 16.sp
                    )
                  }
                }
              }
            }

            // Doctor Questions
            if (currentSummary.suggestedQuestions.isNotEmpty()) {
              Surface(
                color = VitalNavy,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(15.dp))
                    Text(
                      text = "QUESTIONS FOR YOUR NEXT PHYSICIAN VISIT",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = VitalTealPrimary,
                      letterSpacing = 0.5.sp
                    )
                  }
                  currentSummary.suggestedQuestions.forEachIndexed { i, q ->
                    Text(
                      text = "${i + 1}. $q",
                      fontSize = 11.5.sp,
                      color = Color.White,
                      lineHeight = 16.sp
                    )
                  }
                }
              }
            }

            // Provenance footer
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Model: ${currentSummary.modelUsed}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = currentSummary.generatedAt,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      } else {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = VitalTealPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Synthesizing patient records with Gemini AI...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

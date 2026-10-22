package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Citation
import com.example.ui.theme.*

@Composable
fun ClinicalAlertBanner(
  modifier: Modifier = Modifier,
  isRedFlag: Boolean = false,
  customText: String? = null
) {
  if (isRedFlag) {
    Card(
      colors = CardDefaults.cardColors(containerColor = VitalAlertContainer),
      shape = RoundedCornerShape(10.dp),
      modifier = modifier
        .fillMaxWidth()
        .border(1.5.dp, VitalAlertRed, RoundedCornerShape(10.dp))
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = "Medical Alert",
          tint = VitalAlertRed,
          modifier = Modifier.size(24.dp)
        )
        Column {
          Text(
            text = "RED FLAG CLINICAL TRIAGE",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = VitalOnAlertContainer
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = customText ?: "Symptoms suggest potential emergency. Dial 911 or visit the nearest ER immediately. Do not rely on digital summaries.",
            fontSize = 12.sp,
            color = VitalOnAlertContainer,
            lineHeight = 16.sp
          )
        }
      }
    }
  } else {
    // Normal clinical safety disclaimer
    Card(
      colors = CardDefaults.cardColors(containerColor = VitalTealContainer.copy(alpha = 0.4f)),
      shape = RoundedCornerShape(8.dp),
      modifier = modifier
        .fillMaxWidth()
        .border(1.dp, VitalTealPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = "Clinical Safety",
          tint = VitalTealPrimary,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = "Explain, don't diagnose: Grounded in your health records. Consult your clinician for medical diagnosis or dosage changes.",
          fontSize = 11.sp,
          color = VitalOnTealContainer,
          lineHeight = 14.sp
        )
      }
    }
  }
}

@Composable
fun CitationPill(
  citation: Citation,
  modifier: Modifier = Modifier
) {
  var showDetails by remember { mutableStateOf(false) }

  Column(modifier = modifier) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(6.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
      modifier = Modifier
        .clip(RoundedCornerShape(6.dp))
        .clickable { showDetails = !showDetails }
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Bookmark,
          contentDescription = "Source Citation",
          tint = VitalTealPrimary,
          modifier = Modifier.size(12.dp)
        )
        Text(
          text = citation.title,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "(${citation.date})",
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    if (showDetails) {
      Spacer(modifier = Modifier.height(4.dp))
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 2.dp)
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text(
            text = "Source: ${citation.source}",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = VitalTealPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "\"${citation.excerpt}\"",
            fontSize = 11.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

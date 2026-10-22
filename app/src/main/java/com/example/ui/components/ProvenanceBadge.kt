package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Provenance
import com.example.ui.theme.VitalEmeraldContainer
import com.example.ui.theme.VitalEmeraldSuccess
import com.example.ui.theme.VitalNavySurface
import com.example.ui.theme.VitalOnEmeraldContainer
import com.example.ui.theme.VitalTealPrimary

@Composable
fun ProvenanceBadge(
  provenance: Provenance,
  modifier: Modifier = Modifier,
  showFullDetails: Boolean = false
) {
  val confidencePct = (provenance.confidenceScore * 100).toInt()

  Column(modifier = modifier) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier
        .clip(RoundedCornerShape(6.dp))
        .background(VitalEmeraldContainer.copy(alpha = 0.5f))
        .border(1.dp, VitalEmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Shield,
        contentDescription = "Provenance Verified",
        tint = VitalEmeraldSuccess,
        modifier = Modifier.size(13.dp)
      )
      Text(
        text = provenance.sourceSystem,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = VitalOnEmeraldContainer
      )
      Text(
        text = "• ${confidencePct}% match",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = VitalEmeraldSuccess
      )
    }

    if (showFullDetails) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Ingested: ${provenance.ingestedAt} | Consent: ${provenance.consentState}",
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = "Digest: ${provenance.documentHash}",
        fontSize = 9.sp,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )
    }
  }
}

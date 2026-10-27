package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PatientProfile
import com.example.ui.theme.*

/**
 * Glassmorphic hero banner with mesh gradient (deep obsidian navy #071426 -> rich teal #004D61)
 * and glowing border.
 */
@Composable
fun GlassHeroBanner(
  profile: PatientProfile,
  onOpenEmergency: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
  val borderAlpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.75f,
    animationSpec = infiniteRepeatable(
      animation = tween(2400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alpha"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(
        brush = Brush.linearGradient(
          colors = listOf(
            MeshGradientStart,
            MeshGradientMid,
            MeshGradientEnd
          )
        )
      )
      .border(
        width = 1.5.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            GlowTealBorder.copy(alpha = borderAlpha),
            GlassWhiteBorder,
            VitalTealPrimaryDark.copy(alpha = borderAlpha * 0.8f)
          )
        ),
        shape = RoundedCornerShape(20.dp)
      )
      .clickable(onClick = onOpenEmergency)
      .testTag("emergency_snapshot_banner")
  ) {
    // Subtle inner glowing decorative orb
    Box(
      modifier = Modifier
        .size(160.dp)
        .align(Alignment.TopEnd)
        .offset(x = 40.dp, y = (-30).dp)
        .background(
          brush = Brush.radialGradient(
            colors = listOf(VitalTealPrimaryDark.copy(alpha = 0.25f), Color.Transparent)
          ),
          shape = CircleShape
        )
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            color = VitalAlertRed.copy(alpha = 0.22f),
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VitalAlertRed.copy(alpha = 0.5f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.HealthAndSafety,
                contentDescription = null,
                tint = VitalAlertRed,
                modifier = Modifier.size(12.dp)
              )
              Text(
                text = "EMERGENCY ID",
                color = Color(0xFFFCA5A5),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              )
            }
          }

          Surface(
            color = VitalTealContainerDark.copy(alpha = 0.6f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "Blood ${profile.bloodType}",
              color = VitalTealPrimaryDark,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = "Snapshot",
            color = VitalTealPrimaryDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open Emergency",
            tint = VitalTealPrimaryDark,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = profile.name,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "MRN: ${profile.mrn} • Penicillin Anaphylaxis Alert",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }

        Surface(
          color = GlassWhite,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhiteBorder)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = VitalEmeraldSuccess,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = "Verified",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}

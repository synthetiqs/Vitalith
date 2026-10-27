package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberNode
import com.example.ui.theme.CoralNode
import com.example.ui.theme.EmeraldNode

/**
 * Three-zone clinical range bar with glowing indicator dot.
 */
@Composable
fun RangeBar(
  currentValue: Float,
  minVal: Float,
  maxVal: Float,
  lowNormal: Float,
  highNormal: Float,
  unit: String = "",
  modifier: Modifier = Modifier
) {
  val animProgress = remember { Animatable(0f) }

  LaunchedEffect(currentValue) {
    animProgress.snapTo(0f)
    animProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
    )
  }

  val totalRange = (maxVal - minVal).coerceAtLeast(0.1f)
  val normalizedValue = ((currentValue - minVal) / totalRange).coerceIn(0f, 1f)

  val activeColor = when {
    currentValue < lowNormal -> AmberNode
    currentValue <= highNormal -> EmeraldNode
    else -> CoralNode
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(18.dp)
    ) {
      val w = size.width
      val h = 6.dp.toPx()
      val yOffset = (size.height - h) / 2f

      val zone1Width = ((lowNormal - minVal) / totalRange).coerceIn(0f, 1f) * w
      val zone2Width = ((highNormal - lowNormal) / totalRange).coerceIn(0f, 1f) * w
      val zone3Width = (w - zone1Width - zone2Width).coerceAtLeast(0f)

      val corner = CornerRadius(h / 2, h / 2)

      // Background track zones
      // Low Zone (Amber/Muted)
      drawRoundRect(
        color = AmberNode.copy(alpha = 0.3f),
        topLeft = Offset(0f, yOffset),
        size = Size(zone1Width, h),
        cornerRadius = corner
      )

      // Normal Target Zone (Emerald)
      drawRoundRect(
        color = EmeraldNode.copy(alpha = 0.35f),
        topLeft = Offset(zone1Width, yOffset),
        size = Size(zone2Width, h)
      )

      // High Zone (Coral)
      drawRoundRect(
        color = CoralNode.copy(alpha = 0.3f),
        topLeft = Offset(zone1Width + zone2Width, yOffset),
        size = Size(zone3Width, h),
        cornerRadius = corner
      )

      // Current Value Indicator Dot (with smooth animated progress)
      val targetX = (normalizedValue * w) * animProgress.value
      val dotY = size.height / 2f

      // Outer halo glow
      drawCircle(
        color = activeColor.copy(alpha = 0.25f),
        radius = 8.dp.toPx(),
        center = Offset(targetX, dotY)
      )
      // Solid outer ring
      drawCircle(
        color = Color.White,
        radius = 5.dp.toPx(),
        center = Offset(targetX, dotY)
      )
      // Core dot
      drawCircle(
        color = activeColor,
        radius = 3.5.dp.toPx(),
        center = Offset(targetX, dotY)
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Low <$lowNormal",
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )
      Text(
        text = "Target $lowNormal - $highNormal $unit",
        fontSize = 10.sp,
        color = EmeraldNode
      )
      Text(
        text = "High >$highNormal",
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )
    }
  }
}

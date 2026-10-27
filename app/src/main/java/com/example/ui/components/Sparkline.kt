package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TealNode

/**
 * Bezier sparkline canvas composable with gradient fill underneath.
 */
@Composable
fun Sparkline(
  points: List<Float>,
  modifier: Modifier = Modifier,
  lineColor: Color = TealNode,
  gradientStart: Color = lineColor.copy(alpha = 0.35f),
  gradientEnd: Color = lineColor.copy(alpha = 0.0f),
  strokeWidth: Dp = 2.5.dp,
  showEndDot: Boolean = true
) {
  if (points.size < 2) return

  val progress = remember { Animatable(0f) }

  LaunchedEffect(points) {
    progress.snapTo(0f)
    progress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )
  }

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp)
  ) {
    val w = size.width
    val h = size.height
    val minVal = points.minOrNull() ?: 0f
    val maxVal = points.maxOrNull() ?: 1f
    val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

    // Compute pixel coordinates
    val stepX = w / (points.size - 1)
    val coords = points.mapIndexed { index, value ->
      val x = index * stepX
      // Invert Y so higher values are higher on screen, padding top and bottom
      val normalized = (value - minVal) / range
      val y = h - (normalized * (h * 0.7f) + (h * 0.15f))
      x to y
    }

    val path = Path()
    val fillPath = Path()

    path.moveTo(coords.first().first, coords.first().second)
    fillPath.moveTo(coords.first().first, h)
    fillPath.lineTo(coords.first().first, coords.first().second)

    // Build cubic Bezier curves smoothly through points
    for (i in 0 until coords.size - 1) {
      val p0 = coords[i]
      val p1 = coords[i + 1]
      val controlX1 = p0.first + (p1.first - p0.first) / 2f
      val controlY1 = p0.second
      val controlX2 = controlX1
      val controlY2 = p1.second

      path.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.first, p1.second)
      fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.first, p1.second)
    }

    fillPath.lineTo(coords.last().first, h)
    fillPath.close()

    // Draw area fill
    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        colors = listOf(gradientStart, gradientEnd),
        startY = 0f,
        endY = h
      )
    )

    // Draw main Bezier stroke
    drawPath(
      path = path,
      color = lineColor,
      style = Stroke(
        width = strokeWidth.toPx(),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
      )
    )

    // Glowing indicator dot on latest reading
    if (showEndDot) {
      val lastCoord = coords.last()
      // Outer glow circle
      drawCircle(
        color = lineColor.copy(alpha = 0.25f),
        radius = 8.dp.toPx(),
        center = androidx.compose.ui.geometry.Offset(lastCoord.first, lastCoord.second)
      )
      // Solid center dot
      drawCircle(
        color = Color.White,
        radius = 3.dp.toPx(),
        center = androidx.compose.ui.geometry.Offset(lastCoord.first, lastCoord.second)
      )
      drawCircle(
        color = lineColor,
        radius = 3.dp.toPx(),
        style = Stroke(width = 1.5.dp.toPx()),
        center = androidx.compose.ui.geometry.Offset(lastCoord.first, lastCoord.second)
      )
    }
  }
}

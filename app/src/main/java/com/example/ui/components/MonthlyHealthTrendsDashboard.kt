package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlucoseUnit
import com.example.data.model.UnitPreferences
import com.example.data.model.WeightUnit
import com.example.ui.theme.*
import com.example.util.UcumUnitConverter
import kotlin.math.roundToInt

enum class MetricTrendType(
  val label: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val unit: String
) {
  BLOOD_PRESSURE("Blood Pressure", Icons.Default.Favorite, "mmHg"),
  BODY_WEIGHT("Body Weight", Icons.Default.MonitorWeight, "kg"),
  BLOOD_GLUCOSE("Fasting Glucose", Icons.Default.Bloodtype, "mmol/L"),
  HEART_RATE("Heart Rate", Icons.AutoMirrored.Filled.ShowChart, "bpm")
}

data class TrendDataPoint(
  val dayLabel: String,
  val fullDate: String,
  val primaryValue: Float, // Systolic for BP, Weight in kg/lbs, Glucose in mmol/L or mg/dL, Heart Rate
  val secondaryValue: Float? = null, // Diastolic for BP
  val note: String = ""
)

@Composable
fun MonthlyHealthTrendsDashboard(
  modifier: Modifier = Modifier,
  unitPreferences: UnitPreferences = UnitPreferences(),
  onLogReadingClick: (() -> Unit)? = null
) {
  var selectedMetric by remember { mutableStateOf(MetricTrendType.BLOOD_PRESSURE) }
  var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

  // 30-day realistic historical data sets over the past month
  val bpPoints = remember {
    listOf(
      TrendDataPoint("Sep 24", "Sep 24, 2026", 126f, 82f, "Slightly elevated post-travel"),
      TrendDataPoint("Sep 27", "Sep 27, 2026", 124f, 80f, "Routine morning check"),
      TrendDataPoint("Sep 30", "Sep 30, 2026", 122f, 79f, "Post aerobic walk"),
      TrendDataPoint("Oct 03", "Oct 03, 2026", 121f, 78f, "Normal reading"),
      TrendDataPoint("Oct 06", "Oct 06, 2026", 119f, 77f, "Resting morning"),
      TrendDataPoint("Oct 09", "Oct 09, 2026", 122f, 79f, "Clinic verification"),
      TrendDataPoint("Oct 12", "Oct 12, 2026", 118f, 76f, "Optimal morning state"),
      TrendDataPoint("Oct 15", "Oct 15, 2026", 119f, 76f, "Post-hydration check"),
      TrendDataPoint("Oct 18", "Oct 18, 2026", 117f, 75f, "Optimal baseline"),
      TrendDataPoint("Oct 21", "Oct 21, 2026", 118f, 76f, "Resting seated check"),
      TrendDataPoint("Oct 23", "Oct 23, 2026", 116f, 74f, "Current reading")
    )
  }

  val weightPoints = remember {
    listOf(
      TrendDataPoint("Sep 24", "Sep 24, 2026", 80.0f, null, "Baseline start of month"),
      TrendDataPoint("Sep 27", "Sep 27, 2026", 79.8f, null, "Post-workout check"),
      TrendDataPoint("Sep 30", "Sep 30, 2026", 79.5f, null, "Dietary adherence day 7"),
      TrendDataPoint("Oct 03", "Oct 03, 2026", 79.3f, null, "Weekly check-in"),
      TrendDataPoint("Oct 06", "Oct 06, 2026", 79.1f, null, "Stable morning weight"),
      TrendDataPoint("Oct 09", "Oct 09, 2026", 78.9f, null, "Mid-month milestone"),
      TrendDataPoint("Oct 12", "Oct 12, 2026", 78.7f, null, "Clean nutrition trend"),
      TrendDataPoint("Oct 15", "Oct 15, 2026", 78.5f, null, "Consistently trending down"),
      TrendDataPoint("Oct 18", "Oct 18, 2026", 78.4f, null, "Post cardio weigh-in"),
      TrendDataPoint("Oct 21", "Oct 21, 2026", 78.3f, null, "Target zone approach"),
      TrendDataPoint("Oct 23", "Oct 23, 2026", 78.2f, null, "Current morning weight")
    )
  }

  val glucosePoints = remember {
    listOf(
      TrendDataPoint("Sep 24", "Sep 24, 2026", 5.7f, null, "Fasting 10 hours"),
      TrendDataPoint("Sep 28", "Sep 28, 2026", 5.5f, null, "Fasting 12 hours"),
      TrendDataPoint("Oct 02", "Oct 02, 2026", 5.3f, null, "Normal fasting glucose"),
      TrendDataPoint("Oct 06", "Oct 06, 2026", 5.2f, null, "Optimal morning reading"),
      TrendDataPoint("Oct 10", "Oct 10, 2026", 5.3f, null, "Post-walk morning check"),
      TrendDataPoint("Oct 14", "Oct 14, 2026", 5.1f, null, "Lab confirmed fasting"),
      TrendDataPoint("Oct 18", "Oct 18, 2026", 5.1f, null, "Consistent glycemic control"),
      TrendDataPoint("Oct 23", "Oct 23, 2026", 5.0f, null, "Optimal fasting state")
    )
  }

  val heartRatePoints = remember {
    listOf(
      TrendDataPoint("Sep 24", "Sep 24, 2026", 68f, null, "Resting baseline"),
      TrendDataPoint("Sep 28", "Sep 28, 2026", 66f, null, "Post sleep check"),
      TrendDataPoint("Oct 02", "Oct 02, 2026", 64f, null, "Resting morning"),
      TrendDataPoint("Oct 06", "Oct 06, 2026", 63f, null, "Cardio recovery day"),
      TrendDataPoint("Oct 10", "Oct 10, 2026", 62f, null, "High HRV morning"),
      TrendDataPoint("Oct 15", "Oct 15, 2026", 61f, null, "Resting state"),
      TrendDataPoint("Oct 19", "Oct 19, 2026", 60f, null, "Excellent recovery"),
      TrendDataPoint("Oct 23", "Oct 23, 2026", 59f, null, "Current resting heart rate")
    )
  }

  val convertedWeightPoints = remember(weightPoints, unitPreferences.weightUnit) {
    if (unitPreferences.weightUnit == WeightUnit.LBS) {
      weightPoints.map { pt ->
        val lbsVal = UcumUnitConverter.kgToLbs(pt.primaryValue.toDouble()).toFloat()
        val roundedLbs = (lbsVal * 10).roundToInt() / 10f
        pt.copy(primaryValue = roundedLbs)
      }
    } else {
      weightPoints
    }
  }

  val convertedGlucosePoints = remember(glucosePoints, unitPreferences.glucoseUnit) {
    if (unitPreferences.glucoseUnit == GlucoseUnit.MG_DL) {
      glucosePoints.map { pt ->
        val mgDlVal = UcumUnitConverter.mmolLToMgDl(pt.primaryValue.toDouble()).toFloat()
        val roundedMgDl = (mgDlVal * 10).roundToInt() / 10f
        pt.copy(primaryValue = roundedMgDl)
      }
    } else {
      glucosePoints
    }
  }

  val activeUnit = when (selectedMetric) {
    MetricTrendType.BLOOD_PRESSURE -> "mmHg"
    MetricTrendType.BODY_WEIGHT -> unitPreferences.weightUnit.label
    MetricTrendType.BLOOD_GLUCOSE -> unitPreferences.glucoseUnit.label
    MetricTrendType.HEART_RATE -> "bpm"
  }

  val activePoints = when (selectedMetric) {
    MetricTrendType.BLOOD_PRESSURE -> bpPoints
    MetricTrendType.BODY_WEIGHT -> convertedWeightPoints
    MetricTrendType.BLOOD_GLUCOSE -> convertedGlucosePoints
    MetricTrendType.HEART_RATE -> heartRatePoints
  }

  val currentLatest = activePoints.last()
  val firstPoint = activePoints.first()
  val deltaPrimary = currentLatest.primaryValue - firstPoint.primaryValue
  val deltaSecondary = if (currentLatest.secondaryValue != null && firstPoint.secondaryValue != null) {
    currentLatest.secondaryValue - firstPoint.secondaryValue
  } else null

  val isDeltaPositiveDirection = when (selectedMetric) {
    MetricTrendType.BLOOD_PRESSURE -> deltaPrimary <= 0
    MetricTrendType.BODY_WEIGHT -> deltaPrimary <= 0
    MetricTrendType.BLOOD_GLUCOSE -> deltaPrimary <= 0
    MetricTrendType.HEART_RATE -> deltaPrimary <= 0
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(20.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("monthly_health_trends_dashboard")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header: Title & Timeframe Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
              imageVector = Icons.Default.Timeline,
              contentDescription = null,
              tint = VitalTealPrimary,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "Monthly Health Trends",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Text(
            text = "Past 30 Days • Interactive Recharts Visualizer",
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          color = VitalTealContainerDark.copy(alpha = 0.25f),
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, VitalTealPrimary.copy(alpha = 0.3f))
        ) {
          Text(
            text = "PAST MONTH",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = VitalTealPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // Metric Switcher Tabs (Recharts Tab-dock styling)
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          MetricTrendType.values().forEach { metric ->
            val isSelected = selectedMetric == metric
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
              shadowElevation = if (isSelected) 2.dp else 0.dp,
              modifier = Modifier
                .weight(1f)
                .clickable {
                  selectedMetric = metric
                  selectedPointIndex = null
                }
                .testTag("trend_tab_${metric.name.lowercase()}")
            ) {
              Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = metric.icon,
                  contentDescription = metric.label,
                  tint = if (isSelected) VitalTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = when (metric) {
                    MetricTrendType.BLOOD_PRESSURE -> "BP"
                    MetricTrendType.BODY_WEIGHT -> "Weight"
                    MetricTrendType.BLOOD_GLUCOSE -> "Glucose"
                    MetricTrendType.HEART_RATE -> "HR"
                  },
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      // Current Metric Spotlight & 30-Day Delta Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          val activePoint = selectedPointIndex?.let { activePoints.getOrNull(it) } ?: currentLatest

          Text(
            text = if (selectedPointIndex != null) "Selected: ${activePoint.dayLabel}" else "Current: ${activePoint.dayLabel}",
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = if (selectedMetric == MetricTrendType.BLOOD_PRESSURE) {
                "${activePoint.primaryValue.roundToInt()}/${activePoint.secondaryValue?.roundToInt() ?: 0}"
              } else {
                String.format(java.util.Locale.US, "%.1f", activePoint.primaryValue)
              },
              fontSize = 28.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
              fontFamily = FontFamily.Default
            )
            Text(
              text = activeUnit,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 4.dp)
            )
          }
        }

        // 30-Day Change Pill
        Surface(
          color = if (isDeltaPositiveDirection) VitalEmeraldContainer else VitalAlertContainer,
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = if (deltaPrimary <= 0) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
              contentDescription = null,
              tint = if (isDeltaPositiveDirection) VitalEmeraldSuccess else VitalAlertRed,
              modifier = Modifier.size(16.dp)
            )
            val deltaStr = if (selectedMetric == MetricTrendType.BLOOD_PRESSURE) {
              "${if (deltaPrimary > 0) "+" else ""}${deltaPrimary.roundToInt()} / ${if (deltaSecondary != null && deltaSecondary > 0) "+" else ""}${deltaSecondary?.roundToInt() ?: 0}"
            } else {
              String.format(java.util.Locale.US, "%+.1f %s", deltaPrimary, activeUnit)
            }
            Text(
              text = "$deltaStr (30d)",
              color = if (isDeltaPositiveDirection) VitalEmeraldSuccess else VitalAlertRed,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Recharts-Style Interactive Bézier Vector Canvas Chart
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
      ) {
        RechartsStyleNativeCanvas(
          points = activePoints,
          metricType = selectedMetric,
          selectedIndex = selectedPointIndex,
          onPointSelected = { selectedPointIndex = it }
        )
      }

      // X-Axis Date Intervals
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        val count = activePoints.size
        if (count > 0) {
          Text(text = activePoints.first().dayLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          if (count > 2) {
            Text(text = activePoints[count / 3].dayLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = activePoints[(count * 2) / 3].dayLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Text(text = activePoints.last().dayLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      // Recharts Interactive Tooltip inspection banner
      selectedPointIndex?.let { idx ->
        val point = activePoints.getOrNull(idx)
        if (point != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = VitalNavy,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = point.fullDate,
                  fontSize = 11.sp,
                  color = VitalTealPrimary,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = if (selectedMetric == MetricTrendType.BLOOD_PRESSURE) {
                    "Blood Pressure: ${point.primaryValue.roundToInt()} / ${point.secondaryValue?.roundToInt()} mmHg"
                  } else if (selectedMetric == MetricTrendType.BLOOD_GLUCOSE && unitPreferences.glucoseUnit == GlucoseUnit.MG_DL) {
                    "${selectedMetric.label}: ${point.primaryValue.roundToInt()} $activeUnit"
                  } else {
                    "${selectedMetric.label}: ${String.format(java.util.Locale.US, "%.1f", point.primaryValue)} $activeUnit"
                  },
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                if (point.note.isNotBlank()) {
                  Text(
                    text = "Note: ${point.note}",
                    fontSize = 10.5.sp,
                    color = Color(0xFFCBD5E1)
                  )
                }
              }

              IconButton(
                onClick = { selectedPointIndex = null },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }

      // Clinical Context & Reference Target Legend
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(VitalEmeraldSuccess)
            )
            Text(
              text = when (selectedMetric) {
                MetricTrendType.BLOOD_PRESSURE -> "AHA Goal: Systolic < 120, Diastolic < 80 mmHg"
                MetricTrendType.BODY_WEIGHT -> if (unitPreferences.weightUnit == WeightUnit.KG) "Healthy BMI Target: 76.0 – 79.0 kg" else "Healthy BMI Target: 168 – 174 lbs"
                MetricTrendType.BLOOD_GLUCOSE -> if (unitPreferences.glucoseUnit == GlucoseUnit.MMOL_L) "ADA Fasting Target: 3.9 – 5.5 mmol/L" else "ADA Fasting Target: 70 – 99 mg/dL"
                MetricTrendType.HEART_RATE -> "Resting Target: 55 – 70 bpm (Cardio Fit)"
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          if (onLogReadingClick != null) {
            TextButton(
              onClick = onLogReadingClick,
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Log", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RechartsStyleNativeCanvas(
  points: List<TrendDataPoint>,
  metricType: MetricTrendType,
  selectedIndex: Int?,
  onPointSelected: (Int) -> Unit
) {
  // Colors for styling
  val primaryLineColor = when (metricType) {
    MetricTrendType.BLOOD_PRESSURE -> Color(0xFF00BCD4) // Cyan Systolic
    MetricTrendType.BODY_WEIGHT -> Color(0xFF0D9488) // Vital Teal
    MetricTrendType.BLOOD_GLUCOSE -> Color(0xFF10B981) // Emerald
    MetricTrendType.HEART_RATE -> Color(0xFFF59E0B) // Amber
  }

  val secondaryLineColor = Color(0xFF6366F1) // Indigo Diastolic for BP
  val gridColor = Color.LightGray.copy(alpha = 0.35f)
  val cursorLineColor = VitalNavy.copy(alpha = 0.5f)

  // Compute min/max
  val allValues = points.flatMap { listOfNotNull(it.primaryValue, it.secondaryValue) }
  val rawMin = allValues.minOrNull() ?: 0f
  val rawMax = allValues.maxOrNull() ?: 100f
  val paddingRange = (rawMax - rawMin) * 0.15f
  val yMin = (rawMin - paddingRange).coerceAtLeast(0f)
  val yMax = rawMax + paddingRange

  Canvas(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(points) {
        detectTapGestures { offset ->
          val stepX = size.width / (points.size - 1).coerceAtLeast(1)
          val tappedIndex = (offset.x / stepX).roundToInt().coerceIn(0, points.size - 1)
          onPointSelected(tappedIndex)
        }
      }
      .pointerInput(points) {
        detectDragGestures { change, _ ->
          val stepX = size.width / (points.size - 1).coerceAtLeast(1)
          val draggedIndex = (change.position.x / stepX).roundToInt().coerceIn(0, points.size - 1)
          onPointSelected(draggedIndex)
        }
      }
  ) {
    val w = size.width
    val h = size.height
    val paddingHorizontal = 16.dp.toPx()
    val paddingVertical = 16.dp.toPx()
    val chartWidth = w - (paddingHorizontal * 2)
    val chartHeight = h - (paddingVertical * 2)

    val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

    fun valueToY(value: Float): Float {
      val ratio = (value - yMin) / (yMax - yMin).coerceAtLeast(1f)
      return paddingVertical + chartHeight * (1f - ratio)
    }

    // 1. Recharts CartesianGrid: Horizontal dashed lines
    val gridLinesCount = 4
    for (i in 0..gridLinesCount) {
      val gridY = paddingVertical + (chartHeight / gridLinesCount) * i
      drawLine(
        color = gridColor,
        start = Offset(paddingHorizontal, gridY),
        end = Offset(w - paddingHorizontal, gridY),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
      )
    }

    // 2. Primary Curve Path (and Area Fill)
    val primaryPath = Path()
    val primaryAreaPath = Path()

    val primaryCoords = points.mapIndexed { index, pt ->
      Offset(paddingHorizontal + index * stepX, valueToY(pt.primaryValue))
    }

    if (primaryCoords.isNotEmpty()) {
      primaryPath.moveTo(primaryCoords.first().x, primaryCoords.first().y)
      primaryAreaPath.moveTo(primaryCoords.first().x, paddingVertical + chartHeight)
      primaryAreaPath.lineTo(primaryCoords.first().x, primaryCoords.first().y)

      for (i in 0 until primaryCoords.size - 1) {
        val p0 = primaryCoords[i]
        val p1 = primaryCoords[i + 1]
        val controlX = (p0.x + p1.x) / 2f
        primaryPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
        primaryAreaPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
      }

      primaryAreaPath.lineTo(primaryCoords.last().x, paddingVertical + chartHeight)
      primaryAreaPath.close()

      // Draw Gradient Area under curve (Recharts style)
      drawPath(
        path = primaryAreaPath,
        brush = Brush.verticalGradient(
          colors = listOf(
            primaryLineColor.copy(alpha = 0.35f),
            primaryLineColor.copy(alpha = 0.03f)
          ),
          startY = paddingVertical,
          endY = paddingVertical + chartHeight
        )
      )

      // Draw Bézier Stroke Line
      drawPath(
        path = primaryPath,
        color = primaryLineColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      )
    }

    // 3. Secondary Curve (Diastolic for Blood Pressure)
    if (metricType == MetricTrendType.BLOOD_PRESSURE) {
      val secondaryCoords = points.mapIndexed { index, pt ->
        Offset(paddingHorizontal + index * stepX, valueToY(pt.secondaryValue ?: 0f))
      }

      if (secondaryCoords.isNotEmpty()) {
        val secPath = Path()
        secPath.moveTo(secondaryCoords.first().x, secondaryCoords.first().y)

        for (i in 0 until secondaryCoords.size - 1) {
          val p0 = secondaryCoords[i]
          val p1 = secondaryCoords[i + 1]
          val controlX = (p0.x + p1.x) / 2f
          secPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
        }

        drawPath(
          path = secPath,
          color = secondaryLineColor,
          style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
      }
    }

    // 4. Draw interactive Scrubber Cursor and active node highlight
    selectedIndex?.let { index ->
      if (index in points.indices) {
        val currentCoord = primaryCoords[index]

        // Vertical Scrubber line
        drawLine(
          color = cursorLineColor,
          start = Offset(currentCoord.x, paddingVertical),
          end = Offset(currentCoord.x, paddingVertical + chartHeight),
          strokeWidth = 1.5.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )

        // Highlight ring on primary node
        drawCircle(
          color = Color.White,
          radius = 6.5.dp.toPx(),
          center = currentCoord
        )
        drawCircle(
          color = primaryLineColor,
          radius = 5.dp.toPx(),
          center = currentCoord
        )

        // Highlight ring on secondary node (if BP)
        if (metricType == MetricTrendType.BLOOD_PRESSURE) {
          val secY = valueToY(points[index].secondaryValue ?: 0f)
          val secCoord = Offset(currentCoord.x, secY)

          drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = secCoord
          )
          drawCircle(
            color = secondaryLineColor,
            radius = 4.5.dp.toPx(),
            center = secCoord
          )
        }
      }
    }
  }
}

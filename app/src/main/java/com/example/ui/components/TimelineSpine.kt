package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthTimelineItem
import com.example.data.model.TimelineCategory
import com.example.ui.screens.StatusBadge
import com.example.ui.theme.*

/**
 * Connected vertical timeline item using continuous spine rail and color-coded category nodes.
 * Teal = Labs, Amber = Meds, Coral = Allergies, Indigo = Encounters, Emerald = Immunizations.
 */
@Composable
fun TimelineSpineItem(
  item: HealthTimelineItem,
  isFirst: Boolean = false,
  isLast: Boolean = false,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val nodeColor = when (item.category) {
    TimelineCategory.LAB -> TealNode
    TimelineCategory.MEDICATION -> AmberNode
    TimelineCategory.ALLERGY -> CoralNode
    TimelineCategory.CONDITION -> VitalWarningOrange
    TimelineCategory.ENCOUNTER -> IndigoNode
    TimelineCategory.IMMUNIZATION -> EmeraldNode
    else -> VitalTealPrimary
  }

  val categoryIcon: ImageVector = when (item.category) {
    TimelineCategory.LAB -> Icons.Default.Science
    TimelineCategory.MEDICATION -> Icons.Default.Medication
    TimelineCategory.ALLERGY -> Icons.Default.Warning
    TimelineCategory.CONDITION -> Icons.Default.Healing
    TimelineCategory.ENCOUNTER -> Icons.Default.MedicalServices
    TimelineCategory.IMMUNIZATION -> Icons.Default.Vaccines
    else -> Icons.Default.Description
  }

  val spineRailColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("timeline_item_${item.id}"),
    verticalAlignment = Alignment.Top
  ) {
    // Spine Column with Continuous Rail and Node Icon
    Box(
      modifier = Modifier
        .width(44.dp)
        .fillMaxHeight()
        .drawBehind {
          val centerX = size.width / 2f
          val topY = if (isFirst) 20.dp.toPx() else 0f
          val bottomY = if (isLast) 20.dp.toPx() else size.height

          // Continuous vertical connecting spine
          drawLine(
            color = spineRailColor,
            start = Offset(centerX, topY),
            end = Offset(centerX, bottomY),
            strokeWidth = 2.dp.toPx()
          )
        },
      contentAlignment = Alignment.TopCenter
    ) {
      // Color-coded glowing category node
      Box(
        modifier = Modifier
          .padding(top = 10.dp)
          .size(28.dp)
          .clip(CircleShape)
          .background(nodeColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(nodeColor),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = categoryIcon,
            contentDescription = item.category.name,
            tint = Color.White,
            modifier = Modifier.size(11.dp)
          )
        }
      }
    }

    // Modern Tonal Card without harsh 1dp borders
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(16.dp),
      shadowElevation = 1.dp,
      tonalElevation = 2.dp,
      modifier = Modifier
        .weight(1f)
        .padding(start = 6.dp, end = 4.dp, bottom = 12.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              color = nodeColor.copy(alpha = 0.15f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = item.category.name,
                color = nodeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }

            Text(
              text = item.date,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          StatusBadge(status = item.status)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = item.title,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          letterSpacing = (-0.2).sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = item.subtitle,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        ProvenanceBadge(provenance = item.provenance)
      }
    }
  }
}

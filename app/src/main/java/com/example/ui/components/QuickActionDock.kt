package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Horizontal quick-action pill dock with soft tonal icon containers and spring press animations.
 */
@Composable
fun QuickActionDock(
  onConnectFhir: () -> Unit,
  onUploadOcr: () -> Unit,
  onAskAi: () -> Unit,
  onShareRecord: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(20.dp),
    tonalElevation = 2.dp,
    shadowElevation = 2.dp,
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      DockPillButton(
        label = "Connect",
        sublabel = "FHIR",
        icon = Icons.Default.Sync,
        containerColor = VitalTealContainerDark.copy(alpha = 0.5f),
        iconColor = VitalTealPrimaryDark,
        testTag = "action_connect_fhir",
        onClick = onConnectFhir
      )

      DockPillButton(
        label = "Upload",
        sublabel = "OCR Scan",
        icon = Icons.Default.UploadFile,
        containerColor = VitalEmeraldContainer.copy(alpha = 0.4f),
        iconColor = VitalEmeraldSuccess,
        testTag = "action_upload_ocr",
        onClick = onUploadOcr
      )

      DockPillButton(
        label = "Ask AI",
        sublabel = "Grounded",
        icon = Icons.Default.Psychology,
        containerColor = VitalGoldContainer.copy(alpha = 0.5f),
        iconColor = VitalGoldAccent,
        testTag = "action_ask_ai",
        onClick = onAskAi
      )

      DockPillButton(
        label = "Share",
        sublabel = "Token",
        icon = Icons.Default.Share,
        containerColor = IndigoNode.copy(alpha = 0.15f),
        iconColor = IndigoNode,
        testTag = "action_share_record",
        onClick = onShareRecord
      )
    }
  }
}

@Composable
private fun DockPillButton(
  label: String,
  sublabel: String,
  icon: ImageVector,
  containerColor: Color,
  iconColor: Color,
  testTag: String,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.90f else 1f,
    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
    label = "spring_press"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clip(RoundedCornerShape(14.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .testTag(testTag)
  ) {
    Box(
      modifier = Modifier
        .size(46.dp)
        .clip(CircleShape)
        .background(containerColor),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = iconColor,
        modifier = Modifier.size(22.dp)
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = label,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Text(
      text = sublabel,
      fontSize = 10.sp,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

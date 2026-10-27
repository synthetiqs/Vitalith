package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppScreen
import com.example.ui.theme.TealNode
import com.example.ui.theme.VitalTealPrimaryDark

/**
 * Modern floating pill bottom navigation bar with elevated shadow and animated selection pill.
 */
@Composable
fun FloatingNavBar(
  currentScreen: AppScreen,
  onSelectScreen: (AppScreen) -> Unit,
  tabs: List<AppScreen>,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
      tonalElevation = 8.dp,
      shadowElevation = 12.dp,
      modifier = Modifier
        .shadow(16.dp, shape = RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.25f))
        .fillMaxWidth()
        .testTag("vitalith_bottom_bar")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        tabs.forEach { screen ->
          val isSelected = currentScreen == screen
          FloatingNavItem(
            screen = screen,
            isSelected = isSelected,
            onClick = { onSelectScreen(screen) },
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
private fun FloatingNavItem(
  screen: AppScreen,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scale by animateFloatAsState(
    targetValue = if (isSelected) 1.05f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    label = "nav_scale"
  )

  val iconColor by animateColorAsState(
    targetValue = if (isSelected) TealNode else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    label = "icon_color"
  )

  val pillBackground by animateColorAsState(
    targetValue = if (isSelected) TealNode.copy(alpha = 0.15f) else Color.Transparent,
    label = "pill_bg"
  )

  val interactionSource = remember { MutableInteractionSource() }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clip(RoundedCornerShape(20.dp))
      .background(pillBackground)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .padding(vertical = 8.dp)
      .testTag("nav_tab_${screen.name.lowercase()}")
  ) {
    Icon(
      imageVector = screen.icon,
      contentDescription = screen.label,
      tint = iconColor,
      modifier = Modifier.size(20.dp)
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = screen.label,
      fontSize = 10.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    )
  }
}

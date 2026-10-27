package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activeThemeKey by ThemeManager.currentThemeKey.collectAsState()
  val activeTokens by ThemeManager.currentTokens.collectAsState()

  // Local display preferences
  var selectedDensity by remember { mutableStateOf("Comfortable") }
  var textScaleFactor by remember { mutableStateOf("100% (Standard)") }
  var cardCornerStyle by remember { mutableStateOf("Modern Rounded") }

  BackHandler {
    onNavigateBack()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Appearance & Themes",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
            Text(
              text = "Tokens: ${activeTokens.label} • Material 3",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("appearance_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Settings"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier
      .fillMaxSize()
      .testTag("appearance_settings_screen")
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Live Interactive Theme Preview
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.Palette, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(20.dp))
              Text(
                text = "Live Theme Canvas",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }
            Surface(
              color = VitalTealContainerDark.copy(alpha = 0.3f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = activeTokens.label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VitalTealPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }

          // Sample simulated Health Record Card
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(activeTokens.borderWidthCard, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = "Fasting Blood Glucose",
                    fontWeight = activeTokens.fontWeightDisplay,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Comprehensive Metabolic Panel • Oct 2026",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Surface(
                  color = VitalEmeraldContainer,
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = "OPTIMAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VitalEmeraldSuccess,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "5.1",
                  fontSize = 24.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "mmol/L",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(bottom = 3.dp)
                )
              }

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = { /* Demo */ },
                  colors = ButtonDefaults.buttonColors(containerColor = VitalTealPrimary),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text("Primary Action", fontSize = 11.sp, color = Color.White)
                }
                OutlinedButton(
                  onClick = { /* Demo */ },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text("Clinical Details", fontSize = 11.sp)
                }
              }
            }
          }
        }
      }

      var isThemesListExpanded by remember { mutableStateOf(true) }

      // 2. Curated Clinical Themes (9 Design Systems - Collapsible)
      Surface(
        onClick = { isThemesListExpanded = !isThemesListExpanded },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("appearance_themes_collapse_toggle")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Palette, contentDescription = null, tint = VitalTealPrimary, modifier = Modifier.size(18.dp))
            Column {
              Text(
                text = "CURATED CLINICAL THEMES (9 DESIGN SYSTEMS)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Active: ${activeTokens.label} • ${if (isThemesListExpanded) "Tap to collapse" else "Tap to expand theme list"}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Icon(
            imageVector = if (isThemesListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isThemesListExpanded) "Collapse Theme List" else "Expand Theme List",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      AnimatedVisibility(
        visible = isThemesListExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("appearance_themes_list")
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeManager.availableThemes.forEach { descriptor ->
              val isSelected = activeThemeKey == descriptor.key
              ThemeSelectionCard(
                descriptor = descriptor,
                isSelected = isSelected,
                onClick = {
                  ThemeManager.setTheme(descriptor.key, persist = true)
                }
              )
            }
          }
        }
      }

      // 3. Contrast & Auto-Match
      AppearanceSectionHeader("CONTRAST & SYSTEM INTEGRATION")

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          // High-Contrast Quick Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(text = "High Contrast Mode (WCAG AAA)", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
              Text(
                text = "Maximizes color contrast ratios and thickens borders for enhanced accessibility and visual clarity.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
              checked = activeThemeKey == "contrast",
              onCheckedChange = { enable ->
                if (enable) {
                  ThemeManager.setTheme("contrast", persist = true)
                } else {
                  ThemeManager.setTheme("original", persist = true)
                }
              },
              modifier = Modifier.testTag("toggle_high_contrast")
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // System Auto Match
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(text = "Match Android System Default", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
              Text(
                text = "Automatically synchronizes with your device's light or dark mode schedule.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
              onClick = {
                val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                ThemeManager.resetToSystem(isDark)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("appearance_reset_system_theme")
            ) {
              Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // 4. Display Density & Scaling
      AppearanceSectionHeader("LAYOUT DENSITY & TYPOGRAPHY")

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          // Layout Density
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Interface Density", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
              text = "Adjust padding between longitudinal records and biomarker lists.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("Comfortable", "Compact").forEach { opt ->
                FilterChip(
                  selected = selectedDensity == opt,
                  onClick = { selectedDensity = opt },
                  label = { Text(opt, fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VitalTealPrimary,
                    selectedLabelColor = Color.White
                  )
                )
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // Text Scaling
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Clinical Font Scaling", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
              text = "Adjust typographic scale for biomarker values and medication instructions.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("100% (Standard)", "110% (Enhanced)", "125% (Large)").forEach { opt ->
                FilterChip(
                  selected = textScaleFactor == opt,
                  onClick = { textScaleFactor = opt },
                  label = { Text(opt, fontSize = 10.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VitalTealPrimary,
                    selectedLabelColor = Color.White
                  )
                )
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // Corner Radii
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Card Corner Geometry", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
              text = "Card corner curvature for laboratory and medication tiles.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("Modern Rounded", "Classic (10dp)", "Minimalist").forEach { opt ->
                FilterChip(
                  selected = cardCornerStyle == opt,
                  onClick = { cardCornerStyle = opt },
                  label = { Text(opt, fontSize = 10.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VitalTealPrimary,
                    selectedLabelColor = Color.White
                  )
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ThemeSelectionCard(
  descriptor: ThemeDescriptor,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val tokens = descriptor.tokens
  val borderColor = if (isSelected) VitalTealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
  val backgroundColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = backgroundColor,
    border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("theme_card_${descriptor.key}")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 3 Color Dots (Accent, NodeTeal, SwatchBg)
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
          modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(descriptor.swatchAccent)
        )
        Box(
          modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(tokens.nodeTeal)
        )
        Box(
          modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(descriptor.swatchBg)
            .border(0.5.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = descriptor.label,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.5.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Surface(
            color = if (descriptor.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = if (descriptor.isDark) "Dark" else "Light",
              fontSize = 9.sp,
              fontWeight = FontWeight.Medium,
              color = if (descriptor.isDark) Color(0xFF94A3B8) else Color(0xFF475569),
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }
        val themeDescription = when (descriptor.key) {
          "original" -> "High-tech navy with cyan highlights"
          "plum" -> "Deep plum with soothing sage tones"
          "forest" -> "Earthy emerald with warm amber"
          "mono" -> "Clean OLED black and high-contrast white"
          "indigo" -> "Deep midnight with royal indigo violet"
          "sandstone" -> "Warm terracotta and organic clay"
          "arctic" -> "Crisp daylight ice with azure blue"
          "rose" -> "Soft blush with rose and charcoal"
          "contrast" -> "WCAG AAA 7:1+ pure black and safety yellow"
          else -> if (descriptor.isDark) "Curated dark clinical palette" else "Curated light clinical palette"
        }
        Text(
          text = themeDescription,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      RadioButton(
        selected = isSelected,
        onClick = onClick,
        colors = RadioButtonDefaults.colors(selectedColor = VitalTealPrimary),
        modifier = Modifier.size(24.dp)
      )
    }
  }
}

@Composable
private fun AppearanceSectionHeader(title: String) {
  Text(
    text = title,
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp,
    color = VitalTealPrimary,
    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
  )
}

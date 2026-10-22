package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = VitalTealPrimaryDark,
    onPrimary = VitalNavyDark,
    primaryContainer = VitalTealContainerDark,
    onPrimaryContainer = VitalTealContainer,
    secondary = VitalCyanAccent,
    onSecondary = VitalNavyDark,
    background = VitalNavyDark,
    onBackground = Color(0xFFF1F5F9),
    surface = VitalNavy,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = VitalNavySurface,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334E68),
    error = VitalAlertRed,
    errorContainer = VitalAlertContainer,
    onErrorContainer = VitalOnAlertContainer
  )

private val LightColorScheme =
  lightColorScheme(
    primary = VitalTealPrimary,
    onPrimary = Color.White,
    primaryContainer = VitalTealContainer,
    onPrimaryContainer = VitalOnTealContainer,
    secondary = VitalNavy,
    onSecondary = Color.White,
    background = VitalSlateBg,
    onBackground = VitalTextPrimary,
    surface = VitalSurfaceCard,
    onSurface = VitalTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = VitalTextSecondary,
    outline = VitalBorder,
    error = VitalAlertRed,
    errorContainer = VitalAlertContainer,
    onErrorContainer = VitalOnAlertContainer
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use Vitalith health-tech brand identity
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

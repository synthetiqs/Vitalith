package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MyApplicationTheme(
  themeManager: ThemeManager = ThemeManager,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current

  // Ensure ThemeManager is initialized with system color scheme fallback
  LaunchedEffect(darkTheme) {
    themeManager.init(context, darkTheme)
  }

  val currentTokens by themeManager.currentTokens.collectAsStateWithLifecycle()
  val colorScheme = currentTokens.toColorScheme()

  CompositionLocalProvider(
    LocalVitalithThemeTokens provides currentTokens
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}

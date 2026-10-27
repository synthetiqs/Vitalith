package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import com.example.data.preferences.ThemePreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single source of truth for the Vitalith multi-theme design token system.
 * Manages all 9 themes, dark/light mode toggling, persistence via Jetpack DataStore
 * and SharedPreferences under key "app-theme", and instant reactive dispatch
 * to Jetpack Compose without activity recreation.
 */
object ThemeManager {
  const val STORAGE_KEY = "app-theme"
  private const val PREFS_NAME = "vitalith_theme_prefs"

  private var prefs: SharedPreferences? = null
  private var appContext: Context? = null
  private val scope = CoroutineScope(Dispatchers.IO)
  private var initJob: kotlinx.coroutines.Job? = null

  private var lastDarkThemeKey = "original"
  private var lastLightThemeKey = "arctic"

  private val _currentThemeKey = MutableStateFlow("original")
  val currentThemeKey: StateFlow<String> = _currentThemeKey.asStateFlow()

  private val _currentTokens = MutableStateFlow(OriginalTokens)
  val currentTokens: StateFlow<VitalithThemeTokens> = _currentTokens.asStateFlow()

  val availableThemes: List<ThemeDescriptor> = ALL_THEMES

  val isDarkMode: Boolean
    get() = _currentTokens.value.isDark

  /**
   * Initializes theme preferences on app startup.
   * If no preference was stored, defaults to system color scheme:
   * dark -> "original", light -> "arctic"
   */
  fun init(context: Context, isSystemDark: Boolean) {
    appContext = context.applicationContext
    prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val saved = prefs?.getString(STORAGE_KEY, null)
    val initialKey = if (saved != null && ALL_THEMES.any { it.key == saved }) {
      saved
    } else {
      if (isSystemDark) "original" else "arctic"
    }

    setTheme(initialKey, persist = false, context = context)

    // Load and sync from DataStore asynchronously if not already in SharedPreferences
    val ctx = appContext ?: return
    initJob?.cancel()
    initJob = scope.launch {
      if (prefs?.contains(STORAGE_KEY) == false) {
        val dataStoreKey = ThemePreferencesDataStore.getSavedThemeKey(ctx)
        val dataStoreDarkMode = ThemePreferencesDataStore.getSavedDarkMode(ctx)

        if (dataStoreKey != null && ALL_THEMES.any { it.key == dataStoreKey }) {
          setTheme(dataStoreKey, persist = false, context = ctx)
        } else if (dataStoreDarkMode != null) {
          val targetKey = if (dataStoreDarkMode) lastDarkThemeKey else lastLightThemeKey
          setTheme(targetKey, persist = false, context = ctx)
        }
      }
    }
  }

  /**
   * Updates the active theme key and tokens.
   * Persists immediately to SharedPreferences and Jetpack DataStore.
   */
  fun setTheme(key: String, persist: Boolean = true, context: Context? = null) {
    if (persist) {
      initJob?.cancel()
      initJob = null
    }

    val descriptor = ALL_THEMES.find { it.key == key } ?: ALL_THEMES.first()
    _currentThemeKey.value = descriptor.key
    _currentTokens.value = descriptor.tokens

    if (descriptor.isDark) {
      lastDarkThemeKey = descriptor.key
    } else {
      lastLightThemeKey = descriptor.key
    }

    if (persist) {
      prefs?.edit()?.putString(STORAGE_KEY, descriptor.key)?.commit()

      val targetContext = context?.applicationContext ?: appContext
      if (targetContext != null) {
        scope.launch {
          ThemePreferencesDataStore.saveThemeKey(targetContext, descriptor.key)
          ThemePreferencesDataStore.saveDarkMode(targetContext, descriptor.isDark)
        }
      }
    }
  }

  /**
   * Toggles or sets dark mode directly, persisting the preference in Jetpack DataStore.
   * If switched to dark, restores the last active dark theme (default: Original Navy).
   * If switched to light, restores the last active light theme (default: Arctic Ice).
   */
  fun setDarkMode(isDark: Boolean, context: Context? = null) {
    val targetContext = context?.applicationContext ?: appContext
    val targetKey = if (isDark) {
      if (lastDarkThemeKey.isNotBlank()) lastDarkThemeKey else "original"
    } else {
      if (lastLightThemeKey.isNotBlank()) lastLightThemeKey else "arctic"
    }

    setTheme(targetKey, persist = true, context = targetContext)

    if (targetContext != null) {
      scope.launch {
        ThemePreferencesDataStore.saveDarkMode(targetContext, isDark)
        ThemePreferencesDataStore.saveThemeKey(targetContext, targetKey)
      }
    }
  }

  /**
   * Clears saved theme and restores automatic system preference matching:
   * dark -> "original", light -> "arctic"
   */
  fun resetToSystem(isSystemDark: Boolean, context: Context? = null) {
    initJob?.cancel()
    initJob = null
    prefs?.edit()?.remove(STORAGE_KEY)?.commit()
    val fallback = if (isSystemDark) "original" else "arctic"
    setTheme(fallback, persist = false, context = context)

    val targetContext = context?.applicationContext ?: appContext
    if (targetContext != null) {
      scope.launch {
        ThemePreferencesDataStore.saveDarkMode(targetContext, isSystemDark)
        ThemePreferencesDataStore.saveThemeKey(targetContext, fallback)
      }
    }
  }

  fun hasSavedTheme(): Boolean {
    return prefs?.contains(STORAGE_KEY) == true
  }
}

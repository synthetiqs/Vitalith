package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "vitalith_theme_preferences")

object ThemePreferencesDataStore {

  val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
  val KEY_THEME_KEY = stringPreferencesKey("theme_key")

  fun isDarkModeFlow(context: Context): Flow<Boolean?> {
    return context.themeDataStore.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { preferences ->
        preferences[KEY_IS_DARK_MODE]
      }
  }

  suspend fun getSavedDarkMode(context: Context): Boolean? {
    return try {
      context.themeDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_IS_DARK_MODE] }
        .first()
    } catch (_: Exception) {
      null
    }
  }

  suspend fun saveDarkMode(context: Context, isDark: Boolean) {
    try {
      context.themeDataStore.edit { preferences ->
        preferences[KEY_IS_DARK_MODE] = isDark
      }
    } catch (_: Exception) {}
  }

  fun themeKeyFlow(context: Context): Flow<String?> {
    return context.themeDataStore.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { preferences ->
        preferences[KEY_THEME_KEY]
      }
  }

  suspend fun getSavedThemeKey(context: Context): String? {
    return try {
      context.themeDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_THEME_KEY] }
        .first()
    } catch (_: Exception) {
      null
    }
  }

  suspend fun saveThemeKey(context: Context, key: String) {
    try {
      context.themeDataStore.edit { preferences ->
        preferences[KEY_THEME_KEY] = key
      }
    } catch (_: Exception) {}
  }
}

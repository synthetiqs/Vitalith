package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for clinical patient age calculations and Date of Birth formatting.
 * Computes patient age in whole years (or infant status) from a given birth date string.
 */
object PatientAgeCalculator {

  private val DATE_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")

  /**
   * Extracts clean 'YYYY-MM-DD' substring from date string containing age annotations
   * e.g. "1995-05-15 (31 yrs)" -> "1995-05-15"
   */
  fun extractDateOnly(input: String?): String {
    if (input.isNullOrBlank()) return ""
    val match = DATE_REGEX.find(input)
    return match?.value ?: input.take(10).trim()
  }

  /**
   * Calculates age in completed years between birth date and the given reference date (defaults to today).
   * Returns null if string cannot be parsed as a valid date.
   */
  fun calculateAgeYears(dobStr: String?, referenceDate: Calendar = Calendar.getInstance()): Int? {
    if (dobStr.isNullOrBlank()) return null
    val cleanDate = extractDateOnly(dobStr)
    return try {
      val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
      }
      val birthDate = sdf.parse(cleanDate) ?: return null
      val birthCal = Calendar.getInstance().apply { time = birthDate }

      var years = referenceDate.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)

      // Compare day of year to see if birthday has occurred yet this year
      val refMonth = referenceDate.get(Calendar.MONTH)
      val birthMonth = birthCal.get(Calendar.MONTH)
      val refDay = referenceDate.get(Calendar.DAY_OF_MONTH)
      val birthDay = birthCal.get(Calendar.DAY_OF_MONTH)

      if (refMonth < birthMonth || (refMonth == birthMonth && refDay < birthDay)) {
        years--
      }

      if (years >= 0) years else 0
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Formats a human-readable clinical age description.
   * e.g., 0 -> "Infant (< 1 yr)", 1 -> "1 yr old", 31 -> "31 yrs old"
   */
  fun formatAgeLabel(years: Int?): String {
    return when {
      years == null -> "Age undetermined"
      years == 0 -> "Infant (< 1 year old)"
      years == 1 -> "1 year old (Toddler)"
      else -> "$years years old"
    }
  }

  /**
   * Formats full DOB with age suffix for storage and display:
   * e.g. "1995-05-15 (31 yrs)" or "2026-03-10 (0 yrs)"
   */
  fun formatDobWithAge(dateInput: String, years: Int? = calculateAgeYears(dateInput)): String {
    val cleanDate = extractDateOnly(dateInput)
    return if (cleanDate.isNotBlank()) {
      if (years != null) {
        "$cleanDate ($years yrs)"
      } else {
        cleanDate
      }
    } else {
      dateInput.trim()
    }
  }

  /**
   * Converts a YYYY-MM-DD string into UTC epoch milliseconds for Jetpack Compose DatePickerState.
   */
  fun parseDateToUtcMillis(dateStr: String?): Long? {
    if (dateStr.isNullOrBlank()) return null
    val clean = extractDateOnly(dateStr)
    return try {
      val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
      }
      sdf.parse(clean)?.time
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Formats UTC milliseconds from Jetpack Compose DatePicker into 'YYYY-MM-DD'.
   */
  fun formatUtcMillisToDateString(millis: Long): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = millis
    }
    return String.format(
      Locale.US,
      "%04d-%02d-%02d",
      calendar.get(Calendar.YEAR),
      calendar.get(Calendar.MONTH) + 1,
      calendar.get(Calendar.DAY_OF_MONTH)
    )
  }
}

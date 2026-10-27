package com.example.util

import com.example.data.model.*
import java.util.Locale
import kotlin.math.roundToInt

object UcumUnitConverter {

  // Mathematical & Clinical Conversion Constants
  const val GLUCOSE_MOLAR_MASS_FACTOR = 18.0182 // 1 mmol/L = 18.0182 mg/dL
  const val CHOLESTEROL_FACTOR = 38.67 // 1 mmol/L = 38.67 mg/dL
  const val LBS_PER_KG = 2.20462262185
  const val CM_PER_INCH = 2.54

  // --- Blood Glucose Conversions ---
  fun mgDlToMmolL(mgDl: Double): Double = mgDl / GLUCOSE_MOLAR_MASS_FACTOR
  fun mmolLToMgDl(mmolL: Double): Double = mmolL * GLUCOSE_MOLAR_MASS_FACTOR

  // --- Temperature Conversions ---
  fun celsiusToFahrenheit(celsius: Double): Double = (celsius * 9.0 / 5.0) + 32.0
  fun fahrenheitToCelsius(fahrenheit: Double): Double = (fahrenheit - 32.0) * 5.0 / 9.0

  // --- Weight Conversions ---
  fun lbsToKg(lbs: Double): Double = lbs / LBS_PER_KG
  fun kgToLbs(kg: Double): Double = kg * LBS_PER_KG

  // --- Height Conversions ---
  fun cmToInches(cm: Double): Double = cm / CM_PER_INCH
  fun inchesToCm(inches: Double): Double = inches * CM_PER_INCH

  fun cmToFeetAndInches(cm: Double): Pair<Int, Int> {
    val totalInches = cmToInches(cm).roundToInt()
    val feet = totalInches / 12
    val inches = totalInches % 12
    return Pair(feet, inches)
  }

  fun feetAndInchesToCm(feet: Int, inches: Int): Double {
    val totalInches = (feet * 12) + inches
    return inchesToCm(totalInches.toDouble())
  }

  // --- Cholesterol Conversions ---
  fun cholesterolMgDlToMmolL(mgDl: Double): Double = mgDl / CHOLESTEROL_FACTOR
  fun cholesterolMmolLToMgDl(mmolL: Double): Double = mmolL * CHOLESTEROL_FACTOR

  /**
   * Structure holding localized display info alongside computable UCUM metadata
   */
  data class LocalizedMeasurement(
    val displayString: String,
    val displayValue: Double,
    val displayUnit: String,
    val computableValue: Double,
    val ucumCode: String,
    val wasConverted: Boolean
  )

  /**
   * Attempts to parse a numeric value and unit suffix from a clinical string
   */
  fun parseRawQuantity(text: String): Pair<Double, String>? {
    val trimmed = text.trim()
    val regex = """^([0-9]+(?:\.[0-9]+)?)\s*(.*)$""".toRegex()
    val match = regex.find(trimmed) ?: return null
    val (numStr, unitStr) = match.destructured
    val num = numStr.toDoubleOrNull() ?: return null
    return Pair(num, unitStr.trim())
  }

  /**
   * Translates a glucose value according to user preferences
   */
  fun formatGlucose(rawText: String, targetUnit: GlucoseUnit): LocalizedMeasurement {
    val parsed = parseRawQuantity(rawText)
    if (parsed == null) {
      return LocalizedMeasurement(rawText, 0.0, targetUnit.label, 0.0, targetUnit.ucumCode, false)
    }
    val (value, unit) = parsed
    val isMmolInput = unit.contains("mmol", ignoreCase = true)
    val isMgDlInput = unit.contains("mg", ignoreCase = true) || (!isMmolInput && value < 30.0)

    val computableMmol = if (isMmolInput) value else mgDlToMmolL(value)
    val computableMgDl = if (isMmolInput) mmolLToMgDl(value) else value

    return when (targetUnit) {
      GlucoseUnit.MG_DL -> {
        val displayVal = computableMgDl
        val formatted = "${displayVal.roundToInt()} mg/dL"
        LocalizedMeasurement(
          displayString = formatted,
          displayValue = displayVal,
          displayUnit = "mg/dL",
          computableValue = computableMmol,
          ucumCode = "mmol/L", // Canonical SI computable UCUM code
          wasConverted = isMmolInput
        )
      }
      GlucoseUnit.MMOL_L -> {
        val displayVal = computableMmol
        val formatted = String.format(Locale.US, "%.1f mmol/L", displayVal)
        LocalizedMeasurement(
          displayString = formatted,
          displayValue = displayVal,
          displayUnit = "mmol/L",
          computableValue = computableMmol,
          ucumCode = "mmol/L",
          wasConverted = isMgDlInput
        )
      }
    }
  }

  /**
   * Translates a temperature reading according to user preferences
   */
  fun formatTemperature(rawText: String, targetUnit: TemperatureUnit): LocalizedMeasurement {
    val parsed = parseRawQuantity(rawText)
    if (parsed == null) {
      return LocalizedMeasurement(rawText, 0.0, targetUnit.label, 0.0, targetUnit.ucumCode, false)
    }
    val (value, unit) = parsed
    val isCelsiusInput = unit.contains("c", ignoreCase = true) || (!unit.contains("f", ignoreCase = true) && value < 45.0)

    val celsiusVal = if (isCelsiusInput) value else fahrenheitToCelsius(value)
    val fahrenheitVal = if (isCelsiusInput) celsiusToFahrenheit(value) else value

    return when (targetUnit) {
      TemperatureUnit.FAHRENHEIT -> {
        val formatted = String.format(Locale.US, "%.1f °F", fahrenheitVal)
        LocalizedMeasurement(
          displayString = formatted,
          displayValue = fahrenheitVal,
          displayUnit = "°F",
          computableValue = celsiusVal,
          ucumCode = "Cel", // Canonical UCUM temperature
          wasConverted = isCelsiusInput
        )
      }
      TemperatureUnit.CELSIUS -> {
        val formatted = String.format(Locale.US, "%.1f °C", celsiusVal)
        LocalizedMeasurement(
          displayString = formatted,
          displayValue = celsiusVal,
          displayUnit = "°C",
          computableValue = celsiusVal,
          ucumCode = "Cel",
          wasConverted = !isCelsiusInput
        )
      }
    }
  }

  /**
   * Translates a body weight reading according to user preferences
   */
  fun formatWeight(rawText: String, targetUnit: WeightUnit): LocalizedMeasurement {
    val parsed = parseRawQuantity(rawText)
    if (parsed == null) {
      return LocalizedMeasurement(rawText, 0.0, targetUnit.label, 0.0, targetUnit.ucumCode, false)
    }
    val (value, unit) = parsed
    val isKgInput = unit.contains("kg", ignoreCase = true) || (!unit.contains("lb", ignoreCase = true) && value < 110.0)

    val kgVal = if (isKgInput) value else lbsToKg(value)
    val lbsVal = if (isKgInput) kgToLbs(value) else value

    return when (targetUnit) {
      WeightUnit.LBS -> {
        val formatted = String.format(Locale.US, "%.1f lbs", lbsVal)
        LocalizedMeasurement(
          displayString = formatted,
          displayValue = lbsVal,
          displayUnit = "lbs",
          computableValue = kgVal,
          ucumCode = "kg", // Canonical UCUM mass
          wasConverted = isKgInput
        )
      }
      WeightUnit.KG -> {
        val formatted = String.format(Locale.US, "%.1f kg", kgVal)
        LocalizedMeasurement(
          displayString = formatted,
          displayValue = kgVal,
          displayUnit = "kg",
          computableValue = kgVal,
          ucumCode = "kg",
          wasConverted = !isKgInput
        )
      }
    }
  }

  /**
   * Translates cholesterol metrics (Total Chol, LDL, HDL, Triglycerides)
   */
  fun formatCholesterol(rawText: String, targetUnit: CholesterolUnit): LocalizedMeasurement {
    val parsed = parseRawQuantity(rawText)
    if (parsed == null) {
      return LocalizedMeasurement(rawText, 0.0, targetUnit.label, 0.0, targetUnit.ucumCode, false)
    }
    val (value, unit) = parsed
    val isMmolInput = unit.contains("mmol", ignoreCase = true) || (!unit.contains("mg", ignoreCase = true) && value < 20.0)

    val mmolVal = if (isMmolInput) value else cholesterolMgDlToMmolL(value)
    val mgDlVal = if (isMmolInput) cholesterolMmolLToMgDl(value) else value

    return when (targetUnit) {
      CholesterolUnit.MG_DL -> {
        val formatted = "${mgDlVal.roundToInt()} mg/dL"
        LocalizedMeasurement(
          displayString = formatted,
          displayValue = mgDlVal,
          displayUnit = "mg/dL",
          computableValue = mmolVal,
          ucumCode = "mmol/L",
          wasConverted = isMmolInput
        )
      }
      CholesterolUnit.MMOL_L -> {
        val formatted = String.format(Locale.US, "%.2f mmol/L", mmolVal)
        LocalizedMeasurement(
          displayString = formatted,
          displayValue = mmolVal,
          displayUnit = "mmol/L",
          computableValue = mmolVal,
          ucumCode = "mmol/L",
          wasConverted = !isMmolInput
        )
      }
    }
  }

  /**
   * Transforms a key-value entry (like "Total Chol" -> "188 mg/dL") into the user's localized unit
   */
  fun formatLocalizedKeyValue(
    key: String,
    rawValue: String,
    preferences: UnitPreferences
  ): LocalizedMeasurement {
    val lowerKey = key.lowercase()
    return when {
      lowerKey.contains("glucose") || lowerKey.contains("blood sugar") -> {
        formatGlucose(rawValue, preferences.glucoseUnit)
      }
      lowerKey.contains("temp") -> {
        formatTemperature(rawValue, preferences.temperatureUnit)
      }
      lowerKey.contains("weight") || lowerKey.contains("mass") -> {
        formatWeight(rawValue, preferences.weightUnit)
      }
      lowerKey.contains("chol") || lowerKey.contains("ldl") || lowerKey.contains("hdl") || lowerKey.contains("lipid") -> {
        formatCholesterol(rawValue, preferences.cholesterolUnit)
      }
      else -> {
        LocalizedMeasurement(
          displayString = rawValue,
          displayValue = parseRawQuantity(rawValue)?.first ?: 0.0,
          displayUnit = parseRawQuantity(rawValue)?.second ?: "",
          computableValue = parseRawQuantity(rawValue)?.first ?: 0.0,
          ucumCode = parseRawQuantity(rawValue)?.second ?: "",
          wasConverted = false
        )
      }
    }
  }

  /**
   * Helper to create a computable UCUM quantity from raw input and category
   */
  fun createComputableQuantity(
    rawText: String,
    category: String
  ): UcumQuantity {
    val parsed = parseRawQuantity(rawText) ?: return UcumQuantity(0.0, rawText, rawText)
    val (valNum, unit) = parsed

    return when {
      category.contains("glucose", ignoreCase = true) -> {
        if (unit.contains("mg", ignoreCase = true)) {
          UcumQuantity(value = valNum, unit = "mg/dL", code = "mg/dL")
        } else {
          UcumQuantity(value = valNum, unit = "mmol/L", code = "mmol/L")
        }
      }
      category.contains("temp", ignoreCase = true) -> {
        if (unit.contains("c", ignoreCase = true)) {
          UcumQuantity(value = valNum, unit = "Cel", code = "Cel")
        } else {
          UcumQuantity(value = valNum, unit = "[degF]", code = "[degF]")
        }
      }
      category.contains("weight", ignoreCase = true) -> {
        if (unit.contains("kg", ignoreCase = true)) {
          UcumQuantity(value = valNum, unit = "kg", code = "kg")
        } else {
          UcumQuantity(value = valNum, unit = "[lb_av]", code = "[lb_av]")
        }
      }
      else -> {
        UcumQuantity(value = valNum, unit = unit, code = unit)
      }
    }
  }
}

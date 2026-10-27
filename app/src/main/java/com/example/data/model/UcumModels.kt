package com.example.data.model

/**
 * Standard Clinical Unit System Presets
 */
enum class UnitSystem(
  val displayName: String,
  val description: String
) {
  US_CUSTOMARY(
    displayName = "US Customary",
    description = "mg/dL (Glucose/Lipids) • °F (Temp) • lbs (Weight) • ft/in (Height)"
  ),
  METRIC_SI(
    displayName = "Metric / International (SI)",
    description = "mmol/L (Glucose/Lipids) • °C (Temp) • kg (Weight) • cm (Height)"
  )
}

/**
 * Blood Glucose Measurement Units (Standard Clinical & UCUM codes)
 * UCUM system: "http://unitsofmeasure.org"
 */
enum class GlucoseUnit(
  val label: String,
  val ucumCode: String,
  val system: String = "http://unitsofmeasure.org"
) {
  MG_DL("mg/dL", "mg/dL"),
  MMOL_L("mmol/L", "mmol/L")
}

/**
 * Body Temperature Units
 * In UCUM: Cel = Celsius, [degF] = Fahrenheit
 */
enum class TemperatureUnit(
  val label: String,
  val ucumCode: String,
  val symbol: String,
  val system: String = "http://unitsofmeasure.org"
) {
  FAHRENHEIT("°F", "[degF]", "°F"),
  CELSIUS("°C", "Cel", "°C")
}

/**
 * Body Weight / Mass Units
 * In UCUM: [lb_av] = pound (avoirdupois), kg = kilogram
 */
enum class WeightUnit(
  val label: String,
  val ucumCode: String,
  val system: String = "http://unitsofmeasure.org"
) {
  LBS("lbs", "[lb_av]"),
  KG("kg", "kg")
}

/**
 * Height / Length Units
 * In UCUM: [in_i] = inch (international), cm = centimeter
 */
enum class HeightUnit(
  val label: String,
  val ucumCode: String,
  val system: String = "http://unitsofmeasure.org"
) {
  FT_IN("ft / in", "[in_i]"),
  CM("cm", "cm")
}

/**
 * Lipid / Cholesterol Units (Total Cholesterol, LDL, HDL, Triglycerides)
 */
enum class CholesterolUnit(
  val label: String,
  val ucumCode: String,
  val system: String = "http://unitsofmeasure.org"
) {
  MG_DL("mg/dL", "mg/dL"),
  MMOL_L("mmol/L", "mmol/L")
}

/**
 * User-configurable Unit Preferences persisted with the user profile
 */
data class UnitPreferences(
  val unitSystem: UnitSystem = UnitSystem.METRIC_SI,
  val glucoseUnit: GlucoseUnit = GlucoseUnit.MMOL_L,
  val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
  val weightUnit: WeightUnit = WeightUnit.KG,
  val heightUnit: HeightUnit = HeightUnit.CM,
  val cholesterolUnit: CholesterolUnit = CholesterolUnit.MMOL_L
) {
  companion object {
    fun createUsCustomary() = UnitPreferences(
      unitSystem = UnitSystem.US_CUSTOMARY,
      glucoseUnit = GlucoseUnit.MG_DL,
      temperatureUnit = TemperatureUnit.FAHRENHEIT,
      weightUnit = WeightUnit.LBS,
      heightUnit = HeightUnit.FT_IN,
      cholesterolUnit = CholesterolUnit.MG_DL
    )

    fun createMetricSi() = UnitPreferences(
      unitSystem = UnitSystem.METRIC_SI,
      glucoseUnit = GlucoseUnit.MMOL_L,
      temperatureUnit = TemperatureUnit.CELSIUS,
      weightUnit = WeightUnit.KG,
      heightUnit = HeightUnit.CM,
      cholesterolUnit = CholesterolUnit.MMOL_L
    )
  }
}

/**
 * Standard HL7 FHIR / UCUM Computable Quantity Representation
 * Used for lossless, interoperable storage of clinical measures.
 */
data class UcumQuantity(
  val value: Double,
  val unit: String,
  val code: String,
  val system: String = "http://unitsofmeasure.org"
) {
  fun toFhirJsonSnippet(): String {
    return """
      {
        "value": $value,
        "unit": "$unit",
        "system": "$system",
        "code": "$code"
      }
    """.trimIndent()
  }

  fun toFormattedString(): String {
    val formattedVal = if (value % 1.0 == 0.0) {
      value.toInt().toString()
    } else {
      String.format(java.util.Locale.US, "%.1f", value)
    }
    return "$formattedVal $unit"
  }
}

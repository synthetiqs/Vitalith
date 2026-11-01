package com.example.data.model

/**
 * Supported health wearable cloud platforms & sync providers.
 */
enum class WearablePlatform(val displayName: String, val brandSubtitle: String) {
  GOOGLE_FIT("Google Fit", "Google Health Connect & Android OS"),
  ZEPP_HEALTH("Zepp Health", "Amazfit Smartwatches & BioTracker PPG")
}

/**
 * Common health wearable device manufacturers and hardware types.
 */
enum class WearableDeviceType(val displayName: String, val defaultManufacturer: String, val platform: WearablePlatform = WearablePlatform.GOOGLE_FIT) {
  PIXEL_WATCH("Google Pixel Watch", "Google", WearablePlatform.GOOGLE_FIT),
  FITBIT("Fitbit Tracker / Sense", "Google / Fitbit", WearablePlatform.GOOGLE_FIT),
  GALAXY_WATCH("Samsung Galaxy Watch", "Samsung", WearablePlatform.GOOGLE_FIT),
  GARMIN("Garmin Watch", "Garmin", WearablePlatform.GOOGLE_FIT),
  WEAR_OS_GENERIC("Wear OS Smartwatch", "Wear OS", WearablePlatform.GOOGLE_FIT),
  AMAZFIT_BALANCE("Amazfit Balance", "Zepp Health", WearablePlatform.ZEPP_HEALTH),
  AMAZFIT_TREX("Amazfit T-Rex Ultra", "Zepp Health", WearablePlatform.ZEPP_HEALTH),
  AMAZFIT_CHEETAH("Amazfit Cheetah Pro", "Zepp Health", WearablePlatform.ZEPP_HEALTH),
  AMAZFIT_ACTIVE("Amazfit Active", "Zepp Health", WearablePlatform.ZEPP_HEALTH),
  AMAZFIT_HELIO("Amazfit Helio Ring", "Zepp Health", WearablePlatform.ZEPP_HEALTH),
  AMAZFIT_GTR("Amazfit GTR 4", "Zepp Health", WearablePlatform.ZEPP_HEALTH)
}

/**
 * Hardware wearable paired or synced via Google Fit API / Zepp Health Open Platform.
 */
data class WearableDevice(
  val id: String,
  val name: String,
  val type: WearableDeviceType,
  val manufacturer: String,
  val model: String,
  val batteryPercent: Int,
  val isConnected: Boolean,
  val lastSyncedTimestamp: Long?,
  val deviceIdentifier: String,
  val syncFrequencyMinutes: Int = 15,
  val firmwareVersion: String = "v3.14.8",
  val platform: WearablePlatform = WearablePlatform.GOOGLE_FIT,
  val sensorInfo: String = "BioTracker™ Optical PPG"
) {
  val formattedLastSync: String
    get() {
      if (lastSyncedTimestamp == null) return "Never synced"
      val diffMillis = System.currentTimeMillis() - lastSyncedTimestamp
      val minutes = diffMillis / (1000 * 60)
      return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        else -> "${minutes / 1440}d ago"
      }
    }
}

/**
 * Hourly step sample bucket for daily activity bar charts.
 */
data class HourlyStepBucket(
  val hour: Int, // 0..23
  val steps: Int
) {
  val label: String
    get() {
      val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
      val amPm = if (hour < 12) "a" else "p"
      return "$h$amPm"
    }
}

/**
 * Aggregated daily physical activity data from Google Fit API / Zepp Health Open Platform.
 */
data class DailyActivityData(
  val date: String, // e.g. "2026-10-31"
  val stepsCount: Int,
  val stepGoal: Int = 10000,
  val caloriesBurnedKcal: Int,
  val activeMinutes: Int,
  val activeMinutesGoal: Int = 45,
  val distanceMeters: Float,
  val floorsClimbed: Int,
  val hourlySteps: List<HourlyStepBucket> = emptyList(),
  val paiScore: Int? = null
) {
  val stepProgressPercent: Float
    get() = if (stepGoal > 0) (stepsCount.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1.5f) else 0f

  val distanceKm: Float
    get() = distanceMeters / 1000f

  val distanceMiles: Float
    get() = distanceMeters * 0.000621371f
}

/**
 * Individual heart rate data point recorded by optical PPG sensor.
 */
data class HeartRateSample(
  val timestamp: Long,
  val timeFormatted: String, // e.g. "14:30"
  val bpm: Int,
  val context: String = "Resting" // "Resting", "Walking", "Exercise", "Sleep"
)

/**
 * Distribution of time spent in physiological training zones.
 */
data class HeartRateZones(
  val restMinutes: Int, // < 60% max HR (< 100 bpm)
  val fatBurnMinutes: Int, // 60% - 70% max HR (100-120 bpm)
  val cardioMinutes: Int, // 70% - 85% max HR (120-150 bpm)
  val peakMinutes: Int // > 85% max HR (> 150 bpm)
) {
  val totalTrackedMinutes: Int
    get() = restMinutes + fatBurnMinutes + cardioMinutes + peakMinutes
}

/**
 * Aggregated heart rate telemetry for a given day from Google Fit / Zepp BioTracker.
 */
data class HeartRateData(
  val date: String,
  val restingHeartRateBpm: Int,
  val minHeartRateBpm: Int,
  val maxHeartRateBpm: Int,
  val averageHeartRateBpm: Int,
  val heartRateVariabilityMs: Int, // RMSSD in milliseconds
  val samples: List<HeartRateSample> = emptyList(),
  val zones: HeartRateZones = HeartRateZones(360, 45, 25, 10),
  val clinicalStatus: ItemStatus = ItemStatus.NORMAL
)

/**
 * Google Fit OAuth and sync connection state.
 */
enum class GoogleFitConnectionState {
  DISCONNECTED,
  CONNECTING,
  AUTHENTICATED_CONNECTED,
  SYNC_ERROR
}

/**
 * Zepp Health (Amazfit) Open Platform OAuth & sync connection state.
 */
enum class ZeppConnectionState {
  DISCONNECTED,
  CONNECTING,
  AUTHENTICATED_CONNECTED,
  SYNC_ERROR
}

/**
 * Zepp Health & Amazfit specific biometrics (PAI, BioTracker PPG, Readiness).
 */
data class ZeppBiometrics(
  val paiScore: Int = 112, // Personal Activity Intelligence score
  val paiGoal: Int = 100,
  val paiWeeklyTotal: Int = 124,
  val readinessScore: Int = 89, // Physical & Mental readiness (0-100)
  val spo2Percent: Int = 98,
  val stressLevel: Int = 26, // Relaxed (1-29)
  val bioTrackerSensor: String = "BioTracker™ 5.0 PPG (5PD + 2LED)",
  val sleepQualityScore: Int = 86,
  val skinTemperatureCelsius: Float = 36.4f
) {
  val paiStatus: String
    get() = if (paiScore >= paiGoal) "Cardioprotective (≥100 PAI)" else "Building PAI ($paiScore/$paiGoal)"
}

/**
 * Result of syncing wearable data from Google Fit or Zepp Health.
 */
data class WearableSyncResult(
  val isSuccess: Boolean,
  val syncedAt: Long,
  val stepsImported: Int,
  val restingHr: Int,
  val hrSamplesCount: Int,
  val deviceName: String,
  val message: String,
  val sha256Checksum: String,
  val platform: WearablePlatform = WearablePlatform.GOOGLE_FIT,
  val paiImported: Int? = null
)

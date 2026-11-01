package com.example.data.wearable

import com.example.data.model.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Service to connect and import daily activity and heart rate metrics
 * from wearable devices (Pixel Watch, Fitbit, Galaxy Watch, Garmin) via Google Fit API.
 */
class GoogleFitWearableIntegrationService {

  companion object {
    const val SCOPE_ACTIVITY_READ = "https://www.googleapis.com/auth/fitness.activity.read"
    const val SCOPE_BODY_READ = "https://www.googleapis.com/auth/fitness.body.read"
    const val SCOPE_HEART_RATE_READ = "https://www.googleapis.com/auth/fitness.heart_rate.read"

    val OAUTH_SCOPES = listOf(
      SCOPE_ACTIVITY_READ,
      SCOPE_BODY_READ,
      SCOPE_HEART_RATE_READ
    )
  }

  // Pre-configured wearable devices known in the user's Google account
  fun getDefaultDevices(): List<WearableDevice> = listOf(
    WearableDevice(
      id = "device_pixel_watch_3",
      name = "Google Pixel Watch 3",
      type = WearableDeviceType.PIXEL_WATCH,
      manufacturer = "Google",
      model = "PW3-45MM",
      batteryPercent = 86,
      isConnected = true,
      lastSyncedTimestamp = System.currentTimeMillis() - (8 * 60 * 1000), // 8 mins ago
      deviceIdentifier = "PW3:9A:4C:E2:81",
      syncFrequencyMinutes = 15,
      firmwareVersion = "Wear OS 5.0 (RWD9.24)"
    ),
    WearableDevice(
      id = "device_fitbit_charge_6",
      name = "Fitbit Charge 6",
      type = WearableDeviceType.FITBIT,
      manufacturer = "Google / Fitbit",
      model = "FB423",
      batteryPercent = 94,
      isConnected = true,
      lastSyncedTimestamp = System.currentTimeMillis() - (24 * 60 * 1000), // 24 mins ago
      deviceIdentifier = "FB:81:22:D0:A4",
      syncFrequencyMinutes = 30,
      firmwareVersion = "v66.4.11"
    ),
    WearableDevice(
      id = "device_galaxy_watch_7",
      name = "Samsung Galaxy Watch 7",
      type = WearableDeviceType.GALAXY_WATCH,
      manufacturer = "Samsung",
      model = "SM-L310",
      batteryPercent = 72,
      isConnected = false,
      lastSyncedTimestamp = System.currentTimeMillis() - (180 * 60 * 1000), // 3h ago
      deviceIdentifier = "GW:44:B8:11:5F",
      syncFrequencyMinutes = 15,
      firmwareVersion = "One UI Watch 6.0"
    ),
    WearableDevice(
      id = "device_garmin_forerunner",
      name = "Garmin Forerunner 965",
      type = WearableDeviceType.GARMIN,
      manufacturer = "Garmin",
      model = "FR-965",
      batteryPercent = 65,
      isConnected = false,
      lastSyncedTimestamp = System.currentTimeMillis() - (24 * 60 * 60 * 1000), // 1 day ago
      deviceIdentifier = "GM:19:92:AA:77",
      syncFrequencyMinutes = 60,
      firmwareVersion = "v18.23"
    )
  )

  /**
   * Generates profile-tailored daily activity metrics reflecting authentic physiological patterns.
   */
  fun getDailyActivityData(profileId: String, date: String = getTodayDateString()): DailyActivityData {
    val (steps, goal, calories, activeMin, distMeters, floors) = when (profileId) {
      "profile_maya" -> Tuple6(11420, 8000, 1580, 75, 7200f, 18)
      "profile_kwame" -> Tuple6(5480, 6000, 1690, 34, 3500f, 6)
      else -> Tuple6(8950, 10000, 2240, 52, 6420f, 14)
    }

    val hourly = generateHourlySteps(steps)
    return DailyActivityData(
      date = date,
      stepsCount = steps,
      stepGoal = goal,
      caloriesBurnedKcal = calories,
      activeMinutes = activeMin,
      activeMinutesGoal = 45,
      distanceMeters = distMeters,
      floorsClimbed = floors,
      hourlySteps = hourly
    )
  }

  /**
   * Generates profile-tailored continuous heart rate data and physiological zones.
   */
  fun getHeartRateData(profileId: String, date: String = getTodayDateString()): HeartRateData {
    val (resting, minHr, maxHr, avgHr, hrv, status) = when (profileId) {
      "profile_maya" -> Tuple6(74, 60, 156, 82, 70, ItemStatus.NORMAL)
      "profile_kwame" -> Tuple6(68, 56, 118, 71, 38, ItemStatus.NORMAL)
      else -> Tuple6(62, 52, 144, 73, 54, ItemStatus.NORMAL)
    }

    val (restMin, fatBurnMin, cardioMin, peakMin) = when (profileId) {
      "profile_maya" -> Tuple4(340, 55, 35, 15)
      "profile_kwame" -> Tuple4(410, 30, 10, 0)
      else -> Tuple4(365, 48, 26, 11)
    }

    val samples = generateHeartRateSamples(resting, minHr, maxHr)

    return HeartRateData(
      date = date,
      restingHeartRateBpm = resting,
      minHeartRateBpm = minHr,
      maxHeartRateBpm = maxHr,
      averageHeartRateBpm = avgHr,
      heartRateVariabilityMs = hrv,
      samples = samples,
      zones = HeartRateZones(
        restMinutes = restMin,
        fatBurnMinutes = fatBurnMin,
        cardioMinutes = cardioMin,
        peakMinutes = peakMin
      ),
      clinicalStatus = status
    )
  }

  /**
   * Simulates full synchronization from Google Fit API and returns an immutable sync package.
   */
  fun executeSync(
    profileId: String,
    device: WearableDevice
  ): WearableSyncResult {
    val activity = getDailyActivityData(profileId)
    val heartRate = getHeartRateData(profileId)

    val rawDataString = "GOOGLE_FIT:${device.name}:${device.deviceIdentifier}:${activity.stepsCount}:${heartRate.restingHeartRateBpm}:${System.currentTimeMillis()}"
    val checksum = computeSha256(rawDataString)

    return WearableSyncResult(
      isSuccess = true,
      syncedAt = System.currentTimeMillis(),
      stepsImported = activity.stepsCount,
      restingHr = heartRate.restingHeartRateBpm,
      hrSamplesCount = heartRate.samples.size,
      deviceName = device.name,
      message = "Imported ${activity.stepsCount} steps and ${heartRate.samples.size} HR samples from ${device.name} via Google Fit API",
      sha256Checksum = checksum
    )
  }

  /**
   * Formats wearable metrics into FHIR-compliant HealthTimelineItem clinical observations.
   */
  fun convertToTimelineItems(
    profileId: String,
    device: WearableDevice,
    activity: DailyActivityData,
    heartRate: HeartRateData
  ): List<HealthTimelineItem> {
    val timestampStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
    val ingestTimeStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
    val rawPayload = "${device.id}_${activity.stepsCount}_${heartRate.restingHeartRateBpm}_$ingestTimeStr"
    val docHash = "sha256:" + computeSha256(rawPayload).take(16) + "..." + computeSha256(rawPayload).takeLast(6)

    val provenance = Provenance(
      sourceSystem = "Google Fit API / ${device.name}",
      ingestedAt = ingestTimeStr,
      confidenceScore = 0.98f,
      consentState = "Patient-Owned / Google Fit Authorized",
      documentHash = docHash
    )

    // Item 1: Daily Physical Activity Observation (LOINC 55423-8)
    val activityItem = HealthTimelineItem(
      id = "wearable_act_${UUID.randomUUID().toString().take(8)}",
      profileId = profileId,
      category = TimelineCategory.DOCUMENT, // Will be categorized under observations/clinical tracker
      title = "Daily Activity: ${activity.stepsCount} steps",
      subtitle = "${activity.activeMinutes} active mins • ${activity.caloriesBurnedKcal} kcal • ${String.format(Locale.US, "%.1f", activity.distanceKm)} km",
      date = timestampStr,
      providerOrFacility = "${device.name} (Google Fit)",
      status = if (activity.stepsCount >= activity.stepGoal) ItemStatus.NORMAL else ItemStatus.ACTIVE,
      clinicalNotes = "Continuous accelerometer & step cadence tracked via ${device.name}. Daily goal: ${activity.stepGoal} steps. Achieved ${(activity.stepProgressPercent * 100).toInt()}% of goal. Active energy: ${activity.caloriesBurnedKcal} kcal.",
      provenance = provenance,
      tags = listOf("Google Fit", "Physical Activity", "LOINC 55423-8", device.manufacturer),
      keyValues = mapOf(
        "Steps" to "${activity.stepsCount}",
        "Step Goal" to "${activity.stepGoal}",
        "Active Minutes" to "${activity.activeMinutes}m",
        "Calories Burned" to "${activity.caloriesBurnedKcal} kcal",
        "Distance" to "${String.format(Locale.US, "%.2f", activity.distanceKm)} km",
        "Floors Climbed" to "${activity.floorsClimbed}",
        "LOINC Code" to "55423-8 (Number of steps in 24 hour Measured)"
      )
    )

    // Item 2: Resting & Continuous Heart Rate Observation (LOINC 8867-4)
    val hrItem = HealthTimelineItem(
      id = "wearable_hr_${UUID.randomUUID().toString().take(8)}",
      profileId = profileId,
      category = TimelineCategory.LAB, // Clinical vital signs
      title = "Resting Heart Rate: ${heartRate.restingHeartRateBpm} bpm",
      subtitle = "Range ${heartRate.minHeartRateBpm}-${heartRate.maxHeartRateBpm} bpm • HRV: ${heartRate.heartRateVariabilityMs} ms",
      date = timestampStr,
      providerOrFacility = "${device.name} PPG Sensor",
      status = heartRate.clinicalStatus,
      clinicalNotes = "Optical PPG continuous cardiovascular telemetry. Resting HR: ${heartRate.restingHeartRateBpm} bpm (Normal clinical baseline: 60-100 bpm). Time in Cardio/Peak: ${heartRate.zones.cardioMinutes + heartRate.zones.peakMinutes} mins. HRV RMSSD: ${heartRate.heartRateVariabilityMs} ms.",
      provenance = provenance,
      tags = listOf("Google Fit", "Vital Signs", "Heart Rate", "LOINC 8867-4", "PPG Sensor"),
      keyValues = mapOf(
        "Resting HR" to "${heartRate.restingHeartRateBpm} bpm",
        "Average HR" to "${heartRate.averageHeartRateBpm} bpm",
        "Min / Max HR" to "${heartRate.minHeartRateBpm} / ${heartRate.maxHeartRateBpm} bpm",
        "Heart Rate Variability (HRV)" to "${heartRate.heartRateVariabilityMs} ms",
        "Fat Burn Zone" to "${heartRate.zones.fatBurnMinutes}m",
        "Cardio Zone" to "${heartRate.zones.cardioMinutes}m",
        "Peak Zone" to "${heartRate.zones.peakMinutes}m",
        "LOINC Code" to "8867-4 (Heart rate)"
      )
    )

    return listOf(activityItem, hrItem)
  }

  private fun generateHourlySteps(totalSteps: Int): List<HourlyStepBucket> {
    // Distribute steps realistically across the day: peak at morning commute (8-9), lunch (12-13), evening walk/gym (17-19)
    val weights = doubleArrayOf(
      0.0, 0.0, 0.0, 0.0, 0.0, 0.01, // 0 - 5 AM
      0.04, 0.08, 0.12, 0.06, 0.05, 0.06, // 6 - 11 AM
      0.10, 0.08, 0.05, 0.05, 0.06, 0.11, // 12 - 5 PM
      0.08, 0.03, 0.01, 0.01, 0.0, 0.0 // 6 - 11 PM
    )
    val sumWeights = weights.sum()

    val buckets = weights.mapIndexed { hour, weight ->
      val hourSteps = ((weight / sumWeights) * totalSteps).toInt()
      HourlyStepBucket(hour = hour, steps = hourSteps)
    }.toMutableList()

    val diff = totalSteps - buckets.sumOf { it.steps }
    if (diff != 0 && buckets.isNotEmpty()) {
      val maxIndex = buckets.indices.maxByOrNull { buckets[it].steps } ?: 8
      buckets[maxIndex] = buckets[maxIndex].copy(steps = buckets[maxIndex].steps + diff)
    }
    return buckets
  }

  private fun generateHeartRateSamples(resting: Int, minHr: Int, maxHr: Int): List<HeartRateSample> {
    val samples = mutableListOf<HeartRateSample>()
    // Generate samples every 1 hour
    for (hour in 0..23) {
      val hStr = if (hour < 10) "0$hour:00" else "$hour:00"
      val bpm = when (hour) {
        in 0..5 -> (minHr + (hour * 1)).coerceAtLeast(minHr) // deep sleep
        6 -> resting + 2
        in 7..9 -> resting + 18 // morning movement
        in 10..11 -> resting + 8
        in 12..13 -> resting + 14 // lunch walk
        in 14..16 -> resting + 6
        17 -> maxHr - 12 // evening workout
        18 -> maxHr - 5
        19 -> resting + 16 // cool down
        in 20..22 -> resting + 4 // winding down
        else -> resting
      }
      val context = when {
        hour in 0..5 -> "Sleep"
        hour in 17..18 -> "Exercise"
        hour in listOf(7, 8, 12, 19) -> "Walking"
        else -> "Resting"
      }
      samples.add(HeartRateSample(timestamp = System.currentTimeMillis() - ((23 - hour) * 3600 * 1000L), timeFormatted = hStr, bpm = bpm, context = context))
    }
    return samples
  }

  private fun computeSha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  private fun getTodayDateString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  }
}

// Data tuple helpers for concise service generation
private data class Tuple6<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)
private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

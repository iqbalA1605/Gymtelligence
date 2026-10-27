package com.example.savex.model

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthDataProvider(context: Context) {

    class HealthSnapshot {
        @JvmField var steps: Int = 0
        @JvmField var heartRateBpm: Int = 0
        @JvmField var sleepHours: Double = 0.0
    }

    interface HealthCallback {
        fun onData(snapshot: HealthSnapshot)
        fun onError(e: Exception)
    }

    private val appContext = context.applicationContext
    private var sensorManager: SensorManager? = null
    private var sensorListener: SensorEventListener? = null
    private var latestSensorStepCount = 0

    companion object {
        @JvmStatic
        fun requiredHealthConnectPermissions(): Set<String> = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class)
        )

        @JvmStatic
        fun isHealthConnectAvailable(context: Context): Boolean {
            return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        }
    }

    // ===================== STEPS (phone sensor — used only as an immediate fallback before Health Connect responds) =====================

    fun startStepUpdates(onStepsChanged: Runnable) {
        sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return

        var baselineSteps = -1
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val totalSinceBoot = event.values[0].toInt()
                if (baselineSteps == -1) baselineSteps = totalSinceBoot
                latestSensorStepCount = totalSinceBoot - baselineSteps
                Handler(Looper.getMainLooper()).post(onStepsChanged)
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        sensorListener = listener
        sensorManager?.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stopStepUpdates() {
        sensorListener?.let { sensorManager?.unregisterListener(it) }
    }

    fun getLatestStepCount(): Int = latestSensorStepCount

    // ===================== HEART RATE + SLEEP + STEPS (Health Connect — accurate daily totals) =====================

    fun fetchHealthConnectData(callback: HealthCallback) {
        if (!isHealthConnectAvailable(appContext)) {
            callback.onError(IllegalStateException("Health Connect is not installed on this device"))
            return
        }

        val client = HealthConnectClient.getOrCreate(appContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = HealthSnapshot()
                snapshot.steps = latestSensorStepCount // fallback until the real query below overwrites it

                val now = Instant.now()
                val dayStart = LocalDate.now(ZoneId.systemDefault())
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                val last24h = now.minus(Duration.ofHours(24))

                // Steps — sum of all today's records (accurate daily total, matches Samsung Health/Health Connect)
                val stepsResponse = client.readRecords(
                    ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(dayStart, now))
                )
                val totalStepsToday = stepsResponse.records.sumOf { it.count }
                if (totalStepsToday > 0) {
                    snapshot.steps = totalStepsToday.toInt()
                }

                // Heart rate — most recent record in the last 24h
                val hrResponse = client.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(last24h, now))
                )
                val lastSample = hrResponse.records.lastOrNull()?.samples?.lastOrNull()
                if (lastSample != null) {
                    snapshot.heartRateBpm = lastSample.beatsPerMinute.toInt()
                }

                // Sleep — most recent session in the last 24h
                val sleepResponse = client.readRecords(
                    ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(last24h, now))
                )
                val lastSleep = sleepResponse.records.lastOrNull()
                if (lastSleep != null) {
                    val minutes = Duration.between(lastSleep.startTime, lastSleep.endTime).toMinutes()
                    snapshot.sleepHours = minutes / 60.0
                }

                Handler(Looper.getMainLooper()).post { callback.onData(snapshot) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e) }
            }
        }
    }
}
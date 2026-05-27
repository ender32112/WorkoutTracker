package com.example.workouttracker.health

import android.content.Context
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId

private const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
private const val ANDROID_DATA_ORIGIN = "android"

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE
}

class HealthConnectGateway(private val context: Context) {

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    fun availability(): HealthConnectAvailability {
        return when (HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PROVIDER_PACKAGE)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
            else -> HealthConnectAvailability.UNAVAILABLE
        }
    }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean {
        val client = clientOrNull() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    suspend fun readTodaySteps(): Long {
        val client = clientOrNull() ?: return 0L
        val zone = ZoneId.systemDefault()
        return readStepsForDate(client, LocalDate.now(zone), zone)
    }

    suspend fun readStepsByDate(days: Int = 30): Map<String, Long> {
        val client = clientOrNull() ?: return emptyMap()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val safeDays = days.coerceIn(1, 90)
        val grouped = linkedMapOf<String, Long>()

        for (offset in (safeDays - 1) downTo 0) {
            val date = today.minusDays(offset.toLong())
            grouped[date.toString()] = readStepsForDate(client, date, zone)
        }
        return grouped
    }

    private fun clientOrNull(): HealthConnectClient? {
        if (availability() != HealthConnectAvailability.AVAILABLE) return null
        return runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    private suspend fun readStepsForDate(
        client: HealthConnectClient,
        date: LocalDate,
        zone: ZoneId
    ): Long {
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val totalResponse = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        val totalSteps = totalResponse[StepsRecord.COUNT_TOTAL] ?: 0L

        if (!isOnDeviceStepTrackingAvailable()) {
            return totalSteps
        }

        val androidResponse = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
                dataOriginFilter = setOf(DataOrigin(ANDROID_DATA_ORIGIN))
            )
        )
        val androidSteps = androidResponse[StepsRecord.COUNT_TOTAL]

        if (androidSteps != null) {
            if (androidSteps != totalSteps) {
                Log.d(
                    "HealthConnect",
                    "Using android-origin steps for $date: android=$androidSteps total=$totalSteps"
                )
            }
            return androidSteps
        }

        return totalSteps
    }

    private fun isOnDeviceStepTrackingAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 20
    }
}

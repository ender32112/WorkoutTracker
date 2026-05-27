package com.example.workouttracker.data.local

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class WeightSyncRepository @Inject constructor(
    private val dao: WorkoutTrackerDao
) {
    suspend fun harmonizeCurrentWeight(userId: String) {
        val latestEntry = dao.getLatestWeightEntry(userId)
        val canonicalWeight = latestEntry?.weightKg
            ?: dao.getUserById(userId)?.weight?.takeIf { it > 0f }
            ?: dao.getNutritionProfile(userId)?.weightKg?.takeIf { it > 0f }

        canonicalWeight ?: return

        if (latestEntry == null) {
            dao.insertWeightEntry(
                WeightEntryEntity(
                    userId = userId,
                    weightKg = canonicalWeight,
                    loggedAt = System.currentTimeMillis()
                )
            )
        }

        syncProfiles(userId, canonicalWeight)
    }

    suspend fun saveWeightMeasurement(
        userId: String,
        weightKg: Float,
        dateIso: String? = null
    ) {
        val normalizedWeight = weightKg.takeIf { it > 0f } ?: return
        ensureUserExists(userId)
        val latestEntry = dao.getLatestWeightEntry(userId)
        val targetTimestamp = resolveMeasurementTimestamp(dateIso)
        val targetDate = dateIso ?: isoDateFromTimestamp(targetTimestamp)
        val latestDate = latestEntry?.loggedAt?.let(::isoDateFromTimestamp)
        val latestWeight = latestEntry?.weightKg

        val shouldInsert = latestEntry == null ||
            latestDate != targetDate ||
            latestWeight == null ||
            abs(latestWeight - normalizedWeight) > 0.01f

        if (shouldInsert) {
            deleteWeightEntriesForDate(userId, targetDate)
            dao.insertWeightEntry(
                WeightEntryEntity(
                    userId = userId,
                    weightKg = normalizedWeight,
                    loggedAt = targetTimestamp
                )
            )
        }

        syncProfiles(userId, normalizedWeight)
    }

    suspend fun replaceWeightHistory(
        userId: String,
        history: List<Pair<String, Float>>
    ) {
        ensureUserExists(userId)
        dao.deleteWeightEntries(userId)

        var latestTimestamp = Long.MIN_VALUE
        var latestWeight: Float? = null

        history
            .filter { (_, weight) -> weight > 0f }
            .sortedBy { it.first }
            .forEach { (dateIso, weight) ->
                val timestamp = resolveDateTimestamp(dateIso)
                dao.insertWeightEntry(
                    WeightEntryEntity(
                        userId = userId,
                        weightKg = weight,
                        loggedAt = timestamp
                    )
                )
                if (timestamp >= latestTimestamp) {
                    latestTimestamp = timestamp
                    latestWeight = weight
                }
            }

        latestWeight?.let { syncProfiles(userId, it) }
    }

    private suspend fun ensureUserExists(userId: String) {
        if (dao.getUserById(userId) != null) return

        dao.upsertUser(
            UserEntity(
                id = userId,
                name = userId.takeIf { it != "guest" }.orEmpty(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun syncProfiles(userId: String, weightKg: Float) {
        dao.getUserById(userId)?.let { user ->
            if (abs(user.weight - weightKg) > 0.01f) {
                dao.upsertUser(
                    user.copy(
                        weight = weightKg,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        dao.getNutritionProfile(userId)?.let { profile ->
            val currentWeight = profile.weightKg
            if (currentWeight == null || abs(currentWeight - weightKg) > 0.01f) {
                dao.upsertNutritionProfile(
                    profile.copy(
                        weightKg = weightKg,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private suspend fun deleteWeightEntriesForDate(userId: String, dateIso: String) {
        val start = resolveDateTimestamp(dateIso)
        val end = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        dao.deleteWeightEntriesInRange(userId, start, end)
    }

    private fun resolveMeasurementTimestamp(dateIso: String?): Long {
        if (dateIso.isNullOrBlank()) return System.currentTimeMillis()
        if (dateIso == isoDateFromTimestamp(System.currentTimeMillis())) {
            return System.currentTimeMillis()
        }
        return resolveDateTimestamp(dateIso)
    }

    private fun resolveDateTimestamp(dateIso: String): Long {
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateIso)?.time
        }.getOrNull() ?: System.currentTimeMillis()
    }

    private fun isoDateFromTimestamp(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
}

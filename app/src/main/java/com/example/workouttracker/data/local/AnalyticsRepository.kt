package com.example.workouttracker.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalyticsRepository(
    private val dao: WorkoutTrackerDao
) {
    fun observeWeightHistory(userId: String): Flow<List<Pair<String, Float>>> =
        dao.observeWeight(userId).map { list ->
            list.sortedWith(compareBy<WeightEntryEntity> { it.loggedAt }.thenBy { it.id })
                .associateBy(
                    keySelector = { isoDateFromTimestamp(it.loggedAt) },
                    valueTransform = { it.weightKg }
                )
                .toList()
                .sortedBy { it.first }
        }

    fun observeStepHistory(userId: String): Flow<List<StepHistoryRecord>> =
        dao.observeStepEntries(userId).map { list ->
            list.sortedBy { it.dateIso }.map { StepHistoryRecord(it.dateIso, it.steps) }
        }

    suspend fun saveWeight(userId: String, dateIso: String, weightKg: Float) {
        val timestamp = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateIso)?.time
        }.getOrNull() ?: System.currentTimeMillis()
        dao.insertWeightEntry(WeightEntryEntity(userId = userId, weightKg = weightKg, loggedAt = timestamp))
    }

    suspend fun replaceWeightHistory(userId: String, history: List<Pair<String, Float>>) {
        dao.deleteWeightEntries(userId)
        history.sortedBy { it.first }.forEach { (dateIso, weight) ->
            saveWeight(userId, dateIso, weight)
        }
    }

    suspend fun saveSteps(userId: String, dateIso: String, steps: Long) {
        dao.deleteStepEntryByDate(userId, dateIso)
        dao.upsertStepEntry(StepEntryEntity(userId = userId, dateIso = dateIso, steps = steps))
    }

    suspend fun replaceStepHistory(userId: String, history: List<StepHistoryRecord>) {
        dao.deleteStepEntries(userId)
        history.sortedBy { it.dateIso }.forEach { record ->
            dao.upsertStepEntry(StepEntryEntity(userId = userId, dateIso = record.dateIso, steps = record.steps))
        }
    }

    private fun isoDateFromTimestamp(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
}

data class StepHistoryRecord(
    val dateIso: String,
    val steps: Long
)

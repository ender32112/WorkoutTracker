package com.example.workouttracker.feature.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.core.auth.AuthSessionStore
import com.example.workouttracker.data.local.AnalyticsRepository
import com.example.workouttracker.data.local.LegacyDataMigrator
import com.example.workouttracker.data.local.StepHistoryRecord
import com.example.workouttracker.data.local.WeightSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    authSessionStore: AuthSessionStore,
    private val repository: AnalyticsRepository,
    private val migrator: LegacyDataMigrator,
    private val weightSyncRepository: WeightSyncRepository
) : ViewModel() {
    private val userId = authSessionStore.currentUserIdOrGuest()

    private val _effects = MutableSharedFlow<AnalyticsUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<AnalyticsUiEffect> = _effects

    val weightHistory: StateFlow<List<Pair<String, Float>>> =
        repository.observeWeightHistory(userId)
            .map { list -> list.map { prettyDate(it.first) to it.second } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stepHistory: StateFlow<List<AnalyticsStepHistory>> =
        repository.observeStepHistory(userId)
            .map { list -> list.map { AnalyticsStepHistory(it.dateIso, prettyDate(it.dateIso), it.steps) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            runCatching {
                migrator.migrateAllKnownUsers()
                weightSyncRepository.harmonizeCurrentWeight(userId)
            }.onFailure {
                _effects.emit(AnalyticsUiEffect.Snackbar("Не удалось загрузить историю веса"))
            }
        }
    }

    fun saveWeightForToday(weightKg: Float) {
        val today = isoToday()
        viewModelScope.launch {
            runCatching {
                weightSyncRepository.saveWeightMeasurement(userId, weightKg, today)
            }.onSuccess {
                _effects.emit(AnalyticsUiEffect.Snackbar("Вес сохранён"))
            }.onFailure {
                _effects.emit(AnalyticsUiEffect.Snackbar("Не удалось сохранить вес"))
            }
        }
    }

    fun replaceWeightHistory(history: List<Pair<String, Float>>) {
        viewModelScope.launch {
            runCatching {
                weightSyncRepository.replaceWeightHistory(
                    userId,
                    history.map { isoDateFromPretty(it.first) to it.second }
                )
            }.onSuccess {
                _effects.emit(AnalyticsUiEffect.Snackbar("История веса обновлена"))
            }.onFailure {
                _effects.emit(AnalyticsUiEffect.Snackbar("Не удалось обновить историю веса"))
            }
        }
    }

    fun saveStepsForDate(dateIso: String, steps: Long) {
        viewModelScope.launch {
            repository.saveSteps(userId, dateIso, steps)
        }
    }

    fun replaceStepHistory(history: List<StepHistoryRecord>) {
        viewModelScope.launch {
            repository.replaceStepHistory(userId, history)
        }
    }

    fun emitMessage(message: String) {
        viewModelScope.launch {
            _effects.emit(AnalyticsUiEffect.Snackbar(message))
        }
    }

    private fun isoToday(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun prettyDate(dateIso: String): String =
        runCatching {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateIso)
            SimpleDateFormat("dd.MM", Locale.getDefault()).format(parsed ?: Date())
        }.getOrDefault(dateIso)

    private fun isoDateFromPretty(pretty: String): String {
        if (pretty.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) return pretty
        if (!pretty.matches(Regex("""\d{2}\.\d{2}"""))) return isoToday()
        val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        return "$currentYear-${pretty.substring(3, 5)}-${pretty.substring(0, 2)}"
    }
}

data class AnalyticsStepHistory(
    val isoDate: String,
    val prettyDate: String,
    val steps: Long
)

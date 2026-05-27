package com.example.workouttracker.feature.analytics.runtime

import android.content.Context
import com.example.workouttracker.core.auth.AuthSessionStore
import com.example.workouttracker.data.local.AnalyticsRepository
import com.example.workouttracker.data.local.StepHistoryRecord
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AnalyticsRuntimeEntryPoint {
    fun analyticsRepository(): AnalyticsRepository
    fun authSessionStore(): AuthSessionStore
}

class AnalyticsRuntimeStorage private constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val authSessionStore: AuthSessionStore
) {
    private val currentUserId: String
        get() = authSessionStore.currentUserIdOrGuest()

    suspend fun saveStepEntry(dateIso: String, steps: Long) {
        analyticsRepository.saveSteps(currentUserId, dateIso, steps)
    }

    suspend fun replaceStepHistory(stepsByDate: Map<String, Long>) {
        analyticsRepository.replaceStepHistory(
            currentUserId,
            stepsByDate
                .toList()
                .sortedBy { it.first }
                .map { (dateIso, steps) -> StepHistoryRecord(dateIso = dateIso, steps = steps) }
        )
    }

    companion object {
        fun from(context: Context): AnalyticsRuntimeStorage {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                AnalyticsRuntimeEntryPoint::class.java
            )
            return AnalyticsRuntimeStorage(
                analyticsRepository = entryPoint.analyticsRepository(),
                authSessionStore = entryPoint.authSessionStore()
            )
        }
    }
}

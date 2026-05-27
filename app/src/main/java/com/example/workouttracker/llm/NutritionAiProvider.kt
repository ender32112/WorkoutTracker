package com.example.workouttracker.llm

import android.content.Context
import com.example.workouttracker.core.auth.AuthSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionAiProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authSessionStore: AuthSessionStore
) {
    fun forCurrentUser(): NutritionAiRepository =
        NutritionAiRepository.getInstance(context, authSessionStore.currentUserIdOrGuest())

    fun forUser(userId: String): NutritionAiRepository =
        NutritionAiRepository.getInstance(context, userId)
}

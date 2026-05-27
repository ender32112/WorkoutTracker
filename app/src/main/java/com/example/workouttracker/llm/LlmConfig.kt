package com.example.workouttracker.llm

import com.example.workouttracker.BuildConfig

/**
 * Centralized access to LLM settings injected via BuildConfig/local.properties.
 */
object LlmConfig {
    val API_KEY: String = BuildConfig.LLM_API_KEY
    val BASE_URL: String = BuildConfig.LLM_BASE_URL
    val MODEL_ID: String = BuildConfig.LLM_MODEL_ID
    val HTTP_REFERER: String = BuildConfig.LLM_HTTP_REFERER
    val APP_TITLE: String = BuildConfig.LLM_APP_TITLE
}

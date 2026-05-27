package com.example.workouttracker.feature.nutrition.presentation

import org.json.JSONObject

fun JSONObject.optNullableFloat(key: String): Float? {
    if (!has(key) || isNull(key)) return null
    val value = optDouble(key, Double.NaN)
    return if (value.isNaN()) null else value.toFloat()
}


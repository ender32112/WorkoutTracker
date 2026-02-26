package com.example.workouttracker.ui.nutrition

import org.json.JSONObject

fun JSONObject.optNullableFloat(key: String): Float? {
    if (!has(key) || isNull(key)) return null
    val value = optDouble(key, Double.NaN)
    return if (value.isNaN()) null else value.toFloat()
}

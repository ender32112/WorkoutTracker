package com.example.workouttracker.feature.analytics.presentation.sections

import androidx.compose.runtime.Composable
import com.example.workouttracker.feature.analytics.presentation.WeatherCardPretty

@Composable
fun WeatherSection(
    city: String,
    weather: String,
    subtitle: String?
) {
    WeatherCardPretty(city = city, weather = weather, subtitle = subtitle)
}


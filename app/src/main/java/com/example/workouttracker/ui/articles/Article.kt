package com.example.workouttracker.ui.articles

import java.util.UUID

data class Article(
    val id: UUID        = UUID.randomUUID(),
    val title: String,
    val date: String,
    val text: String,
    val cost: Int,
    val purchased: Boolean = false
)

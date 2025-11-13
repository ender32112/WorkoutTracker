package com.example.workouttracker.ui.articles

import java.util.UUID

/**
 * Статья. id должен быть СТАБИЛЬНЫМ между запусками,
 * иначе prefs "purchased_<id>" потеряются.
 * Для этого используем UUID.nameUUIDFromBytes по "слагу".
 */
data class Article(
    val id: UUID,
    val slug: String,       // стабильный ключ ("biceps", "mass-diet", ...)
    val title: String,
    val date: String,       // отображаемая дата
    val content: String,    // полный текст
    val cost: Int,
    val purchased: Boolean = false
)

/** Утилита: стабильно генерит UUID по слагу. */
fun stableIdFromSlug(slug: String): UUID =
    UUID.nameUUIDFromBytes(("article:$slug").toByteArray())

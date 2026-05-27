package com.example.workouttracker.feature.articles.presentation

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

fun defaultArticleCatalog(): List<Article> = listOf(
    Article(
        id = stableIdFromSlug("biceps"),
        slug = "biceps",
        title = "Как накачать бицепс",
        date = "15 мар 2025",
        content = """
            Бицепс — это мышца, которую хотят все. Но как её накачать?

            1. Подъёмы штанги стоя — 4x10
            2. Молотковый гриф — 3x12
            3. Концентрированные подъёмы — 3x15

            Главное — техника и прогрессия веса!
        """.trimIndent(),
        cost = 100
    ),
    Article(
        id = stableIdFromSlug("mass-diet"),
        slug = "mass-diet",
        title = "Диета для набора массы",
        date = "10 мар 2025",
        content = """
            Чтобы расти — нужно есть больше, чем тратишь.

            • Калории: +500 к норме
            • Белок: 2 г/кг веса
            • Углеводы: рис, овсянка, картофель
            • Жиры: орехи, авокадо, масло

            Ешь 5–6 раз в день!
        """.trimIndent(),
        cost = 150
    ),
    Article(
        id = stableIdFromSlug("legs-training"),
        slug = "legs-training",
        title = "Тренировка на ноги",
        date = "05 мар 2025",
        content = """
            Ноги — 50% тела. Не пропускай!

            1. Приседания — 4x8–12
            2. Румынская тяга — 4x10
            3. Жим ногами — 3x15
            4. Выпады — 3x12 на ногу

            Отдых между подходами — 2–3 минуты.
        """.trimIndent(),
        cost = 200
    )
)


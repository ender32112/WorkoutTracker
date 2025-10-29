package com.example.workouttracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.ui.achievements.Achievement
import com.example.workouttracker.ui.articles.Article
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ArticleViewModel(
    private val trainingViewModel: TrainingViewModel
) : ViewModel() {


    private val _articles = MutableStateFlow<List<Article>>(listOf(
        Article(
            id        = UUID.randomUUID(),
            title     = "Роль нервного стимула в регуляции мышечного сокращения",
            cost      = 100,
            date      = "12.05.2025",
            text      = """
        В основном масса мышц растёт за счёт усиления в них процессов синтеза белка. Мышечные волокна механочувствительны, то есть изменение их длины, а также, возможно, формы ядер даёт сигнал к более интенсивному производству белков.

Но одного механического воздействия, судя по всему, недостаточно: чтобы синтез белков шёл интенсивнее, мышце необходимо одновременно с пассивным растяжением получать электрические сигналы к сокращению. Именно это и происходит, когда человек выполняет упражнения с отягощениями.

Также в стимулы гипертрофии часто записывают вещества, которые образуются при работе мышц: лактат, фосфат-ионы и подобное. Однако отделить их влияние от эффектов электрических и механических импульсов пока не удаётся. Возможно, эти метаболиты как-то влияют на механотрансдукцию — преобразование сигнала о растяжении или сокращении мышечных волокон в биохимический ответ этих волокон, но как именно, ещё не известно. По имеющимся данным, непосредственно продукты мышечной работы не усиливают синтез белка.

      """.trimIndent()
        ),
        Article(
            id        = UUID.randomUUID(),
            title     = "«Тренировка» стабильности «кора»",
            cost      = 1000,
            date      = "11.05.2025",
            text      = """
        «Кор» — это совокупность мышц, стабилизирующих позвоночник. Она включает все мышцы брюшного пресса, три слоя боковой брюшной стенки, разгибатели позвоночника, мышцы тазового дна, сгибатели бедра — а еще диафрагму и межреберные мышцы, хотя это, возможно, слишком смелое предположение. Все эти мышцы работают вместе, чтобы контролировать положение позвоночника, что обычно означает удержание его в определенной позиции во время физической работы: усилие, создаваемое ногами, передается через туловище к рукам, удерживающим отягощение, или в определенных тренировочных ситуациях (приседания со штангой) к спине либо плечам.

Мышцы «кора» поддерживают межпозвонковые сочленения, что позволяет позвоночнику не только передавать усилие, но и оставаться при этом неповрежденным. Они чрезвычайно важны во всех видах спорта, особенно в упражнениях со штангой, и именно поэтому я интересуюсь данной темой.

      """.trimIndent()
        )
    ))
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()


    private val thresholds = listOf(100, 500, 1000, 2000, 5000)


    private val rawBalance: Flow<Int> =
        trainingViewModel.sessions
            .map { sessions ->


                val exercises = listOf(
                    "Присед Смит",
                    "Жим штанги лежа",
                    "Тяга верхнего блока",
                    "Становая тяга",
                    "Румынская тяга",
                    "Бёрпи",
                    "Подтягивания",
                    "Выпады"
                )


                val repsByExercise: List<Int> = exercises.map { name ->
                    sessions
                        .flatMap { session -> session.exercises }
                        .filter  { it.name == name }
                        .sumOf   { it.sets * it.reps }
                }


                val starsByExercise: List<Int> = repsByExercise.map { totalReps ->

                    (thresholds.indexOfLast { threshold -> totalReps >= threshold } + 1)
                        .coerceIn(0, 5)
                }


                val totalStars = starsByExercise.sum()
                totalStars * 100
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, 0)


    private val _spent = MutableStateFlow(0)
    val spent: StateFlow<Int> = _spent.asStateFlow()


    val balance: StateFlow<Int> = rawBalance
        .combine(spent) { raw, spent -> (raw - spent).coerceAtLeast(0) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)


    fun buyArticle(articleId: UUID): Boolean {
        val cost = _articles.value.first { it.id == articleId }.cost
        return if (balance.value >= cost) {
            _spent.value += cost
            _articles.value = _articles.value.map {
                if (it.id == articleId) it.copy(purchased = true) else it
            }
            true
        } else {
            false
        }
    }


    val achievements: StateFlow<List<Achievement>> = trainingViewModel.sessions
        .map { sessions ->

            val exercises = listOf("Присед Смит", "Жим штанги лежа", "Тяга верхнего блока", "Становая тяга", "Румынская тяга", "Бёрпи", "Подтягивания", "Выпады")
            exercises.map { name ->

                val prog = sessions.sumOf { session ->
                    session.exercises
                        .filter { it.name == name }
                        .sumOf { it.sets * it.reps }
                }

                val stars = (thresholds.indexOfLast { prog >= it } + 1).coerceIn(0,5)
                Achievement(
                    id           = UUID.randomUUID(),
                    title        = "Мастер \"$name\"",
                    exerciseName = name,
                    progress     = prog,
                    stars        = stars
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

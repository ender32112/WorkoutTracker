package com.example.workouttracker.feature.articles.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.core.auth.AuthSessionStore
import com.example.workouttracker.data.local.ArticleRepository
import com.example.workouttracker.data.local.LegacyDataMigrator
import com.example.workouttracker.data.local.PerformedSessionWithExercises
import com.example.workouttracker.data.local.WorkoutTrackerDao
import com.example.workouttracker.feature.articles.presentation.Article
import com.example.workouttracker.feature.articles.presentation.defaultArticleCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ArticleViewModel @Inject constructor(
    authSessionStore: AuthSessionStore,
    private val repository: ArticleRepository,
    private val dao: WorkoutTrackerDao,
    private val migrator: LegacyDataMigrator
) : ViewModel() {

    private val userId = authSessionStore.currentUserIdOrGuest()

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles

    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance

    private val _effects = MutableSharedFlow<ArticlesUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ArticlesUiEffect> = _effects

    val uiState: StateFlow<ArticlesUiState> =
        combine(_articles, _balance) { articles, balance ->
            ArticlesUiState(
                articles = articles,
                balance = balance
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ArticlesUiState()
        )

    init {
        loadArticles()
        observeBalance()
    }

    private fun loadArticles() {
        viewModelScope.launch {
            migrator.migrateAllKnownUsers()
            repository.observePurchases(userId).collectLatest { purchases ->
                val purchasedIds = purchases.map { it.articleId }.toSet()
                _articles.value = defaultArticleCatalog().map { article ->
                    article.copy(purchased = article.id.toString() in purchasedIds)
                }
            }
        }
    }

    private fun observeBalance() {
        viewModelScope.launch {
            combine(
                dao.observePerformedSessionsWithExercises(userId).map(::totalVolumeFromSessions),
                repository.observeSpentPoints(userId)
            ) { totalVolume, spent ->
                val earned = (totalVolume / 50f).roundToInt().coerceAtLeast(0)
                (earned - spent).coerceAtLeast(0)
            }.collectLatest { _balance.value = it }
        }
    }

    fun buyArticle(articleId: UUID) {
        val current = _articles.value
        val idx = current.indexOfFirst { it.id == articleId }
        if (idx == -1) {
            viewModelScope.launch {
                _effects.emit(ArticlesUiEffect.Snackbar("Статья не найдена"))
            }
            return
        }
        val article = current[idx]
        if (article.purchased) {
            viewModelScope.launch {
                _effects.emit(ArticlesUiEffect.Snackbar("Статья уже открыта"))
            }
            return
        }
        if (_balance.value < article.cost) {
            viewModelScope.launch {
                _effects.emit(ArticlesUiEffect.Snackbar("Недостаточно баллов"))
            }
            return
        }

        viewModelScope.launch {
            repository.purchaseArticle(userId, article.id.toString(), article.cost)
            _effects.emit(ArticlesUiEffect.Snackbar("Статья куплена"))
        }
        _articles.value = current.toMutableList().apply { set(idx, article.copy(purchased = true)) }
        _balance.value = (_balance.value - article.cost).coerceAtLeast(0)
    }

    private fun totalVolumeFromSessions(sessions: List<PerformedSessionWithExercises>): Float =
        sessions.sumOf { session ->
            session.exercises.sumOf { exercise ->
                exercise.sets.sumOf { set -> (set.weight * set.reps).toDouble() }
            }
        }.toFloat()
}


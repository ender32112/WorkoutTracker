package com.example.workouttracker.data.local

import kotlinx.coroutines.flow.Flow

class ArticleRepository(
    private val dao: WorkoutTrackerDao
) {
    fun observePurchases(userId: String): Flow<List<ArticlePurchaseEntity>> = dao.observeArticlePurchases(userId)

    fun observeSpentPoints(userId: String): Flow<Int> = dao.observeArticleSpentPoints(userId)

    suspend fun purchaseArticle(userId: String, articleId: String, cost: Int) {
        dao.upsertArticlePurchase(
            ArticlePurchaseEntity(
                userId = userId,
                articleId = articleId,
                cost = cost,
                purchasedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getPurchasesOnce(userId: String): List<ArticlePurchaseEntity> = dao.getArticlePurchasesOnce(userId)
}

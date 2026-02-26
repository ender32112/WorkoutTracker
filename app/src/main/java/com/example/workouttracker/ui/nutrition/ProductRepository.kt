package com.example.workouttracker.ui.nutrition

import com.example.workouttracker.data.local.ProductCacheEntity
import com.example.workouttracker.data.local.WorkoutTrackerDao

class ProductRepository(
    private val dao: WorkoutTrackerDao,
    private val fatSecretRepository: FatSecretRepository,
    private val offRepository: OpenFoodFactsRepository
) {
    suspend fun lookup(barcode: String): ProductLookupResponse {
        dao.getCachedProductByBarcode(barcode)?.let {
            return ProductLookupResponse(
                product = ProductLookupResult(
                    barcode = barcode,
                    name = it.name,
                    calories100 = it.calories100,
                    protein100 = it.protein100,
                    fats100 = it.fats100,
                    carbs100 = it.carbs100,
                    source = it.source,
                    isPartial = false,
                    isSuspicious = false
                )
            )
        }

        val fatSecretAttempt = fatSecretRepository.lookupProduct(barcode)
        val fatSecret = fatSecretAttempt.data
        val off = offRepository.lookupByBarcode(barcode)
        val merged = merge(fatSecret, off)
        if (merged == null) {
            val offError = "Open Food Facts не вернул результат"
            val fatSecretError = fatSecretAttempt.error ?: "FatSecret не вернул результат"
            return ProductLookupResponse(errorMessage = "$fatSecretError. $offError")
        }

        dao.upsertCachedProduct(
            ProductCacheEntity(
                barcode = barcode,
                name = merged.name,
                calories100 = merged.calories100 ?: 0f,
                protein100 = merged.protein100 ?: 0f,
                fats100 = merged.fats100 ?: 0f,
                carbs100 = merged.carbs100 ?: 0f,
                source = merged.source
            )
        )
        return ProductLookupResponse(product = merged)
    }

    private fun merge(primary: ProductLookupResult?, fallback: ProductLookupResult?): ProductLookupResult? {
        if (primary == null) return fallback
        if (fallback == null) return primary
        val calories = primary.calories100 ?: fallback.calories100
        val protein = primary.protein100 ?: fallback.protein100
        val fats = primary.fats100 ?: fallback.fats100
        val carbs = primary.carbs100 ?: fallback.carbs100
        return ProductLookupResult(
            barcode = primary.barcode,
            name = if (primary.name.isNotBlank()) primary.name else fallback.name,
            calories100 = calories,
            protein100 = protein,
            fats100 = fats,
            carbs100 = carbs,
            source = "${primary.source}+${fallback.source}",
            isPartial = listOf(calories, protein, fats, carbs).any { it == null },
            isSuspicious = primary.isSuspicious || fallback.isSuspicious
        )
    }
}

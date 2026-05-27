package com.example.workouttracker.data.local

import com.example.workouttracker.feature.nutrition.presentation.ProductLookupResponse
import com.example.workouttracker.feature.nutrition.presentation.ProductLookupResult

class ProductRepository(
    private val dao: WorkoutTrackerDao
) {
    private fun ProductCacheEntity.toLookupResult(): ProductLookupResult {
        return ProductLookupResult(
            barcode = barcode,
            name = name,
            calories100 = calories100,
            protein100 = protein100,
            fats100 = fats100,
            carbs100 = carbs100,
            source = source,
            isPartial = false,
            isSuspicious = false
        )
    }

    suspend fun lookup(barcode: String): ProductLookupResponse {
        dao.getCachedProductByBarcode(barcode)?.let {
            return ProductLookupResponse(product = it.toLookupResult())
        }

        return ProductLookupResponse(
            errorMessage = "Продукт не найден в локальной базе. Заполните данные вручную."
        )
    }

    suspend fun getCachedProducts(query: String = ""): List<ProductLookupResult> {
        val normalizedQuery = query.trim()
        val products = if (normalizedQuery.isBlank()) {
            dao.getCachedProducts()
        } else {
            dao.searchCachedProducts(normalizedQuery)
        }
        return products.map { it.toLookupResult() }
    }

    suspend fun saveManualProduct(product: ProductLookupResult) {
        dao.upsertCachedProduct(
            ProductCacheEntity(
                barcode = product.barcode,
                name = product.name,
                calories100 = product.calories100 ?: 0f,
                protein100 = product.protein100 ?: 0f,
                fats100 = product.fats100 ?: 0f,
                carbs100 = product.carbs100 ?: 0f,
                source = product.source
            )
        )
    }
}

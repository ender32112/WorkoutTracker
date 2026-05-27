package com.example.workouttracker.feature.nutrition.presentation

data class LookupAttempt<T>(
    val data: T? = null,
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = data != null
}

data class ProductLookupResponse(
    val product: ProductLookupResult? = null,
    val errorMessage: String? = null
)



package com.example.workouttracker.ui.nutrition

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


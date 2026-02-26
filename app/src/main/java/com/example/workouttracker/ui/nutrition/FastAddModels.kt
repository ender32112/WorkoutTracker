package com.example.workouttracker.ui.nutrition

data class ProductLookupResult(
    val barcode: String,
    val name: String,
    val calories100: Float?,
    val protein100: Float?,
    val fats100: Float?,
    val carbs100: Float?,
    val source: String,
    val isPartial: Boolean,
    val isSuspicious: Boolean
)

enum class QuantityUnit {
    GRAMS,
    PIECES
}

data class FridgeItemUiModel(
    val id: Long,
    val name: String,
    val unitType: QuantityUnit,
    val amount: Int,
    val calories100: Float,
    val protein100: Float,
    val fats100: Float,
    val carbs100: Float,
    val barcode: String? = null
)

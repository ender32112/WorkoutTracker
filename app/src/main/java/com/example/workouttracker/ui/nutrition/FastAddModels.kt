package com.example.workouttracker.ui.nutrition

data class ProductLookupResult(
    val barcode: String,
    val name: String,
    val calories100: Int,
    val protein100: Int,
    val fats100: Int,
    val carbs100: Int,
    val source: String
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
    val calories100: Int,
    val protein100: Int,
    val fats100: Int,
    val carbs100: Int,
    val barcode: String? = null
)

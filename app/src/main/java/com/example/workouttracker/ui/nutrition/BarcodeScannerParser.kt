package com.example.workouttracker.ui.nutrition

import com.google.mlkit.vision.barcode.common.Barcode

object BarcodeScannerParser {
    fun extractCode(barcodes: List<Barcode>): String? {
        return barcodes.firstOrNull()?.rawValue?.trim()?.takeIf { it.isNotEmpty() }
    }
}

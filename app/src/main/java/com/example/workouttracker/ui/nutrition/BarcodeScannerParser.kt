package com.example.workouttracker.ui.nutrition

import com.google.mlkit.vision.barcode.common.Barcode

object BarcodeScannerParser {
    fun extractCode(barcodes: List<Barcode>): String? {
        return barcodes.firstNotNullOfOrNull { normalize(it.rawValue) }
    }

    fun normalize(raw: String?): String? {
        val digits = raw.orEmpty().replace("\\s".toRegex(), "").filter { it.isDigit() }
        return when (digits.length) {
            8, 12 -> digits
            13 -> digits.takeIf { isValidEan13(it) }
            else -> null
        }
    }

    fun isValidEan13(value: String): Boolean {
        if (value.length != 13 || value.any { !it.isDigit() }) return false
        val checksum = value.last().digitToInt()
        val sum = value.dropLast(1).mapIndexed { index, c ->
            val digit = c.digitToInt()
            if ((index + 1) % 2 == 0) digit * 3 else digit
        }.sum()
        val expected = (10 - (sum % 10)) % 10
        return expected == checksum
    }
}

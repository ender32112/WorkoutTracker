package com.example.workouttracker.ui.nutrition

import com.example.workouttracker.feature.nutrition.presentation.BarcodeScannerParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeScannerParserTest {
    @Test
    fun normalize_stripsSpacesAndNonDigits() {
        assertEquals("4601234567893", BarcodeScannerParser.normalize(" 4601 2345-67893 "))
    }

    @Test
    fun normalize_returnsNullForInvalidEan13Checksum() {
        assertNull(BarcodeScannerParser.normalize("4601234567891"))
    }

    @Test
    fun checksum_validationWorks() {
        assertTrue(BarcodeScannerParser.isValidEan13("4601234567893"))
        assertFalse(BarcodeScannerParser.isValidEan13("4601234567891"))
    }
}

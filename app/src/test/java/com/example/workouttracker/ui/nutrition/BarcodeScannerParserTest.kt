package com.example.workouttracker.ui.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeScannerParserTest {
    @Test
    fun normalize_stripsSpacesAndNonDigits() {
        assertEquals("4601234567890", BarcodeScannerParser.normalize(" 4601 2345-67890 "))
    }

    @Test
    fun normalize_returnsNullForInvalidEan13Checksum() {
        assertNull(BarcodeScannerParser.normalize("4601234567891"))
    }

    @Test
    fun checksum_validationWorks() {
        assertTrue(BarcodeScannerParser.isValidEan13("4601234567890"))
        assertFalse(BarcodeScannerParser.isValidEan13("4601234567891"))
    }
}

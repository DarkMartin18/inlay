package com.example.gridime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutsTest {
    @Test
    fun vowelsShowAcuteAccentFirst() {
        assertEquals("á", Layouts.letterVariants('a').first())
        assertEquals("é", Layouts.letterVariants('e').first())
        assertEquals("í", Layouts.letterVariants('i').first())
        assertEquals("ó", Layouts.letterVariants('o').first())
        assertEquals("ú", Layouts.letterVariants('u').first())
    }

    @Test
    fun includesOtherCommonLatinVariants() {
        assertTrue(Layouts.letterVariants('e').containsAll(listOf("ë", "ê")))
        assertEquals(listOf("ñ", "ń", "ņ"), Layouts.letterVariants('n'))
    }

    @Test
    fun variantsRespectCapitalizationAndUnsupportedLettersStayEmpty() {
        assertEquals("Á", Layouts.letterVariants('A').first())
        assertTrue(Layouts.letterVariants('b').isEmpty())
    }
}

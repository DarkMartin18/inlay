package com.example.gridime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutsTest {
    @Test
    fun floatingKeyboardIsOptInAndR3HasAControllerHint() {
        assertFalse(KeyboardSettings().floatingKeyboard)
        assertTrue(KeyboardSettings().copy(floatingKeyboard = true).floatingKeyboard)
        assertTrue(HintText.parse("{R3}").single() is HintPart.Badge)
    }

    @Test
    fun floatingKeyboardWidthsScaleAroundTheMediumSize() {
        assertEquals(1f / 1.2f, floatingKeyboardWidthScale(0), 0.0001f)
        assertEquals(1f, floatingKeyboardWidthScale(1), 0.0001f)
        assertEquals(1.4f / 1.2f, floatingKeyboardWidthScale(2), 0.0001f)
    }

    @Test
    fun numberRowIsOptionalAndAppearsAboveTheLetters() {
        assertFalse(KeyboardSettings().showNumberRow)
        val rows = Layouts.letterRows(KeyLayout.GRID, showNumberRow = true)
        assertEquals("1234567890", rows.first().joinToString("") { it.label })
        assertEquals("qwertyuiop", rows[1].joinToString("") { it.label })
    }

    @Test
    fun numberRowLayerSwitchKeepsExpectedRows() {
        assertEquals(0, Layouts.counterpartLayerRow(Layer.LETTERS, 0, true))
        assertEquals(1, Layouts.counterpartLayerRow(Layer.LETTERS, 1, true))
        assertEquals(1, Layouts.counterpartLayerRow(Layer.LETTERS, 2, true))
        assertEquals(2, Layouts.counterpartLayerRow(Layer.LETTERS, 3, true))
        assertEquals(3, Layouts.counterpartLayerRow(Layer.LETTERS, 4, true))
        assertEquals(3, Layouts.counterpartLayerRow(Layer.SYMBOLS, 2, true))
        assertEquals(4, Layouts.counterpartLayerRow(Layer.SYMBOLS, 3, true))
        assertEquals(3, Layouts.counterpartLayerRow(Layer.LETTERS, 3, false))
    }

    @Test
    fun variantSelectionCommitsOnReleaseByDefault() {
        assertTrue(KeyboardSettings().commitVariantOnRelease)
    }

    @Test
    fun variantMenuDelayDefaultsToMediumAndPreservesCurrentTimings() {
        assertEquals(VariantMenuDelay.MEDIUM.ordinal, KeyboardSettings().variantMenuDelay)
        assertEquals(500L, VariantMenuDelay.MEDIUM.from(500L))
        assertEquals(450L, VariantMenuDelay.MEDIUM.from(450L))
        assertTrue(VariantMenuDelay.SLOWEST.from(500L) > VariantMenuDelay.MEDIUM.from(500L))
        assertTrue(VariantMenuDelay.FASTEST.from(500L) < VariantMenuDelay.MEDIUM.from(500L))
    }

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

    @Test
    fun symbolsIncludeInvertedQuestionAndExclamationMarks() {
        assertEquals(listOf("¿"), Layouts.symbolVariants('?'))
        assertEquals(listOf("¡"), Layouts.symbolVariants('!'))
        assertTrue(Layouts.symbolVariants('=').isEmpty())
    }
}

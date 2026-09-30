package com.example.gridime

import android.os.SystemClock
import android.view.KeyEvent
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import kotlin.math.max
import kotlin.math.min

/** Text editing helpers that work in any app's text box. */
object TextOps {

    /** The text in the box, plus the position where it starts (some apps share only part). */
    fun textAround(ic: InputConnection, selStart: Int, selEnd: Int): Pair<CharSequence, Int> {
        ic.getExtractedText(ExtractedTextRequest(), 0)?.let { extracted ->
            extracted.text?.let { return it to extracted.startOffset }
        }
        val before = ic.getTextBeforeCursor(2000, 0) ?: ""
        val selected = ic.getSelectedText(0) ?: ""
        val after = ic.getTextAfterCursor(2000, 0) ?: ""
        return (before.toString() + selected + after) to (min(selStart, selEnd) - before.length)
    }

    fun textLength(ic: InputConnection, selStart: Int, selEnd: Int): Int {
        ic.getExtractedText(ExtractedTextRequest(), 0)?.let { extracted ->
            extracted.text?.let { return extracted.startOffset + it.length }
        }
        val after = ic.getTextAfterCursor(100_000, 0)?.length ?: 0
        return max(selStart, selEnd) + after
    }

    /** Where the next word boundary is from [from], going left (-1) or right (+1). */
    fun wordBoundary(text: CharSequence, from: Int, dir: Int): Int {
        var i = from.coerceIn(0, text.length)
        if (dir < 0) {
            while (i > 0 && !text[i - 1].isLetterOrDigit()) i--
            while (i > 0 && text[i - 1].isLetterOrDigit()) i--
        } else {
            while (i < text.length && !text[i].isLetterOrDigit()) i++
            while (i < text.length && text[i].isLetterOrDigit()) i++
        }
        return i
    }

    /** Deletes the word before the cursor, plus the spaces after it. */
    fun deleteWordBefore(ic: InputConnection) {
        val before = ic.getTextBeforeCursor(64, 0) ?: return
        if (before.isEmpty()) return
        val start = wordBoundary(before, before.length, -1)
        ic.deleteSurroundingText(max(1, before.length - start), 0)
    }

    /** Ctrl+Z: the standard undo shortcut most text boxes and browsers understand. */
    fun undo(ic: InputConnection) {
        val now = SystemClock.uptimeMillis()
        val meta = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, meta))
    }
}

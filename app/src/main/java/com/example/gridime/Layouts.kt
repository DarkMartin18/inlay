package com.example.gridime

enum class KeyType { CHAR, SHIFT, LAYER, SPACE, ENTER, DELETE, TOOL, CLIP, EMPTY }
enum class ShiftState { OFF, ONCE, LOCK }
enum class Tool { SELECT, SELECT_ALL, CUT, COPY, PASTE, HISTORY, SETTINGS }
enum class Layer { LETTERS, SYMBOLS, CLIPS }

/**
 * One key on the grid.
 * weight = width in units (a full row is 10 units)
 * hint   = the controller badge shown in the key's corner
 * index  = which clipboard item a clip card holds
 */
data class Key(
    val type: KeyType,
    val label: String = "",
    val weight: Float = 1f,
    val tool: Tool? = null,
    val hint: ControllerButton? = null,
    val index: Int = -1
)

/**
 * All keyboard layouts. Change a row here to change the keyboard.
 *
 * The GRID layout lines every key up in 10 columns, the way Microsoft's
 * controller keyboard does, so Up and Down always land on the key directly above
 * or below. CLASSIC keeps the familiar phone stagger.
 */
object Layouts {
    const val UNITS = 10f
    const val MAX_CLIP_CARDS = 6      // 3 rows of 2; the 4th row holds the button guide

    private fun chars(s: String) = s.map { Key(KeyType.CHAR, it.toString()) }

    private val letterVariants = mapOf(
        'a' to "áàâäãåāăą",
        'e' to "éèêëēėę",
        'i' to "íìîïīį",
        'o' to "óòôöõøō",
        'u' to "úùûüū",
        'y' to "ýÿ",
        'c' to "çćč",
        'd' to "ďđ",
        'g' to "ĝğģ",
        'h' to "ĥ",
        'j' to "ĵ",
        'k' to "ķ",
        'l' to "ľĺļł",
        'n' to "ñńņ",
        'r' to "řŕ",
        's' to "śšş",
        't' to "ţť",
        'z' to "źżž"
    )

    fun letterVariants(char: Char): List<String> {
        val variants = letterVariants[char.lowercaseChar()] ?: return emptyList()
        return variants.map { if (char.isUpperCase()) it.uppercase() else it.toString() }
    }

    fun toolRow(selectAllButton: Boolean): List<Key> {
        val select = Key(KeyType.TOOL, "Select", if (selectAllButton) 1.7f else 1.9f, Tool.SELECT, ControllerButton.SELECT)
        val tail = listOf(
            Key(KeyType.TOOL, "", 0.9f, Tool.HISTORY),
            Key(KeyType.TOOL, "", 0.9f, Tool.SETTINGS)
        )
        return if (selectAllButton) {
            listOf(
                select,
                Key(KeyType.TOOL, "All", 1.3f, Tool.SELECT_ALL),
                Key(KeyType.TOOL, "Cut", 1.0f, Tool.CUT),
                Key(KeyType.TOOL, "Copy", 1.2f, Tool.COPY),
                Key(KeyType.TOOL, "Paste", 3.0f, Tool.PASTE)
            ) + tail
        } else {
            listOf(
                select,
                Key(KeyType.TOOL, "Cut", 1.2f, Tool.CUT),
                Key(KeyType.TOOL, "Copy", 1.3f, Tool.COPY),
                Key(KeyType.TOOL, "Paste", 3.8f, Tool.PASTE)
            ) + tail
        }
    }

    fun letterRows(layout: KeyLayout): List<List<Key>> = when (layout) {
        KeyLayout.GRID -> listOf(
            chars("qwertyuiop"),
            chars("asdfghjkl'"),
            listOf(shift(1f)) + chars("zxcvbnm") + delete(2f),
            bottomRow("?123", space = 4f, side = 2f)
        )
        KeyLayout.CLASSIC -> listOf(
            chars("qwertyuiop"),
            chars("asdfghjkl"),
            listOf(shift(1.5f)) + chars("zxcvbnm") + delete(1.5f),
            bottomRow("?123", space = 5f, side = 1.5f)
        )
    }

    fun symbolRows(layout: KeyLayout): List<List<Key>> {
        val space = if (layout == KeyLayout.GRID) 4f else 5f
        val side = if (layout == KeyLayout.GRID) 2f else 1.5f
        return listOf(
            chars("1234567890"),
            chars("@#\$%&-+()/"),
            chars("*\"':;!?=") + delete(2f),
            bottomRow("ABC", space, side)
        )
    }

    fun clipRows(clips: List<String>): List<List<Key>> {
        if (clips.isEmpty()) {
            return listOf(listOf(Key(KeyType.EMPTY, "Copied text will show up here", UNITS)))
        }
        return clips.take(MAX_CLIP_CARDS).chunked(2).mapIndexed { r, pair ->
            pair.mapIndexed { c, text -> Key(KeyType.CLIP, text, UNITS / 2, index = r * 2 + c) }
        }
    }

    private fun shift(weight: Float) = Key(KeyType.SHIFT, weight = weight, hint = ControllerButton.R2)
    private fun delete(weight: Float) = Key(KeyType.DELETE, weight = weight, hint = ControllerButton.X)

    private fun bottomRow(layerLabel: String, space: Float, side: Float) = listOf(
        Key(KeyType.LAYER, layerLabel, side, hint = ControllerButton.L2),
        Key(KeyType.CHAR, ","),
        Key(KeyType.SPACE, "", space, hint = ControllerButton.Y),
        Key(KeyType.CHAR, "."),
        Key(KeyType.ENTER, "", side, hint = ControllerButton.START)
    )
}

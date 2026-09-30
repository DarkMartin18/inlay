package com.example.gridime

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/** The last few copied texts, newest first, stored only on this device. */
class ClipboardHistory(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(KeyboardSettings.PREFS, Context.MODE_PRIVATE)
    private val items = ArrayList<String>()

    val all: List<String> get() = items

    fun reload() {
        items.clear()
        val raw = prefs.getString(KeyboardSettings.KEY_CLIPS, null) ?: return
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) items.add(array.getString(i))
        }.onFailure { items.clear() }
    }

    fun add(text: String) {
        if (text.isBlank()) return
        val item = text.take(MAX_LENGTH)
        items.remove(item)
        items.add(0, item)
        while (items.size > Layouts.MAX_CLIP_CARDS) items.removeAt(items.lastIndex)
        save()
    }

    fun removeAt(index: Int) {
        if (index !in items.indices) return
        items.removeAt(index)
        save()
    }

    fun clear() {
        items.clear()
        save()
    }

    private fun save() {
        prefs.edit().putString(KeyboardSettings.KEY_CLIPS, JSONArray(items).toString()).apply()
    }

    companion object {
        private const val MAX_LENGTH = 5000

        fun clear(context: Context) {
            context.getSharedPreferences(KeyboardSettings.PREFS, Context.MODE_PRIVATE)
                .edit().remove(KeyboardSettings.KEY_CLIPS).apply()
        }
    }
}

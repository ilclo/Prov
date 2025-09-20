package com.example.appbuilder.runtime

import androidx.compose.ui.graphics.Color

fun resolveBindings(text: String, state: Map<String, String>): String {
    // Replace {{key}} con valore
    return Regex("\\{\\{([a-zA-Z0-9_\\.]+)\\}\\}")
        .replace(text) { m ->
            val key = m.groupValues[1]
            state[key] ?: ""
        }
}

fun parseColorHex(s: String?): Color? = try {
    s?.takeIf { it.startsWith("#") }?.let { Color(android.graphics.Color.parseColor(it)) }
} catch (_: Throwable) { null }

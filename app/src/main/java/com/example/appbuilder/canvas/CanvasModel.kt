package com.example.appbuilder.canvas

import androidx.compose.ui.graphics.Color

/** Stato minimo della pagina renderizzata sul canvas. */
data class PageState(
    val id: String,
    var scroll: String = "Nessuna",
    var gridDensity: Int = 6,
    val items: MutableList<DrawItem> = mutableListOf()
) {
    val levels: MutableSet<Int> = mutableSetOf(0)
    var currentLevel: Int = 0
}

/** Elementi disegnabili: rettangoli e linee, con livello (z-order logico). */
sealed class DrawItem(open var level: Int) {
    data class RectItem(
        override var level: Int,
        var row0: Int, var col0: Int,
        var row1: Int, var col1: Int,
        var strokeArgb: Int = 0xFF000000.toInt()  // nero
    ) : DrawItem(level)

    data class LineItem(
        override var level: Int,
        var row0: Int, var col0: Int,
        var row1: Int, var col1: Int,
        var strokeArgb: Int = 0xFF000000.toInt()
    ) : DrawItem(level)
}

/** Piccola history “undo/redo” (fino a 6 stati). */
class History<T>(private val max: Int = 6, initial: T) {
    private val past = ArrayDeque<T>()
    private val future = ArrayDeque<T>()
    var present: T = initial
        private set

    fun push(state: T) {
        past.addLast(present)
        present = state
        future.clear()
        while (past.size > max) past.removeFirst()
    }

    fun undo(): T? {
        if (past.isEmpty()) return null
        future.addLast(present)
        present = past.removeLast()
        return present
    }

    fun redo(): T? {
        if (future.isEmpty()) return null
        past.addLast(present)
        present = future.removeLast()
        return present
    }
}

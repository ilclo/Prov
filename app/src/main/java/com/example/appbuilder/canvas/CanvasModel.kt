// CanvasModel.kt
package com.example.appbuilder.canvas

import androidx.compose.runtime.mutableStateListOf

data class PageState(
    val id: String,
    val scroll: String,         // "Nessuna" | "Verticale" | "Orizzontale"
    var gridDensity: Int = 6,
    var currentLevel: Int = 0,
    val items: MutableList<DrawItem> = mutableStateListOf()
)

sealed interface DrawItem {
    val level: Int

    data class RectItem(
        override val level: Int,
        val row0: Int, val col0: Int,
        val row1: Int, val col1: Int,
        val borderColor: Long
    ) : DrawItem

    /** Linea orizzontale o verticale espressa in coordinate di cella/“edge” interni */
    data class LineItem(
        override val level: Int,
        val horizontal: Boolean,
        val row: Int,            // se orizzontale, riga fissa
        val col: Int,            // se verticale, colonna fissa
        val x0Edge: Float,       // per orizzontali: da colonna-edge a colonna-edge
        val x1Edge: Float,
        val y0Edge: Float,       // per verticali: da riga-edge a riga-edge
        val y1Edge: Float,
        val thicknessDp: Float = 1f
    ) : DrawItem
}

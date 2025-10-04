package com.example.appbuilder.canvas

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Stato minimo di una “pagina di lavoro”.
 * scrollable: "Nessuna" | "Verticale" | "Orizzontale"
 */
data class PageState(
    var scrollable: String = "Nessuna",
    var gridDensity: Int = 6,
    val items: SnapshotStateList<DrawItem> = mutableStateListOf(),
    val levels: SnapshotStateList<Int> = mutableStateListOf(0),
    var currentLevel: Int = 0
)

/** Elementi disegnabili su griglia. */
sealed class DrawItem(open val level: Int) {

    /** Rettangolo definito da due celle (estremi inclusivi) in coordinate di griglia. */
    data class RectItem(
        override val level: Int,
        val r0: Int, val c0: Int,
        val r1: Int, val c1: Int,
        val borderColor: Color = Color.Black,
        val borderWidth: Dp = 1.dp,
        val fillColor: Color = Color.White
    ) : DrawItem(level)

    /** Linea fra due celle (per step successivo). */
    data class LineItem(
        override val level: Int,
        val r0: Int, val c0: Int,
        val r1: Int, val c1: Int,
        val color: Color = Color.Black,
        val width: Dp = 1.dp
    ) : DrawItem(level)
}
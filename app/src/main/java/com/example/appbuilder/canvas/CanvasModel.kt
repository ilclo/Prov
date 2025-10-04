package com.example.appbuilder.canvas

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class PageState(
    val id: String,
    val scroll: String,                // "Nessuna" | "Verticale" | "Orizzontale"
    var gridDensity: Int = 6,
    var currentLevel: Int = 0,
    val levels: SnapshotStateList<Int> = mutableStateListOf(0),
    val items: SnapshotStateList<DrawItem> = mutableStateListOf()
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

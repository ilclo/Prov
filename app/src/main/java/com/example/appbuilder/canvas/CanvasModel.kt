package com.example.appbuilder.canvas

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class PageState(
    val id: String,
    val scroll: String,          // "Nessuna" | "Verticale" | "Orizzontale"
    val gridDensity: Int,
    val currentLevel: Int,
    val items: SnapshotStateList<DrawItem> = mutableStateListOf()
)

sealed class DrawItem(open val level: Int) {
    data class RectItem(
        override val level: Int,
        val r0: Int, val c0: Int,
        val r1: Int, val c1: Int,
        val borderColor: Color,
        val borderWidth: Dp,
        val fillColor: Color
    ) : DrawItem(level)

    data class LineItem(
        override val level: Int,
        val r0: Int, val c0: Int,
        val r1: Int, val c1: Int,
        val color: Color,
        val width: Dp = 1.dp
    ) : DrawItem(level)
}

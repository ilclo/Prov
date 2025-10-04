package com.example.appbuilder.canvas

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Elementi disegnabili (stub minimo)
sealed class DrawItem {
    data class RectItem(
        val level: Int,
        val row0: Int, val col0: Int,
        val row1: Int, val col1: Int,
        val borderColor: Long = 0xFF000000
    ) : DrawItem()
    data class LineItem(
        val level: Int,
        val row: Int, val col: Int,
        val horizontal: Boolean
    ) : DrawItem()
}

data class PageState(
    val id: String,
    val scroll: String,           // "Nessuna" | "Verticale" | "Orizzontale"
    var gridDensity: Int = 6,
    var currentLevel: Int = 0,
    val levels: SnapshotStateList<Int> = mutableStateListOf(0),
    val items: SnapshotStateList<DrawItem> = mutableStateListOf()
)
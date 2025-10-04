package com.example.appbuilder.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Stage di disegno: mostra pagina, overlay griglia (preview singola cella o griglia intera),
 * e consente la creazione dei contenitori (rettangoli) con 2 tap lunghi.
 *
 * @param page            stato pagina (può essere null nelle fasi iniziali)
 * @param gridDensity     densità corrente (N celle per riga/colonna)
 * @param gridPreviewOnly true => mostra solo il primo quadrato in alto-sx (mentre trascini lo slider)
 * @param showFullGrid    true => mostra la griglia completa (dopo 0.5s che non trascini)
 * @param currentLevel    livello attivo (si disegnano gli elementi con level <= currentLevel)
 * @param onAddItem       callback per aggiungere un nuovo elemento creato dall’utente
 */
@Composable
fun CanvasStage(
    page: PageState?,
    gridDensity: Int,
    gridPreviewOnly: Boolean,
    showFullGrid: Boolean,
    currentLevel: Int,
    onAddItem: (DrawItem) -> Unit
) {
    // Evidenziazione cella corrente (tap singolo)
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // Primo estremo fissato con tap lungo
    var firstAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val density = max(1, gridDensity)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(density) {
                detectTapGestures(
                    onTap = { ofs ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val cell = computeCell(ofs, density, w, h)
                        hoverCell = cell
                    },
                    onLongPress = { ofs ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val cell = computeCell(ofs, density, w, h)
                        if (firstAnchor == null) {
                            // 1° quadrato fissato
                            firstAnchor = cell
                        } else {
                            // 2° quadrato: crea rettangolo ai lati esterni
                            val (r0, c0) = firstAnchor!!
                            val (r1, c1) = cell
                            val rect = DrawItem.RectItem(
                                level = currentLevel,
                                row0 = min(r0, r1),
                                col0 = min(c0, c1),
                                row1 = max(r0, r1),
                                col1 = max(c0, c1),
                                borderColor = 0xFF000000L // nero
                            )
                            onAddItem(rect)
                            firstAnchor = null
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = density
            // lato cella: uso il min tra lato orizzontale/verticale
            val cell = min(size.width / cols, size.height / cols)
            val rows = if (cell > 0f) floor(size.height / cell).toInt() else 0

            // 1) Disegna elementi fino al currentLevel
            page?.items?.forEach { item ->
                when (item) {
                    is DrawItem.RectItem -> if (item.level <= currentLevel) {
                        val left = min(item.col0, item.col1) * cell
                        val top = min(item.row0, item.row1) * cell
                        val w = (abs(item.col1 - item.col0) + 1) * cell
                        val h = (abs(item.row1 - item.row0) + 1) * cell

                        // riempimento: bianco (default richiesto)
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(left, top),
                            size = Size(w, h)
                        )
                        // bordo: converto Long -> Color
                        val border = Color((item.borderColor and 0xFFFFFFFF).toInt())
                        drawRect(
                            color = border,
                            topLeft = Offset(left, top),
                            size = Size(w, h),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    is DrawItem.LineItem -> if (item.level <= currentLevel) {
                        val azure = Color(0xFF58A6FF)
                        if (item.horizontal) {
                            val y = (item.row + 0.5f) * cell
                            drawLine(
                                color = azure,
                                start = Offset(0f, y),
                                end = Offset(cols * cell, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        } else {
                            val x = (item.col + 0.5f) * cell
                            drawLine(
                                color = azure,
                                start = Offset(x, 0f),
                                end = Offset(x, rows * cell),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }
            }

            // 2) Overlay griglia: preview o griglia intera
            val azure = Color(0xFF58A6FF)
            if (gridPreviewOnly) {
                // solo la prima cella (0,0)
                if (rows > 0 && cols > 0) {
                    drawRect(
                        color = azure.copy(alpha = 0.20f),
                        topLeft = Offset(0f, 0f),
                        size = Size(cell, cell)
                    )
                    drawRect(
                        color = azure,
                        topLeft = Offset(0f, 0f),
                        size = Size(cell, cell),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            } else if (showFullGrid) {
                // tutte le linee della griglia
                for (c in 0..cols) {
                    val x = c * cell
                    drawLine(
                        color = azure.copy(alpha = 0.30f),
                        start = Offset(x, 0f),
                        end = Offset(x, rows * cell),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                for (r in 0..rows) {
                    val y = r * cell
                    drawLine(
                        color = azure.copy(alpha = 0.30f),
                        start = Offset(0f, y),
                        end = Offset(cols * cell, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // 3) Evidenziazione hover (tap singolo)
            hoverCell?.let { (rr, cc) ->
                if (rr in 0 until rows && cc in 0 until cols) {
                    drawRect(
                        color = azure.copy(alpha = 0.18f),
                        topLeft = Offset(cc * cell, rr * cell),
                        size = Size(cell, cell)
                    )
                    drawRect(
                        color = azure,
                        topLeft = Offset(cc * cell, rr * cell),
                        size = Size(cell, cell),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // 4) Riga/colonna del primo ancoraggio (dopo 1° long‑press)
            firstAnchor?.let { (rr, cc) ->
                if (rr in 0 until rows && cc in 0 until cols) {
                    // colonna evidenziata
                    drawRect(
                        color = azure.copy(alpha = 0.10f),
                        topLeft = Offset(cc * cell, 0f),
                        size = Size(cell, rows * cell)
                    )
                    // riga evidenziata
                    drawRect(
                        color = azure.copy(alpha = 0.10f),
                        topLeft = Offset(0f, rr * cell),
                        size = Size(cols * cell, cell)
                    )
                    // cella di ancoraggio
                    drawRect(
                        color = azure.copy(alpha = 0.22f),
                        topLeft = Offset(cc * cell, rr * cell),
                        size = Size(cell, cell)
                    )
                    drawRect(
                        color = azure,
                        topLeft = Offset(cc * cell, rr * cell),
                        size = Size(cell, cell),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}

private fun computeCell(ofs: Offset, cols: Int, w: Float, h: Float): Pair<Int, Int> {
    val cell = min(w / cols, h / cols)
    val r = floor(ofs.y / cell).toInt().coerceAtLeast(0)
    val c = floor(ofs.x / cell).toInt().coerceAtLeast(0)
    return r to c
}
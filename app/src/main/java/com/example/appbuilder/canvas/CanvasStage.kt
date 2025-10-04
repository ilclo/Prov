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
 * @param page             stato pagina (può essere null nelle fasi iniziali)
 * @param gridDensity      densità corrente (N celle per riga/colonna)
 * @param gridPreviewOnly  true => mostra solo il primo quadrato in alto-sx (mentre trascini lo slider)
 * @param showFullGrid     true => mostra la griglia completa (dopo 0.5s che non trascini)
 * @param currentLevel     livello attivo (si disegnano gli elementi con level <= currentLevel)
 * @param creationEnabled  gate: consente il “modo creazione contenitore” solo quando abilitato
 * @param onAddItem        callback per aggiungere un nuovo elemento creato dall’utente
 */
@Composable
fun CanvasStage(
    page: PageState?,
    gridDensity: Int,
    gridPreviewOnly: Boolean,
    showFullGrid: Boolean,
    currentLevel: Int,
    creationEnabled: Boolean,
    onAddItem: (DrawItem) -> Unit
) {
    // Evidenziazione cella corrente (tap singolo)
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // Primo estremo fissato con tap lungo
    var firstAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // La pagina potrebbe essere null: qui usiamo la densità fornita dal chiamante
    val density = max(1, gridDensity)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(density, creationEnabled) {
                detectTapGestures(
                    onTap = { ofs ->
                        if (!creationEnabled) return@detectTapGestures
                        val cell = computeCell(ofs, density, this.size.width.toFloat(), this.size.height.toFloat())
                        hoverCell = cell
                    },
                    onLongPress = { ofs ->
                        if (!creationEnabled) return@detectTapGestures
                        val cell = computeCell(ofs, density, this.size.width.toFloat(), this.size.height.toFloat())
                        if (firstAnchor == null) {
                            // Fissa il primo quadrato
                            firstAnchor = cell
                        } else {
                            // Secondo quadrato: crea il rettangolo ai lati esterni
                            val (r0, c0) = firstAnchor!!
                            val (r1, c1) = cell
                            val rect = DrawItem.RectItem(
                                level = currentLevel,
                                r0 = min(r0, r1), c0 = min(c0, c1),
                                r1 = max(r0, r1), c1 = max(c0, c1),
                                borderColor = Color.Black,
                                borderWidth = 1.dp,
                                fillColor = Color.White
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
            // Celle quadrate: uso il lato minore
            val cell = min(size.width / cols, size.height / cols)
            val rows = if (cell > 0f) floor(size.height / cell).toInt() else 0

            // 1) Disegno elementi esistenti fino al currentLevel
            page?.items?.forEach { item ->
                if (item.level <= currentLevel) {
                    when (item) {
                        is DrawItem.RectItem -> {
                            val left = min(item.c0, item.c1) * cell
                            val top = min(item.r0, item.r1) * cell
                            val w = (abs(item.c1 - item.c0) + 1) * cell
                            val h = (abs(item.r1 - item.r0) + 1) * cell
                            // riempimento
                            drawRect(
                                color = item.fillColor,
                                topLeft = Offset(left, top),
                                size = Size(w, h)
                            )
                            // bordo
                            drawRect(
                                color = item.borderColor,
                                topLeft = Offset(left, top),
                                size = Size(w, h),
                                style = Stroke(width = item.borderWidth.toPx())
                            )
                        }
                        is DrawItem.LineItem -> {
                            val x0 = (item.c0 + 0.5f) * cell
                            val y0 = (item.r0 + 0.5f) * cell
                            val x1 = (item.c1 + 0.5f) * cell
                            val y1 = (item.r1 + 0.5f) * cell
                            drawLine(
                                color = item.color,
                                start = Offset(x0, y0),
                                end = Offset(x1, y1),
                                strokeWidth = item.width.toPx()
                            )
                        }
                    }
                }
            }

            // 2) Overlay griglia: preview (solo cella 0,0) o griglia completa
            val azure = Color(0xFF58A6FF)
            if (gridPreviewOnly) {
                // disegna solo il primo quadrato (in alto a sinistra)
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
                // griglia completa
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

            // 4) Riga/colonna del primo ancoraggio (dopo primo long-press)
            firstAnchor?.let { (rr, cc) ->
                if (rr in 0 until rows && cc in 0 until cols) {
                    // colonna
                    drawRect(
                        color = azure.copy(alpha = 0.10f),
                        topLeft = Offset(cc * cell, 0f),
                        size = Size(cell, rows * cell)
                    )
                    // riga
                    drawRect(
                        color = azure.copy(alpha = 0.10f),
                        topLeft = Offset(0f, rr * cell),
                        size = Size(cols * cell, cell)
                    )
                    // cella stessa più marcata
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

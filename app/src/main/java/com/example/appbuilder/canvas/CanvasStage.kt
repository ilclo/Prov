package com.example.appbuilder.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * Stage centrale: rende la pagina, la griglia (preview vs full),
 * gestisce il tap breve/lungo per creare rettangoli/linee su griglia,
 * e disegna gli elementi fino al livello selezionato.
 */
@Composable
fun CanvasStage(
    page: PageState?,                        // null => nessuna pagina (placeholder vuoto)
    gridDensity: Int,                        // densità attuale
    gridPreviewOnly: Boolean,                // true = mostra solo il 1° quadrato in alto-sx
    showFullGrid: Boolean,                   // true = disegna tutta la griglia
    currentLevel: Int,                       // livello di rendering selezionato
    onAddItem: (DrawItem) -> Unit            // callback creazione elemento
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x00000000)),
        contentAlignment = Alignment.Center
    ) {
        if (page == null) return@BoxWithConstraints

        // Dimensionamento "pagina": rettangolo bianco centrato, 9:16, con limiti soft
        val maxW = maxWidth - 48.dp
        val pageW = minOf(maxW, 420.dp)
        val pageH = minOf(maxHeight - 200.dp, pageW * (16f / 9f))
        Surface(
            color = Color.White,
            contentColor = MaterialTheme.colorScheme.onBackground,
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(pageW)
                .height(pageH)
        ) {
            PageContent(
                page = page,
                grid = gridDensity.coerceIn(2, 40),
                previewOnly = gridPreviewOnly,
                showFullGrid = showFullGrid,
                currentLevel = currentLevel,
                onAddItem = onAddItem
            )
        }
    }
}

@Composable
private fun PageContent(
    page: PageState,
    grid: Int,
    previewOnly: Boolean,
    showFullGrid: Boolean,
    currentLevel: Int,
    onAddItem: (DrawItem) -> Unit
) {
    var firstCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(grid) {
                detectTapGestures(
                    onTap = { pos ->
                        val (r, c) = pos.toCell(size.width, size.height, grid) ?: return@detectTapGestures
                        hoverCell = r to c
                    },
                    onLongPress = { pos ->
                        val (r, c) = pos.toCell(size.width, size.height, grid) ?: return@detectTapGestures
                        if (firstCell == null) {
                            firstCell = r to c
                        } else {
                            val (r0, c0) = firstCell!!
                            // stessa riga/colonna => linea; altrimenti rettangolo
                            if (r0 == r || c0 == c) {
                                onAddItem(
                                    DrawItem.LineItem(
                                        level = page.currentLevel,
                                        row0 = r0, col0 = c0, row1 = r, col1 = c
                                    )
                                )
                            } else {
                                onAddItem(
                                    DrawItem.RectItem(
                                        level = page.currentLevel,
                                        row0 = min(r0, r), col0 = min(c0, c),
                                        row1 = max(r0, r), col1 = max(c0, c)
                                    )
                                )
                            }
                            page.levels.add(page.currentLevel)
                            firstCell = null
                            hoverCell = null
                        }
                    }
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cw = size.width / grid
            val ch = size.height / grid

            // 1) Griglia: preview vs full
            if (previewOnly) {
                // solo cella [0,0]
                drawRect(
                    color = Color(0x3358A6FF),
                    topLeft = Offset(0f, 0f),
                    size = Size(cw, ch),
                    style = Stroke(width = 2f)
                )
            } else if (showFullGrid) {
                // tutte le righe/colonne
                val stroke = Stroke(width = 1f)
                val clr = Color(0x2258A6FF)
                for (i in 0..grid) {
                    val x = i * cw
                    val y = i * ch
                    drawLine(clr, Offset(x, 0f), Offset(x, size.height), stroke.width)
                    drawLine(clr, Offset(0f, y), Offset(size.width, y), stroke.width)
                }
            }

            // 2) Righe/colonne evidenziate se ho scelto il 1° quadrato
            firstCell?.let { (r0, c0) ->
                val hi = Color(0x3358A6FF)
                // colonna
                drawRect(hi, topLeft = Offset(c0 * cw, 0f), size = Size(cw, size.height))
                // riga
                drawRect(hi, topLeft = Offset(0f, r0 * ch), size = Size(size.width, ch))
            }

            // 3) Cella "hover"
            hoverCell?.let { (r, c) ->
                drawRect(
                    color = Color(0x6658A6FF),
                    topLeft = Offset(c * cw, r * ch),
                    size = Size(cw, ch),
                    style = Stroke(width = 2f)
                )
            }

            // 4) Elementi: disegno fino al livello corrente (incluso)
            page.items
                .filter { it.level <= currentLevel }
                .forEach { item ->
                    when (item) {
                        is DrawItem.RectItem -> {
                            val left = item.col0 * cw
                            val top = item.row0 * ch
                            val w = (item.col1 - item.col0 + 1) * cw
                            val h = (item.row1 - item.row0 + 1) * ch
                            drawRect(
                                color = Color.Transparent,
                                topLeft = Offset(left, top),
                                size = Size(w, h),
                                style = Stroke(width = 3f, miter = 1f),
                                alpha = 1f
                            )
                        }
                        is DrawItem.LineItem -> {
                            val x0 = (item.col0 + 0.5f) * cw
                            val y0 = (item.row0 + 0.5f) * ch
                            val x1 = (item.col1 + 0.5f) * cw
                            val y1 = (item.row1 + 0.5f) * ch
                            drawLine(
                                color = Color.Black,
                                start = Offset(x0, y0),
                                end = Offset(x1, y1),
                                strokeWidth = 3f
                            )
                        }
                    }
                }
        }
    }
}

/** Conversione coordinate → cella griglia (riga, colonna), oppure null se fuori. */
private fun Offset.toCell(w: Float, h: Float, g: Int): Pair<Int, Int>? {
    if (x < 0f || y < 0f || x > w || y > h) return null
    val cw = w / g
    val ch = h / g
    val c = (x / cw).toInt().coerceIn(0, g - 1)
    val r = (y / ch).toInt().coerceIn(0, g - 1)
    return r to c
}

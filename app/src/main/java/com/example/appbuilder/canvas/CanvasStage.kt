package com.example.appbuilder.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor

/**
 * Specifica della griglia “grammatura” (righe/colonne + gap tra celle).
 */
data class GridSpec(
    val rows: Int,
    val cols: Int,
    val gap: Dp = 0.dp
)

/**
 * Stage di lavoro renderizzato a schermo. Disegna opzionalmente una griglia
 * e consente hit-test su cella con tap e long-press (delegati esterni).
 *
 * NOTA: tutti i calcoli interni sono in pixel float → niente errori Int/Float.
 */
@Composable
fun CanvasStage(
    modifier: Modifier = Modifier,
    grid: GridSpec = GridSpec(rows = 12, cols = 8, gap = 0.dp),
    showGrid: Boolean = true,
    spotlightCell: Pair<Int, Int>? = null,            // es. primo quadrato in alto a sx durante lo slide
    onTapCell: ((row: Int, col: Int) -> Unit)? = null,
    onLongPressCell: ((row: Int, col: Int) -> Unit)? = null
) {
    val gapPx = with(LocalDensity.current) { grid.gap.toPx() }

    Box(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(grid.rows, grid.cols) {
                    detectTapGesturesCompat(
                        onTap = { x, y, w, h ->
                            val (r, c) = cellAt(x, y, w, h, grid.rows, grid.cols, gapPx)
                            onTapCell?.invoke(r, c)
                        },
                        onLongPress = { x, y, w, h ->
                            val (r, c) = cellAt(x, y, w, h, grid.rows, grid.cols, gapPx)
                            onLongPressCell?.invoke(r, c)
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height

            if (showGrid) {
                val cellW = (w - gapPx * (grid.cols - 1)) / grid.cols
                val cellH = (h - gapPx * (grid.rows - 1)) / grid.rows
                val gridColor = Color(0x40FFFFFF)

                // Verticali
                var x = 0f
                for (c in 0..grid.cols) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                    x += cellW
                    if (c < grid.cols) x += gapPx
                }
                // Orizzontali
                var y = 0f
                for (r in 0..grid.rows) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                    y += cellH
                    if (r < grid.rows) y += gapPx
                }

                // Spotlight cella (outline tratteggiato)
                spotlightCell?.let { (sr, sc) ->
                    if (sr in 0 until grid.rows && sc in 0 until grid.cols) {
                        val left = (cellW + gapPx) * sc
                        val top  = (cellH + gapPx) * sr
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(left, top),
                            size = Size(cellW, cellH),
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                            )
                        )
                    }
                }
            }
        }
    }
}

/* ---------------------------------------------------------------------------------------------- */
/* Utilities                                                                                      */
/* ---------------------------------------------------------------------------------------------- */

/**
 * Hit-test: converte coordinate touch (x,y) → (row,col) rispettando gap.
 */
private fun cellAt(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    rows: Int,
    cols: Int,
    gapPx: Float
): Pair<Int, Int> {
    val cellW = (w - gapPx * (cols - 1)) / cols
    val cellH = (h - gapPx * (rows - 1)) / rows
    var col = floor(x / (cellW + gapPx)).toInt()
    var row = floor(y / (cellH + gapPx)).toInt()
    if (col < 0) col = 0
    if (row < 0) row = 0
    if (col >= cols) col = cols - 1
    if (row >= rows) row = rows - 1
    return row to col
}

/**
 * Variante compatibile (senza dipendere da Gesture lib esterne): cattura tap e long-press
 * ed espone le coordinate + dimensioni della canvas per il mapping cellAt.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapGesturesCompat(
    onTap: (x: Float, y: Float, w: Float, h: Float) -> Unit,
    onLongPress: (x: Float, y: Float, w: Float, h: Float) -> Unit
) {
    // Usiamo la dimensione disponibile della Canvas tramite size
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            val w = size.width.toFloat()
            val h = size.height.toFloat()

            var longPressed = false
            var upHappened = false

            // Timer long-press ~500ms
            val job = kotlinx.coroutines.launch {
                kotlinx.coroutines.delay(500)
                if (!upHappened) {
                    longPressed = true
                    onLongPress(down.position.x, down.position.y, w, h)
                }
            }

            val up = waitForUpOrCancellation()
            upHappened = true
            job.cancel()

            if (!longPressed && up != null) {
                onTap(up.position.x, up.position.y, w, h)
            }
        }
    }
}

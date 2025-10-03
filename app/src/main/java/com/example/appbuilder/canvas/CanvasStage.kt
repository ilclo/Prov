package com.example.appbuilder.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.roundToInt

// PageState e DrawItem sono dichiarati in CanvasModel.kt nello stesso package.
// NON ridefinirli qui, così eviti "Redeclaration".

/**
 * Overload allineato a EditorKit:
 *
 * CanvasStage(
 *   page = pageState,
 *   gridDensity = pageState?.gridDensity ?: 6,
 *   gridPreviewOnly = ...,
 *   showFullGrid = ...,
 *   currentLevel = currentLevel,
 *   onAddItem = { item: DrawItem -> ... }
 * )
 *
 * Nota: DrawItem è sealed → qui NON lo istanziamo. Il callback resta per compatibilità,
 * ma non è invocato finché non agganciamo una factory coerente con le tue sottoclassi.
 */
@Composable
fun CanvasStage(
    page: PageState? = null,
    gridDensity: Int = page?.gridDensity ?: 6,
    gridPreviewOnly: Boolean = false,
    showFullGrid: Boolean = false,
    currentLevel: Int = page?.currentLevel ?: 0,
    onAddItem: (DrawItem) -> Unit = {},   // compatibilità con EditorKit; non usato qui
    modifier: Modifier = Modifier
) {
    val rows = gridDensity.coerceIn(1, 64)
    val cols = gridDensity.coerceIn(1, 64)

    val showGrid = gridPreviewOnly || showFullGrid
    val spotlight = if (gridPreviewOnly) (0 to 0) else null

    CanvasStageInternal(
        modifier = modifier,
        grid = GridSpec(rows = rows, cols = cols, gap = 0.dp),
        showGrid = showGrid,
        spotlightCell = spotlight,
        onTapCell = { _, _ -> /* in futuro userai onAddItem con una factory */ },
        onLongPressCell = { _, _ -> }
    )
}

/* --------------------------- Disegno griglia interno --------------------------- */

data class GridSpec(
    val rows: Int,
    val cols: Int,
    val gap: Dp = 0.dp
)

@Composable
private fun CanvasStageInternal(
    modifier: Modifier = Modifier,
    grid: GridSpec = GridSpec(rows = 12, cols = 8, gap = 0.dp),
    showGrid: Boolean = true,
    spotlightCell: Pair<Int, Int>? = null,
    onTapCell: ((row: Int, col: Int) -> Unit)? = null,
    onLongPressCell: ((row: Int, col: Int) -> Unit)? = null
) {
    val gapPx = with(LocalDensity.current) { grid.gap.toPx() }

    Box(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(grid.rows, grid.cols, showGrid) {
                    detectTapGestures(
                        onTap = { offset ->
                            val (r, c) = cellAt(
                                x = offset.x,
                                y = offset.y,
                                w = size.width,
                                h = size.height,
                                rows = grid.rows,
                                cols = grid.cols,
                                gapPx = gapPx
                            )
                            onTapCell?.invoke(r, c)
                        },
                        onLongPress = { offset ->
                            val (r, c) = cellAt(
                                x = offset.x,
                                y = offset.y,
                                w = size.width,
                                h = size.height,
                                rows = grid.rows,
                                cols = grid.cols,
                                gapPx = gapPx
                            )
                            onLongPressCell?.invoke(r, c)
                        }
                    )
                }
        ) {
            val w: Float = size.width
            val h: Float = size.height

            if (showGrid) {
                val cellW: Float = (w - gapPx * (grid.cols - 1)) / grid.cols.toFloat()
                val cellH: Float = (h - gapPx * (grid.rows - 1)) / grid.rows.toFloat()
                val gridColor = Color(0x40FFFFFF)

                // Colonne
                var x = 0f
                for (c in 0..grid.cols) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f          // Float, NON Int
                    )
                    x += cellW
                    if (c < grid.cols) x += gapPx
                }
                // Righe
                var y = 0f
                for (r in 0..grid.rows) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f          // Float, NON Int
                    )
                    y += cellH
                    if (r < grid.rows) y += gapPx
                }

                // Spotlight cella (outline tratteggiato)
                spotlightCell?.let { (sr, sc) ->
                    if (sr in 0 until grid.rows && sc in 0 until grid.cols) {
                        val left = (cellW + gapPx) * sc.toFloat()  // Int → Float
                        val top  = (cellH + gapPx) * sr.toFloat()  // Int → Float
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(left, top),
                            size = Size(cellW, cellH),
                            style = Stroke(
                                width = 2f,                             // Float
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(8f, 8f)               // Float[]
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun cellAt(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    rows: Int,
    cols: Int,
    gapPx: Float
): Pair<Int, Int> {
    val cellW: Float = (w - gapPx * (cols - 1)) / cols.toFloat()
    val cellH: Float = (h - gapPx * (rows - 1)) / rows.toFloat()
    var col = floor(x / (cellW + gapPx)).roundToInt()
    var row = floor(y / (cellH + gapPx)).roundToInt()
    if (col < 0) col = 0
    if (row < 0) row = 0
    if (col >= cols) col = cols - 1
    if (row >= rows) row = rows - 1
    return row to col
}
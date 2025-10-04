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
 * Stage di disegno: sfondo pagina, griglia, creazione rettangoli/linee da 2 tap (il secondo long‑press).
 *
 * - page != null  => disegna fondo bianco (pagina visibile).
 * - stessa riga/colonna => crea LINEA che unisce i lati interni dei due quadrati.
 * - altrimenti      => crea RETTANGOLO che ha come estremi i due quadrati.
 * - se una linea attraversa dei rettangoli esistenti => split (versione base, senza dialoghi).
 */
@Composable
fun CanvasStage(
    page: PageState?,
    gridDensity: Int,
    gridPreviewOnly: Boolean,
    showFullGrid: Boolean,
    currentLevel: Int,
    creationEnabled: Boolean = true,
    onAddItem: (DrawItem) -> Unit
) {
    // Evidenziazione cella (tap singolo) e primo ancoraggio (long press)
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var firstAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val cols = max(1, gridDensity)
    val azure = Color(0xFF58A6FF)

    // --- helper locali (no enum, no material) ---
    fun overlappedByHorizontal(
        row: Int, cStart: Int, cEnd: Int, rect: DrawItem.RectItem
    ): Boolean {
        val r0 = min(rect.r0, rect.r1); val r1 = max(rect.r0, rect.r1)
        val c0 = min(rect.c0, rect.c1); val c1 = max(rect.c0, rect.c1)
        if (row !in r0..r1) return false
        return !(cEnd < c0 || cStart > c1) // intervalli che si toccano/sovrappongono
    }
    fun overlappedByVertical(
        col: Int, rStart: Int, rEnd: Int, rect: DrawItem.RectItem
    ): Boolean {
        val r0 = min(rect.r0, rect.r1); val r1 = max(rect.r0, rect.r1)
        val c0 = min(rect.c0, rect.c1); val c1 = max(rect.c0, rect.c1)
        if (col !in c0..c1) return false
        return !(rEnd < r0 || rStart > r1)
    }
    fun crossesInsideHoriz(row: Int, rect: DrawItem.RectItem): Boolean {
        // "dentro" significa non sul bordo: r0 < row < r1
        val r0 = min(rect.r0, rect.r1); val r1 = max(rect.r0, rect.r1)
        return row > r0 && row < r1
    }
    fun crossesInsideVert(col: Int, rect: DrawItem.RectItem): Boolean {
        val c0 = min(rect.c0, rect.c1); val c1 = max(rect.c0, rect.c1)
        return col > c0 && col < c1
    }
    fun splitRectHoriz(row: Int, rect: DrawItem.RectItem): List<DrawItem.RectItem> {
        val r0 = min(rect.r0, rect.r1); val r1 = max(rect.r0, rect.r1)
        val c0 = min(rect.c0, rect.c1); val c1 = max(rect.c0, rect.c1)
        if (row <= r0 || row >= r1) return listOf(rect)
        val top = rect.copy(r0 = r0, r1 = row, c0 = c0, c1 = c1)
        val bottom = rect.copy(r0 = row + 1, r1 = r1, c0 = c0, c1 = c1)
        return listOf(top, bottom)
    }
    fun splitRectVert(col: Int, rect: DrawItem.RectItem): List<DrawItem.RectItem> {
        val r0 = min(rect.r0, rect.r1); val r1 = max(rect.r0, rect.r1)
        val c0 = min(rect.c0, rect.c1); val c1 = max(rect.c0, rect.c1)
        if (col <= c0 || col >= c1) return listOf(rect)
        val left = rect.copy(r0 = r0, r1 = r1, c0 = c0, c1 = col)
        val right = rect.copy(r0 = r0, r1 = r1, c0 = col + 1, c1 = c1)
        return listOf(left, right)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cols, creationEnabled) {
                detectTapGestures(
                    onTap = { ofs ->
                        if (!creationEnabled) return@detectTapGestures
                        val cell = computeCell(
                            ofs = ofs,
                            cols = cols,
                            w = this.size.width.toFloat(),
                            h = this.size.height.toFloat()
                        )
                        hoverCell = cell
                    },
                    onLongPress = { ofs ->
                        if (!creationEnabled) return@detectTapGestures
                        val cell = computeCell(
                            ofs = ofs,
                            cols = cols,
                            w = this.size.width.toFloat(),
                            h = this.size.height.toFloat()
                        )
                        if (firstAnchor == null) {
                            firstAnchor = cell
                        } else {
                            val (r0, c0) = firstAnchor!!
                            val (r1, c1) = cell
                            val rr0 = min(r0, r1); val rr1 = max(r0, r1)
                            val cc0 = min(c0, c1); val cc1 = max(c0, c1)

                            if (r0 == r1 || c0 == c1) {
                                // ===== LINEA =====
                                val rects = page?.items?.filterIsInstance<DrawItem.RectItem>().orEmpty()

                                if (r0 == r1) {
                                    // orizzontale (stessa riga)
                                    val overlapped = rects.filter { overlappedByHorizontal(r0, cc0, cc1, it) }
                                    val splitTargets = overlapped.filter { crossesInsideHoriz(r0, it) }
                                    // split (base): spezza i rettangoli interni alla linea
                                    splitTargets.forEach { rect ->
                                        page?.items?.remove(rect)
                                        page?.items?.addAll(splitRectHoriz(r0, rect))
                                    }
                                    // livello sopra a ciò che incrocia
                                    val overMax = overlapped.maxByOrNull { it.level }?.level ?: currentLevel
                                    val line = DrawItem.LineItem(
                                        level = overMax + 1,
                                        r0 = r0, c0 = cc0,
                                        r1 = r0, c1 = cc1,
                                        color = azure,
                                        width = 2.dp
                                    )
                                    onAddItem(line)
                                } else {
                                    // verticale (stessa colonna)
                                    val overlapped = rects.filter { overlappedByVertical(c0, rr0, rr1, it) }
                                    val splitTargets = overlapped.filter { crossesInsideVert(c0, it) }
                                    splitTargets.forEach { rect ->
                                        page?.items?.remove(rect)
                                        page?.items?.addAll(splitRectVert(c0, rect))
                                    }
                                    val overMax = overlapped.maxByOrNull { it.level }?.level ?: currentLevel
                                    val line = DrawItem.LineItem(
                                        level = overMax + 1,
                                        r0 = rr0, c0 = c0,
                                        r1 = rr1, c1 = c0,
                                        color = azure,
                                        width = 2.dp
                                    )
                                    onAddItem(line)
                                }
                            } else {
                                // ===== RETTANGOLO =====
                                val rect = DrawItem.RectItem(
                                    level = currentLevel,
                                    r0 = rr0, c0 = cc0,
                                    r1 = rr1, c1 = cc1,
                                    borderColor = Color.Black,
                                    borderWidth = 1.dp,
                                    fillColor = Color.White
                                )
                                onAddItem(rect)
                            }

                            firstAnchor = null
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = min(size.width / cols, size.height / cols)
            val rows = if (cell > 0f) floor(size.height / cell).toInt() else 0

            // (0) Fondo pagina bianco
            if (page != null) {
                drawRect(color = Color.White, topLeft = Offset.Zero, size = size)
            }

            // (1) Disegna elementi fino al livello corrente
            page?.items?.forEach { item ->
                if (item.level <= currentLevel) {
                    when (item) {
                        is DrawItem.RectItem -> {
                            val left = min(item.c0, item.c1).toFloat() * cell
                            val top  = min(item.r0, item.r1).toFloat() * cell
                            val w = (abs(item.c1 - item.c0) + 1).toFloat() * cell
                            val h = (abs(item.r1 - item.r0) + 1).toFloat() * cell
                            drawRect(
                                color = item.fillColor,
                                topLeft = Offset(left, top),
                                size = Size(w, h)
                            )
                            drawRect(
                                color = item.borderColor,
                                topLeft = Offset(left, top),
                                size = Size(w, h),
                                style = Stroke(width = item.borderWidth.toPx())
                            )
                        }
                        is DrawItem.LineItem -> {
                            if (item.r0 == item.r1) {
                                // Orizzontale: dai lati interni
                                val row = item.r0
                                val cMin = min(item.c0, item.c1)
                                val cMax = max(item.c0, item.c1)
                                val y = (row + 0.5f) * cell
                                val xStart = (cMin + 1).toFloat() * cell
                                val xEnd   = (cMax).toFloat() * cell
                                drawLine(
                                    color = item.color,
                                    start = Offset(xStart, y),
                                    end   = Offset(xEnd,   y),
                                    strokeWidth = item.width.toPx()
                                )
                            } else if (item.c0 == item.c1) {
                                // Verticale: dai lati interni
                                val col = item.c0
                                val rMin = min(item.r0, item.r1)
                                val rMax = max(item.r0, item.r1)
                                val x = (col + 0.5f) * cell
                                val yStart = (rMin + 1).toFloat() * cell
                                val yEnd   = (rMax).toFloat() * cell
                                drawLine(
                                    color = item.color,
                                    start = Offset(x, yStart),
                                    end   = Offset(x, yEnd),
                                    strokeWidth = item.width.toPx()
                                )
                            }
                        }
                    }
                }
            }

            // (2) Griglia
            if (gridPreviewOnly) {
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
                for (c in 0..cols) {
                    val x = c.toFloat() * cell
                    drawLine(
                        color = azure.copy(alpha = 0.30f),
                        start = Offset(x, 0f),
                        end = Offset(x, rows.toFloat() * cell),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                for (r in 0..rows) {
                    val y = r.toFloat() * cell
                    drawLine(
                        color = azure.copy(alpha = 0.30f),
                        start = Offset(0f, y),
                        end = Offset(cols.toFloat() * cell, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // (3) Hover cell (tap singolo)
            hoverCell?.let { (rr, cc) ->
                if (rr in 0 until rows && cc in 0 until cols) {
                    drawRect(
                        color = azure.copy(alpha = 0.18f),
                        topLeft = Offset(cc.toFloat() * cell, rr.toFloat() * cell),
                        size = Size(cell, cell)
                    )
                    drawRect(
                        color = azure,
                        topLeft = Offset(cc.toFloat() * cell, rr.toFloat() * cell),
                        size = Size(cell, cell),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // (4) Riga/colonna evidenziata dopo 1° long‑press
            firstAnchor?.let { (rr, cc) ->
                if (rr in 0 until rows && cc in 0 until cols) {
                    // colonna
                    drawRect(
                        color = azure.copy(alpha = 0.10f),
                        topLeft = Offset(cc.toFloat() * cell, 0f),
                        size = Size(cell, rows.toFloat() * cell)
                    )
                    // riga
                    drawRect(
                        color = azure.copy(alpha = 0.10f),
                        topLeft = Offset(0f, rr.toFloat() * cell),
                        size = Size(cols.toFloat() * cell, cell)
                    )
                    // cella stessa più marcata
                    drawRect(
                        color = azure.copy(alpha = 0.22f),
                        topLeft = Offset(cc.toFloat() * cell, rr.toFloat() * cell),
                        size = Size(cell, cell)
                    )
                    drawRect(
                        color = azure,
                        topLeft = Offset(cc.toFloat() * cell, rr.toFloat() * cell),
                        size = Size(cell, cell),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Converte una posizione touch in (riga, colonna) per la griglia.
 * NB: qui non controlliamo i limiti max; il chiamante verifica rr/cc dentro i bounds.
 */
private fun computeCell(ofs: Offset, cols: Int, w: Float, h: Float): Pair<Int, Int> {
    val cell = min(w / cols, h / cols)
    val r = floor(ofs.y / cell).toInt().coerceAtLeast(0)
    val c = floor(ofs.x / cell).toInt().coerceAtLeast(0)
    return r to c
}

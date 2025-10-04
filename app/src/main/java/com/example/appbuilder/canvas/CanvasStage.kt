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
    // Stato locale per hover (tap singolo) e primo ancoraggio (long‑press)
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var firstAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val cols = max(1, gridDensity)
    val azure = Color(0xFF58A6FF)

    // ---------- Helper locali (solo funzioni, nessun enum/material) ----------
    fun rectBounds(r: DrawItem.RectItem): IntArray {
        val r0 = min(r.r0, r.r1); val r1 = max(r.r0, r.r1)
        val c0 = min(r.c0, r.c1); val c1 = max(r.c0, r.c1)
        return intArrayOf(r0, r1, c0, c1)
    }

    fun containsCell(r: DrawItem.RectItem, row: Int, col: Int): Boolean {
        val (r0, r1, c0, c1) = rectBounds(r)
        return row in r0..r1 && col in c0..c1
    }

    // Top-most level alla cella (considera SOLO rettangoli, e solo livello <= currentLevel)
    fun topLevelAtCell(row: Int, col: Int): Int {
        var top: Int? = null
        page?.items?.forEach { it ->
            if (it is DrawItem.RectItem && it.level <= currentLevel) {
                if (containsCell(it, row, col)) {
                    top = if (top == null) it.level else max(top!!, it.level)
                }
            }
        }
        return top ?: 0 // 0 = background
    }

    // Linea orizzontale sovrappone un rettangolo?
    fun overlappedByHorizontal(row: Int, cStart: Int, cEnd: Int, rect: DrawItem.RectItem): Boolean {
        val (r0, r1, c0, c1) = rectBounds(rect)
        if (row !in r0..r1) return false
        return !(cEnd < c0 || cStart > c1)
    }

    // Linea verticale sovrappone un rettangolo?
    fun overlappedByVertical(col: Int, rStart: Int, rEnd: Int, rect: DrawItem.RectItem): Boolean {
        val (r0, r1, c0, c1) = rectBounds(rect)
        if (col !in c0..c1) return false
        return !(rEnd < r0 || rStart > r1)
    }

    // La linea attraversa l'interno (non solo il bordo)?
    fun crossesInsideHoriz(row: Int, rect: DrawItem.RectItem): Boolean {
        val (r0, r1, _, _) = rectBounds(rect)
        return row > r0 && row < r1
    }
    fun crossesInsideVert(col: Int, rect: DrawItem.RectItem): Boolean {
        val (_, _, c0, c1) = rectBounds(rect)
        return col > c0 && col < c1
    }

    // Split rettangolo in 2 parti lungo riga/colonna (se attraversa l'interno)
    fun splitRectHoriz(row: Int, rect: DrawItem.RectItem): List<DrawItem.RectItem> {
        val (r0, r1, c0, c1) = rectBounds(rect)
        if (row <= r0 || row >= r1) return listOf(rect)
        val top = rect.copy(r0 = r0, r1 = row,  c0 = c0, c1 = c1)
        val bottom = rect.copy(r0 = row + 1, r1 = r1, c0 = c0, c1 = c1)
        return listOf(top, bottom)
    }
    fun splitRectVert(col: Int, rect: DrawItem.RectItem): List<DrawItem.RectItem> {
        val (r0, r1, c0, c1) = rectBounds(rect)
        if (col <= c0 || col >= c1) return listOf(rect)
        val left  = rect.copy(r0 = r0, r1 = r1, c0 = c0,     c1 = col)
        val right = rect.copy(r0 = r0, r1 = r1, c0 = col + 1, c1 = c1)
        return listOf(left, right)
    }
    // ------------------------------------------------------------------------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cols, creationEnabled) {
                detectTapGestures(
                    onTap = { ofs ->
                        if (!creationEnabled) return@detectTapGestures
                        val cell = computeCell(ofs, cols, this.size.width.toFloat(), this.size.height.toFloat())
                        hoverCell = cell
                    },
                    onLongPress = { ofs ->
                        if (!creationEnabled) return@detectTapGestures
                        val cell = computeCell(ofs, cols, this.size.width.toFloat(), this.size.height.toFloat())
                        if (firstAnchor == null) {
                            firstAnchor = cell
                        } else {
                            val (r0, c0) = firstAnchor!!
                            val (r1, c1) = cell
                            firstAnchor = null

                            // Caso cella identica: no-op
                            if (r0 == r1 && c0 == c1) return@detectTapGestures

                            // Allineamento
                            val sameRow = (r0 == r1)
                            val sameCol = (c0 == c1)

                            val rr0 = min(r0, r1); val rr1 = max(r0, r1)
                            val cc0 = min(c0, c1); val cc1 = max(c0, c1)

                            if (sameRow || sameCol) {
                                // 1) GATE per livello: entrambi i punti DEVONO avere lo stesso top-level
                                val t0 = topLevelAtCell(r0, c0)
                                val t1 = topLevelAtCell(r1, c1)
                                if (t0 != t1) {
                                    // Livelli diversi: non fare nulla (come da richiesta)
                                    return@detectTapGestures
                                }
                                val targetLevel = t0 // == t1

                                // Set rettangoli del livello target
                                val rects = page?.items?.filterIsInstance<DrawItem.RectItem>().orEmpty()
                                    .filter { it.level == targetLevel }

                                if (sameRow) {
                                    // orizzontale
                                    val overlapped = rects.filter { overlappedByHorizontal(r0, cc0, cc1, it) }
                                    val splitTargets = overlapped.filter { crossesInsideHoriz(r0, it) }

                                    if (targetLevel == 0) {
                                        // Background: niente split; linea a livello 0 (sotto i contenitori)
                                        val line = DrawItem.LineItem(
                                            level = 0,
                                            r0 = r0, c0 = cc0,
                                            r1 = r0, c1 = cc1,
                                            color = azure,
                                            width = 2.dp
                                        )
                                        onAddItem(line)
                                    } else {
                                        // Contenitori di un dato livello: split dove attraversa l'interno
                                        splitTargets.forEach { rect ->
                                            page?.items?.remove(rect)
                                            page?.items?.addAll(splitRectHoriz(r0, rect))
                                        }
                                        // Aggiungi linea sopra (L+1) per evidenziare
                                        val line = DrawItem.LineItem(
                                            level = targetLevel + 1,
                                            r0 = r0, c0 = cc0,
                                            r1 = r0, c1 = cc1,
                                            color = azure,
                                            width = 2.dp
                                        )
                                        onAddItem(line)
                                    }
                                } else {
                                    // verticale
                                    val overlapped = rects.filter { overlappedByVertical(c0, rr0, rr1, it) }
                                    val splitTargets = overlapped.filter { crossesInsideVert(c0, it) }

                                    if (targetLevel == 0) {
                                        val line = DrawItem.LineItem(
                                            level = 0,
                                            r0 = rr0, c0 = c0,
                                            r1 = rr1, c1 = c0,
                                            color = azure,
                                            width = 2.dp
                                        )
                                        onAddItem(line)
                                    } else {
                                        splitTargets.forEach { rect ->
                                            page?.items?.remove(rect)
                                            page?.items?.addAll(splitRectVert(c0, rect))
                                        }
                                        val line = DrawItem.LineItem(
                                            level = targetLevel + 1,
                                            r0 = rr0, c0 = c0,
                                            r1 = rr1, c1 = c0,
                                            color = azure,
                                            width = 2.dp
                                        )
                                        onAddItem(line)
                                    }
                                }
                            } else {
                                // Non stessa riga/colonna: crea RETTANGOLO al livello corrente
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
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = min(size.width / cols, size.height / cols)
            val rows = if (cell > 0f) floor(size.height / cell).toInt() else 0

            // Fondo pagina bianco (copre il gradiente di background dell'Editor)
            if (page != null) {
                drawRect(color = Color.White, topLeft = Offset.Zero, size = size)
            }

            // Disegna elementi fino al livello corrente
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
                                // Orizzontale: lati interni
                                val row = item.r0
                                val cMin = min(item.c0, item.c1)
                                val cMax = max(item.c0, item.c1)
                                val y = (row + 0.5f) * cell
                                val xStart = (cMin + 1).toFloat() * cell
                                val xEnd   = (cMax).toFloat() * cell
                                if (xEnd > xStart) {
                                    drawLine(
                                        color = item.color,
                                        start = Offset(xStart, y),
                                        end   = Offset(xEnd,   y),
                                        strokeWidth = item.width.toPx()
                                    )
                                }
                            } else if (item.c0 == item.c1) {
                                // Verticale: lati interni
                                val col = item.c0
                                val rMin = min(item.r0, item.r1)
                                val rMax = max(item.r0, item.r1)
                                val x = (col + 0.5f) * cell
                                val yStart = (rMin + 1).toFloat() * cell
                                val yEnd   = (rMax).toFloat() * cell
                                if (yEnd > yStart) {
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
            }

            // Griglia: preview del 1° quadretto o griglia completa
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

            // Hover cell (tap singolo)
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

            // Evidenziazione riga/colonna dal primo long‑press
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

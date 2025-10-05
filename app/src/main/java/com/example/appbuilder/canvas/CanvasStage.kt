package com.example.appbuilder.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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

@Composable
fun CanvasStage(
    page: PageState?,
    gridDensity: Int,
    gridPreviewOnly: Boolean,
    showFullGrid: Boolean,
    currentLevel: Int,
    creationEnabled: Boolean = true,
    toolMode: ToolMode = ToolMode.Create,         // ⬅️ nuovo (default = Create, così non rompi call-site)
    containerEditingActive: Boolean = true,       // ⬅️ nuovo: true solo quando sei nel menù "Contenitore"
    onAddItem: (DrawItem) -> Unit
) {
    // === Stato locale ===
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }          // solo Create
    var firstAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }        // solo Create

    var selectedRect by remember { mutableStateOf<DrawItem.RectItem?>(null) }    // Point/Resize/Grab
    var resizeFixedCorner by remember { mutableStateOf<Pair<Int, Int>?>(null) }  // Resize: corner ancorato

    val cols = max(1, gridDensity)
    val azure = Color(0xFF58A6FF)
    val lineBlack = Color.Black

    // Uscendo dal menù "Contenitore" puliamo selezione/evidenze
    LaunchedEffect(containerEditingActive) {
        if (!containerEditingActive) {
            selectedRect = null
            resizeFixedCorner = null
            hoverCell = null
            firstAnchor = null
        }
    }

    // ----------------- Helper interni -----------------
    fun rectBounds(r: DrawItem.RectItem): IntArray {
        val r0 = min(r.r0, r.r1); val r1 = max(r.r0, r.r1)
        val c0 = min(r.c0, r.c1); val c1 = max(r.c0, r.c1)
        return intArrayOf(r0, r1, c0, c1)
    }
    fun containsCell(r: DrawItem.RectItem, row: Int, col: Int): Boolean {
        val (r0, r1, c0, c1) = rectBounds(r)
        return row in r0..r1 && col in c0..c1
    }
    fun topRectAtCell(row: Int, col: Int): DrawItem.RectItem? =
        page?.items?.filterIsInstance<DrawItem.RectItem>()
            ?.filter { containsCell(it, row, col) }
            ?.maxByOrNull { it.level }

    fun splitHoriz(rect: DrawItem.RectItem, b: Int): List<DrawItem.RectItem> {
        val (r0, r1, c0, c1) = rectBounds(rect)
        if (b !in r0 until r1) return listOf(rect)
        val top = rect.copy(r0 = r0, r1 = b,     c0 = c0, c1 = c1)
        val bot = rect.copy(r0 = b + 1, r1 = r1, c0 = c0, c1 = c1)
        return listOf(top, bot)
    }
    fun splitVert(rect: DrawItem.RectItem, b: Int): List<DrawItem.RectItem> {
        val (r0, r1, c0, c1) = rectBounds(rect)
        if (b !in c0 until c1) return listOf(rect)
        val left  = rect.copy(r0 = r0, r1 = r1, c0 = c0,     c1 = b)
        val right = rect.copy(r0 = r0, r1 = r1, c0 = b + 1,  c1 = c1)
        return listOf(left, right)
    }
    fun chooseBoundaryRow(rect: DrawItem.RectItem, rr: Int): Int? {
        val (r0, r1, _, _) = rectBounds(rect)
        val candidates = buildList {
            val b1 = rr - 1; val b2 = rr
            if (b1 in r0 until r1) add(b1)
            if (b2 in r0 until r1) add(b2)
        }
        if (candidates.isEmpty()) return null
        val mid = (r0 + r1) / 2.0
        return candidates.minByOrNull { kotlin.math.abs((it + 0.5) - mid) }
    }
    fun chooseBoundaryCol(rect: DrawItem.RectItem, cc: Int): Int? {
        val (_, _, c0, c1) = rectBounds(rect)
        val candidates = buildList {
            val b1 = cc - 1; val b2 = cc
            if (b1 in c0 until c1) add(b1)
            if (b2 in c0 until c1) add(b2)
        }
        if (candidates.isEmpty()) return null
        val mid = (c0 + c1) / 2.0
        return candidates.minByOrNull { kotlin.math.abs((it + 0.5) - mid) }
    }
    // corner del rect più vicino alla cella (per fissare l'ancora in Resize)
    fun nearestCorner(rect: DrawItem.RectItem, rr: Int, cc: Int): Pair<Int, Int> {
        val (r0, r1, c0, c1) = rectBounds(rect)
        val rf = if (kotlin.math.abs(rr - r0) <= kotlin.math.abs(rr - r1)) r0 else r1
        val cf = if (kotlin.math.abs(cc - c0) <= kotlin.math.abs(cc - c1)) c0 else c1
        return rf to cf
    }
    // --------------------------------------------------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cols, creationEnabled, toolMode, containerEditingActive) {
                detectTapGestures(
                    onTap = { ofs ->
                        val (rr, cc) = computeCell(ofs, cols, this.size.width.toFloat(), this.size.height.toFloat())
                        when (toolMode) {
                            ToolMode.Create -> {
                                if (!creationEnabled) return@detectTapGestures
                                hoverCell = rr to cc
                            }
                            ToolMode.Point -> {
                                if (!containerEditingActive) return@detectTapGestures
                                selectedRect = topRectAtCell(rr, cc)
                            }
                            ToolMode.Resize -> {
                                if (!containerEditingActive) return@detectTapGestures
                                // Se ho già fissato l'ancora, il tap sceglie il nuovo corner opposto e ridimensiona
                                if (resizeFixedCorner != null && selectedRect != null) {
                                    val (fr, fc) = resizeFixedCorner!!
                                    val nr0 = min(fr, rr); val nr1 = max(fr, rr)
                                    val nc0 = min(fc, cc); val nc1 = max(fc, cc)
                                    val old = selectedRect!!
                                    val newRect = old.copy(r0 = nr0, r1 = nr1, c0 = nc0, c1 = nc1)
                                    page?.items?.remove(old)
                                    page?.items?.add(newRect)
                                    selectedRect = newRect
                                    // L'ancora resta: puoi fare più resize consecutivi (“tap‑tap”)
                                }
                            }
                            ToolMode.Grab -> {
                                if (!containerEditingActive) return@detectTapGestures
                                // Selezione (drag a scatti lo aggiungeremo separatamente)
                                selectedRect = topRectAtCell(rr, cc)
                            }
                        }
                    },
                    onLongPress = { ofs ->
                        val (rr, cc) = computeCell(ofs, cols, this.size.width.toFloat(), this.size.height.toFloat())
                        when (toolMode) {
                            ToolMode.Create -> {
                                if (!creationEnabled) return@detectTapGestures
                                if (firstAnchor == null) {
                                    firstAnchor = rr to cc
                                } else {
                                    val (r0, c0) = firstAnchor!!
                                    val r1 = rr; val c1 = cc
                                    firstAnchor = null
                                    if (r0 == r1 && c0 == c1) return@detectTapGestures

                                    val sameRow = (r0 == r1)
                                    val sameCol = (c0 == c1)

                                    // Split di UN contenitore "da lato a lato"
                                    if (sameRow || sameCol) {
                                        val rectA = topRectAtCell(r0, c0)
                                        val rectB = topRectAtCell(r1, c1)
                                        val rect = if (rectA != null && rectA == rectB) rectA else null
                                        if (rect != null) {
                                            val (R0, R1, C0, C1) = rectBounds(rect)
                                            if (sameRow) {
                                                val ccMin = min(c0, c1); val ccMax = max(c0, c1)
                                                val fullSpan = (ccMin == C0 && ccMax == C1 && r0 in R0..R1)
                                                if (fullSpan) {
                                                    val b = chooseBoundaryRow(rect, rr) ?: return@detectTapGestures
                                                    page?.items?.remove(rect)
                                                    splitHoriz(rect, b).forEach { onAddItem(it) }
                                                    onAddItem(
                                                        DrawItem.LineItem(
                                                            level = rect.level + 1,
                                                            r0 = b, c0 = C0 - 1, r1 = b, c1 = C1 + 1,
                                                            color = lineBlack, width = 2.dp
                                                        )
                                                    )
                                                    return@detectTapGestures
                                                }
                                            } else { // sameCol
                                                val rrMin = min(r0, r1); val rrMax = max(r0, r1)
                                                val fullSpan = (rrMin == R0 && rrMax == R1 && c0 in C0..C1)
                                                if (fullSpan) {
                                                    val b = chooseBoundaryCol(rect, cc) ?: return@detectTapGestures
                                                    page?.items?.remove(rect)
                                                    splitVert(rect, b).forEach { onAddItem(it) }
                                                    onAddItem(
                                                        DrawItem.LineItem(
                                                            level = rect.level + 1,
                                                            r0 = R0 - 1, c0 = b, r1 = R1 + 1, c1 = b,
                                                            color = lineBlack, width = 2.dp
                                                        )
                                                    )
                                                    return@detectTapGestures
                                                }
                                            }
                                            return@detectTapGestures // no-op se non “da lato a lato”
                                        }
                                    }

                                    // Non allineati → rettangolo standard
                                    val rr0 = min(r0, r1); val rr1 = max(r0, r1)
                                    val cc0 = min(c0, c1); val cc1 = max(c0, c1)
                                    onAddItem(
                                        DrawItem.RectItem(
                                            level = currentLevel,
                                            r0 = rr0, c0 = cc0, r1 = rr1, c1 = cc1,
                                            borderColor = Color.Black,
                                            borderWidth = 1.dp,
                                            fillColor = Color.White
                                        )
                                    )
                                }
                            }
                            ToolMode.Point -> {
                                if (!containerEditingActive) return@detectTapGestures
                                selectedRect = topRectAtCell(rr, cc)
                            }
                            ToolMode.Resize -> {
                                if (!containerEditingActive) return@detectTapGestures
                                val rect = topRectAtCell(rr, cc) ?: return@detectTapGestures
                                selectedRect = rect
                                // Fissa il corner più vicino al long‑press (ancora): resterà fermo
                                resizeFixedCorner = nearestCorner(rect, rr, cc)
                            }
                            ToolMode.Grab -> {
                                if (!containerEditingActive) return@detectTapGestures
                                selectedRect = topRectAtCell(rr, cc)
                                // Il drag a scatti lo tratteremo con detectDragGestures in un pass successivo
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = min(size.width / cols, size.height / cols)
            val rows = if (cell > 0f) floor(size.height / cell).toInt() else 0

            // Fondo pagina bianco quando c'è una Page
            if (page != null) {
                drawRect(color = Color.White, topLeft = Offset.Zero, size = size)
            }

            // Disegno elementi (rettangoli + linee) fino al livello corrente
            page?.items?.forEach { item ->
                if (item.level <= currentLevel) {
                    when (item) {
                        is DrawItem.RectItem -> {
                            val left = min(item.c0, item.c1).toFloat() * cell
                            val top  = min(item.r0, item.r1).toFloat() * cell
                            val w = (abs(item.c1 - item.c0) + 1).toFloat() * cell
                            val h = (abs(item.r1 - item.r0) + 1).toFloat() * cell
                            drawRect(color = item.fillColor, topLeft = Offset(left, top), size = Size(w, h))
                            drawRect(color = item.borderColor, topLeft = Offset(left, top), size = Size(w, h),
                                     style = Stroke(width = item.borderWidth.toPx()))
                        }
                        is DrawItem.LineItem -> {
                            if (item.r0 == item.r1) {
                                // Orizzontale (estendo ai bordi interni del contenitore)
                                val row = item.r0
                                val cMin = min(item.c0, item.c1)
                                val cMax = max(item.c0, item.c1)
                                val y = (row + 0.5f) * cell
                                val xStart = (cMin + 1).toFloat() * cell
                                val xEnd   = (cMax).toFloat() * cell
                                if (xEnd > xStart) {
                                    drawLine(item.color, start = Offset(xStart, y), end = Offset(xEnd, y),
                                             strokeWidth = item.width.toPx())
                                }
                            } else if (item.c0 == item.c1) {
                                // Verticale
                                val col = item.c0
                                val rMin = min(item.r0, item.r1)
                                val rMax = max(item.r0, item.r1)
                                val x = (col + 0.5f) * cell
                                val yStart = (rMin + 1).toFloat() * cell
                                val yEnd   = (rMax).toFloat() * cell
                                if (yEnd > yStart) {
                                    drawLine(item.color, start = Offset(x, yStart), end = Offset(x, yEnd),
                                             strokeWidth = item.width.toPx())
                                }
                            }
                        }
                    }
                }
            }

            // Griglia: preview o completa (come prima)
            if (gridPreviewOnly) {
                if (rows > 0 && cols > 0) {
                    drawRect(azure.copy(alpha = 0.20f), topLeft = Offset(0f, 0f), size = Size(cell, cell))
                    drawRect(azure, topLeft = Offset(0f, 0f), size = Size(cell, cell),
                             style = Stroke(width = 1.5.dp.toPx()))
                }
            } else if (showFullGrid) {
                for (c in 0..cols) {
                    val x = c.toFloat() * cell
                    drawLine(azure.copy(alpha = 0.30f), start = Offset(x, 0f), end = Offset(x, rows.toFloat() * cell),
                             strokeWidth = 1.dp.toPx())
                }
                for (r in 0..rows) {
                    val y = r.toFloat() * cell
                    drawLine(azure.copy(alpha = 0.30f), start = Offset(0f, y), end = Offset(cols.toFloat() * cell, y),
                             strokeWidth = 1.dp.toPx())
                }
            }

            // Hover (solo Create)
            if (toolMode == ToolMode.Create) {
                hoverCell?.let { (rr, cc) ->
                    if (rr in 0 until rows && cc in 0 until cols) {
                        drawRect(azure.copy(alpha = 0.18f), topLeft = Offset(cc.toFloat() * cell, rr.toFloat() * cell),
                                 size = Size(cell, cell))
                        drawRect(azure, topLeft = Offset(cc.toFloat() * cell, rr.toFloat() * cell),
                                 size = Size(cell, cell), style = Stroke(width = 1.dp.toPx()))
                    }
                }
            }

            // Evidenza container selezionato (solo nel menù "Contenitore")
            if (containerEditingActive) {
                selectedRect?.let { sel ->
                    val left = min(sel.c0, sel.c1).toFloat() * cell
                    val top  = min(sel.r0, sel.r1).toFloat() * cell
                    val w = (abs(sel.c1 - sel.c0) + 1).toFloat() * cell
                    val h = (abs(sel.r1 - sel.r0) + 1).toFloat() * cell
                    drawRect(color = Color.Transparent, topLeft = Offset(left, top), size = Size(w, h),
                             style = Stroke(width = 2.dp.toPx(), miter = 1f),)
                }
            }

            // Riga/colonna del primo anchor (Create) — invariato
            if (toolMode == ToolMode.Create) {
                firstAnchor?.let { (rr, cc) ->
                    drawRect(azure.copy(alpha = 0.10f), topLeft = Offset(cc.toFloat() * cell, 0f),
                             size = Size(cell, rows.toFloat() * cell))
                    drawRect(azure.copy(alpha = 0.10f), topLeft = Offset(0f, rr.toFloat() * cell),
                             size = Size(cols.toFloat() * cell, cell))
                    drawRect(azure.copy(alpha = 0.22f), topLeft = Offset(cc.toFloat() * cell, rr.toFloat() * cell),
                             size = Size(cell, cell))
                    drawRect(azure, topLeft = Offset(cc.toFloat() * cell, rr.toFloat() * cell),
                             size = Size(cell, cell), style = Stroke(width = 1.5.dp.toPx()))
                }
            }
        }
    }
}


// Stato locale d’appoggio per Resize (angoli)
private var resizeFixedCorner: Pair<Int, Int>? by mutableStateOf(null)
private var resizeMovingCornerStart: Pair<Int, Int>? by mutableStateOf(null)

// Invariata
private fun computeCell(ofs: Offset, cols: Int, w: Float, h: Float): Pair<Int, Int> {
    val cell = min(w / cols, h / cols)
    val r = floor(ofs.y / cell).toInt().coerceAtLeast(0)
    val c = floor(ofs.x / cell).toInt().coerceAtLeast(0)
    return r to c
}
package com.example.appbuilder.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Stage di disegno: pagina bianca, griglia, creazione/container split, point/grab/resize.
 *
 * NB: non ridefinisco ToolMode qui, usa quello in ToolMode.kt (stesso package).
 */
@Composable
fun CanvasStage(
    page: PageState?,
    gridDensity: Int,
    gridPreviewOnly: Boolean,
    showFullGrid: Boolean,
    currentLevel: Int,
    creationEnabled: Boolean,
    toolMode: ToolMode = ToolMode.Create,
    onAddItem: (DrawItem) -> Unit,
    onItemsMutated: () -> Unit = {},
    // Questi due sono presenti nella tua chiamata da EditorKit: li accetto come no-op per compatibilità
    onUpdateItem: (DrawItem) -> Unit = {},
    onRequestEdit: (DrawItem.RectItem) -> Unit = {}
) {
    // Stato locale interazione
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) } // evidenzia cella (Create)
    var firstAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) } // Create: primo punto
    var activeRectIndex by remember { mutableStateOf<Int?>(null) }        // Point/Grab/Resize selezione
    var resizeFixedCorner by remember { mutableStateOf<Pair<Int, Int>?>(null) } // Resize: angolo “fermo”

    val density = max(1, gridDensity)

    Box(
        modifier = Modifier
            .fillMaxSize()
            // TAP / LONG PRESS
            .pointerInput(density, toolMode, creationEnabled, page?.id, currentLevel) {
                detectTapGestures(
                    onTap = { ofs ->
                        val cell = computeCell(ofs, density, this.size.width.toFloat(), this.size.height.toFloat())
                        when (toolMode) {
                            ToolMode.Create -> if (creationEnabled) hoverCell = cell

                            ToolMode.Point -> {
                                // seleziona contenitore con tap
                                val hit = findRectAtCell(page, cell, currentLevel)
                                activeRectIndex = hit?.first
                                hit?.second?.let { onRequestEdit(it) } // facoltativo: segnala selezione
                            }

                            ToolMode.Resize -> {
                                // se ho già un angolo fisso, questo tap conferma il ridimensionamento
                                val idx = activeRectIndex
                                val fixed = resizeFixedCorner
                                if (idx != null && fixed != null) {
                                    val r = page?.items?.getOrNull(idx) as? DrawItem.RectItem
                                    if (r != null) {
                                        val (fr, fc) = fixed
                                        val (r2, c2) = cell
                                        val nr0 = min(fr, r2)
                                        val nr1 = max(fr, r2)
                                        val nc0 = min(fc, c2)
                                        val nc1 = max(fc, c2)
                                        replaceRectAt(page, idx, r.copy(r0 = nr0, c0 = nc0, r1 = nr1, c1 = nc1))
                                        onItemsMutated()
                                        onUpdateItem(page!!.items[idx]) // compat
                                        resizeFixedCorner = null
                                    }
                                }
                            }

                            ToolMode.Grab -> Unit
                        }
                    },
                    onLongPress = { ofs ->
                        val cell = computeCell(ofs, density, this.size.width.toFloat(), this.size.height.toFloat())
                        when (toolMode) {
                            ToolMode.Create -> if (creationEnabled) {
                                if (firstAnchor == null) {
                                    firstAnchor = cell
                                } else {
                                    val a = firstAnchor!!; firstAnchor = null
                                    // Provo split: due celle allineate nello STESSO rettangolo e da lato a lato
                                    val didSplit = trySplitContainerByLine(page, a, cell, currentLevel)
                                    if (!didSplit) {
                                        // altrimenti crea rettangolo standard
                                        val (r0, c0) = a
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
                                    }
                                }
                            }

                            ToolMode.Point -> {
                                // long-press: cicla spessore bordo (1→2→4→1) del contenitore colpito
                                val hit = findRectAtCell(page, cell, currentLevel)
                                val idx = hit?.first
                                val r = hit?.second
                                if (idx != null && r != null) {
                                    activeRectIndex = idx
                                    val nextBw = when (r.borderWidth.value.roundToInt()) {
                                        1 -> 2.dp
                                        2 -> 4.dp
                                        else -> 1.dp
                                    }
                                    replaceRectAt(page, idx, r.copy(borderWidth = nextBw))
                                    onItemsMutated()
                                    onUpdateItem(page!!.items[idx]) // compat
                                }
                            }

                            ToolMode.Grab -> {
                                // seleziona il contenitore da trascinare
                                activeRectIndex = findRectAtCell(page, cell, currentLevel)?.first
                            }

                            ToolMode.Resize -> {
                                // primo long-press: fissa l’angolo “fermo” del contenitore colpito
                                val hit = findRectAtCell(page, cell, currentLevel)
                                val idx = hit?.first
                                val r = hit?.second
                                if (idx != null && r != null) {
                                    activeRectIndex = idx
                                    resizeFixedCorner = nearestCornerCell(r, cell)
                                }
                            }
                        }
                    }
                )
            }
            // DRAG (Grab)
            .pointerInput(density, toolMode, activeRectIndex, currentLevel, page?.id) {
                if (toolMode == ToolMode.Grab && activeRectIndex != null) {
                    detectDragGesturesAfterLongPress(
                        onDrag = { change, drag ->
                            change.consume()
                            val idx = activeRectIndex ?: return@detectDragGesturesAfterLongPress
                            val r = page?.items?.getOrNull(idx) as? DrawItem.RectItem ?: return@detectDragGesturesAfterLongPress

                            // quantizzazione a celle
                            val cellW = this.size.width.toFloat() / density
                            val cellH = this.size.height.toFloat() / density
                            val dc = (drag.x / cellW).roundToInt()
                            val dr = (drag.y / cellH).roundToInt()
                            if (dc == 0 && dr == 0) return@detectDragGesturesAfterLongPress

                            val moved = r.offsetBy(dr, dc)
                            // collisioni: non sovrapporre a rettangoli stesso livello
                            if (!collides(page, moved, excludeIndex = idx)) {
                                replaceRectAt(page, idx, moved)
                                onItemsMutated()
                                onUpdateItem(page!!.items[idx]) // compat
                            }
                        }
                    )
                }
            }
    ) {
        // --- Disegno ---
        Canvas(Modifier.fillMaxSize()) {
            // pagina bianca (sfondo)
            drawRect(color = Color.White, topLeft = Offset.Zero, size = size)

            val cell = min(size.width / density, size.height / density)
            val rows = if (cell > 0f) floor(size.height / cell).toInt() else 0
            val cols = if (cell > 0f) floor(size.width / cell).toInt() else 0

            // elementi esistenti
            page?.items?.forEach { item ->
                if (item.level <= currentLevel) when (item) {
                    is DrawItem.RectItem -> {
                        val left = item.c0 * cell
                        val top  = item.r0 * cell
                        val w = (item.c1 - item.c0 + 1) * cell
                        val h = (item.r1 - item.r0 + 1) * cell
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
                        // (non usato in questa fase)
                    }
                }
            }

            // evidenzia cella (Create)
            hoverCell?.let { (rr, cc) ->
                if (toolMode == ToolMode.Create && creationEnabled) {
                    if (rr in 0 until rows && cc in 0 until cols) {
                        val x = cc * cell
                        val y = rr * cell
                        drawRect(
                            color = Color(0xFF58A6FF).copy(alpha = 0.20f),
                            topLeft = Offset(x, y),
                            size = Size(cell, cell)
                        )
                        drawRect(
                            color = Color(0xFF58A6FF),
                            topLeft = Offset(x, y),
                            size = Size(cell, cell),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }

            // griglia (anteprima 1 cella o completa)
            if (gridPreviewOnly) {
                drawRect(
                    color = Color(0xFF58A6FF).copy(alpha = 0.20f),
                    topLeft = Offset.Zero,
                    size = Size(cell, cell)
                )
                drawRect(
                    color = Color(0xFF58A6FF),
                    topLeft = Offset.Zero,
                    size = Size(cell, cell),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            } else if (showFullGrid) {
                val azure = Color(0xFF58A6FF).copy(alpha = 0.30f)
                for (c in 0..cols) {
                    val x = c * cell
                    drawLine(azure, Offset(x, 0f), Offset(x, rows.toFloat() * cell), strokeWidth = 1.dp.toPx())
                }
                for (r in 0..rows) {
                    val y = r * cell
                    drawLine(azure, Offset(0f, y), Offset(cols.toFloat() * cell, y), strokeWidth = 1.dp.toPx())
                }
            }
        }
    }
}

/* ============================
 * Helpers & utilities
 * ============================ */

private fun computeCell(ofs: Offset, cols: Int, w: Float, h: Float): Pair<Int, Int> {
    val cell = min(w / cols, h / cols)
    val r = floor(ofs.y / cell).toInt().coerceAtLeast(0)
    val c = floor(ofs.x / cell).toInt().coerceAtLeast(0)
    return r to c
}

private fun containsCell(r: DrawItem.RectItem, cell: Pair<Int, Int>): Boolean {
    val (rr, cc) = cell
    return rr in r.r0..r.r1 && cc in r.c0..r.c1
}

private fun findRectAtCell(
    page: PageState?,
    cell: Pair<Int, Int>,
    currentLevel: Int
): Pair<Int, DrawItem.RectItem>? {
    val items = page?.items ?: return null
    for (i in items.indices) {
        val r = items[i]
        if (r is DrawItem.RectItem && r.level <= currentLevel && containsCell(r, cell)) {
            return i to r
        }
    }
    return null
}

private fun nearestCornerCell(r: DrawItem.RectItem, cell: Pair<Int, Int>): Pair<Int, Int> {
    val (rr, cc) = cell
    val corners = listOf(r.r0 to r.c0, r.r0 to r.c1, r.r1 to r.c0, r.r1 to r.c1)
    return corners.minBy { (cr, cc0) -> abs(cr - rr) + abs(cc0 - cc) }
}

private fun DrawItem.RectItem.offsetBy(dr: Int, dc: Int): DrawItem.RectItem =
    copy(r0 = r0 + dr, r1 = r1 + dr, c0 = c0 + dc, c1 = c1 + dc)

private fun intersects(a: DrawItem.RectItem, b: DrawItem.RectItem): Boolean =
    !(a.c1 < b.c0 || b.c1 < a.c0 || a.r1 < b.r0 || b.r1 < a.r0)

private fun collides(page: PageState?, candidate: DrawItem.RectItem, excludeIndex: Int?): Boolean {
    val items = page?.items ?: return false
    for (i in items.indices) {
        if (i == excludeIndex) continue
        val r = items[i]
        if (r is DrawItem.RectItem && r.level == candidate.level && intersects(r, candidate)) {
            return true
        }
    }
    return false
}

private fun replaceRectAt(page: PageState?, index: Int, newRect: DrawItem.RectItem) {
    val items = page?.items ?: return
    if (index in items.indices) items[index] = newRect
}

/**
 * Split di un contenitore se i punti sono allineati e appartengono allo STESSO rettangolo
 * e la linea va **da lato a lato** del rettangolo.
 * Ritorna true se lo split è avvenuto, false altrimenti.
 */
private fun trySplitContainerByLine(
    page: PageState?,
    a: Pair<Int, Int>,
    b: Pair<Int, Int>,
    currentLevel: Int
): Boolean {
    val items = page?.items ?: return false
    // host = unico rettangolo che contiene entrambe le celle
    val hostIndex = items.indexOfFirst { it is DrawItem.RectItem && containsCell(it, a) && containsCell(it, b) }
    if (hostIndex < 0) return false
    val host = items[hostIndex] as DrawItem.RectItem

    val r0 = host.r0; val c0 = host.c0; val r1 = host.r1; val c1 = host.c1

    // stessa riga
    if (a.first == b.first) {
        val rr = a.first
        // deve essere una linea orizzontale da c0 a c1 (in qualunque verso)
        val ok = (rr in r0..r1) && (
            (a.second == c0 && b.second == c1) || (a.second == c1 && b.second == c0)
        )
        if (!ok) return false

        val upper = host.copy(r1 = rr)       // porzione sopra/compresa la riga
        val lower = host.copy(r0 = rr + 1)   // porzione sotto
        // sostituisco host con 2 parti
        items.removeAt(hostIndex)
        items.add(hostIndex, lower)
        items.add(hostIndex, upper)
        return true
    }

    // stessa colonna
    if (a.second == b.second) {
        val cc = a.second
        // deve essere una linea verticale da r0 a r1 (in qualunque verso)
        val ok = (cc in c0..c1) && (
            (a.first == r0 && b.first == r1) || (a.first == r1 && b.first == r0)
        )
        if (!ok) return false

        val left  = host.copy(c1 = cc)       // porzione sinistra/compresa la colonna
        val right = host.copy(c0 = cc + 1)   // porzione destra
        items.removeAt(hostIndex)
        items.add(hostIndex, right)
        items.add(hostIndex, left)
        return true
    }

    return false
}
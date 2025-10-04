package com.example.appbuilder.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.*
import androidx.compose.ui.Alignment


/** Modalità strumenti (già usata in InfoEdgeDeck) */
enum class ToolMode { Create, Point, Grab, Resize }

/**
 * Stage di disegno: pagina, griglia, creazione/split rettangoli, editing.
 *
 * @param creationEnabled true solo quando sei nel menù Contenitore (il resto già lo gestisci in EditorKit)
 * @param toolMode        Create/Point/Grab/Resize
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
    onItemsMutated: () -> Unit = {} // chiamalo quando modifichi page.items direttamente
) {
    // Stato locale per interazioni
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var firstAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }     // Create: primo punto
    var activeRectId by remember { mutableStateOf<String?>(null) }            // Point/Grab/Resize
    var resizeFixedCorner by remember { mutableStateOf<Pair<Int, Int>?>(null) } // Resize: angolo fissato

    val density = max(1, gridDensity)

    Box(
        modifier = Modifier
            .fillMaxSize()
            // TAP / LONG-PRESS (Create/Point/Resize)
            .pointerInput(density, toolMode, creationEnabled, page?.id, currentLevel) {
                detectTapGestures(
                    onTap = { ofs ->
                        val cell = computeCell(ofs, density, this.size.width, this.size.height)
                        when (toolMode) {
                            ToolMode.Create -> if (creationEnabled) hoverCell = cell
                            ToolMode.Point  -> {
                                // seleziona un contenitore col tap
                                activeRectId = page?.items
                                    ?.asSequence()
                                    ?.filterIsInstance<DrawItem.RectItem>()
                                    ?.filter { it.level <= currentLevel }
                                    ?.firstOrNull { containsCell(it, cell) }
                                    ?.id
                            }
                            ToolMode.Resize -> {
                                // secondo tocco: conferma ridimensionamento se hai già fissato un angolo
                                val rect = page?.items?.firstOrNull { it.id == activeRectId } as? DrawItem.RectItem
                                val fixed = resizeFixedCorner
                                if (rect != null && fixed != null) {
                                    val (fr, fc) = fixed
                                    val (r, c) = cell
                                    val nr0 = min(fr, r); val nr1 = max(fr, r)
                                    val nc0 = min(fc, c); val nc1 = max(fc, c)
                                    replaceRect(page, rect.copy(r0 = nr0, c0 = nc0, r1 = nr1, c1 = nc1))
                                    onItemsMutated()
                                    resizeFixedCorner = null
                                }
                            }
                            ToolMode.Grab   -> Unit
                        }
                    },
                    onLongPress = { ofs ->
                        val cell = computeCell(ofs, density, this.size.width, this.size.height)
                        when (toolMode) {
                            ToolMode.Create -> if (creationEnabled) {
                                if (firstAnchor == null) {
                                    firstAnchor = cell
                                } else {
                                    val a = firstAnchor!!; firstAnchor = null
                                    // 1) se allineati su stessa riga/colonna e nello stesso contenitore “da lato a lato” => split
                                    val didSplit = trySplitContainerByLine(page, a, cell, currentLevel)
                                    if (!didSplit) {
                                        // 2) altrimenti crea un rettangolo “normale”
                                        val (r0, c0) = a; val (r1, c1) = cell
                                        val rect = DrawItem.RectItem(
                                            id = newId("rect"),
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
                                // long-press su un contenitore: cicla lo spessore (1dp → 2dp → 4dp → 1dp)
                                val rect = page?.items
                                    ?.asSequence()
                                    ?.filterIsInstance<DrawItem.RectItem>()
                                    ?.filter { it.level <= currentLevel }
                                    ?.firstOrNull { containsCell(it, cell) }
                                if (rect != null) {
                                    activeRectId = rect.id
                                    val next = when (rect.borderWidth.value.roundToInt()) {
                                        1 -> 2.dp
                                        2 -> 4.dp
                                        else -> 1.dp
                                    }
                                    replaceRect(page, rect.copy(borderWidth = next))
                                    onItemsMutated()
                                }
                            }
                            ToolMode.Grab -> {
                                // seleziona il rettangolo da trascinare (drag gestito sotto)
                                val rect = page?.items
                                    ?.asSequence()
                                    ?.filterIsInstance<DrawItem.RectItem>()
                                    ?.filter { it.level <= currentLevel }
                                    ?.firstOrNull { containsCell(it, cell) }
                                activeRectId = rect?.id
                            }
                            ToolMode.Resize -> {
                                // primo long-press: fissi l’angolo “fermo” (quello più vicino al tocco)
                                val rect = page?.items
                                    ?.asSequence()
                                    ?.filterIsInstance<DrawItem.RectItem>()
                                    ?.filter { it.level <= currentLevel }
                                    ?.firstOrNull { containsCell(it, cell) }
                                if (rect != null) {
                                    activeRectId = rect.id
                                    resizeFixedCorner = nearestCornerCell(rect, cell)
                                }
                            }
                        }
                    }
                )
            }
            // DRAG (Grab)
            .pointerInput(density, toolMode, activeRectId, currentLevel, page?.id) {
                if (toolMode == ToolMode.Grab && activeRectId != null) {
                    detectDragGesturesAfterLongPress(
                        onDrag = { change, drag ->
                            change.consume()
                            val rect = page?.items?.firstOrNull { it.id == activeRectId } as? DrawItem.RectItem ?: return@detectDragGesturesAfterLongPress
                            // conversione pixel → celle
                            val w = this.size.width / density
                            val h = this.size.height / density
                            val dxCells = (drag.x / w).roundToInt()
                            val dyCells = (drag.y / h).roundToInt()
                            if (dxCells == 0 && dyCells == 0) return@detectDragGesturesAfterLongPress
                            val moved = rect.offsetBy(dyCells, dxCells)
                            // blocca contro altri rettangoli (stesso livello)
                            if (!collides(page, moved, excludeId = rect.id)) {
                                replaceRect(page, moved)
                                onItemsMutated()
                            }
                        }
                    )
                }
            }
    ) {
        // --- Disegno ---
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            // sfondo bianco (pagina)
            drawRect(color = Color.White, topLeft = Offset.Zero, size = size)

            val cell = min(size.width / density, size.height / density)
            val rows = floor(size.height / cell).toInt()
            val cols = floor(size.width / cell).toInt()

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
                        // opzionale (non usato in questa fase)
                    }
                }
            }

            // evidenzia cella (Create)
            hoverCell?.let { (rr, cc) ->
                if (toolMode == ToolMode.Create && creationEnabled) {
                    if (rr in 0 until rows && cc in 0 until cols) {
                        val x = cc * cell; val y = rr * cell
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

            // griglia: preview 1 cella o griglia completa (se attivi nel pannello)
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
                    drawLine(azure, Offset(x, 0f), Offset(x, rows * cell), strokeWidth = 1.dp.toPx())
                }
                for (r in 0..rows) {
                    val y = r * cell
                    drawLine(azure, Offset(0f, y), Offset(cols * cell, y), strokeWidth = 1.dp.toPx())
                }
            }
        }
    }
}

/* ============================
 * Helper “modellistici” & utils
 * ============================ */

// Selezione cella da coordinate
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

// Trova l’angolo del rettangolo più vicino alla cella toccata
private fun nearestCornerCell(r: DrawItem.RectItem, cell: Pair<Int, Int>): Pair<Int, Int> {
    val (rr, cc) = cell
    val corners = listOf(r.r0 to r.c0, r.r0 to r.c1, r.r1 to r.c0, r.r1 to r.c1)
    return corners.minBy { (cr, cc0) -> abs(cr - rr) + abs(cc0 - cc) }
}

// Offset di un rettangolo di (dr, dc) celle
private fun DrawItem.RectItem.offsetBy(dr: Int, dc: Int): DrawItem.RectItem =
    copy(r0 = r0 + dr, r1 = r1 + dr, c0 = c0 + dc, c1 = c1 + dc)

// Collisione con altri rettangoli allo stesso livello
private fun collides(page: PageState?, candidate: DrawItem.RectItem, excludeId: String?): Boolean {
    val list = page?.items?.filterIsInstance<DrawItem.RectItem>().orEmpty()
    return list.any { it.id != excludeId && it.level == candidate.level && intersects(it, candidate) }
}

private fun intersects(a: DrawItem.RectItem, b: DrawItem.RectItem): Boolean =
    !(a.c1 < b.c0 || b.c1 < a.c0 || a.r1 < b.r0 || b.r1 < a.r0)

// Sostituisce un rettangolo (immutabile) nella lista
private fun replaceRect(page: PageState?, newRect: DrawItem.RectItem) {
    val list = page?.items ?: return
    val idx = list.indexOfFirst { it is DrawItem.RectItem && it.id == newRect.id }
    if (idx >= 0) list[idx] = newRect
}

// Split di un contenitore se: celle allineate **e** nello stesso rettangolo **da lato a lato**
private fun trySplitContainerByLine(
    page: PageState?,
    a: Pair<Int, Int>,
    b: Pair<Int, Int>,
    currentLevel: Int
): Boolean {
    val list = page?.items?.filterIsInstance<DrawItem.RectItem>().orEmpty()
    val host = list.firstOrNull { containsCell(it, a) && containsCell(it, b) } ?: return false

    val (r0, c0, r1, c1) = arrayOf(host.r0, host.c0, host.r1, host.c1)

    return if (a.first == b.first) {
        // ORIZZONTALE: stessa riga → deve andare da c0 a c1
        val rr = a.first
        if (rr in r0..r1 && a.second == c0 && b.second == c1 || a.second == c1 && b.second == c0) {
            // split in alto/basso
            val upper = host.copy(id = newId("rect"), r1 = rr)
            val lower = host.copy(id = newId("rect"), r0 = rr + 1)
            commitSplit(page, host, listOf(upper, lower))
            true
        } else false
    } else if (a.second == b.second) {
        // VERTICALE: stessa colonna → deve andare da r0 a r1
        val cc = a.second
        if (cc in c0..c1 && a.first == r0 && b.first == r1 || a.first == r1 && b.first == r0) {
            // split in sinistra/destra
            val left  = host.copy(id = newId("rect"), c1 = cc)
            val right = host.copy(id = newId("rect"), c0 = cc + 1)
            commitSplit(page, host, listOf(left, right))
            true
        } else false
    } else false
}

private fun commitSplit(page: PageState?, old: DrawItem.RectItem, parts: List<DrawItem.RectItem>) {
    val list = page?.items ?: return
    val idx = list.indexOfFirst { it is DrawItem.RectItem && it.id == old.id }
    if (idx >= 0) {
        list.removeAt(idx)
        parts.forEach { list.add(it) }
    }
}

private var idSeq = 0
private fun newId(prefix: String) = "${prefix}_${idSeq++}"
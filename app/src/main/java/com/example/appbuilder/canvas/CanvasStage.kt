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
    // Hover (tap singolo) e primo ancoraggio (long press)
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var firstAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val cols = max(1, gridDensity)
    val azure = Color(0xFF58A6FF)
    val lineBlack = Color.Black

    // ----------------- Helper locali (no Material/enum) -----------------
    fun rectBounds(r: DrawItem.RectItem): IntArray {
        val r0 = min(r.r0, r.r1); val r1 = max(r.r0, r.r1)
        val c0 = min(r.c0, r.c1); val c1 = max(r.c0, r.c1)
        return intArrayOf(r0, r1, c0, c1)
    }
    fun containsCell(r: DrawItem.RectItem, row: Int, col: Int): Boolean {
        val (r0, r1, c0, c1) = rectBounds(r)
        return row in r0..r1 && col in c0..c1
    }
    // Ritorna il rettangolo "top-most" (livello più alto) che contiene la cella
    fun topRectAtCell(row: Int, col: Int): DrawItem.RectItem? =
        page?.items?.filterIsInstance<DrawItem.RectItem>()
            ?.filter { containsCell(it, row, col) }
            ?.maxByOrNull { it.level }

    // Divide un rettangolo con linea orizzontale alla "boundary" b (tra b e b+1)
    fun splitHoriz(rect: DrawItem.RectItem, b: Int): List<DrawItem.RectItem> {
        val (r0, r1, c0, c1) = rectBounds(rect)
        if (b !in r0 until r1) return listOf(rect)
        val top = rect.copy(r0 = r0, r1 = b,    c0 = c0, c1 = c1)
        val bot = rect.copy(r0 = b + 1, r1 = r1, c0 = c0, c1 = c1)
        return listOf(top, bot)
    }
    // Divide un rettangolo con linea verticale alla "boundary" b (tra b e b+1)
    fun splitVert(rect: DrawItem.RectItem, b: Int): List<DrawItem.RectItem> {
        val (r0, r1, c0, c1) = rectBounds(rect)
        if (b !in c0 until c1) return listOf(rect)
        val left  = rect.copy(r0 = r0, r1 = r1, c0 = c0,     c1 = b)
        val right = rect.copy(r0 = r0, r1 = r1, c0 = b + 1,  c1 = c1)
        return listOf(left, right)
    }
    // Dato un rect e una riga rr, scegli la boundary interna (rr‑1 oppure rr) più vicina al centro del rect
    fun chooseBoundaryRow(rect: DrawItem.RectItem, rr: Int): Int? {
        val (r0, r1, _, _) = rectBounds(rect)
        val candidates = buildList {
            val b1 = rr - 1
            val b2 = rr
            if (b1 in r0 until r1) add(b1)
            if (b2 in r0 until r1) add(b2)
        }
        if (candidates.isEmpty()) return null
        val mid = (r0 + r1) / 2.0
        return candidates.minByOrNull { kotlin.math.abs((it + 0.5) - mid) }
    }
    // Dato un rect e una colonna cc, scegli la boundary interna (cc‑1 oppure cc) più vicina al centro del rect
    fun chooseBoundaryCol(rect: DrawItem.RectItem, cc: Int): Int? {
        val (_, _, c0, c1) = rectBounds(rect)
        val candidates = buildList {
            val b1 = cc - 1
            val b2 = cc
            if (b1 in c0 until c1) add(b1)
            if (b2 in c0 until c1) add(b2)
        }
        if (candidates.isEmpty()) return null
        val mid = (c0 + c1) / 2.0
        return candidates.minByOrNull { kotlin.math.abs((it + 0.5) - mid) }
    }
    // --------------------------------------------------------------------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cols, creationEnabled) {
                detectTapGestures(
                    onTap = { ofs ->
                        if (!creationEnabled) return@detectTapGestures
                        hoverCell = computeCell(ofs, cols, this.size.width.toFloat(), this.size.height.toFloat())
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

                            if (r0 == r1 && c0 == c1) return@detectTapGestures

                            val sameRow = (r0 == r1)
                            val sameCol = (c0 == c1)

                            // --- SOLO split di un contenitore se: stesso rect e "da lato a lato" ---
                            if (sameRow || sameCol) {
                                val rr = r0.coerceAtLeast(0)
                                val cc = c0.coerceAtLeast(0)

                                // rettangolo (top-most) che contiene ENTRAMBE le celle
                                val rectA = topRectAtCell(r0, c0)
                                val rectB = topRectAtCell(r1, c1)
                                val rect = if (rectA != null && rectA == rectB) rectA else null
                                if (rect != null) {
                                    val (R0, R1, C0, C1) = rectBounds(rect)

                                    if (sameRow) {
                                        // Richiesta: i due quadrati devono estendersi da sinistra a destra del contenitore
                                        val ccMin = min(c0, c1)
                                        val ccMax = max(c0, c1)
                                        val fullSpan = (ccMin == C0 && ccMax == C1 && rr in R0..R1)
                                        if (fullSpan) {
                                            val b = chooseBoundaryRow(rect, rr) ?: return@detectTapGestures
                                            // Sostituisci il rettangolo con i 2 split
                                            page?.items?.remove(rect)
                                            val parts = splitHoriz(rect, b)
                                            parts.forEach { onAddItem(it) }
                                            // Linea orizzontale nera sopra le parti (estesa all'intera larghezza del contenitore)
                                            val line = DrawItem.LineItem(
                                                level = rect.level + 1, // visibile sopra le due parti
                                                r0 = b,          // useremo il nostro renderer per posizionarla
                                                c0 = C0 - 1,     // hack per estendere fino ai bordi (vedi draw sotto)
                                                r1 = b,
                                                c1 = C1 + 1,
                                                color = lineBlack,
                                                width = 2.dp
                                            )
                                            onAddItem(line)
                                            return@detectTapGestures
                                        }
                                    } else if (sameCol) {
                                        // Richiesta: i due quadrati devono estendersi da alto a basso del contenitore
                                        val rrMin = min(r0, r1)
                                        val rrMax = max(r0, r1)
                                        val fullSpan = (rrMin == R0 && rrMax == R1 && cc in C0..C1)
                                        if (fullSpan) {
                                            val b = chooseBoundaryCol(rect, cc) ?: return@detectTapGestures
                                            page?.items?.remove(rect)
                                            val parts = splitVert(rect, b)
                                            parts.forEach { onAddItem(it) }
                                            // Linea verticale nera sopra le parti (estesa all'intera altezza del contenitore)
                                            val line = DrawItem.LineItem(
                                                level = rect.level + 1,
                                                r0 = R0 - 1,
                                                c0 = b,
                                                r1 = R1 + 1,
                                                c1 = b,
                                                color = lineBlack,
                                                width = 2.dp
                                            )
                                            onAddItem(line)
                                            return@detectTapGestures
                                        }
                                    }
                                }

                                // Se non soddisfa le condizioni (non stesso rect o non "da lato a lato") => no-op
                                return@detectTapGestures
                            }

                            // Non allineati: crea un rettangolo al livello corrente
                            val rr0 = min(r0, r1); val rr1 = max(r0, r1)
                            val cc0 = min(c0, c1); val cc1 = max(c0, c1)
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
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = min(size.width / cols, size.height / cols)
            val rows = if (cell > 0f) floor(size.height / cell).toInt() else 0

            // Fondo pagina bianco se esiste una page
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
                            // Renderer linea “interna”:
                            if (item.r0 == item.r1) {
                                // Orizzontale; estendi fino ai bordi (hack: c0-1 .. c1+1)
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
                                // Verticale; estendi fino ai bordi (hack: r0-1 .. r1+1)
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

            // Griglia: preview del 1° quadretto o intera
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

            // Evidenziazione riga/colonna del primo ancoraggio
            firstAnchor?.let { (rr, cc) ->
                if (rr >= 0 && cc >= 0) {
                    drawRect(
                        color = azure.copy(alpha = 0.10f),
                        topLeft = Offset(cc.toFloat() * cell, 0f),
                        size = Size(cell, rows.toFloat() * cell)
                    )
                    drawRect(
                        color = azure.copy(alpha = 0.10f),
                        topLeft = Offset(0f, rr.toFloat() * cell),
                        size = Size(cols.toFloat() * cell, cell)
                    )
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

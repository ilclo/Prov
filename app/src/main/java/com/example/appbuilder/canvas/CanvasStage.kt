package com.example.appbuilder.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.*

/** Colore azzurro che usiamo nel resto dell’UI */
private val AZURE = Color(0xFF58A6FF)

/** Modalità contestuali di modifica contenitore */
private enum class ContainerEditMode { Idle, EditProps, Grab, Resize }

/**
 * Stage di disegno:
 * - sfondo bianco (quando c’è una pagina attiva)
 * - overlay griglia (preview singola cella o griglia intera)
 * - creazione contenitori (rettangoli) solo se creationEnabled = true
 * - creazione linee quando i due punti sono allineati su riga/colonna
 * - “manina” ancorata all’angolo in alto-sinistra di ogni rettangolo con menù
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
    // ===== Stato locale interazioni =====
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }       // evidenziazione singola cella
    var firstAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }     // primo punto fissato per creazione
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    // Se sto modificando un contenitore, disabilito interazioni di creazione
    var editMode by remember { mutableStateOf(ContainerEditMode.Idle) }
    var activeRectIndex by remember { mutableStateOf<Int?>(null) }
    var showHandleMenuFor by remember { mutableStateOf<Int?>(null) }

    val density = max(1, gridDensity)

    // Helpers per geometria
    fun pxPerCell(): Float {
        val w = boxSize.width.toFloat()
        val h = boxSize.height.toFloat()
        return min(w / density, h / density)
    }

    fun cellOf(ofs: Offset): Pair<Int, Int> {
        val cell = pxPerCell().takeIf { it > 0f } ?: 1f
        val r = floor(ofs.y / cell).toInt().coerceAtLeast(0)
        val c = floor(ofs.x / cell).toInt().coerceAtLeast(0)
        return r to c
    }

    // ---- Puntatore/gesture: abilitato solo quando serve ----
    val pointerModifier = if (creationEnabled && editMode == ContainerEditMode.Idle) {
        Modifier.pointerInput(density, creationEnabled) {
            detectTapGestures(
                onTap = { ofs ->
                    hoverCell = cellOf(ofs)
                },
                onLongPress = { ofs ->
                    val (r, c) = cellOf(ofs)
                    if (firstAnchor == null) {
                        // Fissa primo punto
                        firstAnchor = r to c
                    } else {
                        val (r0, c0) = firstAnchor!!
                        val r1 = r; val c1 = c
                        if (r0 == r1 || c0 == c1) {
                            // ALLINEATI: crea LINEA tra i lati interni più vicini al centro
                            createLineBetween(r0, c0, r1, c1, currentLevel, page, onAddItem)
                        } else {
                            // Rettangolo
                            val rect = DrawItem.RectItem(
                                level = currentLevel,
                                row0 = min(r0, r1),
                                col0 = min(c0, c1),
                                row1 = max(r0, r1),
                                col1 = max(c0, c1),
                                borderColor = 0xFF000000L // nero
                            )
                            onAddItem(rect)
                        }
                        firstAnchor = null
                    }
                }
            )
        }
    } else when (editMode) {
        ContainerEditMode.Grab -> Modifier.pointerInput(page, editMode, activeRectIndex) {
            detectTapGestures(
                onLongPress = { ofs ->
                    // Sposta il rettangolo attivo ancorando il suo angolo in alto-sx alla cella premuta
                    val idx = activeRectIndex ?: return@detectTapGestures
                    val (r, c) = cellOf(ofs)
                    val rect = page?.items?.getOrNull(idx) as? DrawItem.RectItem ?: return@detectTapGestures
                    val h = abs(rect.row1 - rect.row0)
                    val w = abs(rect.col1 - rect.col0)
                    val moved = rect.copy(
                        row0 = r, col0 = c,
                        row1 = r + h, col1 = c + w
                    )
                    replaceItem(page, idx, moved)
                }
            )
        }
        ContainerEditMode.Resize -> Modifier.pointerInput(page, editMode, activeRectIndex) {
            detectTapGestures(
                onLongPress = { ofs ->
                    // Usa la cella premuta come “secondo punto”; l’angolo opposto resta fisso
                    val idx = activeRectIndex ?: return@detectTapGestures
                    val (r, c) = cellOf(ofs)
                    val rect = page?.items?.getOrNull(idx) as? DrawItem.RectItem ?: return@detectTapGestures
                    val fixedTop = min(rect.row0, rect.row1)
                    val fixedLeft = min(rect.col0, rect.col1)
                    val resized = DrawItem.RectItem(
                        level = rect.level,
                        row0 = min(fixedTop, r),
                        col0 = min(fixedLeft, c),
                        row1 = max(fixedTop, r),
                        col1 = max(fixedLeft, c),
                        borderColor = rect.borderColor
                    )
                    replaceItem(page, idx, resized)
                }
            )
        }
        else -> Modifier // EditProps/Idle: niente cattura
    }

    // ===== LAYOUT =====
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .then(pointerModifier)
    ) {
        // Disegno
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = density
            val cell = pxPerCell()
            val rows = if (cell > 0f) floor(size.height / cell).toInt() else 0

            // 0) Pagina bianca a pieno canvas (quando c'è una pagina)
            if (page != null) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height)
                )
            }

            // 1) Elementi fino al currentLevel
            page?.items?.forEachIndexed { index, item ->
                when (item) {
                    is DrawItem.RectItem -> if (item.level <= currentLevel) {
                        val left = min(item.col0, item.col1) * cell
                        val top = min(item.row0, item.row1) * cell
                        val w = (abs(item.col1 - item.col0) + 1) * cell
                        val h = (abs(item.row1 - item.row0) + 1) * cell

                        drawRect(
                            color = Color.White, // riempimento default
                            topLeft = Offset(left, top),
                            size = Size(w, h)
                        )
                        val border = Color((item.borderColor and 0xFFFFFFFF).toInt())
                        drawRect(
                            color = border,
                            topLeft = Offset(left, top),
                            size = Size(w, h),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    is DrawItem.LineItem -> if (item.level <= currentLevel) {
                        val y = (item.row + 0.5f) * cell
                        val x = (item.col + 0.5f) * cell
                        if (item.horizontal) {
                            // segmenti già salvati in termini di “interni”
                            val x0 = item.x0Edge * cell
                            val x1 = item.x1Edge * cell
                            drawLine(
                                color = AZURE,
                                start = Offset(x0, y),
                                end = Offset(x1, y),
                                strokeWidth = item.thicknessDp.dp.toPx()
                            )
                        } else {
                            val y0 = item.y0Edge * cell
                            val y1 = item.y1Edge * cell
                            drawLine(
                                color = AZURE,
                                start = Offset(x, y0),
                                end = Offset(x, y1),
                                strokeWidth = item.thicknessDp.dp.toPx()
                            )
                        }
                    }
                }
            }

            // 2) Overlay griglia (preview / intera)
            if (gridPreviewOnly) {
                if (rows > 0 && cols > 0) {
                    drawRect(
                        color = AZURE.copy(alpha = 0.20f),
                        topLeft = Offset(0f, 0f),
                        size = Size(cell, cell)
                    )
                    drawRect(
                        color = AZURE,
                        topLeft = Offset(0f, 0f),
                        size = Size(cell, cell),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            } else if (showFullGrid) {
                for (c in 0..cols) {
                    val x = c * cell
                    drawLine(
                        color = AZURE.copy(alpha = 0.30f),
                        start = Offset(x, 0f),
                        end = Offset(x, rows * cell),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                for (r in 0..rows) {
                    val y = r * cell
                    drawLine(
                        color = AZURE.copy(alpha = 0.30f),
                        start = Offset(0f, y),
                        end = Offset(cols * cell, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // 3) Hover cell (solo se non sto modificando un contenitore)
            if (editMode == ContainerEditMode.Idle) {
                hoverCell?.let { (rr, cc) ->
                    if (rr in 0 until rows && cc in 0 until cols) {
                        drawRect(
                            color = AZURE.copy(alpha = 0.18f),
                            topLeft = Offset(cc * cell, rr * cell),
                            size = Size(cell, cell)
                        )
                        drawRect(
                            color = AZURE,
                            topLeft = Offset(cc * cell, rr * cell),
                            size = Size(cell, cell),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                // Riga/colonna del primo ancoraggio (dopo 1° long‑press)
                firstAnchor?.let { (rr, cc) ->
                    if (rr in 0 until rows && cc in 0 until cols) {
                        drawRect(
                            color = AZURE.copy(alpha = 0.10f),
                            topLeft = Offset(cc * cell, 0f),
                            size = Size(cell, rows * cell)
                        )
                        drawRect(
                            color = AZURE.copy(alpha = 0.10f),
                            topLeft = Offset(0f, rr * cell),
                            size = Size(cols * cell, cell)
                        )
                        drawRect(
                            color = AZURE.copy(alpha = 0.22f),
                            topLeft = Offset(cc * cell, rr * cell),
                            size = Size(cell, cell)
                        )
                        drawRect(
                            color = AZURE,
                            topLeft = Offset(cc * cell, rr * cell),
                            size = Size(cell, cell),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }
        } // Canvas

        // 4) Overlay “manina” per ogni rettangolo + menù contestuale
        val cellPx = pxPerCell()
        page?.items?.forEachIndexed { idx, item ->
            val rect = item as? DrawItem.RectItem ?: return@forEachIndexed
            // coordinate in px dell’angolo top-left
            val leftCells = min(rect.col0, rect.col1).toFloat()
            val topCells  = min(rect.row0, rect.row1).toFloat()
            val leftPx = leftCells * cellPx
            val topPx  = topCells  * cellPx
            // dimensione icona proporzionale ma limitata
            val wCells = abs(rect.col1 - rect.col0) + 1
            val hCells = abs(rect.row1 - rect.row0) + 1
            val iconDp = clampIconSize(wCells, hCells)

            // icona corrente in base alla modalità
            val iconRes = when {
                activeRectIndex == idx && editMode == ContainerEditMode.Grab   -> com.example.appbuilder.R.drawable.ic_grab
                activeRectIndex == idx && editMode == ContainerEditMode.Resize -> com.example.appbuilder.R.drawable.ic_resize
                else -> com.example.appbuilder.R.drawable.ic_point
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                    .size(iconDp)
                    .combinedClickable(
                        onClick = {
                            // tap: entra in "EditProps" (stub: per ora non cambiamo nulla a EditorKit)
                            activeRectIndex = idx
                            editMode = ContainerEditMode.EditProps
                            showHandleMenuFor = null
                        },
                        onLongClick = {
                            // tap prolungato: mostra menù delle 4 icone
                            activeRectIndex = idx
                            showHandleMenuFor = idx
                        }
                    ),
                contentAlignment = Alignment.TopStart
            ) {
                Icon(
                    imageVector = vectorResource(id = iconRes),
                    contentDescription = "Handle",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // menù contestuale (row con 4 icone) – appare vicino alla manina
            if (showHandleMenuFor == idx) {
                Surface(
                    color = Color(0xEE0F141E),
                    contentColor = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .offset { IntOffset(leftPx.roundToInt(), (topPx + iconDp.toPx() + 4f).roundToInt()) }
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MenuIcon(
                            id = com.example.appbuilder.R.drawable.ic_point,
                            desc = "Modifica",
                            onClick = {
                                editMode = ContainerEditMode.EditProps
                                showHandleMenuFor = null
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        MenuIcon(
                            id = com.example.appbuilder.R.drawable.ic_grab,
                            desc = "Sposta",
                            onClick = {
                                editMode = ContainerEditMode.Grab
                                showHandleMenuFor = null
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        MenuIcon(
                            id = com.example.appbuilder.R.drawable.ic_resize,
                            desc = "Ridimensiona",
                            onClick = {
                                editMode = ContainerEditMode.Resize
                                showHandleMenuFor = null
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        MenuIcon(
                            id = com.example.appbuilder.R.drawable.ic_x,
                            desc = "Chiudi",
                            onClick = {
                                editMode = ContainerEditMode.Idle
                                activeRectIndex = null
                                showHandleMenuFor = null
                            }
                        )
                    }
                }
            }
        }
    }
}

/* ---------- Helpers UI/Model ---------- */

@Composable
private fun MenuIcon(id: Int, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = vectorResource(id = id), contentDescription = desc, tint = Color.White)
    }
}

private fun Dp.toPx(): Float = this.value * 3f // abbastanza per icone; non critico

private fun clampIconSize(wCells: Int, hCells: Int): Dp {
    val base = 18
    val grow = min(max(wCells, hCells), 6) // non oltre 6 celle di lato
    val px = base + grow * 2
    return px.dp.coerceIn(18.dp, 32.dp)
}

private fun replaceItem(page: PageState?, index: Int, newItem: DrawItem.RectItem) {
    val list = page?.items ?: return
    if (index in list.indices) list[index] = newItem
}

/**
 * Crea una linea tra due celle allineate:
 *  - Se r0==r1 → orizzontale: unisce i **lati interni** (destra del sinistro, sinistra del destro)
 *  - Se c0==c1 → verticale: unisce i **lati interni** (sotto del superiore, sopra dell’inferiore)
 * Livello: 1 + max livello tra gli elementi intersecati (oppure currentLevel se non interseca).
 *
 * TODO: distinguere i 3 casi richiesti (nessun nuovo rettangolo / parziale / completo)
 *       e mostrare un menù per la scelta. Per ora: linea a livello superiore.
 */
private fun createLineBetween(
    r0: Int, c0: Int, r1: Int, c1: Int,
    currentLevel: Int,
    page: PageState?,
    onAdd: (DrawItem) -> Unit
) {
    if (r0 == r1 && c0 != c1) {
        val left = min(c0, c1)
        val right = max(c0, c1)
        val row = r0
        // lati interni
        val x0Edge = left + 1 // bordo destro della cella sinistra
        val x1Edge = right    // bordo sinistro della cella destra
        val level = 1 + maxIntersectingLevelHorizontal(page, row, x0Edge, x1Edge).coerceAtLeast(currentLevel)
        onAdd(
            DrawItem.LineItem(
                level = level,
                horizontal = true,
                row = row, col = 0,
                x0Edge = x0Edge.toFloat(),
                x1Edge = x1Edge.toFloat(),
                y0Edge = 0f, y1Edge = 0f,
                thicknessDp = 1.0f
            )
        )
    } else if (c0 == c1 && r0 != r1) {
        val top = min(r0, r1)
        val bottom = max(r0, r1)
        val col = c0
        val y0Edge = top + 1   // bordo inferiore della cella in alto
        val y1Edge = bottom    // bordo superiore della cella in basso
        val level = 1 + maxIntersectingLevelVertical(page, col, y0Edge, y1Edge).coerceAtLeast(currentLevel)
        onAdd(
            DrawItem.LineItem(
                level = level,
                horizontal = false,
                row = 0, col = col,
                x0Edge = 0f, x1Edge = 0f,
                y0Edge = y0Edge.toFloat(),
                y1Edge = y1Edge.toFloat(),
                thicknessDp = 1.0f
            )
        )
    }
}

private fun maxIntersectingLevelHorizontal(page: PageState?, row: Int, x0Edge: Int, x1Edge: Int): Int {
    val items = page?.items ?: return 0
    var maxL = 0
    items.forEach { it ->
        when (it) {
            is DrawItem.RectItem -> {
                val rMin = min(it.row0, it.row1)
                val rMax = max(it.row0, it.row1)
                val cMin = min(it.col0, it.col1)
                val cMax = max(it.col0, it.col1)
                if (row in rMin..rMax) {
                    // se orizzontalmente la proiezione si sovrappone ai bordi interni
                    val a = x0Edge
                    val b = x1Edge
                    if (b > cMin && a < cMax) maxL = max(maxL, it.level)
                }
            }
            is DrawItem.LineItem -> if (it.horizontal && it.row == row) {
                // se possibile sovrapposizione, considera il livello
                maxL = max(maxL, it.level)
            }
        }
    }
    return maxL
}

private fun maxIntersectingLevelVertical(page: PageState?, col: Int, y0Edge: Int, y1Edge: Int): Int {
    val items = page?.items ?: return 0
    var maxL = 0
    items.forEach { it ->
        when (it) {
            is DrawItem.RectItem -> {
                val rMin = min(it.row0, it.row1)
                val rMax = max(it.row0, it.row1)
                val cMin = min(it.col0, it.col1)
                val cMax = max(it.col0, it.col1)
                if (col in cMin..cMax) {
                    val a = y0Edge
                    val b = y1Edge
                    if (b > rMin && a < rMax) maxL = max(maxL, it.level)
                }
            }
            is DrawItem.LineItem -> if (!it.horizontal && it.col == col) {
                maxL = max(maxL, it.level)
            }
        }
    }
    return maxL
}

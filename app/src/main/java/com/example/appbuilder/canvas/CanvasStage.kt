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

/**
 * Modalità strumenti disponibili (definita nel tuo package canvas).
 * Create | Point | Grab | Resize
 *
 * NB: qui la usiamo soltanto; non ridefinirla se l'hai già.
 */
// enum class ToolMode { Create, Point, Grab, Resize }

/**
 * Stage di disegno e interazioni:
 * - Create: crea contenitori (rettangolo) oppure divide un contenitore con una linea da lato a lato
 * - Point : tap su contenitore → apri menù “Contenitore” (callback)
 * - Grab  : long-press + trascinamento a scatti di cella con collisioni
 * - Resize: long-press 1 = angolo ancora; long-press 2 = angolo opposto (collisioni evitate)
 */
@Composable
fun CanvasStage(
    page: PageState?,
    gridDensity: Int,
    gridPreviewOnly: Boolean,
    showFullGrid: Boolean,
    currentLevel: Int,
    creationEnabled: Boolean,
    toolMode: ToolMode,
    onAddItem: (DrawItem) -> Unit,
    onUpdateItem: (DrawItem.RectItem) -> Unit = {},
    onRequestEdit: (DrawItem.RectItem) -> Unit = {}
) {
    // Stato interno: ancore e selezioni contestuali
    var createAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }     // per Create
    var hoverCell   by remember { mutableStateOf<Pair<Int, Int>?>(null) }      // highlight cella
    var pointStage  by remember { mutableStateOf<DrawItem.RectItem?>(null) }   // per “2° tap” in Point
    var resizeAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }     // cella angolo fisso
    var resizeRect   by remember { mutableStateOf<DrawItem.RectItem?>(null) }  // rettangolo che si ridimensiona

    // Box con dimensioni ⇒ ricavo la “cell size” in px
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cols = max(1, gridDensity)
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val cell = min(wPx / cols, hPx / cols)            // lato cella in px
        val rows = max(1, floor(hPx / cell).toInt())

        // Helper: cella da Offset
        fun cellOf(ofs: Offset): Pair<Int, Int> {
            val r = floor(ofs.y / cell).toInt().coerceIn(0, rows - 1)
            val c = floor(ofs.x / cell).toInt().coerceIn(0, cols - 1)
            return r to c
        }

        // Cerca il primo rettangolo (al livello visibile) che contiene (r,c)
        fun findRectAt(r: Int, c: Int): DrawItem.RectItem? {
            val items = page?.items ?: return null
            // priorità: rettangoli con level <= currentLevel; se ve ne sono più, prendi l’ultimo disegnato (in coda)
            for (i in items.indices.reversed()) {
                val it = items[i]
                if (it is DrawItem.RectItem && it.level <= currentLevel) {
                    if (r in min(it.r0, it.r1)..max(it.r0, it.r1) &&
                        c in min(it.c0, it.c1)..max(it.c0, it.c1)
                    ) return it
                }
            }
            return null
        }

        fun rectContains(rect: DrawItem.RectItem, r: Int, c: Int): Boolean =
            r in min(rect.r0, rect.r1)..max(rect.r0, rect.r1) &&
            c in min(rect.c0, rect.c1)..max(rect.c0, rect.c1)

        fun intersects(a: DrawItem.RectItem, b: DrawItem.RectItem): Boolean {
            val aL = min(a.c0, a.c1); val aR = max(a.c0, a.c1)
            val aT = min(a.r0, a.r1); val aB = max(a.r0, a.r1)
            val bL = min(b.c0, b.c1); val bR = max(b.c0, b.c1)
            val bT = min(b.r0, b.r1); val bB = max(b.r0, b.r1)
            // sovrapposizione (esclusa adiacenza)
            return !(aR < bL || aL > bR || aB < bT || aT > bB)
        }

        fun collidesWithOthers(candidate: DrawItem.RectItem, ignore: DrawItem.RectItem?): Boolean {
            val items = page?.items ?: return false
            return items.any {
                it is DrawItem.RectItem &&
                it !== ignore &&
                it.level == candidate.level &&
                intersects(candidate, it)
            }
        }

        fun replaceRect(old: DrawItem.RectItem, new: DrawItem.RectItem) {
            val items = page?.items ?: return
            val idx = items.indexOf(old)
            if (idx >= 0) {
                items[idx] = new
                onUpdateItem(new)
            }
        }

        fun removeRect(target: DrawItem.RectItem) {
            val items = page?.items ?: return
            items.remove(target)
        }

        fun addRect(new: DrawItem.RectItem) {
            onAddItem(new)
        }

        // =============== GESTURE: TAP / LONG-PRESS ===============
        val tapGesture = Modifier.pointerInput(toolMode, creationEnabled, currentLevel, cols, rows) {
            detectTapGestures(
                onTap = { ofs ->
                    val (r, c) = cellOf(ofs)
                    hoverCell = r to c

                    when (toolMode) {
                        ToolMode.Point -> {
                            val rect = findRectAt(r, c) ?: return@detectTapGestures
                            // 1° tap: salvo; 2° tap: chiamo editor
                            pointStage = if (pointStage == null) {
                                rect
                            } else {
                                onRequestEdit(rect)
                                null
                            }
                        }
                        ToolMode.Create -> {
                            // in Create il tap singolo serve solo per hover; azioni su long-press
                        }
                        ToolMode.Grab   -> Unit
                        ToolMode.Resize -> Unit
                    }
                },
                onLongPress = { ofs ->
                    val (r, c) = cellOf(ofs)
                    when (toolMode) {
                        ToolMode.Create -> {
                            // logica doppio long-press: se i due punti sono nello stesso contenitore ed “a lato a lato” ⇒ split
                            val container = findRectAt(r, c)
                            if (createAnchor == null) {
                                createAnchor = r to c
                            } else {
                                val (r0, c0) = createAnchor!!
                                val container2 = findRectAt(r0, c0)
                                if (container != null &&
                                    container2 != null &&
                                    container === container2
                                ) {
                                    // stesso contenitore: se stessi r o stessi c, e l’ancora tocca un lato e il secondo l’altro lato ⇒ split
                                    val L = min(container.c0, container.c1)
                                    val R = max(container.c0, container.c1)
                                    val T = min(container.r0, container.r1)
                                    val B = max(container.r0, container.r1)

                                    val sameRow = (r == r0) && (r in T..B)
                                    val sameCol = (c == c0) && (c in L..R)

                                    val anchorTouchesLeft  = (c0 == L)
                                    val anchorTouchesRight = (c0 == R)
                                    val anchorTouchesTop   = (r0 == T)
                                    val anchorTouchesBottom= (r0 == B)

                                    val currTouchesLeft    = (c == L)
                                    val currTouchesRight   = (c == R)
                                    val currTouchesTop     = (r == T)
                                    val currTouchesBottom  = (r == B)

                                    var didSplit = false
                                    if (sameRow && anchorTouchesLeft && currTouchesRight) {
                                        // orizzontale da lato a lato → split in alto/basso
                                        val topRect = container.copy(r0 = T, c0 = L, r1 = r - 1, c1 = R)
                                        val bottomRect = container.copy(r0 = r + 1, c0 = L, r1 = B, c1 = R)
                                        removeRect(container)
                                        if (topRect.r0 <= topRect.r1) addRect(topRect)
                                        if (bottomRect.r0 <= bottomRect.r1) addRect(bottomRect)
                                        didSplit = true
                                    } else if (sameRow && anchorTouchesRight && currTouchesLeft) {
                                        val topRect = container.copy(r0 = T, c0 = L, r1 = r - 1, c1 = R)
                                        val bottomRect = container.copy(r0 = r + 1, c0 = L, r1 = B, c1 = R)
                                        removeRect(container)
                                        if (topRect.r0 <= topRect.r1) addRect(topRect)
                                        if (bottomRect.r0 <= bottomRect.r1) addRect(bottomRect)
                                        didSplit = true
                                    } else if (sameCol && anchorTouchesTop && currTouchesBottom) {
                                        // verticale da lato a lato → split in sinistra/destra
                                        val leftRect  = container.copy(r0 = T, c0 = L, r1 = B, c1 = c - 1)
                                        val rightRect = container.copy(r0 = T, c0 = c + 1, r1 = B, c1 = R)
                                        removeRect(container)
                                        if (leftRect.c0 <= leftRect.c1) addRect(leftRect)
                                        if (rightRect.c0 <= rightRect.c1) addRect(rightRect)
                                        didSplit = true
                                    } else if (sameCol && anchorTouchesBottom && currTouchesTop) {
                                        val leftRect  = container.copy(r0 = T, c0 = L, r1 = B, c1 = c - 1)
                                        val rightRect = container.copy(r0 = T, c0 = c + 1, r1 = B, c1 = R)
                                        removeRect(container)
                                        if (leftRect.c0 <= leftRect.c1) addRect(leftRect)
                                        if (rightRect.c0 <= rightRect.c1) addRect(rightRect)
                                        didSplit = true
                                    }

                                    if (!didSplit) {
                                        // fallback: crea un contenitore nuovo come area (min..max) senza linea
                                        val nr0 = min(r0, r); val nr1 = max(r0, r)
                                        val nc0 = min(c0, c); val nc1 = max(c0, c)
                                        val newRect = DrawItem.RectItem(
                                            level = currentLevel,
                                            r0 = nr0, c0 = nc0,
                                            r1 = nr1, c1 = nc1,
                                            borderColor = Color.Black,
                                            borderWidth = 1.dp,
                                            fillColor = Color.White
                                        )
                                        // evito collisione con altri allo stesso livello
                                        if (!collidesWithOthers(newRect, null)) addRect(newRect)
                                    }
                                } else {
                                    // punti non nello stesso contenitore → crea rettangolo “classico”
                                    val nr0 = min(r0, r); val nr1 = max(r0, r)
                                    val nc0 = min(c0, c); val nc1 = max(c0, c)
                                    val newRect = DrawItem.RectItem(
                                        level = currentLevel,
                                        r0 = nr0, c0 = nc0,
                                        r1 = nr1, c1 = nc1,
                                        borderColor = Color.Black,
                                        borderWidth = 1.dp,
                                        fillColor = Color.White
                                    )
                                    if (!collidesWithOthers(newRect, null)) addRect(newRect)
                                }
                                createAnchor = null
                            }
                        }

                        ToolMode.Resize -> {
                            val rect = findRectAt(r, c) ?: return@detectTapGestures
                            // 1° long-press: imposta ancora + rettangolo
                            if (resizeRect == null) {
                                resizeRect = rect
                                // ancora = l’angolo più vicino alla cella premuta
                                val corners = listOf(rect.r0 to rect.c0, rect.r0 to rect.c1, rect.r1 to rect.c0, rect.r1 to rect.c1)
                                resizeAnchor = corners.minByOrNull { (rr, cc) -> abs(rr - r) + abs(cc - c) }
                            } else {
                                // 2° long-press: definisce nuovo angolo opposto e tenta resize
                                val base = resizeRect!!
                                val (ar, ac) = resizeAnchor ?: (base.r0 to base.c0)
                                val target = base.copy(
                                    r0 = min(ar, r), c0 = min(ac, c),
                                    r1 = max(ar, r), c1 = max(ac, c)
                                )
                                // blocca sovrapposizioni
                                if (!collidesWithOthers(target, base)) {
                                    replaceRect(base, target)
                                }
                                resizeRect = null
                                resizeAnchor = null
                            }
                        }

                        ToolMode.Point -> {
                            // già gestito su tap
                        }

                        ToolMode.Grab -> {
                            // drag gestito sotto (detectDragGesturesAfterLongPress)
                        }
                    }
                }
            )
        }

        // =============== GESTURE: DRAG (GRAB) ===============
        val dragGesture = Modifier.pointerInput(toolMode, currentLevel, cols, rows) {
            if (toolMode != ToolMode.Grab) return@pointerInput
            detectDragGesturesAfterLongPress(
                onDragStart = { ofs -> 
                    val (r, c) = cellOf(ofs)
                    // seleziona rettangolo da trascinare
                    val rect = findRectAt(r, c) ?: return@detectDragGesturesAfterLongPress
                    resizeRect = rect // riuso questo slot come “rect in manipolazione”
                },
                onDrag = { change, drag ->
                    val rect = resizeRect ?: return@detectDragGesturesAfterLongPress
                    change.consume()

                    // accumulo in px → scatti di cella
                    var dx = drag.x
                    var dy = drag.y

                    var moved = rect
                    fun tryMove(stepR: Int, stepC: Int): DrawItem.RectItem {
                        val nr0 = (min(moved.r0, moved.r1) + stepR).coerceIn(0, rows - 1)
                        val nr1 = (max(moved.r0, moved.r1) + stepR).coerceIn(0, rows - 1)
                        val nc0 = (min(moved.c0, moved.c1) + stepC).coerceIn(0, cols - 1)
                        val nc1 = (max(moved.c0, moved.c1) + stepC).coerceIn(0, cols - 1)
                        val candidate = moved.copy(r0 = nr0, c0 = nc0, r1 = nr1, c1 = nc1)
                        return if (!collidesWithOthers(candidate, rect)) candidate else moved
                    }

                    // applico scatti X
                    while (abs(dx) >= cell) {
                        val stepC = if (dx > 0) +1 else -1
                        val candidate = tryMove(stepR = 0, stepC = stepC)
                        if (candidate !== moved) moved = candidate
                        dx -= cell * (if (dx > 0) 1 else -1)
                    }
                    // applico scatti Y
                    while (abs(dy) >= cell) {
                        val stepR = if (dy > 0) +1 else -1
                        val candidate = tryMove(stepR = stepR, stepC = 0)
                        if (candidate !== moved) moved = candidate
                        dy -= cell * (if (dy > 0) 1 else -1)
                    }

                    if (moved !== rect) {
                        replaceRect(rect, moved)
                        resizeRect = moved // continua a trascinare quello aggiornato
                    }
                },
                onDragEnd = { resizeRect = null },
                onDragCancel = { resizeRect = null }
            )
        }

        // =================== DRAW ===================
        Box(Modifier.fillMaxSize().then(tapGesture).then(dragGesture)) {
            Canvas(Modifier.fillMaxSize()) {
                // (A) background pagina *bianca* sempre visibile
                drawRect(color = Color.White, topLeft = Offset.Zero, size = size)

                // (B) disegno elementi esistenti fino al currentLevel
                page?.items?.forEach { item ->
                    if (item.level <= currentLevel) {
                        when (item) {
                            is DrawItem.RectItem -> {
                                val left = min(item.c0, item.c1) * cell
                                val top  = min(item.r0, item.r1) * cell
                                val w = (abs(item.c1 - item.c0) + 1) * cell
                                val h = (abs(item.r1 - item.r0) + 1) * cell
                                // riempimento
                                drawRect(
                                    color = item.fillColor,
                                    topLeft = Offset(left, top),
                                    size = Size(w, h)
                                )
                                // bordo
                                drawRect(
                                    color = item.borderColor,
                                    topLeft = Offset(left, top),
                                    size = Size(w, h),
                                    style = Stroke(width = item.borderWidth.toPx())
                                )
                            }
                            is DrawItem.LineItem -> {
                                // opzionale: se vuoi mantenere linee
                                val x0 = (item.c0 + 0.5f) * cell
                                val y0 = (item.r0 + 0.5f) * cell
                                val x1 = (item.c1 + 0.5f) * cell
                                val y1 = (item.r1 + 0.5f) * cell
                                drawLine(
                                    color = item.color,
                                    start = Offset(x0, y0),
                                    end = Offset(x1, y1),
                                    strokeWidth = item.width.toPx()
                                )
                            }
                        }
                    }
                }

                // (C) overlay griglia
                val azure = Color(0xFF58A6FF)
                if (gridPreviewOnly) {
                    // solo cella [0,0]
                    if (rows > 0 && cols > 0) {
                        drawRect(azure.copy(alpha = 0.20f), Offset(0f, 0f), Size(cell, cell))
                        drawRect(azure, Offset(0f, 0f), Size(cell, cell), style = Stroke(1.5.dp.toPx()))
                    }
                } else if (showFullGrid) {
                    for (c in 0..cols) {
                        val x = c * cell
                        drawLine(azure.copy(alpha = 0.30f), Offset(x, 0f), Offset(x, rows * cell), 1.dp.toPx())
                    }
                    for (r in 0..rows) {
                        val y = r * cell
                        drawLine(azure.copy(alpha = 0.30f), Offset(0f, y), Offset(cols * cell, y), 1.dp.toPx())
                    }
                }

                // (D) hover cell
                hoverCell?.let { (rr, cc) ->
                    if (rr in 0 until rows && cc in 0 until cols) {
                        drawRect(azure.copy(alpha = 0.18f), Offset(cc * cell, rr * cell), Size(cell, cell))
                        drawRect(azure, Offset(cc * cell, rr * cell), Size(cell, cell), style = Stroke(1.dp.toPx()))
                    }
                }

                // (E) riga/colonna dell’ancora “Create” (dopo 1° long press)
                createAnchor?.let { (rr, cc) ->
                    val rect = findRectAt(rr, cc)
                    if (rect != null) {
                        val L = min(rect.c0, rect.c1)
                        val R = max(rect.c0, rect.c1)
                        val T = min(rect.r0, rect.r1)
                        val B = max(rect.r0, rect.r1)
                        // colonna dell’ancora
                        drawRect(azure.copy(alpha = 0.10f), Offset(cc * cell, T * cell), Size(cell, (B - T + 1) * cell))
                        // riga dell’ancora
                        drawRect(azure.copy(alpha = 0.10f), Offset(L * cell, rr * cell), Size((R - L + 1) * cell, cell))
                        // cella ancora evidenziata
                        drawRect(azure.copy(alpha = 0.22f), Offset(cc * cell, rr * cell), Size(cell, cell))
                        drawRect(azure, Offset(cc * cell, rr * cell), Size(cell, cell), style = Stroke(1.5.dp.toPx()))
                    }
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

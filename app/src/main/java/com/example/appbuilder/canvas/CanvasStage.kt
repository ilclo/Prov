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
 * Stage di disegno: mostra pagina, overlay griglia (preview singola cella o griglia intera),
 * e consente la creazione dei contenitori (rettangoli) con 2 tap lunghi.
 *
 * @param page             stato pagina (può essere null nelle fasi iniziali)
 * @param gridDensity      densità corrente (N celle per riga/colonna)
 * @param gridPreviewOnly  true => mostra solo il primo quadrato in alto-sx (mentre trascini lo slider)
 * @param showFullGrid     true => mostra la griglia completa (dopo 0.5s che non trascini)
 * @param currentLevel     livello attivo (si disegnano gli elementi con level <= currentLevel)
 * @param creationEnabled  gate: consente il “modo creazione contenitore” solo quando abilitato
 * @param onAddItem        callback per aggiungere un nuovo elemento creato dall’utente
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
    // ====== Stati interni ======
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }      // evidenziazione singola cella (tap corto)
    var firstAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }    // primo estremo (long press)
    // Tool per rettangoli
    enum class RectTool { Point, Grab, Resize }
    var activeRect by remember { mutableStateOf<DrawItem.RectItem?>(null) }
    var currentTool by remember { mutableStateOf<RectTool?>(null) }
    var showRectMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Decisione linea → dialogo
    data class LineCtx(
        val line: DrawItem.LineItem,
        val overlapped: List<DrawItem.RectItem>,
        val splitCandidates: List<DrawItem.RectItem>,
        val maxOverLevel: Int,
        val horizontal: Boolean
    )
    var pendingLineCtx by remember { mutableStateOf<LineCtx?>(null) }

    // Colore accento (azzurro già usato)
    val Azure = Color(0xFF58A6FF)

    // ====== Layout con misure per overlay (BoxWithConstraints) ======
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cols = max(1, gridDensity)
        val cellDp = (min(maxWidth, maxHeight) / cols.toFloat())
        val density = LocalDensity.current

        // ====== Interazione: Tap / Long‑press ======
        // Nota: blocco totale creazione quando attivo uno strumento sul rettangolo
        val creationGate = creationEnabled && (currentTool == null)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cols, creationGate, currentTool) {
                    detectTapGestures(
                        onTap = { ofs ->
                            if (!creationGate) return@detectTapGestures
                            val cell = computeCell(ofs, cols, this.size.width.toFloat(), this.size.height.toFloat())
                            hoverCell = cell
                        },
                        onLongPress = { ofs ->
                            val (r, c) = computeCell(ofs, cols, this.size.width.toFloat(), this.size.height.toFloat())

                            // 1) Tool attivi per un rettangolo
                            activeRect?.let { rect ->
                                when (currentTool) {
                                    RectTool.Grab -> {
                                        // Sposta mantenendo le dimensioni
                                        val w = abs(rect.c1 - rect.c0)
                                        val h = abs(rect.r1 - rect.r0)
                                        val rows = floor(this.size.height.toFloat() / (min(this.size.width, this.size.height).toFloat() / cols)).toInt()
                                        val newC0 = c.coerceIn(0, (cols - 1 - w).coerceAtLeast(0))
                                        val newR0 = r.coerceIn(0, (rows - 1 - h).coerceAtLeast(0))
                                        rect.c0 = newC0; rect.c1 = newC0 + w
                                        rect.r0 = newR0; rect.r1 = newR0 + h

                                        // Se si sovrappone a pari livello → porta sopra
                                        val overlapped = page?.items
                                            ?.filterIsInstance<DrawItem.RectItem>()
                                            ?.filter { other ->
                                                other !== rect &&
                                                other.level == rect.level &&
                                                !(rect.c1 < min(other.c0, other.c1) || rect.c0 > max(other.c0, other.c1) ||
                                                  rect.r1 < min(other.r0, other.r1) || rect.r0 > max(other.r0, other.r1))
                                            }
                                            .orEmpty()
                                        if (overlapped.isNotEmpty()) {
                                            val topLevel = overlapped.maxOf { it.level }
                                            rect.level = topLevel + 1
                                        }
                                        return@detectTapGestures
                                    }
                                    RectTool.Resize -> {
                                        // Ridimensiona: angolo fisso = opposto rispetto al punto premuto
                                        val midR = (rect.r0 + rect.r1) / 2f
                                        val midC = (rect.c0 + rect.c1) / 2f
                                        val anchorR = if (r < midR) max(rect.r0, rect.r1) else min(rect.r0, rect.r1)
                                        val anchorC = if (c < midC) max(rect.c0, rect.c1) else min(rect.c0, rect.c1)
                                        rect.r0 = min(anchorR, r); rect.r1 = max(anchorR, r)
                                        rect.c0 = min(anchorC, c); rect.c1 = max(anchorC, c)
                                        return@detectTapGestures
                                    }
                                    RectTool.Point -> {
                                        // Point: proprietà (solo colore) → nessuna azione sul long‑press
                                        return@detectTapGestures
                                    }
                                    null -> { /* non attivo tool → proseguo con creazione contenitori/linee */ }
                                }
                            }

                            // 2) Creazione contenitori / linee
                            if (!creationGate) return@detectTapGestures

                            if (firstAnchor == null) {
                                // Fissa il primo quadrato
                                firstAnchor = r to c
                            } else {
                                val (r0, c0) = firstAnchor!!
                                val rr0 = min(r0, r)
                                val rr1 = max(r0, r)
                                val cc0 = min(c0, c)
                                val cc1 = max(c0, c)

                                if (r0 == r || c0 == c) {
                                    // === LINEA ===
                                    val horizontal = (r0 == r)
                                    val line = if (horizontal) {
                                        DrawItem.LineItem(
                                            level = currentLevel,
                                            r0 = r0, c0 = cc0, r1 = r0, c1 = cc1,
                                            color = Azure,
                                            width = 2.dp
                                        )
                                    } else {
                                        DrawItem.LineItem(
                                            level = currentLevel,
                                            r0 = rr0, c0 = c0, r1 = rr1, c1 = c0,
                                            color = Azure,
                                            width = 2.dp
                                        )
                                    }

                                    // Calcola rettangoli incrociati e split candidates
                                    val rects = page?.items?.filterIsInstance<DrawItem.RectItem>().orEmpty()
                                    val overlapped = rects.filter { rect ->
                                        val R0 = min(rect.r0, rect.r1); val R1 = max(rect.r0, rect.r1)
                                        val C0 = min(rect.c0, rect.c1); val C1 = max(rect.c0, rect.c1)
                                        if (horizontal) {
                                            (line.r0 in R0..R1) && !(line.c1 < C0 || line.c0 > C1)
                                        } else {
                                            (line.c0 in C0..C1) && !(line.r1 < R0 || line.r0 > R1)
                                        }
                                    }
                                    val splitCandidates = overlapped.filter { rect ->
                                        if (horizontal) crossesInsideHoriz(line.r0, min(line.c0, line.c1), max(line.c0, line.c1), rect)
                                        else            crossesInsideVert(line.c0, min(line.r0, line.r1), max(line.r0, line.r1), rect)
                                    }
                                    val maxOverLevel = overlapped.maxOfOrNull { it.level } ?: currentLevel

                                    if (splitCandidates.isEmpty()) {
                                        // nessuno split → linea sopra a tutto ciò che incrocia
                                        line.level = maxOverLevel + 1
                                        onAddItem(line)
                                    } else {
                                        // chiedo se splittare
                                        pendingLineCtx = LineCtx(line, overlapped, splitCandidates, maxOverLevel, horizontal)
                                    }
                                } else {
                                    // === RETTANGOLO ===
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
            // ====== Disegno ======
            Canvas(modifier = Modifier.fillMaxSize()) {
                // (1) Fondo pagina bianco se page!=null
                if (page != null) {
                    drawRect(color = Color.White, topLeft = Offset.Zero, size = size)
                }

                val colsN = cols
                val cell = min(size.width / colsN, size.height / colsN)
                val rows = if (cell > 0f) floor(size.height / cell).toInt() else 0

                // (2) Elementi esistenti fino al currentLevel
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

                // (3) Overlay griglia
                if (gridPreviewOnly) {
                    // solo il primo quadretto in alto‑sx
                    if (rows > 0 && colsN > 0) {
                        drawRect(
                            color = Azure.copy(alpha = 0.20f),
                            topLeft = Offset(0f, 0f),
                            size = Size(cell, cell)
                        )
                        drawRect(
                            color = Azure,
                            topLeft = Offset(0f, 0f),
                            size = Size(cell, cell),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                } else if (showFullGrid) {
                    for (c in 0..colsN) {
                        val x = c * cell
                        drawLine(
                            color = Azure.copy(alpha = 0.30f),
                            start = Offset(x, 0f),
                            end = Offset(x, rows * cell),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    for (r in 0..rows) {
                        val y = r * cell
                        drawLine(
                            color = Azure.copy(alpha = 0.30f),
                            start = Offset(0f, y),
                            end = Offset(colsN * cell, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                // (4) Evidenziazione hover (tap singolo) — solo se non in tool mode
                if (creationGate) {
                    hoverCell?.let { (rr, cc) ->
                        if (rr in 0 until rows && cc in 0 until colsN) {
                            drawRect(
                                color = Azure.copy(alpha = 0.18f),
                                topLeft = Offset(cc * cell, rr * cell),
                                size = Size(cell, cell)
                            )
                            drawRect(
                                color = Azure,
                                topLeft = Offset(cc * cell, rr * cell),
                                size = Size(cell, cell),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }

                // (5) Riga/colonna del primo ancoraggio (dopo primo long‑press)
                firstAnchor?.let { (rr, cc) ->
                    if (rr in 0 until rows && cc in 0 until colsN) {
                        // colonna
                        drawRect(
                            color = Azure.copy(alpha = 0.10f),
                            topLeft = Offset(cc * cell, 0f),
                            size = Size(cell, rows * cell)
                        )
                        // riga
                        drawRect(
                            color = Azure.copy(alpha = 0.10f),
                            topLeft = Offset(0f, rr * cell),
                            size = Size(colsN * cell, cell)
                        )
                        // cella più marcata
                        drawRect(
                            color = Azure.copy(alpha = 0.22f),
                            topLeft = Offset(cc * cell, rr * cell),
                            size = Size(cell, cell)
                        )
                        drawRect(
                            color = Azure,
                            topLeft = Offset(cc * cell, rr * cell),
                            size = Size(cell, cell),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }

            // ====== Dialogo decisione linea (split / solo linea) ======
            pendingLineCtx?.let { ctx ->
                AlertDialog(
                    onDismissRequest = { pendingLineCtx = null },
                    title = { Text("Linea e contenitori") },
                    text = {
                        Text("La linea attraversa uno o più contenitori. Vuoi creare nuovi rettangoli dove li divide?")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            // Split rettangoli attraversati
                            ctx.splitCandidates.forEach { rect ->
                                page?.items?.remove(rect)
                                val parts = if (ctx.horizontal) splitRectHoriz(ctx.line.r0, rect)
                                            else                  splitRectVert(ctx.line.c0, rect)
                                page?.items?.addAll(parts)
                            }
                            // Aggiungo la linea sopra (feedback visivo)
                            ctx.line.level = (page?.items?.maxOfOrNull { it.level } ?: currentLevel) + 1
                            onAddItem(ctx.line)
                            pendingLineCtx = null
                        }) { Text("Sì, crea") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            // Solo linea sopra a ciò che incrocia
                            ctx.line.level = ctx.maxOverLevel + 1
                            onAddItem(ctx.line)
                            pendingLineCtx = null
                        }) { Text("No, solo linea") }
                    }
                )
            }

            // ====== Overlay manina + menu strumenti + picker colore ======
            page?.items?.filterIsInstance<DrawItem.RectItem>()?.forEach { rect ->
                val r0 = min(rect.r0, rect.r1)
                val c0 = min(rect.c0, rect.c1)
                val wCells = abs(rect.c1 - rect.c0) + 1
                val hCells = abs(rect.r1 - rect.r0) + 1

                val iconSize = (min(wCells, hCells) * cellDp * 0.25f).coerceIn(16.dp, 28.dp)
                val x = c0 * cellDp
                val y = r0 * cellDp

                // Manina: tap → colore; long‑press → menu strumenti
                IconButton(
                    onClick = {
                        activeRect = rect
                        currentTool = RectTool.Point
                        showColorPicker = true
                        showRectMenu = false
                    },
                    modifier = Modifier
                        .offset(x, y)
                        .size(iconSize)
                        .combinedClickable(
                            onClick = {
                                activeRect = rect
                                currentTool = RectTool.Point
                                showColorPicker = true
                                showRectMenu = false
                            },
                            onLongClick = {
                                activeRect = rect
                                showRectMenu = true
                                showColorPicker = false
                            }
                        )
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.TouchApp,
                        contentDescription = "Modifica contenitore",
                        tint = Color.White
                    )
                }

                // Mini menu strumenti (Point / Grab / Resize / X)
                if (showRectMenu && activeRect === rect) {
                    Surface(
                        color = Color(0xFF0F141E),
                        contentColor = Color.White,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .offset(x + iconSize + 6.dp, y)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = {
                                currentTool = RectTool.Point
                                showColorPicker = true
                                showRectMenu = false
                            }) {
                                Icon(Icons.Outlined.TouchApp, contentDescription = "Proprietà")
                            }
                            IconButton(onClick = {
                                currentTool = RectTool.Grab
                                showColorPicker = false
                                showRectMenu = false
                            }) {
                                Icon(Icons.Outlined.OpenWith, contentDescription = "Sposta")
                            }
                            IconButton(onClick = {
                                currentTool = RectTool.Resize
                                showColorPicker = false
                                showRectMenu = false
                            }) {
                                Icon(Icons.Outlined.AspectRatio, contentDescription = "Ridimensiona")
                            }
                            IconButton(onClick = {
                                currentTool = null
                                activeRect = null
                                showColorPicker = false
                                showRectMenu = false
                            }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Chiudi")
                            }
                        }
                    }
                }

                // Mini picker colore (solo bordo)
                if (showColorPicker && activeRect === rect) {
                    val swatches = listOf(Color.Black, Azure, Color.Red, Color.Green, Color.White)
                    Surface(
                        color = Color(0xFF0F141E),
                        contentColor = Color.White,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .offset(x, y + iconSize + 6.dp)
                    ) {
                        Row(
                            Modifier.padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            swatches.forEach { col ->
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .background(col, shape = CircleShape)
                                        .border(1.dp, Color(0x66FFFFFF), CircleShape)
                                        .combinedClickable(onClick = { rect.borderColor = col })
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun computeCell(ofs: Offset, cols: Int, w: Float, h: Float): Pair<Int, Int> {
    val cell = min(w / cols, h / cols)
    val r = floor(ofs.y / cell).toInt().coerceAtLeast(0)
    val c = floor(ofs.x / cell).toInt().coerceAtLeast(0)
    return r to c
}



// Verifica se la linea orizzontale r==const attraversa "dentro" il rettangolo
private fun crossesInsideHoriz(r: Int, segC0: Int, segC1: Int, rect: DrawItem.RectItem): Boolean {
    // split se la r del segmento è COMPRESA strettamente tra r0..r1
    val crossesInsideRow = r in (min(rect.r0, rect.r1) until max(rect.r0, rect.r1))
    if (!crossesInsideRow) return false
    // e il segmento orizzontale interseca la proiezione colonnare del rettangolo
    val rc0 = min(rect.c0, rect.c1)
    val rc1 = max(rect.c0, rect.c1)
    return !(segC1 < rc0 || segC0 > rc1)
}

// Verifica se la linea verticale c==const attraversa "dentro" il rettangolo
private fun crossesInsideVert(c: Int, segR0: Int, segR1: Int, rect: DrawItem.RectItem): Boolean {
    // split se la c del segmento è COMPRESA strettamente tra c0..c1
    val crossesInsideCol = c in (min(rect.c0, rect.c1) until max(rect.c0, rect.c1))
    if (!crossesInsideCol) return false
    // e il segmento verticale interseca la proiezione riga del rettangolo
    val rr0 = min(rect.r0, rect.r1)
    val rr1 = max(rect.r0, rect.r1)
    return !(segR1 < rr0 || segR0 > rr1)
}

// Dividi rettangolo in due lungo LINEA ORIZZONTALE r
private fun splitRectHoriz(r: Int, rect: DrawItem.RectItem): List<DrawItem.RectItem> {
    val r0 = min(rect.r0, rect.r1)
    val r1 = max(rect.r0, rect.r1)
    val c0 = min(rect.c0, rect.c1)
    val c1 = max(rect.c0, rect.c1)
    // taglio tra r e r+1: parti [r0..r] e [r+1..r1] se valgono
    val top =
        if (r >= r0) DrawItem.RectItem(rect.level, r0, c0, r, c1, rect.borderColor, rect.borderWidth, rect.fillColor) else null
    val bottom =
        if (r + 1 <= r1) DrawItem.RectItem(rect.level, r + 1, c0, r1, c1, rect.borderColor, rect.borderWidth, rect.fillColor) else null
    return listOfNotNull(top, bottom)
}

// Dividi rettangolo in due lungo LINEA VERTICALE c
private fun splitRectVert(c: Int, rect: DrawItem.RectItem): List<DrawItem.RectItem> {
    val r0 = min(rect.r0, rect.r1)
    val r1 = max(rect.r0, rect.r1)
    val c0 = min(rect.c0, rect.c1)
    val c1 = max(rect.c0, rect.c1)
    // taglio tra c e c+1: parti [c0..c] e [c+1..c1] se valgono
    val left  =
        if (c >= c0) DrawItem.RectItem(rect.level, r0, c0, r1, c, rect.borderColor, rect.borderWidth, rect.fillColor) else null
    val right =
        if (c + 1 <= c1) DrawItem.RectItem(rect.level, r0, c + 1, r1, c1, rect.borderColor, rect.borderWidth, rect.fillColor) else null
    return listOfNotNull(left, right)
}

/**
 * Applica le regole richieste per l'inserimento di una LINEA:
 * - Se non crea nuovi rettangoli: la linea va a livello max+1 rispetto a ciò che incrocia
 * - Se crea *solo in parte* nuovi rettangoli: chiedi via dialog (split rettangoli intersected + linea sopra dove serve)
 * - Se crea *solo* nuovi rettangoli: chiedi via dialog (split rettangoli, senza aggiungere la linea)
 *
 * Per semplicità: uso un piccolo dialogo Compose “inline” che vive finché pending!=null.
 */
@Composable
private fun resolveLineInsert(
    page: PageState?,
    line: DrawItem.LineItem,
    currentLevel: Int,
    onAddItem: (DrawItem) -> Unit
) {
    if (page == null) return

    // 1) trova rettangoli coinvolti
    val rects = page.items.filterIsInstance<DrawItem.RectItem>()
    val (segR0, segC0, segR1, segC1) = listOf(line.r0, line.c0, line.r1, line.c1)
    val horiz = (line.r0 == line.r1)

    val overlapped = rects.filter { rect ->
        val r0 = min(rect.r0, rect.r1); val r1 = max(rect.r0, rect.r1)
        val c0 = min(rect.c0, rect.c1); val c1 = max(rect.c0, rect.c1)
        if (horiz) {
            // intersezione segmento [c0..c1] a r=fisso con area del rect
            (segR0 in r0..r1) && !(segC1 < c0 || segC0 > c1)
        } else {
            // intersezione segmento [r0..r1] a c=fisso con area del rect
            (segC0 in c0..c1) && !(segR1 < r0 || segR0 > r1)
        }
    }

    val splitCandidates = overlapped.filter { rect ->
        if (horiz) crossesInsideHoriz(segR0, min(segC0,segC1), max(segC0,segC1), rect)
        else       crossesInsideVert(segC0, min(segR0,segR1), max(segR0,segR1), rect)
    }

    val hasSplit = splitCandidates.isNotEmpty()
    val maxOverLevel = overlapped.maxOfOrNull { it.level } ?: currentLevel

    // Stato per il piccolo dialogo
    var ask by remember { mutableStateOf(hasSplit) }

    // Caso 1: nessuno split possibile → aggiungi linea a livello superiore
    if (!hasSplit) {
        line.level = maxOverLevel + 1
        onAddItem(line)
        return
    }

    // Caso 2/3: chiedo cosa fare (split sì/no)
    if (ask) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { ask = false },
            title = { androidx.compose.material3.Text("Linea e contenitori") },
            text  = {
                androidx.compose.material3.Text(
                    "La linea attraversa uno o più contenitori. Vuoi creare nuovi rettangoli dove li divide?"
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    // SPLIT: per ogni rettangolo candidato, lo rimpiazzo con due
                    splitCandidates.forEach { rect ->
                        page.items.remove(rect)
                        val parts = if (horiz) splitRectHoriz(segR0, rect) else splitRectVert(segC0, rect)
                        page.items.addAll(parts)
                    }
                    // Dopo lo split, per aderenza alle regole:
                    // - se la linea ha diviso SOLO in parte, la manteniamo sopra per le parti a “vuoto”
                    // - se ha diviso solo rettangoli, *posso* anche non aggiungerla; teniamola sopra a livello max+1: è innocua
                    line.level = (page.items.maxOfOrNull { it.level } ?: currentLevel) + 1
                    onAddItem(line)

                    ask = false
                }) { androidx.compose.material3.Text("Sì, crea") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    // NO SPLIT: la linea va davanti a tutto ciò che incrocia
                    line.level = maxOverLevel + 1
                    onAddItem(line)
                    ask = false
                }) { androidx.compose.material3.Text("No, solo linea") }
            }
        )
    }
}

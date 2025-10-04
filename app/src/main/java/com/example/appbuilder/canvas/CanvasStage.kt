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
    // Evidenziazione singola cella (tap)
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // Primo estremo selezionato (long press)
    var firstAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val cols = max(1, gridDensity)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cols, creationEnabled) {
                detectTapGestures(
                    onTap = { ofs ->
                        if (!creationEnabled) return@detectTapGestures
                        val cell = computeCell(
                            ofs, cols,
                            this.size.width.toFloat(), this.size.height.toFloat()
                        )
                        hoverCell = cell
                    },
                    onLongPress = { ofs ->
                        if (!creationEnabled) return@detectTapGestures
                        val (r, c) = computeCell(
                            ofs, cols,
                            this.size.width.toFloat(), this.size.height.toFloat()
                        )

                        if (firstAnchor == null) {
                            firstAnchor = r to c
                        } else {
                            val (r0, c0) = firstAnchor!!
                            val rr0 = min(r0, r)
                            val rr1 = max(r0, r)
                            val cc0 = min(c0, c)
                            val cc1 = max(c0, c)

                            if (r0 == r || c0 == c) {
                                // ===== LINEA (stessa riga o stessa colonna) =====
                                val line = if (r0 == r) {
                                    // Orizzontale: unisce i lati interni (più vicini al centro)
                                    DrawItem.LineItem(
                                        level = currentLevel,
                                        r0 = r0, c0 = cc0,
                                        r1 = r0, c1 = cc1,
                                        color = Color(0xFF58A6FF),
                                        width = 2.dp
                                    )
                                } else {
                                    // Verticale: unisce i lati interni (più vicini al centro)
                                    DrawItem.LineItem(
                                        level = currentLevel,
                                        r0 = rr0, c0 = c0,
                                        r1 = rr1, c1 = c0,
                                        color = Color(0xFF58A6FF),
                                        width = 2.dp
                                    )
                                }

                                // Valuta incroci: se non ci sono split → linea sopra a tutto ciò che incrocia
                                val rects = page?.items?.filterIsInstance<DrawItem.RectItem>().orEmpty()
                                val overlapped = rects.filter { rect ->
                                    val R0 = min(rect.r0, rect.r1); val R1 = max(rect.r0, rect.r1)
                                    val C0 = min(rect.c0, rect.c1); val C1 = max(rect.c0, rect.c1)
                                    if (r0 == r) {
                                        (line.r0 in R0..R1) && !(line.c1 < C0 || line.c0 > C1)
                                    } else {
                                        (line.c0 in C0..C1) && !(line.r1 < R0 || line.r0 > R1)
                                    }
                                }
                                val splitCandidates = overlapped.filter { rect ->
                                    if (r0 == r) crossesInsideHoriz(line.r0, min(line.c0,line.c1), max(line.c0,line.c1), rect)
                                    else         crossesInsideVert(line.c0, min(line.r0,line.r1), max(line.r0,line.r1), rect)
                                }
                                val maxOverLevel = overlapped.maxOfOrNull { it.level } ?: currentLevel

                                if (splitCandidates.isEmpty()) {
                                    // Nessuno split → la linea va sopra a ciò che incrocia
                                    onAddItem(line.copy(level = maxOverLevel + 1))
                                } else {
                                    // Semplificazione: chiedere scelta (dialog in EditorKit o in uno step successivo).
                                    // Per ora: crea split e poi aggiungi la linea sopra.
                                    splitCandidates.forEach { rect ->
                                        page?.items?.remove(rect)
                                        val parts = if (r0 == r) splitRectHoriz(line.r0, rect)
                                                    else           splitRectVert(line.c0, rect)
                                        page?.items?.addAll(parts)
                                    }
                                    val newLevel = (page?.items?.maxOfOrNull { it.level } ?: currentLevel) + 1
                                    onAddItem(line.copy(level = newLevel))
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

            // (1) Elementi
            page?.items?.forEach { item ->
                if (item.level <= currentLevel) {
                    when (item) {
                        is DrawItem.RectItem -> {
                            val left = min(item.c0, item.c1) * cell
                            val top  = min(item.r0, item.r1) * cell
                            val w = (abs(item.c1 - item.c0) + 1) * cell
                            val h = (abs(item.r1 - item.r0) + 1) * cell
                            // riempimento
                            drawRect(color = item.fillColor, topLeft = Offset(left, top), size = Size(w, h))
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
                                end   = Offset(x1, y1),
                                strokeWidth = item.width.toPx()
                            )
                        }
                    }
                }
            }

            // (2) Griglia
            if (gridPreviewOnly) {
                if (rows > 0 && cols > 0) {
                    drawRect(
                        color = Color(0xFF58A6FF).copy(alpha = 0.20f),
                        topLeft = Offset(0f, 0f),
                        size = Size(cell, cell)
                    )
                    drawRect(
                        color = Color(0xFF58A6FF),
                        topLeft = Offset(0f, 0f),
                        size = Size(cell, cell),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            } else if (showFullGrid) {
                for (c in 0..cols) {
                    val x = c * cell
                    drawLine(
                        color = Color(0xFF58A6FF).copy(alpha = 0.30f),
                        start = Offset(x, 0f),
                        end = Offset(x, rows * cell),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                for (r in 0..rows) {
                    val y = r * cell
                    drawLine(
                        color = Color(0xFF58A6FF).copy(alpha = 0.30f),
                        start = Offset(0f, y),
                        end = Offset(cols * cell, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // (3) Hover cell
            hoverCell?.let { (rr, cc) ->
                if (rr in 0 until rows && cc in 0 until cols) {
                    drawRect(
                        color = Color(0xFF58A6FF).copy(alpha = 0.18f),
                        topLeft = Offset(cc * cell, rr * cell),
                        size = Size(cell, cell)
                    )
                    drawRect(
                        color = Color(0xFF58A6FF),
                        topLeft = Offset(cc * cell, rr * cell),
                        size = Size(cell, cell),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // (4) Riga/colonna del primo ancoraggio
            firstAnchor?.let { (rr, cc) ->
                if (rr in 0 until rows && cc in 0 until cols) {
                    drawRect(
                        color = Color(0xFF58A6FF).copy(alpha = 0.10f),
                        topLeft = Offset(cc * cell, 0f),
                        size = Size(cell, rows * cell)
                    )
                    drawRect(
                        color = Color(0xFF58A6FF).copy(alpha = 0.10f),
                        topLeft = Offset(0f, rr * cell),
                        size = Size(cols * cell, cell)
                    )
                    drawRect(
                        color = Color(0xFF58A6FF).copy(alpha = 0.22f),
                        topLeft = Offset(cc * cell, rr * cell),
                        size = Size(cell, cell)
                    )
                    drawRect(
                        color = Color(0xFF58A6FF),
                        topLeft = Offset(cc * cell, rr * cell),
                        size = Size(cell, cell),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
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

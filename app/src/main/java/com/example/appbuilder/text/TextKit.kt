package com.example.appbuilder.text

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset // uso standard (foundation.layout)
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appbuilder.canvas.DrawItem
import com.example.appbuilder.canvas.PageState
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import java.util.UUID

/**
 * Motore “leggero” per i testi: conserva nodi, caret e associazione al contenitore.
 */
class TextEngine {
    data class TextNode(
        val id: String = UUID.randomUUID().toString(),
        var owner: DrawItem.RectItem?,   // null = testo su pagina
        var posPx: Offset,               // posizione assoluta in px nello spazio del Canvas
        var text: String = ""
    )


    // Stato osservabile
    val nodes = mutableStateListOf<TextNode>()
    var active: TextNode? by mutableStateOf(null)

    // Ambiente per conversioni (aggiornato dalla layer)
    var lastCanvasSize by mutableStateOf(IntSize.Zero)
        private set
    var gridCols by mutableStateOf(6)
        private set
    fun activate(node: TextNode?) { active = node }
    fun isActive(node: TextNode): Boolean = active === node
    fun updateEnv(size: IntSize, cols: Int) {
        lastCanvasSize = size
        gridCols = max(1, cols)
    }

    private fun cellSize(): Float {
        val w = lastCanvasSize.width.toFloat()
        val h = lastCanvasSize.height.toFloat()
        return min(w / gridCols, h / gridCols)
    }

    private fun rectTopLeftPx(r: DrawItem.RectItem): Offset {
        val cell = cellSize()
        val left = min(r.c0, r.c1) * cell
        val top  = min(r.r0, r.r1) * cell
        return Offset(left, top)
    }

    /** Quando un rect viene sostituito (drag/resize), migro owner e posizione relativa. */
    fun onRectReplaced(old: DrawItem.RectItem, updated: DrawItem.RectItem) {
        val delta = rectTopLeftPx(updated) - rectTopLeftPx(old)
        nodes.filter { it.owner === old }.forEach { n ->
            n.owner = updated
            n.posPx = n.posPx + delta
            if (active === n) active = n // notifica recomposition
        }
    }

    /** Posiziona il caret: se vicino a un nodo esistente lo seleziona, altrimenti ne crea uno. */
    fun placeCaret(owner: DrawItem.RectItem?, tapPx: Offset, nearPx: Float) {
        val nearest = nodes.minByOrNull { (it.posPx - tapPx).getDistance() }
        val picked = if (nearest != null && (nearest.posPx - tapPx).getDistance() <= nearPx) {
            nearest
        } else {
            TextNode(owner = owner, posPx = tapPx).also { nodes.add(it) }
        }
        active = picked
    }

    fun updateText(newValue: String) { active?.text = newValue }
}

/**
 * Layer testo sovrapposto: attivo SOLO quando il menù "Testo" è aperto.
 * - Mostra caret nel punto di tap.
 * - Apre la tastiera solo su gesto utente.
 * - Mantiene il caret visibile sopra la tastiera usando bottomSafePx.
 */
@Composable
fun TextLayer(
    active: Boolean,
    page: PageState?,
    engine: TextEngine,
    bottomSafePx: Int = 0
) {
    if (!active) return

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val kb = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    // Aggiorno ambiente per hit‑test e conversioni
    LaunchedEffect(canvasSize, page?.gridDensity) {
        engine.updateEnv(canvasSize, page?.gridDensity ?: 6)
    }

    // Valore “live” del nodo attivo
    val textValue by remember(engine.active) {
        derivedStateOf { engine.active?.text.orEmpty() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(page?.items, page?.gridDensity, canvasSize) {
                detectTapGestures(onTap = { ofs ->
                    val owner = hitTestRectAt(ofs, page, canvasSize)
                    val nearPx = with(density) { 24.dp.toPx() } // soglia vicinanza caret
                    engine.placeCaret(owner, ofs, nearPx)
                    kb?.show() // tastiera SOLO su gesto utente
                })
            }
    ) {
        // Nodo attivo: campo “flottante” ancorato alla posizione
        engine.active?.let { node ->
            val caretMargin = with(density) { 32.dp.toPx() }
            val safeY = min(
                node.posPx.y,
                (canvasSize.height - bottomSafePx).toFloat() - caretMargin
            ).coerceAtLeast(0f)

            val focusReq = remember { FocusRequester() }

            BasicTextField(
                value = textValue,
                onValueChange = { engine.updateText(it) },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                cursorBrush = SolidColor(Color.Black),
                modifier = Modifier
                    .offset { IntOffset(node.posPx.x.roundToInt(), safeY.roundToInt()) }
                    .focusRequester(focusReq)
                    .onFocusChanged { st ->
                        if (st.isFocused) kb?.show()
                    }
            )
            // porta il focus quando cambia l’attivo -> mostra caret
            LaunchedEffect(node.id) { focusReq.requestFocus() }
        }

        // Nodi non attivi: render statico + tap per riselezionare
        engine.nodes.forEach { n ->
            if (engine.active?.id == n.id) return@forEach
            BasicText(
                text = n.text,
                style = TextStyle(fontSize = 16.sp, color = Color.Black),
                modifier = Modifier
                    .offset { IntOffset(n.posPx.x.roundToInt(), n.posPx.y.roundToInt()) }
                    .pointerInput(n.id) {
                        detectTapGestures {
                            engine.activate(n)
                            kb?.show()
                        }
                    }
            )
        }
    }
}

/* ------------------------- helpers privati ------------------------- */

private fun hitTestRectAt(
    ofs: Offset,
    page: PageState?,
    size: IntSize
): DrawItem.RectItem? {
    val cols = max(1, page?.gridDensity ?: 6)
    val cell = min(size.width.toFloat() / cols, size.height.toFloat() / cols)
    if (cell <= 0f || page == null) return null

    fun bounds(r: DrawItem.RectItem): IntArray {
        val r0 = min(r.r0, r.r1); val r1 = max(r.r0, r.r1)
        val c0 = min(r.c0, r.c1); val c1 = max(r.c0, r.c1)
        return intArrayOf(r0, r1, c0, c1)
    }
    fun contains(r: DrawItem.RectItem, row: Int, col: Int): Boolean {
        val (r0, r1, c0, c1) = bounds(r)
        return row in r0..r1 && col in c0..c1
    }
    fun computeCell(ofs: Offset): Pair<Int, Int> {
        val r = floor(ofs.y / cell).toInt().coerceAtLeast(0)
        val c = floor(ofs.x / cell).toInt().coerceAtLeast(0)
        return r to c
    }

    val (rr, cc) = computeCell(ofs)
    return page.items
        .filterIsInstance<DrawItem.RectItem>()
        .filter { contains(it, rr, cc) }
        .maxByOrNull { it.level }
}

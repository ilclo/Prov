package com.example.appbuilder.text

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.*
import java.util.UUID
import androidx.compose.ui.layout.layout

/**
 * Motore “leggero” per i testi: conserva nodi (caret), owner (rect/pagina) e posizione in px.
 */
class TextEngine {
    data class TextNode(
        val id: String = UUID.randomUUID().toString(),
        var owner: DrawItem.RectItem?,   // null = testo su pagina
        var posPx: Offset,               // posizione assoluta in px nello spazio Canvas
        var text: String = ""
    )

    // Stato osservabile
    val nodes = mutableStateListOf<TextNode>()
    var active by mutableStateOf<TextNode?>(null)
        private set

    // Ambiente per conversioni (aggiornato dal layer)
    var lastCanvasSize by mutableStateOf(IntSize.Zero)
        private set
    var gridCols by mutableStateOf(6)
        private set

    fun updateEnv(size: IntSize, cols: Int) {
        lastCanvasSize = size
        gridCols = cols.coerceAtLeast(1)
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

    /** Quando un rect viene “rimpiazzato” (spostato/ridimensionato), migra owner e posizione relativa. */
    fun onRectReplaced(old: DrawItem.RectItem, updated: DrawItem.RectItem) {
        val delta = rectTopLeftPx(updated) - rectTopLeftPx(old)
        nodes.filter { it.owner === old }.forEach { n ->
            n.owner = updated
            n.posPx = n.posPx + delta
            if (active === n) active = n // notifica recomposition
        }
    }

    /** Posiziona il caret: se vicino a un nodo esistente lo seleziona, altrimenti ne crea uno nuovo. */
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
 * Overlay testo: attivo **solo** quando il menù "Testo" è aperto.
 * - Tap: crea/seleziona caret e apre la tastiera
 * - Il caret appartiene al rettangolo top-most che contiene la cella tappata (o alla pagina)
 */
@Composable
fun TextLayer(
    active: Boolean,
    page: PageState?,
    engine: TextEngine
) {
    if (!active) return

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val focusReq = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    val textValue by remember(engine.active) {
        derivedStateOf { engine.active?.text.orEmpty() }
    }

    // Tieni in sync ambiente
    LaunchedEffect(canvasSize, page?.gridDensity) {
        engine.updateEnv(canvasSize, page?.gridDensity ?: 6)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(page?.items, page?.gridDensity, canvasSize) {
                detectTapGestures(onTap = { ofs ->
                    val owner = hitTestRectAt(ofs, page, canvasSize)
                    // soglia "vicinanza" ~24dp
                    val nearPx = with(density) { 24.dp.toPx() }
                    engine.placeCaret(owner, ofs, nearPx)
                    kb?.show()
                })
            }
    ) {
        // Campo “flottante” all’ancora del caret
        val pos = engine.active?.posPx
        if (pos != null) {
            BasicTextField(
                value = textValue,
                onValueChange = { engine.updateText(it) },
                textStyle = TextStyle(fontSize = 16.sp),
                cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Black),
                modifier = Modifier
                    // NOTE: usa l’offset “pixel” (lambda -> IntOffset) della UI layout API
                    .offsetPx { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                    .focusRequester(focusReq)
                    .onFocusChanged { state ->
                        if (state.isFocused) kb?.show()
                    }
            )
            // Richiedi focus quando cambia il nodo attivo
            LaunchedEffect(engine.active?.id) { focusReq.requestFocus() }
        }
    }
}

// Offset "in pixel" compatibile con tutte le versioni di Compose
private fun Modifier.offsetPx(provider: () -> IntOffset): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val (ox, oy) = provider()
        layout(placeable.width, placeable.height) {
            // Usa placeRelative per rispettare LTR/RTL
            placeable.placeRelative(ox, oy)
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

    fun rectBounds(r: DrawItem.RectItem): IntArray {
        val r0 = min(r.r0, r.r1); val r1 = max(r.r0, r.r1)
        val c0 = min(r.c0, r.c1); val c1 = max(r.c0, r.c1)
        return intArrayOf(r0, r1, c0, c1)
    }
    fun containsCell(r: DrawItem.RectItem, row: Int, col: Int): Boolean {
        val (r0, r1, c0, c1) = rectBounds(r)
        return row in r0..r1 && col in c0..c1
    }
    fun computeCell(ofs: Offset): Pair<Int, Int> {
        val row = floor(ofs.y / cell).toInt().coerceAtLeast(0)
        val col = floor(ofs.x / cell).toInt().coerceAtLeast(0)
        return row to col
    }

    val (row, col) = computeCell(ofs)

    // Scorri i rettangoli top-most per trovare il primo che contiene la cella
    val rects = page.items.filterIsInstance<DrawItem.RectItem>()
    for (r in rects.asReversed()) { // top-most first
        if (containsCell(r, row, col)) return r
    }
    return null
}

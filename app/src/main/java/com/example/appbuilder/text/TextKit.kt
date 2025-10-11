package com.example.appbuilder.text

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset   // usa SOLO foundation.layout.offset
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
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
 * Motore testuale leggero: blocchi testo “flottanti” associabili a pagina o contenitore.
 * Ogni blocco usa TextFieldValue → caret/selection nativi (copy/paste/select-all via toolbar di sistema).
 */
class TextEngine {

    class TextNode(
        val id: String = UUID.randomUUID().toString(),
        var owner: DrawItem.RectItem?,     // null = testo su pagina
        var posPx: Offset                  // posizione assoluta nel Canvas
    ) {
        var value by mutableStateOf(TextFieldValue(""))
        // ultimi size di layout (per hit-test “dentro” al testo)
        var layoutW by mutableStateOf(0)
        var layoutH by mutableStateOf(0)
    }

    // Stato osservabile
    val nodes = mutableStateListOf<TextNode>()
    var active: TextNode? by mutableStateOf(null)

    // Ambiente Canvas (per hit-test cella/rect)
    private var lastCanvasSize: IntSize = IntSize.Zero
    private var gridCols: Int = 6
    fun updateEnv(size: IntSize, cols: Int) { lastCanvasSize = size; gridCols = max(1, cols) }

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

    /** Migra owner/posizione quando un Rect viene rimpiazzato (drag/resize). */
    fun onRectReplaced(old: DrawItem.RectItem, updated: DrawItem.RectItem) {
        val delta = rectTopLeftPx(updated) - rectTopLeftPx(old)
        nodes.filter { it.owner === old }.forEach { n ->
            n.owner = updated
            n.posPx = n.posPx + delta
            if (active === n) active = n // notifica recomposition
        }
    }

    /** Ritorna il nodo “cliccato” (se il tap cade nel suo box testo) o il più vicino entro soglia. */
    private fun pickNodeForTap(tapPx: Offset, nearPx: Float): TextNode? {
        // 1) dentro il box del testo (con un piccolo margine)
        val boxMargin = 8f
        nodes.firstOrNull { n ->
            val left = n.posPx.x - boxMargin
            val top = n.posPx.y - boxMargin
            val right = n.posPx.x + n.layoutW + boxMargin
            val bottom = n.posPx.y + n.layoutH + boxMargin
            tapPx.x in left..right && tapPx.y in top..bottom
        }?.let { return it }

        // 2) il più vicino alla sua origine entro nearPx
        return nodes.minByOrNull { (it.posPx - tapPx).getDistance() }?.takeIf {
            (it.posPx - tapPx).getDistance() <= nearPx
        }
    }

    /**
     * Posiziona il caret: se tocchi un testo esistente lo attiva, altrimenti crea un nuovo blocco.
     * Ritorna il nodo attivo dopo il tap.
     */
    fun placeCaret(owner: DrawItem.RectItem?, tapPx: Offset, nearPx: Float): TextNode {
        val picked = pickNodeForTap(tapPx, nearPx)
            ?: TextNode(owner = owner, posPx = tapPx).also { nodes.add(it) }
        active = picked
        return picked
    }

    fun replaceActiveValue(newValue: TextFieldValue) { active?.value = newValue }
}

/* -------------------------------------------------------------------------------------- */

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

    // aggiorna ambiente di hit-test su cambi dimensioni/griglia
    LaunchedEffect(canvasSize, page?.gridDensity) {
        engine.updateEnv(canvasSize, page?.gridDensity ?: 6)
    }

    // tap → attiva/crea blocco; nessun long-press/drag qui: li gestisce BasicTextField (selection nativa)
    var pendingTap by remember { mutableStateOf<Offset?>(null) } // per posizionare il caret dentro il testo

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(page?.items, page?.gridDensity, canvasSize) {
                detectTapGestures(
                    onTap = { ofs ->
                        val owner = hitTestRectAt(ofs, page, canvasSize)
                        val nearPx = with(density) { 24.dp.toPx() }
                        val node = engine.placeCaret(owner, ofs, nearPx)
                        pendingTap = ofs           // posizionamento caret tra le lettere
                        kb?.show()                 // tastiera SOLO su gesto utente
                    }
                )
            }
    ) {
        // Nodo attivo: campo editabile “flottante” (caret visibile sopra la tastiera)
        engine.active?.let { node ->
            val caretMargin = with(density) { 32.dp.toPx() }
            val safeY = min(
                node.posPx.y,
                (canvasSize.height - bottomSafePx).toFloat() - caretMargin
            ).coerceAtLeast(0f)

            val focusReq = remember { FocusRequester() }
            var layout by remember(node.id) { mutableStateOf<TextLayoutResult?>(null) }

            BasicTextField(
                value = node.value,
                onValueChange = { v -> engine.replaceActiveValue(v) },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                cursorBrush = SolidColor(Color.Black),
                modifier = Modifier
                    .offset { IntOffset(node.posPx.x.roundToInt(), safeY.roundToInt()) }
                    .focusRequester(focusReq)
                    .onFocusChanged { st -> if (st.isFocused) kb?.show() },
                onTextLayout = { tlr ->
                    layout = tlr
                    // aggiorna box di hit-test (serve per capire se un prossimo tap cade “dentro”)
                    node.layoutW = tlr.size.width
                    node.layoutH = tlr.size.height
                }
            )
            // Porta il focus quando cambia l’attivo → caret lampeggiante + tastiera
            LaunchedEffect(node.id) { focusReq.requestFocus() }

            // Se ho un tap “sopra” questo testo, posiziono il caret tra le lettere
            LaunchedEffect(pendingTap, layout, node.posPx, node.value.text) {
                val p = pendingTap ?: return@LaunchedEffect
                val tlr = layout ?: return@LaunchedEffect
                // coordinate relative al testo
                val rel = p - node.posPx
                val y = rel.y.coerceIn(0f, (tlr.size.height - 1).toFloat())
                val x = rel.x.coerceIn(0f, (tlr.size.width  - 1).toFloat())
                val caret = tlr.getOffsetForPosition(Offset(x, y))
                engine.replaceActiveValue(
                    node.value.copy(selection = TextRange(caret))
                )
                pendingTap = null
            }
        }

        // Nodi NON attivi: rendering statico + tap singolo per riattivare (caret nella parola cliccata)
        engine.nodes.forEach { n ->
            if (engine.active?.id == n.id) return@forEach
            BasicText(
                text = n.value.text,
                style = TextStyle(fontSize = 16.sp, color = Color.Black),
                modifier = Modifier
                    .offset { IntOffset(n.posPx.x.roundToInt(), n.posPx.y.roundToInt()) }
                    .pointerInput(n.id) {
                        detectTapGestures(
                            onTap = { localTap ->
                                // converte il tap locale (nel BasicText) in coordinate Canvas
                                val canvasTap = Offset(n.posPx.x + localTap.x, n.posPx.y + localTap.y)
                                engine.active = n          // continua a scrivere nello stesso blocco
                                pendingTap = canvasTap     // caret preciso tra le lettere
                                kb?.show()
                            }
                        )
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

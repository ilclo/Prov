package com.example.appbuilder.text

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset // <-- usa SOLO questo offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
/* =========================================================================================
 * MOTORE TESTO
 * ========================================================================================= */
class TextEngine {

    class TextNode(
        val id: String = UUID.randomUUID().toString(),
        var owner: DrawItem.RectItem?,     // null = testo libero su pagina
        var posPx: Offset                  // posizione assoluta nel Canvas
    ) {
        var value by mutableStateOf(TextFieldValue(""))
        // info layout per hit-test
        var layoutW by mutableStateOf(0)
        var layoutH by mutableStateOf(0)
    }

    val nodes = mutableStateListOf<TextNode>()
    var active: TextNode? by mutableStateOf(null)

    // ambiente Canvas (x hit-test contenitori)
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

    /** Migra owner/posizioni quando un Rect viene rimpiazzato (drag/resize). */
    fun onRectReplaced(old: DrawItem.RectItem, updated: DrawItem.RectItem) {
        val delta = rectTopLeftPx(updated) - rectTopLeftPx(old)
        nodes.filter { it.owner === old }.forEach { n ->
            n.owner = updated
            n.posPx = n.posPx + delta
            if (active === n) active = n
        }
    }

    /** Nodo “cliccato” o il più vicino entro soglia. */
    private fun pickNodeForTap(tapPx: Offset, nearPx: Float): TextNode? {
        // dentro al box del testo (margine piccolo)
        val boxMargin = 8f
        nodes.firstOrNull { n ->
            val left = n.posPx.x - boxMargin
            val top = n.posPx.y - boxMargin
            val right = n.posPx.x + n.layoutW + boxMargin
            val bottom = n.posPx.y + n.layoutH + boxMargin
            tapPx.x in left..right && tapPx.y in top..bottom
        }?.let { return it }

        // altrimenti il più vicino all'origine
        return nodes.minByOrNull { (it.posPx - tapPx).getDistance() }?.takeIf {
            (it.posPx - tapPx).getDistance() <= nearPx
        }
    }

    /** Tap: attiva un blocco esistente o ne crea uno nuovo. */
    fun placeCaret(owner: DrawItem.RectItem?, tapPx: Offset, nearPx: Float): TextNode {
        val picked = pickNodeForTap(tapPx, nearPx)
            ?: TextNode(owner = owner, posPx = tapPx).also { nodes.add(it) }
        active = picked
        return picked
    }

    fun replaceActiveValue(newValue: TextFieldValue) { active?.value = newValue }
}

/* =========================================================================================
 * LAYER TESTO
 * ========================================================================================= */
@Composable
fun TextLayer(
    editEnabled: Boolean,          // <— prima si chiamava "active": ora indica SOLO se l'editor testo è aperto
    page: PageState?,
    engine: TextEngine,
    bottomSafePx: Int = 0          // spazio IME per il caret (in px), NON usato per lo scroll
) {
    // NIENTE early return: i testi vanno disegnati anche a editor chiuso

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val kb = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val clipboard = LocalClipboardManager.current

    // Nasconde la toolbar di sistema: usiamo la barra nera custom
    val emptyToolbar = remember {
        object : TextToolbar {
            override val status: TextToolbarStatus get() = TextToolbarStatus.Hidden
            override fun showMenu(
                rect: androidx.compose.ui.geometry.Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?,
            ) { /* no-op */ }
            override fun hide() {}
        }
    }

    // Aggiorna l’ambiente di hit-test (dimensioni canvas / densità griglia)
    LaunchedEffect(canvasSize, page?.gridDensity) {
        engine.updateEnv(canvasSize, page?.gridDensity ?: 6)
    }

    // Tap “deferred” per posizionare il caret tra le lettere
    var pendingTap by remember { mutableStateOf<Offset?>(null) }

    // Stato barra nera
    var showTextBar by remember { mutableStateOf(false) }
    var textBarAnchor by remember { mutableStateOf(IntOffset.Zero) }

    CompositionLocalProvider(LocalTextToolbar provides emptyToolbar) {
        // Installa gesture solo quando si può editare
        val gestureMod = if (editEnabled) {
            Modifier.pointerInput(page?.items, page?.gridDensity, canvasSize) {
                detectTapGestures(
                    onTap = { ofs ->
                        val owner = hitTestRectAt(ofs, page, canvasSize)
                        val nearPx = with(density) { 24.dp.toPx() }
                        val node = engine.placeCaret(owner, ofs, nearPx)
                        pendingTap = ofs
                        showTextBar = false
                        kb?.show()
                    }
                    // NIENTE drag/long-press qui: selezione & drag li gestisce il BasicTextField
                )
            }
        } else Modifier

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .then(gestureMod)
        ) {
            // 1) Campo editabile “flottante”: SOLO quando l’editor è aperto
            if (editEnabled) {
                engine.active?.let { node ->
                    val caretMargin = with(density) { 32.dp.toPx() }
                    val safeY = min(
                        node.posPx.y,
                        (canvasSize.height - bottomSafePx).toFloat() - caretMargin
                    ).coerceAtLeast(0f)

                    val focusReq = remember { FocusRequester() }
                    var layout by remember(node.id) { mutableStateOf<TextLayoutResult?>(null) }

                    // Mostra barra nera se c’è selezione non-collassata
                    LaunchedEffect(node.value.selection, layout) {
                        val sel = node.value.selection
                        if (layout != null && !sel.collapsed) {
                            val box = layout!!.getBoundingBox(sel.min.coerceIn(0, node.value.text.length))
                            val anchorX = node.posPx.x + box.left
                            val anchorY = safeY + box.top
                            val barH = with(density) { 38.dp.toPx() }
                            textBarAnchor = IntOffset(
                                anchorX.roundToInt(),
                                (anchorY - barH - with(density){8.dp.toPx()}).roundToInt()
                            )
                            showTextBar = true
                        } else {
                            showTextBar = false
                        }
                    }

                    BasicTextField(
                        value = node.value,
                        onValueChange = { v ->
                            engine.replaceActiveValue(v)
                            if (showTextBar && v.selection.collapsed) showTextBar = false
                        },
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                        cursorBrush = SolidColor(Color.Black),
                        modifier = Modifier
                            .offset { IntOffset(node.posPx.x.roundToInt(), safeY.roundToInt()) }
                            .focusRequester(focusReq)
                            .onFocusChanged { st -> if (st.isFocused) kb?.show() },
                        onTextLayout = { tlr ->
                            layout = tlr
                            node.layoutW = tlr.size.width
                            node.layoutH = tlr.size.height
                        }
                    )
                    // Focus sul nodo attivo
                    LaunchedEffect(node.id) { focusReq.requestFocus(); kb?.show() }

                    // Posiziona il caret esattamente dove hai toccato (tap singolo)
                    LaunchedEffect(pendingTap, layout, node.posPx, node.value.text) {
                        val p = pendingTap ?: return@LaunchedEffect
                        val tlr = layout ?: return@LaunchedEffect
                        val relX = (p.x - node.posPx.x).coerceIn(0f, (tlr.size.width  - 1).toFloat())
                        val relY = (p.y - safeY).coerceIn(0f, (tlr.size.height - 1).toFloat())
                        val caret = tlr.getOffsetForPosition(Offset(relX, relY))
                        engine.replaceActiveValue(node.value.copy(selection = TextRange(caret)))
                        pendingTap = null
                    }

                    // Barra nera stile GitHub
                    TextContextBar(
                        visible = editEnabled && showTextBar,
                        anchor = textBarAnchor,
                        onCopy = {
                            val sel = node.value.selection
                            if (!sel.collapsed) {
                                val txt = node.value.text.substring(sel.min, sel.max)
                                clipboard.setText(AnnotatedString(txt))
                            }
                            showTextBar = false
                        },
                        onSelectAll = {
                            engine.replaceActiveValue(
                                node.value.copy(selection = TextRange(0, node.value.text.length))
                            )
                        },
                        onPaste = {
                            val paste = clipboard.getText()?.text.orEmpty()
                            if (paste.isNotEmpty()) {
                                val sel = node.value.selection
                                val t = node.value.text
                                val start = sel.min.coerceIn(0, t.length)
                                val end = sel.max.coerceIn(0, t.length)
                                val newText = t.substring(0, start) + paste + t.substring(end)
                                engine.replaceActiveValue(
                                    TextFieldValue(text = newText, selection = TextRange(start + paste.length))
                                )
                            }
                            showTextBar = false
                        },
                        onDismiss = { showTextBar = false }
                    )
                }
            }

            // 2) Rendering STATICO: SEMPRE visibile (anche a editor chiuso)
            engine.nodes.forEach { n ->
                val isActiveRenderedAsField = editEnabled && (engine.active?.id == n.id)
                if (!isActiveRenderedAsField) {
                    val tapMod = if (editEnabled) {
                        Modifier.pointerInput(n.id) {
                            detectTapGestures(
                                onTap = { localTap ->
                                    // coord canvas + caret preciso
                                    val canvasTap = Offset(n.posPx.x + localTap.x, n.posPx.y + localTap.y)
                                    engine.active = n
                                    pendingTap = canvasTap
                                    kb?.show()
                                }
                            )
                        }
                    } else Modifier

                    BasicText(
                        text = n.value.text,
                        style = TextStyle(fontSize = 16.sp, color = Color.Black),
                        modifier = Modifier
                            .offset { IntOffset(n.posPx.x.roundToInt(), n.posPx.y.roundToInt()) }
                            .then(tapMod)
                    )
                }
            }
        }
    }
}


/* ---------- Toolbar nera “GitHub-like” ---------- */
@Composable
private fun TextContextBar(
    visible: Boolean,
    anchor: IntOffset,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onPaste: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    Box(Modifier.fillMaxSize()) {
        // chiusura su tap fuori
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
        )
        // barra
        Box(
            Modifier
                .offset { anchor }
                .pointerInput(Unit) { detectTapGestures(onTap = { /* assorbi */ }) }
        ) {
            Surface(
                color = Color(0xFF0D1117),
                contentColor = Color.White,
                shape = RoundedCornerShape(10.dp),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0x22000000))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("Copia",    modifier = Modifier.pointerInput(Unit){ detectTapGestures(onTap = { onCopy() }) })
                    Spacer(Modifier.width(14.dp))
                    Text("Seleziona tutto", modifier = Modifier.pointerInput(Unit){ detectTapGestures(onTap = { onSelectAll() }) })
                    Spacer(Modifier.width(14.dp))
                    Text("Incolla",  modifier = Modifier.pointerInput(Unit){ detectTapGestures(onTap = { onPaste() }) })
                }
            }
        }
    }
}

/* ------------------------- helpers ------------------------- */

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

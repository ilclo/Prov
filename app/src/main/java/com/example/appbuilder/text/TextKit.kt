package com.example.appbuilder.text

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset // <-- usa SOLO questo offset
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
import com.example.appbuilder.canvas.DrawItem
import com.example.appbuilder.canvas.PageState
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import java.util.UUID
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement

/* =========================================================================================
 * MOTORE TESTO
 * ========================================================================================= */
class TextEngine {
    data class TextNode(
        val id: String = UUID.randomUUID().toString(),
        var owner: DrawItem.RectItem?,   // null = testo su pagina
        var posPx: Offset,               // posizione assoluta in px nello spazio del Canvas
        var text: String = ""
    )
    val nodes = mutableStateListOf<TextNode>()
    var active: TextNode? by mutableStateOf(null)
    var lastCanvasSize by mutableStateOf(IntSize.Zero); private set
    var gridCols by mutableStateOf(6); private set
    fun activate(node: TextNode?) { active = node }
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
    fun onRectReplaced(old: DrawItem.RectItem, updated: DrawItem.RectItem) {
        val delta = rectTopLeftPx(updated) - rectTopLeftPx(old)
        nodes.filter { it.owner === old }.forEach { n ->
            n.owner = updated; n.posPx = n.posPx + delta
            if (active === n) active = n
        }
    }
    fun placeCaret(owner: DrawItem.RectItem?, tapPx: Offset, nearPx: Float) {
        val nearest = nodes.minByOrNull { (it.posPx - tapPx).getDistance() }
        val picked = if (nearest != null && (nearest.posPx - tapPx).getDistance() <= nearPx) nearest
        else TextNode(owner = owner, posPx = tapPx).also { nodes.add(it) }
        active = picked
    }
    fun updateText(newValue: String) { active?.text = newValue }
}
/* =========================================================================================
 * LAYER TESTO — SEMPRE visibile in statico; editing solo se editEnabled
 * ========================================================================================= */
@Composable
fun TextLayer(
    editEnabled: Boolean,
    page: PageState?,
    engine: TextEngine,
    bottomSafePx: Int = 0,
    textStyle: TextStyle = TextStyle.Default
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val kb = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    LaunchedEffect(canvasSize, page?.gridDensity) {
        engine.updateEnv(canvasSize, page?.gridDensity ?: 6)
    }

    // Modificatore con/p senza input a seconda di editEnabled
    val baseMod = Modifier
        .fillMaxSize()
        .onSizeChanged { canvasSize = it }

    val mod = if (editEnabled) {
        baseMod.pointerInput(page?.items, page?.gridDensity, canvasSize) {
            detectTapGestures(onTap = { ofs ->
                val owner = hitTestRectAt(ofs, page, canvasSize)
                val nearPx = with(density) { 24.dp.toPx() }
                engine.placeCaret(owner, ofs, nearPx)
                kb?.show()
            })
        }
    } else baseMod

    Box(modifier = mod) {
        // 1) Nodo attivo → campo editabile, SOLO se edit abilitato
        if (editEnabled) {
            engine.active?.let { node ->
                val caretMargin = with(density) { 32.dp.toPx() }
                val safeY = min(node.posPx.y, (canvasSize.height - bottomSafePx).toFloat() - caretMargin).coerceAtLeast(0f)
                val focusReq = remember { FocusRequester() }

                BasicTextField(
                    value = node.text,
                    onValueChange = { engine.updateText(it) },
                    textStyle = textStyle, // ⬅️  applico font/size/colore
                    cursorBrush = SolidColor(textStyle.color),
                    modifier = Modifier
                        .offset { IntOffset(node.posPx.x.roundToInt(), safeY.roundToInt()) }
                        .focusRequester(focusReq)
                        .onFocusChanged { st -> if (st.isFocused) kb?.show() }
                )
                LaunchedEffect(node.id) { focusReq.requestFocus() }
            }
        }

        // 2) Nodi non attivi → render statico SEMPRE (anche fuori menù Testo)
        engine.nodes.forEach { n ->
            if (engine.active?.id == n.id && editEnabled) return@forEach
            BasicText(
                text = n.text,
                style = textStyle, // ⬅️ stesso stile anche per il render statico
                modifier = Modifier
                    .offset { IntOffset(n.posPx.x.roundToInt(), n.posPx.y.roundToInt()) }
                    .let {
                        if (editEnabled) it.pointerInput(n.id) {
                            detectTapGestures {
                                engine.activate(n)
                                kb?.show()
                            }
                        } else it
                    }
            )
        }
    }
}


/* ---------- Toolbar nera “GitHub-like” ---------- */
@Composable
private fun TextContextBar(
    visible: Boolean,
    anchor: IntOffset,                // posizione schermo dove ancorare (sopra selezione)
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
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Copia",
                        modifier = Modifier.pointerInput(Unit){ detectTapGestures(onTap = { onCopy() }) })
                    Text("Seleziona tutto",
                        modifier = Modifier.pointerInput(Unit){ detectTapGestures(onTap = { onSelectAll() }) })
                    Text("Incolla",
                        modifier = Modifier.pointerInput(Unit){ detectTapGestures(onTap = { onPaste() }) })
                }
            }
        }
    }
}

/* ------------------------- helpers ------------------------- */


private fun hitTestRectAt(ofs: Offset, page: PageState?, size: IntSize): DrawItem.RectItem? {
    val cols = max(1, page?.gridDensity ?: 6)
    val cell = min(size.width.toFloat() / cols, size.height.toFloat() / cols)
    if (cell <= 0f || page == null) return null
    fun bounds(r: DrawItem.RectItem): IntArray {
        val r0 = min(r.r0, r.r1); val r1 = max(r.r0, r.r1)
        val c0 = min(r.c0, r.c1); val c1 = max(r.c0, r.c1)
        return intArrayOf(r0, r1, c0, c1)
    }
    fun contains(r: DrawItem.RectItem, row: Int, col: Int): Boolean {
        val (r0, r1, c0, c1) = bounds(r); return row in r0..r1 && col in c0..c1
    }
    fun computeCell(ofs: Offset): Pair<Int, Int> {
        val r = floor(ofs.y / cell).toInt().coerceAtLeast(0)
        val c = floor(ofs.x / cell).toInt().coerceAtLeast(0)
        return r to c
    }
    val (rr, cc) = computeCell(ofs)
    return page.items.filterIsInstance<DrawItem.RectItem>().filter { contains(it, rr, cc) }.maxByOrNull { it.level }
}
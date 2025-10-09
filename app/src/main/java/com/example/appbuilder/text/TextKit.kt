package com.example.appbuilder.text

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.offset as pixelOffset // per offset(IntOffset)
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appbuilder.canvas.DrawItem
import com.example.appbuilder.canvas.PageState
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Blocco di testo posizionato su griglia, eventualmente figlio di un contenitore. */
@Stable
data class TextBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    var parent: DrawItem.RectItem? = null,       // null => testo su background pagina
    var localRow: Int = 0,                       // cella (riga) locale al parent (o pagina)
    var localCol: Int = 0,                       // cella (colonna) locale al parent (o pagina)
    var offsetInCellPx: Offset = Offset.Zero,    // offset fine nella cella (px)
    var value: TextFieldValue = TextFieldValue(""),
    var style: TextStyle = TextStyle(
        fontSize = 16.sp,
        color = Color.Black
    )
)

@Stable
class TextEngineState {
    val blocks = mutableStateListOf<TextBlock>()
    var active: TextBlock? by mutableStateOf(null)

    /** Chiamata quando un RectItem viene sostituito (drag/resize/replace). */
    fun onRectReplaced(old: DrawItem.RectItem, updated: DrawItem.RectItem) {
        blocks.forEach { if (it.parent === old) it.parent = updated }
    }
}

@Composable
fun rememberTextEngine(): TextEngineState = remember { TextEngineState() }

/**
 * Overlay di testo da piazzare sopra al Canvas.
 *
 * @param enabled se true intercetta il tap e mostra il caret/tastiera
 * @param gridDensity numero di celle per lato (come page.gridDensity)
 * @param bottomSafePx margine “sicuro” in px (es. altezza barre) per non coprire il caret
 */
@Composable
fun BoxScope.TextOverlay(
    engine: TextEngineState,
    page: PageState?,
    enabled: Boolean,
    gridDensity: Int,
    bottomSafePx: Int = 0
) {
    val density = LocalDensity.current
    val kb = LocalSoftwareKeyboardController.current

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // layer che copre esattamente il Canvas
    Box(
        modifier = Modifier
            .matchParentSize()
            .onSizeChanged { newSize -> canvasSize = newSize }
            .pointerInput(enabled, page, gridDensity, canvasSize) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onTap = { p ->
                        if (canvasSize.width <= 0 || canvasSize.height <= 0 || gridDensity <= 0) return@detectTapGestures

                        // 1) px -> cella della pagina (come nel Canvas)
                        val cell = min(
                            canvasSize.width.toFloat() / gridDensity,
                            canvasSize.height.toFloat() / gridDensity
                        )
                        val rr = floor(p.y / cell).toInt().coerceIn(0, gridDensity - 1)
                        val cc = floor(p.x / cell).toInt().coerceIn(0, gridDensity - 1)

                        // 2) rettangolo “in top” che contiene la cella
                        val parentRect = topRectAtCell(page, rr, cc)

                        // 3) cella locale e offset fine nella cella
                        val (localR, localC) = if (parentRect != null) {
                            val r0 = min(parentRect.r0, parentRect.r1)
                            val c0 = min(parentRect.c0, parentRect.c1)
                            (rr - r0) to (cc - c0)
                        } else rr to cc

                        val ox = p.x - cc * cell
                        val oy = p.y - rr * cell

                        // 4) crea/attiva un blocco nel punto
                        val newBlock = TextBlock(
                            parent = parentRect,
                            localRow = localR,
                            localCol = localC,
                            offsetInCellPx = Offset(ox, oy)
                        )
                        engine.blocks.add(newBlock)
                        engine.active = newBlock
                        kb?.show()
                    }
                )
            }
    ) {
        // Rendering dei blocchi (attivo = BasicTextField con caret)
        engine.blocks.forEach { block ->
            val minSide = min(canvasSize.width, canvasSize.height)
            if (gridDensity <= 0 || minSide <= 0) return@forEach

            val cell = min(
                canvasSize.width.toFloat() / gridDensity,
                canvasSize.height.toFloat() / gridDensity
            )

            // top-left del parent in px + offset locale
            val parentLeft = (block.parent?.let { min(it.c0, it.c1) } ?: 0).toFloat() * cell
            val parentTop  = (block.parent?.let { min(it.r0, it.r1) } ?: 0).toFloat() * cell
            val contentLeft = parentLeft + block.localCol * cell + block.offsetInCellPx.x
            val contentTop  = parentTop  + block.localRow * cell + block.offsetInCellPx.y

            // Clip all’area del parent (se presente), altrimenti all’intera pagina
            val clipW = block.parent?.let { (abs(it.c1 - it.c0) + 1).toFloat() * cell }
                ?: canvasSize.width.toFloat()
            val clipH = block.parent?.let { (abs(it.r1 - it.r0) + 1).toFloat() * cell }
                ?: canvasSize.height.toFloat()

            // Evito caret sotto tastiera/barre
            val yMax = canvasSize.height - bottomSafePx
            val safeTop = min(contentTop, yMax - with(density) { 32.dp.toPx() }) // 32dp margine

            Box(
                modifier = Modifier
                    // offset del “clip layer” in pixel
                    .pixelOffset { IntOffset(parentLeft.toInt(), parentTop.toInt()) }
                    // size in dp
                    .size(
                        width = clipW.dpPxToDp(density),
                        height = clipH.dpPxToDp(density)
                    )
                    .clipToBounds()
            ) {
                val isActive = (engine.active === block)
                if (isActive) {
                    val fr = remember { FocusRequester() }
                    LaunchedEffect(block.id) {
                        fr.requestFocus()
                        kb?.show()
                    }
                    BasicTextField(
                        value = block.value,
                        onValueChange = { block.value = it },
                        textStyle = block.style,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            // offset del campo rispetto al parent, in pixel
                            .pixelOffset {
                                val relX = (contentLeft - parentLeft).toInt()
                                val relY = (safeTop - parentTop).toInt()
                                IntOffset(relX, relY)
                            }
                            .focusRequester(fr)
                            .onFocusChanged { st: FocusState ->
                                if (!st.isFocused && engine.active === block) {
                                    engine.active = null
                                }
                            }
                    )
                } else {
                    // Testo “statico” cliccabile per riselezione
                    Box(
                        modifier = Modifier
                            .pixelOffset {
                                val relX = (contentLeft - parentLeft).toInt()
                                val relY = (safeTop - parentTop).toInt()
                                IntOffset(relX, relY)
                            }
                            .pointerInput(block.id) {
                                detectTapGestures {
                                    engine.active = block
                                    kb?.show()
                                }
                            }
                    ) {
                        Text(
                            text = block.value.text,
                            style = block.style,
                            color = block.style.color
                        )
                    }
                }
            }
        }
    }
}

/* —————————— utility —————————— */

private fun Float.dpPxToDp(density: Density): Dp =
    (this / density.density).dp

private fun topRectAtCell(page: PageState?, row: Int, col: Int): DrawItem.RectItem? {
    // rettangolo che contiene la cella con livello massimo
    val rects = page?.items?.filterIsInstance<DrawItem.RectItem>().orEmpty()
    val candidates = rects.filter { containsCell(it, row, col) }
    return candidates.maxByOrNull { it.level }
}

private fun containsCell(r: DrawItem.RectItem, row: Int, col: Int): Boolean {
    val r0 = min(r.r0, r.r1); val r1 = max(r.r0, r.r1)
    val c0 = min(r.c0, r.c1); val c1 = max(r.c0, r.c1)
    // Inclusivo (coerente con il disegno: w = (abs(c1-c0)+1)*cell)
    return row in r0..r1 && col in c0..c1
}

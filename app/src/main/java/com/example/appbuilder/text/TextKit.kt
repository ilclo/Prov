package com.example.appbuilder.text

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import com.example.appbuilder.canvas.DrawItem
import com.example.appbuilder.canvas.PageState

/** Blocco di testo posizionato su griglia, opzionalmente “figlio” di un contenitore. **/
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

    /** Usato dall’editor quando un RectItem viene rimpiazzato (spostato/ridimensionato). */
    fun onRectReplaced(old: DrawItem.RectItem, updated: DrawItem.RectItem) {
        blocks.forEach { if (it.parent === old) it.parent = updated }
    }
}

@Composable
fun rememberTextEngine(): TextEngineState = remember { TextEngineState() }

/**
 * Overlay di testo da piazzare sopra al Canvas.
 * - enabled: attivo solo quando è aperto il menù “Testo”
 * - gridDensity: numero di celle per lato (stesso di page.gridDensity)
 * - bottomSafePx: margine “di sicurezza” in px per evitare sovrapposizione con le barre e l’IME
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
            .onSizeChanged { canvasSize = it }
            .pointerInput(enabled, page, gridDensity, canvasSize) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onTap = { p ->
                        if (canvasSize.width <= 0 || canvasSize.height <= 0 || gridDensity <= 0) return@detectTapGestures
                        // 1) converto px -> cella della pagina (come computeCell)
                        val cell = min(canvasSize.width.toFloat() / gridDensity, canvasSize.height.toFloat() / gridDensity)
                        val rr = floor(p.y / cell).toInt().coerceIn(0, gridDensity - 1)
                        val cc = floor(p.x / cell).toInt().coerceIn(0, gridDensity - 1)

                        // 2) cerco il rettangolo in top a quella cella (stesso criterio del Canvas)
                        val parentRect = topRectAtCell(page, rr, cc)

                        // 3) calcolo cella locale e offset fine nella cella
                        val (localR, localC) = if (parentRect != null) {
                            val r0 = min(parentRect.r0, parentRect.r1)
                            val c0 = min(parentRect.c0, parentRect.c1)
                            (rr - r0) to (cc - c0)
                        } else rr to cc

                        val ox = p.x - cc * cell
                        val oy = p.y - rr * cell

                        // 4) creo (o attivo) un blocco nuovo nel punto
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
        // Rendering dei blocchi (quelli attivi come BasicTextField con caret)
        engine.blocks.forEach { block ->
            val cell = if (gridDensity > 0 && canvasSize.minDimension > 0) {
                min(canvasSize.width.toFloat() / gridDensity, canvasSize.height.toFloat() / gridDensity)
            } else 0f

            if (cell <= 0f) return@forEach

            // top-left del parent in px + offset locale
            val parentLeft = (block.parent?.let { min(it.c0, it.c1) } ?: 0).toFloat() * cell
            val parentTop  = (block.parent?.let { min(it.r0, it.r1) } ?: 0).toFloat() * cell
            val contentLeft = parentLeft + block.localCol * cell + block.offsetInCellPx.x
            val contentTop  = parentTop  + block.localRow * cell + block.offsetInCellPx.y

            // Box di clipping quando il testo appartiene a un contenitore
            val clipW = (block.parent?.let { (abs(it.c1 - it.c0) + 1).toFloat() * cell } ?: canvasSize.width.toFloat())
            val clipH = (block.parent?.let { (abs(it.r1 - it.r0) + 1).toFloat() * cell } ?: canvasSize.height.toFloat())

            // Evito che il caret finisca sotto tastiera+barre: rialzo il campo se serve
            val yMax = canvasSize.height - bottomSafePx
            val safeTop = min(contentTop, yMax - with(density) { 32.dp.toPx() }) // 32dp margine

            Box(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(parentLeft.toInt(), parentTop.toInt()) }
                    .size(width = clipW.dpPxToDp(density), height = clipH.dpPxToDp(density))
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
                            .offset {
                                val relX = (contentLeft - parentLeft).toInt()
                                val relY = (safeTop - parentTop).toInt()
                                androidx.compose.ui.unit.IntOffset(relX, relY)
                            }
                            .focusRequester(fr)
                            .onFocusChanged { st ->
                                if (!st.isFocused && engine.active === block) {
                                    engine.active = null
                                }
                            }
                    )
                } else {
                    // Testo “statico” cliccabile per riselezione
                    Box(
                        modifier = Modifier
                            .offset {
                                val relX = (contentLeft - parentLeft).toInt()
                                val relY = (safeTop - parentTop).toInt()
                                androidx.compose.ui.unit.IntOffset(relX, relY)
                            }
                            .pointerInput(block.id) {
                                detectTapGestures {
                                    engine.active = block
                                    kb?.show()
                                }
                            }
                    ) {
                        androidx.compose.material3.Text(
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

/* —————————— utility —————————— **/

private fun Float.dpPxToDp(density: LocalDensity): androidx.compose.ui.unit.Dp =
    with(density) { (this@dpPxToDp / density.density).dp }

private fun topRectAtCell(page: PageState?, row: Int, col: Int): DrawItem.RectItem? {
    // Stesso criterio usato nel Canvas: rettangolo che contiene la cella con livello massimo
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


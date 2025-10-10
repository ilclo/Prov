package com.example.appbuilder.text

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.example.appbuilder.canvas.DrawItem
import com.example.appbuilder.canvas.PageState
import kotlin.math.*
import androidx.compose.foundation.text.BasicText

/** Blocco testuale posizionato su griglia e (opzionalmente) figlio di un contenitore. */
@Stable
data class TextBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    var parent: DrawItem.RectItem? = null,     // null => background pagina
    var localRow: Int = 0,                     // cella (riga) locale al parent/pagina
    var localCol: Int = 0,                     // cella (colonna) locale al parent/pagina
    var offsetInCellPx: Offset = Offset.Zero,  // offset “fine” all’interno della cella
    var value: TextFieldValue = TextFieldValue(""),
    var style: TextStyle = TextStyle(fontSize = 16.sp, color = Color.Black)
)

/** Motore “semplice” per i testi, pensato per EditorKit che usa TextOverlay + TextEngine. */
@Stable
class TextEngine {
    val blocks = mutableStateListOf<TextBlock>()
    var active: TextBlock? by mutableStateOf(null)

    /** Quando un Rect viene rimpiazzato (drag/resize), migra il parent dei blocchi testo. */
    fun onRectReplaced(old: DrawItem.RectItem, updated: DrawItem.RectItem) {
        blocks.forEach { if (it.parent === old) it.parent = updated }
    }
}

/**
 * Overlay di testo da posizionare SOPRA al Canvas.
 * Attivo solo quando [enabled] = true (così non blocca il “Crea contenitore”).
 */
@Composable
fun BoxScope.TextOverlay(
    engine: TextEngine,
    page: PageState?,
    enabled: Boolean,
    gridDensity: Int
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .then(
                if (enabled) {
                    Modifier.pointerInput(page, gridDensity, canvasSize) {
                        detectTapGestures(
                            onTap = { p ->   // ⬅️ QUI
                                // 1) guard-rails
                                if (canvasSize.width <= 0 || canvasSize.height <= 0 || gridDensity <= 0) return@detectTapGestures

                                // 2) px -> cella
                                val cell = min(
                                    canvasSize.width.toFloat() / gridDensity,
                                    canvasSize.height.toFloat() / gridDensity
                                )
                                val rr = floor(p.y / cell).toInt().coerceIn(0, gridDensity - 1)
                                val cc = floor(p.x / cell).toInt().coerceIn(0, gridDensity - 1)

                                // 3) top-most rect che include la cella
                                val parentRect = topRectAtCell(page, rr, cc)

                                // 4) coordinate locali + offset fine nella cella
                                val (localR, localC) = if (parentRect != null) {
                                    val r0 = min(parentRect.r0, parentRect.r1)
                                    val c0 = min(parentRect.c0, parentRect.c1)
                                    (rr - r0) to (cc - c0)
                                } else rr to cc

                                val ox = p.x - cc * cell
                                val oy = p.y - rr * cell

                                // 5) crea/attiva blocco in quel punto
                                val newBlock = TextBlock(
                                    parent = parentRect,
                                    localRow = localR,
                                    localCol = localC,
                                    offsetInCellPx = Offset(ox, oy)
                                )
                                engine.blocks.add(newBlock)
                                engine.active = newBlock

                                // 6) focus + tastiera (dopo aver reso attivo il blocco)
                                keyboard?.show()
                                focusRequester.requestFocus()
                            }
                        )
                    }
                } else {
                    Modifier // non intercetta input quando il menu Testo è chiuso
                }
            )
    ) { /* …vedi 1.3… */ }
}


/* ============================ Helpers ============================ */

private fun IntSize.minDimension(): Int = min(width, height)

/** Ritorna il rettangolo con level più alto che contiene la cella (row,col). */
private fun topRectAtCell(page: PageState?, row: Int, col: Int): DrawItem.RectItem? {
    if (page == null) return null
    return page.items.asSequence()
        .filterIsInstance<DrawItem.RectItem>()
        .filter { r ->
            val r0 = min(r.r0, r.r1); val r1 = max(r.r0, r.r1)
            val c0 = min(r.c0, r.c1); val c1 = max(r.c0, r.c1)
            row in r0..r1 && col in c0..c1
        }
        .maxByOrNull { it.level } // “top-most” per level
}

package com.example.appbuilder.text


import androidx.compose.foundation.layout.offset as offsetDp  
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.example.appbuilder.canvas.DrawItem
import com.example.appbuilder.canvas.PageState
import androidx.compose.foundation.layout.*

/* =========================================================================================
 *  MODELLO TESTO
 * ========================================================================================= */

private fun genId(): String {
    val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
    return (0 until 8).map { alphabet.random() }.joinToString("")
}

/** Uno span di testo ancorato alla pagina o ad un contenitore (RectItem). */
data class TextSpan(
    val id: String = genId(),
    /** null = pagina (background) */
    var parent: DrawItem.RectItem? = null,
    /** offset riga/colonna RELATIVO al top-left del parent (se parent==null, assoluto pagina) */
    var rowOff: Int,
    var colOff: Int,
    var value: TextFieldValue = TextFieldValue("")
)

/** Motore minimale: gestisce lista span e focus corrente. */
class TextEngine {
    val spans = mutableStateListOf<TextSpan>()
    var editingId by mutableStateOf<String?>(null)
        private set

    fun current(): TextSpan? = spans.firstOrNull { it.id == editingId }

    fun clearFocus() { editingId = null }

    /** Se esiste uno span al punto (parent, rr, cc) lo seleziona; altrimenti lo crea. */
    fun focusOrCreateAt(parent: DrawItem.RectItem?, rr: Int, cc: Int): TextSpan {
        val (r0, c0) = if (parent == null) 0 to 0 else min(parent.r0, parent.r1) to min(parent.c0, parent.c1)
        val ro = rr - r0
        val co = cc - c0

        val hit = spans.firstOrNull { it.parent === parent && it.rowOff == ro && it.colOff == co }
        val tgt = hit ?: TextSpan(parent = parent, rowOff = ro, colOff = co).also { spans.add(it) }
        editingId = tgt.id
        return tgt
    }

    /** Quando un RectItem viene sostituito (move/resize), aggiorna i riferimenti degli span. */
    fun onRectReplaced(oldRect: DrawItem.RectItem, newRect: DrawItem.RectItem) {
        spans.forEach { if (it.parent === oldRect) it.parent = newRect }
    }
}

/* =========================================================================================
 *  OVERLAY — gestione tap, cursore e campo editabile
 * ========================================================================================= */

@Composable
fun TextOverlay(
    engine: TextEngine,
    page: PageState?,
    gridDensity: Int,
    enabled: Boolean
) {
    // conserviamo la size del canvas (serve per tradurre pixel -> cella)
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val kb = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    // Tap ONLY quando siamo nel menù Testo
    val tapMod = Modifier.pointerInput(enabled, page, gridDensity, canvasSize) {
        if (!enabled) return@pointerInput
        detectTapGestures(
            onTap = { ofs ->
                val cols = max(1, gridDensity)
                val (rr, cc) = computeCell(ofs, cols, canvasSize.width.toFloat(), canvasSize.height.toFloat())
                val parent = topRectAtCell(page, rr, cc)
                engine.focusOrCreateAt(parent, rr, cc)
                // la tastiera deve rimanere su finché non si esce dal menù Testo
                kb?.show()
            }
        )
    }

    // Disegniamo SIA gli span statici SIA quello in editing.
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .then(tapMod)
    ) {
        // 1) Span NON in editing (puri label)
        engine.spans.forEach { span ->
            if (span.id == engine.editingId) return@forEach
            val pos = spanPxTopLeft(span, page, gridDensity, canvasSize) ?: return@forEach
            androidx.compose.material3.Text(
                text = span.value.text,
                color = Color.Black,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.offsetDp { IntOffset(pos.first.toInt(), pos.second.toInt()) }
            )
        }

        // 2) Span in editing — 1 solo BasicTextField con caret
        val current = engine.current()
        if (current != null) {
            val (x, y, widthPx) = spanEditBox(current, page, gridDensity, canvasSize) ?: Triple(0f, 0f, 0f)
            val focusRequester = remember { FocusRequester() }
            var tfv by remember(current.id) { mutableStateOf(current.value) }

            BasicTextField(
                value = tfv,
                onValueChange = { v -> tfv = v; current.value = v },
                textStyle = TextStyle(color = Color.Black),
                singleLine = false,
                modifier = Modifier
                    .offsetDp { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .widthIn(min = 48.dp, max = with(density) { widthPx.coerceAtLeast(32f).toDp() })
                    .focusRequester(focusRequester)
                    .onFocusChanged { st -> if (!st.isFocused) engine.clearFocus() }
            )

            // Richiesta focus & apertura tastiera all'avvio editing
            LaunchedEffect(current.id) {
                focusRequester.requestFocus()
                kb?.show()
            }

            // Back chiude solo l’editing (mantiene il menù Testo aperto)
            BackHandler(enabled = enabled && engine.current() != null) {
                engine.clearFocus()
            }
        }
    }
}

/* =========================================================================================
 *  HELPER: mapping pixel↔cella, lookup rettangolo, posizionamento
 * ========================================================================================= */

private fun computeCell(ofs: Offset, cols: Int, w: Float, h: Float): Pair<Int, Int> {
    val cell = min(w / cols, h / cols)
    val r = floor(ofs.y / cell).toInt().coerceAtLeast(0)
    val c = floor(ofs.x / cell).toInt().coerceAtLeast(0)
    return r to c
}

private fun containsCell(r: DrawItem.RectItem, row: Int, col: Int): Boolean {
    val r0 = min(r.r0, r.r1); val r1 = max(r.r0, r.r1)
    val c0 = min(r.c0, r.c1); val c1 = max(r.c0, r.c1)
    return row in r0..r1 && col in c0..c1
}

private fun topRectAtCell(page: PageState?, row: Int, col: Int): DrawItem.RectItem? =
    page?.items?.filterIsInstance<DrawItem.RectItem>()?.filter { containsCell(it, row, col) }?.maxByOrNull { it.level }

/** Calcola la posizione (px) del top-left dello span sul canvas. */
private fun spanPxTopLeft(span: TextSpan, page: PageState?, cols: Int, canvas: IntSize): Pair<Float, Float>? {
    val colsSafe = max(1, cols)
    val cell = min(canvas.width.toFloat() / colsSafe, canvas.height.toFloat() / colsSafe)
    val (baseR, baseC) = span.parent?.let { min(it.r0, it.r1) to min(it.c0, it.c1) } ?: (0 to 0)
    val rr = baseR + span.rowOff
    val cc = baseC + span.colOff
    val pad = 4f
    return (cc * cell + pad) to (rr * cell + pad)
}

/** Posizione e larghezza “utile” (in px) per l’editor sullo span. */
private fun spanEditBox(span: TextSpan, page: PageState?, cols: Int, canvas: IntSize): Triple<Float, Float, Float>? {
    val colsSafe = max(1, cols)
    val cell = min(canvas.width.toFloat() / colsSafe, canvas.height.toFloat() / colsSafe)
    val pad = 6f
    val (x, y) = spanPxTopLeft(span, page, cols, canvas) ?: return null
    val maxW: Float = span.parent?.let {
        val wCells = abs(it.c1 - it.c0) + 1
        val leftCellsUsed = span.colOff
        (max(1, wCells - leftCellsUsed).toFloat() * cell) - pad * 2
    } ?: (canvas.width - x - pad)
    return Triple(x, y, maxW)
}

package com.example.appbuilder.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.roundToInt


data class ImageCrop(
    val left: Float,   // [0f,1f]
    val top: Float,    // [0f,1f]
    val right: Float,  // [0f,1f]
    val bottom: Float  // [0f,1f]
)

// Variante di rendering del contenitore
enum class Variant { Full, Outlined, Text, TopBottom }

// Forme supportate (rettangolo con raggi, cerchio, pillola/stadium, diamante)
enum class ShapeKind { Rect, Circle, Pill, Diamond }

enum class ImageFit { Cover, Contain, Stretch }
enum class ImageFilter { None, Mono, Sepia }

data class ImageStyle(
    val uri: Uri,
    val fit: ImageFit = ImageFit.Cover,
    val filter: ImageFilter = ImageFilter.None,
    val crop: ImageCrop? = null,
    val cropToShape: Boolean = true
)

data class CornerRadii(
    val tl: Dp = 0.dp,
    val tr: Dp = 0.dp,
    val br: Dp = 0.dp,
    val bl: Dp = 0.dp
)

// Effetti grafici opzionali
enum class FxKind { None, Vignette, Noise, Stripes }

// — Direzione gradiente minimale (se vorrai aggiungere altre diagonali è banale estendere)
enum class GradientDir { Monocolore, Orizzontale, Verticale, DiagTL_BR, DiagTR_BL }

// — Stile riempimento per un rettangolo (usato senza toccare RectItem)
data class FillStyle(
    val col1: Color,
    val col2: Color? = null,
    val dir: GradientDir = GradientDir.Monocolore
)



@Composable
fun CanvasStage(
    page: PageState?,
    gridDensity: Int,
    gridPreviewOnly: Boolean,
    showFullGrid: Boolean,
    currentLevel: Int,
    creationEnabled: Boolean = true,
    toolMode: ToolMode = ToolMode.Create,
    selected: DrawItem.RectItem? = null,
    onAddItem: (DrawItem) -> Unit,
    onRequestEdit: (DrawItem.RectItem?) -> Unit = {},
    onUpdateItem: (DrawItem.RectItem, DrawItem.RectItem) -> Unit = { _, _ -> },
    fillStyles: Map<DrawItem.RectItem, FillStyle> = emptyMap(),
    variants : Map<DrawItem.RectItem, Variant> = emptyMap(),
    shapes   : Map<DrawItem.RectItem, ShapeKind> = emptyMap(),
    corners  : Map<DrawItem.RectItem, CornerRadii> = emptyMap(),
    fx       : Map<DrawItem.RectItem, FxKind> = emptyMap(),
    imageStyles: Map<DrawItem.RectItem, ImageStyle> = emptyMap(),
    pageBackgroundColor: Color = Color.White,
    pageBackgroundBrush: Brush? = null
) {
    val context = LocalContext.current

    // Cache semplice per le immagini caricate (per URI)
    val imageCache = remember { mutableMapOf<Uri, ImageBitmap?>() }


    // Loader NON-@Composable (si può usare dentro draw{})
    fun loadBitmap(uri: Uri?): ImageBitmap? {
        if (uri == null) return null
        imageCache[uri]?.let { return it }
        return try {
            context.contentResolver.openInputStream(uri)?.use { s ->
                val bmp = BitmapFactory.decodeStream(s)?.asImageBitmap()
                imageCache[uri] = bmp
                bmp
            }
        } catch (_: Throwable) {
            null
        }
    }


    fun colorFilterFor(filter: ImageFilter): ColorFilter? = when (filter) {
        ImageFilter.Mono -> {
            val m = ColorMatrix()
            m.setToSaturation(0f)
            ColorFilter.colorMatrix(m)
        }
        ImageFilter.Sepia -> {
            // matrice seppia semplice
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )))
        }
        else -> null
    }

    // Stato "Create" (lasciato invariato)
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var firstAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // Stato locale per RESIZE
    var resizeTarget by remember { mutableStateOf<DrawItem.RectItem?>(null) }
    var resizeFixed  by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Quando esci dal menù Contenitore (creationEnabled=false) azzera ogni evidenziazione
    LaunchedEffect(creationEnabled) {
        if (!creationEnabled) {
            hoverCell = null
            firstAnchor = null
            resizeTarget = null
            resizeFixed = null
        }
    }

    // Stato anteprima per Grab/Resize
    var movingRect by remember { mutableStateOf<DrawItem.RectItem?>(null) }   // preview movimento
    var resizingRect by remember { mutableStateOf<DrawItem.RectItem?>(null) } // preview resize
    var activeRect by remember { mutableStateOf<DrawItem.RectItem?>(null) }   // rettangolo coinvolto (grab/resize)

    val cols = max(1, gridDensity)
    val azure = Color(0xFF58A6FF)
    val lineBlack = Color.Black

    // --- Helpers come nel file originale (più collisioni/intersezioni) ---
    fun rectBounds(r: DrawItem.RectItem): IntArray {
        val r0 = min(r.r0, r.r1); val r1 = max(r.r0, r.r1)
        val c0 = min(r.c0, r.c1); val c1 = max(r.c0, r.c1)
        return intArrayOf(r0, r1, c0, c1)
    }
    fun containsCell(r: DrawItem.RectItem, row: Int, col: Int): Boolean {
        val (r0, r1, c0, c1) = rectBounds(r)
        return row in r0..r1 && col in c0..c1
    }
    fun topRectAtCell(row: Int, col: Int): DrawItem.RectItem? =
        page?.items?.filterIsInstance<DrawItem.RectItem>()
            ?.filter { containsCell(it, row, col) }
            ?.maxByOrNull { it.level }

    fun intersects(a: DrawItem.RectItem, b: DrawItem.RectItem): Boolean {
        val (ar0, ar1, ac0, ac1) = rectBounds(a)
        val (br0, br1, bc0, bc1) = rectBounds(b)
        return (ar0 <= br1 && br0 <= ar1 && ac0 <= bc1 && bc0 <= ac1)
    }
    fun collides(candidate: DrawItem.RectItem, except: DrawItem.RectItem?): Boolean {
        val others = page?.items?.filterIsInstance<DrawItem.RectItem>().orEmpty()
        return others.any { it !== except && intersects(candidate, it) }
    }

    fun splitHoriz(rect: DrawItem.RectItem, b: Int): List<DrawItem.RectItem> {
        val (r0, r1, c0, c1) = rectBounds(rect)
        if (b !in r0 until r1) return listOf(rect)
        val top = rect.copy(r0 = r0, r1 = b,    c0 = c0, c1 = c1)
        val bot = rect.copy(r0 = b + 1, r1 = r1, c0 = c0, c1 = c1)
        return listOf(top, bot)
    }
    fun splitVert(rect: DrawItem.RectItem, b: Int): List<DrawItem.RectItem> {
        val (r0, r1, c0, c1) = rectBounds(rect)
        if (b !in c0 until c1) return listOf(rect)
        val left  = rect.copy(r0 = r0, r1 = r1, c0 = c0,     c1 = b)
        val right = rect.copy(r0 = r0, r1 = r1, c0 = b + 1,  c1 = c1)
        return listOf(left, right)
    }
    fun chooseBoundaryRow(rect: DrawItem.RectItem, rr: Int): Int? {
        val (r0, r1, _, _) = rectBounds(rect)
        val candidates = buildList {
            val b1 = rr - 1; val b2 = rr
            if (b1 in r0 until r1) add(b1)
            if (b2 in r0 until r1) add(b2)
        }
        if (candidates.isEmpty()) return null
        val mid = (r0 + r1) / 2.0
        return candidates.minByOrNull { kotlin.math.abs((it + 0.5) - mid) }
    }
    fun chooseBoundaryCol(rect: DrawItem.RectItem, cc: Int): Int? {
        val (_, _, c0, c1) = rectBounds(rect)
        val candidates = buildList {
            val b1 = cc - 1; val b2 = cc
            if (b1 in c0 until c1) add(b1)
            if (b2 in c0 until c1) add(b2)
        }
        if (candidates.isEmpty()) return null
        val mid = (c0 + c1) / 2.0
        return candidates.minByOrNull { kotlin.math.abs((it + 0.5) - mid) }
    }

    // Pulizia overlay Create quando cambio modalità
    LaunchedEffect(toolMode, creationEnabled) {
        if (toolMode != ToolMode.Create || !creationEnabled) {
            hoverCell = null
            firstAnchor = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()

            // --- TAP / LONG‑PRESS: Create (immutata) e Point ---
            .pointerInput(cols, creationEnabled, toolMode) {
                detectTapGestures(
                    onTap = { ofs ->
                        when (toolMode) {
                            ToolMode.Create -> {
                                if (!creationEnabled) return@detectTapGestures
                                hoverCell = computeCell(
                                    ofs, cols,
                                    this.size.width.toFloat(), this.size.height.toFloat()
                                )
                            }
                            ToolMode.Point -> {
                                val (rr, cc) = computeCell(
                                    ofs, cols,
                                    this.size.width.toFloat(), this.size.height.toFloat()
                                )
                                onRequestEdit(topRectAtCell(rr, cc))
                            }
                            else -> Unit
                        }
                    },
                    onLongPress = { ofs ->
                        when (toolMode) {
                            ToolMode.Create -> {
                                if (!creationEnabled) return@detectTapGestures
                                val cell = computeCell(ofs, cols, this.size.width.toFloat(), this.size.height.toFloat())
                                if (firstAnchor == null) {
                                    firstAnchor = cell
                                } else {
                                    val (r0, c0) = firstAnchor!!
                                    val (r1, c1) = cell
                                    firstAnchor = null

                                    if (r0 == r1 && c0 == c1) return@detectTapGestures
                                    val sameRow = (r0 == r1)
                                    val sameCol = (c0 == c1)

                                    if (sameRow || sameCol) {
                                        val rr = r0.coerceAtLeast(0)
                                        val rectA = topRectAtCell(r0, c0)
                                        val rectB = topRectAtCell(r1, c1)
                                        val rect = if (rectA != null && rectA == rectB) rectA else null
                                        if (rect != null) {
                                            val (R0, R1, C0, C1) = rectBounds(rect)
                                            if (sameRow) {
                                                val ccMin = min(c0, c1)
                                                val ccMax = max(c0, c1)
                                                val fullSpan = (ccMin == C0 && ccMax == C1 && rr in R0..R1)
                                                if (fullSpan) {
                                                    val b = chooseBoundaryRow(rect, rr) ?: return@detectTapGestures
                                                    page?.items?.remove(rect)
                                                    splitHoriz(rect, b).forEach { onAddItem(it) }
                                                    val line = DrawItem.LineItem(
                                                        level = rect.level + 1,
                                                        r0 = b, c0 = C0 - 1, r1 = b, c1 = C1 + 1,
                                                        color = Color.Black, width = 2.dp
                                                    )
                                                    onAddItem(line)
                                                    return@detectTapGestures
                                                }
                                            } else { // sameCol
                                                val cc = c0.coerceAtLeast(0)
                                                val rrMin = min(r0, r1)
                                                val rrMax = max(r0, r1)
                                                val fullSpan = (rrMin == R0 && rrMax == R1 && cc in C0..C1)
                                                if (fullSpan) {
                                                    val b = chooseBoundaryCol(rect, cc) ?: return@detectTapGestures
                                                    page?.items?.remove(rect)
                                                    splitVert(rect, b).forEach { onAddItem(it) }
                                                    val line = DrawItem.LineItem(
                                                        level = rect.level + 1,
                                                        r0 = R0 - 1, c0 = b, r1 = R1 + 1, c1 = b,
                                                        color = Color.Black, width = 2.dp
                                                    )
                                                    onAddItem(line)
                                                    return@detectTapGestures
                                                }
                                            }
                                        }
                                        return@detectTapGestures // non satisfy le condizioni -> no-op
                                    }

                                    // non allineati -> rettangolo nuovo (comportamento esistente)
                                    val rr0 = min(r0, r1); val rr1 = max(r0, r1)
                                    val cc0 = min(c0, c1); val cc1 = max(c0, c1)
                                    val rect = DrawItem.RectItem(
                                        level = currentLevel,
                                        r0 = rr0, c0 = cc0, r1 = rr1, c1 = cc1,
                                        borderColor = Color.Black, borderWidth = 1.dp, fillColor = Color.White
                                    )
                                    onAddItem(rect)
                                }
                            }

                            ToolMode.Resize -> {
                                // 1) converto il tocco in cella
                                val (rr, cc) = computeCell(ofs, cols, this.size.width.toFloat(), this.size.height.toFloat())

                                // 2) se non sto già ridimensionando: devo iniziare da un ANGOLO del rettangolo
                                if (resizeTarget == null) {
                                    val rect = topRectAtCell(rr, cc) ?: return@detectTapGestures
                                    val (r0, r1, c0, c1) = rectBounds(rect)
                                    val tl = r0 to c0
                                    val tr = r0 to c1
                                    val bl = r1 to c0
                                    val br = r1 to c1
                                    val pressed = listOf(tl, tr, bl, br).firstOrNull { it.first == rr && it.second == cc }
                                        ?: return@detectTapGestures // deve essere proprio un angolo

                                    val opposite = when (pressed) {
                                        tl -> br
                                        tr -> bl
                                        bl -> tr
                                        else -> tl // br
                                    }
                                    resizeTarget = rect
                                    resizeFixed = opposite
                                    return@detectTapGestures
                                }

                                // 3) già in corso: il secondo long‑press definisce il nuovo angolo mobile
                                val target = resizeTarget ?: return@detectTapGestures
                                val (fr, fc) = resizeFixed ?: return@detectTapGestures
                                val newR0 = min(fr, rr); val newR1 = max(fr, rr)
                                val newC0 = min(fc, cc); val newC1 = max(fc, cc)

                                val updated = target.copy(r0 = newR0, r1 = newR1, c0 = newC0, c1 = newC1)

                                // Sostituisco il rect in lista (senza callback aggiuntivi, per restare minimale)
                                page?.items?.let { lst ->
                                    val ix = lst.indexOf(target)
                                    if (ix >= 0) lst[ix] = updated else lst.add(updated)
                                } ?: run {
                                    // in assenza di page, lo aggiungo
                                    onAddItem(updated)
                                }

                                // reset stato resize
                                resizeTarget = null
                                resizeFixed  = null
                            }

                            else -> Unit // Point/Grab: gestite altrove; qui non tocco nulla
                        }
                    }
                )
            }


            // --- DRAG dopo LONG‑PRESS: Grab / Resize ---
            .pointerInput(cols, page?.items, toolMode) {
                if (toolMode == ToolMode.Grab || toolMode == ToolMode.Resize) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { ofs ->
                            val (rr, cc) = computeCell(ofs, cols, this.size.width.toFloat(), this.size.height.toFloat())
                            val rect = topRectAtCell(rr, cc) ?: return@detectDragGesturesAfterLongPress
                            activeRect = rect
                            onRequestEdit(rect) // seleziona in menù

                            if (toolMode == ToolMode.Grab) {
                                movingRect = rect // anteprima parte da qui
                            } else {
                                // scegli angolo più vicino come “mobile”
                                val (r0, r1, c0, c1) = rectBounds(rect)
                                val corners = listOf(r0 to c0, r0 to c1, r1 to c0, r1 to c1)
                                val nearest = corners.minByOrNull { (cr, cc2) -> abs(cr - rr) + abs(cc2 - cc) }!!
                                val fixed = when (nearest) {
                                    r0 to c0 -> r1 to c1
                                    r0 to c1 -> r1 to c0
                                    r1 to c0 -> r0 to c1
                                    else     -> r0 to c0
                                }
                                // salvo come rettangolo “preview”: userò fixed + cursore per ridisegnare
                                resizingRect = rect.copy() // preview = rettangolo corrente
                                // memorizzo i corner in state locali
                                resizeFixedCorner = fixed
                                resizeMovingCornerStart = nearest
                            }
                        },
                        onDrag = { change, _ ->
                            val cellSize = min(this.size.width.toFloat() / cols, this.size.height.toFloat() / cols)
                            val rows = if (cellSize > 0f) floor(this.size.height / cellSize).toInt() else 0
                            val (cr, cc) = computeCell(change.position, cols, this.size.width.toFloat(), this.size.height.toFloat())
                            val base = activeRect ?: return@detectDragGesturesAfterLongPress

                            if (toolMode == ToolMode.Grab) {
                                val (br0, br1, bc0, bc1) = rectBounds(base)
                                val height = br1 - br0
                                val width  = bc1 - bc0
                                val startCenter = Pair((br0 + br1) / 2, (bc0 + bc1) / 2)
                                val currCenter  = Pair(cr, cc)
                                val dy = currCenter.first - startCenter.first
                                val dx = currCenter.second - startCenter.second
                                var nr0 = (br0 + dy).coerceIn(0, (rows - 1 - height).coerceAtLeast(0))
                                var nc0 = (bc0 + dx).coerceIn(0, (cols - 1 - width).coerceAtLeast(0))
                                var candidate = base.copy(r0 = nr0, r1 = nr0 + height, c0 = nc0, c1 = nc0 + width)

                                // collisioni: se collide, non aggiorno (effetto stop contro i lati)
                                if (!collides(candidate, except = base)) {
                                    movingRect = candidate
                                }
                            } else if (toolMode == ToolMode.Resize) {
                                val fixed = resizeFixedCorner ?: return@detectDragGesturesAfterLongPress
                                var r0 = min(fixed.first, cr).coerceAtLeast(0)
                                var r1 = max(fixed.first, cr).coerceAtMost(rows - 1)
                                var c0 = min(fixed.second, cc).coerceAtLeast(0)
                                var c1 = max(fixed.second, cc).coerceAtMost(cols - 1)
                                // garantisci almeno 1 cella
                                if (r1 < r0) r1 = r0
                                if (c1 < c0) c1 = c0

                                val candidate = base.copy(r0 = r0, r1 = r1, c0 = c0, c1 = c1)
                                if (!collides(candidate, except = base)) {
                                    resizingRect = candidate
                                }
                            }
                        },
                        onDragEnd = {
                            val base = activeRect
                            if (toolMode == ToolMode.Grab) {
                                movingRect?.let { preview ->
                                    if (base != null && preview != base) onUpdateItem(base, preview)
                                }
                                movingRect = null
                            } else if (toolMode == ToolMode.Resize) {
                                resizingRect?.let { preview ->
                                    if (base != null && preview != base) onUpdateItem(base, preview)
                                }
                                resizingRect = null
                            }
                            activeRect = null
                            resizeFixedCorner = null
                            resizeMovingCornerStart = null
                        },
                        onDragCancel = {
                            movingRect = null; resizingRect = null; activeRect = null
                            resizeFixedCorner = null; resizeMovingCornerStart = null
                        }
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = min(size.width / cols, size.height / cols)
            val rows = if (cell > 0f) floor(size.height / cell).toInt() else 0

            if (page != null) {
                pageBackgroundBrush?.let { brush ->
                    drawRect(brush = brush, topLeft = Offset.Zero, size = size)
                } ?: drawRect(
                    color = pageBackgroundColor,
                    topLeft = Offset.Zero,
                    size = size
                )
            }

            // Disegno elementi fino al livello corrente
            page?.items?.forEach { item ->
                if (item.level <= currentLevel) {
                    when (item) {
                        is DrawItem.RectItem -> {
                            val left = min(item.c0, item.c1).toFloat() * cell
                            val top  = min(item.r0, item.r1).toFloat() * cell
                            val w = (abs(item.c1 - item.c0) + 1).toFloat() * cell
                            val h = (abs(item.r1 - item.r0) + 1).toFloat() * cell

                            // look-up con default che riproducono il comportamento attuale
                            val kind   = shapes[item]   ?: ShapeKind.Rect
                            val varnt  = variants[item] ?: Variant.Full
                            val fxKind = fx[item]       ?: FxKind.None
                            val rad    = corners[item]  ?: CornerRadii()
                            val style  = fillStyles[item]  // può essere null
                            val imgSt  = imageStyles[item] // può essere null

                            // path della forma (rettangolo arrotondato, cerchio, pillola, rombo)
                            val path: Path = run {
                                val p = Path()
                                when (kind) {
                                    ShapeKind.Rect -> {
                                        val rr = RoundRect(
                                            rect = Rect(left, top, left + w, top + h),
                                            topLeft     = CornerRadius(rad.tl.toPx(), rad.tl.toPx()),
                                            topRight    = CornerRadius(rad.tr.toPx(), rad.tr.toPx()),
                                            bottomRight = CornerRadius(rad.br.toPx(), rad.br.toPx()),
                                            bottomLeft  = CornerRadius(rad.bl.toPx(), rad.bl.toPx())
                                        )
                                        p.addRoundRect(rr)
                                    }
                                    ShapeKind.Circle -> {
                                        val r  = kotlin.math.min(w, h) / 2f
                                        val cx = left + w / 2f
                                        val cy = top  + h / 2f
                                        p.addOval(Rect(cx - r, cy - r, cx + r, cy + r))
                                    }
                                    ShapeKind.Pill -> {
                                        val r = kotlin.math.min(w, h) / 2f
                                        p.addRoundRect(RoundRect(Rect(left, top, left + w, top + h), CornerRadius(r, r)))
                                    }
                                    ShapeKind.Diamond -> {
                                        p.moveTo(left + w/2f, top)
                                        p.lineTo(left + w,   top + h/2f)
                                        p.lineTo(left + w/2f, top + h)
                                        p.lineTo(left,       top + h/2f)
                                        p.close()
                                    }
                                }
                                p
                            }

                            // 1) FILL (solo se non "Text")
                            if (varnt != Variant.Text) {
                                if (varnt == Variant.Full) {
                                    // prova immagine; se non c’è, gradiente; altrimenti tinta
                                    val imgStyle = imageStyles[item]
                                    val imgBitmap = loadBitmap(imgStyle?.uri)

                                    if (imgStyle != null && imgBitmap != null) {
                                        val srcW = imgBitmap.width.toFloat()
                                        val srcH = imgBitmap.height.toFloat()

                                        val (dstW, dstH) = when (imgStyle.fit) {
                                            ImageFit.Cover -> {
                                                val s = kotlin.math.max(w / srcW, h / srcH)
                                                srcW * s to srcH * s
                                            }
                                            ImageFit.Contain -> {
                                                val s = kotlin.math.min(w / srcW, h / srcH)
                                                srcW * s to srcH * s
                                            }
                                            ImageFit.Stretch -> w to h
                                        }
                                        val dx = left + (w - dstW) / 2f
                                        val dy = top  + (h - dstH) / 2f

                                        val drawImageBlock: DrawScope.() -> Unit = {
                                            drawImage(
                                                image = imgBitmap,
                                                dstOffset = IntOffset(dx.toInt(), dy.toInt()),
                                                dstSize = IntSize(dstW.toInt(), dstH.toInt()),
                                                filterQuality = FilterQuality.Low,
                                                colorFilter = colorFilterFor(imgStyle.filter)
                                            )
                                        }

                                        val img = loadBitmap(imgSt?.uri)
                                        if (varnt != Variant.Text && imgSt != null && img != null) {
                                            val srcW = img.width.toFloat()
                                            val srcH = img.height.toFloat()


                                            val iw = imgBitmap.width
                                            val ih = imgBitmap.height
                                            val crop = imgStyle.crop

                                            val sx0 = ((crop?.left   ?: 0f) * iw.toFloat()).roundToInt().coerceIn(0, iw)
                                            val sy0 = ((crop?.top    ?: 0f) * ih.toFloat()).roundToInt().coerceIn(0, ih)
                                            val sx1 = ((crop?.right  ?: 1f) * iw.toFloat()).roundToInt().coerceIn(0, iw)
                                            val sy1 = ((crop?.bottom ?: 1f) * ih.toFloat()).roundToInt().coerceIn(0, ih)

                                            var srcOffset = IntOffset(sx0, sy0)
                                            var srcSize   = IntSize(max(1, sx1 - sx0), max(1, sy1 - sy0))

                                            // area destinazione = tutto il contenitore (default)
                                            var dstOffset = IntOffset(left.roundToInt(), top.roundToInt())
                                            var dstSize   = IntSize(w.roundToInt(), h.roundToInt())

                                            // AR della sorgente croppata e del contenitore
                                            val cw = srcSize.width.toFloat()
                                            val ch = srcSize.height.toFloat()
                                            val containerAR = w / h
                                            val srcAR = if (ch > 0f) cw / ch else 1f

                                            when (imgStyle.fit) {
                                                ImageFit.Stretch -> { /* niente: srcOffset/srcSize restano come sopra */ }

                                                ImageFit.Cover -> {
                                                    if (srcAR > containerAR) {
                                                        val newW = (ch * containerAR).roundToInt().coerceAtLeast(1)
                                                        val extra = (srcSize.width - newW).coerceAtLeast(0)
                                                        // evita copy(...) se dà noie: usa nuovo costruttore
                                                        srcOffset = IntOffset(srcOffset.x + extra / 2, srcOffset.y)
                                                        srcSize   = IntSize(newW, srcSize.height)
                                                    } else {
                                                        val newH = (cw / containerAR).roundToInt().coerceAtLeast(1)
                                                        val extra = (srcSize.height - newH).coerceAtLeast(0)
                                                        srcOffset = IntOffset(srcOffset.x, srcOffset.y + extra / 2)
                                                        srcSize   = IntSize(srcSize.width, newH)
                                                    }
                                                    // dst resta = tutto il contenitore
                                                }

                                                ImageFit.Contain -> {
                                                    val scale = if (srcAR > containerAR) w / cw else h / ch
                                                    val dW = (cw * scale).roundToInt().coerceAtLeast(1)
                                                    val dH = (ch * scale).roundToInt().coerceAtLeast(1)
                                                    val dx = left + (w - dW) / 2f
                                                    val dy = top  + (h - dH) / 2f
                                                    dstOffset = IntOffset(dx.roundToInt(), dy.roundToInt())
                                                    dstSize   = IntSize(dW, dH)
                                                }
                                            }

                                            // draw (clip alla forma se richiesto)
                                            val cf = colorFilterFor(imgStyle.filter)
                                            clipPath(path) {
                                                drawImage(
                                                    image = imgBitmap,
                                                    srcOffset = srcOffset,
                                                    srcSize = srcSize,
                                                    dstOffset = dstOffset,
                                                    dstSize = dstSize,
                                                    colorFilter = cf,
                                                    filterQuality = FilterQuality.Medium
                                                )
                                            }
                                        }
                                        // opzionale: applica FX sopra l’immagine (mantieni la tua logica fxKind)
                                    } else {
                                        // gradiente se definito, altrimenti tinta
                                        if (style != null && style.dir != GradientDir.Monocolore && style.col2 != null) {
                                            val (start, end) = when (style.dir) {
                                                GradientDir.Orizzontale -> Offset(left, top + h/2f)    to Offset(left + w, top + h/2f)
                                                GradientDir.Verticale   -> Offset(left + w/2f, top)    to Offset(left + w/2f, top + h)
                                                GradientDir.DiagTL_BR   -> Offset(left, top)           to Offset(left + w, top + h)
                                                GradientDir.DiagTR_BL   -> Offset(left + w, top)       to Offset(left, top + h)
                                                else -> Offset(left, top) to Offset(left, top)
                                            }
                                            clipPath(path) {
                                                drawRect(
                                                    brush = Brush.linearGradient(listOf(style.col1, style.col2), start = start, end = end),
                                                    topLeft = Offset(left, top),
                                                    size = Size(w, h)
                                                )
                                            }
                                        } else {
                                            drawPath(path = path, color = item.fillColor, style = Fill)
                                        }
                                    }
                                }
                                // 2) FX opzionali (clip alla forma)
                                if (fxKind != FxKind.None) {
                                    clipPath(path) {
                                        when (fxKind) {
                                            FxKind.Vignette -> {
                                                val center = Offset(left + w/2f, top + h/2f)
                                                val radius = kotlin.math.max(w, h) * 0.6f
                                                drawRect(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f)),
                                                        center = center, radius = radius
                                                    ),
                                                    topLeft = Offset(left, top), size = Size(w, h)
                                                )
                                            }
                                            FxKind.Noise -> {
                                                val step = 6.dp.toPx()
                                                var y = top
                                                while (y < top + h) {
                                                    var x = left + ((y.toInt() % 2) * step / 2f)
                                                    while (x < left + w) {
                                                        drawRect(
                                                            color = Color.White.copy(alpha = 0.015f),
                                                            topLeft = Offset(x, y), size = Size(1f, 1f)
                                                        )
                                                        x += step
                                                    }
                                                    y += step
                                                }
                                            }
                                            FxKind.Stripes -> {
                                                val spacing = 8.dp.toPx()
                                                var x = left - h
                                                while (x < left + w + h) {
                                                    drawLine(
                                                        color = Color.Black.copy(alpha = 0.08f),
                                                        start = Offset(x, top),
                                                        end   = Offset(x + h, top + h),
                                                        strokeWidth = 1.dp.toPx()
                                                    )
                                                    x += spacing
                                                }
                                            }
                                            else -> Unit
                                        }
                                    }
                                }
                            }
                            // 3) BORDI in base a variant
                            when (varnt) {
                                Variant.Text -> Unit // niente bordi, niente fill
                                Variant.TopBottom -> {
                                    // per forme non rettangolari → bordo completo
                                    if (kind == ShapeKind.Rect) {
                                        drawLine(
                                            color = item.borderColor,
                                            start = Offset(left, top),
                                            end   = Offset(left + w, top),
                                            strokeWidth = item.borderWidth.toPx()
                                        )
                                        drawLine(
                                            color = item.borderColor,
                                            start = Offset(left, top + h),
                                            end   = Offset(left + w, top + h),
                                            strokeWidth = item.borderWidth.toPx()
                                        )
                                    } else {
                                        drawPath(path = path, color = item.borderColor, style = Stroke(width = item.borderWidth.toPx()))
                                    }
                                }
                                else -> {
                                    // Full + Outlined → bordi completi
                                    drawPath(path = path, color = item.borderColor, style = Stroke(width = item.borderWidth.toPx()))
                                }
                            }
                        }

                        is DrawItem.LineItem -> {
                            if (item.r0 == item.r1) {
                                val row = item.r0
                                val cMin = min(item.c0, item.c1)
                                val cMax = max(item.c0, item.c1)
                                val y = (row + 0.5f) * cell
                                val xStart = (cMin + 1).toFloat() * cell
                                val xEnd   = (cMax).toFloat() * cell
                                if (xEnd > xStart) {
                                    drawLine(
                                        color = item.color,
                                        start = Offset(xStart, y),
                                        end   = Offset(xEnd,   y),
                                        strokeWidth = item.width.toPx()
                                    )
                                }
                            } else if (item.c0 == item.c1) {
                                val col = item.c0
                                val rMin = min(item.r0, item.r1)
                                val rMax = max(item.r0, item.r1)
                                val x = (col + 0.5f) * cell
                                val yStart = (rMin + 1).toFloat() * cell
                                val yEnd   = (rMax).toFloat() * cell
                                if (yEnd > yStart) {
                                    drawLine(
                                        color = item.color,
                                        start = Offset(x, yStart),
                                        end   = Offset(x, yEnd),
                                        strokeWidth = item.width.toPx()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Evidenzia contenitore selezionato (se presente)
            selected?.let { sel ->
                val left = min(sel.c0, sel.c1).toFloat() * cell
                val top  = min(sel.r0, sel.r1).toFloat() * cell
                val w = (abs(sel.c1 - sel.c0) + 1).toFloat() * cell
                val h = (abs(sel.r1 - sel.r0) + 1).toFloat() * cell
                drawRect(
                    color = azure,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Overlay anteprima Grab/Resize
            (movingRect ?: resizingRect)?.let { prev ->
                val left = min(prev.c0, prev.c1).toFloat() * cell
                val top  = min(prev.r0, prev.r1).toFloat() * cell
                val w = (abs(prev.c1 - prev.c0) + 1).toFloat() * cell
                val h = (abs(prev.r1 - prev.r0) + 1).toFloat() * cell
                drawRect(
                    color = azure.copy(alpha = 0.12f),
                    topLeft = Offset(left, top),
                    size = Size(w, h)
                )
                drawRect(
                    color = azure,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Griglia (identica al tuo file)
            if (gridPreviewOnly) {
                if (rows > 0 && cols > 0) {
                    drawRect(color = azure.copy(alpha = 0.20f), topLeft = Offset(0f, 0f), size = Size(cell, cell))
                    drawRect(color = azure, topLeft = Offset(0f, 0f), size = Size(cell, cell), style = Stroke(width = 1.5.dp.toPx()))
                }
            } else if (showFullGrid) {
                for (c in 0..cols) {
                    val x = c.toFloat() * cell
                    drawLine(
                        color = azure.copy(alpha = 0.30f),
                        start = Offset(x, 0f),
                        end = Offset(x, rows.toFloat() * cell),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                for (r in 0..rows) {
                    val y = r.toFloat() * cell
                    drawLine(
                        color = azure.copy(alpha = 0.30f),
                        start = Offset(0f, y),
                        end = Offset(cols.toFloat() * cell, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Overlay Create (cella/righe/colonne) — invariato
            hoverCell?.let { (rr, cc) ->
                if (toolMode == ToolMode.Create && rr in 0 until rows && cc in 0 until cols) {
                    drawRect(color = azure.copy(alpha = 0.18f), topLeft = Offset(cc.toFloat() * cell, rr.toFloat() * cell), size = Size(cell, cell))
                    drawRect(color = azure, topLeft = Offset(cc.toFloat() * cell, rr.toFloat() * cell), size = Size(cell, cell), style = Stroke(width = 1.dp.toPx()))
                }
            }
            firstAnchor?.let { (rr, cc) ->
                if (toolMode == ToolMode.Create && rr >= 0 && cc >= 0) {
                    drawRect(color = azure.copy(alpha = 0.10f), topLeft = Offset(cc.toFloat() * cell, 0f), size = Size(cell, rows.toFloat() * cell))
                    drawRect(color = azure.copy(alpha = 0.10f), topLeft = Offset(0f, rr.toFloat() * cell), size = Size(cols.toFloat() * cell, cell))
                    drawRect(color = azure.copy(alpha = 0.22f), topLeft = Offset(cc.toFloat() * cell, rr.toFloat() * cell), size = Size(cell, cell))
                    drawRect(color = azure, topLeft = Offset(cc.toFloat() * cell, rr.toFloat() * cell), size = Size(cell, cell), style = Stroke(width = 1.5.dp.toPx()))
                }
            }
        }
    }
}

// Stato locale d’appoggio per Resize (angoli).
private var resizeFixedCorner: Pair<Int, Int>? by mutableStateOf(null)
private var resizeMovingCornerStart: Pair<Int, Int>? by mutableStateOf(null)

// Invariata
private fun computeCell(ofs: Offset, cols: Int, w: Float, h: Float): Pair<Int, Int> {
    val cell = min(w / cols, h / cols)
    val r = floor(ofs.y / cell).toInt().coerceAtLeast(0)
    val c = floor(ofs.x / cell).toInt().coerceAtLeast(0)
    return r to c
}
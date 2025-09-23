package com.example.appbuilder.editor


import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import com.example.appbuilder.icons.EditorIcons
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import java.util.UUID
import com.example.appbuilder.editor.icons.EditorIcons

/* ==========================================================
 *  # MODEL — document / nodes / styles (minimal v0)
 * ========================================================== */

enum class BgMode { Color, Gradient, Image, Album }
enum class Variant { Full, Outlined, Text, TopBottom }
enum class ShapeKind { Rect, RoundedRect, Circle }
enum class ScrollMode { None, Vertical, Horizontal }
enum class Behavior { Normal, Paged, Tabs }

data class PageStyle(
    val mode: BgMode = BgMode.Color,
    val color1: Color = Color(0xFFF2F2F2),
    val color2: Color = Color(0xFFECECEC),
    val gradientAngleDeg: Float = 0f,
    val image: Uri? = null,
    val album: List<Uri> = emptyList(),
    val pageFlipAnim: String = "none",
    val pageFlipSpeed: Float = 1.0f
)

data class BorderStyle(
    val color: Color = Color(0x33000000),
    val width: Dp = 1.dp,
    val shadow: Dp = 4.dp
)

data class CornerStyle(
    val topStart: Dp = 12.dp,
    val topEnd: Dp = 12.dp,
    val bottomStart: Dp = 12.dp,
    val bottomEnd: Dp = 12.dp
)

data class ContainerStyle(
    val name: String = "contenitore ${UUID.randomUUID().toString().take(4)}",
    val isDefault: Boolean = false,
    val variant: Variant = Variant.Full,
    val shape: ShapeKind = ShapeKind.RoundedRect,
    val corners: CornerStyle = CornerStyle(),
    val border: BorderStyle = BorderStyle(),
    val bgMode: BgMode = BgMode.Color,
    val color1: Color = Color.White,
    val color2: Color = Color(0xFFF6F6F6),
    val gradientAngleDeg: Float = 90f,
    val image: Uri? = null,
    val album: List<Uri> = emptyList(),
    val pageFlipAnim: String = "none",
    val pageFlipSpeed: Float = 1.0f,
    val scroll: ScrollMode = ScrollMode.None,
    val behavior: Behavior = Behavior.Normal,
    val pages: Int = 1,
    val tabsCount: Int = 0,
    val tabsShape: String = "underline",
)

data class GridRect(
    val c0: Int, val r0: Int,
    val c1: Int, val r1: Int
) {
    val left get() = min(c0, c1)
    val right get() = max(c0, c1)
    val top get() = min(r0, r1)
    val bottom get() = max(r0, r1)
}

sealed interface Node {
    val id: String
    val frame: GridRect
}

data class ContainerNode(
    override val id: String = "cont_" + UUID.randomUUID().toString().take(8),
    override val frame: GridRect,
    val style: ContainerStyle = ContainerStyle(),
    val children: List<Node> = emptyList()
) : Node

data class TextNode(
    override val id: String = "text_" + UUID.randomUUID().toString().take(8),
    override val frame: GridRect,
    val text: String = "Testo",
    val fontFamily: String? = null,
    val weight: FontWeight = FontWeight.Medium,
    val sizeSp: Float = 16f,
    val underline: Boolean = false,
    val highlight: Boolean = false,
    val highlightColor: Color = Color.Yellow.copy(alpha = 0.3f),
    val italic: Boolean = false,
    val color: Color = Color.Unspecified
) : Node

data class IconNode(
    override val id: String = "icon_" + UUID.randomUUID().toString().take(8),
    override val frame: GridRect,
    val color: Color = Color.Unspecified,
    val shadow: Dp = 0.dp,
    val border: BorderStyle = BorderStyle(color = Color.Transparent, width = 0.dp, shadow = 0.dp)
) : Node

data class ImageNode(
    override val id: String = "img_" + UUID.randomUUID().toString().take(8),
    override val frame: GridRect,
    val uri: Uri? = null,
    val fit: String = "cover",
    val crop: String? = null
) : Node

data class PageDocument(
    val gridCols: Int = 12,
    val gridRows: Int = 24,
    val style: PageStyle = PageStyle(),
    val nodes: List<Node> = emptyList()
)

data class EditorState(
    val doc: PageDocument = PageDocument(),
    val selection: String? = null,
    val isEditor: Boolean = true
)

/* ==========================================================
 *  # ENTRY DEMO — collega per provare
 * ========================================================== */

@Composable
private fun IconToggle(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val bw = if (selected) 2.dp else 1.dp
    Box(
        Modifier
            .size(40.dp)
            .border(bw, Color.White, CircleShape)
            .clip(CircleShape)
            .background(Color.Transparent)
            .padding(6.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}


@Composable
fun EditorDemoScreen() {
    var state by remember {
        mutableStateOf(
            EditorState(
                doc = PageDocument(
                    nodes = listOf(
                        ContainerNode(
                            frame = GridRect(1, 1, 12, 6),
                            style = ContainerStyle(name = "Hero", variant = Variant.Full)
                        ),
                        TextNode(frame = GridRect(2, 2, 6, 3), text = "Titolo pagina", sizeSp = 22f, weight = FontWeight.SemiBold),
                        ContainerNode(
                            frame = GridRect(2, 7, 11, 13),
                            style = ContainerStyle(name = "Card base", variant = Variant.Outlined, scroll = ScrollMode.Vertical),
                            children = listOf(
                                TextNode(frame = GridRect(1, 1, 5, 2), text = "Sezione", weight = FontWeight.Bold),
                                ImageNode(frame = GridRect(7, 1, 10, 4))
                            )
                        )
                    )
                )
            )
        )
    }

    EditorRoot(
        state = state,
        onStateChange = { state = it }
    )
}

/* ==========================================================
 *  # ROOT — tela + menù
 * ========================================================== */

@Composable
fun EditorRoot(
    state: EditorState,
    onStateChange: (EditorState) -> Unit
) {
    var menuPath by remember { mutableStateOf<List<String>>(emptyList()) } // es. ["Layout", "Colore"]
    var pathHint by remember { mutableStateOf<String?>(null) }             // es. "• Sottolinea: on"

    var workingPageStyle by remember { mutableStateOf(state.doc.style) }
    var workingContainerStyle by remember { mutableStateOf<ContainerStyle?>(null) }

    fun setSelection(id: String?) {
        onStateChange(state.copy(selection = id))
        workingContainerStyle = selectedContainer(state)?.style
    }

    // Wrapper: quando apro un macro‑menù (lista size = 1) azzero l’hint
    fun updateMenuPath(newPath: List<String>) {
        if (newPath.size <= 1) pathHint = null
        menuPath = newPath
    }

    Box(Modifier.fillMaxSize()) {
        // Canvas
        EditorCanvas(
            doc = state.doc,
            selection = state.selection,
            isEditor = state.isEditor,
            onSelect = { setSelection(it) },
            onAddContainerViaGrid = { rect ->
                val newNode = ContainerNode(frame = rect)
                onStateChange(
                    state.copy(
                        doc = state.doc.copy(nodes = state.doc.nodes + newNode),
                        selection = newNode.id
                    )
                )
                workingContainerStyle = newNode.style
                menuPath = listOf("Contenitore")
                pathDetail = null
            }
        )

        EditorBottomBars(
            state = state,
            menuPath = menuPath,
            onMenuPath = { updateMenuPath(it) },

            workingPageStyle = workingPageStyle,
            onWorkingPageStyle = { workingPageStyle = it },

            workingContainerStyle = workingContainerStyle,
            onWorkingContainerStyle = { workingContainerStyle = it },

            pathHint = pathHint,
            onHintChange = { pathHint = it },

            onApply = {
                val selectionId = state.selection
                if (menuPath.firstOrNull() == "Layout") {
                    onStateChange(state.copy(doc = state.doc.copy(style = workingPageStyle)))
                } else if (menuPath.firstOrNull() == "Contenitore" && selectionId != null) {
                    val updated = state.doc.nodes.map {
                        if (it.id == selectionId && it is ContainerNode && workingContainerStyle != null)
                            it.copy(style = workingContainerStyle!!)
                        else it
                    }
                    onStateChange(state.copy(doc = state.doc.copy(nodes = updated)))
                }
                menuPath = emptyList()
                pathHint = null
            },
            onCancel = {
                workingPageStyle = state.doc.style
                workingContainerStyle = selectedContainer(state)?.style
                menuPath = emptyList()
                pathHint = null
            },

            onDuplicate = { /* invariato */ },
            onDelete = { /* invariato */ }
        )
    }
}

/* ==========================================================
 *  # CANVAS — disegno pagina + griglia + nodi + selezione
 * ========================================================== */

@Composable
private fun EditorCanvas(
    doc: PageDocument,
    selection: String?,
    isEditor: Boolean,
    onSelect: (String?) -> Unit,
    onAddContainerViaGrid: (GridRect) -> Unit,
) {
    val density = LocalDensity.current
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    var startCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var hoveringCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { sizePx = it.size }
            .background(pageBrush(doc.style))
            .pointerInput(isEditor, doc.gridCols, doc.gridRows) {
                if (!isEditor) return@pointerInput
                detectTapGestures(
                    onTap = { pos ->
                        val (col, row) = posToCell(pos.x, pos.y, sizePx, doc.gridCols, doc.gridRows)
                        if (startCell == null) {
                            startCell = col to row
                            hoveringCell = col to row
                        } else {
                            val (c0, r0) = startCell!!
                            onAddContainerViaGrid(GridRect(c0, r0, col, row))
                            startCell = null
                            hoveringCell = null
                        }
                    },
                    onPress = { offset ->
                        if (startCell != null) return@detectTapGestures
                        val hit = hitTest(doc, sizePx, offset.x, offset.y)
                        onSelect(hit?.id)
                    }
                )
            }
    ) {
        // Griglia
        Canvas(Modifier.fillMaxSize()) {
            drawGrid(doc.gridCols, doc.gridRows, sizePx)
        }

        // Nodi
        doc.nodes.forEach { node ->
            val rectPx = cellRectPx(node.frame, sizePx, doc.gridCols, doc.gridRows)
            val sel = selection == node.id
            NodeView(node, rectPx, sel)
        }

        // Overlay sizing
        if (startCell != null) {
            val from = startCell!!
            val to = hoveringCell ?: from
            val rectPx = cellRectPx(GridRect(from.first, from.second, to.first, to.second), sizePx, doc.gridCols, doc.gridRows)
            Box(
                Modifier
                    .offset { IntOffset(rectPx.left, rectPx.top) }
                    .size(
                        with(density) { rectPx.width().toDp() },
                        with(density) { rectPx.height().toDp() }
                    )
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            )
        }
    }
}

private fun pageBrush(style: PageStyle): Brush {
    return when (style.mode) {
        BgMode.Color -> Brush.linearGradient(listOf(style.color1, style.color1))
        BgMode.Gradient -> {
            val colors = listOf(style.color1, style.color2)
            when ((style.gradientAngleDeg.toInt() % 360 + 360) % 360) {
                90 -> Brush.verticalGradient(colors)
                180 -> Brush.horizontalGradient(colors.reversed())
                270 -> Brush.verticalGradient(colors.reversed())
                else -> Brush.horizontalGradient(colors)
            }
        }
        BgMode.Image, BgMode.Album -> {
            Brush.linearGradient(listOf(Color(0xFF101010), Color(0xFF202020)))
        }
    }
}

private fun hitTest(doc: PageDocument, size: IntSize, x: Float, y: Float): Node? {
    return doc.nodes.asReversed().firstOrNull { node ->
        val r = cellRectPx(node.frame, size, doc.gridCols, doc.gridRows)
        x >= r.left && x <= r.right && y >= r.top && y <= r.bottom
    }
}

private fun cellRectPx(frame: GridRect, size: IntSize, cols: Int, rows: Int): android.graphics.Rect {
    val cellW = size.width / cols.toFloat()
    val cellH = size.height / rows.toFloat()
    val left = ((frame.left - 1) * cellW).toInt()
    val top = ((frame.top - 1) * cellH).toInt()
    val right = (frame.right * cellW).toInt()
    val bottom = (frame.bottom * cellH).toInt()
    return android.graphics.Rect(left, top, right, bottom)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(cols: Int, rows: Int, size: IntSize) {
    val cw = size.width / cols.toFloat()
    val rh = size.height / rows.toFloat()
    val gridColor = Color.White.copy(alpha = 0.12f)
    for (c in 1 until cols) {
        val x = cw * c
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height.toFloat()), strokeWidth = 1f)
    }
    for (r in 1 until rows) {
        val y = rh * r
        drawLine(gridColor, Offset(0f, y), Offset(size.width.toFloat(), y), strokeWidth = 1f)
    }
}

/* ==========================================================
 *  # NODE VIEW
 * ========================================================== */

@Composable
private fun BoxScope.NodeView(node: Node, rect: android.graphics.Rect, selected: Boolean) {
    val density = LocalDensity.current
    val width = with(density) { rect.width().toDp() }
    val height = with(density) { rect.height().toDp() }
    val offset = IntOffset(rect.left, rect.top)

    Box(
        Modifier
            .offset { offset }
            .size(width, height)
    ) {
        when (node) {
            is ContainerNode -> {
                val shape = when (node.style.shape) {
                    ShapeKind.Rect -> RoundedCornerShape(0.dp)
                    ShapeKind.RoundedRect -> RoundedCornerShape(
                        node.style.corners.topStart,
                        node.style.corners.topEnd,
                        node.style.corners.bottomEnd,
                        node.style.corners.bottomStart
                    )
                    ShapeKind.Circle -> CircleShape
                }

                val bg = when (node.style.bgMode) {
                    BgMode.Color -> Brush.linearGradient(listOf(node.style.color1, node.style.color1))
                    BgMode.Gradient -> Brush.horizontalGradient(listOf(node.style.color1, node.style.color2))
                    BgMode.Image, BgMode.Album ->
                        Brush.linearGradient(listOf(Color(0xFF2C2C2C), Color(0xFF111111)))
                }

                val base = when (node.style.variant) {
                    Variant.Text -> Modifier
                    Variant.TopBottom -> Modifier.drawBehind {
                        val h = size.height * 0.12f
                        drawRect(
                            color = node.style.color1,
                            size = androidx.compose.ui.geometry.Size(size.width, h)
                        )
                        drawRect(
                            color = node.style.color2,
                            topLeft = Offset(0f, size.height - h),
                            size = androidx.compose.ui.geometry.Size(size.width, h)
                        )
                    }
                    else -> Modifier.background(bg, shape)
                }

                val borderModifier =
                    if (node.style.variant == Variant.Outlined || node.style.border.width > 0.dp)
                        Modifier.border(node.style.border.width, node.style.border.color, shape)
                    else Modifier

                OutlinedCard(
                    shape = shape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = base.then(borderModifier).fillMaxSize()
                ) { }
            }

            is TextNode -> {
                val bgHighlight = if (node.highlight) node.highlightColor else Color.Transparent
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(bgHighlight, RoundedCornerShape(6.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = node.text,
                        color = if (node.color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else node.color,
                        fontSize = node.sizeSp.sp,
                        fontWeight = node.weight
                    )
                }
            }

            is ImageNode -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x22000000), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x33000000), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(EditorIcons.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            is IconNode -> {
                Box(
                    Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = if (node.color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else node.color)
                }
            }
        }

        if (selected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            )
        }
    }
}

/* ==========================================================
 *  # BOTTOM BARS — breadcrumb + menu corrente + azioni
 * ========================================================== */

@Composable
private fun BoxScope.EditorBottomBars(
    state: EditorState,
    menuPath: List<String>,
    onMenuPath: (List<String>) -> Unit,

    workingPageStyle: PageStyle,
    onWorkingPageStyle: (PageStyle) -> Unit,

    workingContainerStyle: ContainerStyle?,
    onWorkingContainerStyle: (ContainerStyle?) -> Unit,

    pathHint: String?,                    // <— NOVITÀ
    onHintChange: (String?) -> Unit,      // <— NOVITÀ

    onApply: () -> Unit,
    onCancel: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var actionsHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val actionsHeightDp = with(density) { actionsHeightPx.toDp() }

    // Barra comandi (breadcrumb + dup/del + ok/annulla)
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .onGloballyPositioned { actionsHeightPx = it.size.height }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val base = if (menuPath.isEmpty())
                "Seleziona un menù (Layout / Contenitore / Testo / Immagine / Inserisci)"
            else
                menuPath.joinToString("  →  ")

            val bread = if (pathHint.isNullOrBlank()) base else "$base  •  $pathHint"

            Text(bread, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDuplicate, enabled = state.selection != null) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Duplica", tint = Color.White)
                }
                IconButton(onClick = onDelete, enabled = state.selection != null) {
                    Icon(Icons.Filled.Delete, contentDescription = "Elimina", tint = Color.White)
                }
                TextButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "Annulla", tint = Color.White)
                    Spacer(Modifier.width(4.dp)); Text("Annulla")
                }
                Button(onClick = onApply) {
                    Icon(Icons.Filled.Check, contentDescription = "OK")
                    Spacer(Modifier.width(4.dp)); Text("OK")
                }
            }
        }
    }

    // Barra menù corrente (sopra quella comandi, stessa “area” di sempre)
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 12.dp, bottom = actionsHeightDp + 8.dp)
    ) {
        val scroll = rememberScrollState()
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (menuPath.isEmpty()) {
                // SOLO QUI: sostituzione icona Layout con icona pagina
                ElevatedFilterChip(
                    selected = false,
                    onClick = { onMenuPath(listOf("Layout")) },
                    label = { Text("Layout") },
                    leadingIcon = { Icon(EditorIcons.Page, contentDescription = null, tint = Color.White) }
                )
                ElevatedFilterChip(
                    selected = false,
                    onClick = { onMenuPath(listOf("Contenitore")) },
                    label = { Text("Contenitore") },
                    leadingIcon = { Icon(EditorIcons.Container, contentDescription = null, tint = Color.White) }
                )
                ElevatedFilterChip(
                    selected = false,
                    onClick = { onMenuPath(listOf("Testo")) },
                    label = { Text("Testo") },
                    leadingIcon = { Icon(Icons.Filled.TextFields, contentDescription = null, tint = Color.White) }
                )
                ElevatedFilterChip(
                    selected = false,
                    onClick = { onMenuPath(listOf("Immagine")) },
                    label = { Text("Immagine") },
                    leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null, tint = Color.White) }
                )
                ElevatedFilterChip(
                    selected = false,
                    onClick = { onMenuPath(listOf("Inserisci")) },
                    label = { Text("Aggiungi") },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White) }
                )
            } else {
                when (menuPath.first()) {
                    "Layout" -> LayoutMenu(menuPath, onMenuPath, workingPageStyle, onWorkingPageStyle, onHintChange)
                    "Contenitore" -> ContainerMenu(menuPath, onMenuPath, workingContainerStyle, onWorkingContainerStyle, onHintChange)
                    "Testo" -> TextMenu(menuPath, onMenuPath, onHintChange)
                    "Immagine" -> ImageMenu(menuPath, onMenuPath, onHintChange)
                    "Inserisci" -> InsertMenu(onMenuPath) // nessun hint qui
                }
            }
        }
    }
}


/* -------------------------
 *  LAYOUT MENU (pagina)
 * ------------------------- */

@Composable
private fun LayoutMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    working: PageStyle,
    onWorking: (PageStyle) -> Unit,
    onHintChange: (String?) -> Unit     // <— aggiunto
) {
    when (path.getOrNull(1)) {
        null -> {
            ElevatedFilterChip(
                selected = false,
                onClick = { onPath(path + "Colore") },
                label = { Text("Colore") },
                leadingIcon = { Icon(EditorIcons.Palette1, null, tint = Color.White) }
            )
            ElevatedFilterChip(
                selected = false,
                onClick = { onPath(path + "Immagini") },
                label = { Text("Immagini") },
                leadingIcon = { Icon(Icons.Filled.Image, null, tint = Color.White) }
            )
        }
        "Colore" -> {
            FilterChip(
                selected = working.mode == BgMode.Color,
                onClick = { onWorking(working.copy(mode = BgMode.Color)); onHintChange("Colore: singolo") },
                label = { Text("Colore singolo") }
            )
            FilterChip(
                selected = working.mode == BgMode.Gradient,
                onClick = { onWorking(working.copy(mode = BgMode.Gradient)); onHintChange("Colore: gradiente") },
                label = { Text("Gradiente") },
                leadingIcon = { Icon(EditorIcons.Gradient, null, tint = Color.White) }
            )
            listOf(Color(0xFF0EA5E9), Color(0xFF9333EA), Color(0xFFEF4444), Color(0xFF10B981)).forEach { c ->
                ColorDot(c) { onWorking(working.copy(color1 = c)); onHintChange("Colore 1") }
            }
        }
        "Immagini" -> {
            val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) { onWorking(working.copy(mode = BgMode.Image, image = uri)); onHintChange("Immagine: selezionata") }
            }
            val pickAlbum = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                if (uris.isNotEmpty()) { onWorking(working.copy(mode = BgMode.Album, album = uris)); onHintChange("Album: ${uris.size} foto") }
            }
            OutlinedButton(onClick = { pickImage.launch("image/*") }) { Icon(Icons.Filled.Crop, null); Spacer(Modifier.width(6.dp)); Text("Foto") }
            OutlinedButton(onClick = { pickAlbum.launch("image/*") }) { Icon(Icons.Filled.Collections, null); Spacer(Modifier.width(6.dp)); Text("Album") }
        }
    }
}


/* -------------------------
 *  CONTAINER MENU (placeholder + esempi)
 * ------------------------- */

@Composable
private fun ContainerMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    working: ContainerStyle?,
    onWorking: (ContainerStyle?) -> Unit,
    onHintChange: (String?) -> Unit    // <— aggiunto
) {
    val w = working ?: ContainerStyle().also { onWorking(it) }

    when (path.getOrNull(1)) {
        null -> {
            ElevatedFilterChip(false, { onPath(path + "Colore") }, { Text("Colore") },
                leadingIcon = { Icon(EditorIcons.Palette1, null, tint = Color.White) })
            ElevatedFilterChip(false, { onPath(path + "Immagini") }, { Text("Immagini") },
                leadingIcon = { Icon(Icons.Filled.Image, null, tint = Color.White) })
            ElevatedFilterChip(false, { onPath(path + "Scroll") }, { Text("Scrollabilità") })
            ElevatedFilterChip(false, { onPath(path + "Forma") }, { Text("Forma/Angoli") },
                leadingIcon = { Icon(EditorIcons.ShapeSquare, null, tint = Color.White) })
            ElevatedFilterChip(false, { onPath(path + "Stile") }, { Text("Stile") },
                leadingIcon = { Icon(EditorIcons.Variant, null, tint = Color.White) })
            ElevatedFilterChip(false, { onPath(path + "Bordi") }, { Text("Bordi/Ombra") },
                leadingIcon = { Icon(EditorIcons.BorderThick, null, tint = Color.White) })
            ElevatedFilterChip(false, { onPath(path + "Comportamento") }, { Text("Tipo") },
                leadingIcon = { Icon(EditorIcons.Type, null, tint = Color.White) })
            ElevatedFilterChip(false, { onPath(path + "Azioni") }, { Text("Azioni (stub)") })
        }

        "Colore" -> {
            FilterChip(w.bgMode == BgMode.Color, { onWorking(w.copy(bgMode = BgMode.Color)); onHintChange("Colore: singolo") }, { Text("Colore") })
            FilterChip(w.bgMode == BgMode.Gradient, { onWorking(w.copy(bgMode = BgMode.Gradient)); onHintChange("Colore: gradiente") }, { Text("Gradiente") },
                leadingIcon = { Icon(EditorIcons.Gradient, null, tint = Color.White) })
            listOf(Color(0xFFFFFFFF), Color(0xFFF3F4F6), Color(0xFF111827), Color(0xFF0EA5E9)).forEach {
                ColorDot(it) { c -> onWorking(w.copy(color1 = c)); onHintChange("Colore 1") }
            }
        }

        // (resto invariato, aggiungi onHintChange con stringhe sintetiche dove utile)
        // Esempio:
        "Stile" -> {
            FilterChip(w.variant == Variant.Full, { onWorking(w.copy(variant = Variant.Full)); onHintChange("Stile: full") }, { Text("Full") })
            FilterChip(w.variant == Variant.Outlined, { onWorking(w.copy(variant = Variant.Outlined)); onHintChange("Stile: outlined") }, { Text("Outlined") })
            FilterChip(w.variant == Variant.Text, { onWorking(w.copy(variant = Variant.Text)); onHintChange("Stile: text") }, { Text("Text") })
            FilterChip(w.variant == Variant.TopBottom, { onWorking(w.copy(variant = Variant.TopBottom)); onHintChange("Stile: topbottom") }, { Text("TopBottom") })
        }
    }
}


/* -------------------------
 *  TESTO MENU
 * ------------------------- */
@Composable
private fun TextMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    onHintChange: (String?) -> Unit      // <— aggiunto
) {
    var underline by remember { mutableStateOf(false) }
    var italic by remember { mutableStateOf(false) }

    IconToggle(
        selected = underline,
        icon = EditorIcons.TextUnderline,
        onClick = { underline = !underline; onHintChange("Sottolinea: " + if (underline) "on" else "off") }
    )
    IconToggle(
        selected = italic,
        icon = EditorIcons.TextItalic,
        onClick = { italic = !italic; onHintChange("Corsivo: " + if (italic) "on" else "off") }
    )

    IconButton(onClick = { onHintChange("Evidenzia") }) { Icon(EditorIcons.TextHighlight, null, tint = Color.White) }
    IconButton(onClick = { onHintChange("Font") })       { Icon(EditorIcons.Font, null, tint = Color.White) }
    IconButton(onClick = { onHintChange("Weight") })     { Icon(EditorIcons.Weight, null, tint = Color.White) }
    IconButton(onClick = { onHintChange("Size") })       { Icon(EditorIcons.Size, null, tint = Color.White) }
    IconButton(onClick = { onHintChange("Colore testo") }) { Icon(EditorIcons.TextColor, null, tint = Color.White) }
}


/* -------------------------
 *  IMMAGINE MENU (placeholder)
 * ------------------------- */
@Composable
private fun ImageMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    onHintChange: (String?) -> Unit      // <— aggiunto
) {
    FilterChip(false, { onHintChange("Crop") }, { Text("Crop (prossimo step)") }, leadingIcon = { Icon(Icons.Filled.Crop, null) })
    FilterChip(false, { onHintChange("Cornice") }, { Text("Cornice") })
    FilterChip(false, { onHintChange("Album") }, { Text("Album") }, leadingIcon = { Icon(Icons.Filled.Collections, null) })
    FilterChip(false, { onHintChange("Adattamento") }, { Text("Adattamento") })
}


/* -------------------------
 *  INSERISCI (hint)
 * ------------------------- */
@Composable
private fun InsertMenu(
    onPath: (List<String>) -> Unit,
    onPathDetail: (String?) -> Unit
) {
    // Suggerimento
    Text("Tocca due celle della griglia per creare un Contenitore.", color = Color.White)
}

/* ==========================================================
 *  # WIDGET MENU
 * ========================================================== */

@Composable
private fun PillToggleIcon(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(if (selected) 2.dp else 1.dp, Color.White),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun DropdownIconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    options: List<String>,
    onExpand: (current: String) -> Unit,
    onSelect: (choice: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf(options.firstOrNull() ?: "") }

    Box {
        AssistChip(
            onClick = {
                expanded = true
                onExpand(current)
            },
            label = { Text("$label: $current") },
            leadingIcon = { Icon(icon, null, tint = Color.White) },
            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1A2233), labelColor = Color.White)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        current = opt
                        expanded = false
                        onSelect(opt)
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color, onPick: (Color) -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .background(color, CircleShape)
            .border(1.dp, Color(0x33000000), CircleShape)
            .padding(2.dp)
            .pointerInput(Unit) {
                detectTapGestures { onPick(color) }
            }
    )
    Spacer(Modifier.width(4.dp))
}

/* ==========================================================
 *  # UTILITY
 * ========================================================== */

private fun posToCell(x: Float, y: Float, size: IntSize, cols: Int, rows: Int): Pair<Int, Int> {
    val cw = size.width / cols.toFloat()
    val rh = size.height / rows.toFloat()
    val col = min(cols, max(1, (x / cw + 1).toInt()))
    val row = min(rows, max(1, (y / rh + 1).toInt()))
    return col to row
}

private fun selectedContainer(state: EditorState): ContainerNode? =
    state.selection?.let { sel -> state.doc.nodes.firstOrNull { it.id == sel } as? ContainerNode }

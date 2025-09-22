package com.example.appbuilder.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import kotlin.math.max
import kotlin.math.min
import java.util.UUID

/* ==========================================================
 *  MODEL — document / nodes / styles (base v0)
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

data class GridRect(val c0: Int, val r0: Int, val c1: Int, val r1: Int) {
    val left get() = min(c0, c1)
    val right get() = max(c0, c1)
    val top get() = min(r0, r1)
    val bottom get() = max(r0, r1)
}

sealed interface Node { val id: String; val frame: GridRect }

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
    val fit: String = "cover", // contain / cover / fill / fitWidth / fitHeight
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
 *  DEMO ENTRY
 * ========================================================== */

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

    EditorRoot(state = state, onStateChange = { state = it })
}

/* ==========================================================
 *  ROOT — tela + menù
 * ========================================================== */

@Composable
fun EditorRoot(
    state: EditorState,
    onStateChange: (EditorState) -> Unit
) {
    var menuPath by remember { mutableStateOf<List<String>>(emptyList()) } // es. ["Layout","Colore"]
    var lastEditedLeaf by remember { mutableStateOf<String?>(null) }       // es. "Gradiente: Verticale"
    var workingPageStyle by remember { mutableStateOf(state.doc.style) }
    var workingContainerStyle by remember { mutableStateOf<ContainerStyle?>(null) }
    var hasPendingChanges by remember { mutableStateOf(false) }
    var showConfirmBar by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }

    fun setSelection(id: String?) {
        onStateChange(state.copy(selection = id))
        workingContainerStyle = selectedContainer(state)?.style
    }

    fun applyChanges() {
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
        hasPendingChanges = false
    }

    fun exitCurrentMenu() {
        if (hasPendingChanges) {
            showConfirmBar = true
        } else {
            menuPath = emptyList()
            lastEditedLeaf = null
        }
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
            }
        )

        // STRISCIA MODALITÀ (sopra la command bar) — solo quando NON siamo in sottomenu
        if (menuPath.isEmpty()) {
            ModeStrip(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(horizontal = 12.dp)
                    .offset { IntOffset(0, -96) }, // sta sopra la command bar
                onOpen = { root ->
                    // apri sottomenu → nascondi command bar
                    menuPath = listOf(root)
                }
            )
        }

        // BARRA COMANDI (inferiore, icone bianche) — visibile solo a root
        if (menuPath.isEmpty()) {
            CommandBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                onUndo = { /* stub */ },
                onRedo = { /* stub */ },
                onSavePage = { /* stub */ },
                onDelete = {
                    state.selection?.let { sel ->
                        onStateChange(
                            state.copy(
                                doc = state.doc.copy(nodes = state.doc.nodes.filterNot { it.id == sel }),
                                selection = null
                            )
                        )
                    }
                },
                onDuplicate = {
                    state.selection?.let { sel ->
                        val found = state.doc.nodes.find { it.id == sel }
                        if (found != null) {
                            val clone = when (found) {
                                is ContainerNode -> found.copy(id = "cont_" + UUID.randomUUID().toString().take(8))
                                is TextNode -> found.copy(id = "text_" + UUID.randomUUID().toString().take(8))
                                is ImageNode -> found.copy(id = "img_" + UUID.randomUUID().toString().take(8))
                                is IconNode -> found.copy(id = "icon_" + UUID.randomUUID().toString().take(8))
                                else -> found
                            }
                            onStateChange(state.copy(doc = state.doc.copy(nodes = state.doc.nodes + clone)))
                        }
                    }
                },
                onProperties = { menuPath = listOf("Contenitore") },
                onPageLayout = { menuPath = listOf("Layout") },
                onCreate = { /* drop-down stub */ },
                onList = { /* drop-down stub */ },
                onOk = { /* in root non fa nulla */ },
                onSaveProject = { /* stub */ },
                onOpen = { /* stub */ },
                onNewProject = { /* stub */ }
            )
        }

        // MENU CORRENTE (contenuto scorrevole con icone/chips) — visibile quando siamo in sottomenu
        if (menuPath.isNotEmpty()) {
            CurrentMenuScroller(
                path = menuPath,
                onPath = { menuPath = it },
                onBack = { exitCurrentMenu() },
                workingPageStyle = workingPageStyle,
                onWorkingPageStyle = {
                    workingPageStyle = it
                    hasPendingChanges = true
                },
                workingContainerStyle = workingContainerStyle,
                onWorkingContainerStyle = {
                    workingContainerStyle = it
                    hasPendingChanges = true
                },
                onLeafChanged = { leaf -> lastEditedLeaf = leaf }
            )
        }

        // PATH BAR (testo) — appare al posto della command bar quando sei in sottomenu
        if (menuPath.isNotEmpty()) {
            PathBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                pathText = buildString {
                    append(menuPath.joinToString("  →  "))
                    lastEditedLeaf?.let { append("  ·  "); append(it) }
                }
            )
        }

        // BARRA DI CONFERMA (quando lasci sottomenù con modifiche)
        if (showConfirmBar) {
            ConfirmBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                onCancel = {
                    // scarta modifiche
                    workingPageStyle = state.doc.style
                    workingContainerStyle = selectedContainer(state)?.style
                    hasPendingChanges = false
                    showConfirmBar = false
                    menuPath = emptyList()
                    lastEditedLeaf = null
                },
                onOk = {
                    applyChanges()
                    showConfirmBar = false
                    menuPath = emptyList()
                    lastEditedLeaf = null
                },
                onSavePreset = { showSavePresetDialog = true }
            )
        }

        // DIALOG SALVA PRESET (stub)
        if (showSavePresetDialog) {
            SavePresetDialog(
                onDismiss = { showSavePresetDialog = false },
                onSave = { _name ->
                    // TODO: salva preset (stub)
                    showSavePresetDialog = false
                }
            )
        }
    }
}

/* ==========================================================
 *  CANVAS — pagina + griglia + nodi
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
                    .size(with(density) { rectPx.width().toDp() }, with(density) { rectPx.height().toDp() })
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            )
        }
    }
}

private fun pageBrush(style: PageStyle): Brush =
    when (style.mode) {
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
        BgMode.Image, BgMode.Album -> Brush.linearGradient(listOf(Color(0xFF101010), Color(0xFF202020)))
    }

private fun hitTest(doc: PageDocument, size: IntSize, x: Float, y: Float): Node? =
    doc.nodes.asReversed().firstOrNull { node ->
        val r = cellRectPx(node.frame, size, doc.gridCols, doc.gridRows)
        x >= r.left && x <= r.right && y >= r.top && y <= r.bottom
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
 *  NODE VIEW — minimo rendering
 * ========================================================== */

@Composable
private fun BoxScope.NodeView(node: Node, rect: android.graphics.Rect, selected: Boolean) {
    val width = with(LocalDensity.current) { rect.width().toDp() }
    val height = with(LocalDensity.current) { rect.height().toDp() }
    val offset = IntOffset(rect.left, rect.top)

    Box(Modifier.offset { offset }.size(width, height)) {
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
                    BgMode.Image, BgMode.Album -> Brush.linearGradient(listOf(Color(0xFF2C2C2C), Color(0xFF111111)))
                }
                val base = when (node.style.variant) {
                    Variant.Text -> Modifier
                    Variant.TopBottom -> Modifier.drawBehind {
                        val h = size.height * 0.12f
                        drawRect(node.style.color1, size = androidx.compose.ui.geometry.Size(size.width, h))
                        drawRect(node.style.color2, topLeft = Offset(0f, size.height - h), size = androidx.compose.ui.geometry.Size(size.width, h))
                    }
                    else -> Modifier.background(bg, shape)
                }
                val borderMod =
                    if (node.style.variant == Variant.Outlined || node.style.border.width > 0.dp)
                        Modifier.border(node.style.border.width, node.style.border.color, shape)
                    else Modifier

                OutlinedCard(
                    shape = shape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = base.then(borderMod).fillMaxSize()
                ) { /* child placeholder */ }
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
                    Icon(Icons.Filled.Image, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            is IconNode -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Settings, null, tint = if (node.color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else node.color)
                }
            }
        }
        if (selected) {
            Box(Modifier.fillMaxSize().border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)))
        }
    }
}

/* ==========================================================
 *  BARRE & MENU
 * ========================================================== */

@Composable
private fun CommandBar(
    modifier: Modifier = Modifier,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSavePage: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onProperties: () -> Unit,
    onPageLayout: () -> Unit,
    onCreate: () -> Unit,
    onList: () -> Unit,
    onOk: () -> Unit,
    onSaveProject: () -> Unit,
    onOpen: () -> Unit,
    onNewProject: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = Color(0xFF0F172A), // dark (tipo GitHub)
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                WhiteIcon(onClick = onUndo, icon = Icons.Filled.Undo)
                WhiteIcon(onClick = onRedo, icon = Icons.Filled.Redo)
                WhiteIcon(onClick = onSavePage, icon = Icons.Filled.Save)
                WhiteIcon(onClick = onDelete, icon = Icons.Filled.Delete)
                WhiteIcon(onClick = onDuplicate, icon = Icons.Filled.ContentCopy)
                WhiteIcon(onClick = onProperties, icon = Icons.Filled.Settings)
                WhiteIcon(onClick = onPageLayout, icon = Icons.Filled.ViewAgenda)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                WhiteIcon(onClick = onCreate, icon = Icons.Filled.AddCircle)
                WhiteIcon(onClick = onList, icon = Icons.Filled.List)
                WhiteIcon(onClick = onOk, icon = Icons.Filled.CheckCircle)
                WhiteIcon(onClick = onSaveProject, icon = Icons.Filled.SaveAs)
                WhiteIcon(onClick = onOpen, icon = Icons.Filled.FolderOpen)
                WhiteIcon(onClick = onNewProject, icon = Icons.Filled.CreateNewFolder)
            }
        }
    }
}

@Composable
private fun ModeStrip(
    modifier: Modifier = Modifier,
    onOpen: (root: String) -> Unit
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF111827),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WhiteIcon(onClick = { onOpen("Layout") }, icon = Icons.Filled.ViewAgenda)
            WhiteIcon(onClick = { onOpen("Contenitore") }, icon = Icons.Filled.Widgets)
            WhiteIcon(onClick = { onOpen("Testo") }, icon = Icons.Filled.TextFields)
            WhiteIcon(onClick = { onOpen("Aggiungi") }, icon = Icons.Filled.Add)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrentMenuScroller(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    onBack: () -> Unit,
    workingPageStyle: PageStyle,
    onWorkingPageStyle: (PageStyle) -> Unit,
    workingContainerStyle: ContainerStyle?,
    onWorkingContainerStyle: (ContainerStyle?) -> Unit,
    onLeafChanged: (String) -> Unit
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF111827),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .imePadding()
            .offset { IntOffset(0, -100) } // sopra la (ipotetica) path bar
    ) {
        val scroll = rememberScrollState()
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // back
            WhiteIcon(onClick = onBack, icon = Icons.Filled.ArrowBack)

            Spacer(Modifier.width(8.dp))

            when (path.firstOrNull()) {
                "Layout" -> LayoutMenu(
                    path = path,
                    onPath = {
                        onPath(it)
                    },
                    working = workingPageStyle,
                    onWorking = {
                        onWorkingPageStyle(it)
                        onLeafChanged(describePageLeaf(path, it))
                    }
                )
                "Contenitore" -> ContainerMenu(
                    path = path,
                    onPath = { onPath(it) },
                    working = workingContainerStyle,
                    onWorking = {
                        onWorkingContainerStyle(it)
                        it?.let { s -> onLeafChanged(describeContainerLeaf(path, s)) }
                    }
                )
                "Testo" -> TextMenu(
                    path = path,
                    onPath = { onPath(it) },
                    onLeafChanged = onLeafChanged
                )
                "Aggiungi" -> InsertMenu()
            }
        }
    }
}

@Composable
private fun PathBar(modifier: Modifier = Modifier, pathText: String) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = Color(0xFF0F172A),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { Text(pathText, color = Color.White, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun ConfirmBar(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onOk: () -> Unit,
    onSavePreset: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = Color(0xFF0F172A),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                WhiteIcon(onClick = onCancel, icon = Icons.Filled.Close)
                Spacer(Modifier.width(8.dp))
                WhiteIcon(onClick = onOk, icon = Icons.Filled.Check)
            }
            OutlinedButton(
                onClick = onSavePreset,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Filled.Save, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Salva impostazioni", color = Color.White)
            }
        }
    }
}

/* ==========================================================
 *  MENU: LAYOUT / CONTENITORE / TESTO / AGGIUNGI
 * ========================================================== */

@Composable
private fun LayoutMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    working: PageStyle,
    onWorking: (PageStyle) -> Unit
) {
    when (path.getOrNull(1)) {
        null -> {
            WhiteChip(onClick = { onPath(path + "Colore") }, icon = Icons.Filled.ColorLens)
            WhiteChip(onClick = { onPath(path + "Immagini") }, icon = Icons.Filled.Image)
        }
        "Colore" -> {
            // scelta modalità
            ToggleIcon(
                selected = working.mode == BgMode.Color,
                onClick = { onWorking(working.copy(mode = BgMode.Color)) },
                icon = Icons.Filled.InvertColors
            )
            ToggleIcon(
                selected = working.mode == BgMode.Gradient,
                onClick = { onWorking(working.copy(mode = BgMode.Gradient)) },
                icon = Icons.Filled.Gradient
            )
            Spacer(Modifier.width(8.dp))
            // palette rapida (colore1)
            listOf(0xFF0EA5E9, 0xFF9333EA, 0xFFEF4444, 0xFF10B981).forEach { hex ->
                ColorDot(Color(hex)) { c -> onWorking(working.copy(color1 = c)) }
            }
            // effetti carini (demo: solo toggle icona con pallino)
            Spacer(Modifier.width(8.dp))
            ToggleIcon(selected = false, onClick = { /* stub effetto */ }, icon = Icons.Filled.BlurOn)
            ToggleIcon(selected = false, onClick = { /* stub effetto */ }, icon = Icons.Filled.AutoAwesome)
        }
        "Immagini" -> {
            val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) onWorking(working.copy(mode = BgMode.Image, image = uri))
            }
            val pickAlbum = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                if (uris.isNotEmpty()) onWorking(working.copy(mode = BgMode.Album, album = uris))
            }
            WhiteChip(onClick = { pickImage.launch("image/*") }, icon = Icons.Filled.Crop)   // crop step successivo
            WhiteChip(onClick = { pickAlbum.launch("image/*") }, icon = Icons.Filled.Collections)
        }
    }
}

@Composable
private fun ContainerMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    working: ContainerStyle?,
    onWorking: (ContainerStyle?) -> Unit
) {
    val w = working ?: ContainerStyle().also { onWorking(it) }

    when (path.getOrNull(1)) {
        null -> {
            WhiteChip({ onPath(path + "Colore") }, Icons.Filled.ColorLens)
            WhiteChip({ onPath(path + "Immagini") }, Icons.Filled.Image)
            WhiteChip({ onPath(path + "Scroll") }, Icons.Filled.Swipe)
            WhiteChip({ onPath(path + "Forma") }, Icons.Filled.CropSquare)
            WhiteChip({ onPath(path + "Stile") }, Icons.Filled.Style)
            WhiteChip({ onPath(path + "Bordi") }, Icons.Filled.BorderAll)
            WhiteChip({ onPath(path + "Comportamento") }, Icons.Filled.Tabs)
            WhiteChip({ onPath(path + "Azioni") }, Icons.Filled.TouchApp)
        }

        "Colore" -> {
            ToggleIcon(w.bgMode == BgMode.Color, { onWorking(w.copy(bgMode = BgMode.Color)) }, Icons.Filled.InvertColors)
            ToggleIcon(w.bgMode == BgMode.Gradient, { onWorking(w.copy(bgMode = BgMode.Gradient)) }, Icons.Filled.Gradient)
            Spacer(Modifier.width(8.dp))
            listOf(Color.White, Color(0xFFF3F4F6), Color(0xFF111827), Color(0xFF0EA5E9)).forEach {
                ColorDot(it) { c -> onWorking(w.copy(color1 = c)) }
            }
        }

        "Immagini" -> {
            val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) onWorking(w.copy(bgMode = BgMode.Image, image = uri))
            }
            val pickAlbum = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                if (uris.isNotEmpty()) onWorking(w.copy(bgMode = BgMode.Album, album = uris))
            }
            WhiteChip(onClick = { pickImage.launch("image/*") }, icon = Icons.Filled.Crop)
            WhiteChip(onClick = { pickAlbum.launch("image/*") }, icon = Icons.Filled.Collections)
        }

        "Scroll" -> {
            ToggleIcon(w.scroll == ScrollMode.None, { onWorking(w.copy(scroll = ScrollMode.None)) }, Icons.Filled.Block)
            ToggleIcon(w.scroll == ScrollMode.Vertical, { onWorking(w.copy(scroll = ScrollMode.Vertical)) }, Icons.Filled.SwapVert)
            ToggleIcon(w.scroll == ScrollMode.Horizontal, { onWorking(w.copy(scroll = ScrollMode.Horizontal)) }, Icons.Filled.SwapHoriz)
        }

        "Forma" -> {
            ToggleIcon(w.shape == ShapeKind.Rect, { onWorking(w.copy(shape = ShapeKind.Rect)) }, Icons.Filled.CropSquare)
            ToggleIcon(w.shape == ShapeKind.RoundedRect, { onWorking(w.copy(shape = ShapeKind.RoundedRect)) }, Icons.Filled.Crop32)
            ToggleIcon(w.shape == ShapeKind.Circle, { onWorking(w.copy(shape = ShapeKind.Circle)) }, Icons.Filled.Circle)
            if (w.shape != ShapeKind.Circle) {
                Spacer(Modifier.width(8.dp))
                listOf(0.dp, 8.dp, 12.dp, 16.dp, 24.dp).forEach { v ->
                    OutlinePill(text = "${v.value.toInt()}dp") {
                        onWorking(w.copy(corners = w.corners.copy(topStart = v, topEnd = v, bottomStart = v, bottomEnd = v)))
                    }
                }
            }
        }

        "Stile" -> {
            ToggleIcon(w.variant == Variant.Full, { onWorking(w.copy(variant = Variant.Full)) }, Icons.Filled.Layers)
            ToggleIcon(w.variant == Variant.Outlined, { onWorking(w.copy(variant = Variant.Outlined)) }, Icons.Filled.BorderClear)
            ToggleIcon(w.variant == Variant.Text, { onWorking(w.copy(variant = Variant.Text)) }, Icons.Filled.TextFields)
            ToggleIcon(w.variant == Variant.TopBottom, { onWorking(w.copy(variant = Variant.TopBottom)) }, Icons.Filled.SpaceBar)
        }

        "Bordi" -> {
            OutlinePill("Nessuno") { onWorking(w.copy(border = w.border.copy(color = Color.Transparent, width = 0.dp, shadow = 0.dp))) }
            OutlinePill("Leggero") { onWorking(w.copy(border = w.border.copy(color = Color(0x22000000), width = 1.dp))) }
            OutlinePill("Evidente") { onWorking(w.copy(border = w.border.copy(color = Color(0x55000000), width = 2.dp, shadow = 8.dp))) }
        }

        "Comportamento" -> {
            ToggleIcon(w.behavior == Behavior.Normal, { onWorking(w.copy(behavior = Behavior.Normal)) }, Icons.Filled.Stop)
            ToggleIcon(w.behavior == Behavior.Paged, { onWorking(w.copy(behavior = Behavior.Paged, pages = max(2, w.pages))) }, Icons.Filled.ViewCarousel)
            ToggleIcon(w.behavior == Behavior.Tabs, { onWorking(w.copy(behavior = Behavior.Tabs, tabsCount = max(2, w.tabsCount))) }, Icons.Filled.Tab)
        }

        "Azioni" -> {
            Text("Azioni al tap (placeholder): apri pagina/menù/HTTP/evidenzia/input…", color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    onLeafChanged: (String) -> Unit
) {
    var underline by remember { mutableStateOf(false) }
    var italic by remember { mutableStateOf(false) }

    ToggleIcon(underline, {
        underline = !underline
        onLeafChanged("Sottolinea: ${if (underline) "On" else "Off"}")
    }, Icons.Filled.FormatUnderlined)

    ToggleIcon(italic, {
        italic = !italic
        onLeafChanged("Corsivo: ${if (italic) "On" else "Off"}")
    }, Icons.Filled.FormatItalic)

    Spacer(Modifier.width(8.dp))

    // Font size dropdown (mostra valore a menu chiuso)
    var expanded by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf("16sp") }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedButton(
            onClick = { expanded = true },
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Icon(Icons.Filled.FormatSize, null, tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text(size, color = Color.White)
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("14sp", "16sp", "18sp", "20sp", "24sp").forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = {
                    size = opt
                    expanded = false
                    onLeafChanged("Size: $opt")
                })
            }
        }
    }
}

@Composable
private fun InsertMenu() {
    Text("Suggerimento: tocca due celle della griglia per creare un Contenitore.", color = Color.White)
}

/* ==========================================================
 *  DIALOG STUB SALVA PRESET
 * ========================================================== */

@Composable
private fun SavePresetDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 8.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Salva impostazioni come preset")
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome preset") })
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Annulla") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Salva") }
                }
            }
        }
    }
}

/* ==========================================================
 *  UTILITY UI
 * ========================================================== */

@Composable
private fun WhiteIcon(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun WhiteChip(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Icon(icon, null, tint = Color.White)
    }
}

@Composable
private fun ToggleIcon(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val border = if (selected) 2.dp else 1.dp
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(border, Color.White),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) { Icon(icon, null, tint = Color.White) }
}

@Composable
private fun OutlinePill(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) { Text(text, color = Color.White) }
}

@Composable
private fun ColorDot(color: Color, onPick: (Color) -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .background(color, CircleShape)
            .border(1.dp, Color(0x33000000), CircleShape)
            .pointerInput(Unit) { detectTapGestures { onPick(color) } }
    )
    Spacer(Modifier.width(4.dp))
}

private fun posToCell(x: Float, y: Float, size: IntSize, cols: Int, rows: Int): Pair<Int, Int> {
    val cw = size.width / cols.toFloat()
    val rh = size.height / rows.toFloat()
    val col = min(cols, max(1, (x / cw + 1).toInt()))
    val row = min(rows, max(1, (y / rh + 1).toInt()))
    return col to row
}

private fun selectedContainer(state: EditorState): ContainerNode? =
    state.selection?.let { sel -> state.doc.nodes.firstOrNull { it.id == sel } as? ContainerNode }

/* Helper per path bar (ultimo leaf) */
private fun describePageLeaf(path: List<String>, s: PageStyle): String =
    when (path.getOrNull(1)) {
        "Colore" -> if (s.mode == BgMode.Color) "Colore singolo" else "Gradiente"
        "Immagini" -> if (s.mode == BgMode.Image) "Immagine" else if (s.mode == BgMode.Album) "Album" else "Immagini"
        else -> path.lastOrNull() ?: ""
    }

private fun describeContainerLeaf(path: List<String>, s: ContainerStyle): String =
    when (path.getOrNull(1)) {
        "Colore" -> if (s.bgMode == BgMode.Color) "Colore" else "Gradiente"
        "Scroll" -> when (s.scroll) { ScrollMode.None -> "Nessuna"; ScrollMode.Vertical -> "Verticale"; ScrollMode.Horizontal -> "Orizzontale" }
        "Stile" -> s.variant.name
        "Forma" -> s.shape.name
        "Comportamento" -> s.behavior.name
        else -> path.lastOrNull() ?: ""
    }
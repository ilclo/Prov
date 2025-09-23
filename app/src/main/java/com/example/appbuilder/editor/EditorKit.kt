package com.example.appbuilder.editor

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.AutoAwesome // sostituisce Radiology
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlin.math.max
import kotlin.math.min
import java.util.UUID
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

/* ===========================
 *  MODEL (minimo editor)
 * =========================== */

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
): Node

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
): Node

data class IconNode(
    override val id: String = "icon_" + UUID.randomUUID().toString().take(8),
    override val frame: GridRect,
    val color: Color = Color.Unspecified
): Node

data class ImageNode(
    override val id: String = "img_" + UUID.randomUUID().toString().take(8),
    override val frame: GridRect,
    val uri: Uri? = null,
    val fit: String = "cover",
    val crop: String? = null
): Node

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

/* ===========================
 *  DEMO ENTRY
 * =========================== */

@Composable
fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,          // se vuoi evidenziare uno stato ON/OFF
    onClick: () -> Unit
) {
    val container = when {
        selected -> MaterialTheme.colorScheme.primary
        else    -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content   = Color.White
    val containerDisabled = container.copy(alpha = 0.5f)
    val contentDisabled   = content.copy(alpha = 0.5f)

    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(44.dp), // pulsante più “importante” di una icon button base
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = containerDisabled,
            disabledContentColor = contentDisabled
        )
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}


@Composable
fun EditorDemoScreen() {
    var state by remember {
        mutableStateOf(
            EditorState(
                doc = PageDocument(
                    nodes = listOf(
                        ContainerNode(frame = GridRect(1, 1, 12, 6)),
                        TextNode(frame = GridRect(2, 2, 6, 3), text = "Titolo", sizeSp = 22f, weight = FontWeight.SemiBold),
                        ContainerNode(frame = GridRect(2, 7, 11, 13))
                    )
                )
            )
        )
    }
    EditorRoot(state = state, onStateChange = { state = it })
}

/* ===========================
 *  ROOT: tela + menù
 * =========================== */

@Composable
fun EditorRoot(
    state: EditorState,
    onStateChange: (EditorState) -> Unit
) {
    // Path menù e working styles
    var menuPath by remember { mutableStateOf<List<String>>(emptyList()) }
    var workingPageStyle by remember { mutableStateOf(state.doc.style) }
    var workingContainerStyle by remember { mutableStateOf<ContainerStyle?>(null) }

    // “Ultima modifica” per il path
    var lastChanged by remember { mutableStateOf<String?>(null) }
    var dirty by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    fun setSelection(id: String?) {
        onStateChange(state.copy(selection = id))
        workingContainerStyle = selectedContainer(state)?.style
    }

    val barBg = Color(0xFF0F172A)    // slate-900
    val barBg2 = Color(0xFF111827)   // slate-800
    val iconTint = Color.White

    Box(Modifier.fillMaxSize()) {
        // Tela (sfondo bianco per ora) con griglia e hit test
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
            },
        )

        // BOTTOM UI
        EditorBottomArea(
            isInMenu = menuPath.isNotEmpty(),
            menuPath = menuPath,
            lastChanged = lastChanged,
            onExitMenu = {
                if (dirty) showConfirm = true
                else menuPath = emptyList()
            },
            barBg = barBg,
            barBg2 = barBg2,
            iconTint = iconTint,

            // categorie (icone solo)
            onOpenLayout = { menuPath = listOf("Layout") },
            onOpenContainer = { menuPath = listOf("Contenitore") },
            onOpenText = { menuPath = listOf("Testo") },
            onOpenImage = { menuPath = listOf("Immagine") },
            onOpenInsert = { menuPath = listOf("Inserisci") },

            // azioni (solo UI, nessuna logica per ora)
            onUndo = { /* no-op */ },
            onRedo = { /* no-op */ },
            onSavePage = { /* no-op */ },
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
                    state.doc.nodes.find { it.id == sel }?.let { n ->
                        val clone = when (n) {
                            is ContainerNode -> n.copy(id = "cont_" + UUID.randomUUID().toString().take(8))
                            is TextNode -> n.copy(id = "text_" + UUID.randomUUID().toString().take(8))
                            is ImageNode -> n.copy(id = "img_" + UUID.randomUUID().toString().take(8))
                            is IconNode -> n.copy(id = "icon_" + UUID.randomUUID().toString().take(8))
                        }
                        onStateChange(state.copy(doc = state.doc.copy(nodes = state.doc.nodes + clone)))
                    }
                }
            },
            onProps = { /* no-op */ },
            onPageLayout = { menuPath = listOf("Layout") },
            onCreate = { /* dropdown handled inside bar */ },
            onList = { /* no-op */ },
            onSaveProject = { /* no-op */ },
            onOpen = { /* no-op */ },
            onNew = { /* no-op */ },

            // Pannello contenuti del menù corrente
            menuContent = {
                when (menuPath.firstOrNull()) {
                    "Layout" -> LayoutMenu(
                        path = menuPath,
                        onPath = { menuPath = it },
                        working = workingPageStyle,
                        onWorking = {
                            dirty = true
                            workingPageStyle = it
                        },
                        onChanged = { lastChanged = it }
                    )

                    "Contenitore" -> ContainerMenu(
                        path = menuPath,
                        onPath = { menuPath = it },
                        working = workingContainerStyle,
                        onWorking = {
                            dirty = true
                            workingContainerStyle = it
                        },
                        onChanged = { lastChanged = it }
                    )

                    "Testo" -> TextMenu(
                        path = menuPath,
                        onPath = { menuPath = it },
                        onChanged = { lastChanged = it; dirty = true }
                    )

                    "Immagine" -> ImageMenu(
                        path = menuPath,
                        onPath = { menuPath = it },
                        onChanged = { lastChanged = it; dirty = true }
                    )

                    "Inserisci" -> InsertMenu()
                }
            }
        )

        // Barra conferma (se esco con modifiche)
        if (showConfirm) {
            ConfirmBar(
                onCancel = {
                    // annulla: scarta working e chiudi conferma
                    workingPageStyle = state.doc.style
                    workingContainerStyle = selectedContainer(state)?.style
                    dirty = false
                    showConfirm = false
                    menuPath = emptyList()
                },
                onOk = {
                    // conferma: applica working
                    if (menuPath.firstOrNull() == "Layout") {
                        onStateChange(state.copy(doc = state.doc.copy(style = workingPageStyle)))
                    } else if (menuPath.firstOrNull() == "Contenitore" && state.selection != null && workingContainerStyle != null) {
                        val updated = state.doc.nodes.map {
                            if (it.id == state.selection && it is ContainerNode) it.copy(style = workingContainerStyle!!) else it
                        }
                        onStateChange(state.copy(doc = state.doc.copy(nodes = updated)))
                    }
                    dirty = false
                    showConfirm = false
                    menuPath = emptyList()
                },
                onSavePreset = {
                    // solo UI/placeholder
                    // mostreremo un dialog “salva come stile…”
                    dirty = false
                    showConfirm = false
                    menuPath = emptyList()
                }
            )
        }
    }
}

/* ===========================
 *  CANVAS
 * =========================== */

@Composable
private fun EditorCanvas(
    doc: PageDocument,
    selection: String?,
    isEditor: Boolean,
    onSelect: (String?) -> Unit,
    onAddContainerViaGrid: (GridRect) -> Unit
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
        // griglia
        Canvas(Modifier.fillMaxSize()) { drawGrid(doc.gridCols, doc.gridRows, sizePx) }

        // nodi
        doc.nodes.forEach { node ->
            val rectPx = cellRectPx(node.frame, sizePx, doc.gridCols, doc.gridRows)
            val sel = selection == node.id
            NodeView(node, rectPx, sel)
        }

        // overlay sizing
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
    val cw = size.width / cols.toFloat()
    val rh = size.height / rows.toFloat()
    val left = ((frame.left - 1) * cw).toInt()
    val top = ((frame.top - 1) * rh).toInt()
    val right = (frame.right * cw).toInt()
    val bottom = (frame.bottom * rh).toInt()
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

@Composable
private fun BoxScope.NodeView(node: Node, rect: android.graphics.Rect, selected: Boolean) {
    val density = LocalDensity.current
    val w = with(density) { rect.width().toDp() }
    val h = with(density) { rect.height().toDp() }
    val offset = IntOffset(rect.left, rect.top)

    Box(Modifier.offset { offset }.size(w, h)) {
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
                    else -> Brush.linearGradient(listOf(Color(0xFF2C2C2C), Color(0xFF111111)))
                }
                val base = when (node.style.variant) {
                    Variant.Text -> Modifier
                    Variant.TopBottom -> Modifier.drawBehind {
                        val th = size.height * 0.12f
                        drawRect(node.style.color1, size = androidx.compose.ui.geometry.Size(size.width, th))
                        drawRect(
                            node.style.color2,
                            topLeft = Offset(0f, size.height - th),
                            size = androidx.compose.ui.geometry.Size(size.width, th)
                        )
                    }
                    else -> Modifier.background(bg, shape)
                }
                val border = if (node.style.variant == Variant.Outlined || node.style.border.width > 0.dp)
                    Modifier.border(node.style.border.width, node.style.border.color, shape) else Modifier

                OutlinedCard(
                    modifier = base.then(border).fillMaxSize(),
                    shape = shape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) { /* children placeholder */ }
            }

            is TextNode -> {
                val hi = if (node.highlight) node.highlightColor else Color.Transparent
                Box(Modifier.fillMaxSize().background(hi, RoundedCornerShape(6.dp)).padding(6.dp)) {
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
                    Modifier.fillMaxSize()
                        .background(Color(0x22000000), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x33000000), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Image, , contentDescription = null,, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            is IconNode -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Settings, , contentDescription = null,, tint = if (node.color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else node.color)
                }
            }
        }

        if (selected) {
            Box(Modifier.fillMaxSize().border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)))
        }
    }
}

/* ===========================
 *  BOTTOM AREA (barre)
 * =========================== */

@Composable
private fun BoxScope.EditorBottomArea(
    isInMenu: Boolean,
    menuPath: List<String>,
    lastChanged: String?,
    onExitMenu: () -> Unit,
    barBg: Color,
    barBg2: Color,
    iconTint: Color,

    onOpenLayout: () -> Unit,
    onOpenContainer: () -> Unit,
    onOpenText: () -> Unit,
    onOpenImage: () -> Unit,
    onOpenInsert: () -> Unit,

    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSavePage: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onProps: () -> Unit,
    onPageLayout: () -> Unit,
    onCreate: () -> Unit,
    onList: () -> Unit,
    onSaveProject: () -> Unit,
    onOpen: () -> Unit,
    onNew: () -> Unit,

    menuContent: @Composable () -> Unit
) {
    var actionsHeightPx by remember { mutableStateOf(0) }

    // ——— Quando NON sono nei sottomenu: due barre sempre visibili (categorie + azioni)
    if (!isInMenu) {
        // Barra AZIONI (in basso)
        Surface(
            color = barBg,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onGloballyPositioned { actionsHeightPx = it.size.height }
                .windowInsetsPadding(WindowInsets.ime)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolbarIconButton(onClick = onUndo) { Icon(Icons.Filled.Undo, , contentDescription = null,, tint = iconTint) }
                    ToolbarIconButton(onClick = onRedo) { Icon(Icons.Filled.Redo, , contentDescription = null,, tint = iconTint) }
                    ToolbarIconButton(onClick = onSavePage) { Icon(Icons.Filled.Save, , contentDescription = null,, tint = iconTint) }
                    ToolbarIconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, , contentDescription = null,, tint = iconTint) }
                    ToolbarIconButton(onClick = onDuplicate) { Icon(Icons.Filled.ContentCopy, , contentDescription = null,, tint = iconTint) }
                    ToolbarIconButton(onClick = onProps) { Icon(Icons.Filled.Settings, , contentDescription = null,, tint = iconTint) }
                    ToolbarIconButton(onClick = onPageLayout) { Icon(Icons.Filled.Tune, , contentDescription = null,, tint = iconTint) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // “Crea” con dropdown stub
                    var openCreate by remember { mutableStateOf(false) }
                    Box {
                        ToolbarIconButton(onClick = { openCreate = true }) { Icon(Icons.Filled.Add, , contentDescription = null,, tint = iconTint) }
                        DropdownMenu(expanded = openCreate, onDismissRequest = { openCreate = false }) {
                            DropdownMenuItem(text = { Text("Nuova pagina") }, onClick = { openCreate = false })
                            DropdownMenuItem(text = { Text("Nuovo avviso") }, onClick = { openCreate = false })
                            DropdownMenuItem(text = { Text("Menù laterale") }, onClick = { openCreate = false })
                            DropdownMenuItem(text = { Text("Menù centrale") }, onClick = { openCreate = false })
                        }
                    }
                    ToolbarIconButton(onClick = onList) { Icon(Icons.Filled.List, , contentDescription = null,, tint = iconTint) }
                    ToolbarIconButton(onClick = onSaveProject) { Icon(Icons.Filled.SaveAs, , contentDescription = null,, tint = iconTint) }
                    ToolbarIconButton(onClick = onOpen) { Icon(Icons.Filled.FolderOpen, , contentDescription = null,, tint = iconTint) }
                    ToolbarIconButton(onClick = onNew) { Icon(Icons.Filled.CreateNewFolder, , contentDescription = null,, tint = iconTint) }
                }
            }
        }

        // Barra CATEGORIE (sopra)
        Surface(
            color = barBg2,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
                .padding(
                    start = 12.dp, end = 12.dp,
                    bottom = with(LocalDensity.current) { actionsHeightPx.toDp() + 8.dp }
                )
        ) {
            val s = rememberScrollState()
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(s)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarIconButton(onClick = onOpenLayout) { Icon(Icons.Filled.Tune, , contentDescription = null,, tint = iconTint) }
                ToolbarIconButton(onClick = onOpenContainer) { Icon(Icons.Filled.Widgets, , contentDescription = null,, tint = iconTint) }
                ToolbarIconButton(onClick = onOpenText) { Icon(Icons.Filled.TextFields, , contentDescription = null,, tint = iconTint) }
                ToolbarIconButton(onClick = onOpenImage) { Icon(Icons.Filled.Image, , contentDescription = null,, tint = iconTint) }
                ToolbarIconButton(onClick = onOpenInsert) { Icon(Icons.Filled.Add, , contentDescription = null,, tint = iconTint) }
            }
        }
    } else {
        // ——— Nei sottomenu: compare PATH + pannello menu
        val bread = buildString {
            append(menuPath.joinToString(" → "))
            lastChanged?.let { append("  •  "); append(it) }
        }

        // Path bar
        Surface(
            color = barBg2,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolbarIconButton(onClick = onExitMenu) { Icon(Icons.Filled.ArrowBack, , contentDescription = null,, tint = Color.White) }
                Text(bread, color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }

        // Menu panel (sopra la path bar)
        Surface(
            color = barBg,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
                .padding(horizontal = 12.dp)
                .padding(bottom = 60.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                menuContent()
            }
        }
    }
}

/* ===========================
 *  MINI CONFIRM BAR
 * =========================== */

@Composable
private fun BoxScope.ConfirmBar(
    onCancel: () -> Unit,
    onOk: () -> Unit,
    onSavePreset: () -> Unit
) {
    Surface(
        color = Color(0xFF0B1220),
        tonalElevation = 10.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Salvare le modifiche?", color = Color.White, modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) { Icon(Icons.Filled.Close, , contentDescription = null,, tint = Color.White) }
            TextButton(onClick = onSavePreset) { Icon(Icons.Filled.BookmarkAdd, , contentDescription = null,, tint = Color.White) }
            Button(onClick = onOk) { Icon(Icons.Filled.Check, , contentDescription = null,) }
        }
    }
}

/* ===========================
 *  MENU: Layout / Container / Text / Image / Insert
 * =========================== */

@Composable
private fun LayoutMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    working: PageStyle,
    onWorking: (PageStyle) -> Unit,
    onChanged: (String) -> Unit
) {
    when (path.getOrNull(1)) {
        null -> {
            ToolbarIconButton(onClick = { onPath(path + "Colore") }) { Icon(Icons.Filled.ColorLens, , contentDescription = null,, tint = Color.White) }
            ToolbarIconButton(onClick = { onPath(path + "Immagini") }) { Icon(Icons.Filled.Image, , contentDescription = null,, tint = Color.White) }
        }

        "Colore" -> {
            // Colore singolo / gradiente
            ToggleIcon(
                selected = working.mode == BgMode.Color,
                onClick = {
                    onWorking(working.copy(mode = BgMode.Color))
                    onChanged("Layout → Colore: singolo")
                },
                icon = Icons.Filled.Lens
            )
            ToggleIcon(
                selected = working.mode == BgMode.Gradient,
                onClick = {
                    onWorking(working.copy(mode = BgMode.Gradient))
                    onChanged("Layout → Colore: gradiente")
                },
                icon = Icons.Filled.LinearScale
            )
            // palette rapida (aggiorna color1)
            listOf(Color(0xFF0EA5E9), Color(0xFF9333EA), Color(0xFFEF4444), Color(0xFF10B981)).forEach { c ->
                ColorDot(c) {
                    onWorking(working.copy(color1 = c))
                    onChanged("Layout → Colore1")
                }
            }
            // seconda tinta
            Spacer(Modifier.width(8.dp))
            listOf(Color(0xFF94A3B8), Color(0xFFF59E0B), Color(0xFF22C55E), Color(0xFF3B82F6)).forEach { c ->
                ColorDot(c) {
                    onWorking(working.copy(color2 = c, mode = BgMode.Gradient))
                    onChanged("Layout → Colore2")
                }
            }
        }

        "Immagini" -> {
            val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    onWorking(working.copy(mode = BgMode.Image, image = uri))
                    onChanged("Layout → Immagine: selezionata")
                }
            }
            val pickAlbum = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                if (uris.isNotEmpty()) {
                    onWorking(working.copy(mode = BgMode.Album, album = uris))
                    onChanged("Layout → Album: ${uris.size} foto")
                }
            }
            OutlinedButton(onClick = { pickImage.launch("image/*") }) { Icon(Icons.Filled.Crop, , contentDescription = null,); Spacer(Modifier.width(6.dp)); Text("Foto") }
            OutlinedButton(onClick = { pickAlbum.launch("image/*") }) { Icon(Icons.Filled.Collections, , contentDescription = null,); Spacer(Modifier.width(6.dp)); Text("Album") }
        }
    }
}

@Composable
private fun ContainerMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    working: ContainerStyle?,
    onWorking: (ContainerStyle?) -> Unit,
    onChanged: (String) -> Unit
) {
    val w = working ?: ContainerStyle().also { onWorking(it) }
    when (path.getOrNull(1)) {
        null -> {
            ToolbarIconButto(onClick = { onPath(path + "Colore") }) { Icon(Icons.Filled.ColorLens, , contentDescription = null,, tint = Color.White) }
            ToolbarIconButto(onClick = { onPath(path + "Immagini") }) { Icon(Icons.Filled.Image, , contentDescription = null,, tint = Color.White) }
            ToolbarIconButto(onClick = { onPath(path + "Scroll") }) { Icon(Icons.Filled.SwapVert, , contentDescription = null,, tint = Color.White) }
            ToolbarIconButto(onClick = { onPath(path + "Forma") }) { Icon(Icons.Filled.CropSquare, , contentDescription = null,, tint = Color.White) }
            ToolbarIconButto(onClick = { onPath(path + "Stile") }) { Icon(Icons.Filled.FlashOn, , contentDescription = null,, tint = Color.White) }
            ToolbarIconButto(onClick = { onPath(path + "Bordi") }) { Icon(Icons.Filled.BorderStyle, , contentDescription = null,, tint = Color.White) }
            ToolbarIconButto(onClick = { onPath(path + "Comportamento") }) { Icon(Icons.Filled.ViewCarousel, , contentDescription = null,, tint = Color.White) }
        }

        "Colore" -> {
            ToggleIcon(w.bgMode == BgMode.Color, {
                onWorking(w.copy(bgMode = BgMode.Color)); onChanged("Contenitore → Colore: singolo")
            }, Icons.Filled.Lens)
            ToggleIcon(w.bgMode == BgMode.Gradient, {
                onWorking(w.copy(bgMode = BgMode.Gradient)); onChanged("Contenitore → Colore: gradiente")
            }, Icons.Filled.LinearScale)

            listOf(Color(0xFFFFFFFF), Color(0xFFF3F4F6), Color(0xFF111827), Color(0xFF0EA5E9)).forEach {
                ColorDot(it) { c -> onWorking(w.copy(color1 = c)); onChanged("Contenitore → Colore1") }
            }
        }

        "Immagini" -> {
            val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) { onWorking(w.copy(bgMode = BgMode.Image, image = uri)); onChanged("Contenitore → Immagine") }
            }
            val pickAlbum = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                if (uris.isNotEmpty()) { onWorking(w.copy(bgMode = BgMode.Album, album = uris)); onChanged("Contenitore → Album: ${uris.size} foto") }
            }
            OutlinedButton(onClick = { pickImage.launch("image/*") }) { Icon(Icons.Filled.Crop, , contentDescription = null,); Spacer(Modifier.width(6.dp)); Text("Foto") }
            OutlinedButton(onClick = { pickAlbum.launch("image/*") }) { Icon(Icons.Filled.Collections, , contentDescription = null,); Spacer(Modifier.width(6.dp)); Text("Album") }
        }

        "Scroll" -> {
            ToggleIcon(w.scroll == ScrollMode.None, { onWorking(w.copy(scroll = ScrollMode.None)); onChanged("Contenitore → Scroll: nessuno") }, Icons.Filled.Block)
            ToggleIcon(w.scroll == ScrollMode.Vertical, { onWorking(w.copy(scroll = ScrollMode.Vertical)); onChanged("Contenitore → Scroll: verticale") }, Icons.Filled.SwapVert)
            ToggleIcon(w.scroll == ScrollMode.Horizontal, { onWorking(w.copy(scroll = ScrollMode.Horizontal)); onChanged("Contenitore → Scroll: orizzontale") }, Icons.Filled.SwapHoriz)
        }

        "Forma" -> {
            ToggleIcon(w.shape == ShapeKind.Rect, { onWorking(w.copy(shape = ShapeKind.Rect)); onChanged("Contenitore → Forma: rettangolo") }, Icons.Filled.CropSquare)
            ToggleIcon(w.shape == ShapeKind.RoundedRect, { onWorking(w.copy(shape = ShapeKind.RoundedRect)); onChanged("Contenitore → Forma: arrotondato") }, Icons.Filled.RoundedCorner)
            ToggleIcon(w.shape == ShapeKind.Circle, { onWorking(w.copy(shape = ShapeKind.Circle)); onChanged("Contenitore → Forma: cerchio") }, Icons.Filled.Radiology)
        }

        "Stile" -> {
            ToggleIcon(w.variant == Variant.Full, { onWorking(w.copy(variant = Variant.Full)); onChanged("Contenitore → Stile: full") }, Icons.Filled.InvertColors)
            ToggleIcon(w.variant == Variant.Outlined, { onWorking(w.copy(variant = Variant.Outlined)); onChanged("Contenitore → Stile: outlined") }, Icons.Filled.CropDin)
            ToggleIcon(w.variant == Variant.Text, { onWorking(w.copy(variant = Variant.Text)); onChanged("Contenitore → Stile: text") }, Icons.Filled.TextFields)
            ToggleIcon(w.variant == Variant.TopBottom, { onWorking(w.copy(variant = Variant.TopBottom)); onChanged("Contenitore → Stile: topbottom") }, Icons.Filled.ViewDay)
        }

        "Bordi" -> {
            OutlinedButton(onClick = { onWorking(w.copy(border = w.border.copy(color = Color.Transparent, width = 0.dp, shadow = 0.dp))); onChanged("Contenitore → Bordi: nessuno") }) { Text("Nessuno") }
            OutlinedButton(onClick = { onWorking(w.copy(border = w.border.copy(color = Color(0x22000000), width = 1.dp))); onChanged("Contenitore → Bordi: leggero") }) { Text("Leggero") }
            OutlinedButton(onClick = { onWorking(w.copy(border = w.border.copy(color = Color(0x55000000), width = 2.dp, shadow = 8.dp))); onChanged("Contenitore → Bordi: evidente") }) { Text("Evidente") }
        }

        "Comportamento" -> {
            ToggleIcon(w.behavior == Behavior.Normal, { onWorking(w.copy(behavior = Behavior.Normal)); onChanged("Contenitore → Tipo: normale") }, Icons.Filled.SmartButton)
            ToggleIcon(w.behavior == Behavior.Paged, { onWorking(w.copy(behavior = Behavior.Paged, pages = max(2, w.pages))); onChanged("Contenitore → Tipo: sfogliabile") }, Icons.Filled.ViewCarousel)
            ToggleIcon(w.behavior == Behavior.Tabs, { onWorking(w.copy(behavior = Behavior.Tabs, tabsCount = max(2, w.tabsCount))); onChanged("Contenitore → Tipo: tabs") }, Icons.Filled.Tab)
        }
    }
}

@Composable
private fun TextMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    onChanged: (String) -> Unit
) {
    var underline by remember { mutableStateOf(false) }
    var italic by remember { mutableStateOf(false) }

    ToggleIcon(underline, {
        underline = !underline; onChanged("Testo → Sottolinea: ${if (underline) "on" else "off"}")
    }, Icons.Filled.FormatUnderlined)

    ToggleIcon(italic, {
        italic = !italic; onChanged("Testo → Corsivo: ${if (italic) "on" else "off"}")
    }, Icons.Filled.FormatItalic)

    // font/weight/size come placeholder (menu a tendina in step successivo)
    ToolbarIconButto(onClick = { onChanged("Testo → Font…") }) { Icon(Icons.Filled.FontDownload, contentDescription = null, tint = Color.White) }
    ToolbarIconButto(onClick = { onChanged("Testo → Weight…") }) { Icon(Icons.Filled.FormatBold, contentDescription = null, tint = Color.White) }
    ToolbarIconButto(onClick = { onChanged("Testo → Size…") }) { Icon(Icons.Filled.FormatSize, contentDescription = null, tint = Color.White) }
}

@Composable
private fun ImageMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    onChanged: (String) -> Unit
) {
    ToolbarIconButto(onClick = { onChanged("Immagine → Crop") }) { Icon(Icons.Filled.Crop, contentDescription = null, tint = Color.White) }
    ToolbarIconButto(onClick = { onChanged("Immagine → Cornice") }) { Icon(Icons.Filled.CropPortrait, contentDescription = null, tint = Color.White) }
    ToolbarIconButto(onClick = { onChanged("Immagine → Album") }) { Icon(Icons.Filled.Collections, contentDescription = null, tint = Color.White) }
    ToolbarIconButto(onClick = { onChanged("Immagine → Adattamento") }) { Icon(Icons.Filled.FitScreen, contentDescription = null, tint = Color.White) }
}

@Composable
private fun InsertMenu() {
    Text("Suggerimento: tocca due celle della griglia per creare un Contenitore.", color = Color.White)
}

/* ===========================
 *  UTILS
 * =========================== */

@Composable
private fun ToggleIcon(selected: Boolean, onClick: () -> Unit, icon: ImageVector) {
    val bw = if (selected) 2.dp else 1.dp
    Box(
        Modifier
            .size(40.dp)
            .border(bw, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun ColorDot(color: Color, onPick: (Color) -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .background(color, CircleShape)
            .border(1.dp, Color(0x33000000), CircleShape)
            .clickable { onPick(color) }
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

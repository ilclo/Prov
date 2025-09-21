package com.example.appbuilder.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import org.json.JSONObject
import java.util.UUID

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
    val fit: String = "cover", // contain / cover / fill / fitWidth / fitHeight
    val crop: String? = null   // placeholder: in futuro passiamo un rect di crop
) : Node

/* Nodo “contenitore annidato” si ottiene con ContainerNode dentro ContainerNode */

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
    // Ui state per menù e working copies (OK/Annulla)
    var menuPath by remember { mutableStateOf<List<String>>(emptyList()) } // es. ["Layout", "Immagini", "Crop"]
    var workingPageStyle by remember { mutableStateOf(state.doc.style) }
    var workingContainerStyle by remember { mutableStateOf<ContainerStyle?>(null) }

    // Selezione
    fun setSelection(id: String?) {
        onStateChange(state.copy(selection = id))
        // Reset working stile quando cambio target
        workingContainerStyle = selectedContainer(state)?.style
    }

    Box(Modifier.fillMaxSize()) {
        // Canvas della pagina + griglia + nodi
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
                // Porta direttamente al menù del contenitore
                menuPath = listOf("Contenitore")
            }
        )

        // Barra inferiore: breadcrumb + menù orizzontale corrente + azioni (Save/Dup/Del) + OK/Annulla
        EditorBottomBars(
            state = state,
            menuPath = menuPath,
            onMenuPath = { menuPath = it },

            workingPageStyle = workingPageStyle,
            onWorkingPageStyle = { workingPageStyle = it },

            workingContainerStyle = workingContainerStyle,
            onWorkingContainerStyle = { workingContainerStyle = it },

            onApply = {
                // Commit degli stili correnti
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
                // Esci dal menù
                menuPath = emptyList()
            },
            onCancel = {
                // Ripristina working dagli effettivi
                workingPageStyle = state.doc.style
                workingContainerStyle = selectedContainer(state)?.style
                menuPath = emptyList()
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
            onDelete = {
                state.selection?.let { sel ->
                    onStateChange(
                        state.copy(
                            doc = state.doc.copy(nodes = state.doc.nodes.filterNot { it.id == sel }),
                            selection = null
                        )
                    )
                }
            }
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
    var startCell by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (col,row) di start sizing
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
                        // Se sto in “modalità sizing”, seconda selezione = fine
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
                        // Se non in sizing, prova selezione nodo
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

        // Overlay sizing (selezione cella iniziale/finale)
        if (startCell != null) {
            val from = startCell!!
            val to = hoveringCell ?: from
            val rectPx = cellRectPx(GridRect(from.first, from.second, to.first, to.second), sizePx, doc.gridCols, doc.gridRows)
            Box(
                Modifier
                    .offset { IntOffset(rectPx.left, rectPx.top) }
                    .size(with(density) { (rectPx.width).toDp() }, with(density) { (rectPx.height).toDp() })
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
            // angolo semplificato 0/90/180/270
            val colors = listOf(style.color1, style.color2)
            when ((style.gradientAngleDeg.toInt() % 360 + 360) % 360) {
                90 -> Brush.verticalGradient(colors)
                180 -> Brush.horizontalGradient(colors.reversed())
                270 -> Brush.verticalGradient(colors.reversed())
                else -> Brush.horizontalGradient(colors)
            }
        }
        BgMode.Image, BgMode.Album -> {
            // placeholder: gradient tenue dietro, immagine render in futuro
            Brush.linearGradient(listOf(Color(0xFF101010), Color(0xFF202020)))
        }
    }
}

private fun hitTest(doc: PageDocument, size: IntSize, x: Float, y: Float): Node? {
    // cerca dall’ultimo al primo (sopra→sotto)
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
    // verticali
    for (c in 1 until cols) {
        val x = cw * c
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height.toFloat()), strokeWidth = 1f)
    }
    // orizzontali
    for (r in 1 until rows) {
        val y = rh * r
        drawLine(gridColor, Offset(0f, y), Offset(size.width.toFloat(), y), strokeWidth = 1f)
    }
}

/* ==========================================================
 *  # NODE VIEW — minimo rendering editor‑like
 * ========================================================== */

@Composable
private fun BoxScope.NodeView(node: Node, rect: android.graphics.Rect, selected: Boolean) {
    val width = with(LocalDensity.current) { (rect.width()).toDp() }
    val height = with(LocalDensity.current) { (rect.height()).toDp() }
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
                        Brush.linearGradient(listOf(Color(0xFF2C2C2C), Color(0xFF111111))) // placeholder
                }

                val cardColors = CardDefaults.cardColors(containerColor = Color.Transparent)

                val borderModifier =
                    if (node.style.variant == Variant.Outlined || node.style.border.width > 0.dp)
                        Modifier.border(node.style.border.width, node.style.border.color, shape)
                    else Modifier

                val base = when (node.style.variant) {
                    Variant.Text -> Modifier // solo contenuto, niente fondo
                    Variant.TopBottom -> Modifier.drawBehind {
                        // top e bottom band (placeholder)
                        val h = size.height * 0.12f
                        drawRect(node.style.color1, size = androidx.compose.ui.geometry.Size(size.width, h))
                        drawRect(node.style.color2, topLeft = Offset(0f, size.height - h), size = androidx.compose.ui.geometry.Size(size.width, h))
                    }
                    else -> Modifier.background(bg, shape)
                }

                OutlinedCard(
                    shape = shape,
                    colors = cardColors,
                    modifier = base.then(borderModifier).fillMaxSize()
                ) {
                    // (placeholder) contenuti annidati non disegnati in questa demo
                }
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
                        fontSize = androidx.compose.ui.unit.sp(node.sizeSp),
                        fontWeight = node.weight
                    )
                }
            }

            is ImageNode -> {
                // placeholder: solo riquadro immagine
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x22000000), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x33000000), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            is IconNode -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = if (node.color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else node.color)
                }
            }
        }

        // Evidenziazione selezione
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
 *  # BOTTOM BARS — breadcrumb + menu corrente + icone azione + ok/annulla
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

    onApply: () -> Unit,
    onCancel: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    // Barra comandi principale
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Breadcrumb (dove sono nel menù)
            val bread = if (menuPath.isEmpty()) "Seleziona un menù (Layout / Contenitore / Testo / Icona / Immagine…)" else menuPath.joinToString("  →  ")
            Text(bread, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))

            // Azioni a destra (sempre visibili)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDuplicate, enabled = state.selection != null) { Icon(Icons.Filled.ContentCopy, null) }
                IconButton(onClick = onDelete, enabled = state.selection != null) { Icon(Icons.Filled.Delete, null) }
                TextButton(onClick = onCancel) { Icon(Icons.Filled.Close, null); Spacer(Modifier.width(4.dp)); Text("Annulla") }
                Button(onClick = onApply) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(4.dp)); Text("OK") }
            }
        }
    }

    // Barra menù corrente (scorrevole dietro OK/Annulla)
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .offset { IntOffset(0, -70) }
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
                ElevatedFilterChip(selected = false, onClick = { onMenuPath(listOf("Layout")) }, label = { Text("Layout") }, leadingIcon = { Icon(Icons.Filled.Tune, null) })
                ElevatedFilterChip(selected = false, onClick = { onMenuPath(listOf("Contenitore")) }, label = { Text("Contenitore") }, leadingIcon = { Icon(Icons.Filled.BorderColor, null) })
                ElevatedFilterChip(selected = false, onClick = { onMenuPath(listOf("Testo")) }, label = { Text("Testo") }, leadingIcon = { Icon(Icons.Filled.TextFields, null) })
                ElevatedFilterChip(selected = false, onClick = { onMenuPath(listOf("Immagine")) }, label = { Text("Immagine") }, leadingIcon = { Icon(Icons.Filled.Image, null) })
                ElevatedFilterChip(selected = false, onClick = { onMenuPath(listOf("Inserisci")) }, label = { Text("Inserisci") }, leadingIcon = { Icon(Icons.Filled.Add, null) })
            } else {
                when (menuPath.first()) {
                    "Layout" -> LayoutMenu(menuPath, onMenuPath, workingPageStyle, onWorkingPageStyle)
                    "Contenitore" -> ContainerMenu(menuPath, onMenuPath, workingContainerStyle, onWorkingContainerStyle)
                    "Testo" -> TextMenu(menuPath, onMenuPath)
                    "Immagine" -> ImageMenu(menuPath, onMenuPath)
                    "Inserisci" -> InsertMenu(onMenuPath)
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
    onWorking: (PageStyle) -> Unit
) {
    when (path.getOrNull(1)) {
        null -> {
            FilterChip(selected = false, onClick = { onPath(path + "Colore") }, label = { Text("Colore") }, leadingIcon = { Icon(Icons.Filled.ColorLens, null) })
            FilterChip(selected = false, onClick = { onPath(path + "Immagini") }, label = { Text("Immagini") }, leadingIcon = { Icon(Icons.Filled.Image, null) })
        }
        "Colore" -> {
            FilterChip(selected = working.mode == BgMode.Color, onClick = { onWorking(working.copy(mode = BgMode.Color)) }, label = { Text("Colore singolo") })
            FilterChip(selected = working.mode == BgMode.Gradient, onClick = { onWorking(working.copy(mode = BgMode.Gradient)) }, label = { Text("Gradiente") })
            // palette rapida
            listOf(Color(0xFF0EA5E9), Color(0xFF9333EA), Color(0xFFEF4444), Color(0xFF10B981)).forEach {
                ColorDot(it) { c ->
                    onWorking(working.copy(color1 = c))
                }
            }
        }
        "Immagini" -> {
            val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) onWorking(working.copy(mode = BgMode.Image, image = uri))
            }
            val pickAlbum = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                if (uris.isNotEmpty()) onWorking(working.copy(mode = BgMode.Album, album = uris))
            }
            OutlinedButton(onClick = { pickImage.launch("image/*") }) { Icon(Icons.Filled.Crop, null); Spacer(Modifier.width(6.dp)); Text("Seleziona immagine (crop in step 2)") }
            OutlinedButton(onClick = { pickAlbum.launch("image/*") }) { Icon(Icons.Filled.Collections, null); Spacer(Modifier.width(6.dp)); Text("Album (multi‑selezione)") }
        }
    }
}

/* -------------------------
 *  CONTAINER MENU
 * ------------------------- */

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
            FilterChip(false, { onPath(path + "Colore") }, { Text("Colore") }, leadingIcon = { Icon(Icons.Filled.ColorLens, null) })
            FilterChip(false, { onPath(path + "Immagini") }, { Text("Immagini") }, leadingIcon = { Icon(Icons.Filled.Image, null) })
            FilterChip(false, { onPath(path + "Scroll") }, { Text("Scrollabilità") })
            FilterChip(false, { onPath(path + "Forma") }, { Text("Forma/Angoli") })
            FilterChip(false, { onPath(path + "Stile") }, { Text("Stile") })
            FilterChip(false, { onPath(path + "Bordi") }, { Text("Bordi/Ombra") })
            FilterChip(false, { onPath(path + "Comportamento") }, { Text("Tipo") })
            FilterChip(false, { onPath(path + "Azioni") }, { Text("Azioni (stub)") })
        }

        "Colore" -> {
            FilterChip(w.bgMode == BgMode.Color, { onWorking(w.copy(bgMode = BgMode.Color)) }, { Text("Colore") })
            FilterChip(w.bgMode == BgMode.Gradient, { onWorking(w.copy(bgMode = BgMode.Gradient)) }, { Text("Gradiente") })
            listOf(Color(0xFFFFFFFF), Color(0xFFF3F4F6), Color(0xFF111827), Color(0xFF0EA5E9)).forEach {
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
            OutlinedButton(onClick = { pickImage.launch("image/*") }) { Icon(Icons.Filled.Crop, null); Spacer(Modifier.width(6.dp)); Text("Seleziona immagine (crop in step 2)") }
            OutlinedButton(onClick = { pickAlbum.launch("image/*") }) { Icon(Icons.Filled.Collections, null); Spacer(Modifier.width(6.dp)); Text("Album (multi‑selezione)") }
        }

        "Scroll" -> {
            FilterChip(w.scroll == ScrollMode.None, { onWorking(w.copy(scroll = ScrollMode.None)) }, { Text("Nessuna") })
            FilterChip(w.scroll == ScrollMode.Vertical, { onWorking(w.copy(scroll = ScrollMode.Vertical)) }, { Text("Verticale") })
            FilterChip(w.scroll == ScrollMode.Horizontal, { onWorking(w.copy(scroll = ScrollMode.Horizontal)) }, { Text("Orizzontale") })
        }

        "Forma" -> {
            FilterChip(w.shape == ShapeKind.Rect, { onWorking(w.copy(shape = ShapeKind.Rect)) }, { Text("Rettangolo") })
            FilterChip(w.shape == ShapeKind.RoundedRect, { onWorking(w.copy(shape = ShapeKind.RoundedRect)) }, { Text("Arrotondato") })
            FilterChip(w.shape == ShapeKind.Circle, { onWorking(w.copy(shape = ShapeKind.Circle)) }, { Text("Cerchio") })
            // Angoli rapidi (per cerchio non applica)
            if (w.shape != ShapeKind.Circle) {
                Spacer(Modifier.width(8.dp))
                listOf(0.dp, 8.dp, 12.dp, 16.dp, 24.dp).forEach { v ->
                    OutlinedButton(onClick = {
                        onWorking(w.copy(corners = w.corners.copy(topStart = v, topEnd = v, bottomStart = v, bottomEnd = v)))
                    }) { Text("${v.value.toInt()}dp") }
                }
            }
        }

        "Stile" -> {
            FilterChip(w.variant == Variant.Full, { onWorking(w.copy(variant = Variant.Full)) }, { Text("Full") })
            FilterChip(w.variant == Variant.Outlined, { onWorking(w.copy(variant = Variant.Outlined)) }, { Text("Outlined") })
            FilterChip(w.variant == Variant.Text, { onWorking(w.copy(variant = Variant.Text)) }, { Text("Text") })
            FilterChip(w.variant == Variant.TopBottom, { onWorking(w.copy(variant = Variant.TopBottom)) }, { Text("TopBottom") })
        }

        "Bordi" -> {
            // Semplificato: tre preset
            OutlinedButton(onClick = { onWorking(w.copy(border = w.border.copy(color = Color.Transparent, width = 0.dp, shadow = 0.dp))) }) { Text("Nessuno") }
            OutlinedButton(onClick = { onWorking(w.copy(border = w.border.copy(color = Color(0x22000000), width = 1.dp))) }) { Text("Leggero") }
            OutlinedButton(onClick = { onWorking(w.copy(border = w.border.copy(color = Color(0x55000000), width = 2.dp, shadow = 8.dp))) }) { Text("Evidente") }
        }

        "Comportamento" -> {
            FilterChip(w.behavior == Behavior.Normal, { onWorking(w.copy(behavior = Behavior.Normal)) }, { Text("Normale") })
            FilterChip(w.behavior == Behavior.Paged, { onWorking(w.copy(behavior = Behavior.Paged, pages = max(2, w.pages))) }, { Text("Sfogliabile") })
            FilterChip(w.behavior == Behavior.Tabs, { onWorking(w.copy(behavior = Behavior.Tabs, tabsCount = max(2, w.tabsCount))) }, { Text("Tabs") })
        }

        "Azioni" -> {
            // Placeholder: in editor non eseguiamo; qui solo configurazione futura
            Text("Azioni al tap (placeholder): apri pagina, menù, HTTP, evidenzia, input testo…")
        }
    }
}

/* -------------------------
 *  TESTO MENU (placeholder)
 * ------------------------- */
@Composable
private fun TextMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit
) {
    // In questa prima iterazione mostriamo opzioni indicative (riusabili su TextNode)
    FilterChip(false, {}, { Text("Sottolinea") })
    FilterChip(false, {}, { Text("Evidenzia") })
    FilterChip(false, {}, { Text("Font") })
    FilterChip(false, {}, { Text("Weight") })
    FilterChip(false, {}, { Text("Size") })
    FilterChip(false, {}, { Text("Corsivo") })
    FilterChip(false, {}, { Text("Importa font (.ttf)") })
}

/* -------------------------
 *  IMMAGINE MENU (placeholder)
 * ------------------------- */
@Composable
private fun ImageMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit
) {
    FilterChip(false, {}, { Text("Crop (prossimo step)") }, leadingIcon = { Icon(Icons.Filled.Crop, null) })
    FilterChip(false, {}, { Text("Cornice") })
    FilterChip(false, {}, { Text("Album") }, leadingIcon = { Icon(Icons.Filled.Collections, null) })
    FilterChip(false, {}, { Text("Adattamento") })
}

/* -------------------------
 *  INSERISCI (creazione via griglia)
 * ------------------------- */
@Composable
private fun InsertMenu(
    onPath: (List<String>) -> Unit
) {
    // Nella demo la creazione contenitore avviene tap‑tap sulla griglia.
    Text("Suggerimento: tocca due celle della griglia per creare un Contenitore.")
}

/* ==========================================================
 *  # UTILITY
 * ========================================================== */

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

private fun posToCell(x: Float, y: Float, size: IntSize, cols: Int, rows: Int): Pair<Int, Int> {
    val cw = size.width / cols.toFloat()
    val rh = size.height / rows.toFloat()
    val col = min(cols, max(1, (x / cw + 1).toInt()))
    val row = min(rows, max(1, (y / rh + 1).toInt()))
    return col to row
}

private fun selectedContainer(state: EditorState): ContainerNode? =
    state.selection?.let { sel -> state.doc.nodes.firstOrNull { it.id == sel } as? ContainerNode }
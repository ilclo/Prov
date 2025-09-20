package com.example.appbuilder.editor

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/* =========================================================
 *  Mini‑DSL in memoria (v1 super semplice)
 * ========================================================= */

data class RectFrac(
    val left: Float,  // 0..1
    val top: Float,   // 0..1
    val width: Float, // 0..1
    val height: Float // 0..1
) {
    companion object {
        fun fromGrid(startCol: Int, startRow: Int, endCol: Int, endRow: Int, cols: Int, rows: Int): RectFrac {
            val c1 = min(startCol, endCol).coerceIn(0, cols - 1)
            val r1 = min(startRow, endRow).coerceIn(0, rows - 1)
            val c2 = max(startCol, endCol).coerceIn(0, cols - 1)
            val r2 = max(startRow, endRow).coerceIn(0, rows - 1)
            val left = c1 / cols.toFloat()
            val top = r1 / rows.toFloat()
            val width = (c2 - c1 + 1) / cols.toFloat()
            val height = (r2 - r1 + 1) / rows.toFloat()
            return RectFrac(left, top, width, height)
        }
    }
}

enum class Direction { Verticale, Orizzontale }
sealed class PageFill {
    data class Solid(val color: Color) : PageFill()
    data class Gradient(val c1: Color, val c2: Color, val direction: Direction) : PageFill()
    data class Image(val uri: Uri?, val mode: ImageMode) : PageFill() // mode = cover/contain/center ecc. (stub)
}
enum class ImageMode { Cover, Contain, Center }

data class ContainerStyle(
    val name: String,
    val style: String = "full", // full, outlined, text, topbottom
    val cornerDp: Dp = 12.dp,
    val borderColor: Color? = null,
    val borderDp: Dp = 0.dp,
    val shadowDp: Dp = 6.dp,
    val fill: PageFill = PageFill.Solid(Color(0xFFFFFFFF))
)

sealed class EditorNode {
    data class Page(
        val id: String = "page",
        var background: PageFill = PageFill.Gradient(
            c1 = Color(0xFFF5F7FF),
            c2 = Color(0xFFE9ECFF),
            direction = Direction.Verticale
        )
    ) : EditorNode()

    data class Container(
        val id: String,
        var rect: RectFrac,
        var styleName: String = "Default",
        var style: ContainerStyle = ContainerStyle("Default"),
        var scrollable: Boolean = false,
        var anchored: Boolean = false, // es. top/bottom floating bars
        var behavior: String = "normal", // normal | swipeable | tabs
        val children: MutableList<EditorNode> = mutableStateListOf()
    ) : EditorNode()

    data class TextItem(
        val id: String,
        var rect: RectFrac,
        var text: String = "Testo",
        var font: String = "Default",
        var sizeSp: Float = 16f,
        var weight: Int = 400,
        var color: Color = Color(0xFF111111),
        var underline: Boolean = false,
        var italic: Boolean = false
    ) : EditorNode()

    data class IconItem(
        val id: String,
        var rect: RectFrac,
        var name: String = "⭐",
        var color: Color = Color(0xFF333333)
    ) : EditorNode()

    data class ImageItem(
        val id: String,
        var rect: RectFrac,
        var uri: Uri? = null,
        var mode: ImageMode = ImageMode.Cover
    ) : EditorNode()
}

/* =========================================================
 *  Editor State
 * ========================================================= */

data class EditorState(
    val page: EditorNode.Page = EditorNode.Page(),
    val nodes: MutableList<EditorNode> = mutableStateListOf(),
    var selectedId: String? = null,
    var mode: Mode = Mode.Idle,
    var gridCols: Int = 12,
    var gridRows: Int = 20,
    // selection temp
    var selStartCol: Int? = null,
    var selStartRow: Int? = null,
    var selCurCol: Int? = null,
    var selCurRow: Int? = null,
    // palette for container style
    var selectedStyle: ContainerStyle = ContainerStyle("Default")
)

enum class Mode { Idle, DrawingContainer, EditingContainer, DrawingItem }

/* =========================================================
 *  API di alto livello (per Main)
 * ========================================================= */

@Composable
fun AppEditorScreen() {
    val state = remember { EditorState() }
    EditorScaffold(state = state)
}

/* =========================================================
 *  Scaffold con Toolbar + Canvas
 * ========================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScaffold(state: EditorState) {
    var showGrid by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor") },
                actions = {
                    FilterChip(
                        selected = showGrid,
                        onClick = { showGrid = !showGrid },
                        label = { Text("Griglia") })
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { state.mode = Mode.DrawingContainer }) {
                        Text("Nuovo contenitore")
                    }
                }
            )
        },
        bottomBar = {
            EditorBottomBar(state = state)
        }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            EditorCanvas(state = state, showGrid = showGrid)
        }
    }
}

/* =========================================================
 *  Bottom bar (proprietà selezione + palette rapida)
 * ========================================================= */

@Composable
private fun EditorBottomBar(state: EditorState) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sel = state.selectedId?.let { id -> state.nodes.find { it.id() == id } }
            if (sel == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stile contenitore:")
                    StyleChips(state)
                }
            } else {
                SelectedInspector(state, sel)
            }
        }
    }
}

@Composable
private fun StyleChips(state: EditorState) {
    val styles = listOf(
        ContainerStyle("Default", style = "full", cornerDp = 12.dp, fill = PageFill.Solid(Color.White)),
        ContainerStyle("Outline", style = "outlined", borderColor = Color(0xFF9AA0A6), borderDp = 1.dp, cornerDp = 12.dp),
        ContainerStyle("Tonal", style = "full", fill = PageFill.Solid(Color(0xFFF3F6FF)), cornerDp = 14.dp),
        ContainerStyle("TopBottom", style = "topbottom", fill = PageFill.Gradient(Color(0xFFEAF0FF), Color(0xFFDCE6FF), Direction.Verticale))
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        styles.forEach { s ->
            ElevatedFilterChip(
                selected = state.selectedStyle.name == s.name,
                onClick = { state.selectedStyle = s },
                label = { Text(s.name) }
            )
        }
    }
}

@Composable
private fun SelectedInspector(state: EditorState, node: EditorNode) {
    Column(Modifier.fillMaxSize()) {
        when (node) {
            is EditorNode.Container -> ContainerInspector(state, node)
            is EditorNode.TextItem -> TextInspector(state, node)
            is EditorNode.ImageItem -> ImageInspector(state, node)
            is EditorNode.IconItem -> IconInspector(state, node)
            is EditorNode.Page -> {}
        }
    }
}

/* ---------- Inspector: Container ---------- */

@Composable
private fun ContainerInspector(state: EditorState, node: EditorNode.Container) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Contenitore: ${node.id}")
        Spacer(Modifier.width(8.dp))
        ElevatedFilterChip(selected = node.scrollable, onClick = { node.scrollable = !node.scrollable }, label = { Text("Scrollable") })
        ElevatedFilterChip(selected = node.anchored, onClick = { node.anchored = !node.anchored }, label = { Text("Ancorato") })
        Spacer(Modifier.width(12.dp))
        OutlinedButton(onClick = { state.selectedId = null }) { Text("Chiudi") }
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Stile:")
        StyleChipsForContainer(node)
    }
}

@Composable
private fun StyleChipsForContainer(node: EditorNode.Container) {
    val styles = listOf(
        ContainerStyle("Default", style = "full", cornerDp = 12.dp, fill = PageFill.Solid(Color.White)),
        ContainerStyle("Outline", style = "outlined", borderColor = Color(0xFF9AA0A6), borderDp = 1.dp, cornerDp = 12.dp),
        ContainerStyle("Tonal", style = "full", fill = PageFill.Solid(Color(0xFFF3F6FF)), cornerDp = 14.dp),
        ContainerStyle("TopBottom", style = "topbottom", fill = PageFill.Gradient(Color(0xFFEAF0FF), Color(0xFFDCE6FF), Direction.Verticale))
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        styles.forEach { s ->
            FilterChip(
                selected = node.styleName == s.name,
                onClick = { node.styleName = s.name; node.style = s },
                label = { Text(s.name) }
            )
        }
    }
}

/* ---------- Inspector: Text ---------- */

@Composable
private fun TextInspector(state: EditorState, node: EditorNode.TextItem) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Testo: ${node.id}")
        OutlinedTextField(value = node.text, onValueChange = { node.text = it }, label = { Text("Contenuto") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = node.sizeSp.toInt().toString(),
                onValueChange = { v -> v.toIntOrNull()?.let { node.sizeSp = it.toFloat() } },
                label = { Text("Size (sp)") },
                modifier = Modifier.width(120.dp)
            )
            OutlinedTextField(
                value = node.weight.toString(),
                onValueChange = { v -> v.toIntOrNull()?.let { node.weight = it } },
                label = { Text("Weight") },
                modifier = Modifier.width(120.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ElevatedFilterChip(selected = node.underline, onClick = { node.underline = !node.underline }, label = { Text("Sottolineato") })
            ElevatedFilterChip(selected = node.italic, onClick = { node.italic = !node.italic }, label = { Text("Corsivo") })
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = { state.selectedId = null }) { Text("Chiudi") }
    }
}

/* ---------- Inspector: Icon ---------- */

@Composable
private fun IconInspector(state: EditorState, node: EditorNode.IconItem) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Icona: ${node.id}")
        OutlinedTextField(value = node.name, onValueChange = { node.name = it }, label = { Text("Simbolo (es. ⭐)") })
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = { state.selectedId = null }) { Text("Chiudi") }
    }
}

/* ---------- Inspector: Image ---------- */

@Composable
private fun ImageInspector(state: EditorState, node: EditorNode.ImageItem) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Immagine: ${node.id}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = node.mode == ImageMode.Cover, onClick = { node.mode = ImageMode.Cover }, label = { Text("Cover") })
            FilterChip(selected = node.mode == ImageMode.Contain, onClick = { node.mode = ImageMode.Contain }, label = { Text("Contain") })
            FilterChip(selected = node.mode == ImageMode.Center, onClick = { node.mode = ImageMode.Center }, label = { Text("Center") })
        }
        // Stub: picker/crop non collegati (evitiamo dipendenze); lasciare come campo descrittivo
        OutlinedTextField(
            value = node.uri?.toString() ?: "",
            onValueChange = { s -> node.uri = s.takeIf { it.isNotBlank() }?.let(Uri::parse) },
            label = { Text("URI immagine (stub)") }
        )
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = { state.selectedId = null }) { Text("Chiudi") }
    }
}

/* =========================================================
 *  Canvas + Griglia + Selezione/Resize + Rendering
 * ========================================================= */

@Composable
private fun EditorCanvas(state: EditorState, showGrid: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wPx = with(LocalDensity.current) { maxWidth.toPx() }
        val hPx = with(LocalDensity.current) { maxHeight.toPx() }

        // Sfondo pagina
        Box(
            Modifier
                .fillMaxSize()
                .background(pageBrush(state.page.background))
        )

        // Contenitori ed elementi
        state.nodes.forEach { node ->
            when (node) {
                is EditorNode.Container -> ContainerView(node, wPx, hPx, onSelect = { state.selectedId = node.id })
                is EditorNode.TextItem -> TextView(node, wPx, hPx, onSelect = { state.selectedId = node.id })
                is EditorNode.IconItem -> IconView(node, wPx, hPx, onSelect = { state.selectedId = node.id })
                is EditorNode.ImageItem -> ImageView(node, wPx, hPx, onSelect = { state.selectedId = node.id })
                is EditorNode.Page -> {}
            }
        }

        // Griglia + selezione durante drawing
        if (showGrid || state.mode == Mode.DrawingContainer) {
            GridOverlay(
                cols = state.gridCols,
                rows = state.gridRows,
                color = Color(0x33000000)
            )
        }

        if (state.mode == Mode.DrawingContainer) {
            SelectionOverlay(
                cols = state.gridCols,
                rows = state.gridRows,
                onStart = { c, r -> state.selStartCol = c; state.selStartRow = r; state.selCurCol = c; state.selCurRow = r },
                onDrag = { c, r -> state.selCurCol = c; state.selCurRow = r },
                onCommit = {
                    val rect = RectFrac.fromGrid(
                        state.selStartCol ?: 0, state.selStartRow ?: 0,
                        state.selCurCol ?: 0, state.selCurRow ?: 0,
                        state.gridCols, state.gridRows
                    )
                    val id = "c" + (state.nodes.count { it is EditorNode.Container } + 1)
                    state.nodes.add(
                        EditorNode.Container(
                            id = id,
                            rect = rect,
                            styleName = state.selectedStyle.name,
                            style = state.selectedStyle.copy()
                        )
                    )
                    state.selectedId = id
                    state.mode = Mode.Idle
                    state.selStartCol = null; state.selStartRow = null; state.selCurCol = null; state.selCurRow = null
                }
            )
        }
    }
}

/* ---------- Helpers rendering ---------- */

private fun pageBrush(fill: PageFill): Brush {
    return when (fill) {
        is PageFill.Solid -> Brush.linearGradient(listOf(fill.color, fill.color))
        is PageFill.Gradient -> {
            val colors = listOf(fill.c1, fill.c2)
            when (fill.direction) {
                Direction.Verticale -> Brush.verticalGradient(colors)
                Direction.Orizzontale -> Brush.horizontalGradient(colors)
            }
        }
        is PageFill.Image -> Brush.linearGradient(listOf(Color.LightGray, Color.LightGray)) // stub
    }
}

@Composable
private fun ContainerView(node: EditorNode.Container, wPx: Float, hPx: Float, onSelect: () -> Unit) {
    val left = (node.rect.left * wPx).toInt()
    val top = (node.rect.top * hPx).toInt()
    val width = (node.rect.width * wPx).toInt()
    val height = (node.rect.height * hPx).toInt()

    val shape = when {
        node.styleName == "Default" || node.style.style == "full" || node.style.style == "topbottom" ->
            RoundedCornerShape(node.style.cornerDp)
        node.style.style == "outlined" -> RoundedCornerShape(node.style.cornerDp)
        node.style.style == "text" -> RoundedCornerShape(0.dp)
        else -> RoundedCornerShape(node.style.cornerDp)
    }

    val bgBrush = when (val f = node.style.fill) {
        is PageFill.Solid -> Brush.linearGradient(listOf(f.color, f.color))
        is PageFill.Gradient -> when (f.direction) {
            Direction.Verticale -> Brush.verticalGradient(listOf(f.c1, f.c2))
            Direction.Orizzontale -> Brush.horizontalGradient(listOf(f.c1, f.c2))
        }
        is PageFill.Image -> Brush.linearGradient(listOf(Color(0xFFECECEC), Color(0xFFECECEC))) // stub
    }

    val borderModifier = if (node.style.borderDp > 0.dp && node.style.borderColor != null) {
        Modifier.border(node.style.borderDp, node.style.borderColor, shape)
    } else Modifier

    Box(
        Modifier
            .offset { IntOffset(left, top) }
            .size(width.dp, height.dp)
            .clip(shape)
            .background(bgBrush)
            .then(borderModifier)
            .pointerInput(Unit) { detectTapSelect(onSelect) }
    ) {
        // Evidenziazione se selezionato
        // (La selection bar vera è nel bottom inspector; qui solo bordo tratteggiato)
    }
}

@Composable
private fun TextView(node: EditorNode.TextItem, wPx: Float, hPx: Float, onSelect: () -> Unit) {
    val left = (node.rect.left * wPx).toInt()
    val top = (node.rect.top * hPx).toInt()
    val width = (node.rect.width * wPx).toInt()
    val height = (node.rect.height * hPx).toInt()

    Box(
        Modifier
            .offset { IntOffset(left, top) }
            .size(width.dp, height.dp)
            .pointerInput(Unit) { detectTapSelect(onSelect) }
            .padding(4.dp)
    ) {
        Text(
            node.text,
            color = node.color,
            fontSize = node.sizeSp.sp,
            fontWeight = FontWeight(node.weight)
        )
    }
}

@Composable
private fun IconView(node: EditorNode.IconItem, wPx: Float, hPx: Float, onSelect: () -> Unit) {
    val left = (node.rect.left * wPx).toInt()
    val top = (node.rect.top * hPx).toInt()
    val size = ((node.rect.width * wPx + node.rect.height * hPx) / 2f).toInt()

    Box(
        Modifier
            .offset { IntOffset(left, top) }
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .pointerInput(Unit) { detectTapSelect(onSelect) },
        contentAlignment = Alignment.Center
    ) {
        Text(text = node.name, fontSize = 18.sp, color = node.color)
    }
}

@Composable
private fun ImageView(node: EditorNode.ImageItem, wPx: Float, hPx: Float, onSelect: () -> Unit) {
    val left = (node.rect.left * wPx).toInt()
    val top = (node.rect.top * hPx).toInt()
    val width = (node.rect.width * wPx).toInt()
    val height = (node.rect.height * hPx).toInt()
    Box(
        Modifier
            .offset { IntOffset(left, top) }
            .size(width.dp, height.dp)
            .border(1.dp, Color(0xFFDDDDDD))
            .pointerInput(Unit) { detectTapSelect(onSelect) },
        contentAlignment = Alignment.Center
    ) {
        Text("Immagine", color = Color(0xFF888888))
    }
}

/* ---------- Grid overlay & selection ---------- */

@Composable
private fun GridOverlay(cols: Int, rows: Int, color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val cellW = size.width / cols
        val cellH = size.height / rows
        for (c in 1 until cols) {
            drawLine(color, Offset(c * cellW, 0f), Offset(c * cellW, size.height), strokeWidth = 1f)
        }
        for (r in 1 until rows) {
            drawLine(color, Offset(0f, r * cellH), Offset(size.width, r * cellH), strokeWidth = 1f)
        }
    }
}

@Composable
private fun SelectionOverlay(
    cols: Int,
    rows: Int,
    onStart: (col: Int, row: Int) -> Unit,
    onDrag: (col: Int, row: Int) -> Unit,
    onCommit: () -> Unit
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos ->
                        val c = ((pos.x / size.width) * cols).toInt().coerceIn(0, cols - 1)
                        val r = ((pos.y / size.height) * rows).toInt().coerceIn(0, rows - 1)
                        onStart(c, r)
                    },
                    onDrag = { change, _ ->
                        val p = change.position
                        val c = ((p.x / size.width) * cols).toInt().coerceIn(0, cols - 1)
                        val r = ((p.y / size.height) * rows).toInt().coerceIn(0, rows - 1)
                        onDrag(c, r)
                    },
                    onDragEnd = { onCommit() }
                )
            }
    ) {
        // opzionale: potremmo disegnare il rettangolo di selezione con Canvas.
    }
}

/* ---------- Tap helper ---------- */

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapSelect(onSelect: () -> Unit) {
    detectDragGestures(
        onDragStart = { onSelect() },
        onDrag = { _, _ -> },
        onDragEnd = { }
    )
}

/* ---------- Helpers ---------- */

private fun EditorNode.id(): String = when (this) {
    is EditorNode.Page -> id
    is EditorNode.Container -> id
    is EditorNode.TextItem -> id
    is EditorNode.IconItem -> id
    is EditorNode.ImageItem -> id
}

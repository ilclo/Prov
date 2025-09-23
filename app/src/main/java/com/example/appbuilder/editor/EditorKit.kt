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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import java.util.UUID
import androidx.compose.foundation.layout.imePadding

/* ==========================================================
 *  MODEL — document / nodes / styles (minimal v0)
 * ========================================================== */

enum class BgMode { Color, Gradient, Image, Album }
enum class Variant { Full, Outlined, Text, TopBottom }
enum class ShapeKind { Rect, RoundedRect, Circle }
enum class ScrollMode { None, Vertical, Horizontal }
enum class Behavior { Normal, Paged, Tabs }

data class PageStyle(
    val mode: BgMode = BgMode.Color,
    val color1: Color = Color.White,
    val color2: Color = Color(0xFFF5F5F5),
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
    val color1: Color = Color(0xFF1F2937),
    val color2: Color = Color(0xFF111827),
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
    val crop: String? = null   // in futuro passeremo un rect di crop
) : Node

/* Documento pagina (per ora una sola pagina) */
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
 *  ENTRY DEMO — schermata di prova
 * ========================================================== */

@Composable
fun EditorDemoScreen() {
    // Per questa fase mostriamo solo i menù; lo sfondo non è interattivo.
    var state by remember {
        mutableStateOf(
            EditorState(doc = PageDocument(style = PageStyle()))
        )
    }
    EditorRoot(state = state, onStateChange = { state = it })
}

/* ==========================================================
 *  ROOT — canvas neutro + menù
 * ========================================================== */

@Composable
fun EditorRoot(
    state: EditorState,
    onStateChange: (EditorState) -> Unit
) {
    // Path del menù corrente (es. ["Layout","Immagini"])
    var menuPath by remember { mutableStateOf<List<String>>(emptyList()) }

    // Stili “di lavoro” (si confermano con OK)
    var workingPageStyle by remember { mutableStateOf(state.doc.style) }
    var workingContainerStyle by remember { mutableStateOf<ContainerStyle?>(null) }

    // Solo estetica: sfondo bianco, non interattivo
    Box(Modifier.fillMaxSize().background(Color.White)) {

        // === BOTTOM AREA (due barre) ===
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()                  // si appoggia alla tastiera
                .padding(bottom = 56.dp),      // “spazio bottombar”
            verticalArrangement = Arrangement.Bottom
        ) {
            // Barra MENÙ corrente (scrollabile)
            MenuBar(
                menuPath = menuPath,
                onMenuPath = { menuPath = it },
                workingPageStyle = workingPageStyle,
                onWorkingPageStyle = { workingPageStyle = it },
                workingContainerStyle = workingContainerStyle,
                onWorkingContainerStyle = { workingContainerStyle = it },
            )

            Spacer(Modifier.height(8.dp))

            // Barra COMANDI (icone bianche, solo estetica)
            CommandBar(
                onUndo = { /* no-op */ },
                onRedo = { /* no-op */ },
                onSavePage = { /* no-op */ },
                onDelete = { /* no-op */ },
                onDuplicate = { /* no-op */ },
                onProps = { /* no-op */ },
                onPage = { menuPath = listOf("Layout") },
                onCreate = { /* apri menù a tendina in futuro */ },
                onList = { /* no-op */ },
                onOk = {
                    // Applica eventuali modifiche allo stile pagina
                    if (menuPath.firstOrNull() == "Layout") {
                        onStateChange(state.copy(doc = state.doc.copy(style = workingPageStyle)))
                    }
                    menuPath = emptyList()
                },
                onSaveProject = { /* no-op */ },
                onFolder = { /* no-op */ },
                onNewProject = { /* no-op */ }
            )
        }
    }
}

/* ==========================================================
 *  BARRA MENÙ (superiore fra le due)
 * ========================================================== */

@Composable
private fun MenuBar(
    menuPath: List<String>,
    onMenuPath: (List<String>) -> Unit,
    workingPageStyle: PageStyle,
    onWorkingPageStyle: (PageStyle) -> Unit,
    workingContainerStyle: ContainerStyle?,
    onWorkingContainerStyle: (ContainerStyle?) -> Unit,
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
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
                // Solo icone (layout / contenitore / testo / immagine / aggiungi)
                IconButton(onClick = { onMenuPath(listOf("Layout")) }) { Icon(Icons.Filled.Description, null, tint = Color.White) }
                IconButton(onClick = { onMenuPath(listOf("Contenitore")) }) { Icon(Icons.Filled.BorderColor, null, tint = Color.White) }
                IconButton(onClick = { onMenuPath(listOf("Testo")) }) { Icon(Icons.Filled.TextFields, null, tint = Color.White) }
                IconButton(onClick = { onMenuPath(listOf("Immagine")) }) { Icon(Icons.Filled.Image, null, tint = Color.White) }
                IconButton(onClick = { onMenuPath(listOf("Inserisci")) }) { Icon(Icons.Filled.Add, null, tint = Color.White) }
            } else {
                when (menuPath.first()) {
                    "Layout" -> LayoutMenu(menuPath, onMenuPath, workingPageStyle, onWorkingPageStyle)
                    "Contenitore" -> ContainerMenu(menuPath, onMenuPath, workingContainerStyle, onWorkingContainerStyle)
                    "Testo" -> TextMenu(menuPath, onMenuPath)   // placeholder estetico
                    "Immagine" -> ImageMenu(menuPath, onMenuPath) // placeholder estetico
                    "Inserisci" -> InsertMenu(onMenuPath)       // placeholder
                }
            }
        }
    }
}

/* ==========================================================
 *  BARRA COMANDI (inferiore)
 * ========================================================== */

@Composable
private fun CommandBar(
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSavePage: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onProps: () -> Unit,
    onPage: () -> Unit,
    onCreate: () -> Unit,
    onList: () -> Unit,
    onOk: () -> Unit,
    onSaveProject: () -> Unit,
    onFolder: () -> Unit,
    onNewProject: () -> Unit,
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onUndo) { Icon(Icons.Filled.Undo, null, tint = Color.White) }
                IconButton(onClick = onRedo) { Icon(Icons.Filled.Redo, null, tint = Color.White) }
                IconButton(onClick = onSavePage) { Icon(Icons.Filled.Save, null, tint = Color.White) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null, tint = Color.White) }
                IconButton(onClick = onDuplicate) { Icon(Icons.Filled.ContentCopy, null, tint = Color.White) }
                IconButton(onClick = onProps) { Icon(Icons.Filled.Settings, null, tint = Color.White) }
                IconButton(onClick = onPage) { Icon(Icons.Filled.Description, null, tint = Color.White) }
                IconButton(onClick = onCreate) { Icon(Icons.Filled.CreateNewFolder, null, tint = Color.White) }
                IconButton(onClick = onList) { Icon(Icons.Filled.List, null, tint = Color.White) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Ok conferma (per ora solo estetica; in EditorRoot applichiamo lo stile Layout)
                FilledIconButton(onClick = onOk) { Icon(Icons.Filled.Check, null, tint = Color.White) }
                IconButton(onClick = onSaveProject) { Icon(Icons.Filled.SaveAs, null, tint = Color.White) }
                IconButton(onClick = onFolder) { Icon(Icons.Filled.FolderOpen, null, tint = Color.White) }
                IconButton(onClick = onNewProject) { Icon(Icons.Filled.NoteAdd, null, tint = Color.White) }
            }
        }
    }
}

/* ==========================================================
 *  MENU: LAYOUT (pagina)
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
            IconButton(onClick = { onPath(path + "Colore") }) { Icon(Icons.Filled.ColorLens, null, tint = Color.White) }
            IconButton(onClick = { onPath(path + "Immagini") }) { Icon(Icons.Filled.Image, null, tint = Color.White) }
        }

        "Colore" -> {
            SuggestChip(selected = working.mode == BgMode.Color, onClick = { onWorking(working.copy(mode = BgMode.Color)) }, label = { Text("Colore") })
            SuggestChip(selected = working.mode == BgMode.Gradient, onClick = { onWorking(working.copy(mode = BgMode.Gradient)) }, label = { Text("Gradiente") })
            Spacer(Modifier.width(8.dp))
            listOf(Color(0xFF0EA5E9), Color(0xFF9333EA), Color(0xFFEF4444), Color(0xFF10B981)).forEach {
                ColorDot(it) { c -> onWorking(working.copy(color1 = c)) }
            }
        }

        "Immagini" -> {
            val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) onWorking(working.copy(mode = BgMode.Image, image = uri))
            }
            val pickAlbum = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                if (uris.isNotEmpty()) onWorking(working.copy(mode = BgMode.Album, album = uris))
            }
            OutlinedButton(onClick = { pickImage.launch("image/*") }) { Icon(Icons.Filled.Crop, null); Spacer(Modifier.width(6.dp)); Text("Immagine (crop a parte)") }
            OutlinedButton(onClick = { pickAlbum.launch("image/*") }) { Icon(Icons.Filled.Collections, null); Spacer(Modifier.width(6.dp)); Text("Album (multi)") }
        }
    }
}

/* ==========================================================
 *  MENU: CONTENITORE (placeholder estetico)
 * ========================================================== */

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
            IconButton(onClick = { onPath(path + "Colore") }) { Icon(Icons.Filled.ColorLens, null, tint = Color.White) }
            IconButton(onClick = { onPath(path + "Immagini") }) { Icon(Icons.Filled.Image, null, tint = Color.White) }
            SuggestChip(false, { onPath(path + "Scroll") }, { Text("Scroll") })
            SuggestChip(false, { onPath(path + "Forma") }, { Text("Forma") })
            SuggestChip(false, { onPath(path + "Stile") }, { Text("Stile") })
            SuggestChip(false, { onPath(path + "Bordi") }, { Text("Bordi") })
            SuggestChip(false, { onPath(path + "Comportamento") }, { Text("Tipo") })
        }

        "Colore" -> {
            SuggestChip(w.bgMode == BgMode.Color, { onWorking(w.copy(bgMode = BgMode.Color)) }, { Text("Colore") })
            SuggestChip(w.bgMode == BgMode.Gradient, { onWorking(w.copy(bgMode = BgMode.Gradient)) }, { Text("Gradiente") })
            Spacer(Modifier.width(8.dp))
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
            OutlinedButton(onClick = { pickImage.launch("image/*") }) { Icon(Icons.Filled.Crop, null); Spacer(Modifier.width(6.dp)); Text("Immagine (crop a parte)") }
            OutlinedButton(onClick = { pickAlbum.launch("image/*") }) { Icon(Icons.Filled.Collections, null); Spacer(Modifier.width(6.dp)); Text("Album (multi)") }
        }

        "Scroll" -> {
            SuggestChip(w.scroll == ScrollMode.None, { onWorking(w.copy(scroll = ScrollMode.None)) }, { Text("Nessuna") })
            SuggestChip(w.scroll == ScrollMode.Vertical, { onWorking(w.copy(scroll = ScrollMode.Vertical)) }, { Text("Verticale") })
            SuggestChip(w.scroll == ScrollMode.Horizontal, { onWorking(w.copy(scroll = ScrollMode.Horizontal)) }, { Text("Orizzontale") })
        }

        "Forma" -> {
            SuggestChip(w.shape == ShapeKind.Rect, { onWorking(w.copy(shape = ShapeKind.Rect)) }, { Text("Rettangolo") })
            SuggestChip(w.shape == ShapeKind.RoundedRect, { onWorking(w.copy(shape = ShapeKind.RoundedRect)) }, { Text("Arrotondato") })
            SuggestChip(w.shape == ShapeKind.Circle, { onWorking(w.copy(shape = ShapeKind.Circle)) }, { Text("Cerchio") })
        }

        "Stile" -> {
            SuggestChip(w.variant == Variant.Full, { onWorking(w.copy(variant = Variant.Full)) }, { Text("Full") })
            SuggestChip(w.variant == Variant.Outlined, { onWorking(w.copy(variant = Variant.Outlined)) }, { Text("Outlined") })
            SuggestChip(w.variant == Variant.Text, { onWorking(w.copy(variant = Variant.Text)) }, { Text("Text") })
            SuggestChip(w.variant == Variant.TopBottom, { onWorking(w.copy(variant = Variant.TopBottom)) }, { Text("TopBottom") })
        }

        "Bordi" -> {
            OutlinedButton(onClick = { onWorking(w.copy(border = w.border.copy(color = Color.Transparent, width = 0.dp, shadow = 0.dp))) }) { Text("Nessuno") }
            OutlinedButton(onClick = { onWorking(w.copy(border = w.border.copy(color = Color(0x22000000), width = 1.dp))) }) { Text("Leggero") }
            OutlinedButton(onClick = { onWorking(w.copy(border = w.border.copy(color = Color(0x55000000), width = 2.dp, shadow = 8.dp))) }) { Text("Evidente") }
        }

        "Comportamento" -> {
            SuggestChip(w.behavior == Behavior.Normal, { onWorking(w.copy(behavior = Behavior.Normal)) }, { Text("Normale") })
            SuggestChip(w.behavior == Behavior.Paged, { onWorking(w.copy(behavior = Behavior.Paged, pages = max(2, w.pages))) }, { Text("Sfogliabile") })
            SuggestChip(w.behavior == Behavior.Tabs, { onWorking(w.copy(behavior = Behavior.Tabs, tabsCount = max(2, w.tabsCount))) }, { Text("Tabs") })
        }
    }
}

/* ==========================================================
 *  MENU: TESTO / IMMAGINE / INSERISCI (placeholder, estetica)
 * ========================================================== */

@Composable
private fun TextMenu(path: List<String>, onPath: (List<String>) -> Unit) {
    AssistChip(onClick = {}, label = { Text("Sottolinea") }, leadingIcon = { Icon(Icons.Filled.FormatUnderlined, null) })
    AssistChip(onClick = {}, label = { Text("Corsivo") }, leadingIcon = { Icon(Icons.Filled.FormatItalic, null) })
    AssistChip(onClick = {}, label = { Text("Evidenzia") }, leadingIcon = { Icon(Icons.Filled.FormatColorFill, null) })
    AssistChip(onClick = {}, label = { Text("Font") }, leadingIcon = { Icon(Icons.Filled.FontDownload, null) })
    AssistChip(onClick = {}, label = { Text("Size") }, leadingIcon = { Icon(Icons.Filled.FormatSize, null) })
    AssistChip(onClick = {}, label = { Text("Importa .ttf") }, leadingIcon = { Icon(Icons.Filled.UploadFile, null) })
}

@Composable
private fun ImageMenu(path: List<String>, onPath: (List<String>) -> Unit) {
    AssistChip(onClick = {}, label = { Text("Crop") }, leadingIcon = { Icon(Icons.Filled.Crop, null) })
    AssistChip(onClick = {}, label = { Text("Cornice") }, leadingIcon = { Icon(Icons.Filled.CropSquare, null) })
    AssistChip(onClick = {}, label = { Text("Album") }, leadingIcon = { Icon(Icons.Filled.Collections, null) })
    AssistChip(onClick = {}, label = { Text("Adattamento") }, leadingIcon = { Icon(Icons.Filled.AspectRatio, null) })
}

@Composable
private fun InsertMenu(onPath: (List<String>) -> Unit) {
    Text("Placeholder: qui andranno le azioni per aggiungere contenuti.")
}

/* ==========================================================
 *  UTILITY
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
                detectTapGestures(onTap = { onPick(color) })
            }
    )
    Spacer(Modifier.width(4.dp))
}

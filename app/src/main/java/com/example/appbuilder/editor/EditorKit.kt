package com.example.appbuilder.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlin.math.max
import kotlin.math.min
import java.util.UUID

/* ==========================================================
 *  MODEL — minimal: solo stato editor/menù
 * ========================================================== */

enum class BgMode { Color, Gradient, Image, Album }
enum class Variant { Full, Outlined, Text, TopBottom }
enum class ShapeKind { Rect, RoundedRect, Circle }
enum class ScrollMode { None, Vertical, Horizontal }
enum class Behavior { Normal, Paged, Tabs }

data class PageStyle(
    val mode: BgMode = BgMode.Color,
    val color1: Color = Color(0xFF0B0C10),
    val color2: Color = Color(0xFF16181D),
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
    val color1: Color = Color(0xFF111214),
    val color2: Color = Color(0xFF1B1C21),
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

data class PageDocument(
    val gridCols: Int = 12,
    val gridRows: Int = 24,
    val style: PageStyle = PageStyle()
)

data class EditorState(
    val doc: PageDocument = PageDocument()
)

/* ==========================================================
 *  ENTRY DEMO
 * ========================================================== */

@Composable
fun EditorScreen() {
    var state by remember { mutableStateOf(EditorState()) }

    // Menù navigation
    var menuPath by remember { mutableStateOf<List<String>>(emptyList()) }   // es. ["Layout","Colore","Gradiente"]
    var lastChangedLabel by remember { mutableStateOf<String?>(null) }       // es. "Gradiente: Verticale"
    var dirty by remember { mutableStateOf(false) }                          // modifiche fatte nel menù corrente
    var showConfirm by remember { mutableStateOf(false) }                    // pannello conferma

    // Working copies (non applichiamo nulla davvero in questa fase)
    var pageStyle by remember { mutableStateOf(state.doc.style) }
    var containerStyle by remember { mutableStateOf(ContainerStyle()) } // placeholder per i menù Contenitore

    // ========= Gestione uscita menù =========
    fun tryLeaveMenu() {
        if (dirty) {
            showConfirm = true
        } else {
            menuPath = emptyList()
            lastChangedLabel = null
        }
    }

    fun applyAndLeave() {
        // (futuro) salva pageStyle/containerStyle in state
        dirty = false
        showConfirm = false
        menuPath = emptyList()
        lastChangedLabel = null
    }

    fun cancelAndLeave() {
        // (futuro) ripristina working dagli effettivi
        dirty = false
        showConfirm = false
        menuPath = emptyList()
        lastChangedLabel = null
    }

    BackHandler(enabled = menuPath.isNotEmpty() || showConfirm) {
        if (showConfirm) cancelAndLeave() else tryLeaveMenu()
    }

    // ========= LAYOUT =========
    Box(Modifier.fillMaxSize().background(Color.White)) {
        // (Canvas vuoto, solo sfondo)
        Canvas(Modifier.fillMaxSize()) { /* vuoto */ }

        // === BOTTOM AREA ===
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                showConfirm -> {
                    ConfirmBar(
                        onCancel = { cancelAndLeave() },
                        onOk = { applyAndLeave() },
                        onSavePreset = { /* dialog di naming */ }
                    )
                }
                menuPath.isNotEmpty() -> {
                    PathBar(menuPath, lastChangedLabel, onBack = { tryLeaveMenu() })
                }
                else -> {
                    // MODE BAR (sopra)
                    ModeBar(
                        onLayout = { menuPath = listOf("Layout") },
                        onContainer = { menuPath = listOf("Contenitore") },
                        onText = { menuPath = listOf("Testo") },
                        onAdd = { menuPath = listOf("Aggiungi") }
                    )
                    // spazio tra le 2 barre
                    Spacer(Modifier.height(8.dp))
                    // COMMAND BAR (sotto)
                    CommandBar()
                }
            }
        }

        // === CONTENUTO DEL MENÙ (compare sopra le barre quando entri) ===
        if (menuPath.isNotEmpty() && !showConfirm) {
            MenuSheet(
                path = menuPath,
                onPath = { menuPath = it },
                onDirty = { dirty = true },
                onLastChanged = { lastChangedLabel = it },
                pageStyle = pageStyle,
                onPageStyle = { pageStyle = it; dirty = true },
                contStyle = containerStyle,
                onContStyle = { containerStyle = it; dirty = true }
            )
        }
    }
}

/* ==========================================================
 *  BOTTOM: MODE BAR (icone bianche)
 * ========================================================== */

@Composable
private fun ModeBar(
    onLayout: () -> Unit,
    onContainer: () -> Unit,
    onText: () -> Unit,
    onAdd: () -> Unit
) {
    Surface(
        color = Color(0xFF0D1117),
        contentColor = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(onClick = onText, colors = iconBtnDark()) { Icon(Icons.Filled.TextFields, null) }
            FilledIconButton(onClick = onContainer, colors = iconBtnDark()) { Icon(Icons.Filled.Widgets, null) }
            FilledIconButton(onClick = onLayout, colors = iconBtnDark()) { Icon(Icons.Filled.Tab, null) }
            FilledIconButton(onClick = onAdd, colors = iconBtnDark()) { Icon(Icons.Filled.Add, null) }
        }
    }
}

private fun iconBtnDark() = IconButtonDefaults.filledIconButtonColors(
    containerColor = Color(0xFF1F232A),
    contentColor = Color.White
)

/* ==========================================================
 *  BOTTOM: COMMAND BAR (icone bianche, nessun testo)
 * ========================================================== */

@Composable
private fun CommandBar() {
    var showCreate by remember { mutableStateOf(false) }
    var showList by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF0D1117),
        contentColor = Color.White,
        tonalElevation = 10.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledIconButton(onClick = { /* undo */ }, colors = iconBtnDark()) { Icon(Icons.Filled.Undo, null) }
                FilledIconButton(onClick = { /* redo */ }, colors = iconBtnDark()) { Icon(Icons.Filled.Redo, null) }
                FilledIconButton(onClick = { /* save page */ }, colors = iconBtnDark()) { Icon(Icons.Filled.Save, null) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledIconButton(onClick = { /* dup */ }, colors = iconBtnDark()) { Icon(Icons.Filled.ContentCopy, null) }
                FilledIconButton(onClick = { /* delete */ }, colors = iconBtnDark()) { Icon(Icons.Filled.Delete, null) }
                FilledIconButton(onClick = { /* properties */ }, colors = iconBtnDark()) { Icon(Icons.Filled.Settings, null) }
                // Create (dropdown)
                Box {
                    FilledIconButton(onClick = { showCreate = !showCreate }, colors = iconBtnDark()) { Icon(Icons.Filled.CreateNewFolder, null) }
                    DropdownMenu(expanded = showCreate, onDismissRequest = { showCreate = false }) {
                        DropdownMenuItem(text = { Text("Nuova pagina") }, onClick = { showCreate = false })
                        DropdownMenuItem(text = { Text("Nuovo avviso") }, onClick = { showCreate = false })
                        DropdownMenuItem(text = { Text("Menù laterale") }, onClick = { showCreate = false })
                        DropdownMenuItem(text = { Text("Menù centrale") }, onClick = { showCreate = false })
                    }
                }
                // List (dropdown)
                Box {
                    FilledIconButton(onClick = { showList = !showList }, colors = iconBtnDark()) { Icon(Icons.Filled.List, null) }
                    DropdownMenu(expanded = showList, onDismissRequest = { showList = false }) {
                        DropdownMenuItem(text = { Text("Home") }, onClick = { showList = false })
                        DropdownMenuItem(text = { Text("Impostazioni") }, onClick = { showList = false })
                        DropdownMenuItem(text = { Text("Avvisi salvati") }, onClick = { showList = false })
                    }
                }
                FilledIconButton(onClick = { /* save project */ }, colors = iconBtnDark()) { Icon(Icons.Filled.SaveAs, null) }
                FilledIconButton(onClick = { /* open/import */ }, colors = iconBtnDark()) { Icon(Icons.Filled.FolderOpen, null) }
                FilledIconButton(onClick = { /* new project */ }, colors = iconBtnDark()) { Icon(Icons.Filled.NoteAdd, null) }
            }
        }
    }
}

/* ==========================================================
 *  PATH BAR (quando sei dentro ai menù)
 * ========================================================== */

@Composable
private fun PathBar(path: List<String>, lastChanged: String?, onBack: () -> Unit) {
    Surface(
        color = Color(0xFF0D1117),
        contentColor = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(onClick = onBack, colors = iconBtnDark()) { Icon(Icons.Filled.ArrowBack, null) }
            Spacer(Modifier.width(8.dp))
            val crumb = path.joinToString("  →  ")
            val tail = lastChanged?.let { " — $it" } ?: ""
            Text(crumb + tail, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/* ==========================================================
 *  CONFIRM BAR (OK / ANNULLA / SALVA IMPOSTAZIONI)
 * ========================================================== */

@Composable
private fun ConfirmBar(
    onCancel: () -> Unit,
    onOk: () -> Unit,
    onSavePreset: () -> Unit
) {
    Surface(
        color = Color(0xFF0D1117),
        contentColor = Color.White,
        tonalElevation = 12.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Modifiche non salvate", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(onClick = onCancel, colors = iconBtnDark()) { Icon(Icons.Filled.Close, null) }
                FilledIconButton(onClick = onSavePreset, colors = iconBtnDark()) { Icon(Icons.Filled.BookmarkAdd, null) }
                FilledIconButton(onClick = onOk, colors = iconBtnDark()) { Icon(Icons.Filled.Check, null) }
            }
        }
    }
}

/* ==========================================================
 *  MENU SHEET (contenuto dei sottomenu richiesti)
 *  Solo estetica + aggiornamento "dirty" e "lastChanged"
 * ========================================================== */

@Composable
private fun MenuSheet(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    onDirty: () -> Unit,
    onLastChanged: (String) -> Unit,
    pageStyle: PageStyle,
    onPageStyle: (PageStyle) -> Unit,
    contStyle: ContainerStyle,
    onContStyle: (ContainerStyle) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.45f)
            .padding(bottom = 8.dp),
        color = Color(0xFF0D1117),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        val scroll = rememberScrollState()
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp)
                .horizontalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Primo livello
            when (path.firstOrNull()) {
                "Layout" -> LayoutMenu(path, onPath, pageStyle, onPageStyle, onDirty, onLastChanged)
                "Contenitore" -> ContainerMenu(path, onPath, contStyle, onContStyle, onDirty, onLastChanged)
                "Testo" -> TextMenu(path, onPath, onDirty, onLastChanged)
                "Aggiungi" -> AddMenu(path, onPath, onDirty, onLastChanged)
            }
        }
    }
}

/* -------------------------
 *  LAYOUT
 * ------------------------- */
@Composable
private fun LayoutMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    working: PageStyle,
    onWorking: (PageStyle) -> Unit,
    onDirty: () -> Unit,
    onLastChanged: (String) -> Unit
) {
    when (path.getOrNull(1)) {
        null -> {
            IconChip(Icons.Filled.ColorLens) { onPath(path + "Colore") }
            IconChip(Icons.Filled.Image) { onPath(path + "Immagini") }
            IconChip(Icons.Filled.Style) { onPath(path + "Scegli default") }
        }
        "Colore" -> {
            // colore1 / colore2 / gradiente (H/V) + 2 effetti grafici
            ColorPickerIcon("Colore 1", working.color1) { c ->
                onWorking(working.copy(color1 = c)); onDirty(); onLastChanged("Colore 1")
            }
            ColorPickerIcon("Colore 2", working.color2) { c ->
                onWorking(working.copy(color2 = c)); onDirty(); onLastChanged("Colore 2")
            }

            // Gradiente (H / V)
            var grad by remember { mutableStateOf(if (working.gradientAngleDeg == 90f) "Verticale" else "Orizzontale") }
            DropIcon(
                icon = Icons.Filled.Gradient,
                value = grad,
                options = listOf("Orizzontale", "Verticale")
            ) {
                grad = it
                val angle = if (it == "Verticale") 90f else 0f
                onWorking(working.copy(mode = BgMode.Gradient, gradientAngleDeg = angle))
                onDirty(); onLastChanged("Gradiente: $it")
            }

            // Effetti: “Noise” e “Split” come toggle (solo estetica)
            ToggleIcon(label = "Noise", icon = Icons.Filled.BlurOn)
            ToggleIcon(label = "Split", icon = Icons.Filled.AutoAwesome)
        }
        "Immagini" -> {
            // Aggiungi foto
            IconChip(Icons.Filled.Photo) { onPath(path + "Aggiungi foto") }
            // Aggiungi album
            IconChip(Icons.Filled.Collections) { onPath(path + "Aggiungi album") }
        }
        "Aggiungi foto" -> {
            val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    onWorking(working.copy(mode = BgMode.Image, image = uri)); onDirty()
                    onLastChanged("Immagine selezionata")
                }
            }
            IconChip(Icons.Filled.Crop) { /* crop (placeholder) */ onLastChanged("Crop") }
            IconChip(Icons.Filled.PhotoFilter) { /* cornice/filtri (placeholder) */ onLastChanged("Filtro foto") }
            DropIcon(Icons.Filled.AspectRatio, value = "Cover", options = listOf("Cover", "Contain", "FitWidth", "FitHeight")) {
                onLastChanged("Adattamento: $it"); onDirty()
            }
            IconChip(Icons.Filled.Image) { pick.launch("image/*") }
        }
        "Aggiungi album" -> {
            IconChip(Icons.Filled.Crop) { onLastChanged("Crop album") }
            IconChip(Icons.Filled.PhotoFilter) { onLastChanged("Filtro album") }
            DropIcon(Icons.Filled.AspectRatio, value = "Cover", options = listOf("Cover", "Contain", "FitWidth", "FitHeight")) {
                onLastChanged("Adattamento: $it"); onDirty()
            }
            DropIcon(Icons.Filled.Movie, value = "Slide", options = listOf("Slide", "Fade", "Flip")) {
                onLastChanged("Animazione: $it"); onDirty()
            }
            DropIcon(Icons.Filled.Speed, value = "1x", options = listOf("0.5x", "1x", "1.5x", "2x")) {
                onLastChanged("Velocità: $it"); onDirty()
            }
        }
        "Scegli default" -> {
            DropIcon(Icons.Filled.Bookmarks, value = "Default 1", options = listOf("Default 1", "Default 2", "Default 3")) {
                onLastChanged("Default: $it")
            }
        }
    }
}

/* -------------------------
 *  CONTENITORE
 * ------------------------- */
@Composable
private fun ContainerMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    working: ContainerStyle,
    onWorking: (ContainerStyle) -> Unit,
    onDirty: () -> Unit,
    onLastChanged: (String) -> Unit
) {
    when (path.getOrNull(1)) {
        null -> {
            IconChip(Icons.Filled.ColorLens) { onPath(path + "Colore") }
            IconChip(Icons.Filled.Image) { onPath(path + "Immagini") }
            IconChip(Icons.Filled.Swipe) { onPath(path + "Scrollabilità") }
            IconChip(Icons.Filled.CropSquare) { onPath(path + "Forma") }
            IconChip(Icons.Filled.Style) { onPath(path + "Stile") }
            IconChip(Icons.Filled.BorderColor) { onPath(path + "Bordi") }
            IconChip(Icons.Filled.BrowseGallery) { onPath(path + "Tipo") }
            IconChip(Icons.Filled.TouchApp) { onPath(path + "Azione al tap") }
            IconChip(Icons.Filled.Bookmarks) { onPath(path + "Scegli default") }
        }
        "Colore" -> {
            ColorPickerIcon("Colore 1", working.color1) { c ->
                onWorking(working.copy(color1 = c)); onDirty(); onLastChanged("Colore 1")
            }
            ColorPickerIcon("Colore 2", working.color2) { c ->
                onWorking(working.copy(color2 = c)); onDirty(); onLastChanged("Colore 2")
            }
            var grad by remember { mutableStateOf(if (working.gradientAngleDeg == 90f) "Verticale" else "Orizzontale") }
            DropIcon(Icons.Filled.Gradient, grad, listOf("Orizzontale","Verticale")) {
                grad = it
                val angle = if (it == "Verticale") 90f else 0f
                onWorking(working.copy(bgMode = BgMode.Gradient, gradientAngleDeg = angle))
                onDirty(); onLastChanged("Gradiente: $it")
            }
            ToggleIcon("Noise", Icons.Filled.BlurOn)
            ToggleIcon("Split", Icons.Filled.AutoAwesome)
        }
        "Immagini" -> {
            IconChip(Icons.Filled.Crop) { onLastChanged("Crop") }
            IconChip(Icons.Filled.PhotoFilter) { onLastChanged("Cornice / Effetti") }
            DropIcon(Icons.Filled.AspectRatio, "Contain", listOf("Contain","Cover","FitWidth","FitHeight")) {
                onLastChanged("Adattamento: $it"); onDirty()
            }
        }
        "Scrollabilità" -> {
            ToggleChoice(
                options = listOf("Assente","Verticale","Orizzontale"),
                values = listOf(ScrollMode.None, ScrollMode.Vertical, ScrollMode.Horizontal),
                current = working.scroll
            ) { label, value ->
                onWorking(working.copy(scroll = value)); onDirty(); onLastChanged("Scroll: $label")
            }
        }
        "Forma" -> {
            ToggleChoice(
                options = listOf("Rettangolo", "Arrotondato", "Cerchio"),
                values = listOf(ShapeKind.Rect, ShapeKind.RoundedRect, ShapeKind.Circle),
                current = working.shape
            ) { label, value ->
                onWorking(working.copy(shape = value)); onDirty(); onLastChanged("Forma: $label")
            }
            // Angoli (globale semplificato)
            DropIcon(Icons.Filled.Corners, "12dp", listOf("0dp","8dp","12dp","16dp","24dp")) {
                val v = it.removeSuffix("dp").toInt().dp
                onWorking(working.copy(corners = working.corners.copy(
                    topStart = v, topEnd = v, bottomStart = v, bottomEnd = v
                ))); onDirty(); onLastChanged("Angoli: $it")
            }
        }
        "Stile" -> {
            ToggleChoice(
                options = listOf("Full","Outlined","Text","TopBottom"),
                values = listOf(Variant.Full, Variant.Outlined, Variant.Text, Variant.TopBottom),
                current = working.variant
            ) { label, value ->
                onWorking(working.copy(variant = value)); onDirty(); onLastChanged("Stile: $label")
            }
        }
        "Bordi" -> {
            DropIcon(Icons.Filled.BorderColor, "Leggero", listOf("Nessuno","Leggero","Medio","Forte")) {
                onLastChanged("Bordo: $it"); onDirty()
            }
            DropIcon(Icons.Filled.Shadow, "Ombra 4dp", listOf("0dp","4dp","8dp","12dp")) {
                onLastChanged("Ombra: $it"); onDirty()
            }
        }
        "Tipo" -> {
            // Sfogliabile
            IconChip(Icons.Filled.ViewCarousel) { onPath(path + "Sfogliabile") }
            // Tabs
            IconChip(Icons.Filled.Tab) { onPath(path + "Tabs") }
        }
        "Sfogliabile" -> {
            DropIcon(Icons.Filled.Numbers, "3 pagine", listOf("2 pagine","3 pagine","4 pagine","5 pagine")) {
                onLastChanged("Pagine: $it"); onDirty()
            }
            DropIcon(Icons.Filled.MoreHoriz, "Pallini", listOf("Pallini","Linee","Nessuno")) {
                onLastChanged("Indicatore: $it"); onDirty()
            }
        }
        "Tabs" -> {
            DropIcon(Icons.Filled.Numbers, "3 tabs", listOf("2 tabs","3 tabs","4 tabs","5 tabs")) {
                onLastChanged("Tabs: $it"); onDirty()
            }
            DropIcon(Icons.Filled.Architecture, "Underline", listOf("Underline","Pill","Outline")) {
                onLastChanged("Forma Tab: $it"); onDirty()
            }
            DropIcon(Icons.Filled.Highlight, "Evidenzia", listOf("Evidenzia","Scala","Sottolinea")) {
                onLastChanged("Selettore: $it"); onDirty()
            }
        }
        "Azione al tap" -> {
            // Solo elenco estetico
            listOf(
                "Evidenzia" to Icons.Filled.Highlight,
                "Apri pagina" to Icons.Filled.OpenInNew,
                "Apri menù" to Icons.Filled.Menu,
                "Menù a tendina" to Icons.Filled.ArrowDropDownCircle,
                "Chiamata backend" to Icons.Filled.Cloud,
                "Inserimento testo" to Icons.Filled.TextFields
            ).forEach { (label, icon) ->
                IconChip(icon) { onLastChanged("Azione: $label"); onDirty() }
            }
        }
        "Scegli default" -> {
            DropIcon(Icons.Filled.Bookmarks, "Card base", listOf("Card base","Hero","Box outline")) {
                onLastChanged("Default: $it")
            }
        }
    }
}

/* -------------------------
 *  TESTO
 * ------------------------- */
@Composable
private fun TextMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    onDirty: () -> Unit,
    onLastChanged: (String) -> Unit
) {
    // Toggle con bordo spesso quando selezionati
    var underline by remember { mutableStateOf(false) }
    var italic by remember { mutableStateOf(false) }
    var highlight by remember { mutableStateOf(false) }

    ToggleIcon("Sottolinea", Icons.Filled.FormatUnderlined, selected = underline) {
        underline = !underline; onDirty(); onLastChanged("Sottolinea: ${if (underline) "ON" else "OFF"}")
    }
    ToggleIcon("Corsivo", Icons.Filled.FormatItalic, selected = italic) {
        italic = !italic; onDirty(); onLastChanged("Corsivo: ${if (italic) "ON" else "OFF"}")
    }
    ToggleIcon("Evidenzia", Icons.Filled.FormatColorFill, selected = highlight) {
        highlight = !highlight; onDirty(); onLastChanged("Evidenzia: ${if (highlight) "ON" else "OFF"}")
    }

    // Dropdown: Font / Weight / Size / Importa
    DropIcon(Icons.Filled.FontDownload, "Default", listOf("Default","Inter","Lato","Roboto Mono")) {
        onDirty(); onLastChanged("Font: $it")
    }
    DropIcon(Icons.Filled.DensitySmall, "Medium", listOf("Light","Regular","Medium","Bold")) {
        onDirty(); onLastChanged("Weight: $it")
    }
    DropIcon(Icons.Filled.FormatSize, "16sp", listOf("12sp","14sp","16sp","18sp","20sp")) {
        onDirty(); onLastChanged("Size: $it")
    }
    IconChip(Icons.Filled.Download) { onLastChanged("Importa font (.ttf)"); onDirty() }
    DropIcon(Icons.Filled.Style, "Nessuna evid.", listOf("Nessuna evid.","Oblique","Brush","Marker")) {
        onDirty(); onLastChanged("Evidenziazione: $it")
    }
}

/* -------------------------
 *  AGGIUNGI (Icona/Toggle/Slider)
 * ------------------------- */
@Composable
private fun AddMenu(
    path: List<String>,
    onPath: (List<String>) -> Unit,
    onDirty: () -> Unit,
    onLastChanged: (String) -> Unit
) {
    // Icona
    IconChip(Icons.Filled.InsertEmoticon) { onPath(path + "Icona") }
    // Toggle
    IconChip(Icons.Filled.ToggleOn) { onPath(path + "Toggle") }
    // Slider
    IconChip(Icons.Filled.Tune) { onPath(path + "Slider") }

    when (path.getOrNull(1)) {
        "Icona" -> {
            DropIcon(Icons.Filled.Search, "Seleziona", listOf("Home","User","Star","Settings")) {
                onLastChanged("Icona: $it"); onDirty()
            }
            DropIcon(Icons.Filled.InvertColors, "Bianco", listOf("Bianco","Primario","Secondario")) {
                onLastChanged("Colore icona: $it"); onDirty()
            }
            DropIcon(Icons.Filled.BlurOn, "Ombra 0dp", listOf("0dp","2dp","4dp","8dp")) {
                onLastChanged("Ombra icona: $it"); onDirty()
            }
        }
        "Toggle" -> {
            DropIcon(Icons.Filled.ToggleOn, "Default", listOf("Default","Switch","Checkbox","Radio")) {
                onLastChanged("Toggle: $it"); onDirty()
            }
            DropIcon(Icons.Filled.InvertColors, "Primario", listOf("Bianco","Primario","Secondario")) {
                onLastChanged("Colore toggle: $it"); onDirty()
            }
            DropIcon(Icons.Filled.BlurOn, "Ombra 2dp", listOf("0dp","2dp","4dp","8dp")) {
                onLastChanged("Ombra toggle: $it"); onDirty()
            }
        }
        "Slider" -> {
            DropIcon(Icons.Filled.Tune, "Default", listOf("Default","Discrete","Continuous")) {
                onLastChanged("Slider: $it"); onDirty()
            }
            DropIcon(Icons.Filled.InvertColors, "Primario", listOf("Bianco","Primario","Secondario")) {
                onLastChanged("Colore slider: $it"); onDirty()
            }
            DropIcon(Icons.Filled.BlurOn, "Ombra 2dp", listOf("0dp","2dp","4dp","8dp")) {
                onLastChanged("Ombra slider: $it"); onDirty()
            }
        }
    }
}

/* ==========================================================
 *  WIDGET COMPATTI (icone bianche, senza testo)
 * ========================================================== */

@Composable
private fun IconChip(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF1F232A),
        contentColor = Color.White,
        shape = RoundedCornerShape(14.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(icon, contentDescription = null)
        }
    }
}

@Composable
private fun ToggleIcon(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean = false,
    onToggle: (() -> Unit)? = null
) {
    val w = if (selected) 3.dp else 1.dp
    Surface(
        color = Color(0xFF1F232A),
        contentColor = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .border(w, Color.White.copy(alpha = 0.9f), RoundedCornerShape(14.dp))
    ) {
        IconButton(
            onClick = { onToggle?.invoke() },
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
        ) {
            Icon(icon, null)
        }
    }
}

@Composable
private fun DropIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }

    Box {
        // Icona + “pill” con valore attuale
        Surface(
            color = Color(0xFF1F232A),
            contentColor = Color.White,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { open = true }, modifier = Modifier.size(44.dp)) {
                    Icon(icon, null)
                }
                // pill compatta (mostra valore attivo anche a menu chiuso)
                Surface(
                    color = Color(0x26FFFFFF),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(value, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { open = false; onSelect(opt) })
            }
        }
    }
}

@Composable
private fun <T> ToggleChoice(
    options: List<String>,
    values: List<T>,
    current: T,
    onPicked: (String, T) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.zip(values).forEach { (label, value) ->
            val sel = value == current
            ToggleIcon(label, if (sel) Icons.Filled.CheckCircle else Icons.Filled.Circle, selected = sel) {
                onPicked(label, value)
            }
        }
    }
}

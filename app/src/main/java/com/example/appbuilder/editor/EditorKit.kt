package com.example.appbuilder.editor

import com.example.appbuilder.icons.EditorIcons
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* =========================================================================================
 *  MODELLO MINIMO DI STATO (solo per navigazione menù)
 * ========================================================================================= */

data class EditorShellState(
    val isEditor: Boolean = true
)

/* =========================================================================================
 *  ENTRY — schermata demo (sfondo neutro + menù)
 * ========================================================================================= */

@Composable
fun EditorDemoScreen() {
    val state = remember { EditorShellState(isEditor = true) }
    EditorMenusOnly(state = state)
}

/* =========================================================================================
 *  ROOT — solo menù (nessuna azione applicata)
 * ========================================================================================= */

@Composable
fun EditorMenusOnly(
    state: EditorShellState
) {
    // Path del menù (es. ["Contenitore", "Bordi", "Spessore"])
    var menuPath by remember { mutableStateOf<List<String>>(emptyList()) }
    // Selezioni effimere dei dropdown/toggle (key = pathTestuale)
    val menuSelections = remember { mutableStateMapOf<String, Any?>() }
    // Modifiche in corso (serve per mostrare la barra di conferma alla risalita)
    var dirty by remember { mutableStateOf(false) }
    // Ultima opzione interessata (per mostrare info extra nel path)
    var lastChanged by remember { mutableStateOf<String?>(null) }

    // Preset salvati (solo demo, in memoria) per le voci "Scegli default"
    val savedPresets = remember { mutableStateMapOf(
        "Layout" to mutableListOf("Nessuno", "Default chiaro", "Default scuro"),
        "Contenitore" to mutableListOf("Nessuno", "Card base", "Hero"),
        "Testo" to mutableListOf("Nessuno", "Titolo", "Sottotitolo", "Body")
    )}

    // Dialog salvataggio stile
    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    // Conferma all’uscita dai sottomenu verso la home
    var showConfirm by remember { mutableStateOf(false) }

    // Gestione “indietro” hardware/gesto
    BackHandler(enabled = menuPath.isNotEmpty() || showConfirm || showSaveDialog) {
        when {
            showSaveDialog -> showSaveDialog = false
            showConfirm -> showConfirm = false
            menuPath.isNotEmpty() -> {
                // se sto tornando alla home e ho modifiche → conferma
                if (menuPath.size == 1 && dirty) showConfirm = true
                else menuPath = menuPath.dropLast(1)
            }
        }
    }

    // Misuro l’altezza della barra azioni per distanziare la barra categorie
    var actionsBarHeightPx by remember { mutableStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F131A), Color(0xFF141922))
                )
            )
    ) {
        if (menuPath.isEmpty()) {
            // HOME: due barre sempre visibili
            MainBottomBar(
                onUndo = { /* stub */ },
                onRedo = { /* stub */ },
                onSaveFile = { /* stub */ },
                onDelete = { /* stub */ },
                onDuplicate = { /* stub */ },
                onProperties = { /* stub */ },
                onLayout = { menuPath = listOf("Layout") },
                onCreate = { /* dropdown nella bar stessa */ },
                onOpenList = { /* stub */ },
                onSaveProject = { /* stub */ },
                onOpenProject = { /* stub */ },
                onNewProject = { /* stub */ },
                onMeasured = { actionsBarHeightPx = it }
            )
            MainMenuBar(
                onLayout = { menuPath = listOf("Layout") },
                onContainer = { menuPath = listOf("Contenitore") },
                onText = { menuPath = listOf("Testo") },
                onAdd = { menuPath = listOf("Aggiungi") },
                bottomBarHeightPx = actionsBarHeightPx
            )
        } else {
            // IN MENU: mostro pannello di livello corrente + breadcrumb
            SubMenuBar(
                path = menuPath,
                selections = menuSelections,
                onBack = {
                    if (menuPath.size == 1 && dirty) showConfirm = true
                    else menuPath = menuPath.dropLast(1)
                },
                onEnter = { label ->
                    // entro in un sotto ramo
                    menuPath = menuPath + label
                    lastChanged = label // evidenzio nel path cosa sto aprendo
                },
                onToggle = { label, value ->
                    menuSelections[key(menuPath, label)] = value
                    lastChanged = "$label: ${if (value) "ON" else "OFF"}"
                    dirty = true
                },
                onPick = { label, value ->
                    menuSelections[key(menuPath, label)] = value
                    lastChanged = "$label: $value"
                    dirty = true
                },
                savedPresets = savedPresets
            )
            BreadcrumbBar(path = menuPath, lastChanged = lastChanged)
        }

        // Barra di conferma quando risalgo con modifiche
        if (showConfirm) {
            ConfirmBar(
                onCancel = {
                    // scarto le modifiche effimere
                    dirty = false
                    lastChanged = null
                    showConfirm = false
                    menuPath = emptyList()
                },
                onOk = {
                    // accetto (in questa demo non applichiamo a un documento reale)
                    dirty = false
                    showConfirm = false
                    menuPath = emptyList()
                },
                onSavePreset = { showSaveDialog = true }
            )
        }

        // Dialog: Salva impostazioni come preset
        if (showSaveDialog) {
            val root = menuPath.firstOrNull() ?: "Contenitore"
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val bucket = savedPresets.getOrPut(root) { mutableListOf() }
                            // se esiste chiedo "sovrascrivere?"
                            if (bucket.any { it.equals(newPresetName, ignoreCase = true) }) {
                                // sovrascrivo: in demo rimuovo e riaggiungo in coda
                                bucket.removeAll { it.equals(newPresetName, ignoreCase = true) }
                            }
                            if (newPresetName.isNotBlank()) bucket.add(newPresetName.trim())
                            newPresetName = ""
                            dirty = false
                            showSaveDialog = false
                            showConfirm = false
                            menuPath = emptyList()
                        }
                    ) { Text("Salva") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) { Text("Annulla") }
                },
                title = { Text("Salva impostazioni come stile") },
                text = {
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text("Nome stile") }
                    )
                }
            )
        }
    }
}

/* =========================================================================================
 *  BARRA PRINCIPALE (icone stile GitHub, scura, sempre visibile in HOME)
 * ========================================================================================= */

@Composable
private fun BoxScope.MainBottomBar(
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSaveFile: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onProperties: () -> Unit,
    onLayout: () -> Unit,
    onCreate: () -> Unit,
    onOpenList: () -> Unit,
    onSaveProject: () -> Unit,
    onOpenProject: () -> Unit,
    onNewProject: () -> Unit,
    onMeasured: (Int) -> Unit
) {
    var showCreateMenu by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF0D1117),
        contentColor = Color.White,
        tonalElevation = 10.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            .navigationBarsPadding()
            .imePadding()
            .onGloballyPositioned { onMeasured(it.size.height) }
    ) {
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(scroll),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolbarIconButton(Icons.Filled.Undo, "Undo", onClick = onUndo)
                ToolbarIconButton(Icons.Filled.Redo, "Redo", onClick = onRedo)

                dividerDot()

                ToolbarIconButton(EditorIcons.Save, "Salva pagina", onClick = onSaveFile)
                ToolbarIconButton(EditorIcons.Delete, "Cestino", onClick = onDelete)
                ToolbarIconButton(EditorIcons.Duplicate, "Duplica", onClick = onDuplicate)

                dividerDot()

                ToolbarIconButton(EditorIcons.Settings, "Proprietà", onClick = onProperties)
                ToolbarIconButton(EditorIcons.Layout, "Layout pagina", onClick = onLayout)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Crea
                Box {
                    ToolbarIconButton(EditorIcons.Insert, "Crea", onClick = { showCreateMenu = true })
                    DropdownMenu(expanded = showCreateMenu, onDismissRequest = { showCreateMenu = false }) {
                        DropdownMenuItem(text = { Text("Nuova pagina") }, onClick = { showCreateMenu = false })
                        DropdownMenuItem(text = { Text("Nuovo avviso") }, onClick = { showCreateMenu = false })
                        DropdownMenuItem(text = { Text("Menù laterale") }, onClick = { showCreateMenu = false })
                        DropdownMenuItem(text = { Text("Menù centrale") }, onClick = { showCreateMenu = false })
                    }
                }
                // Lista
                Box {
                    ToolbarIconButton(Icons.Filled.List, "Lista", onClick = { showListMenu = true })
                    DropdownMenu(expanded = showListMenu, onDismissRequest = { showListMenu = false }) {
                        DropdownMenuItem(text = { Text("Pagine…") }, onClick = { showListMenu = false })
                        DropdownMenuItem(text = { Text("Avvisi…") }, onClick = { showListMenu = false })
                        DropdownMenuItem(text = { Text("Menu laterali…") }, onClick = { showListMenu = false })
                        DropdownMenuItem(text = { Text("Menu centrali…") }, onClick = { showListMenu = false })
                    }
                }

                dividerDot()

                ToolbarIconButton(Icons.Filled.Save, "Salva progetto", onClick = onSaveProject)
                ToolbarIconButton(Icons.Filled.FolderOpen, "Apri", onClick = onOpenProject)
                ToolbarIconButton(Icons.Filled.CreateNewFolder, "Nuovo progetto", onClick = onNewProject)
            }
        }
    }
}

/* Barretta categorie (sopra la barra principale), icone-only */
@Composable
private fun BoxScope.MainMenuBar(
    onLayout: () -> Unit,
    onContainer: () -> Unit,
    onText: () -> Unit,
    onAdd: () -> Unit,
    bottomBarHeightPx: Int
) {
    val gap = 8.dp
    val dy = with(LocalDensity.current) { bottomBarHeightPx.toDp() + gap }

    Surface(
        color = Color(0xFF111621),
        contentColor = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .offset { IntOffset(0, -dy.roundToPx()) }
    ) {
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIconButton(EditorIcons.Text, "Testo", onClick = onText)
            ToolbarIconButton(EditorIcons.Container, "Contenitore", onClick = onContainer)
            ToolbarIconButton(EditorIcons.Layout, "Layout", onClick = onLayout)
            ToolbarIconButton(EditorIcons.Insert, "Aggiungi", onClick = onAdd)
        }
    }
}

/* =========================================================================================
 *  SUBMENU — barra icone livello corrente (menù “ad albero”)
 *  SOLO UI: nessuna modifica applicata al documento.
 * ========================================================================================= */

@Composable
private fun BoxScope.SubMenuBar(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onBack: () -> Unit,
    onEnter: (String) -> Unit,
    onToggle: (label: String, value: Boolean) -> Unit,
    onPick: (label: String, value: String) -> Unit,
    savedPresets: Map<String, MutableList<String>>
) {
    Surface(
        color = Color(0xFF0F141E),
        contentColor = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .offset { IntOffset(0, -64) }
    ) {
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // back icon
            ToolbarIconButton(Icons.Filled.ArrowBack, "Indietro", onClick = onBack)

            when (path.firstOrNull()) {
                "Layout" -> LayoutLevel(path, selections, onEnter, onToggle, onPick, savedPresets)
                "Contenitore" -> ContainerLevel(path, selections, onEnter, onToggle, onPick, savedPresets)
                "Testo" -> TextLevel(path, selections, onToggle, onPick, savedPresets)
                "Aggiungi" -> AddLevel(path, selections, onEnter)
            }
        }
    }
}

/* =========================================================================================
 *  BREADCRUMB — path corrente + ultima opzione
 * ========================================================================================= */
@Composable
private fun BoxScope.BreadcrumbBar(path: List<String>, lastChanged: String?) {
    Surface(
        color = Color(0xFF0B0F16),
        contentColor = Color(0xFF9BA3AF),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .imePadding()
    ) {
        val pretty = buildString {
            append(if (path.isEmpty()) "—" else path.joinToString("  →  "))
            lastChanged?.let { append("   •   "); append(it) }
        }
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(pretty, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/* =========================================================================================
 *  LIVELLI (Layout / Contenitore / Testo / Aggiungi)
 *  Icone-only + dropdown con badge valore corrente
 * ========================================================================================= */

/* ---------- LAYOUT ---------- */
@Composable
private fun LayoutLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onEnter: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onPick: (String, String) -> Unit,
    saved: Map<String, MutableList<String>>
) {
    fun get(keyLeaf: String) = selections[key(path, keyLeaf)] as? String

    when (path.getOrNull(1)) {
        null -> {
            ToolbarIconButton(EditorIcons.Color, "Colore") { onEnter("Colore") }
            ToolbarIconButton(EditorIcons.Image, "Immagini") { onEnter("Immagini") }
            IconDropdown(
                icon = Icons.Filled.BookmarkAdd,
                contentDescription = "Scegli default",
                current = get("default") ?: saved["Layout"]?.firstOrNull(),
                options = saved["Layout"].orEmpty(),
                onSelected = { onPick("default", it) }
            )
        }
        "Colore" -> {
            IconDropdown(EditorIcons.Colors, "Colore 1",
                current = get("col1") ?: "Bianco",
                options = listOf("Bianco", "Grigio", "Nero", "Ciano"),
                onSelected = { onPick("col1", it) }
            )
            IconDropdown(EditorIcons.Colors, "Colore 2",
                current = get("col2") ?: "Grigio chiaro",
                options = listOf("Grigio chiaro", "Blu", "Verde", "Arancio"),
                onSelected = { onPick("col2", it) }
            )
            IconDropdown(EditorIcons.Gradient, "Gradiente",
                current = get("grad") ?: "Orizzontale",
                options = listOf("Orizzontale", "Verticale"),
                onSelected = { onPick("grad", it) }
            )
            IconDropdown(EditorIcons.Functions, "Effetti",
                current = get("fx") ?: "Vignettatura",
                options = listOf("Vignettatura", "Noise", "Strisce"),
                onSelected = { onPick("fx", it) }
            )
        }
        "Immagini" -> {
            ToolbarIconButton(EditorIcons.AddPhotoAlternate, "Aggiungi immagine") { onEnter("Aggiungi foto") }
            ToolbarIconButton(EditorIcons.PermMedia, "Aggiungi album") { onEnter("Aggiungi album") }
        }
        "Aggiungi foto" -> {
            IconDropdown(EditorIcons.Crop, "Crop",
                current = get("crop") ?: "Nessuno",
                options = listOf("Nessuno", "4:3", "16:9", "Quadrato"),
                onSelected = { onPick("crop", it) }
            )
            IconDropdown(EditorIcons.Layout, "Cornice",
                current = get("frame") ?: "Sottile",
                options = listOf("Nessuna", "Sottile", "Marcata"),
                onSelected = { onPick("frame", it) }
            )
            IconDropdown(EditorIcons.Layout, "Filtri",
                current = get("filtro") ?: "Nessuno",
                options = listOf("Nessuno", "B/N", "Vintage", "Vivido"),
                onSelected = { onPick("filtro", it) }
            )
            IconDropdown(EditorIcons.Layout, "Adattamento",
                current = get("fit") ?: "Cover",
                options = listOf("Cover", "Contain", "Fill"),
                onSelected = { onPick("fit", it) }
            )
        }
        "Aggiungi album" -> {
            IconDropdown(EditorIcons.Crop, "Crop",
                current = get("cropAlbum") ?: "Nessuno",
                options = listOf("Nessuno", "4:3", "16:9", "Quadrato"),
                onSelected = { onPick("cropAlbum", it) }
            )
            IconDropdown(EditorIcons.Layout, "Cornice",
                current = get("frameAlbum") ?: "Sottile",
                options = listOf("Nessuna", "Sottile", "Marcata"),
                onSelected = { onPick("frameAlbum", it) }
            )
            IconDropdown(EditorIcons.Layout, "Filtri album",
                current = get("filtroAlbum") ?: "Nessuno",
                options = listOf("Nessuno", "Tutte foto stesso filtro"),
                onSelected = { onPick("filtroAlbum", it) }
            )
            IconDropdown(EditorIcons.Layout, "Adattamento",
                current = get("fitAlbum") ?: "Cover",
                options = listOf("Cover", "Contain", "Fill"),
                onSelected = { onPick("fitAlbum", it) }
            )
            IconDropdown(EditorIcons.Layout, "Animazione",
                current = get("anim") ?: "Slide",
                options = listOf("Slide", "Fade", "Page flip"),
                onSelected = { onPick("anim", it) }
            )
            IconDropdown(EditorIcons.Layout, "Velocità",
                current = get("speed") ?: "Media",
                options = listOf("Lenta", "Media", "Veloce"),
                onSelected = { onPick("speed", it) }
            )
        }
    }
}

/* ---------- CONTENITORE ---------- */
@Composable
private fun ContainerLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onEnter: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onPick: (String, String) -> Unit,
    saved: Map<String, MutableList<String>>
) {
    fun get(keyLeaf: String) = selections[key(path, keyLeaf)] as? String

    when (path.getOrNull(1)) {
        null -> {
            ToolbarIconButton(EditorIcons.Color, "Colore") { onEnter("Colore") }
            ToolbarIconButton(EditorIcons.Image, "Immagini") { onEnter("Immagini") }
            IconDropdown(EditorIcons.SwipeVertical, "Scrollabilità",
                current = get("scroll") ?: "Assente",
                options = listOf("Assente", "Verticale", "Orizzontale"),
                onSelected = { onPick("scroll", it) }
            )
            IconDropdown(EditorIcons.Square, "Shape",
                current = get("shape") ?: "Rettangolo",
                options = listOf("Rettangolo", "Quadrato", "Cerchio", "Altre"),
                onSelected = { onPick("shape", it) }
            )
            IconDropdown(EditorIcons.Variant, "Variant",
                current = get("variant") ?: "Full",
                options = listOf("Full", "Outlined", "Text", "TopBottom"),
                onSelected = { onPick("variant", it) }
            )
            IconDropdown(EditorIcons.LineWeight, "b_tick",
                current = get("b_thick") ?: "1dp",
                options = listOf("0dp", "1dp", "2dp", "3dp"),
                onSelected = { onPick("b_thick", it) }
            )
            IconDropdown(EditorIcons.SwipeRight, "Tipo",
                current = get("tipo") ?: "Normale",
                options = listOf("Normale", "Sfogliabile", "Tab"),
                onSelected = { onPick("tipo", it) }
            )
            IconDropdown(
                icon = Icons.Filled.BookmarkAdd,
                contentDescription = "Scegli default",
                current = get("default") ?: saved["Contenitore"]?.firstOrNull(),
                options = saved["Contenitore"].orEmpty(),
                onSelected = { onPick("default", it) }
            )
        }
        "Colore" -> {
            IconDropdown(EditorIcons.Colors, "Colore 1",
                current = get("col1") ?: "Bianco",
                options = listOf("Bianco", "Grigio", "Nero", "Ciano"),
                onSelected = { onPick("col1", it) }
            )
            IconDropdown(EditorIcons.Colors, "Colore 2",
                current = get("col2") ?: "Grigio chiaro",
                options = listOf("Grigio chiaro", "Blu", "Verde", "Arancio"),
                onSelected = { onPick("col2", it) }
            )
            IconDropdown(EditorIcons.Gradient, "Gradiente",
                current = get("grad") ?: "Orizzontale",
                options = listOf("Orizzontale", "Verticale"),
                onSelected = { onPick("grad", it) }
            )
            IconDropdown(EditorIcons.Functions, "FX",
                current = get("fx") ?: "Vignettatura",
                options = listOf("Vignettatura", "Noise", "Strisce"),
                onSelected = { onPick("fx", it) }
            )
        }
        "Immagini" -> {
            ToolbarIconButton(EditorIcons.AddPhotoAlternate, "Aggiungi immagine") { onEnter("Aggiungi foto") }
            ToolbarIconButton(EditorIcons.PermMedia, "Aggiungi album") { onEnter("Aggiungi album") }
        }
        "Aggiungi foto" -> {
            IconDropdown(EditorIcons.Crop, "Crop",
                current = get("crop") ?: "Nessuno",
                options = listOf("Nessuno", "4:3", "16:9", "Quadrato"),
                onSelected = { onPick("crop", it) }
            )
            IconDropdown(EditorIcons.Layout, "Cornice",
                current = get("frame") ?: "Sottile",
                options = listOf("Nessuna", "Sottile", "Marcata"),
                onSelected = { onPick("frame", it) }
            )
            IconDropdown(EditorIcons.Layout, "Filtri",
                current = get("filtro") ?: "Nessuno",
                options = listOf("Nessuno", "B/N", "Vintage", "Vivido"),
                onSelected = { onPick("filtro", it) }
            )
            IconDropdown(EditorIcons.Layout, "Adatta",
                current = get("fitCont") ?: "Cover",
                options = listOf("Cover", "Contain", "Fill", "FitWidth", "FitHeight"),
                onSelected = { onPick("fitCont", it) }
            )
        }
        "Aggiungi album" -> {
            IconDropdown(EditorIcons.Crop, "Crop",
                current = get("cropAlbum") ?: "Nessuno",
                options = listOf("Nessuno", "4:3", "16:9", "Quadrato"),
                onSelected = { onPick("cropAlbum", it) }
            )
            IconDropdown(EditorIcons.Layout, "Cornice",
                current = get("frameAlbum") ?: "Sottile",
                options = listOf("Nessuna", "Sottile", "Marcata"),
                onSelected = { onPick("frameAlbum", it) }
            )
            IconDropdown(EditorIcons.Layout, "Filtri album",
                current = get("filtroAlbum") ?: "Nessuno",
                options = listOf("Nessuno", "Tutte foto stesso filtro"),
                onSelected = { onPick("filtroAlbum", it) }
            )
            IconDropdown(EditorIcons.Layout, "Adattamento",
                current = get("fit") ?: "Cover",
                options = listOf("Cover", "Contain", "Fill"),
                onSelected = { onPick("fit", it) }
            )
            IconDropdown(EditorIcons.Layout, "Animazione",
                current = get("anim") ?: "Slide",
                options = listOf("Slide", "Fade", "Page flip"),
                onSelected = { onPick("anim", it) }
            )
            IconDropdown(EditorIcons.Layout, "Velocità",
                current = get("speed") ?: "Media",
                options = listOf("Lenta", "Media", "Veloce"),
                onSelected = { onPick("speed", it) }
            )
        }
    }
}

/* ---------- TESTO ---------- */
@Composable
private fun TextLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onToggle: (String, Boolean) -> Unit,
    onPick: (String, String) -> Unit,
    saved: Map<String, MutableList<String>>
) {
    // toggles (bordo più spesso se selezionati)
    val uKey = key(path, "underline")
    val iKey = key(path, "italic")
    // Sottolinea
    ToggleIcon(
        selected = (selections[uKey] as? Boolean) == true,
        onClick = { onToggle("Sottolinea", !((selections[uKey] as? Boolean) == true)) },
        icon = EditorIcons.Underline
    )
    // Corsivo
    ToggleIcon(
        selected = (selections[iKey] as? Boolean) == true,
        onClick = { onToggle("Corsivo", !((selections[iKey] as? Boolean) == true)) },
        icon = EditorIcons.Italic
    )

    // dropdown (font / weight / size / evidenzia)
    IconDropdown(EditorIcons.Highlight, "Evidenzia",
        current = (selections[key(path, "highlight")] as? String) ?: "Nessuna",
        options = listOf("Nessuna", "Marker", "Oblique", "Scribble"),
        onSelected = { onPick("Evidenzia", it) }
    )
    IconDropdown(EditorIcons.CustomTypography, "Font",
        current = (selections[key(path, "font")] as? String) ?: "System",
        options = listOf("System", "Inter", "Roboto", "SF Pro"),
        onSelected = { onPick("Font", it) }
    )
    IconDropdown(EditorIcons.Bold, "Peso",
        current = (selections[key(path, "weight")] as? String) ?: "Regular",
        options = listOf("Light", "Regular", "Medium", "Bold"),
        onSelected = { onPick("Weight", it) }
    )
    IconDropdown(EditorIcons.Size, "Size",
        current = (selections[key(path, "size")] as? String) ?: "16sp",
        options = listOf("12sp", "14sp", "16sp", "18sp", "22sp"),
        onSelected = { onPick("Size", it) }
    )
    IconDropdown(EditorIcons.Brush, "Colore",
        current = (selections[key(path, "tcolor")] as? String) ?: "Nero",
        options = listOf("Nero", "Bianco", "Blu", "Verde", "Rosso"),
        onSelected = { onPick("Colore", it) }
    )

    // NUOVO: Colore (menù testo) — brush
    IconDropdown(EditorIcons.Brush, "Colore",
        current = (selections[key(path, "textColor")] as? String) ?: "Predefinito",
        options = listOf("Predefinito", "Primario", "Secondario", "Rosso", "Verde", "Blu"),
        onSelected = { onPick("Colore", it) }
    )

    // default
    IconDropdown(
        icon = Icons.Filled.BookmarkAdd,
        contentDescription = "Scegli default",
        current = (selections[key(path, "default")] as? String) ?: saved["Testo"]?.firstOrNull(),
        options = saved["Testo"].orEmpty(),
        onSelected = { onPick("default", it) }
    )
}

/* ---------- AGGIUNGI ---------- */
@Composable
private fun AddLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onEnter: (String) -> Unit
) {
    if (path.getOrNull(1) == null) {
        ToolbarIconButton(EditorIcons.Icon, "Icona") { onEnter("Icona") }
        ToolbarIconButton(Icons.Filled.ToggleOn, "Toggle") { onEnter("Toggle") }
        ToolbarIconButton(Icons.Filled.sliders, "Slider") { onEnter("Slider") }
    } else {
        // placeholder: solo navigazione visiva
        ElevatedCard(
            modifier = Modifier.size(40.dp),
            shape = CircleShape
        ) {}
    }
}

/* =========================================================================================
 *  WIDGET MENU — pulsanti a icona, toggle con bordo spesso, dropdown con badge
 * ========================================================================================= */

@Composable
private fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF1B2334)
    val content = Color.White
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(42.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.5f),
            disabledContentColor = content.copy(alpha = 0.5f)
        )
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun ToggleIcon(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val borderWidth = if (selected) 2.dp else 1.dp
    Surface(
        shape = CircleShape,
        color = Color(0xFF1B2334),
        contentColor = Color.White,
        tonalElevation = if (selected) 6.dp else 0.dp,
        shadowElevation = if (selected) 6.dp else 0.dp,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
    ) {
        Box(
            Modifier
                .background(Color.Transparent)
        ) {
            IconButton(onClick = onClick, modifier = Modifier.matchParentSize()) {
                Icon(icon, contentDescription = null)
            }
            // bordo manuale
            Box(
                Modifier
                    .matchParentSize()
                    .padding(1.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
            )
        }
    }
    // disegno bordo esterno (usiamo ElevatedCard per un bordo pulito)
    ElevatedCard(
        modifier = Modifier
            .offset { IntOffset(-42, -42) } // invisibile: hack rimosso, lasciamo così (no overlay)
            .size(0.dp)
    ) {}
}

@Composable
private fun IconDropdown(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    current: String?,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ToolbarIconButton(icon, contentDescription, onClick = { expanded = true })
        // badge numerico angolare per "Colore 1"/"Colore 2"
        val cornerBadge = when (contentDescription) {
            "Colore 1" -> "1"
            "Colore 2" -> "2"
            else -> null
        }
        if (cornerBadge != null) {
            Surface(
                color = Color(0xFF1B2334),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(6, -6) }
            ) {
                Text(
                    text = cornerBadge,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    fontSize = 10.sp
                )
            }
        }
        // badge valore corrente
        if (!current.isNullOrBlank()) {
            Surface(
                color = Color(0xFF22304B),
                contentColor = Color.White,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, 14) } // poco sotto l'icona
            ) {
                Text(
                    text = current,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

/* =========================================================================================
 *  MINI CONFIRM BAR
 * ========================================================================================= */

@Composable
private fun BoxScope.ConfirmBar(
    onCancel: () -> Unit,
    onOk: () -> Unit,
    onSavePreset: () -> Unit
) {
    Surface(
        color = Color(0xFF0B1220),
        contentColor = Color.White,
        tonalElevation = 10.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Salvare le modifiche?",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium
            )
            // icone-only
            ToolbarIconButton(EditorIcons.Cancel, "Annulla", onClick = onCancel)
            ToolbarIconButton(Icons.Filled.BookmarkAdd, "Salva impostazioni", onClick = onSavePreset)
            ToolbarIconButton(EditorIcons.Ok, "OK", onClick = onOk, selected = true)
        }
    }
}

/* =========================================================================================
 *  UTILITY
 * ========================================================================================= */

private fun key(path: List<String>, leaf: String) = (path + leaf).joinToString(" / ")

@Composable
private fun dividerDot() {
    Box(
        Modifier
            .padding(horizontal = 6.dp)
            .size(6.dp)
            .clip(CircleShape)
            .background(Color(0xFF233049))
    )
}
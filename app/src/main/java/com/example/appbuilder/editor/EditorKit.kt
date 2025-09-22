package com.example.appbuilder.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

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
    // Hook per futura barra “OK/Annulla/Salva stile” (ora non usato)
    var stagedChanges by remember { mutableStateOf(false) }

    // Gestione “back” quando sono dentro al menù
    BackHandler(enabled = menuPath.isNotEmpty()) {
        if (menuPath.isNotEmpty()) {
            menuPath = menuPath.dropLast(1)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F131A),
                        Color(0xFF141922)
                    )
                )
            )
    ) {
        // Stato HOME: barre visibili (prima barra + barra rapida sopra)
        if (menuPath.isEmpty()) {
            MainBottomBar(
                onUndo = { /* stub */ },
                onRedo = { /* stub */ },
                onSaveFile = { /* stub */ },
                onDelete = { /* stub */ },
                onDuplicate = { /* stub */ },
                onProperties = { /* stub */ },
                onLayout = { menuPath = listOf("Layout") },
                onCreate = { /* menu a tendina nella bar stessa */ },
                onOpenList = { /* stub */ },
                onSaveProject = { /* stub */ },
                onOpenProject = { /* stub */ },
                onNewProject = { /* stub */ },
            )

            MainMenuBar( // barra con “Testo / Contenitore / Layout / Aggiungi”
                onLayout = { menuPath = listOf("Layout") },
                onContainer = { menuPath = listOf("Contenitore") },
                onText = { menuPath = listOf("Testo") },
                onAdd = { menuPath = listOf("Aggiungi") }
            )
        } else {
            // Stato IN MENU: barre di home nascoste → mostro:
            // 1) Barra orizzontale con opzioni del livello corrente
            SubMenuBar(
                path = menuPath,
                selections = menuSelections,
                onPathChange = { newPath ->
                    menuPath = newPath
                    // Se volessimo marcare “modificato”, attiviamo questo:
                    // stagedChanges = true
                }
            )
            // 2) Breadcrumb appoggiato sopra la tastiera
            BreadcrumbBar(path = menuPath)
        }

        // Se in futuro vorrai il “commit bar” alla risalita (OK/Annulla/Salva stile),
        // puoi mostrare una Surface simile qui in basso quando stagedChanges == true.
        // (Per ora volutamente non visibile, come richiesto.)
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
) {
    var showCreateMenu by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF0D1117),
        contentColor = Color(0xFFC9D1D9),
        tonalElevation = 10.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            .navigationBarsPadding()
            .imePadding() // si adagia sulla tastiera
    ) {
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(scroll),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            IconButton(onClick = onUndo) { Icon(Icons.Filled.Undo, contentDescription = "Indietro modifica") }
            IconButton(onClick = onRedo) { Icon(Icons.Filled.Redo, contentDescription = "Avanti modifica") }

            dividerDot()

            IconButton(onClick = onSaveFile) { Icon(Icons.Filled.Save, contentDescription = "Salva pagina") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Cestino") }
            IconButton(onClick = onDuplicate) { Icon(Icons.Filled.ViewList, contentDescription = "Duplica") } // (icona indicativa)

            dividerDot()

            IconButton(onClick = onProperties) { Icon(Icons.Filled.Settings, contentDescription = "Proprietà selezione") }
            IconButton(onClick = onLayout) { Icon(Icons.Filled.Article, contentDescription = "Layout pagina") }

            // Crea (menu a tendina)
            Box {
                IconButton(onClick = { showCreateMenu = true }) { Icon(Icons.Filled.Add, contentDescription = "Crea") }
                DropdownMenu(expanded = showCreateMenu, onDismissRequest = { showCreateMenu = false }) {
                    DropdownMenuItem(text = { Text("Nuova pagina") }, onClick = { showCreateMenu = false })
                    DropdownMenuItem(text = { Text("Nuovo avviso") }, onClick = { showCreateMenu = false })
                    DropdownMenuItem(text = { Text("Nuovo menù laterale") }, onClick = { showCreateMenu = false })
                    DropdownMenuItem(text = { Text("Nuovo menù centrale") }, onClick = { showCreateMenu = false })
                }
            }

            // Lista elementi creati (menu a tendina)
            Box {
                IconButton(onClick = { showListMenu = true }) { Icon(Icons.Filled.List, contentDescription = "Lista elementi") }
                DropdownMenu(expanded = showListMenu, onDismissRequest = { showListMenu = false }) {
                    DropdownMenuItem(text = { Text("Pagine…") }, onClick = { showListMenu = false })
                    DropdownMenuItem(text = { Text("Avvisi…") }, onClick = { showListMenu = false })
                    DropdownMenuItem(text = { Text("Menu laterali…") }, onClick = { showListMenu = false })
                    DropdownMenuItem(text = { Text("Menu centrali…") }, onClick = { showListMenu = false })
                }
            }

            dividerDot()

            IconButton(onClick = onSaveProject) { Icon(Icons.Filled.Save, contentDescription = "Salva progetto") }
            IconButton(onClick = onOpenProject) { Icon(Icons.Filled.FolderOpen, contentDescription = "Apri/Importa") }
            IconButton(onClick = onNewProject) { Icon(Icons.Filled.CreateNewFolder, contentDescription = "Nuovo progetto") }
        }
    }
}

/* Barretta con i quattro ingressi principali (sopra la barra principale) */
@Composable
private fun BoxScope.MainMenuBar(
    onLayout: () -> Unit,
    onContainer: () -> Unit,
    onText: () -> Unit,
    onAdd: () -> Unit
) {
    Surface(
        color = Color(0xFF111621),
        contentColor = Color(0xFFC9D1D9),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp)
            .offset { IntOffset(0, -74) } // si sovrappone sopra la barra principale
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
            ElevatedFilterChip(selected = false, onClick = onText,       label = { Text("Testo") },       leadingIcon = { Icon(Icons.Filled.TextFields, null) })
            ElevatedFilterChip(selected = false, onClick = onContainer,  label = { Text("Contenitore") }, leadingIcon = { Icon(Icons.Filled.Widgets, null) })
            ElevatedFilterChip(selected = false, onClick = onLayout,     label = { Text("Layout") },      leadingIcon = { Icon(Icons.Filled.Tune, null) })
            ElevatedFilterChip(selected = false, onClick = onAdd,        label = { Text("Aggiungi") },    leadingIcon = { Icon(Icons.Filled.Add, null) })
        }
    }
}

/* =========================================================================================
 *  SUBMENU — barra orizzontale con le opzioni del livello corrente (menù “ad albero”)
 *  SOLO UI: nessuna modifica applicata al documento.
 * ========================================================================================= */

@Composable
private fun BoxScope.SubMenuBar(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onPathChange: (List<String>) -> Unit
) {
    Surface(
        color = Color(0xFF0F141E),
        contentColor = Color(0xFFC9D1D9),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp)
            .offset { IntOffset(0, -64) } // sopra al breadcrumb
    ) {
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // chip “Indietro”
            AssistChip(
                onClick = { if (path.isNotEmpty()) onPathChange(path.dropLast(1)) },
                label = { Text("Indietro") },
                leadingIcon = { Icon(Icons.Filled.ArrowBack, null) },
                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF172033))
            )

            when (path.firstOrNull()) {
                "Layout" -> LayoutLevel(path, selections, onPathChange)
                "Contenitore" -> ContainerLevel(path, selections, onPathChange)
                "Testo" -> TextLevel(path, selections, onPathChange)
                "Aggiungi" -> AddLevel(path, selections, onPathChange)
                else -> Text("Menù")
            }
        }
    }
}

/* =========================================================================================
 *  BREADCRUMB — riga con il path corrente, appoggiata sopra la tastiera
 * ========================================================================================= */
@Composable
private fun BoxScope.BreadcrumbBar(path: List<String>) {
    Surface(
        color = Color(0xFF0B0F16),
        contentColor = Color(0xFF9BA3AF),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            .imePadding()
    ) {
        val pretty = if (path.isEmpty()) "—" else path.joinToString("  →  ")
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(pretty, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/* =========================================================================================
 *  DEFINIZIONE DEI LIVELLI (Layout / Contenitore / Testo / Aggiungi)
 *  SOLO NAVIGAZIONE + DROPDOWN/TAGGLE DUMMY (nessuna azione applicata)
 * ========================================================================================= */

/* ---------- LAYOUT ---------- */
@Composable
private fun LayoutLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onPath: (List<String>) -> Unit
) {
    when (path.getOrNull(1)) {
        null -> {
            ChipNav("Colore") { onPath(path + "Colore") }
            ChipNav("Immagini") { onPath(path + "Immagini") }
            ChipDropdown("Scegli default", selections, key(path, "default"), listOf("Nessuno", "Default chiaro", "Default scuro"))
        }
        "Colore" -> {
            ChipDropdown("Colore 1", selections, key(path, "col1"), listOf("Blu", "Viola", "Rosso", "Verde"))
            ChipDropdown("Colore 2", selections, key(path, "col2"), listOf("Grigio", "Nero", "Ciano", "Arancio"))
            ChipDropdown("Gradiente", selections, key(path, "grad"), listOf("Orizzontale", "Verticale"))
            ChipDropdown("Effetti", selections, key(path, "fx"), listOf("Vignettatura", "Grana/Noise", "Strisce diagonali"))
        }
        "Immagini" -> {
            ChipNav("Aggiungi foto") { onPath(path + "Aggiungi foto") }
            ChipNav("Aggiungi album") { onPath(path + "Aggiungi album") }
        }
        "Aggiungi foto" -> {
            ChipNav("Crop") {}
            ChipNav("Cornice") {}
            ChipDropdown("Filtri", selections, key(path, "filtro"), listOf("Nessuno", "B/N", "Vintage", "Vivido"))
            ChipDropdown("Adattamento", selections, key(path, "fit"), listOf("Cover", "Contain", "Fill"))
        }
        "Aggiungi album" -> {
            ChipNav("Crop") {}
            ChipNav("Cornice") {}
            ChipDropdown("Filtri album", selections, key(path, "filtroAlbum"), listOf("Nessuno", "Tutte foto stesso filtro"))
            ChipDropdown("Adattamento", selections, key(path, "fit"), listOf("Cover", "Contain", "Fill"))
            ChipDropdown("Animazione sfoglio", selections, key(path, "anim"), listOf("Slide", "Fade", "Page flip"))
            ChipDropdown("Velocità sfoglio", selections, key(path, "speed"), listOf("Lenta", "Media", "Veloce"))
        }
    }
}

/* ---------- CONTENITORE ---------- */
@Composable
private fun ContainerLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onPath: (List<String>) -> Unit
) {
    when (path.getOrNull(1)) {
        null -> {
            ChipNav("Colore") { onPath(path + "Colore") }
            ChipNav("Immagini") { onPath(path + "Immagini") }
            ChipNav("Scrollabilità") { onPath(path + "Scrollabilità") }
            ChipNav("Forma") { onPath(path + "Forma") }
            ChipNav("Angoli") { onPath(path + "Angoli") }
            ChipNav("Stile") { onPath(path + "Stile") }
            ChipNav("Bordi") { onPath(path + "Bordi") }
            ChipNav("Tipo") { onPath(path + "Tipo") }
            ChipNav("Azione al tap") { onPath(path + "Azione al tap") }
            ChipDropdown("Scegli default", selections, key(path, "default"), listOf("Nessuno", "Card base", "Hero"))
        }
        "Colore" -> {
            ChipDropdown("Colore 1", selections, key(path, "col1"), listOf("Bianco", "Grigio", "Nero", "Ciano"))
            ChipDropdown("Colore 2", selections, key(path, "col2"), listOf("Grigio chiaro", "Blu", "Verde", "Arancio"))
            ChipDropdown("Gradiente", selections, key(path, "grad"), listOf("Orizzontale", "Verticale"))
            ChipDropdown("Effetti", selections, key(path, "fx"), listOf("Vignettatura", "Noise", "Strisce"))
        }
        "Immagini" -> {
            ChipNav("Aggiungi foto") { onPath(path + "Aggiungi foto") }
            ChipNav("Aggiungi album") { onPath(path + "Aggiungi album") }
        }
        "Aggiungi foto" -> {
            ChipNav("Crop") {}
            ChipNav("Cornice") {}
            ChipDropdown("Filtri", selections, key(path, "filtro"), listOf("Nessuno", "B/N", "Vintage", "Vivido"))
            ChipDropdown("Adatta al contenitore", selections, key(path, "fitCont"), listOf("Cover", "Contain", "Fill", "FitWidth", "FitHeight"))
        }
        "Aggiungi album" -> {
            ChipNav("Crop") {}
            ChipNav("Cornice") {}
            ChipDropdown("Filtri album", selections, key(path, "filtroAlbum"), listOf("Nessuno", "Tutte foto stesso filtro"))
            ChipDropdown("Adattamento", selections, key(path, "fit"), listOf("Cover", "Contain", "Fill"))
            ChipDropdown("Animazione sfoglio", selections, key(path, "anim"), listOf("Slide", "Fade", "Page flip"))
            ChipDropdown("Velocità sfoglio", selections, key(path, "speed"), listOf("Lenta", "Media", "Veloce"))
        }
        "Scrollabilità" -> {
            ChipDropdown("Direzione", selections, key(path, "scroll"), listOf("Assente", "Verticale", "Orizzontale"))
        }
        "Forma" -> {
            ChipDropdown("Forma", selections, key(path, "shape"), listOf("Rettangolo", "Quadrato", "Cerchio", "Altre"))
        }
        "Angoli" -> {
            ChipDropdown("Top‑sx", selections, key(path, "c_tsx"), listOf("0dp", "8dp", "12dp", "16dp", "24dp"))
            ChipDropdown("Top‑dx", selections, key(path, "c_tdx"), listOf("0dp", "8dp", "12dp", "16dp", "24dp"))
            ChipDropdown("Bottom‑sx", selections, key(path, "c_bsx"), listOf("0dp", "8dp", "12dp", "16dp", "24dp"))
            ChipDropdown("Bottom‑dx", selections, key(path, "c_bdx"), listOf("0dp", "8dp", "12dp", "16dp", "24dp"))
        }
        "Stile" -> {
            ChipDropdown("Variante", selections, key(path, "variant"), listOf("Full", "Outlined", "Text", "TopBottom"))
        }
        "Bordi" -> {
            ChipDropdown("Colore bordo", selections, key(path, "b_color"), listOf("Trasparente", "Chiaro", "Scuro"))
            ChipDropdown("Spessore", selections, key(path, "b_thick"), listOf("0dp", "1dp", "2dp", "3dp"))
            ChipDropdown("Ombreggiatura", selections, key(path, "b_shadow"), listOf("Nessuna", "Leggera", "Media", "Forte"))
        }
        "Tipo" -> {
            ChipNav("Sfogliabile (pagine)") { onPath(path + "Sfogliabile") }
            ChipNav("Tab") { onPath(path + "Tab") }
        }
        "Sfogliabile" -> {
            ChipDropdown("Numero pagine", selections, key(path, "pages"), (2..10).map { "$it" })
            ChipDropdown("Indicatore", selections, key(path, "indicator"), listOf("Pallini", "Segmenti", "Nessuno"))
        }
        "Tab" -> {
            ChipDropdown("Numero pagine", selections, key(path, "tabs"), (2..6).map { "$it" })
            ChipDropdown("Forma Tab", selections, key(path, "tabShape"), listOf("Underline", "Pill", "Block"))
            ChipDropdown("Evidenziazione", selections, key(path, "tabHi"), listOf("Colore", "Spessore", "Glow"))
        }
        "Azione al tap" -> {
            ChipDropdown("Azione", selections, key(path, "tap"), listOf(
                "Evidenzia",
                "Apri pagina",
                "Apri menù",
                "Menù a tendina",
                "Chiamata backend + risultato",
                "Inserimento testo"
            ))
        }
    }
}

/* ---------- TESTO ---------- */
@Composable
private fun TextLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onPath: (List<String>) -> Unit
) {
    when (path.getOrNull(1)) {
        null -> {
            ChipToggle("Sottolinea", selections, key(path, "underline"))
            ChipDropdown("Evidenzia", selections, key(path, "highlight"), listOf("Nessuna", "Marker", "Oblique", "Scribble"))
            ChipDropdown("Font", selections, key(path, "font"), listOf("System", "Inter", "Roboto", "SF Pro"))
            ChipDropdown("Weight", selections, key(path, "weight"), listOf("Light", "Regular", "Medium", "Bold"))
            ChipDropdown("Size", selections, key(path, "size"), listOf("12sp", "14sp", "16sp", "18sp", "22sp"))
            ChipToggle("Corsivo", selections, key(path, "italic"))
            ChipNav("Importa font (.ttf)") { /* stub */ }
            ChipDropdown("Scegli default", selections, key(path, "default"), listOf("Nessuno", "Titolo", "Sottotitolo", "Body"))
        }
    }
}

/* ---------- AGGIUNGI ---------- */
@Composable
private fun AddLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onPath: (List<String>) -> Unit
) {
    when (path.getOrNull(1)) {
        null -> {
            ChipNav("Icona") { onPath(path + "Icona") }
            ChipNav("Toggle") { onPath(path + "Toggle") }
            ChipNav("Slider") { onPath(path + "Slider") }
        }
        "Icona" -> {
            ChipDropdown("Seleziona icona", selections, key(path, "icon"), listOf("Star", "Heart", "Home", "Settings"))
            ChipNav("Cerca icona") { /* stub */ }
            ChipDropdown("Colore", selections, key(path, "color"), listOf("Primario", "Secondario", "Grigio", "Rosso"))
            ChipDropdown("Ombra", selections, key(path, "shadow"), listOf("Nessuna", "Leggera", "Media"))
        }
        "Toggle" -> {
            ChipDropdown("Seleziona toggle", selections, key(path, "toggleKind"), listOf("Switch", "Checkbox"))
            ChipNav("Cerca toggle") { /* stub */ }
            ChipDropdown("Colore", selections, key(path, "color"), listOf("Primario", "Secondario", "Grigio"))
            ChipDropdown("Ombra", selections, key(path, "shadow"), listOf("Nessuna", "Leggera", "Media"))
        }
        "Slider" -> {
            ChipDropdown("Seleziona slider", selections, key(path, "sliderKind"), listOf("Standard", "Range"))
            ChipNav("Cerca slider") { /* stub */ }
            ChipDropdown("Colore", selections, key(path, "color"), listOf("Primario", "Secondario", "Grigio"))
            ChipDropdown("Ombra", selections, key(path, "shadow"), listOf("Nessuna", "Leggera", "Media"))
        }
    }
}

/* =========================================================================================
 *  WIDGET MENU (chip di navigazione / dropdown / toggle) — solo estetica
 * ========================================================================================= */

@Composable
private fun ChipNav(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.height(38.dp)) {
        Text(text)
    }
}

@Composable
private fun ChipDropdown(
    label: String,
    selections: MutableMap<String, Any?>,
    key: String,
    options: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    val current = selections[key] as? String

    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(if (current.isNullOrEmpty()) label else "$label: $current") },
            leadingIcon = { Icon(Icons.Filled.MoreVert, null) },
            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1A2233)),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        selections[key] = opt
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ChipToggle(
    label: String,
    selections: MutableMap<String, Any?>,
    key: String
) {
    val selected = (selections[key] as? Boolean) == true
    ElevatedFilterChip(
        selected = selected,
        onClick = { selections[key] = !selected },
        label = { Text(label) }
    )
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

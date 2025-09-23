package com.example.appbuilder.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ============================================================================
 *  MENUS-ONLY: niente canvas/oggetti. Solo estetica e navigazione dei menù.
 *  - Due barre scure sempre visibili (icone bianche) quando NON sei in un sotto-menù
 *  - Se entri in Layout/Contenitore/Testo/Aggiungi: le barre spariscono,
 *    compaiono pannellini di opzioni + breadcrumb in basso;
 *    se hai toccato qualche opzione → “uscendo” vedi la barra Annulla/OK/Salva.
 * ============================================================================ */

@Composable
fun EditorScreen() {
    MenusOnlyEditor()
}

private enum class TopMenu { Layout, Contenitore, Testo, Aggiungi }

/** Stato locale “solo menù” */
private data class MenusState(
    val activeTop: TopMenu? = null,            // quale menù principale è aperto
    val path: List<String> = emptyList(),      // es. ["Contenitore", "Bordi", "Spessore"]
    val lastEdited: String? = null,            // stringa sintetica dell’ultima opzione toccata
    val dirty: Boolean = false,                // true se ci sono modifiche non confermate
    val showExitConfirm: Boolean = false,      // mostra la barra (o dialog) di conferma uscita
    // esempi di valori selezionati (solo per UI) — niente azioni
    val pageColor1: String = "Indigo",
    val pageColor2: String = "Gray",
    val pageGradient: String = "Orizzontale",
    val textUnderline: Boolean = false,
    val textItalic: Boolean = false,
    val textFont: String = "Default",
    val textWeight: String = "Medium",
    val textSize: String = "16sp"
)

/** Colori delle barre scure tipo GitHub */
private val BarBg = Color(0xFF0D1117)
private val BarBorder = Color(0x1FFFFFFF)
private val IconTint = Color.White

@Composable
private fun MenusOnlyEditor() {
    var s by remember { mutableStateOf(MenusState()) }
    val onBack: () -> Unit = {
        // se sei dentro a un sotto-menù e ci sono modifiche non confermate
        if (s.activeTop != null && s.dirty && !s.showExitConfirm) {
            s = s.copy(showExitConfirm = true)
        } else if (s.activeTop != null) {
            // esci dal menù
            s = s.copy(activeTop = null, path = emptyList(), lastEdited = null, dirty = false, showExitConfirm = false)
        }
    }
    BackHandler(enabled = s.activeTop != null) { onBack() }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        // Se non sei in un sotto-menù → mostra le due barre in basso
        if (s.activeTop == null) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Barra superiore (categorie: Layout/Contenitore/Testo/Aggiungi)
                TopCategoryBar(
                    onLayout = { s = s.copy(activeTop = TopMenu.Layout, path = listOf("Layout")) },
                    onContainer = { s = s.copy(activeTop = TopMenu.Contenitore, path = listOf("Contenitore")) },
                    onText = { s = s.copy(activeTop = TopMenu.Testo, path = listOf("Testo")) },
                    onAdd = { s = s.copy(activeTop = TopMenu.Aggiungi, path = listOf("Aggiungi")) }
                )
                // Barra inferiore (comandi generali – icone bianche, scrollabile)
                BottomActionBar()
            }
        } else {
            // Sei dentro un menù: pannello opzioni + path bar + eventuale conferma uscita
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pannellino opzioni (orizzontale, chips/dropdown/icons)
                Surface(
                    color = BarBg, tonalElevation = 8.dp, shadowElevation = 8.dp,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.padding(10.dp)) {
                        when (s.activeTop) {
                            TopMenu.Layout -> LayoutPanel(s) { s = it }
                            TopMenu.Contenitore -> ContainerPanel(s) { s = it }
                            TopMenu.Testo -> TextPanel(s) { s = it }
                            TopMenu.Aggiungi -> AddPanel(s) { s = it }
                            null -> {}
                        }
                    }
                }

                // Breadcrumb/path + ultima opzione toccata
                PathBar(
                    path = s.path,
                    last = s.lastEdited,
                    onBack = {
                        // se ho modifiche, prima la conferma
                        if (s.dirty) s = s.copy(showExitConfirm = true)
                        else s = s.copy(activeTop = null, path = emptyList(), lastEdited = null)
                    }
                )

                // Barra conferma uscita (se modifiche fatte)
                if (s.showExitConfirm) {
                    ConfirmBar(
                        onCancel = { // scarta modifiche e chiudi
                            s = s.copy(activeTop = null, path = emptyList(), lastEdited = null, dirty = false, showExitConfirm = false)
                        },
                        onOk = { // tieni modifiche e chiudi
                            s = s.copy(activeTop = null, path = emptyList(), showExitConfirm = false)
                        },
                        onSaveStyle = { // finta “salva come stile”
                            s = s.copy(showExitConfirm = false)
                            // Dialog “salva stile con nome”
                            StyleSaveDialog(
                                onDismiss = { s = s.copy(showExitConfirm = false) },
                                onSave = { _ -> s = s.copy(activeTop = null, path = emptyList(), showExitConfirm = false) }
                            )
                        }
                    )
                }
            }
        }
    }
}

/* ----------------------------------------------------------
 *   BARRE INFERIORI (icone bianche, stile scuro GitHub-like)
 * ---------------------------------------------------------- */

@Composable
private fun TopCategoryBar(
    onLayout: () -> Unit,
    onContainer: () -> Unit,
    onText: () -> Unit,
    onAdd: () -> Unit
) {
    Surface(
        color = BarBg, tonalElevation = 8.dp, shadowElevation = 8.dp,
        shape = RoundedCornerShape(16.dp), modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        val scroll = rememberScrollState()
        Row(
            Modifier
                .horizontalScroll(scroll)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActionIcon(Icons.Filled.Tune, "Layout", onClick = onLayout)
            ActionIcon(Icons.Filled.Settings, "Contenitore", onClick = onContainer)
            ActionIcon(Icons.Filled.TextFields, "Testo", onClick = onText)
            ActionIcon(Icons.Filled.Add, "Aggiungi", onClick = onAdd)
        }
    }
}

@Composable
private fun BottomActionBar() {
    Surface(
        color = BarBg, tonalElevation = 8.dp, shadowElevation = 8.dp,
        shape = RoundedCornerShape(16.dp), modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        val scroll = rememberScrollState()
        var showCreate by remember { mutableStateOf(false) }
        var showList by remember { mutableStateOf(false) }

        Row(
            Modifier
                .horizontalScroll(scroll)
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .heightIn(min = 56.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                ActionIcon(Icons.Filled.ArrowBack, "Indietro") { }
                ActionIcon(Icons.Filled.ArrowForward, "Avanti") { }
                ActionIcon(Icons.Filled.Save, "Salva pagina") { }
                ActionIcon(Icons.Filled.Delete, "Elimina") { }
                ActionIcon(Icons.Filled.ContentCopy, "Duplica") { }
                ActionIcon(Icons.Filled.Settings, "Proprietà") { }
                ActionIcon(Icons.Filled.Tune, "Layout pagina") { }
                Box {
                    ActionIcon(Icons.Filled.Add, "Crea") { showCreate = !showCreate }
                    DropdownMenu(expanded = showCreate, onDismissRequest = { showCreate = false }) {
                        DropdownMenuItem(text = { Text("Nuova pagina") }, onClick = { showCreate = false })
                        DropdownMenuItem(text = { Text("Nuovo avviso") }, onClick = { showCreate = false })
                        DropdownMenuItem(text = { Text("Nuovo menù laterale") }, onClick = { showCreate = false })
                        DropdownMenuItem(text = { Text("Nuovo menù centrale") }, onClick = { showCreate = false })
                    }
                }
                Box {
                    ActionIcon(Icons.Filled.List, "Elenco") { showList = !showList }
                    DropdownMenu(expanded = showList, onDismissRequest = { showList = false }) {
                        DropdownMenuItem(text = { Text("Pagine create") }, onClick = { showList = false })
                        DropdownMenuItem(text = { Text("Avvisi creati") }, onClick = { showList = false })
                        DropdownMenuItem(text = { Text("Menù creati") }, onClick = { showList = false })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                ActionIcon(Icons.Filled.Check, "OK") { /* conferma proprietà correnti (futuro) */ }
                ActionIcon(Icons.Filled.Cloud, "Salva progetto") { }
                ActionIcon(Icons.Filled.FolderOpen, "Apri/Importa") { }
                ActionIcon(Icons.Filled.CreateNewFolder, "Nuovo progetto") { }
            }
        }
    }
}

@Composable
private fun ActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(icon, contentDescription = desc, tint = IconTint)
    }
}

/* ---------------------------
 *  PATH + CONFERMA USCITA
 * --------------------------- */

@Composable
private fun PathBar(path: List<String>, last: String?, onBack: () -> Unit) {
    Surface(
        color = BarBg, tonalElevation = 8.dp, shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionIcon(Icons.Filled.ArrowBack, "Indietro", onClick)
            val text = buildString {
                append(path.joinToString("  →  "))
                if (!last.isNullOrBlank()) { append("  —  "); append(last) }
            }
            Text(text, color = Color.White, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun ConfirmBar(
    onCancel: () -> Unit,
    onOk: () -> Unit,
    onSaveStyle: () -> Unit
) {
    Surface(
        color = BarBg, tonalElevation = 10.dp, shadowElevation = 10.dp,
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel) { Text("Annulla", color = Color.White) }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onSaveStyle) { Text("Salva impostazioni") }
            androidx.compose.material3.Button(onClick = onOk) { Text("OK") }
        }
    }
}

@Composable
private fun StyleSaveDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("Stile 1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text("Salva") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        title = { Text("Salva come stile") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = name, onValueChange = { name = it },
                singleLine = true, label = { Text("Nome") }
            )
        }
    )
}

/* ----------------------------------------------------------
 *  PANNELLI OPZIONI (UI-only, nessuna azione collegata)
 * ---------------------------------------------------------- */

@Composable
private fun LayoutPanel(s: MenusState, set: (MenusState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 1) Colore
        SectionTitle("Colore")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colore 1
            DropdownSelector(
                icon = Icons.Filled.ColorLens,
                selected = s.pageColor1,
                items = listOf("Indigo", "Teal", "Purple", "Red", "Emerald"),
                onSelected = {
                    set(s.copy(pageColor1 = it, dirty = true, lastEdited = "Colore 1: $it"))
                }
            )
            // Colore 2
            DropdownSelector(
                icon = Icons.Filled.ColorLens,
                selected = s.pageColor2,
                items = listOf("Gray", "Slate", "Stone", "Zinc", "Black"),
                onSelected = {
                    set(s.copy(pageColor2 = it, dirty = true, lastEdited = "Colore 2: $it"))
                }
            )
            // Gradiente
            DropdownSelector(
                icon = Icons.Filled.Tune,
                selected = s.pageGradient,
                items = listOf("Orizzontale", "Verticale"),
                onSelected = {
                    set(s.copy(pageGradient = it, dirty = true, lastEdited = "Gradiente: $it"))
                }
            )
            // Effetti combinazione 2 colori (toggle)
            ChipToggle(label = "Strisce", selected = false) {
                set(s.copy(dirty = true, lastEdited = "Effetto: Strisce"))
            }
            ChipToggle(label = "Noise", selected = false) {
                set(s.copy(dirty = true, lastEdited = "Effetto: Noise"))
            }
        }

        // 2) Immagini (UI placeholder)
        SectionTitle("Immagini")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElevatedFilterChip(selected = false, onClick = { set(s.copy(dirty = true, lastEdited = "Aggiungi foto")) }, label = { Text("Aggiungi foto", color = Color.White) }, leadingIcon = { Icon(Icons.Filled.Image, null, tint = IconTint) })
            ElevatedFilterChip(selected = false, onClick = { set(s.copy(dirty = true, lastEdited = "Aggiungi album")) }, label = { Text("Aggiungi album", color = Color.White) }, leadingIcon = { Icon(Icons.Filled.Collections, null, tint = IconTint) })
            DropdownSelector(icon = Icons.Filled.Tune, selected = "Adatta alla pagina", items = listOf("Adatta alla pagina","Riempi","Contieni","Centra")) {
                set(s.copy(dirty = true, lastEdited = "Adattamento: $it"))
            }
            DropdownSelector(icon = Icons.Filled.Tune, selected = "Filtro: nessuno", items = listOf("Filtro: nessuno","BN","Seppia","Vivido")) {
                set(s.copy(dirty = true, lastEdited = it))
            }
        }

        // 3) Scegli default
        SectionTitle("Scegli default")
        DropdownSelector(icon = Icons.Filled.Tune, selected = "Default Light", items = listOf("Default Light", "Default Dark", "Indigo", "Emerald")) {
            set(s.copy(dirty = true, lastEdited = "Default: $it"))
        }
    }
}

@Composable
private fun ContainerPanel(s: MenusState, set: (MenusState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Colore contenitore")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DropdownSelector(Icons.Filled.ColorLens, "Bianco", listOf("Bianco","Nero","Indigo","Teal")) {
                set(s.copy(dirty = true, lastEdited = "Contenitore: Colore $it"))
            }
            DropdownSelector(Icons.Filled.Tune, "Gradiente: Orizzontale", listOf("Gradiente: Orizzontale","Gradiente: Verticale")) {
                set(s.copy(dirty = true, lastEdited = it))
            }
            ChipToggle("Strisce", false) { set(s.copy(dirty = true, lastEdited = "Effetto: Strisce")) }
            ChipToggle("Noise", false) { set(s.copy(dirty = true, lastEdited = "Effetto: Noise")) }
        }

        SectionTitle("Immagini contenitore")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElevatedFilterChip(false, { set(s.copy(dirty = true, lastEdited = "Aggiungi foto")) }, { Text("Aggiungi foto", color = Color.White) }, leadingIcon = { Icon(Icons.Filled.Image, null, tint = IconTint) })
            ElevatedFilterChip(false, { set(s.copy(dirty = true, lastEdited = "Aggiungi album")) }, { Text("Aggiungi album", color = Color.White) }, leadingIcon = { Icon(Icons.Filled.Collections, null, tint = IconTint) })
            DropdownSelector(Icons.Filled.Tune, "Adatta al contenitore", listOf("Adatta al contenitore","Riempi","Contieni","Centra")) {
                set(s.copy(dirty = true, lastEdited = "Adattamento: $it"))
            }
        }

        SectionTitle("Scrollabilità / Forma / Stile / Bordi / Tipo / Azione")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DropdownSelector(Icons.Filled.Tune, "Scroll: assente", listOf("Scroll: assente","Scroll: verticale","Scroll: orizzontale")) { set(s.copy(dirty = true, lastEdited = it)) }
            DropdownSelector(Icons.Filled.Tune, "Forma: rettangolo", listOf("Forma: cerchio","Forma: quadrato","Forma: rettangolo","Forma: altra")) { set(s.copy(dirty = true, lastEdited = it)) }
            DropdownSelector(Icons.Filled.Tune, "Angoli: 8dp", listOf("Angoli: 0dp","Angoli: 8dp","Angoli: 16dp","Angoli: 24dp")) { set(s.copy(dirty = true, lastEdited = it)) }
            DropdownSelector(Icons.Filled.Tune, "Stile: full", listOf("Stile: text","Stile: outlined","Stile: full","Stile: topbottom")) { set(s.copy(dirty = true, lastEdited = it)) }
            DropdownSelector(Icons.Filled.Tune, "Bordo: leggero", listOf("Bordo: nessuno","Bordo: leggero","Bordo: spesso + ombra")) { set(s.copy(dirty = true, lastEdited = it)) }
            DropdownSelector(Icons.Filled.Tune, "Tipo: normale", listOf("Tipo: normale","Tipo: sfogliabile","Tipo: tabs")) { set(s.copy(dirty = true, lastEdited = it)) }
            DropdownSelector(Icons.Filled.Tune, "Tap: evidenzia", listOf("Tap: evidenzia","Tap: apri pagina","Tap: apri menù","Tap: tendina","Tap: HTTP+mostra","Tap: input testo")) { set(s.copy(dirty = true, lastEdited = it)) }
            DropdownSelector(Icons.Filled.Tune, "Default: nessuno", listOf("Default: nessuno","Default: Stile A","Default: Stile B")) { set(s.copy(dirty = true, lastEdited = it)) }
        }
    }
}

@Composable
private fun TextPanel(s: MenusState, set: (MenusState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Formattazione")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Toggle con bordo più spesso se selezionati
            ChipToggle("Sottolinea", s.textUnderline) {
                set(s.copy(textUnderline = !s.textUnderline, dirty = true, lastEdited = if (!s.textUnderline) "Sottolinea: on" else "Sottolinea: off"))
            }
            ChipToggle("Corsivo", s.textItalic) {
                set(s.copy(textItalic = !s.textItalic, dirty = true, lastEdited = if (!s.textItalic) "Corsivo: on" else "Corsivo: off"))
            }
            DropdownSelector(Icons.Filled.TextFields, s.textFont, listOf("Default","Roboto","Inter","Lato","Merriweather")) {
                set(s.copy(textFont = it, dirty = true, lastEdited = "Font: $it"))
            }
            DropdownSelector(Icons.Filled.TextFields, s.textWeight, listOf("Light","Regular","Medium","Bold","Black")) {
                set(s.copy(textWeight = it, dirty = true, lastEdited = "Weight: $it"))
            }
            DropdownSelector(Icons.Filled.TextFields, s.textSize, listOf("12sp","14sp","16sp","18sp","20sp","24sp")) {
                set(s.copy(textSize = it, dirty = true, lastEdited = "Size: $it"))
            }
            ElevatedFilterChip(false, { set(s.copy(dirty = true, lastEdited = "Importa font (.ttf)")) }, { Text("Importa .ttf", color = Color.White) })
            DropdownSelector(Icons.Filled.Tune, "Evidenzia: nessuna", listOf("Evidenzia: nessuna","Marker giallo","Obliqua","Sporca")) {
                set(s.copy(dirty = true, lastEdited = it))
            }
        }

        SectionTitle("Scegli default")
        DropdownSelector(Icons.Filled.Tune, "Testo default: Nessuno", listOf("Testo default: Nessuno","Titolo A","Paragrafo B","Callout C")) {
            set(s.copy(dirty = true, lastEdited = it))
        }
    }
}

@Composable
private fun AddPanel(s: MenusState, set: (MenusState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Aggiungi elementi")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElevatedFilterChip(false, { set(s.copy(dirty = true, lastEdited = "Aggiungi icona")) }, { Text("Icona", color = Color.White) }, leadingIcon = { Icon(Icons.Filled.Image, null, tint = IconTint) })
            ElevatedFilterChip(false, { set(s.copy(dirty = true, lastEdited = "Aggiungi toggle")) }, { Text("Toggle", color = Color.White) })
            ElevatedFilterChip(false, { set(s.copy(dirty = true, lastEdited = "Aggiungi slider")) }, { Text("Slider", color = Color.White) })
        }
    }
}

/* ----------------------------------------------------------
 *  WIDGETS RIUTILIZZABILI
 * ---------------------------------------------------------- */

@Composable
private fun SectionTitle(t: String) {
    Text(t, color = Color.White, fontSize = 13.sp, modifier = Modifier
        .fillMaxWidth()
        .padding(top = 2.dp, bottom = 2.dp))
}

@Composable
private fun ChipToggle(label: String, selected: Boolean, onToggle: () -> Unit) {
    val border = if (selected) 2.dp else 1.dp
    Surface(
        color = Color.Transparent, shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .height(36.dp)
            .border(border, Color.White.copy(alpha = 0.9f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .padding(horizontal = 2.dp)
                .wrapContentWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White)
        }
    }
    Spacer(Modifier.width(4.dp))
    // Tap “fake”: usiamo un piccolo IconButton trasparente sovrapposto
    IconButton(onClick = onToggle, modifier = Modifier.size(0.dp)) { }
}

@Composable
private fun DropdownSelector(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: String,
    items: List<String>,
    onSelected: (String) -> Unit
) {
    var expand by remember { mutableStateOf(false) }
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .heightIn(min = 36.dp)
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = IconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(selected, color = Color.White)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { expand = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = IconTint)
            }
            DropdownMenu(expanded = expand, onDismissRequest = { expand = false }) {
                items.forEach { v ->
                    DropdownMenuItem(
                        text = { Text(v) },
                        onClick = { expand = false; onSelected(v) }
                    )
                }
            }
        }
    }
}

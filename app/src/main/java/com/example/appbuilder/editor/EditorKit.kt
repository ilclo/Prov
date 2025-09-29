package com.example.appbuilder.editor

import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.appbuilder.icons.EditorIcons
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Widgets
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.appbuilder.R
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.input.KeyboardCapitalization
/* ---- BARS: altezze fisse + gap ---- */
private val BOTTOM_BAR_HEIGHT = 56.dp        // barra inferiore (base)
private val BOTTOM_BAR_EXTRA = 8.dp          // extra altezza barra inferiore (stessa in Home e Submenu)
private val TOP_BAR_HEIGHT = 52.dp           // barra superiore (categorie / submenu)
private val BARS_GAP = 14.dp                 // distacco tra le due barre (+2dp di “aria”)
private val SAFE_BOTTOM_MARGIN = 32.dp     // barra inferiore più alta rispetto al bordo schermo
private val LocalExitClassic = staticCompositionLocalOf<() -> Unit> { {} }

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

/* ---------- AGGIUNGI ---------- */
@Composable
private fun AddLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onEnter: (String) -> Unit
) {
    if (path.getOrNull(1) == null) {
        ToolbarIconButton(EditorIcons.Icon, "Icona") { onEnter("Icona") }
        ToolbarIconButton(Icons.Outlined.ToggleOn, "Toggle") { onEnter("Toggle") }
        ToolbarIconButton(Icons.Outlined.LinearScale, "Slider") { onEnter("Slider") }

        // NEW: divider vertical & horizontal (Outlined-only, con fallback XML)
        ToolbarIconButton(
            icon = ImageVector.vectorResource(id = R.drawable.ic_align_flex_end),
            contentDescription = "Divisore verticale",
            onClick = { onEnter("Divisore verticale") }
        )
        ToolbarIconButton(
            icon = ImageVector.vectorResource(id = R.drawable.ic_horizontal_rule),
            contentDescription = "Divisore orizzontale",
            onClick = { onEnter("Divisore orizzontale") }
        )
    } else {
        // placeholder: solo navigazione visiva
        ElevatedCard(
            modifier = Modifier.size(40.dp),
            shape = CircleShape
        ) {}
    }
}

// ===== DECK: tipi & CompositionLocal =====
private enum class DeckRoot { PAGINA, MENU_LATERALE, MENU_CENTRALE, AVVISO }
private enum class SecondBarMode { Deck, Classic }

private data class DeckState(val openKey: String?, val toggle: (String) -> Unit)
// ↑ Aggiungiamo la possibilità di aprire il wizard direttamente dalla CPlus
private data class DeckController(
    val openChild: (DeckRoot) -> Unit,
    val openWizard: (DeckRoot) -> Unit
)

// Locals per pilotare MainMenuBar senza cambiare la sua firma
private val LocalSecondBarMode = compositionLocalOf { SecondBarMode.Deck }
private val LocalDeckState = compositionLocalOf { DeckState(null) { _ -> } }
// ↑ ora espone anche openWizard
private val LocalDeckController = compositionLocalOf {
    DeckController(openChild = { _ -> }, openWizard = { _ -> })
}
private val LocalIsPageContext = compositionLocalOf { false }



// ===== Token colore (variabili facili da cambiare) =====
private val DECK_HIGHLIGHT = Color(0xFF58A6FF) // bordo icona madre quando cluster aperto
private val DECK_BADGE_BG  = Color(0xFF22304B) // stendardetto ID figlio (bg)
private val DECK_BADGE_TXT = Color.White       // stendardetto ID figlio (testo)

@Composable
fun EditorMenusOnly(
    state: EditorShellState
) {
    var deckOpen by remember { mutableStateOf<String?>(null) }        // "pagina"|"menuL"|"menuC"|"avviso"|null
    var editingClass by remember { mutableStateOf<DeckRoot?>(null) }   // classe della figlia aperta (per flag Layout)
    // Path del menù (es. ["Contenitore", "Bordi", "Spessore"])
    var menuPath by remember { mutableStateOf<List<String>>(emptyList()) }
    // Selezioni effimere dei dropdown/toggle (key = pathTestuale)
    val menuSelections = remember { mutableStateMapOf<String, Any?>() }
    // Modifiche in corso (serve per mostrare la barra di conferma alla risalita)
    var dirty by remember { mutableStateOf(false) }
    // Ultima opzione interessata (per mostrare info extra nel path)
    var lastChanged by remember { mutableStateOf<String?>(null) }
    // Conferma all’uscita dai sottomenu verso la home
    var showConfirm by remember { mutableStateOf(false) }
    var classicEditing by remember { mutableStateOf(false) } // false = Deck, true = Classic (vecchia root)
    // MODE della seconda barra: "deck" (icone madre + cluster) oppure "classic" (vecchia root)
    // Preset salvati (nomi da mostrare nelle tendine)
    // Wizard di creazione
    var wizardVisible by remember { mutableStateOf(false) }
    var wizardKind by remember { mutableStateOf<DeckRoot?>(null) }
    var homePageId by remember { mutableStateOf<String?>(null) }
    var wizardTarget  by remember { mutableStateOf<DeckRoot?>(null) }

    fun openWizardFor(root: DeckRoot) {
        wizardTarget = root
        wizardVisible = true
    }
    BackHandler(enabled = wizardVisible) {
        wizardVisible = false
    }

    // Back: se il wizard è aperto, il tasto indietro chiude il wizard (non l'app)
    BackHandler(enabled = wizardVisible) { wizardVisible = false }
    val savedPresets = remember {
        mutableStateMapOf(
            "Layout" to mutableListOf("Nessuno", "Default chiaro", "Default scuro"),
            "Contenitore" to mutableListOf("Nessuno", "Card base", "Hero"),
            "Testo" to mutableListOf("Nessuno", "Titolo", "Sottotitolo", "Body")
        )
    }

    // Valori dei preset/stili: root -> (nome -> mappa configurazioni)
    val presetValues = remember {
        mutableStateMapOf<String, MutableMap<String, Map<String, Any?>>>(
            "Layout" to mutableMapOf(),
            "Contenitore" to mutableMapOf(),
            "Testo" to mutableMapOf()
        )
    }
    BackHandler(enabled = menuPath.isNotEmpty()) {
        if (menuPath.size == 1 && dirty) {
            showConfirm = true
        } else {
            menuPath = menuPath.dropLast(1)
            lastChanged = null
        }
    }

    BackHandler(enabled = menuPath.isEmpty()) {
        when {
            classicEditing -> classicEditing = false   // esci dalla Classic: torni alle icone madre
            deckOpen != null -> deckOpen = null        // chiudi il cluster aperto
            else -> Unit                                // ignora: evita che l’Activity si chiuda
        }
    }

    // Elenco chiavi COMPLETE + default per ciascun root (usiamo le stesse label del menu)
    fun keysForRoot(root: String): List<Pair<String, Any?>> {
        fun k(vararg segs: String) = segs.joinToString(" / ")
        return when (root) {
            "Testo" -> listOf(
                k("Testo","Sottolinea") to false,
                k("Testo","Corsivo") to false,
                k("Testo","Evidenzia") to "Nessuna",
                k("Testo","Font") to "System",
                k("Testo","Weight") to "Regular",
                k("Testo","Size") to "16sp",
                k("Testo","Colore") to "Nero",
            )
            "Contenitore" -> listOf(
                k("Contenitore","scroll") to "Assente",
                k("Contenitore","shape") to "Rettangolo",
                k("Contenitore","variant") to "Full",
                k("Contenitore","b_thick") to "1dp",
                k("Contenitore","tipo") to "Normale",
                k("Contenitore","Colore","col1") to "Bianco",
                k("Contenitore","Colore","col2") to "Grigio chiaro",
                k("Contenitore","Colore","grad") to "Orizzontale",
                k("Contenitore","Colore","fx") to "Vignettatura",
                k("Contenitore","Aggiungi foto","crop") to "Nessuno",
                k("Contenitore","Aggiungi foto","frame") to "Sottile",
                k("Contenitore","Aggiungi foto","filtro") to "Nessuno",
                k("Contenitore","Aggiungi foto","fitCont") to "Cover",
                k("Contenitore","Aggiungi album","cropAlbum") to "Nessuno",
                k("Contenitore","Aggiungi album","frameAlbum") to "Sottile",
                k("Contenitore","Aggiungi album","filtroAlbum") to "Nessuno",
                k("Contenitore","Aggiungi album","fit") to "Cover",
                k("Contenitore","Aggiungi album","anim") to "Slide",
                k("Contenitore","Aggiungi album","speed") to "Media",
            )
            "Layout" -> listOf(
                k("Layout","Colore","col1") to "Bianco",
                k("Layout","Colore","col2") to "Grigio chiaro",
                k("Layout","Colore","grad") to "Orizzontale",
                k("Layout","Colore","fx") to "Vignettatura",
                k("Layout","Aggiungi foto","crop") to "Nessuno",
                k("Layout","Aggiungi foto","frame") to "Sottile",
                k("Layout","Aggiungi foto","filtro") to "Nessuno",
                k("Layout","Aggiungi foto","fit") to "Cover",
                k("Layout","Aggiungi album","cropAlbum") to "Nessuno",
                k("Layout","Aggiungi album","frameAlbum") to "Sottile",
                k("Layout","Aggiungi album","filtroAlbum") to "Nessuno",
                k("Layout","Aggiungi album","fit") to "Cover",
                k("Layout","Aggiungi album","anim") to "Slide",
                k("Layout","Aggiungi album","speed") to "Media",
            )
            else -> emptyList()
        }
    }

    fun collectConfig(root: String): Map<String, Any?> =
        keysForRoot(root).associate { (k, def) -> k to (menuSelections[k] ?: def) }

    fun applyConfig(config: Map<String, Any?>) {
        config.forEach { (k, v) -> menuSelections[k] = v }
    }

    fun savePreset(root: String, name: String) {
        val normalized = name.trim()
        if (normalized.isBlank()) return
        val list = savedPresets.getOrPut(root) { mutableListOf("Nessuno") }
        // mantieni "Nessuno", rimuovi eventuali duplicati (case-insensitive), poi aggiungi
        list.removeAll { it.equals(normalized, ignoreCase = true) }
        list.add(normalized)
        val store = presetValues.getOrPut(root) { mutableMapOf() }
        store[normalized] = collectConfig(root)
    }

    fun applyPresetByName(root: String, name: String) {
        val cfg = presetValues[root]?.get(name) ?: return
        applyConfig(cfg)
    }

    fun resolveAndApply(root: String) {
        val defaultKey = key(listOf(root), "default")
        val styleKey = key(listOf(root), "style")
        val defSel = (menuSelections[defaultKey] as? String)?.trim().orEmpty()
        val styleSel = (menuSelections[styleKey] as? String)?.trim().orEmpty()
        when {
            styleSel.isNotEmpty() && !styleSel.equals("Nessuno", true) && presetValues[root]?.containsKey(styleSel) == true ->
                applyPresetByName(root, styleSel)
            defSel.isNotEmpty() && !defSel.equals("Nessuno", true) && presetValues[root]?.containsKey(defSel) == true ->
                applyPresetByName(root, defSel)
            else ->
                applyConfig(keysForRoot(root).associate { (k, def) -> k to def })
        }
    }

    // Seeding di alcuni preset iniziali (così "Default/Titolo/..." agiscono subito)
    LaunchedEffect(Unit) {
        fun ensure(root: String, name: String, values: Map<String, Any?>) {
            val store = presetValues.getOrPut(root) { mutableMapOf() }
            if (store[name] == null) store[name] = values
        }
        // TESTO
        ensure("Testo", "Titolo", mapOf(
            key(listOf("Testo"),"Sottolinea") to false,
            key(listOf("Testo"),"Corsivo") to false,
            key(listOf("Testo"),"Evidenzia") to "Nessuna",
            key(listOf("Testo"),"Font") to "Inter",
            key(listOf("Testo"),"Weight") to "Bold",
            key(listOf("Testo"),"Size") to "22sp",
            key(listOf("Testo"),"Colore") to "Bianco",
        ))
        ensure("Testo", "Sottotitolo", mapOf(
            key(listOf("Testo"),"Sottolinea") to false,
            key(listOf("Testo"),"Corsivo") to false,
            key(listOf("Testo"),"Evidenzia") to "Nessuna",
            key(listOf("Testo"),"Font") to "Inter",
            key(listOf("Testo"),"Weight") to "Medium",
            key(listOf("Testo"),"Size") to "18sp",
            key(listOf("Testo"),"Colore") to "Bianco",
        ))
        ensure("Testo", "Body", mapOf(
            key(listOf("Testo"),"Sottolinea") to false,
            key(listOf("Testo"),"Corsivo") to false,
            key(listOf("Testo"),"Evidenzia") to "Nessuna",
            key(listOf("Testo"),"Font") to "Inter",
            key(listOf("Testo"),"Weight") to "Regular",
            key(listOf("Testo"),"Size") to "16sp",
            key(listOf("Testo"),"Colore") to "Nero",
        ))
        // CONTENITORE
        ensure("Contenitore", "Card base", mapOf(
            key(listOf("Contenitore"),"scroll") to "Assente",
            key(listOf("Contenitore"),"shape") to "Rettangolo",
            key(listOf("Contenitore"),"variant") to "Outlined",
            key(listOf("Contenitore"),"b_thick") to "1dp",
            key(listOf("Contenitore","Colore"),"col1") to "Bianco",
            key(listOf("Contenitore","Colore"),"col2") to "Grigio chiaro",
        ))
        ensure("Contenitore", "Hero", mapOf(
            key(listOf("Contenitore"),"variant") to "Full",
            key(listOf("Contenitore","Colore"),"col1") to "Ciano",
            key(listOf("Contenitore","Colore"),"grad") to "Orizzontale",
        ))
        // LAYOUT
        ensure("Layout", "Default chiaro", mapOf(
            key(listOf("Layout","Colore"),"col1") to "Bianco",
            key(listOf("Layout","Colore"),"col2") to "Grigio chiaro",
            key(listOf("Layout","Colore"),"grad") to "Orizzontale",
            key(listOf("Layout","Colore"),"fx") to "Vignettatura",
        ))
        ensure("Layout", "Default scuro", mapOf(
            key(listOf("Layout","Colore"),"col1") to "Nero",
            key(listOf("Layout","Colore"),"col2") to "Grigio",
            key(listOf("Layout","Colore"),"grad") to "Verticale",
            key(listOf("Layout","Colore"),"fx") to "Noise",
        ))
    }


    // Misuro l’altezza della barra azioni per distanziare la barra categorie
    var actionsBarHeightPx by remember { mutableStateOf(0) }
    // Dialog salvataggio stile
    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }



    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1A1A), Color(0xFF242424)) // grigi scuri
                )
            )
    ) {
        if (menuPath.isEmpty()) {
            // PRIMA BARRA
            MainBottomBar(
                onUndo = { /* stub */ },
                onRedo = { /* stub */ },
                onSaveFile = { /* stub */ },
                onDelete = { /* stub */ },
                onDuplicate = { /* stub */ },
                onProperties = { /* stub */ },
                onLayout = { menuPath = listOf("Layout") },
                onCreate = { openWizardFor(DeckRoot.PAGINA) },  // ⟵ QUI
                onOpenList = { /* stub */ },
                onSaveProject = { /* stub */ },
                onOpenProject = { /* stub */ },
                onNewProject = { /* stub */ },
                onMeasured = { actionsBarHeightPx = it },
                discontinuousBottom = menuPath.isEmpty()
            )

            // SECONDA BARRA
            if (!classicEditing) {

                CompositionLocalProvider(
                    LocalSecondBarMode provides SecondBarMode.Deck,
                    LocalDeckState provides DeckState(
                        openKey = deckOpen,
                        toggle = { key -> deckOpen = if (deckOpen == key) null else key }
                    ),
                    LocalDeckController provides DeckController(
                        openChild = { root ->
                            classicEditing = true
                            editingClass = root
                            deckOpen = null
                        },
                        openWizard = { root ->
                            wizardTarget = root
                            wizardVisible = true
                        }
                    )
                ) {
                    MainMenuBar(
                        onLayout = { menuPath = listOf("Layout") },
                        onContainer = { menuPath = listOf("Contenitore") },
                        onText     = { menuPath = listOf("Testo") },
                        onAdd      = { menuPath = listOf("Aggiungi") },
                        bottomBarHeightPx = actionsBarHeightPx
                    )
                }
            } else {
                CompositionLocalProvider(
                    LocalSecondBarMode provides SecondBarMode.Classic,
                    LocalExitClassic provides { classicEditing = false }    // torna alle icone madre
                ) {
                    MainMenuBar(
                        onLayout = { menuPath = listOf("Layout") },
                        onContainer = { menuPath = listOf("Contenitore") },
                        onText = { menuPath = listOf("Testo") },
                        onAdd = { menuPath = listOf("Aggiungi") },
                        bottomBarHeightPx = actionsBarHeightPx
                    )
                }
            }
            CreationWizardOverlay(
                visible = wizardVisible,
                target = wizardTarget,
                onDismiss = { wizardVisible = false },
                onCreate = {
                    // TODO: aggiornerai gli elenchi reali (pagine, menù, avvisi)
                    wizardVisible = false
                }
            )
        }
        else {
            // IN SOTTOMENU: seconda barra = SubMenuBar; sotto c’è sempre il Breadcrumb.
            // Imposta il “contesto pagina” per mostrare (in Layout) le voci Top/Bottom bar SOLO per Pagine.
            val isPageCtx = classicEditing && (editingClass == DeckRoot.PAGINA)
            CompositionLocalProvider(LocalIsPageContext provides isPageCtx) {
                SubMenuBar(
                    path = menuPath,
                    selections = menuSelections,
                    onBack = {
                        if (menuPath.size == 1 && dirty) showConfirm = true
                        else {
                            menuPath = menuPath.dropLast(1)
                            lastChanged = null
                        }
                    },
                    onEnter = { label ->
                        // evita accumulo “foglie sorelle” (Aggiungi foto/album/video)
                        val leafSiblings = setOf("Aggiungi foto", "Aggiungi album", "Aggiungi video")
                        menuPath = when {
                            menuPath.lastOrNull() == label -> menuPath
                            menuPath.lastOrNull() in leafSiblings && label in leafSiblings ->
                                menuPath.dropLast(1) + label
                            else -> menuPath + label
                        }
                        lastChanged = null
                    },
                    onToggle = { label, value ->
                        val root = menuPath.firstOrNull() ?: "Contenitore"
                        menuSelections[key(menuPath, label)] = value
                        lastChanged = "$label: ${if (value) "ON" else "OFF"}"
                        dirty = true
                        // qualsiasi modifica manuale annulla lo STILE (Default resta)
                        val styleKey = key(listOf(root), "style")
                        val styleVal = (menuSelections[styleKey] as? String).orEmpty()
                        if (styleVal.isNotEmpty() && !styleVal.equals("Nessuno", true)) {
                            menuSelections[styleKey] = "Nessuno"
                        }
                    },
                    onPick = { label, value ->
                        val root = menuPath.firstOrNull() ?: "Contenitore"
                        val fullKey = key(menuPath, label)
                        menuSelections[fullKey] = value
                        lastChanged = "$label: $value"
                        dirty = true

                        when (label) {
                            "default" -> {
                                val name = value
                                if (name.equals("Nessuno", true)) {
                                    resolveAndApply(root)
                                } else {
                                    applyPresetByName(root, name)
                                    val styleVal = (menuSelections[key(listOf(root), "style")] as? String).orEmpty()
                                    if (styleVal.isNotEmpty() && !styleVal.equals("Nessuno", true) && !styleVal.equals(name, true)) {
                                        applyPresetByName(root, styleVal) // stile ha priorità
                                    }
                                }
                            }
                            "style" -> {
                                val name = value
                                if (name.equals("Nessuno", true)) {
                                    resolveAndApply(root)
                                } else {
                                    applyPresetByName(root, name) // applica subito e con precedenza
                                }
                            }
                            else -> {
                                // modifica puntuale → stile attivo passa a “Nessuno”
                                val styleKey = key(listOf(root), "style")
                                val currentStyle = (menuSelections[styleKey] as? String).orEmpty()
                                if (currentStyle.isNotEmpty() && !currentStyle.equals("Nessuno", true)) {
                                    menuSelections[styleKey] = "Nessuno"
                                }
                            }
                        }
                    },
                    savedPresets = savedPresets
                )
                BreadcrumbBar(path = menuPath, lastChanged = lastChanged)
            }

            // Barra di conferma (quando risali con modifiche)
            if (showConfirm) {
                ConfirmBar(
                    onCancel = {
                        dirty = false
                        lastChanged = null
                        showConfirm = false
                        menuPath = emptyList()
                    },
                    onOk = {
                        dirty = false
                        showConfirm = false
                        menuPath = emptyList()
                    },
                    onSavePreset = { showSaveDialog = true }
                )
            }

            // Dialog “Salva come stile”
            if (showSaveDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    title = { Text("Salva come stile") },
                    text = {
                        Column {
                            Text("Dai un nome allo stile corrente. Se esiste già, verrà aggiornato.")
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newPresetName,
                                onValueChange = { newPresetName = it },
                                singleLine = true,
                                label = { Text("Nome stile") }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val root = menuPath.firstOrNull() ?: "Contenitore"
                            val name = newPresetName.trim()
                            if (name.isNotBlank()) {
                                savePreset(root, name)
                            }
                            newPresetName = ""
                            dirty = false
                            showSaveDialog = false
                            showConfirm = false
                            menuPath = emptyList()
                        }) { Text("Salva") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            newPresetName = ""
                            showSaveDialog = false
                        }) { Text("Annulla") }
                    }
                )
            }
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
    onCreate: () -> Unit,            // rimane, se lo usi altrove
    onCreatePage: () -> Unit = {},   // NEW
    onCreateAlert: () -> Unit = {},  // NEW
    onCreateMenuLaterale: () -> Unit = {}, // NEW
    onCreateMenuCentrale: () -> Unit = {}, // NEW
    onOpenList: () -> Unit,
    onSaveProject: () -> Unit,
    onOpenProject: () -> Unit,
    onNewProject: () -> Unit,
    onMeasured: (Int) -> Unit,
    discontinuousBottom: Boolean = true
) {
    // --- stato locale ---
    var showCreateMenu by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }

    // densità e misure contenitore
    val localDensity = LocalDensity.current
    var containerHeightPx by remember { mutableStateOf(0f) }
    var containerLeftInRoot by remember { mutableStateOf(0f) }

    // misure per etichette
    var firstBlockCenter by remember { mutableStateOf<Float?>(null) }   // 4 icone sinistra
    var lastBlockCenter by remember { mutableStateOf<Float?>(null) }    // icone progetti
    var wElementi by remember { mutableStateOf(0f) }
    var wPagine by remember { mutableStateOf(0f) }
    var wProgressi by remember { mutableStateOf(0f) }

    // misure per "gap tra gruppi" (più affidabile dei puntini)
    var firstBlockRightEdge by remember { mutableStateOf<Float?>(null) }      // destra blocco 1
    var middleBlockLeftEdge by remember { mutableStateOf<Float?>(null) }      // sinistra blocco Pagine&Menù
    var preSecondBlockRightEdge by remember { mutableStateOf<Float?>(null) }  // destra blocco Crea+Lista
    var lastBlockLeftEdge by remember { mutableStateOf<Float?>(null) }        // sinistra blocco Progetti

    // misure dei puntini (se servono in futuro)
    var firstDotCenter by remember { mutableStateOf<Float?>(null) }
    var secondDotCenter by remember { mutableStateOf<Float?>(null) }

    // --- varia colore linea (scritte + linea) ---
    val lineAccent = Color(0xFF233049) // varia colore linea
    // --- alza etichette di poco rispetto alla linea ---
    val labelLift = 3.dp

    // stile etichette (usa lineAccent)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = lineAccent
    )

    // parametri linea a filo del bordo inferiore
    val underlineStroke = 1.dp // spessore
    val extraGapPad = 2.dp     // piccolo margine extra su entrambi i lati dei gap

    Surface(
        color = Color(0xFF0D1117),
        contentColor = Color.White,
        tonalElevation = 10.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = SAFE_BOTTOM_MARGIN)
            .height(BOTTOM_BAR_HEIGHT + BOTTOM_BAR_EXTRA)
            .onGloballyPositioned { onMeasured(it.size.height) }
    ) {
        val scroll = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    containerHeightPx = it.size.height.toFloat()
                    containerLeftInRoot = it.positionInRoot().x
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp)
                    .horizontalScroll(scroll),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --------- GRUPPO SINISTRO ---------
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {

                    // BLOCCO 1: quattro icone (ELEMENTI)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val left = coords.positionInRoot().x - containerLeftInRoot
                            val width = coords.size.width.toFloat()
                            firstBlockCenter = left + width / 2f
                            firstBlockRightEdge = left + width
                        }
                    ) {
                        ToolbarIconButton(Icons.Outlined.Undo, "Undo", onClick = onUndo)
                        ToolbarIconButton(Icons.Outlined.Redo, "Redo", onClick = onRedo)
                        ToolbarIconButton(EditorIcons.Delete, "Cestino", onClick = onDelete)
                        ToolbarIconButton(EditorIcons.Duplicate, "Duplica", onClick = onDuplicate)
                    }

                    // PUNTINO 1
                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val left = coords.positionInRoot().x - containerLeftInRoot
                            val width = coords.size.width.toFloat()
                            firstDotCenter = left + width / 2f
                        }
                    ) { dividerDot() }

                    // BLOCCO INTERMEDIO (PAGINE E MENÙ)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val left = coords.positionInRoot().x - containerLeftInRoot
                            middleBlockLeftEdge = left
                        }
                    ) {
                        ToolbarIconButton(EditorIcons.Settings, "Proprietà", onClick = onProperties)
                        ToolbarIconButton(EditorIcons.Layout, "Layout pagina", onClick = onLayout)
                        ToolbarIconButton(EditorIcons.Save, "Salva pagina", onClick = onSaveFile)
                    }
                }

                // --------- GRUPPO DESTRO ---------
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {

                    // BLOCCO PRE-SECONDO PUNTINO: CREA + LISTA
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val left = coords.positionInRoot().x - containerLeftInRoot
                            val width = coords.size.width.toFloat()
                            preSecondBlockRightEdge = left + width
                        }
                    ) {
                        // CREA
                        DropdownMenu(expanded = showCreateMenu, onDismissRequest = { showCreateMenu = false }) {
                            DropdownMenuItem(text = { Text("Nuova pagina") },
                                onClick = { showCreateMenu = false; onCreatePage() })
                            DropdownMenuItem(text = { Text("Nuovo avviso") },
                                onClick = { showCreateMenu = false; onCreateAlert() })
                            DropdownMenuItem(text = { Text("Menù laterale") },
                                onClick = { showCreateMenu = false; onCreateMenuLaterale() })
                            DropdownMenuItem(text = { Text("Menù centrale") },
                                onClick = { showCreateMenu = false; onCreateMenuCentrale() })
                        }

                        Box {
                            ToolbarIconButton(EditorIcons.Insert, "Crea", onClick = { showCreateMenu = true })
                            DropdownMenu(expanded = showCreateMenu, onDismissRequest = { showCreateMenu = false }) {
                                DropdownMenuItem(text = { Text("Nuova pagina") }, onClick = { showCreateMenu = false; onCreate() })
                                DropdownMenuItem(text = { Text("Nuovo avviso") }, onClick = { showCreateMenu = false })
                                DropdownMenuItem(text = { Text("Menù laterale") }, onClick = { showCreateMenu = false })
                                DropdownMenuItem(text = { Text("Menù centrale") }, onClick = { showCreateMenu = false })
                            }
                        }
                        // LISTA
                        Box {
                            ToolbarIconButton(Icons.Outlined.List, "Lista", onClick = { showListMenu = true; onOpenList() })
                            DropdownMenu(expanded = showListMenu, onDismissRequest = { showListMenu = false }) {
                                DropdownMenuItem(text = { Text("Pagine…") }, onClick = { showListMenu = false })
                                DropdownMenuItem(text = { Text("Avvisi…") }, onClick = { showListMenu = false })
                                DropdownMenuItem(text = { Text("Menu laterali…") }, onClick = { showListMenu = false })
                                DropdownMenuItem(text = { Text("Menu centrali…") }, onClick = { showListMenu = false })
                            }
                        }
                    }

                    // PUNTINO 2
                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val left = coords.positionInRoot().x - containerLeftInRoot
                            val width = coords.size.width.toFloat()
                            secondDotCenter = left + width / 2f
                        }
                    ) { dividerDot() }

                    // BLOCCO PROGETTI / PROGRESSI
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val left = coords.positionInRoot().x - containerLeftInRoot
                            val width = coords.size.width.toFloat()
                            lastBlockCenter = left + width / 2f
                            lastBlockLeftEdge = left
                        }
                    ) {
                        ToolbarIconButton(Icons.Outlined.Save, "Salva progetto", onClick = onSaveProject)
                        ToolbarIconButton(Icons.Outlined.FolderOpen, "Apri", onClick = onOpenProject)
                        ToolbarIconButton(Icons.Outlined.CreateNewFolder, "Nuovo progetto", onClick = onNewProject)
                    }
                }
            }

            // ---------- BORDO BIANCO: top/sinistra/destra SEMPRE continui; bottom condizionale ----------
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                val stroke = with(localDensity) { underlineStroke.toPx() }
                val y = containerHeightPx - stroke / 2f
                val pad = with(localDensity) { 6.dp.toPx() }
                val extra = with(localDensity) { extraGapPad.toPx() }

                // TOP (curvo + segmento) — segue gli angoli arrotondati della Surface
                val corner = with(localDensity) { 18.dp.toPx() } // deve combaciare con RoundedCornerShape(topStart/topEnd)
                val topY = stroke / 2f
                // segmento centrale
                drawLine(
                    color = lineAccent,
                    start = androidx.compose.ui.geometry.Offset(corner, topY),
                    end   = androidx.compose.ui.geometry.Offset(size.width - corner, topY),
                    strokeWidth = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Butt
                )
                // arco top-left (180° → 270°)
                drawArc(
                    color = lineAccent,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, stroke / 2f),
                    size = androidx.compose.ui.geometry.Size(2 * corner - stroke, 2 * corner - stroke),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
                // arco top-right (270° → 360°)
                drawArc(
                    color = lineAccent,
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width - (2 * corner) + stroke / 2f, stroke / 2f),
                    size = androidx.compose.ui.geometry.Size(2 * corner - stroke, 2 * corner - stroke),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
                // LEFT
                drawLine(
                    color = lineAccent,
                    start = androidx.compose.ui.geometry.Offset(stroke / 2f, 0f),
                    end   = androidx.compose.ui.geometry.Offset(stroke / 2f, size.height),
                    strokeWidth = stroke
                )
                // RIGHT
                drawLine(
                    color = lineAccent,
                    start = androidx.compose.ui.geometry.Offset(size.width - stroke / 2f, 0f),
                    end   = androidx.compose.ui.geometry.Offset(size.width - stroke / 2f, size.height),
                    strokeWidth = stroke
                )

                if (!discontinuousBottom) {
                    // PATH ATTIVO → bordo inferiore CONTINUO
                    drawLine(
                        color = lineAccent,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end   = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    return@Canvas
                }

                // STATO HOME → bordo inferiore con GAP sotto le scritte e tra i gruppi (come ora)
                val gaps = mutableListOf<Pair<Float, Float>>()

                firstBlockCenter?.let { cx ->
                    if (wElementi > 0f) gaps += (cx - wElementi / 2f - pad) to (cx + wElementi / 2f + pad)
                }
                if (firstDotCenter != null && secondDotCenter != null && wPagine > 0f) {
                    val cx = (firstDotCenter!! + secondDotCenter!!) / 2f
                    gaps += (cx - wPagine / 2f - pad) to (cx + wPagine / 2f + pad)
                }
                lastBlockCenter?.let { cx ->
                    if (wProgressi > 0f) gaps += (cx - wProgressi / 2f - pad) to (cx + wProgressi / 2f + pad)
                }
                if (firstBlockRightEdge != null && middleBlockLeftEdge != null) {
                    val s = (firstBlockRightEdge!! - extra).coerceAtLeast(0f)
                    val e = (middleBlockLeftEdge!! + extra).coerceAtMost(size.width)
                    if (e > s) gaps += s to e
                }
                if (preSecondBlockRightEdge != null && lastBlockLeftEdge != null) {
                    val s = (preSecondBlockRightEdge!! - extra).coerceAtLeast(0f)
                    val e = (lastBlockLeftEdge!! + extra).coerceAtMost(size.width)
                    if (e > s) gaps += s to e
                }

                gaps.sortBy { it.first }
                var x0 = 0f
                for ((gs, ge) in gaps) {
                    val startX = gs.coerceAtLeast(0f)
                    if (startX > x0) {
                        drawLine(
                            color = lineAccent,
                            start = androidx.compose.ui.geometry.Offset(x0, y),
                            end   = androidx.compose.ui.geometry.Offset(startX, y),
                            strokeWidth = stroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                    x0 = ge.coerceAtLeast(x0)
                }
                if (x0 < size.width) {
                    drawLine(
                        color = lineAccent,
                        start = androidx.compose.ui.geometry.Offset(x0, y),
                        end   = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            // ---------- ETICHETTE (baseline allineata alla linea inferiore) ----------
            // "elementi"
            if (firstBlockCenter != null) {
                var baselineElemPx by remember { mutableStateOf(0f) }
                Text(
                    "elementi",
                    style = labelStyle,
                    onTextLayout = { tlr -> baselineElemPx = tlr.getLineBaseline(0).toFloat() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .onGloballyPositioned { wElementi = it.size.width.toFloat() }
                        .offset {
                            val lineY = containerHeightPx - with(localDensity) { underlineStroke.toPx() } / 2f
                            val y = (lineY - baselineElemPx - with(localDensity) { labelLift.toPx() }).toInt()
                            val x = ((firstBlockCenter ?: 0f) - wElementi / 2f).toInt()
                            IntOffset(x, y)
                        }
                )
            }
            // "pagine e menù"
            if (firstDotCenter != null && secondDotCenter != null) {
                var baselinePagPx by remember { mutableStateOf(0f) }
                Text(
                    "pagine e menù",
                    style = labelStyle,
                    onTextLayout = { tlr -> baselinePagPx = tlr.getLineBaseline(0).toFloat() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .onGloballyPositioned { wPagine = it.size.width.toFloat() }
                        .offset {
                            val cx = (firstDotCenter!! + secondDotCenter!!) / 2f
                            val lineY = containerHeightPx - with(localDensity) { underlineStroke.toPx() } / 2f
                            val y = (lineY - baselinePagPx - with(localDensity) { labelLift.toPx() }).toInt()
                            val x = (cx - wPagine / 2f).toInt()
                            IntOffset(x, y)
                        }
                )
            }
            // "progressi"
            if (lastBlockCenter != null) {
                var baselineProgPx by remember { mutableStateOf(0f) }
                Text(
                    "progetti",
                    style = labelStyle,
                    onTextLayout = { tlr -> baselineProgPx = tlr.getLineBaseline(0).toFloat() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .onGloballyPositioned { wProgressi = it.size.width.toFloat() }
                        .offset {
                            val lineY = containerHeightPx - with(localDensity) { underlineStroke.toPx() } / 2f
                            val y = (lineY - baselineProgPx - with(localDensity) { labelLift.toPx() }).toInt()
                            val x = ((lastBlockCenter ?: 0f) - wProgressi / 2f).toInt()
                            IntOffset(x, y)
                        }
                )
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
    val dy = with(LocalDensity.current) {
        (if (bottomBarHeightPx > 0) bottomBarHeightPx.toDp() else BOTTOM_BAR_HEIGHT) +
                BARS_GAP + SAFE_BOTTOM_MARGIN
    }
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
            .height(TOP_BAR_HEIGHT)
    ) {
        val scroll = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (LocalSecondBarMode.current) {
                SecondBarMode.Classic -> {
                    LocalExitClassic.current?.let { exitClassic ->
                        ToolbarIconButton(
                            icon = Icons.Outlined.ArrowBack,
                            contentDescription = "Indietro",
                            onClick = LocalExitClassic.current
                        )
                    }

                    ToolbarIconButton(EditorIcons.Text, "Testo", onClick = onText)
                    ToolbarIconButton(EditorIcons.Container, "Contenitore", onClick = onContainer)
                    ToolbarIconButton(EditorIcons.Layout, "Layout", onClick = onLayout)
                    ToolbarIconButton(EditorIcons.Insert, "Aggiungi", onClick = onAdd)
                    ToolbarIconButton(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_question),
                        contentDescription = "Info",
                        onClick = { /* stub */ }
                    )
                }

                SecondBarMode.Deck -> {
                    // NUOVA ROOT — MADRI + CLUSTER, con hiding delle madri a destra
                    val deck = LocalDeckState.current
                    val controller = LocalDeckController.current

                    // Ordine e mapping: serve per stabilire cosa "sta a destra"
                    data class Mother(val key: String, val iconRes: Int, val root: DeckRoot, val sampleId: String)
                    val mothers = listOf(
                        Mother("pagina", R.drawable.ic_page, DeckRoot.PAGINA, "pg001"),
                        Mother("menuL", R.drawable.ic_menu_laterale, DeckRoot.MENU_LATERALE, "ml001"),
                        Mother("menuC", R.drawable.ic_menu_centrale, DeckRoot.MENU_CENTRALE, "mc001"),
                        Mother("avviso", R.drawable.ic_avviso, DeckRoot.AVVISO, "al001")
                    )
                    val activeIdx = mothers.indexOfFirst { it.key == deck.openKey }  // -1 = nessun cluster aperto

                    mothers.forEachIndexed { idx, m ->
                        val showMother = (activeIdx == -1) || (idx <= activeIdx)   // nascondi madri a destra
                        if (showMother) {
                            MotherIcon(
                                icon = ImageVector.vectorResource(id = m.iconRes),
                                contentDescription = when (m.root) {
                                    DeckRoot.PAGINA -> "Pagina"
                                    DeckRoot.MENU_LATERALE -> "Menù laterale"
                                    DeckRoot.MENU_CENTRALE -> "Menù centrale"
                                    DeckRoot.AVVISO -> "Avviso"
                                },
                                selected = deck.openKey == m.key,
                                onClick = { deck.toggle(m.key) },
                                ringColor = DECK_HIGHLIGHT
                            )
                            val controller = LocalDeckController.current
                            if (deck.openKey == m.key) {
                                // c+ apre il wizard per la madre corrente
                                CPlusIcon(onClick = { controller.openWizard(m.root) })

                                // figlia demo (tap → editor classico)
                                ChildIconWithBadge(
                                    icon = ImageVector.vectorResource(id = m.iconRes),
                                    id = m.sampleId,
                                    onClick = { controller.openChild(m.root) },
                                    badgeBg = DECK_BADGE_BG,
                                    badgeTxt = DECK_BADGE_TXT
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MotherIcon(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    ringColor: Color = DECK_HIGHLIGHT
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF1B2334), // stesso “nero” delle tue icone
        contentColor = Color.White,
        tonalElevation = if (selected) 6.dp else 0.dp,
        shadowElevation = if (selected) 6.dp else 0.dp,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, ringColor) else null
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}

@Composable
private fun CPlusIcon(
    onClick: () -> Unit,
    icon: ImageVector = ImageVector.vectorResource(id = R.drawable.ic_add_circle)
) {
    Surface(shape = CircleShape, color = Color(0xFF1B2334), contentColor = Color.White) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(icon, contentDescription = "Nuovo")
        }
    }
}

@Composable
private fun ChildIconWithBadge(
    icon: ImageVector,
    id: String,
    onClick: () -> Unit,
    badgeBg: Color = DECK_BADGE_BG,
    badgeTxt: Color = DECK_BADGE_TXT
) {
    val shown = id.take(8)
    Box(contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = Color(0xFF1B2334), contentColor = Color.White) {
            IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
                Icon(icon, contentDescription = shown)
            }
        }
        Surface(
            color = badgeBg,
            contentColor = badgeTxt,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, 14) } // stendardetto sotto, senza “alzare” l’icona
        ) {
            Text(
                text = shown,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 10.sp
            )
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
    val offsetY = with(LocalDensity.current) { (BOTTOM_BAR_HEIGHT + BOTTOM_BAR_EXTRA + BARS_GAP + SAFE_BOTTOM_MARGIN).roundToPx() }
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
            .offset { IntOffset(0, -offsetY) }
            .height(TOP_BAR_HEIGHT)
    ) {
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // back icon
            ToolbarIconButton(Icons.Outlined.ArrowBack, "Indietro", onClick = onBack)

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
            .padding(start = 12.dp, top = 0.dp, end = 12.dp, bottom = SAFE_BOTTOM_MARGIN)
            .height(BOTTOM_BAR_HEIGHT + BOTTOM_BAR_EXTRA)
    ) {
        val pretty = buildString {
            append(if (path.isEmpty()) "—" else path.joinToString("  →  "))
            lastChanged?.let { append("   •   "); append(it) } // ← mostra sempre scelte utente
        }
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
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

    // Aree "wrappate" del Layout che devono riusare SOLO i sottomenu whitelist (Colore/Immagini)
    val layoutAreas = setOf("Bottom bar", "Top bar", "Menù centrale", "Menù laterale")

    when (path.getOrNull(1)) {
        null -> {
            // ROOT Layout
            ToolbarIconButton(EditorIcons.Color, "Colore") { onEnter("Colore") }
            ToolbarIconButton(EditorIcons.Image, "Immagini") { onEnter("Immagini") }

            // NEW: aree che riusano i sottomenu del Layout (whitelist)

            ToolbarIconButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_call_to_action),
                contentDescription = "Bottom bar",
                onClick = { onEnter("Bottom bar") }
            )
            ToolbarIconButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_ad_units),
                contentDescription = "Top bar",
                onClick = { onEnter("Top bar") }
            )

            // DEFAULT
            IconDropdown(
                icon = Icons.Outlined.BookmarkAdd,
                contentDescription = "Scegli default",
                current = get("default") ?: saved["Layout"]?.firstOrNull(),
                options = saved["Layout"].orEmpty(),
                onSelected = { onPick("default", it) }
            )
            // STILE (Outlined custom o Material)
            IconDropdown(
                icon = ImageVector.vectorResource(id = R.drawable.ic_style),
                contentDescription = "Stile",
                current = get("style") ?: "Nessuno",
                options = saved["Layout"].orEmpty(),
                onSelected = { onPick("style", it) }
            )
            /* Variante senza risorsa:
            IconDropdown(
                icon = Icons.Outlined.Style,
                contentDescription = "Stile",
                current = get("style") ?: "Nessuno",
                options = saved["Layout"].orEmpty(),
                onSelected = { onPick("style", it) }
            )
            */
        }

        // ----- WRAPPER AREE: whitelist solo Colore/Immagini (+ sottomenu relativi) -----
        in layoutAreas -> {
            when (path.getOrNull(2)) {
                null -> {
                    // Mostra SOLO le voci consentite (whitelist), non tutte quelle presenti/future
                    ToolbarIconButton(EditorIcons.Color, "Colore") { onEnter("Colore") }
                    ToolbarIconButton(EditorIcons.Image, "Immagini") { onEnter("Immagini") }
                }
                "Colore" -> {
                    IconDropdown(EditorIcons.Colors1, "Colore 1",
                        current = get("col1") ?: "Bianco",
                        options = listOf("Bianco", "Grigio", "Nero", "Ciano"),
                        onSelected = { onPick("col1", it) }
                    )
                    IconDropdown(EditorIcons.Colors2, "Colore 2",
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

        // ----- Layout “piano” (già esistente) -----
        "Colore" -> {
            IconDropdown(EditorIcons.Colors1, "Colore 1",
                current = get("col1") ?: "Bianco",
                options = listOf("Bianco", "Grigio", "Nero", "Ciano"),
                onSelected = { onPick("col1", it) }
            )
            IconDropdown(EditorIcons.Colors2, "Colore 2",
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
            fun get(keyLeaf: String) = selections[key(path, keyLeaf)] as? String

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

            // DEFAULT
            IconDropdown(
                icon = Icons.Outlined.BookmarkAdd,
                contentDescription = "Scegli default",
                current = get("default") ?: saved["Contenitore"]?.firstOrNull(),
                options = saved["Contenitore"].orEmpty(),
                onSelected = { onPick("default", it) }
            )

            // STILE
            IconDropdown(
                icon = ImageVector.vectorResource(id = R.drawable.ic_style),
                contentDescription = "Stile",
                current = get("style") ?: "Nessuno",
                options = saved["Contenitore"].orEmpty(),
                onSelected = { onPick("style", it) }
            )
        }
        "Colore" -> {
            IconDropdown(EditorIcons.Colors1, "Colore 1",
                current = get("col1") ?: "Bianco",
                options = listOf("Bianco", "Grigio", "Nero", "Ciano"),
                onSelected = { onPick("col1", it) }
            )
            IconDropdown(EditorIcons.Colors2, "Colore 2",
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
    val uKey = key(path, "Sottolinea")
    val iKey = key(path, "Corsivo")
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

    // dropdown (font / weight / size / evidenzia / colore) — chiavi allineate a keysForRoot("Testo")
    IconDropdown(EditorIcons.Highlight, "Evidenzia",
        current = (selections[key(path, "Evidenzia")] as? String) ?: "Nessuna",
        options = listOf("Nessuna", "Marker", "Oblique", "Scribble"),
        onSelected = { onPick("Evidenzia", it) }
    )
    IconDropdown(EditorIcons.CustomTypography, "Font",
        current = (selections[key(path, "Font")] as? String) ?: "System",
        options = listOf("System", "Inter", "Roboto", "SF Pro"),
        onSelected = { onPick("Font", it) }
    )
    IconDropdown(EditorIcons.Bold, "Peso",
        current = (selections[key(path, "Weight")] as? String) ?: "Regular",
        options = listOf("Light", "Regular", "Medium", "Bold"),
        onSelected = { onPick("Weight", it) }
    )
    IconDropdown(EditorIcons.Size, "Size",
        current = (selections[key(path, "Size")] as? String) ?: "16sp",
        options = listOf("12sp", "14sp", "16sp", "18sp", "22sp"),
        onSelected = { onPick("Size", it) }
    )
    IconDropdown(EditorIcons.Brush, "Colore",
        current = (selections[key(path, "Colore")] as? String) ?: "Nero",
        options = listOf("Nero", "Bianco", "Blu", "Verde", "Rosso"),
        onSelected = { onPick("Colore", it) }
    )

    // default
    IconDropdown(
        icon = Icons.Outlined.BookmarkAdd,
        contentDescription = "Scegli default",
        current = (selections[key(path, "default")] as? String) ?: saved["Testo"]?.firstOrNull(),
        options = saved["Testo"].orEmpty(),
        onSelected = { onPick("default", it) })
    IconDropdown(
        icon = ImageVector.vectorResource(id = R.drawable.ic_style),
        contentDescription = "Stile",
        current = (selections[key(path, "style")] as? String) ?: "Nessuno",
        options = saved["Testo"].orEmpty(),
        onSelected = { onPick("style", it) }
    )
    /* Variante senza risorsa:
    IconDropdown(
        icon = Icons.Outlined.Style,
        contentDescription = "Stile",
        current = (selections[key(path, "style")] as? String) ?: "Nessuno",
        options = saved["Testo"].orEmpty(),
        onSelected = { onPick("style", it) }
    )
    */
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
    Surface(
        shape = CircleShape,
        color = Color(0xFF1B2334),
        contentColor = Color.White,
        tonalElevation = if (selected) 6.dp else 0.dp,
        shadowElevation = if (selected) 6.dp else 0.dp,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null,
        modifier = Modifier.size(42.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = null)
        }
    }
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
            ToolbarIconButton(Icons.Outlined.BookmarkAdd, "Salva impostazioni", onClick = onSavePreset)
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

// ======================================================================
// WIZARD OVERLAY — crea Pagina / Menù Laterale / Menù Centrale / Avviso
// Aspetto scuro, barre coperte da scrim, nessuna logica esterna.
// ======================================================================
private data class CreationResult(
    val kind: DeckRoot,
    val id: String,
    val title: String,
    val description: String?,
    val scroll: String,           // "Assente" | "Verticale" | "Orizzontale"
    val assocId: String?,         // eventuale associazione
    val side: String?,            // solo per menù laterale: "Sinistra"|"Destra"|"Alto"|"Basso"
    val setAsHome: Boolean        // solo per pagine
)

@Composable
private fun BoxScope.CreationWizardOverlay(
    visible: Boolean,
    target: DeckRoot?,
    onDismiss: () -> Unit,
    onCreate: (WizardResult) -> Unit
) {
    if (!visible) return
    val darkBg = Color(0xFF0D1117) // stile GitHub scuro
    val panelBg = Color(0xFF131A24)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center),
        color = darkBg.copy(alpha = 0.98f),
        contentColor = Color.White
    ) {
        // stato campi
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var id by remember { mutableStateOf("") }
        var idEdited by remember { mutableStateOf(false) }

        // associazione
        var assocMode by remember { mutableStateOf("manual") } // "manual" | "tap3s"
        var assocId by remember { mutableStateOf("") }

        // specifici per Pagine
        var scroll by remember { mutableStateOf("Nessuna") } // Nessuna | Verticale | Orizzontale
        var setAsHome by remember { mutableStateOf(false) }

        // specifici per Menù laterale
        var side by remember { mutableStateOf("Sinistra") } // Sinistra | Destra | Alto | Basso

        // regole ID auto
        fun prefixFor(root: DeckRoot?) = when (root) {
            DeckRoot.PAGINA -> "pg"
            DeckRoot.MENU_LATERALE -> "ml"
            DeckRoot.MENU_CENTRALE -> "mc"
            DeckRoot.AVVISO -> "al"
            else -> "pg"
        }

        fun sanitize(s: String) = s.filter { it.isLetterOrDigit() }
        fun autoIdFrom(name: String, root: DeckRoot?): String {
            val p = prefixFor(root)
            val base = sanitize(name).lowercase()
            val candidate = if (base.length >= 5) base.take(8) else (base + "001").take(8)
            val withPrefix = (p + candidate).take(8)
            // garantiamo min 5
            return if (withPrefix.length < 5) (withPrefix + "0".repeat(5 - withPrefix.length)) else withPrefix
        }

        LaunchedEffect(name, target) {
            // aggiorno ID auto se l’utente non ha “bloccato” l’ID
            if (!idEdited) {
                id = autoIdFrom(name, target)
            }
        }

        // header
        Column(Modifier.fillMaxSize()) {
            // Barra superiore del wizard
            Surface(
                color = panelBg,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Chiudi")
                    }
                    val title = when (target) {
                        DeckRoot.PAGINA -> "Nuova pagina"
                        DeckRoot.MENU_LATERALE -> "Nuovo menù laterale"
                        DeckRoot.MENU_CENTRALE -> "Nuovo menù centrale"
                        DeckRoot.AVVISO -> "Nuovo avviso"
                        else -> "Nuovo elemento"
                    }
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { /* help globale, se vuoi */ }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Aiuto")
                    }
                }
            }

            // Corpo pannello
            Column(
                Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(panelBg)
                    .padding(16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = id,
                    onValueChange = {
                        id = sanitize(it).take(8)
                        idEdited = true
                    },
                    label = { Text("ID (5–8 caratteri, auto se vuoto o troppo corto)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione (opzionale)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Associazione
                Text("Associazione (mostrare elemento collegato)", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = assocMode == "manual", onClick = { assocMode = "manual" })
                        Text("ID manuale")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = assocMode == "tap3s", onClick = { assocMode = "tap3s" })
                        Text("Seleziona a schermo (3s)")
                        IconButton(onClick = { /* tooltip/modal di aiuto */ }) {
                            Icon(Icons.Outlined.HelpOutline, contentDescription = "Come funziona")
                        }
                    }
                }
                if (assocMode == "manual") {
                    OutlinedTextField(
                        value = assocId,
                        onValueChange = { assocId = sanitize(it).take(16) },
                        label = { Text("ID elemento associato") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "Entra in modalità selezione tenendo premuto 3s sul componente desiderato. (stub)",
                        fontSize = 12.sp,
                        color = Color(0xFF9BA3AF)
                    )
                    OutlinedButton(onClick = { /* TODO: abilita modalità selezione */ }) {
                        Text("Avvia selezione (stub)")
                    }
                }

                // Campi specifici per tipo
                when (target) {
                    DeckRoot.PAGINA -> {
                        Text("Opzioni pagina", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Scrollabilità:")
                            OptionPill(selected = scroll == "Nessuna", onClick = { scroll = "Nessuna" }, label = "Nessuna")
                            OptionPill(selected = scroll == "Verticale", onClick = { scroll = "Verticale" }, label = "Verticale")
                            OptionPill(selected = scroll == "Orizzontale", onClick = { scroll = "Orizzontale" }, label = "Orizzontale")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Switch(checked = setAsHome, onCheckedChange = { setAsHome = it })
                            Text("Imposta come Home")
                        }
                    }
                    DeckRoot.MENU_LATERALE -> {
                        Text("Opzioni menù laterale", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Lato:")
                            OptionPill(selected = side == "Sinistra", onClick = { side = "Sinistra" }, label = "Sinistra")
                            OptionPill(selected = side == "Destra",   onClick = { side = "Destra"   }, label = "Destra")
                            OptionPill(selected = side == "Alto",     onClick = { side = "Alto"     }, label = "Alto")
                            OptionPill(selected = side == "Basso",    onClick = { side = "Basso"    }, label = "Basso")
                        }
                    }
                    DeckRoot.MENU_CENTRALE -> {
                        Text("Opzioni menù centrale", fontWeight = FontWeight.SemiBold)
                        Text("Nessuna opzione speciale per ora.", color = Color(0xFF9BA3AF), fontSize = 12.sp)
                    }
                    DeckRoot.AVVISO -> {
                        Text("Opzioni avviso", fontWeight = FontWeight.SemiBold)
                        OutlinedButton(onClick = { /* apri ECA mode (stub) */ }) {
                            Text("Apri Event–Condition–Action mode (stub)")
                        }
                    }
                    else -> Unit
                }
            }

            // footer
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Annulla")
                }
                Button(
                    onClick = {
                        val finalId = (if (id.isNotBlank()) sanitize(id) else autoIdFrom(name, target)).take(8)
                        val finalName = if (name.isBlank()) finalId else name
                        onCreate(
                            WizardResult(
                                root = target ?: DeckRoot.PAGINA,
                                id = finalId,
                                name = finalName,
                                description = description.ifBlank { "n/a" },
                                assocId = assocId.takeIf { assocMode == "manual" && it.isNotBlank() },
                                scroll = scroll,
                                setAsHome = (target == DeckRoot.PAGINA && setAsHome),
                                side = (target == DeckRoot.MENU_LATERALE).let { if (it) side else null }
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Crea")
                }
            }
        }
    }
}

@Composable
private fun OptionPill(selected: Boolean, onClick: () -> Unit, label: String) {
    OutlinedButton(
        onClick = onClick,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null
    ) { Text(label) }
}

private data class WizardResult(
    val root: DeckRoot,
    val id: String,
    val name: String,
    val description: String,
    val assocId: String?,
    val scroll: String,
    val setAsHome: Boolean = false,
    val side: String? = null
)


// Piccolo dropdown “scuro” in linea con lo stile
@Composable
private fun DropdownSmall(
    current: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(current) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelected(opt); open = false })
            }
        }
    }
}

// Chip minimale, aspetto scuro, niente API sperimentali
@Composable
private fun FilterChipLike(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    val bg = if (selected) Color(0xFF22304B) else Color(0xFF1B2334)
    Surface(
        color = bg,
        contentColor = Color.White,
        shape = RoundedCornerShape(12.dp)
    ) {
        TextButton(onClick = onClick) {
            Text(label)
        }
    }
}



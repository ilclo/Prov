package com.example.appbuilder.editor

import com.example.appbuilder.AppEngine
import com.example.appbuilder.eca.ActionHandler
import com.example.appbuilder.core.Binding
import com.example.appbuilder.core.RefreshPolicy
import com.example.appbuilder.data.HttpTextSource
import com.example.appbuilder.data.FakeFolderSource
import com.example.appbuilder.ui.BoundDropdown
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.appbuilder.R


/* ---- BARS: altezze fisse + gap ---- */
private val BOTTOM_BAR_HEIGHT = 56.dp        // barra inferiore (base)
private val BOTTOM_BAR_EXTRA = 8.dp          // extra altezza barra inferiore (stessa in Home e Submenu)
private val TOP_BAR_HEIGHT = 52.dp           // barra superiore (categorie / submenu)
private val BARS_GAP = 14.dp                 // distacco tra le due barre (+2dp di “aria”)
private val SAFE_BOTTOM_MARGIN = 32.dp     // barra inferiore più alta rispetto al bordo schermo


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
    // Conferma all’uscita dai sottomenu verso la home
    var showConfirm by remember { mutableStateOf(false) }
    var engineReady by remember { mutableStateOf(false) }

    // Preset salvati (nomi da mostrare nelle tendine)
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



    LaunchedEffect(Unit) {
        AppEngine.init(object : ActionHandler {
            override fun showBanner(text: String) { /* TODO snackbar */ }
            override fun openMenu(name: String) { /* hook al tuo menuPath se vuoi */ }
            override fun navigate(path: String) { }
            override fun swapTemplate(component: String, template: String) { }
        })
        engineReady = true // <— aggiungi questa riga
    }


    // 2) Registra fonti dati + binding MVP (esempio: lista città + clip mock)
    LaunchedEffect(Unit) {
        // SORGENTI
        AppEngine.data.register(
            HttpTextSource(
                id = "src_citta",
                url = "http://127.0.0.1:8080/",
                separator = '|'
            )
        )
        AppEngine.data.register(
            FakeFolderSource(
                id = "src_clip",
                items = listOf("demo.mp4", "cover.jpg", "intro.mov")  // mock
            )
        )

        // BINDING (componentPath -> source)
        AppEngine.bindings.set(
            Binding(
                componentPath = "Home/DropdownCitta",
                sourceId = "src_citta",
                refresh = RefreshPolicy.ON_ENTER_PAGE
            )
        )
    }


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
                onMeasured = { actionsBarHeightPx = it },
                discontinuousBottom = menuPath.isEmpty() // ⟵ Home = discontinuo; con path (se visibile) = continuo
            )
            MainMenuBar(
                onLayout = { menuPath = listOf("Layout") },
                onContainer = { menuPath = listOf("Contenitore") },
                onText = { menuPath = listOf("Testo") },
                onAdd = { menuPath = listOf("Aggiungi") },
                bottomBarHeightPx = actionsBarHeightPx
            )
            // Anteprima in alto a sinistra (solo quando il motore è pronto)
            if (engineReady) {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    // Usa la sorgente HTTP (src_citta) — ricordati INTERNET e un URL valido
                    BoundDropdown(
                        binding = Binding("Home/DropdownCitta", "src_citta"),
                        data = AppEngine.data,
                        engine = AppEngine.engine,
                        label = "Seleziona città"
                    )
                }
            }
        } else {
            // IN MENU: mostro pannello di livello corrente + breadcrumb
            SubMenuBar(
                path = menuPath,
                selections = menuSelections,
                onBack = {
                    if (menuPath.size == 1 && dirty) showConfirm = true
                    else {
                        menuPath = menuPath.dropLast(1)
                        lastChanged = null   // ← niente “scia” nel breadcrumb
                    }
                },
                onEnter = { label ->
                    // Sibling foglia nello stesso ramo (immagini)
                    val leafSiblings = setOf("Aggiungi foto", "Aggiungi album", "Aggiungi video")
                    menuPath = when {
                        menuPath.lastOrNull() == label -> menuPath
                        menuPath.lastOrNull() in leafSiblings && label in leafSiblings ->
                            menuPath.dropLast(1) + label   // ← sostituisci, non accumulare
                        else -> menuPath + label
                    }
                    // Navigazione ≠ scelta: non mostrare nel breadcrumb
                    lastChanged = null
                },
                onToggle = { label, value ->
                    val root = menuPath.firstOrNull() ?: "Contenitore"
                    menuSelections[key(menuPath, label)] = value
                    lastChanged = "$label: ${if (value) "ON" else "OFF"}"
                    dirty = true
                    // Se c'era uno STILE attivo, qualsiasi modifica manuale lo annulla (Default resta)
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
                                // Applica eventuale stile, altrimenti reset a default base
                                resolveAndApply(root)
                            } else {
                                // Applica prima il Default...
                                applyPresetByName(root, name)
                                // ...poi se c'è uno Stile diverso da Nessuno → lo Stile vince
                                val styleVal = (menuSelections[key(listOf(root), "style")] as? String).orEmpty()
                                if (styleVal.isNotEmpty() && !styleVal.equals("Nessuno", true) && !styleVal.equals(name, true)) {
                                    applyPresetByName(root, styleVal)
                                }
                            }
                        }
                        "style" -> {
                            val name = value
                            if (name.equals("Nessuno", true)) {
                                // Se tolgo lo stile: applica default se presente, altrimenti default base
                                resolveAndApply(root)
                            } else {
                                // Stile applicato istantaneamente e con precedenza
                                applyPresetByName(root, name)
                            }
                        }
                        else -> {
                            // Modifica puntuale: Stile → Nessuno (Default resta selezionato)
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
                            savePreset(root, name) // crea/aggiorna con i valori correnti
                        }
                        newPresetName = ""
                        dirty = false
                        showSaveDialog = false
                        showConfirm = false
                        menuPath = emptyList()
                    }) {
                        Text("Salva")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        newPresetName = ""
                        showSaveDialog = false
                    }) {
                        Text("Annulla")
                    }
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
    onMeasured: (Int) -> Unit,
    discontinuousBottom: Boolean = true // ⟵ NEW
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
                .padding(horizontal = 10.dp)
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
            ToolbarIconButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_view_day),
                contentDescription = "Menù centrale",
                onClick = { onEnter("Menù centrale") }
            )
            ToolbarIconButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_view_sidebar),
                contentDescription = "Menù laterale",
                onClick = { onEnter("Menù laterale") }
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
package com.example.appbuilder.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding

/**
 * Schermata editor di base: solo menu, niente interazioni con la pagina.
 * Sfondo bianco, due barre inferiori permanenti.
 */
@Composable
fun AppEditorScreen() {
    MaterialTheme(colorScheme = darkGitHubishScheme()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Placeholder contenuto pagina (bianco, non interattivo)
            // ...

            // Barre inferiori (sempre visibili)
            BottomBars()
        }
    }
}

@Composable
private fun BottomBars() {
    // Stato fittizio (aperture menu a tendina)
    var showCreate by remember { mutableStateOf(false) }
    var showList by remember { mutableStateOf(false) }

    // Spazio desiderato sopra il bordo inferiore (per lasciare posto ad una futura bottom bar di sistema)
    val extraBottomGap = with(LocalDensity.current) { 24.dp.roundToPx() }

    // --- 1) Barra comandi principale ---
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .imePadding() // si adagia sulla tastiera
            .padding(bottom = 16.dp) // “un paio di cm”
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        val scroll = rememberScrollState()
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sinistra: undo/redo
            IconButton(onClick = { /* undo (stub) */ }) { Icon(Icons.Filled.Undo, null) }
            IconButton(onClick = { /* redo (stub) */ }) { Icon(Icons.Filled.Redo, null) }

            // Salva pagina corrente
            IconButton(onClick = { /* save page (stub) */ }) { Icon(Icons.Filled.Save, null) }

            // Elimina / Duplica (dipenderanno da selezione, qui sono attivi sempre come mock)
            IconButton(onClick = { /* delete (stub) */ }) { Icon(Icons.Filled.Delete, null) }
            IconButton(onClick = { /* duplicate (stub) */ }) { Icon(Icons.Filled.ContentCopy, null) }

            // Proprietà e layout pagina
            IconButton(onClick = { /* proprietà (stub) */ }) { Icon(Icons.Filled.Settings, null) }
            IconButton(onClick = { /* layout pagina (stub) */ }) { Icon(Icons.Filled.DashboardCustomize, null) }

            // Crea ⌄
            Box {
                IconButton(onClick = { showCreate = true }) { Icon(Icons.Filled.Add, null) }
                DropdownMenu(expanded = showCreate, onDismissRequest = { showCreate = false }) {
                    DropdownMenuItem(
                        text = { Text("Nuova pagina") },
                        onClick = { showCreate = false /* stub */ },
                        leadingIcon = { Icon(Icons.Filled.NoteAdd, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Nuovo avviso (overlay)") },
                        onClick = { showCreate = false /* stub */ },
                        leadingIcon = { Icon(Icons.Filled.Announcement, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Nuovo menù laterale") },
                        onClick = { showCreate = false /* stub */ },
                        leadingIcon = { Icon(Icons.Filled.ViewSidebar, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Nuovo menù centrale") },
                        onClick = { showCreate = false /* stub */ },
                        leadingIcon = { Icon(Icons.Filled.ViewDay, null) }
                    )
                }
            }

            // Lista elementi ⌄
            Box {
                IconButton(onClick = { showList = true }) { Icon(Icons.Filled.List, null) }
                DropdownMenu(expanded = showList, onDismissRequest = { showList = false }) {
                    // mock elenco
                    DropdownMenuItem(text = { Text("Pagina: Home") }, onClick = { showList = false })
                    DropdownMenuItem(text = { Text("Avviso: Info") }, onClick = { showList = false })
                    DropdownMenuItem(text = { Text("Menù laterale: App") }, onClick = { showList = false })
                }
            }

            // Spacer centrale
            Spacer(Modifier.weight(1f))

            // Annulla / OK (conferma proprietà)
            TextButton(onClick = { /* annulla (stub) */ }) {
                Icon(Icons.Filled.Close, null)
                Spacer(Modifier.width(6.dp))
                Text("Annulla")
            }
            Button(onClick = { /* ok (stub) */ }) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(6.dp))
                Text("OK")
            }

            // Salva progetto / Apri / Nuovo
            OutlinedButton(onClick = { /* salva progetto (stub) */ }) {
                Icon(Icons.Filled.SaveAs, null); Spacer(Modifier.width(6.dp)); Text("Salva progetto")
            }
            IconButton(onClick = { /* apri/importa (stub) */ }) { Icon(Icons.Filled.FolderOpen, null) }
            IconButton(onClick = { /* nuovo progetto (stub) */ }) { Icon(Icons.Filled.CreateNewFolder, null) }
        }
    }

    // --- 2) Barra di contesto (sopra la principale) ---
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .imePadding()
            .padding(bottom = 90.dp) // si posiziona “sopra” la barra comandi
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        val scroll = rememberScrollState()
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AssistChip(onClick = { /* testo (stub) */ }, label = { Text("Testo") }, leadingIcon = {
                Icon(Icons.Filled.TextFields, contentDescription = null)
            })
            AssistChip(onClick = { /* contenitore (stub) */ }, label = { Text("Contenitore") }, leadingIcon = {
                Icon(Icons.Filled.Category, contentDescription = null)
            })
            AssistChip(onClick = { /* aggiungi altro (stub) */ }, label = { Text("Aggiungi") }, leadingIcon = {
                Icon(Icons.Filled.AddCircle, contentDescription = null)
            })
        }
    }
}

@Composable
private fun darkGitHubishScheme(): ColorScheme {
    // Palette scura sobria (ispirazione GitHub dark)
    return darkColorScheme(
        surface = Color(0xFF0D1117),
        onSurface = Color(0xFFC9D1D9),
        primary = Color(0xFF58A6FF),
        onPrimary = Color(0xFF0D1117),
        secondary = Color(0xFF8B949E),
        outline = Color(0xFF30363D)
    )
}

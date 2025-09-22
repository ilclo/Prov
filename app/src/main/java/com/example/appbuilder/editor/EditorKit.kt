package com.example.appbuilder.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding

/**
 * Schermata demo: solo menù (nessun canvas, nessun contenitore selezionabile).
 * Sfondo bianco neutro. Toni scuri per le barre.
 */
@Composable
fun MenusOnlyScreen() {
    // Forziamo una palette scura per dare subito l’effetto “GitHub-like”
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White) // sfondo pagina “vuota”
        ) {
            // Overlay delle barre (sempre visibili)
            EditorChromeBars()
        }
    }
}

/**
 * Chrome dell’editor:
 * - Barra comandi inferiore (scrollabile) con tutte le icone richieste + OK / Salva progetto.
 * - Barra contestuale sopra (Testo / Contenitore / +).
 */
@Composable
private fun BoxScope.EditorChromeBars() {
    // Stato dei due menù a tendina (“Crea” e “Lista”)
    var createMenuOpen by remember { mutableStateOf(false) }
    var listMenuOpen by remember { mutableStateOf(false) }

    // Ancora per i popup (usiamo Box locali per avere una posizione stabile)
    var createMenuAnchor by remember { mutableStateOf<IntOffset?>(null) }
    var listMenuAnchor by remember { mutableStateOf<IntOffset?>(null) }

    // -------- Barra COMANDI inferiore (principale)
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp) // un po’ sopra al limite inferiore (spazio bottombar)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .imePadding()
    ) {
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // blocco sinistro (undo/redo + salva file)
            GroupPill {
                IconButton(onClick = { /* TODO undo */ })  { Icon(Icons.Filled.Undo, contentDescription = "Annulla") }
                IconButton(onClick = { /* TODO redo */ })  { Icon(Icons.Filled.Redo, contentDescription = "Ripeti") }
                DividerDot()
                IconButton(onClick = { /* TODO salva file corrente */ })  { Icon(Icons.Filled.Save, contentDescription = "Salva file") }
            }

            // blocco modifica/oggetti
            GroupPill {
                IconButton(onClick = { /* TODO elimina */ })     { Icon(Icons.Filled.Delete, contentDescription = "Elimina") }
                IconButton(onClick = { /* TODO duplica */ })     { Icon(Icons.Filled.ContentCopy, contentDescription = "Duplica") }
                IconButton(onClick = { /* TODO proprietà */ })   { Icon(Icons.Filled.Settings, contentDescription = "Proprietà") }
                IconButton(onClick = { /* TODO layout pagina */ }){ Icon(Icons.Filled.Article, contentDescription = "Layout pagina") }
            }

            // Crea (dropdown)
            Box {
                IconButton(
                    onClick = { createMenuOpen = true },
                ) { Icon(Icons.Filled.AddCircle, contentDescription = "Crea") }

                DropdownMenu(
                    expanded = createMenuOpen,
                    onDismissRequest = { createMenuOpen = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    DropdownMenuItem(text = { Text("Nuova pagina") },    onClick = { /* TODO */ createMenuOpen = false })
                    DropdownMenuItem(text = { Text("Nuovo avviso") },    onClick = { /* TODO */ createMenuOpen = false })
                    DropdownMenuItem(text = { Text("Nuovo menù laterale") }, onClick = { /* TODO */ createMenuOpen = false })
                    DropdownMenuItem(text = { Text("Nuovo menù centrale") }, onClick = { /* TODO */ createMenuOpen = false })
                }
            }

            // Lista (dropdown)
            Box {
                IconButton(onClick = { listMenuOpen = true }) { Icon(Icons.Filled.ViewList, contentDescription = "Lista elementi") }
                DropdownMenu(
                    expanded = listMenuOpen,
                    onDismissRequest = { listMenuOpen = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    DropdownMenuItem(text = { Text("Pagine (0)") }, onClick = { listMenuOpen = false })
                    DropdownMenuItem(text = { Text("Avvisi (0)") }, onClick = { listMenuOpen = false })
                    DropdownMenuItem(text = { Text("Menù laterali (0)") }, onClick = { listMenuOpen = false })
                    DropdownMenuItem(text = { Text("Menù centrali (0)") }, onClick = { listMenuOpen = false })
                }
            }

            Spacer(Modifier.weight(1f)) // spinge a destra i bottoni di conferma

            // Conferma / Progetto
            GroupPill {
                TextButton(onClick = { /* TODO annulla modifiche proprietà correnti */ }) {
                    Text("Annulla")
                }
                Button(onClick = { /* TODO conferma */ }) {
                    Icon(Icons.Filled.Check, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("OK")
                }
            }

            GroupPill {
                IconButton(onClick = { /* TODO salva progetto */ }) { Icon(Icons.Filled.Save, contentDescription = "Salva progetto") }
                IconButton(onClick = { /* TODO apri/importa */ })   { Icon(Icons.Filled.FolderOpen, contentDescription = "Apri/Importa") }
                IconButton(onClick = { /* TODO nuovo progetto */ }) { Icon(Icons.Filled.Add, contentDescription = "Nuovo progetto") }
            }
        }
    }

    // -------- Barra CONTESTUALE (sopra la principale)
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .offset { IntOffset(0, -84) } // poco sopra la barra principale
    ) {
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ElevatedFilterChip(
                selected = false,
                onClick = { /* TODO: apri strumenti testo */ },
                label = { Text("Testo") },
                leadingIcon = { Icon(Icons.Filled.Article, contentDescription = null) }
            )
            ElevatedFilterChip(
                selected = false,
                onClick = { /* TODO: apri strumenti contenitore */ },
                label = { Text("Contenitore") },
                leadingIcon = { Icon(Icons.Filled.Widgets, contentDescription = null) }
            )
            ElevatedFilterChip(
                selected = false,
                onClick = { /* TODO: aggiungi elemento */ },
                label = { Text("Aggiungi") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
            )
        }
    }
}

/** micro‑pill per raggruppare pulsanti con look compatto */
@Composable
private fun GroupPill(content: @Composable RowScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            content = content
        )
    }
}

/** puntino separatore */
@Composable
private fun DividerDot() {
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .size(6.dp)
            .background(
                brush = Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.0f)
                    )
                ),
                shape = CircleShape
            )
    )
}

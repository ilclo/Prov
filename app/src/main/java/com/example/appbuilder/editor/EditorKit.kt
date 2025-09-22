package com.example.appbuilder.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Evita conflitto con kotlin.collections.List
import androidx.compose.material.icons.filled.List as ListIcon

/**
 * DEMO ENTRY — schermo bianco con sole barre dei menu.
 * Da MainActivity: setContent { MaterialTheme { EditorMenuOnlyDemo() } }
 */
@Composable
fun EditorMenuOnlyDemo() {
    // Tema di pagina bianco (sfondo non interattivo)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        EditorMenuScaffold()
    }
}

/**
 * Contiene:
 * - QuickInsertBar (sopra)
 * - SecondaryBar (OK / Salva Progetto / Apri / Nuovo)
 * - PrimaryBar (Undo/Redo/Save/Delete/Dup/Props/Layout/Create/List)
 *
 * Le barre stanno SEMPRE visibili, 2–3 cm sopra il bordo,
 * e “si adagiano” sulla tastiera (imePadding).
 */
@Composable
private fun BoxScope.EditorMenuScaffold() {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .imePadding()                     // si solleva sopra la tastiera
            .navigationBarsPadding()          // rispetta gesture bar
            .padding(horizontal = 12.dp)
            .padding(bottom = 24.dp),         // “paio di cm” sopra il bordo
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickInsertBar()

        SecondaryBottomBar()

        PrimaryBottomBar()
    }
}

/* -----------------------------
 *  Quick bar: Testo / Contenitore / +
 * ----------------------------- */

@Composable
private fun QuickInsertBar() {
    Surface(
        color = githubPanelAlt,
        contentColor = githubOnPanel,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(14.dp)
    ) {
        val scroll = rememberScrollState()
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { /* no-op: solo estetica */ },
                leadingIcon = { Icon(Icons.Filled.TextFields, contentDescription = null) },
                label = { Text("Testo") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = githubPanel,
                    labelColor = githubOnPanel
                )
            )
            AssistChip(
                onClick = { /* no-op */ },
                leadingIcon = { Icon(Icons.Filled.Widgets, contentDescription = null) },
                label = { Text("Contenitore") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = githubPanel,
                    labelColor = githubOnPanel
                )
            )
            AssistChip(
                onClick = { /* no-op */ },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                label = { Text("Aggiungi") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = githubPanel,
                    labelColor = githubOnPanel
                )
            )
        }
    }
}

/* -----------------------------
 *  Barra primaria — comandi base
 * ----------------------------- */

@Composable
private fun PrimaryBottomBar() {
    Surface(
        color = githubPanel,
        contentColor = githubOnPanel,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        val scroll = rememberScrollState()

        // Stati per tendine "Crea" e "Lista"
        var showCreate by remember { mutableStateOf(false) }
        var showList by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Undo / Redo
            IconBtn(Icons.Filled.Undo, "Indietro")
            IconBtn(Icons.Filled.Redo, "Avanti")

            DividerDot()

            // Save pagina (dischetto)
            IconBtn(Icons.Filled.Save, "Salva pagina")

            // Elimina / Duplica
            IconBtn(Icons.Filled.Delete, "Elimina")
            IconBtn(Icons.Filled.ContentCopy, "Duplica")

            DividerDot()

            // Proprietà (contenitore/contenuto selezionato) — placeholder
            IconBtn(Icons.Filled.Settings, "Proprietà")

            // Layout pagina
            IconBtn(Icons.Filled.DashboardCustomize, "Layout pagina")

            DividerDot()

            // Crea… (tendina)
            Box {
                IconBtn(Icons.Filled.AddCircle, "Crea") { showCreate = true }
                DropdownMenu(expanded = showCreate, onDismissRequest = { showCreate = false }) {
                    DropdownMenuItem(
                        text = { Text("Nuova pagina") },
                        onClick = { showCreate = false },
                        leadingIcon = { Icon(Icons.Filled.Description, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Nuovo avviso") },
                        onClick = { showCreate = false },
                        leadingIcon = { Icon(Icons.Filled.Campaign, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Nuovo menù laterale") },
                        onClick = { showCreate = false },
                        leadingIcon = { Icon(Icons.Filled.ViewSidebar, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Nuovo menù centrale") },
                        onClick = { showCreate = false },
                        leadingIcon = { Icon(Icons.Filled.ViewCompact, null) }
                    )
                }
            }

            // Lista elementi creati (tendina)
            Box {
                IconBtn(Icons.Filled.ListIcon, "Lista") { showList = true }
                DropdownMenu(expanded = showList, onDismissRequest = { showList = false }) {
                    DropdownMenuItem(
                        text = { Text("Pagine") },
                        onClick = { showList = false },
                        leadingIcon = { Icon(Icons.Filled.Description, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Avvisi") },
                        onClick = { showList = false },
                        leadingIcon = { Icon(Icons.Filled.Campaign, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Menù laterali") },
                        onClick = { showList = false },
                        leadingIcon = { Icon(Icons.Filled.ViewSidebar, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Menù centrali") },
                        onClick = { showList = false },
                        leadingIcon = { Icon(Icons.Filled.ViewCompact, null) }
                    )
                }
            }
        }
    }
}

/* -----------------------------
 *  Barra secondaria — OK e progetto
 * ----------------------------- */

@Composable
private fun SecondaryBottomBar() {
    Surface(
        color = githubPanelAlt,
        contentColor = githubOnPanel,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(14.dp)
    ) {
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // OK / Annulla della sessione proprietà corrente
            Button(
                onClick = { /* no-op */ },
                colors = ButtonDefaults.buttonColors(containerColor = githubGreen, contentColor = Color.White)
            ) {
                Icon(Icons.Filled.Check, null); Spacer(Modifier.width(6.dp)); Text("OK")
            }
            TextButton(
                onClick = { /* no-op */ },
                colors = ButtonDefaults.textButtonColors(contentColor = githubOnPanel)
            ) {
                Icon(Icons.Filled.Close, null); Spacer(Modifier.width(6.dp)); Text("Annulla")
            }

            Spacer(Modifier.width(12.dp))
            DividerDot()

            // Azioni progetto
            OutlinedButton(
                onClick = { /* no-op */ },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = githubOnPanel)
            ) { Icon(Icons.Filled.SaveAs, null); Spacer(Modifier.width(6.dp)); Text("Salva progetto") }

            OutlinedButton(
                onClick = { /* no-op */ },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = githubOnPanel)
            ) { Icon(Icons.Filled.FolderOpen, null); Spacer(Modifier.width(6.dp)); Text("Apri / Importa") }

            OutlinedButton(
                onClick = { /* no-op */ },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = githubOnPanel)
            ) { Icon(Icons.Filled.NoteAdd, null); Spacer(Modifier.width(6.dp)); Text("Nuovo progetto") }
        }
    }
}

/* -----------------------------
 *  Piccoli building-block
 * ----------------------------- */

@Composable
private fun RowScope.DividerDot() {
    Box(
        Modifier
            .padding(horizontal = 6.dp)
            .size(width = 1.dp, height = 22.dp)
            .background(githubDivider, RoundedCornerShape(1.dp))
    )
}

@Composable
private fun IconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tooltip: String,
    onClick: () -> Unit = {}
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = githubOnPanel)
    ) {
        Icon(icon, contentDescription = tooltip)
    }
}

/* -----------------------------
 *  Palette scura “tipo GitHub”
 * ----------------------------- */

private val githubPanel     = Color(0xFF161B22)  // scuro principale
private val githubPanelAlt  = Color(0xFF1E2630)  // scuro secondario
private val githubOnPanel   = Color(0xFFADBAC7)  // testo/icon
private val githubDivider   = Color(0xFF30363D)  // separatori
private val githubGreen     = Color(0xFF238636)  // OK

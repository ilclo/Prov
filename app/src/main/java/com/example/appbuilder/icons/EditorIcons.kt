package com.example.appbuilder.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

/**
 * Pacchetto icone centralizzato per l'editor.
 * Mappa nomi semantici → Material Icons, così in futuro puoi cambiare stile una volta sola.
 */
object EditorIcons {

    /* — Azioni generali / global — */
    val Undo: ImageVector       = Icons.Filled.Undo
    val Redo: ImageVector       = Icons.Filled.Redo
    val Save: ImageVector       = Icons.Filled.Save
    val Delete: ImageVector     = Icons.Filled.Delete
    val Duplicate: ImageVector  = Icons.Filled.ContentCopy
    val Settings: ImageVector   = Icons.Filled.Settings
    val Ok: ImageVector         = Icons.Filled.Check
    val Cancel: ImageVector     = Icons.Filled.Close
    val Back: ImageVector       = Icons.Filled.ArrowBack

    /* — Categorie di menù (barra superiore) — */
    val Layout: ImageVector     = Icons.Filled.Tune
    val Container: ImageVector  = Icons.Filled.Widgets    // sostituisce BorderColor
    val Text: ImageVector       = Icons.Filled.TextFields
    val Image: ImageVector      = Icons.Filled.Image
    val Insert: ImageVector     = Icons.Filled.Add

    /* — Opzioni layout/immagini — */
    val Color: ImageVector      = Icons.Filled.ColorLens
    val Crop: ImageVector       = Icons.Filled.Crop
    val Album: ImageVector      = Icons.Filled.Collections
}

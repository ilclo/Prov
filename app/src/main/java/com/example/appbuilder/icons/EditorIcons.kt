package com.example.appbuilder.editor.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Pacchetto icone per l’editor.
 * Mappate su Material Icons per evitare dipendenze extra.
 */
object EditorIcons {
    // Macro categorie
    val Page: ImageVector = Icons.Filled.Article          // Layout
    val Text: ImageVector = Icons.Filled.TextFields       // Testo
    val Container: ImageVector = Icons.Filled.Widgets     // Contenitore (quadrato + cerchio, concetto)
    val Image: ImageVector = Icons.Filled.Image           // Immagine
    val Add: ImageVector = Icons.Filled.Add               // Inserisci

    // Testo
    val Underline: ImageVector = Icons.Filled.FormatUnderlined   // "T" sottolineata
    val Italic: ImageVector = Icons.Filled.FormatItalic          // "T" corsivo
    val Highlight: ImageVector = Icons.Filled.BorderColor        // tratto evidenziatore
    val Font: ImageVector = Icons.Filled.FontDownload            // "F" font
    val Weight: ImageVector = Icons.Filled.FormatBold            // grassetto (G spessa)
    val Size: ImageVector = Icons.Filled.FormatSize              // T piccola + T grande
    val TextColor: ImageVector = Icons.Filled.Brush              // pennello

    // Layout
    val Gradient: ImageVector = Icons.Filled.SwapVert            // freccia su/giù
    val FX: ImageVector = Icons.Filled.AutoAwesome               // "FX" concettuale

    // Contenitore (per futuri sottomenu)
    val Palette1: ImageVector = Icons.Filled.Palette             // Colore 1
    val Palette2: ImageVector = Icons.Filled.Palette             // Colore 2
    val ShapeSquare: ImageVector = Icons.Filled.CropSquare       // Shape quadrato vuoto
    val ThickBorder: ImageVector = Icons.Filled.CropDin          // bordo spesso
    val Variant: ImageVector = Icons.Filled.Style                // variante (S stilizzata) – fallback generico

    // Inserisci
    val Toggle: ImageVector = Icons.Filled.ToggleOn
    val Slider: ImageVector = Icons.Filled.LinearScale
}

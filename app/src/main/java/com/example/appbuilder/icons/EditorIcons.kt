package com.example.appbuilder.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object EditorIcons {
    // Barre / Navigazione
    val Page: ImageVector = Icons.Filled.Article

    // Testo
    val TextUnderline: ImageVector = Icons.Filled.FormatUnderlined
    val TextItalic: ImageVector     = Icons.Filled.FormatItalic
    val TextHighlight: ImageVector  = Icons.Filled.BorderTop     // “tratto” orizzontale spesso (appross.)
    val Font: ImageVector           = Icons.Filled.FontDownload
    val Weight: ImageVector         = Icons.Filled.FormatBold
    val Size: ImageVector           = Icons.Filled.FormatSize
    val TextColor: ImageVector      = Icons.Filled.Brush          // colore testo (nuova opzione)

    // Contenitore
    val Container: ImageVector      = Icons.Filled.Widgets        // “quadrato + cerchio” (appross.)
    val Palette1: ImageVector       = Icons.Filled.ColorLens      // “tavolozza + 1” (appross.)
    val Palette2: ImageVector       = Icons.Filled.ColorLens      // “tavolozza + 2” (appross.)
    val ShapeSquare: ImageVector    = Icons.Filled.CropSquare     // quadrato vuoto
    val Variant: ImageVector        = Icons.Filled.TextFields     // “S corsiva” (appross.)
    val BorderThick: ImageVector    = Icons.Filled.BorderStyle    // bordo spesso (appross.)
    val Type: ImageVector           = Icons.Filled.TextFields     // “Ty corsivo” (appross.)

    // Layout
    val Gradient: ImageVector       = Icons.Filled.SwapVert       // frecce su/giù
    val Fx: ImageVector             = Icons.Filled.Tune           // FX (appross.)

    // Aggiungi
    val IconMenu: ImageVector       = Icons.Filled.SwapVert       // freccia su/giù (appross.) + pennello richiesto
    val Toggle: ImageVector         = Icons.Filled.ToggleOn
    val Slider: ImageVector         = Icons.Filled.LinearScale
}

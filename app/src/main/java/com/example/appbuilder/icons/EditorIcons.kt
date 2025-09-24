package com.example.appbuilder.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.* // per CrueltyFree

/**
 * Pacchetto icone centralizzato per l'editor.
 * Mappa nomi semantici → Material Icons (Compose, Android).
 * Dove il nome richiesto non esiste in Android, usiamo un fallback vicino (commentato).
 */
object EditorIcons {

    /* — Azioni generali — */
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
    // testo: title
    val Text: ImageVector       = Icons.Filled.Title
    // contenitore: add_box
    val Container: ImageVector  = Icons.Filled.AddBox
    // layout: news → fallback: Article
    val Layout: ImageVector     = Icons.Filled.Article

    // generici
    val Image: ImageVector      = Icons.Filled.Image
    val Insert: ImageVector     = Icons.Filled.Add

    /* — Testo (menù) — */
    // Sottolinea: format_color_text
    val Underline: ImageVector         = Icons.Filled.FormatColorText
    // Corsivo: format_italic
    val Italic: ImageVector            = Icons.Filled.FormatItalic
    // Evidenzia: format_ink_highlighter → fallback: Highlight
    val Highlight: ImageVector         = Icons.Filled.Highlight
    // Font: custom_typography → fallback: FontDownload
    val CustomTypography: ImageVector  = Icons.Filled.FontDownload
    // Weight: format_bold
    val Bold: ImageVector              = Icons.Filled.FormatBold
    // Size: format_size
    val Size: ImageVector              = Icons.Filled.FormatSize
    // Colore (menù testo): brush
    val Brush: ImageVector             = Icons.Filled.Brush

    /* — Colore (contenitore & layout) — */
    // voce "Colore": format_paint
    val Color: ImageVector             = Icons.Filled.FormatPaint
    // "Colore 1/2": colors → fallback: Palette
    val Colors: ImageVector            = Icons.Filled.Palette
    // Gradiente: transition_fade → fallback: LinearScale
    val Gradient: ImageVector          = Icons.Filled.LinearScale
    // fx: function
    val Functions: ImageVector         = Icons.Filled.Functions

    /* — Contenitore (altre opzioni) — */
    // scroll: swipe_vertical → fallback: SwapVert
    val SwipeVertical: ImageVector     = Icons.Filled.SwapVert
    // Shape: square → fallback: CropSquare
    val Square: ImageVector            = Icons.Filled.CropSquare
    // Variant: variables → fallback: ViewModule
    val Variant: ImageVector           = Icons.Filled.ViewModule
    // b_tick: line_weight
    val LineWeight: ImageVector        = Icons.Filled.LineWeight
    // Tipo: swipe_right → fallback: SwapHoriz
    val SwipeRight: ImageVector        = Icons.Filled.SwapHoriz

    /* — Immagini — */
    // aggiungi immagine: add_photo_alternate
    val AddPhotoAlternate: ImageVector = Icons.Filled.AddPhotoAlternate
    // aggiungi album: perm_media
    val PermMedia: ImageVector         = Icons.Filled.PermMedia
    val Crop: ImageVector              = Icons.Filled.Crop

    /* — Menù "Aggiungi" — */
    // icona: cruelty_free (Outlined); alternativa fallback: Pets
    val Icon: ImageVector              = Icons.Outlined.CrueltyFree
    // toggle: toggle_on
    val Toggle: ImageVector            = Icons.Filled.ToggleOn
    // slider: switches → fallback: Tune (sliders)
    val Slider: ImageVector            = Icons.Filled.Tune
}
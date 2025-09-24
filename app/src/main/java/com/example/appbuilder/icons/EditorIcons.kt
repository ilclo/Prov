package com.example.appbuilder.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.example.appbuilder.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*

/**
 * Pacchetto icone centralizzato per l'editor (Vector Asset + fallback Icons.*).
 * Tutte le icone "nuove" (Material Symbols) arrivano da res/drawable via vectorResource.
 * In questo modo EditorKit continua a ricevere ImageVector e non va toccato.
 */
object EditorIcons {

    /* — Azioni generali (restano da Icons.*) — */
    val Undo: ImageVector       = Icons.Outlined.Undo
    val Redo: ImageVector       = Icons.Outlined.Redo
    val Save: ImageVector       = Icons.Outlined.Save
    val Delete: ImageVector     = Icons.Outlined.Delete
    val Duplicate: ImageVector  = Icons.Outlined.ContentCopy
    val Settings: ImageVector   = Icons.Outlined.Settings
    val Ok: ImageVector         = Icons.Outlined.Check
    val Cancel: ImageVector     = Icons.Outlined.Close
    val Back: ImageVector       = Icons.Outlined.ArrowBack
    val Image: ImageVector      = Icons.Outlined.Image
    val Insert: ImageVector     = Icons.Outlined.Add
    val Crop: ImageVector       = Icons.Outlined.Crop

    /* — Categorie di menù (barra superiore) — */
    val Text: ImageVector       @Composable get() = ImageVector.vectorResource(R.drawable.ic_title)       // title
    val Container: ImageVector  @Composable get() = ImageVector.vectorResource(R.drawable.ic_add_box)     // add_box
    val Layout: ImageVector     @Composable get() = ImageVector.vectorResource(R.drawable.ic_news)        // news

    /* — Testo (menù) — */
    val Underline: ImageVector         @Composable get() = ImageVector.vectorResource(R.drawable.ic_format_color_text)
    val Italic: ImageVector            @Composable get() = ImageVector.vectorResource(R.drawable.ic_format_italic)
    val Highlight: ImageVector         @Composable get() = ImageVector.vectorResource(R.drawable.ic_format_ink_highlighter)
    val CustomTypography: ImageVector  @Composable get() = ImageVector.vectorResource(R.drawable.ic_custom_typography)
    val Bold: ImageVector              @Composable get() = ImageVector.vectorResource(R.drawable.ic_format_bold)
    val Size: ImageVector              @Composable get() = ImageVector.vectorResource(R.drawable.ic_format_size)
    val Brush: ImageVector             @Composable get() = ImageVector.vectorResource(R.drawable.ic_brush) // Colore testo

    /* — Colore (contenitore & layout) — */
    val Color: ImageVector             @Composable get() = ImageVector.vectorResource(R.drawable.ic_format_paint)
    val Colors: ImageVector            @Composable get() = ImageVector.vectorResource(R.drawable.ic_colors) // 1 & 2
    val Gradient: ImageVector          @Composable get() = ImageVector.vectorResource(R.drawable.ic_transition_fade)
    val Functions: ImageVector         @Composable get() = ImageVector.vectorResource(R.drawable.ic_function)

    /* — Contenitore (altre opzioni) — */
    val SwipeVertical: ImageVector     @Composable get() = ImageVector.vectorResource(R.drawable.ic_swipe_vertical)
    val Square: ImageVector            @Composable get() = ImageVector.vectorResource(R.drawable.ic_square)
    val Variant: ImageVector           @Composable get() = ImageVector.vectorResource(R.drawable.ic_variables)
    val LineWeight: ImageVector        @Composable get() = ImageVector.vectorResource(R.drawable.ic_line_weight)
    val SwipeRight: ImageVector        @Composable get() = ImageVector.vectorResource(R.drawable.ic_swipe_right)

    /* — Immagini — */
    val AddPhotoAlternate: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_add_photo_alternate)
    val PermMedia: ImageVector         @Composable get() = ImageVector.vectorResource(R.drawable.ic_perm_media)

    /* — Menù "Aggiungi" — */
    val Icon: ImageVector              @Composable get() = ImageVector.vectorResource(R.drawable.ic_cruelty_free) // cruelty_free
    val Toggle: ImageVector            @Composable get() = ImageVector.vectorResource(R.drawable.ic_toggle_on)    // toggle_on
    val Slider: ImageVector            @Composable get() = ImageVector.vectorResource(R.drawable.ic_switches)     // switches
}

package com.example.appbuilder.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Collezione icone usate nell'editor.
 * - Le icone Material sono esposte come alias in EditorIcons.*
 * - Alcune icone semplici sono disegnate via ImageVector.Builder (es. 'Container').
 *   NB: usiamo Color.Unspecified per consentire il tint di Icon() (contentColor).
 */
object EditorIcons {

    // --- Azioni di progetto / file
    val Save: ImageVector = Icons.Filled.Save
    val Delete: ImageVector = Icons.Filled.Delete
    val Duplicate: ImageVector = Icons.Filled.ContentCopy
    val Settings: ImageVector = Icons.Filled.Tune
    val Insert: ImageVector = Icons.Filled.BookmarkAdd

    // --- Categorie / comuni
    val Layout: ImageVector = Icons.Filled.Article     // icona pagina
    val Text: ImageVector = Icons.Filled.TextFields
    val Image: ImageVector = Icons.Filled.Image
    val Album: ImageVector = Icons.Filled.Album
    val Crop: ImageVector = Icons.Filled.Crop
    val Color: ImageVector = Icons.Filled.Palette

    // --- Conferma
    val Cancel: ImageVector = Icons.Filled.Cancel
    val Ok: ImageVector = Icons.Filled.Check

    // --- Progetti
    val FolderOpen: ImageVector = Icons.Filled.FolderOpen
    val CreateNewFolder: ImageVector = Icons.Filled.CreateNewFolder

    // --- Contenitore (quadrato + cerchio) — vettore semplice
    val Container: ImageVector by lazy {
        Builder(
            name = "EditorContainer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Quadrato a sinistra (riempito; usa tint di Icon grazie a Color.Unspecified)
            path(
                fill = SolidColor(Color.Unspecified),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3f, 7f)
                lineTo(11f, 7f)
                lineTo(11f, 15f)
                lineTo(3f, 15f)
                close()
            }
            // Cerchio a destra (approssimato con 4 curve cubiche — niente addOval)
            path(
                fill = SolidColor(Color.Unspecified),
                pathFillType = PathFillType.NonZero
            ) {
                val cx = 17f
                val cy = 11f
                val r = 4f
                // Fattore di controllo per approssimare un cerchio con Bezier cubiche
                val c = 0.552284749831f * r

                moveTo(cx, cy - r)                            // top
                cubicTo(cx + c, cy - r, cx + r, cy - c, cx + r, cy)     // top-right to right
                cubicTo(cx + r, cy + c, cx + c, cy + r, cx, cy + r)     // right to bottom
                cubicTo(cx - c, cy + r, cx - r, cy + c, cx - r, cy)     // bottom to left
                cubicTo(cx - r, cy - c, cx - c, cy - r, cx, cy - r)     // left to top
                close()
            }
        }.build()
    }
}
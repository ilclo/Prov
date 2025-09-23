package com.example.appbuilder.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * EditorIcons: alias a Material Icons + poche icone vettoriali semplici.
 * Nessun uso di Color.* / addOval / cubicTo per massima compatibilità.
 */
object EditorIcons {
    // --- Azioni progetto/file
    val Save = Icons.Filled.Save
    val Delete = Icons.Filled.Delete
    val Duplicate = Icons.Filled.ContentCopy
    val Settings = Icons.Filled.Tune
    val Insert = Icons.Filled.BookmarkAdd
    val FolderOpen = Icons.Filled.FolderOpen
    val CreateNewFolder = Icons.Filled.CreateNewFolder

    // --- Categorie
    val Layout = Icons.Filled.Article  // icona pagina
    val Text = Icons.Filled.TextFields
    val Image = Icons.Filled.Image
    val Album = Icons.Filled.Album
    val Crop = Icons.Filled.Crop
    val Color = Icons.Filled.Palette

    // --- Conferma
    val Cancel = Icons.Filled.Cancel
    val Ok = Icons.Filled.Check

    // --- Testo (icone specifiche richieste)
    val TextUnderline = Icons.Filled.FormatUnderlined   // T sottolineata
    val TextItalic = Icons.Filled.FormatItalic          // T corsiva
    val HighlightStroke = Icons.Filled.Remove           // tratto orizzontale spesso
    val Font = Icons.Filled.FontDownload                // F di font
    val Weight = Icons.Filled.FormatBold                // G spessa (concettuale)
    val Size = Icons.Filled.FormatSize                  // T piccola + T grande
    val TextColor = Icons.Filled.Brush                  // pennello

    // --- Contenitore (categoria: quadrato + cerchio)
    val Container: ImageVector by lazy {
        Builder("EditorContainer", 24.dp, 24.dp, 24f, 24f).apply {
            // Quadrato sinistro
            path(pathFillType = PathFillType.NonZero) {
                moveTo(3f, 7f); lineTo(11f, 7f); lineTo(11f, 15f); lineTo(3f, 15f); close()
            }
            // Cerchio destro ~ poligono
            path(pathFillType = PathFillType.NonZero) {
                moveTo(21.000f, 11.000f)
                lineTo(20.464f, 13.000f)
                lineTo(19.000f, 14.464f)
                lineTo(17.000f, 15.000f)
                lineTo(15.000f, 14.464f)
                lineTo(13.536f, 13.000f)
                lineTo(13.000f, 11.000f)
                lineTo(13.536f, 9.000f)
                lineTo(15.000f, 7.536f)
                lineTo(17.000f, 7.000f)
                lineTo(19.000f, 7.536f)
                lineTo(20.464f, 9.000f)
                close()
            }
        }.build()
    }

    // --- Contenitore (voci)
    val Shape = Icons.Filled.CropSquare         // quadrato vuoto
    val Variant = Icons.Filled.FormatItalic     // S corsiva (concettuale)
    val Type = Icons.Filled.TextFields          // Ty corsivo (concettuale)

    // --- Layout (voci)
    val Gradient = Icons.Filled.SwapVert        // freccia su e giù

    // FX con lettere "FX" disegnate a poligoni
    val FX: ImageVector by lazy {
        Builder("FX", 24.dp, 24.dp, 24f, 24f).apply {
            // Lettera F
            path(pathFillType = PathFillType.NonZero) {
                moveTo(3f,7f); lineTo(5f,7f); lineTo(5f,17f); lineTo(3f,17f); close()
                moveTo(5f,7f); lineTo(10f,7f); lineTo(10f,9f); lineTo(5f,9f); close()
                moveTo(5f,11f); lineTo(9f,11f); lineTo(9f,13f); lineTo(5f,13f); close()
            }
            // Lettera X (due losanghe sottili)
            path(pathFillType = PathFillType.NonZero) {
                moveTo(12f,7f); lineTo(14f,7f); lineTo(21f,17f); lineTo(19f,17f); close()
            }
            path(pathFillType = PathFillType.NonZero) {
                moveTo(19f,7f); lineTo(21f,7f); lineTo(14f,17f); lineTo(12f,17f); close()
            }
        }.build()
    }

    // --- Menù icona: frecce su/giù + pennello semplice
    val IconMenu: ImageVector by lazy {
        Builder("IconMenu", 24.dp, 24.dp, 24f, 24f).apply {
            // Freccia su
            path(pathFillType = PathFillType.NonZero) {
                moveTo(4f,14f); lineTo(6f,14f); lineTo(6f,10f); lineTo(8f,10f); lineTo(5f,7f); lineTo(2f,10f); lineTo(4f,10f); close()
            }
            // Freccia giù
            path(pathFillType = PathFillType.NonZero) {
                moveTo(10f,10f); lineTo(12f,10f); lineTo(12f,14f); lineTo(14f,14f); lineTo(11f,17f); lineTo(8f,14f); lineTo(10f,14f); close()
            }
            // Pennello stilizzato (manico + punta)
            path(pathFillType = PathFillType.NonZero) {
                moveTo(16f,8f); lineTo(20f,8f); lineTo(20f,10f); lineTo(16f,10f); close()  // manico
            }
            path(pathFillType = PathFillType.NonZero) {
                moveTo(20f,10f); lineTo(22f,12f); lineTo(18f,12f); close()                 // punta
            }
        }.build()
    }
}

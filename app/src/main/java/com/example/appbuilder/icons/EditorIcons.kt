package com.example.appbuilder.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Collezione icone usate nell'editor.
 * Alcune sono alias di Material Icons, altre sono icone semplici custom.
 */
object EditorIcons {

    // --- Azioni di progetto / file
    val Save: ImageVector = Icons.Filled.Save
    val Delete: ImageVector = Icons.Filled.Delete
    val Duplicate: ImageVector = Icons.Filled.ContentCopy
    val Settings: ImageVector = Icons.Filled.Tune
    val Insert: ImageVector = Icons.Filled.BookmarkAdd
    val FolderOpen: ImageVector = Icons.Filled.FolderOpen
    val CreateNewFolder: ImageVector = Icons.Filled.CreateNewFolder

    // --- Categorie
    val Layout: ImageVector = Icons.Filled.Article     // icona pagina
    val Text: ImageVector = Icons.Filled.TextFields
    val Image: ImageVector = Icons.Filled.Image
    val Album: ImageVector = Icons.Filled.Album
    val Crop: ImageVector = Icons.Filled.Crop
    val Color: ImageVector = Icons.Filled.Palette

    // --- Conferma
    val Cancel: ImageVector = Icons.Filled.Cancel
    val Ok: ImageVector = Icons.Filled.Check

    // --- Contenitore (quadrato + cerchio)
    val Container: ImageVector by lazy {
        Builder(
            name = "EditorContainer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Quadrato pieno a sinistra
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3f, 7f)
                lineTo(11f, 7f)
                lineTo(11f, 15f)
                lineTo(3f, 15f)
                close()
            }
            // Cerchio pieno a destra
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero
            ) {
                addOval(Rect(13f, 7f, 21f, 15f))
            }
        }.build()
    }
}
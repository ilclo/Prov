
package com.example.appbuilder.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Collezione icone usate nell'editor.
 * Tutti gli alias Material sono qui; la custom 'Container' evita addOval/cubicTo/Color.Unspecified.
 */
object EditorIcons {
    // --- Azioni
    val Save = Icons.Filled.Save
    val Delete = Icons.Filled.Delete
    val Duplicate = Icons.Filled.ContentCopy
    val Settings = Icons.Filled.Tune
    val Insert = Icons.Filled.BookmarkAdd

    // --- Categorie / comuni
    val Layout = Icons.Filled.Article
    val Text = Icons.Filled.TextFields
    val Image = Icons.Filled.Image
    val Album = Icons.Filled.Album
    val Crop = Icons.Filled.Crop
    val Color = Icons.Filled.Palette

    // --- Conferma
    val Cancel = Icons.Filled.Cancel
    val Ok = Icons.Filled.Check

    // --- Progetti
    val FolderOpen = Icons.Filled.FolderOpen
    val CreateNewFolder = Icons.Filled.CreateNewFolder

    // --- Contenitore: quadrato + "cerchio" (poligono regolare a 12 lati)
    val Container: ImageVector by lazy {
        Builder(
            name = "EditorContainer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Quadrato sinistro (7..15)
            path(
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3f, 7f)
                lineTo(11f, 7f)
                lineTo(11f, 15f)
                lineTo(3f, 15f)
                close()
            }
            // Poligono destro (approssima un cerchio)
            path(
                pathFillType = PathFillType.NonZero
            ) {
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
}
package com.example.appbuilder.editor

import com.example.appbuilder.canvas.DrawItem
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import com.example.appbuilder.canvas.ToolMode
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.derivedStateOf 
import com.example.appbuilder.overlay.GridSliderOverlay
import com.example.appbuilder.overlay.LevelPickerOverlay
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import com.example.appbuilder.canvas.CanvasStage
import com.example.appbuilder.canvas.PageState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.appbuilder.icons.EditorIcons
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.vectorResource
import com.example.appbuilder.R
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.animation.core.animateDpAsState
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.width
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import kotlin.math.cos
import kotlin.math.sin
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.appbuilder.canvas.CornerRadii
import com.example.appbuilder.canvas.ImageStyle
import com.example.appbuilder.canvas.ImageFit
import com.example.appbuilder.canvas.ImageFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import android.graphics.BitmapFactory
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.BasicText

private val LocalIsFree = staticCompositionLocalOf { true }

private const val codiceprofree = 12345
private val LocalDeckItems =
    staticCompositionLocalOf<Map<DeckRoot, List<String>>> { emptyMap() }

/* ---- BARS: altezze fisse + gap ---- */
private val BOTTOM_BAR_HEIGHT = 56.dp        // barra inferiore (base)
private val BOTTOM_BAR_EXTRA = 8.dp          // extra altezza barra inferiore (stessa in Home e Submenu)
private val TOP_BAR_HEIGHT = 52.dp           // barra superiore (categorie / submenu)
private val BARS_GAP = 14.dp                 
private val SAFE_BOTTOM_MARGIN = 32.dp     // barra inferiore più alta rispetto al bordo schermo
private val LocalExitClassic = staticCompositionLocalOf<() -> Unit> { {} }
// Accento per i wizard "Crea ..."
private val WIZ_AZURE = Color(0xFF58A6FF)   // azzurro (come DECK_HIGHLIGHT)
private data class InfoModeEnv(val enabled: Boolean, val show: (String, String) -> Unit)
private val LocalInfoMode = staticCompositionLocalOf { InfoModeEnv(false) { _, _ -> } }
/* =========================================================================================
*  MODELLO MINIMO DI STATO (solo per navigazione menù)
* ========================================================================================= */
// === Color picking infra (TOP-LEVEL, fuori da composable) ===
sealed class ColorTarget {
    object Border : ColorTarget()
    data class ContainerFill(val slot: Int) : ColorTarget() // 1 o 2
    data class LayoutFill(val slot: Int) : ColorTarget()     // 1 o 2
    object Text : ColorTarget()
}

private fun androidx.compose.ui.graphics.Color.toHex(): String {
    val r = (red   * 255).toInt().coerceIn(0,255)
    val g = (green * 255).toInt().coerceIn(0,255)
    val b = (blue  * 255).toInt().coerceIn(0,255)
    return "#%02X%02X%02X".format(r, g, b)
}

data class EditorShellState(
    val isEditor: Boolean = true
)

// Handle/mode di interazione nel cropper
private enum class CropMode { NONE, TL, TR, BR, BL, MOVE }
/* =========================================================================================
*  ENTRY — schermata demo (sfondo neutro + menù)
* ========================================================================================= */

@Composable
fun EditorDemoScreen() {
    val state = remember { EditorShellState(isEditor = true) }
    EditorMenusOnly(state = state)
}

/* ==========================================================================================
*  ROOT — solo menù (nessuna azione applicata)
* ========================================================================================= */
private fun colorToHex(c: Color): String {
    val argb = c.toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr  8) and 0xFF
    val b = (argb       ) and 0xFF
    return String.format("#%02X%02X%02X", r, g, b)
}
private fun hexToColor(s: String?): Color? {
    if (s == null) return null
    val t = s.trim().removePrefix("#")
    val v = t.toLongOrNull(16) ?: return null
    return when (t.length) {
        6 -> {
            val r = ((v shr 16) and 0xFF).toInt() / 255f
            val g = ((v shr 8)  and 0xFF).toInt() / 255f
            val b = ( v         and 0xFF).toInt() / 255f
            Color(r, g, b, 1f)
        }
        8 -> {
            val a = ((v shr 24) and 0xFF).toInt() / 255f
            val r = ((v shr 16) and 0xFF).toInt() / 255f
            val g = ((v shr 8)  and 0xFF).toInt() / 255f
            val b = ( v         and 0xFF).toInt() / 255f
            Color(r, g, b, a)
        }
        else -> null
    }
}

@Composable
private fun FilterDropdown(
    current: String?,
    onSelected: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolbarIconButton(
            icon = ImageVector.vectorResource(id = R.drawable.ic_filter),
            contentDescription = "Filtro"
        ) { open = true }

        if (!current.isNullOrBlank()) {
            // badge come nelle tue dropdown: riuso lo stesso stile
            Surface(
                color = Color(0xFF22304B),
                contentColor = Color.White,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.BottomCenter).offset { IntOffset(0, 14) }
            ) {
                Text(current, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
            }
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf("Nessuno","B/N","Seppia").forEach { name ->
                DropdownMenuItem(
                    text = {
                        // Etichetta con "sfondo dimostrativo"
                        val bg = when (name) {
                            "B/N"    -> Brush.linearGradient(listOf(Color.Black, Color.White))
                            "Seppia" -> Brush.linearGradient(listOf(Color(0xFF704214), Color(0xFFE0C9A6)))
                            else     -> Brush.linearGradient(listOf(Color(0xFF10151F), Color(0xFF1B2334)))
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(bg, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) { Text(name, color = if (name=="Seppia") Color.Black else Color.White) }
                    },
                    onClick = { onSelected(name); open = false }
                )
            }
        }
    }
}

@Composable
private fun rememberImageBitmapFromUri(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    return remember(uri) {
        if (uri == null) return@remember null
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { s ->
                BitmapFactory.decodeStream(s)?.asImageBitmap()
            }
        }.getOrNull()
    }
}


@Composable
fun ImageCropperDialog(
    visible: Boolean,
    imageUri: Uri?,
    initial: com.example.appbuilder.canvas.ImageCrop? = null,
    onDismiss: () -> Unit,
    onApply: (com.example.appbuilder.canvas.ImageCrop) -> Unit
) {
    if (!visible || imageUri == null) return

    val img = rememberImageBitmapFromUri(imageUri)
    if (img == null) {
        // fallback molto leggero
        Dialog(onDismissRequest = onDismiss) {
            Box(
                Modifier
                    .size(280.dp, 140.dp)
                    .background(Color(0xFF222222)),
                contentAlignment = Alignment.Center
            ) {
                BasicText("Impossibile aprire l'immagine", Modifier.padding(16.dp))
            }
        }
        return
    }

    val density = LocalDensity.current

    // stato UI
    var boxSize by remember { mutableStateOf(IntSize(0, 0)) }
    var mode by remember { mutableStateOf(CropMode.NONE) }
    var moveGrab by remember { mutableStateOf(Offset.Zero) }

    // crop normalizzato 0..1
    var crop by remember(imageUri) {
        mutableStateOf(initial ?: com.example.appbuilder.canvas.ImageCrop(0f, 0f, 1f, 1f))
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(Color(0xFF121212))
                .padding(12.dp)
                .widthIn(min = 280.dp, max = 480.dp)
        ) {
            // area interattiva
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp, max = 480.dp)
                    .onSizeChanged { boxSize = it }
                    .pointerInput(boxSize, img, crop) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                // calcolo bounding dell'immagine in "contain"
                                val bw = boxSize.width.toFloat()
                                val bh = boxSize.height.toFloat()
                                val iw = img.width.toFloat()
                                val ih = img.height.toFloat()
                                val scale = min(bw / iw, bh / ih)
                                val dstW = iw * scale
                                val dstH = ih * scale
                                val dx = (bw - dstW) / 2f
                                val dy = (bh - dstH) / 2f

                                val left   = dx + crop.left   * dstW
                                val top    = dy + crop.top    * dstH
                                val right  = dx + crop.right  * dstW
                                val bottom = dy + crop.bottom * dstH

                                val handles = listOf(
                                    CropMode.TL to Offset(left, top),
                                    CropMode.TR to Offset(right, top),
                                    CropMode.BR to Offset(right, bottom),
                                    CropMode.BL to Offset(left, bottom)
                                )
                                val th = with(density) { 24.dp.toPx() }
                                val nearest = handles.minByOrNull { (_, p) -> (p - pos).getDistance() }!!
                                mode = if ((nearest.second - pos).getDistance() <= th) {
                                    nearest.first
                                } else if (pos.x in left..right && pos.y in top..bottom) {
                                    moveGrab = Offset(pos.x - left, pos.y - top)
                                    CropMode.MOVE
                                } else CropMode.NONE
                            },
                            onDragEnd = { mode = CropMode.NONE },
                            onDragCancel = { mode = CropMode.NONE },
                            onDrag = { change, drag ->
                                val bw = boxSize.width.toFloat()
                                val bh = boxSize.height.toFloat()
                                val iw = img.width.toFloat()
                                val ih = img.height.toFloat()
                                val scale = min(bw / iw, bh / ih)
                                val dstW = iw * scale
                                val dstH = ih * scale
                                val dx = (bw - dstW) / 2f
                                val dy = (bh - dstH) / 2f

                                var left   = dx + crop.left   * dstW
                                var top    = dy + crop.top    * dstH
                                var right  = dx + crop.right  * dstW
                                var bottom = dy + crop.bottom * dstH

                                when (mode) {
                                    CropMode.TL -> { left += drag.x; top += drag.y }
                                    CropMode.TR -> { right += drag.x; top += drag.y }
                                    CropMode.BR -> { right += drag.x; bottom += drag.y }
                                    CropMode.BL -> { left += drag.x; bottom += drag.y }
                                    CropMode.MOVE -> {
                                        val w = right - left
                                        val h = bottom - top
                                        var nl = change.position.x - moveGrab.x
                                        var nt = change.position.y - moveGrab.y
                                        nl = nl.coerceIn(dx, dx + dstW - w)
                                        nt = nt.coerceIn(dy, dy + dstH - h)
                                        left = nl
                                        top = nt
                                        right = nl + w
                                        bottom = nt + h
                                    }
                                    else -> Unit
                                }

                                // limiti + dimensione minima
                                val minSide = with(density) { 40.dp.toPx() }
                                left   = left.coerceIn(dx, right - minSide)
                                top    = top.coerceIn(dy, bottom - minSide)
                                right  = right.coerceIn(left + minSide, dx + dstW)
                                bottom = bottom.coerceIn(top + minSide, dy + dstH)

                                // aggiorna crop normalizzato
                                crop = com.example.appbuilder.canvas.ImageCrop(
                                    left   = ((left   - dx) / dstW).coerceIn(0f, 1f),
                                    top    = ((top    - dy) / dstH).coerceIn(0f, 1f),
                                    right  = ((right  - dx) / dstW).coerceIn(0f, 1f),
                                    bottom = ((bottom - dy) / dstH).coerceIn(0f, 1f)
                                )
                            }
                        )
                    }
            ) {
                // Disegno immagine + overlay
                Canvas(Modifier.matchParentSize()) {
                    val bw = size.width
                    val bh = size.height
                    val iw = img.width.toFloat()
                    val ih = img.height.toFloat()
                    val scale = min(bw / iw, bh / ih)
                    val dstW = iw * scale
                    val dstH = ih * scale
                    val dx = (bw - dstW) / 2f
                    val dy = (bh - dstH) / 2f

                    // immagine
                    drawImage(
                        image = img,
                        dstOffset = IntOffset(dx.roundToInt(), dy.roundToInt()),
                        dstSize   = IntSize(dstW.roundToInt(), dstH.roundToInt()),
                        filterQuality = FilterQuality.Low
                    )

                    // rettangolo di crop
                    val left   = dx + crop.left   * dstW
                    val top    = dy + crop.top    * dstH
                    val right  = dx + crop.right  * dstW
                    val bottom = dy + crop.bottom * dstH

                    // maschera scura fuori dal crop (even‑odd)
                    val p = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(0f, 0f, bw, bh))
                        addRect(Rect(left, top, right, bottom))
                    }
                    drawPath(p, Color.Black.copy(alpha = 0.45f), style = Fill)

                    // bordo e maniglie
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        style = Stroke(width = with(density) { 2.dp.toPx() })
                    )
                    val r = with(density) { 6.dp.toPx() }
                    listOf(
                        Offset(left, top), Offset(right, top),
                        Offset(right, bottom), Offset(left, bottom)
                    ).forEach { c -> drawCircle(color = Color.White, radius = r, center = c) }
                }
            }

            Spacer(Modifier.height(12.dp))

            // pulsanti essenziali senza Material
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    Modifier
                        .padding(end = 8.dp)
                        .clickable { onDismiss() }
                        .background(Color(0xFF333333))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) { BasicText("Annulla", Modifier) }

                Box(
                    Modifier
                        .clickable { onApply(crop) }
                        .background(Color(0xFF0066CC))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) { BasicText("OK", Modifier) }
            }
        }
    }
}


// mapping basilare dei nomi già usati nei default
private fun tokenToColor(token: String?): Color? = when(token?.lowercase()?.trim()) {
    "#fff", "#ffffff", "bianco" -> Color.White
    "nero", "#000000"            -> Color.Black
    "grigio chiaro"              -> Color(0xFFEEEEEE)
    "grigio"                     -> Color(0xFF9E9E9E)
    "ciano"                      -> Color.Cyan
    else -> hexToColor(token)
}

/* ---------- AGGIUNGI ---------- */

@Composable
private fun BoxScope.CropImageOverlay(
    visible: Boolean,
    imageUri: Uri?,
    onDismiss: () -> Unit,
    onApply: (com.example.appbuilder.canvas.ImageStyle) -> Unit
) {
    if (!visible) return
    Surface(
        color = Color(0xCC0D1117),
        contentColor = Color.White,
        modifier = Modifier.fillMaxSize().zIndex(1000f)
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ritaglia immagine", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            // Qui metterai la tua UI di crop manuale (pinch/zoom/drag, griglia, ecc.)
            Box(
                Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp)
                    .background(Color(0xFF131A24), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Anteprima crop (stub)")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, WIZ_AZURE),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WIZ_AZURE),
                    modifier = Modifier.weight(1f)
                ) { Text("Annulla") }
                Button(
                    onClick = {
                        // per ora non cambiamo nulla: rimandiamo la logica di crop a quando avrai la UI
                        imageUri?.let { onApply(com.example.appbuilder.canvas.ImageStyle(uri = it)) } ?: onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WIZ_AZURE, contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) { Text("Applica") }
            }
        }
    }
}

@Composable
private fun AddLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onEnter: (String) -> Unit,
    onFreeGate: () -> Unit // ⬅️ NEW: lo passa chi chiama (EditorMenusOnly)
) {
    val isFree = LocalIsFree.current

    if (path.getOrNull(1) == null) {
        val isFree = LocalIsFree.current

        ToolbarIconButton(EditorIcons.Icon, "Icona") { onEnter("Icona") }
        ToolbarIconButton(Icons.Outlined.ToggleOn, "Toggle") { onEnter("Toggle") }

        ToolbarIconButton(
            Icons.Outlined.LinearScale, "Slider",
            locked = isFree,
            onLockedAttempt = onFreeGate
        ) { onEnter("Slider") }


        ToolbarIconButton(
            icon = ImageVector.vectorResource(id = R.drawable.ic_align_flex_end),
            contentDescription = "Divisore verticale"
        ) { onEnter("Divisore verticale") }

        ToolbarIconButton(
            icon = ImageVector.vectorResource(id = R.drawable.ic_horizontal_rule),
            contentDescription = "Divisore orizzontale"
        ) { onEnter("Divisore orizzontale") }
    } else {
        // placeholder: solo navigazione visiva
        ElevatedCard(
            modifier = Modifier.size(40.dp),
            shape = CircleShape
        ) {}
    }
}

@Composable
private fun BoxScope.ProUpsellSheet(onClose: () -> Unit) {
    androidx.compose.material3.Surface(
        color = Color(0xFF0F141E),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = BorderStroke(1.dp, WIZ_AZURE),
        tonalElevation = 12.dp,
        shadowElevation = 12.dp,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Sblocca Starter o Pro", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(
                "Starter e Pro sbloccano: preset/“default”, più font, Colore 2 e Gradiente, Immagini/Album per Layout e Contenitore, e altro.",
                color = Color(0xFF9BA3AF)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onClose,
                    border = BorderStroke(1.5.dp, WIZ_AZURE),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WIZ_AZURE),
                    modifier = Modifier.weight(1f)
                ) { Text("Chiudi") }

                androidx.compose.material3.Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = WIZ_AZURE, contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) { Text("Vedi piani") }
            }
            // Mini tabella prezzi (stub)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Starter", fontWeight = FontWeight.SemiBold)
                    Text("• Tutto ciò che serve\n• Supporto base\n", color = Color(0xFF9BA3AF))
                    Text("E' X/mese", fontWeight = FontWeight.Medium)
                }
                Column(Modifier.weight(1f)) {
                    Text("Pro", fontWeight = FontWeight.SemiBold)
                    Text("• Tutto di Starter\n• Funzioni extra (stub)\n", color = Color(0xFF9BA3AF))
                    Text("E' Y/mese", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ===== DECK: tipi & CompositionLocal =====
private enum class DeckRoot { PAGINA, MENU_LATERALE, MENU_CENTRALE, AVVISO }
private enum class SecondBarMode { Deck, Classic }

private data class DeckState(val openKey: String?, val toggle: (String) -> Unit)

private data class DeckController(
    val openChild: (DeckRoot) -> Unit,
    val openWizard: (DeckRoot) -> Unit
)

// Locals per pilotare MainMenuBar senza cambiare la sua firma
private val LocalSecondBarMode = compositionLocalOf { SecondBarMode.Deck }
private val LocalDeckState = compositionLocalOf { DeckState(null) { _ -> } }

private val LocalDeckController = compositionLocalOf {
    DeckController(openChild = { _ -> }, openWizard = { _ -> })
}
private val LocalIsPageContext = compositionLocalOf { false }



// ===== Token colore (variabili facili da cambiare) =====
private val DECK_HIGHLIGHT = Color(0xFF58A6FF) // bordo icona madre quando cluster aperto
private val DECK_BADGE_BG  = Color(0xFF22304B) // stendardetto ID figlio (bg)
private val DECK_BADGE_TXT = Color.White       // stendardetto ID figlio (testo)

@Composable
fun EditorMenusOnly(
    state: EditorShellState
) {
    var deckOpen by remember { mutableStateOf<String?>(null) }        // "pagina"|"menuL"|"menuC"|"avviso"|null
    var editingClass by remember { mutableStateOf<DeckRoot?>(null) }   // classe della figlia aperta (per flag Layout)
// Path del menù (es. ["Contenitore", "Bordi", "Spessore"])
    var menuPath by remember { mutableStateOf<List<String>>(emptyList()) }
// Selezioni effimere dei dropdown/toggle (key = pathTestuale)
    val menuSelections = remember { mutableStateMapOf<String, Any?>() }
// Modifiche in corso (serve per mostrare la barra di conferma alla risalita)
    var dirty by remember { mutableStateOf(false) }
    // stile riempimento (per non toccare RectItem)
    val rectFillStyles = remember { 
        mutableStateMapOf<DrawItem.RectItem, com.example.appbuilder.canvas.FillStyle>() 
    }
    // Stili aggiuntivi per RectItem
    val rectVariants = remember { mutableStateMapOf<DrawItem.RectItem, com.example.appbuilder.canvas.Variant>() }
    val rectShapes   = remember { mutableStateMapOf<DrawItem.RectItem, com.example.appbuilder.canvas.ShapeKind>() }
    val rectCorners  = remember { mutableStateMapOf<DrawItem.RectItem, com.example.appbuilder.canvas.CornerRadii>() }
    val rectFx       = remember { mutableStateMapOf<DrawItem.RectItem, com.example.appbuilder.canvas.FxKind>() }
    // --- STATO USATO DAL PICKER: deve stare PRIMA del launcher ---
    var lastChanged by remember { mutableStateOf<String?>(null) }
    var cropOverlayVisible by remember { mutableStateOf(false) }
    var cropTargetRect by remember { mutableStateOf<DrawItem.RectItem?>(null) }

    // Rettangolo correntemente selezionato nel menù “Contenitore”
    var selectedRect by remember { mutableStateOf<DrawItem.RectItem?>(null) }

    // Mappa stile immagine per rettangolo
    val rectImages: SnapshotStateMap<DrawItem.RectItem, ImageStyle> = remember { mutableStateMapOf() }

    // Dialog “cancella immagine”
    var showDeleteImageDialog by remember { mutableStateOf(false) }

    // Stato cropper
    var cropperVisible by remember { mutableStateOf(false) }
    var cropperImageUri by remember { mutableStateOf<Uri?>(null) }
    var cropperTarget by remember { mutableStateOf<DrawItem.RectItem?>(null) }

    // File picker per “Aggiungi foto”
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && selectedRect != null) {
            cropperImageUri = uri
            cropperTarget   = selectedRect
            cropperVisible  = true
        }
    }

    ImageCropperDialog(
        visible = cropperVisible,
        imageUri = cropperImageUri,
        initial = rectImages[cropperTarget]?.crop,   // ora compila
        onDismiss = { cropperVisible = false },
        onApply = { normCrop ->
            val rect = cropperTarget ?: return@ImageCropperDialog
            val prev = rectImages[rect]
            rectImages[rect] = (prev ?: ImageStyle(uri = cropperImageUri!!)).copy(
                crop = normCrop,
                fit = ImageFit.Cover,
                filter = prev?.filter ?: ImageFilter.None
            )
            cropperVisible = false
        }
    )

    // overlay palette colore
    var colorPickerVisible by remember { mutableStateOf(false) }
    var colorTarget by remember { mutableStateOf<ColorTarget?>(null) }
    var pickerOffset by remember { mutableStateOf(IntOffset(0, 0)) }

// Conferma all'uscita dai sottomenu verso la home
    var showConfirm by remember { mutableStateOf(false) }
    var classicEditing by remember { mutableStateOf(false) } // false = Deck, true = Classic (vecchia root)
// MODE della seconda barra: "deck" (icone madre + cluster) oppure "classic" (vecchia root)
// Preset salvati (nomi da mostrare nelle tendine)
// Wizard di creazione
    var wizardVisible by remember { mutableStateOf(false) }
    var wizardKind by remember { mutableStateOf<DeckRoot?>(null) }
    var homePageId by remember { mutableStateOf<String?>(null) }
    var wizardTarget  by remember { mutableStateOf<DeckRoot?>(null) }
// ---- Stato per modalità  info + pannelli descrittivi ----
    var infoDeckOpen by remember { mutableStateOf(false) }      // deck laterale destro aperto/chiuso
    var infoMode by remember { mutableStateOf(false) }          // flag modalità  info
    var infoCard by remember { mutableStateOf<Pair<String, String>?>(null) } // (titolo, testo)
    var infoCardVisible by remember { mutableStateOf(false) }
    // === Stato palette colori flottante ===
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickTarget by remember { mutableStateOf<ColorTarget?>(null) }
    var colorPickInitial by remember { mutableStateOf(Color.Black) }    
    // ====== STATO CANVAS/OVERLAY ======
    var pageState by remember { mutableStateOf<PageState?>(null) }
    fun dpToKey(dp: Dp) = "${dp.value.toInt()}dp"
    fun keyToDp(s: String): androidx.compose.ui.unit.Dp {
        val n = s.trim().lowercase().removeSuffix("dp").toFloatOrNull() ?: 1f
        return n.dp
    }
    fun applyContainerMenuFromRect(rect: DrawItem.RectItem) {
        // spessore (già esistente)
        menuSelections[(listOf("Contenitore") + "b_thick").joinToString(" / ")] = dpToKey(rect.borderWidth)

        // variant
        val v = when (rectVariants[rect] ?: com.example.appbuilder.canvas.Variant.Full) {
            com.example.appbuilder.canvas.Variant.Full      -> "Full"
            com.example.appbuilder.canvas.Variant.Outlined  -> "Outlined"
            com.example.appbuilder.canvas.Variant.Text      -> "Text"
            com.example.appbuilder.canvas.Variant.TopBottom -> "TopBottom"
        }
        menuSelections[(listOf("Contenitore") + "variant").joinToString(" / ")] = v

        // shape
        val s = when (rectShapes[rect] ?: com.example.appbuilder.canvas.ShapeKind.Rect) {
            com.example.appbuilder.canvas.ShapeKind.Rect    -> "Rettangolo"
            com.example.appbuilder.canvas.ShapeKind.Circle  -> "Cerchio"
            com.example.appbuilder.canvas.ShapeKind.Pill    -> "Pillola"
            com.example.appbuilder.canvas.ShapeKind.Diamond -> "Diamante"
        }
        menuSelections[(listOf("Contenitore") + "shape").joinToString(" / ")] = s

        // angoli
        val cr = rectCorners[rect] ?: com.example.appbuilder.canvas.CornerRadii()
        menuSelections["Contenitore / ic_as"] = dpToKey(cr.tl)
        menuSelections["Contenitore / ic_ad"] = dpToKey(cr.tr)
        menuSelections["Contenitore / ic_bs"] = dpToKey(cr.bl)
        menuSelections["Contenitore / ic_bd"] = dpToKey(cr.br)

        // colore corpo (col1) / col2 / grad (rimasti come nelle tue versioni)
        val col1 = rectFillStyles[rect]?.col1 ?: rect.fillColor
        menuSelections[(listOf("Contenitore","Colore") + "col1").joinToString(" / ")] = colorToHex(col1)
        rectFillStyles[rect]?.col2?.let { c2 ->
            menuSelections[(listOf("Contenitore","Colore") + "col2").joinToString(" / ")] = colorToHex(c2)
        }
        menuSelections[(listOf("Contenitore","Colore") + "grad").joinToString(" / ")] =
            when (rectFillStyles[rect]?.dir ?: com.example.appbuilder.canvas.GradientDir.Monocolore) {
                com.example.appbuilder.canvas.GradientDir.Monocolore -> "Monocolore"
                com.example.appbuilder.canvas.GradientDir.Orizzontale -> "Orizzontale"
                com.example.appbuilder.canvas.GradientDir.Verticale   -> "Verticale"
                com.example.appbuilder.canvas.GradientDir.DiagTL_BR   -> "Diag TL→BR"
                com.example.appbuilder.canvas.GradientDir.DiagTR_BL   -> "Diag TR→BL"
            }

        // FX
        val fx = when (rectFx[rect] ?: com.example.appbuilder.canvas.FxKind.None) {
            com.example.appbuilder.canvas.FxKind.None       -> "Nessuno"
            com.example.appbuilder.canvas.FxKind.Vignette   -> "Vignettatura"
            com.example.appbuilder.canvas.FxKind.Noise      -> "Noise"
            com.example.appbuilder.canvas.FxKind.Stripes    -> "Strisce"
        }
        menuSelections[(listOf("Contenitore","Colore") + "fx").joinToString(" / ")] = fx
    }


    
    // Griglia
    var gridPanelOpen by remember { mutableStateOf(false) }
    var gridIsDragging by remember { mutableStateOf(false) }
    var showGridLines by remember { mutableStateOf(false) }
    
    // Livelli
    var levelPanelOpen by remember { mutableStateOf(false) }
    var currentLevel by remember { mutableStateOf(0) }
    var toolMode by remember { mutableStateOf(ToolMode.Create) }
        
    // Auto‑show griglia completa dopo 500ms se lo slider non è in drag
    LaunchedEffect(gridPanelOpen, gridIsDragging) {
        if (gridPanelOpen && !gridIsDragging) {
            kotlinx.coroutines.delay(500)
            showGridLines = true
        } else {
            showGridLines = false
        }
    }
    val canCreateContainer by remember(
        classicEditing, editingClass, menuPath,
        infoMode, wizardVisible, gridPanelOpen, levelPanelOpen
    ) {
        derivedStateOf {
            classicEditing &&
            editingClass == DeckRoot.PAGINA &&
            menuPath.firstOrNull() == "Contenitore" &&   // ← basta essere “dentro Contenitore”
            !infoMode && !wizardVisible &&
            !gridPanelOpen && !levelPanelOpen
        }
    }
    val isContainerContext by remember(classicEditing, editingClass, menuPath) {
        derivedStateOf {
            classicEditing &&
            editingClass == DeckRoot.PAGINA &&
            menuPath.firstOrNull() == "Contenitore"
        }
    }

// Auto-hide del pannello descrittivo (5s)
    LaunchedEffect(infoCard) {
        if (infoCard != null) {
            infoCardVisible = true
            delay(5000)
            infoCardVisible = false
            infoCard = null
        }
    }

// Mostra il "benvenuto" alla modalità  info quando si attiva
    LaunchedEffect(infoMode) {
        if (infoMode) {
            infoCard = "Modalità  info" to
                    "Tocca un'icona per una descrizione. Tieni premuto per entrare dove consentito. " +
                    "Per uscire, riapri il deck a destra e tocca di nuovo '?'."
        }
    }
    LaunchedEffect(isContainerContext) {
        if (isContainerContext) {
            infoDeckOpen = true
            toolMode = ToolMode.Create
        } else {
            // appena esci dal menù "Contenitore" nessun selezionato resta evidenziato
            selectedRect = null
        }
    }

    LaunchedEffect(menuPath, selectedRect) {
        if (selectedRect == null) return@LaunchedEffect
        // Bordi → Colore (bordo singolo colore)
        if (menuPath.size >= 3 &&
            menuPath.firstOrNull() == "Contenitore" &&
            menuPath.contains("Bordi") &&
            menuPath.last() == "Colore"
        ) {
            colorTarget = ColorTarget.Border
            colorPickerVisible = true
        }

        // Contenitore → Colore → col1 / col2
        if (menuPath.size >= 3 &&
            menuPath[0] == "Contenitore" && menuPath[1] == "Colore"
        ) {
            when (menuPath.last()) {
                "col1" -> { colorTarget = ColorTarget.ContainerFill(1); colorPickerVisible = true }
                "col2" -> { colorTarget = ColorTarget.ContainerFill(2); colorPickerVisible = true }
            }
        }
        // (Opzionale) Layout → Colore → col1/col2: se/quando vorrai usarlo per lo sfondo pagina
    }

    val deckItems = remember {
        mutableStateMapOf(
            DeckRoot.PAGINA        to mutableListOf("pg001"),
            DeckRoot.MENU_LATERALE to mutableListOf("ml001"),
            DeckRoot.MENU_CENTRALE to mutableListOf("mc001"),
            DeckRoot.AVVISO        to mutableListOf("al001")
        )
    }

    fun openWizardFor(root: DeckRoot) {
        wizardTarget = root
        wizardVisible = true
    }
    BackHandler(enabled = wizardVisible) {
        wizardVisible = false
    }

// Back: se il wizard è aperto, il tasto indietro chiude il wizard (non l'app)
    BackHandler(enabled = wizardVisible) { wizardVisible = false }
    val savedPresets = remember {
        mutableStateMapOf(
            "Layout" to mutableListOf("Nessuno", "Default chiaro", "Default scuro"),
            "Contenitore" to mutableListOf("Nessuno", "Card base", "Hero"),
            "Testo" to mutableListOf("Nessuno", "Titolo", "Sottotitolo", "Body")
        )
    }

// Valori dei preset/stili: root -> (nome -> mappa configurazioni)
    val presetValues = remember {
        mutableStateMapOf<String, MutableMap<String, Map<String, Any?>>>(
            "Layout" to mutableMapOf(),
            "Contenitore" to mutableMapOf(),
            "Testo" to mutableMapOf()
        )
    }
    BackHandler(enabled = menuPath.isNotEmpty()) {
        if (menuPath.size == 1 && dirty) {
            showConfirm = true
        } else {
            menuPath = menuPath.dropLast(1)
            lastChanged = null
        }
    }

    BackHandler(enabled = menuPath.isEmpty()) {
        when {
            classicEditing -> classicEditing = false   // esci dalla Classic: torni alle icone madre
            deckOpen != null -> deckOpen = null        // chiudi il cluster aperto
            else -> Unit                                // ignora: evita che l'Activity si chiuda
        }
    }
    BackHandler(enabled = gridPanelOpen)  { gridPanelOpen = false }
    BackHandler(enabled = levelPanelOpen) { levelPanelOpen = false }

    // Elenco chiavi COMPLETE + default per ciascun root (usiamo le stesse label del menu)
    fun keysForRoot(root: String): List<Pair<String, Any?>> {
        fun k(vararg segs: String) = segs.joinToString(" / ")
        return when (root) {
            "Testo" -> listOf(
                k("Testo","Sottolinea") to false,
                k("Testo","Corsivo") to false,
                k("Testo","Evidenzia") to "Nessuna",
                k("Testo","Font") to "System",
                k("Testo","Weight") to "Regular",
                k("Testo","Size") to "16sp",
                k("Testo","Colore") to "Nero",
            )
            "Contenitore" -> listOf(
                k("Contenitore","scroll") to "Assente",
                k("Contenitore","shape") to "Rettangolo",
                k("Contenitore","variant") to "Full",
                k("Contenitore","b_thick") to "1dp",
                k("Contenitore","tipo") to "Normale",
                k("Contenitore","Colore","col1") to "Bianco",
                k("Contenitore","Colore","col2") to "Grigio chiaro",
                k("Contenitore","Colore","grad") to "Monocolore",
                k("Contenitore","Colore","fx") to "Vignettatura",
                k("Contenitore","Aggiungi foto","crop") to "Nessuno",
                k("Contenitore","Aggiungi foto","frame") to "Sottile",
                k("Contenitore","Aggiungi foto","filtro") to "Nessuno",
                k("Contenitore","Aggiungi foto","fitCont") to "Cover",
                k("Contenitore","Aggiungi album","cropAlbum") to "Nessuno",
                k("Contenitore","Aggiungi album","frameAlbum") to "Sottile",
                k("Contenitore","Aggiungi album","filtroAlbum") to "Nessuno",
                k("Contenitore","Aggiungi album","fit") to "Cover",
                k("Contenitore","Aggiungi album","anim") to "Slide",
                k("Contenitore","Aggiungi album","speed") to "Media",
                k("Contenitore","ic_as") to "0dp",  // angolo alto-sinistra
                k("Contenitore","ic_ad") to "0dp",  // angolo alto-destra
                k("Contenitore","ic_bs") to "0dp",  // angolo basso-sinistra
                k("Contenitore","ic_bd") to "0dp",  // angolo basso-destra
            )
            "Layout" -> listOf(
                k("Layout","Colore","col1") to "Bianco",
                k("Layout","Colore","col2") to "Grigio chiaro",
                k("Layout","Colore","grad") to "Monocolore",
                k("Layout","Colore","fx") to "Vignettatura",
                k("Layout","Aggiungi foto","crop") to "Nessuno",
                k("Layout","Aggiungi foto","frame") to "Sottile",
                k("Layout","Aggiungi foto","filtro") to "Nessuno",
                k("Layout","Aggiungi foto","fit") to "Cover",
                k("Layout","Aggiungi album","cropAlbum") to "Nessuno",
                k("Layout","Aggiungi album","frameAlbum") to "Sottile",
                k("Layout","Aggiungi album","filtroAlbum") to "Nessuno",
                k("Layout","Aggiungi album","fit") to "Cover",
                k("Layout","Aggiungi album","anim") to "Slide",
                k("Layout","Aggiungi album","speed") to "Media",
            )
            else -> emptyList()
        }
    }

    fun collectConfig(root: String): Map<String, Any?> =
        keysForRoot(root).associate { (k, def) -> k to (menuSelections[k] ?: def) }

    fun applyConfig(config: Map<String, Any?>) {
        config.forEach { (k, v) -> menuSelections[k] = v }
    }

    fun savePreset(root: String, name: String) {
        val normalized = name.trim()
        if (normalized.isBlank()) return
        val list = savedPresets.getOrPut(root) { mutableListOf("Nessuno") }
// mantieni "Nessuno", rimuovi eventuali duplicati (case-insensitive), poi aggiungi
        list.removeAll { it.equals(normalized, ignoreCase = true) }
        list.add(normalized)
        val store = presetValues.getOrPut(root) { mutableMapOf() }
        store[normalized] = collectConfig(root)
    }

    fun applyPresetByName(root: String, name: String) {
        val cfg = presetValues[root]?.get(name) ?: return
        applyConfig(cfg)
    }

    fun resolveAndApply(root: String) {
        val defaultKey = key(listOf(root), "default")
        val styleKey = key(listOf(root), "style")
        val defSel = (menuSelections[defaultKey] as? String)?.trim().orEmpty()
        val styleSel = (menuSelections[styleKey] as? String)?.trim().orEmpty()

        when {
            styleSel.isNotEmpty() && !styleSel.equals("Nessuno", true) && presetValues[root]?.containsKey(styleSel) == true ->
                applyPresetByName(root, styleSel)
            defSel.isNotEmpty() && !defSel.equals("Nessuno", true) && presetValues[root]?.containsKey(defSel) == true ->
                applyPresetByName(root, defSel)
            else ->
                applyConfig(keysForRoot(root).associate { (k, def) -> k to def })
        }
    }

// Seeding di alcuni preset iniziali (cosà "Default/Titolo/..." agiscono subito)
    LaunchedEffect(Unit) {
        fun ensure(root: String, name: String, values: Map<String, Any?>) {
            val store = presetValues.getOrPut(root) { mutableMapOf() }
            if (store[name] == null) store[name] = values
        }
// TESTO
        ensure("Testo", "Titolo", mapOf(
            key(listOf("Testo"),"Sottolinea") to false,
            key(listOf("Testo"),"Corsivo") to false,
            key(listOf("Testo"),"Evidenzia") to "Nessuna",
            key(listOf("Testo"),"Font") to "Inter",
            key(listOf("Testo"),"Weight") to "Bold",
            key(listOf("Testo"),"Size") to "22sp",
            key(listOf("Testo"),"Colore") to "Bianco",
        ))
        ensure("Testo", "Sottotitolo", mapOf(
            key(listOf("Testo"),"Sottolinea") to false,
            key(listOf("Testo"),"Corsivo") to false,
            key(listOf("Testo"),"Evidenzia") to "Nessuna",
            key(listOf("Testo"),"Font") to "Inter",
            key(listOf("Testo"),"Weight") to "Medium",
            key(listOf("Testo"),"Size") to "18sp",
            key(listOf("Testo"),"Colore") to "Bianco",
        ))
        ensure("Testo", "Body", mapOf(
            key(listOf("Testo"),"Sottolinea") to false,
            key(listOf("Testo"),"Corsivo") to false,
            key(listOf("Testo"),"Evidenzia") to "Nessuna",
            key(listOf("Testo"),"Font") to "Inter",
            key(listOf("Testo"),"Weight") to "Regular",
            key(listOf("Testo"),"Size") to "16sp",
            key(listOf("Testo"),"Colore") to "Nero",
        ))
// CONTENITORE
        ensure("Contenitore", "Card base", mapOf(
            key(listOf("Contenitore"),"scroll") to "Assente",
            key(listOf("Contenitore"),"shape") to "Rettangolo",
            key(listOf("Contenitore"),"variant") to "Outlined",
            key(listOf("Contenitore"),"b_thick") to "1dp",
            key(listOf("Contenitore","Colore"),"col1") to "Bianco",
            key(listOf("Contenitore","Colore"),"col2") to "Grigio chiaro",
        ))
        ensure("Contenitore", "Hero", mapOf(
            key(listOf("Contenitore"),"variant") to "Full",
            key(listOf("Contenitore","Colore"),"col1") to "Ciano",
            key(listOf("Contenitore","Colore"),"grad") to "Orizzontale",
        ))
// LAYOUT
        ensure("Layout", "Default chiaro", mapOf(
            key(listOf("Layout","Colore"),"col1") to "Bianco",
            key(listOf("Layout","Colore"),"col2") to "Grigio chiaro",
            key(listOf("Layout","Colore"),"grad") to "Orizzontale",
            key(listOf("Layout","Colore"),"fx") to "Vignettatura",
        ))
        ensure("Layout", "Default scuro", mapOf(
            key(listOf("Layout","Colore"),"col1") to "Nero",
            key(listOf("Layout","Colore"),"col2") to "Grigio",
            key(listOf("Layout","Colore"),"grad") to "Verticale",
            key(listOf("Layout","Colore"),"fx") to "Noise",
        ))
    }


// Misuro l'altezza della barra azioni per distanziare la barra categorie
    var actionsBarHeightPx by remember { mutableStateOf(0) }
// Dialog salvataggio stile
    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var showUpsell by remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalInfoMode provides InfoModeEnv(
            enabled = infoMode,
            show = { title, body -> infoCard = title to body }
        ),
        LocalIsFree provides (codiceprofree == 12345)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A1A1A), Color(0xFF242424)) // grigi scuri
                    )
                )
        ) {
            // 1) CANVAS — PRIMO FIGLIO del Box con gradiente
            Box(
                Modifier
                    .fillMaxSize()
                    // quando la griglia è aperta, sfoca e abbassa l'alpha SOLO del canvas
                    .let { if (gridPanelOpen) it.blur(16.dp).graphicsLayer(alpha = 0.40f) else it }
            ) {
                CanvasStage(
                    page            = pageState,
                    gridDensity     = pageState?.gridDensity ?: 6,
                    gridPreviewOnly = gridPanelOpen && gridIsDragging,
                    showFullGrid    = gridPanelOpen && showGridLines,
                    currentLevel    = currentLevel,
                    creationEnabled = (canCreateContainer && toolMode == ToolMode.Create),
                    toolMode        = toolMode,
                    selected        = selectedRect,
                    onAddItem       = { item -> pageState?.items?.add(item) },

                    onRequestEdit   = { rect ->
                        selectedRect = rect
                        if (rect != null) {
                            if (menuPath.firstOrNull() != "Contenitore") menuPath = listOf("Contenitore")
                            applyContainerMenuFromRect(rect)
                        }
                    },
                    onUpdateItem    = { old, updated ->
                        val items = pageState?.items ?: return@CanvasStage
                        val ix = items.indexOf(old)
                        if (ix >= 0) {
                            items[ix] = updated
                            // trasferisci le “decorazioni”
                            rectFillStyles[updated] = rectFillStyles.remove(old) ?: rectFillStyles[updated] ?: return@CanvasStage
                            rectImages[updated]     = rectImages.remove(old)     ?: rectImages[updated]     ?: return@CanvasStage
                            rectCorners[updated]    = rectCorners.remove(old)    ?: rectCorners[updated]    ?: return@CanvasStage
                            if (selectedRect == old) {
                                selectedRect = updated
                                applyContainerMenuFromRect(updated)
                            }
                        }
                    },

                    // mappe
                    fillStyles   = rectFillStyles,
                    variants     = rectVariants,
                    shapes       = rectShapes,
                    corners      = rectCorners,
                    fx           = rectFx,
                    imageStyles  = rectImages
                )
            }
            var idError by remember { mutableStateOf(false) }
            if (menuPath.isEmpty()) {
// PRIMA BARRA
                MainBottomBar(
                    onUndo = { /* ... */ },
                    onRedo = { /* ... */ },
                    onSaveFile = { /* ... */ },
                    onDelete = { /* ... */ },
                    onDuplicate = { /* ... */ },
                    onProperties = { /* ... */ },
                    onLayout = { menuPath = listOf("Layout") },
                    onCreate = { /* se vuoi tenerlo per retrocompatibilità  */ },
                    onCreatePage = { openWizardFor(DeckRoot.PAGINA) },          // • NEW
                    onCreateAlert = { openWizardFor(DeckRoot.AVVISO) },         // • NEW
                    onCreateMenuLaterale = { openWizardFor(DeckRoot.MENU_LATERALE) }, // • NEW
                    onCreateMenuCentrale = { openWizardFor(DeckRoot.MENU_CENTRALE) }, // • NEW
                    onOpenList = { /* ... */ },
                    onSaveProject = { /* ... */ },
                    onOpenProject = { /* ... */ },
                    onNewProject = { /* ... */ },
                    onMeasured = { actionsBarHeightPx = it },
                    discontinuousBottom = menuPath.isEmpty()
                )


// SECONDA BARRA
                if (!classicEditing) {

                    CompositionLocalProvider(
                        LocalSecondBarMode provides SecondBarMode.Deck,
                        LocalDeckState provides DeckState(
                            openKey = deckOpen,
                            toggle = { key -> deckOpen = if (deckOpen == key) null else key }
                        ),
                        LocalDeckController provides DeckController(
                            openChild = { root -> classicEditing = true; editingClass = root; deckOpen = null },
                            openWizard = { root -> wizardTarget = root; wizardVisible = true }
                        ),

                        LocalDeckItems provides deckItems.mapValues { entry -> entry.value.toList() }
                    ) {
                        MainMenuBar(
                            onLayout = { menuPath = listOf("Layout") },
                            onContainer = { menuPath = listOf("Contenitore") },
                            onText     = { menuPath = listOf("Testo") },
                            onAdd      = { menuPath = listOf("Aggiungi") },
                            bottomBarHeightPx = actionsBarHeightPx
                        )
                    }
                } else {
                    CompositionLocalProvider(
                        LocalSecondBarMode provides SecondBarMode.Classic,
                        LocalExitClassic provides { classicEditing = false }    // torna alle icone madre
                    ) {
                        MainMenuBar(
                            onLayout = { menuPath = listOf("Layout") },
                            onContainer = { menuPath = listOf("Contenitore") },
                            onText = { menuPath = listOf("Testo") },
                            onAdd = { menuPath = listOf("Aggiungi") },
                            bottomBarHeightPx = actionsBarHeightPx
                        )
                    }
                }
                CreationWizardOverlay(
                    visible = wizardVisible,
                    target  = wizardTarget,
                    existingIds = deckItems.values.flatten().toSet(),   // • tutti gli ID esistenti
                    onDismiss = { wizardVisible = false },
                    onCreate  = { wr ->
                        deckItems.getOrPut(wr.root) { mutableStateListOf() }.add(wr.id)
                        wizardVisible = false
                    
                        deckOpen = when (wr.root) {
                            DeckRoot.PAGINA        -> "pagina"
                            DeckRoot.MENU_LATERALE -> "menuL"
                            DeckRoot.MENU_CENTRALE -> "menuC"
                            DeckRoot.AVVISO        -> "avviso"
                        }
                    
                        if (wr.root == DeckRoot.PAGINA) {
                            pageState = PageState(
                                id = wr.id,
                                scroll = wr.scroll,
                                gridDensity = 6,
                                currentLevel = 0
                            )

                            // entra nella “pagina figlia” appena creata (seconda barra in Classic)
                            editingClass   = DeckRoot.PAGINA
                            classicEditing = true
                        } else {
                            classicEditing = false
                        }
                    }
                )
            }
            else {
// IN SOTTOMENU: seconda barra = SubMenuBar; sotto c'è sempre il Breadcrumb.
// Imposta il "contesto pagina" per mostrare (in Layout) le voci Top/Bottom bar SOLO per Pagine.
                val isPageCtx = classicEditing && (editingClass == DeckRoot.PAGINA)
                CompositionLocalProvider(LocalIsPageContext provides isPageCtx) {
                    SubMenuBar(
                        path = menuPath,
                        selections = menuSelections,
                        onBack = {
                            if (menuPath.size == 1 && dirty) showConfirm = true
                            else {
                                menuPath = menuPath.dropLast(1)
                                lastChanged = null
                            }
                        },
                        onEnter = { label ->
                            // PRIMA calcolo la nuova rotta, POI la applico, e uso quella per i match
                            val leafSiblings = setOf("Aggiungi foto", "Aggiungi album", "Aggiungi video")
                            val nextPath = when {
                                menuPath.lastOrNull() == label -> menuPath
                                menuPath.lastOrNull() in leafSiblings && label in leafSiblings ->
                                    menuPath.dropLast(1) + label
                                else -> menuPath + label
                            }
                            menuPath = nextPath
                            lastChanged = null

                            val fullPath = nextPath.joinToString(" / ")

                            // --- UPLOAD: Contenitore / Immagini / Aggiungi foto / Upload
                            if (fullPath.endsWith("Contenitore / Immagini / Aggiungi foto / Upload")) {
                                // evita accumulo "foglie sorelle" (Aggiungi foto/album/video)
                                val leafSiblings = setOf("Aggiungi foto", "Aggiungi album", "Aggiungi video")
                                val nextPath = when {
                                    menuPath.lastOrNull() == label -> menuPath
                                    menuPath.lastOrNull() in leafSiblings && label in leafSiblings ->
                                        menuPath.dropLast(1) + label
                                    else -> menuPath + label
                                }
                                menuPath = nextPath
                                lastChanged = null

                                val fullPath = nextPath.joinToString(" / ")

                                // --- UPLOAD: Contenitore / Immagini / Aggiungi foto / Upload
                                if (fullPath.endsWith("Contenitore / Immagini / Aggiungi foto / Upload")) {
                                    val rect = selectedRect
                                    when {
                                        rect == null -> {
                                            infoCard = "Nessun contenitore" to "Seleziona prima un contenitore nella griglia."
                                        }
                                        rectImages[rect]?.uri != null -> {
                                            infoCard = "Foto già presente" to "Per caricare una nuova immagine, premi ic_cancel e conferma la cancellazione."
                                        }
                                        else -> {
                                            pickImageLauncher.launch("image/*")  // al ritorno apre direttamente il cropper
                                        }
                                    }
                                    return@SubMenuBar
                                }

                                // --- CROP: Contenitore / Immagini / Aggiungi foto / Crop
                                if (fullPath.endsWith("Contenitore / Immagini / Aggiungi foto / Crop")) {
                                    val rect = selectedRect ?: return@SubMenuBar
                                    val existing = rectImages[rect]
                                    if (existing?.uri != null) {
                                        cropperImageUri = existing.uri
                                        cropperTarget   = rect
                                        cropperVisible  = true         // riapre il cropper reale (ImageCropperDialog)
                                    } else {
                                        pickImageLauncher.launch("image/*")
                                    }
                                    return@SubMenuBar
                                }

                                // --- CANCELLA: Contenitore / Immagini / Aggiungi foto / Cancella immagine
                                if (fullPath.endsWith("Contenitore / Immagini / Aggiungi foto / Cancella immagine")) {
                                    if (selectedRect != null && rectImages[selectedRect] != null) {
                                        showDeleteImageDialog = true
                                    } else {
                                        infoCard = "Nessuna foto da cancellare" to "Non c'è un'immagine associata al contenitore selezionato."
                                    }
                                    return@SubMenuBar
                                }
                            when {
                                // Bordi -> Colore
                                fullPath.endsWith("Contenitore / Bordi / Colore") -> {
                                    colorPickTarget = ColorTarget.Border
                                    colorPickInitial = selectedRect?.borderColor ?: Color.Black
                                    showColorPicker = true
                                }
                                // Contenitore -> Colore -> col1
                                fullPath.endsWith("Contenitore / Colore / col1") -> {
                                    colorPickTarget = ColorTarget.ContainerFill(1)
                                    colorPickInitial = selectedRect?.fillColor ?: Color.White
                                    showColorPicker = true
                                }
                                // Contenitore -> Colore -> col2 (gating pro rimane gestito dal tuo SubMenuBar)
                                fullPath.endsWith("Contenitore / Colore / col2") -> {
                                    colorPickTarget = ColorTarget.ContainerFill(2)
                                    colorPickInitial = Color.Gray
                                    showColorPicker = true
                                }
                                // Testo -> Colore
                                fullPath.endsWith("Testo / Colore") -> {
                                    colorPickTarget = ColorTarget.Text
                                    colorPickInitial = Color.White
                                    showColorPicker = true
                                }
                                // Layout -> Colore -> col1/col2
                                fullPath.endsWith("Layout / Colore / col1") -> {
                                    colorPickTarget = ColorTarget.LayoutFill(1)
                                    colorPickInitial = Color.White
                                    showColorPicker = true
                                }
                                fullPath.endsWith("Layout / Colore / col2") -> {
                                    colorPickTarget = ColorTarget.LayoutFill(2)
                                    colorPickInitial = Color.Gray
                                    showColorPicker = true
                                }
                            }
                        }
                        onToggle = { label, value ->
                            val root = menuPath.firstOrNull() ?: "Contenitore"
                            menuSelections[key(menuPath, label)] = value
                            lastChanged = "$label: ${if (value) "ON" else "OFF"}"
                            dirty = true
// qualsiasi modifica manuale annulla lo STILE (Default resta)
                            val styleKey = key(listOf(root), "style")
                            val styleVal = (menuSelections[styleKey] as? String).orEmpty()
                            if (styleVal.isNotEmpty() && !styleVal.equals("Nessuno", true)) {
                                menuSelections[styleKey] = "Nessuno"
                            }
                        }
                        onPick = pick@{ label, value ->
                            // --- Intercetta e apre la palette flottante al posto del dropdown ---
                            run {
                                val isBordiColore = menuPath.size >= 2 &&
                                    menuPath[0] == "Contenitore" && menuPath[1] == "Bordi" && label == "Colore"
                                val isContCol1 = menuPath.size >= 2 &&
                                    menuPath[0] == "Contenitore" && menuPath[1] == "Colore" && label == "col1"
                                val isContCol2 = menuPath.size >= 2 &&
                                    menuPath[0] == "Contenitore" && menuPath[1] == "Colore" && label == "col2"

                                if (isBordiColore) {
                                    colorPickTarget = ColorTarget.Border
                                    colorPickInitial = selectedRect?.borderColor ?: Color.Black
                                    showColorPicker = true
                                    return@pick
                                }
                                if (isContCol1) {
                                    colorPickTarget = ColorTarget.ContainerFill(1)
                                    colorPickInitial = selectedRect?.fillColor ?: Color.White
                                    showColorPicker = true
                                    return@pick
                                }
                                if (isContCol2) {
                                    colorPickTarget = ColorTarget.ContainerFill(2)
                                    colorPickInitial = Color.Gray
                                    showColorPicker = true
                                    return@pick
                                }
                            }

                            // --- qui lascia invariata la tua logica esistente di onPick ---
                            val root = menuPath.firstOrNull() ?: "Contenitore"
                            val fullKey = key(menuPath, label)
                            menuSelections[fullKey] = value
                            lastChanged = "$label: $value"
                            dirty = true

                            // --- ANGOLO per rettangoli ---
                            if ((menuPath.firstOrNull() ?: "") == "Contenitore" && label in setOf("ic_as","ic_ad","ic_bd","ic_bs")) {
                                val rect = selectedRect
                                if (rect != null) {
                                    val dpVal = keyToDp(value)
                                    val cur = rectCorners[rect] ?: com.example.appbuilder.canvas.CornerRadii()
                                    val upd = when (label) {
                                        "ic_as" -> cur.copy(tl = dpVal)
                                        "ic_ad" -> cur.copy(tr = dpVal)
                                        "ic_bd" -> cur.copy(br = dpVal)
                                        else    -> cur.copy(bl = dpVal) // "ic_bs"
                                    }
                                    rectCorners[rect] = upd
                                }
                            }

                            // --- IMMAGINE: adatta + filtro (cluster "Aggiungi foto") ---
                            if ((menuPath.firstOrNull() ?: "") == "Contenitore" && menuPath.contains("Aggiungi foto")) {
                                val rect = selectedRect ?: return@pick
                                val st = rectImages[rect] ?: com.example.appbuilder.canvas.ImageStyle(
                                    uri = rectImages[rect]?.uri ?: return@pick
                                )
                                when (label) {
                                    "fitCont" -> {
                                        val fit = when ((value as? String)?.trim()?.lowercase()) {
                                            "cover"                  -> com.example.appbuilder.canvas.ImageFit.Cover
                                            "contain"                -> com.example.appbuilder.canvas.ImageFit.Contain
                                            "stretch", "riempi"      -> com.example.appbuilder.canvas.ImageFit.Stretch
                                            else                     -> com.example.appbuilder.canvas.ImageFit.Cover
                                        }
                                        rectImages[rect] = st.copy(fit = fit)
                                    }
                                    "filtro" -> {
                                        val f = when ((value as? String)?.trim()?.lowercase()) {
                                            "b/n", "bianco e nero", "mono", "b&w" -> com.example.appbuilder.canvas.ImageFilter.Mono
                                            "seppia", "sepia"                      -> com.example.appbuilder.canvas.ImageFilter.Sepia
                                            else                                   -> com.example.appbuilder.canvas.ImageFilter.None
                                        }
                                        rectImages[rect] = st.copy(filter = f)
                                    }
                                    // "crop" (toggle “ritaglia nella forma”)
                                    "crop" -> {
                                        val on = !((value as? String).orEmpty().equals("Nessuno", ignoreCase = true))
                                        rectImages[rect] = st.copy(cropToShape = on)
                                    }
                                }
                            }

                            // Aggiornamenti mirati sui container
                            if ((menuPath.firstOrNull() ?: "") == "Contenitore") {
                                val rect = selectedRect

                                when (label) {
                                    // Variant
                                    "variant" -> {
                                        rect?.let {
                                            val v = when ((value as? String)?.lowercase()?.trim()) {
                                                "full"       -> com.example.appbuilder.canvas.Variant.Full
                                                "outlined"   -> com.example.appbuilder.canvas.Variant.Outlined
                                                "text"       -> com.example.appbuilder.canvas.Variant.Text
                                                "topbottom"  -> com.example.appbuilder.canvas.Variant.TopBottom
                                                else         -> com.example.appbuilder.canvas.Variant.Full
                                            }
                                            rectVariants[it] = v
                                        }
                                    }

                                    // Shape (blocco "cerchio" se non è quadrato)
                                    "shape" -> {
                                        rect?.let {
                                            val rows = kotlin.math.abs(it.r1 - it.r0) + 1
                                            val cols = kotlin.math.abs(it.c1 - it.c0) + 1
                                            val isSquare = rows == cols
                                            val s = when ((value as? String)?.lowercase()?.trim()) {
                                                "cerchio"   -> if (isSquare) com.example.appbuilder.canvas.ShapeKind.Circle
                                                            else { infoCard = "Forma non disponibile" to "Cerchio è selezionabile solo per contenitori quadrati."; com.example.appbuilder.canvas.ShapeKind.Rect }
                                                "pillola"   -> com.example.appbuilder.canvas.ShapeKind.Pill
                                                "diamante"  -> com.example.appbuilder.canvas.ShapeKind.Diamond
                                                else        -> com.example.appbuilder.canvas.ShapeKind.Rect
                                            }
                                            rectShapes[it] = s
                                        }
                                    }

                                    "ic_as", "ic_ad", "ic_bs", "ic_bd" -> {
                                        val rect = selectedRect
                                        if (rect != null) {
                                            val dp = keyToDp(value)
                                            val current = rectCorners[rect] ?: com.example.appbuilder.canvas.CornerRadii()
                                            rectCorners[rect] = when (label) {
                                                "ic_as" -> current.copy(tl = dp)
                                                "ic_ad" -> current.copy(tr = dp)
                                                "ic_bs" -> current.copy(bl = dp)
                                                else    -> current.copy(br = dp)
                                            }
                                        }
                                    }
                                    // FX
                                    "fx" -> {
                                        rect?.let {
                                            val f = when ((value as? String)?.lowercase()?.trim()) {
                                                "vignettatura" -> com.example.appbuilder.canvas.FxKind.Vignette
                                                "noise"        -> com.example.appbuilder.canvas.FxKind.Noise
                                                "strisce"      -> com.example.appbuilder.canvas.FxKind.Stripes
                                                "nessuno", "", null -> com.example.appbuilder.canvas.FxKind.None
                                                else -> com.example.appbuilder.canvas.FxKind.None
                                            }
                                            rectFx[it] = f
                                        }
                                    }
                                }
                            }

                            when (label) {
                                "default" -> {
                                    val name = value
                                    if (name.equals("Nessuno", true)) {
                                        resolveAndApply(root)
                                    } else {
                                        applyPresetByName(root, name)
                                        val styleVal = (menuSelections[key(listOf(root), "style")] as? String).orEmpty()
                                        if (styleVal.isNotEmpty() && !styleVal.equals("Nessuno", true) && !styleVal.equals(name, true)) {
                                            applyPresetByName(root, styleVal) // stile ha priorità
                                        }
                                    }
                                }
                                "style" -> {
                                    val name = value
                                    if (name.equals("Nessuno", true)) {
                                        resolveAndApply(root)
                                    } else {
                                        applyPresetByName(root, name) // applica subito e con precedenza
                                    }
                                }
                                else -> {
// modifica puntuale → stile attivo passa a "Nessuno"
                                    val styleKey = key(listOf(root), "style")
                                    val currentStyle = (menuSelections[styleKey] as? String).orEmpty()
                                    if (currentStyle.isNotEmpty() && !currentStyle.equals("Nessuno", true)) {
                                        menuSelections[styleKey] = "Nessuno"
                                    }
                                }
                            }
                        },
                        savedPresets = savedPresets,
                        onFreeGate = { showUpsell = true }
                    )
                    BreadcrumbBar(path = menuPath, lastChanged = lastChanged)
                }

// Barra di conferma (quando risali con modifiche)
                if (showConfirm) {
                    val isFreeUser = LocalIsFree.current
                    ConfirmBar(
                        onCancel = {
                            dirty = false
                            lastChanged = null
                            showConfirm = false
                            menuPath = emptyList()
                        },
                        onOk = {
                            dirty = false
                            showConfirm = false
                            menuPath = emptyList()
                        },
                        onSavePreset = {
                            if (isFreeUser) showUpsell = true
                            else showSaveDialog = true
                        }
                    )
                }

// Dialog "Salva come stile"
                if (showSaveDialog) {
                    AlertDialog(
                        onDismissRequest = { showSaveDialog = false },
                        title = { Text("Salva come stile") },
                        text = {
                            Column {
                                Text("Dai un nome allo stile corrente. Se esiste già , verrà  aggiornato.")
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = newPresetName,
                                    onValueChange = { newPresetName = it },
                                    singleLine = true,
                                    label = { Text("Nome stile") },
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = WIZ_AZURE,
                                        focusedIndicatorColor = WIZ_AZURE,
                                        unfocusedIndicatorColor = Color(0xFF2A3B5B),
                                        focusedLabelColor = WIZ_AZURE,
                                        unfocusedLabelColor = Color(0xFF9BA3AF)
                                    )
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val root = menuPath.firstOrNull() ?: "Contenitore"
                                val name = newPresetName.trim()
                                if (name.isNotBlank()) {
                                    savePreset(root, name)
                                }
                                newPresetName = ""
                                dirty = false
                                showSaveDialog = false
                                showConfirm = false
                                menuPath = emptyList()
                            }) { Text("Salva") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                newPresetName = ""
                                showSaveDialog = false
                            }) { Text("Annulla") }
                        }
                    )
                }
                if (showDeleteImageDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteImageDialog = false },
                        title   = { Text("Cancellare?") },
                        text    = { Text("Attenzione, se scegli Sì perderai tutte le modifiche che hai apportato alla tua foto.") },
                        confirmButton = {
                            TextButton(onClick = {
                                selectedRect?.let { rectImages.remove(it) }
                                showDeleteImageDialog = false
                            }) { Text("Sì") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteImageDialog = false }) { Text("Annulla") }
                        }
                    )
                }
            }

            InfoEdgeDeck(
                open = infoDeckOpen,
                onToggleOpen = { infoDeckOpen = !infoDeckOpen },
                infoEnabled = infoMode,
                onToggleInfo = { infoMode = !infoMode },
                enabled = (menuPath.isEmpty() || menuPath.firstOrNull() == "Contenitore"),

                gridEnabled = gridPanelOpen,
                onToggleGrid = { gridPanelOpen = !gridPanelOpen },
                levelEnabled = levelPanelOpen,
                onToggleLevel = { levelPanelOpen = !levelPanelOpen },
                currentLevel = currentLevel,

                isContainerContext = isContainerContext,
                toolMode = toolMode,
                onCycleMode = {
                    toolMode = when (toolMode) {
                        ToolMode.Create -> ToolMode.Point
                        ToolMode.Point  -> ToolMode.Grab
                        ToolMode.Grab   -> ToolMode.Resize
                        ToolMode.Resize -> ToolMode.Create
                    }
                }
            )
            FloatingColorPickerOverlay(
                visible = showColorPicker,
                initialColor = colorPickInitial,
                onDismiss = { showColorPicker = false },
                onPick = { picked: Color ->    // ⬅️ tipo esplicito
                    val hex = picked.toHex()
                    when (val tgt = colorPickTarget) {
                        ColorTarget.Border -> {
                            selectedRect?.let { rect ->
                                pageState?.let { ps ->
                                    val i = ps.items.indexOf(rect)
                                    if (i >= 0) {
                                        val updated = rect.copy(borderColor = picked)
                                        ps.items[i] = updated
                                        selectedRect = updated
                                    }
                                }
                            }
                            menuSelections[(listOf("Contenitore") + "b_color").joinToString(" / ")] = hex
                        }
                        is ColorTarget.ContainerFill -> {
                            selectedRect?.let { rect ->
                                if (tgt.slot == 1) {
                                    pageState?.let { ps ->
                                        val i = ps.items.indexOf(rect)
                                        if (i >= 0) {
                                            val updated = rect.copy(fillColor = picked)
                                            ps.items[i] = updated
                                            selectedRect = updated
                                        }
                                    }
                                    menuSelections[(listOf("Contenitore","Colore") + "col1").joinToString(" / ")] = hex
                                } else {
                                    menuSelections[(listOf("Contenitore","Colore") + "col2").joinToString(" / ")] = hex
                                }
                            } ?: run {
                                val key = (listOf("Contenitore","Colore") + if (tgt.slot==1) "col1" else "col2").joinToString(" / ")
                                menuSelections[key] = hex
                            }
                        }
                        is ColorTarget.LayoutFill -> {
                            val key = (listOf("Layout","Colore") + if (tgt.slot==1) "col1" else "col2").joinToString(" / ")
                            menuSelections[key] = hex
                        }
                        ColorTarget.Text -> {
                            val key = (listOf("Testo") + "Colore").joinToString(" / ")
                            menuSelections[key] = hex
                        }
                        null -> Unit
                    }
                    showColorPicker = false
                }
            )



            // Overlay: Slider densità griglia (valori NON arbitrari)
            GridSliderOverlay(
                visible = gridPanelOpen,
                value = pageState?.gridDensity ?: 6,
                allowedValues = listOf(4, 6, 8, 10, 12), // passi ammessi
                onStartDrag = { gridIsDragging = true },
                onValueChange = { v -> pageState = pageState?.copy(gridDensity = v) ?: pageState },
                onEndDrag = { gridIsDragging = false },
                onDismiss = { gridPanelOpen = false }
            )
            
            // Deriva il range livelli dagli items e dal livello corrente
            val minLvl = pageState?.items?.minOfOrNull { it.level } ?: 0
            val maxFromItems = pageState?.items?.maxOfOrNull { it.level } ?: 0
            val maxLvl = currentLevel.coerceAtLeast(maxFromItems) // ← niente import

            LevelPickerOverlay(
                visible = levelPanelOpen,
                current = currentLevel,
                minLevel = minLvl,
                maxLevel = maxLvl,
                onPick = { lvl ->
                    currentLevel = lvl
                    levelPanelOpen = false
                    pageState = pageState?.copy(currentLevel = lvl) ?: pageState
                },
                onDismiss = { levelPanelOpen = false }
            )
            CropImageOverlay(
                visible = cropOverlayVisible,
                imageUri = cropTargetRect?.let { rectImages[it]?.uri },
                onDismiss = { cropOverlayVisible = false },
                onApply = { style ->
                    cropTargetRect?.let { tgt ->
                        rectImages[tgt] = style
                        lastChanged = "Ritaglio applicato"
                        dirty = true
                    }
                    cropOverlayVisible = false
                }
            )
            // 2) Toast informativo (in alto, scompare con fade)
            InfoToastCard(
                visible = infoCardVisible && infoCard != null,
                title = infoCard?.first ?: "",
                body = infoCard?.second ?: "",
                onDismiss = { infoCardVisible = false; infoCard = null }
            )
            if (showUpsell) {
                ProUpsellSheet(onClose = { showUpsell = false })
            }
        }
    }
}

@Composable
fun FloatingColorPickerOverlay(
    visible: Boolean,
    initialColor: Color,
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit
) {
    if (!visible) return

    // palette: 24 tinte "ruota" + alcuni neutri al centro
    val ring = remember {
        listOf(
            0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00, 0xFF34C759, 0xFF30B0C7, 0xFF007AFF,
            0xFF5856D6, 0xFFAF52DE, 0xFFFF2D55, 0xFFFF6B81, 0xFFFFD166, 0xFFA7E34B,
            0xFF64D2FF, 0xFF74A7FF, 0xFF8E8AFF, 0xFFCDA7FF, 0xFFFF8FA3, 0xFFFFB86C,
            0xFFEDE56A, 0xFF72E7A9, 0xFF80E8FF, 0xFFB0CCFF, 0xFFD0B0FF, 0xFFFFA7C2
        ).map { Color(it) }
    }
    val neutrals = listOf(Color.Black, Color.DarkGray, Color.Gray, Color.LightGray, Color.White)

    var drag by remember { mutableStateOf(IntOffset.Zero) }

    Box(Modifier.fillMaxSize().zIndex(1000f)) {
        // scrim e tap-chiusura
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
        )

        // pannello trascinabile
        Box(
            Modifier
                .align(Alignment.Center)
                .offset { drag }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        drag = drag.copy(
                            x = drag.x + dragAmount.x.toInt(),
                            y = drag.y + dragAmount.y.toInt()
                        )
                    }
                }
        ) {
            // "ruota" + preview
            Box(
                Modifier
                    .size(260.dp)
                    .background(Color(0xFF10151F), shape = RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                // anello di chip circolari
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val n = ring.size
                    val radius = 88.dp
                    val chip = 32.dp
                    val centerX = maxWidth / 2
                    val centerY = maxHeight / 2
                    val r = radius
                    val step = 360f / n

                    ring.forEachIndexed { i, c ->
                        val ang = Math.toRadians((i * step - 90f).toDouble()) // parte da alto
                        val dx = ((cos(ang) * r.value).toFloat()).dp
                        val dy = ((sin(ang) * r.value).toFloat()).dp

                        Box(
                            Modifier
                                .size(chip)
                                .offset(x = dx, y = dy)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(c)
                                .border(2.dp, Color.White.copy(alpha = 0.70f), CircleShape)
                                .pointerInput(c) {
                                    detectTapGestures(onTap = { onPick(c) })
                                }
                        )
                    }
                }

                // neutri centrali + HEX
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        neutrals.forEach { c ->
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    .pointerInput(c) {
                                        detectTapGestures(onTap = { onPick(c) })
                                    }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = initialColor.copy(alpha = 0.15f),
                        contentColor = initialColor,
                        shape = RoundedCornerShape(10.dp),
                        tonalElevation = 2.dp,
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = initialColor.toHex(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}


/* =========================================================================================
*  BARRA PRINCIPALE (icone stile GitHub, scura, sempre visibile in HOME)
* ========================================================================================= */
@Composable
private fun BoxScope.MainBottomBar(
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSaveFile: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onProperties: () -> Unit,
    onLayout: () -> Unit,
    onCreate: () -> Unit,            // rimane, se lo usi altrove
    onCreatePage: () -> Unit = {},   // NEW
    onCreateAlert: () -> Unit = {},  // NEW
    onCreateMenuLaterale: () -> Unit = {}, // NEW
    onCreateMenuCentrale: () -> Unit = {}, // NEW
    onOpenList: () -> Unit,
    onSaveProject: () -> Unit,
    onOpenProject: () -> Unit,
    onNewProject: () -> Unit,
    onMeasured: (Int) -> Unit,
    discontinuousBottom: Boolean = true
) {
// --- stato locale ---
    var showCreateMenu by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }

// densità  e misure contenitore
    val localDensity = LocalDensity.current
    var containerHeightPx by remember { mutableStateOf(0f) }
    var containerLeftInRoot by remember { mutableStateOf(0f) }

// misure per etichette
    var firstBlockCenter by remember { mutableStateOf<Float?>(null) }   // 4 icone sinistra
    var lastBlockCenter by remember { mutableStateOf<Float?>(null) }    // icone progetti
    var wElementi by remember { mutableStateOf(0f) }
    var wPagine by remember { mutableStateOf(0f) }
    var wProgressi by remember { mutableStateOf(0f) }

// misure per "gap tra gruppi" (più affidabile dei puntini)
    var firstBlockRightEdge by remember { mutableStateOf<Float?>(null) }      // destra blocco 1
    var middleBlockLeftEdge by remember { mutableStateOf<Float?>(null) }      // sinistra blocco Pagine&Menù
    var preSecondBlockRightEdge by remember { mutableStateOf<Float?>(null) }  // destra blocco Crea+Lista
    var lastBlockLeftEdge by remember { mutableStateOf<Float?>(null) }        // sinistra blocco Progetti

// misure dei puntini (se servono in futuro)
    var firstDotCenter by remember { mutableStateOf<Float?>(null) }
    var secondDotCenter by remember { mutableStateOf<Float?>(null) }

// --- varia colore linea (scritte + linea) ---
    val lineAccent = DECK_HIGHLIGHT // varia colore linea
// --- alza etichette di poco rispetto alla linea ---
    val labelLift = 3.dp

// stile etichette (usa lineAccent)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = lineAccent
    )

// parametri linea a filo del bordo inferiore
    val underlineStroke = 1.dp // spessore
    val extraGapPad = 2.dp     // piccolo margine extra su entrambi i lati dei gap

    Surface(
        color = Color(0xFF0D1117),
        contentColor = Color.White,
        tonalElevation = 10.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = SAFE_BOTTOM_MARGIN)
            .height(BOTTOM_BAR_HEIGHT + BOTTOM_BAR_EXTRA)
            .onGloballyPositioned { onMeasured(it.size.height) }
    ) {
        val scroll = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    containerHeightPx = it.size.height.toFloat()
                    containerLeftInRoot = it.positionInRoot().x
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp)
                    .horizontalScroll(scroll),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
// --------- GRUPPO SINISTRO ---------
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {

// BLOCCO 1: quattro icone (ELEMENTI)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val left = coords.positionInRoot().x - containerLeftInRoot
                            val width = coords.size.width.toFloat()
                            firstBlockCenter = left + width / 2f
                            firstBlockRightEdge = left + width
                        }
                    ) {
// BLOCCO 1
                        ToolbarIconButton(Icons.Outlined.Undo, "Undo", onClick = onUndo,
                            infoTitle = "Annulla", infoBody = "Annulla l'ultima modifica", allowLongPressInInfo = false )
                        ToolbarIconButton(Icons.Outlined.Redo, "Redo", onClick = onRedo,
                            infoTitle = "Ripeti", infoBody = "Ripristina l'ultima azione annullata", allowLongPressInInfo = false )
                        ToolbarIconButton(EditorIcons.Delete, "Cestino", onClick = onDelete,
                            infoTitle = "Cestino", infoBody = "Elimina l'elemento corrente", allowLongPressInInfo = false )
                        ToolbarIconButton(EditorIcons.Duplicate, "Duplica", onClick = onDuplicate,
                            infoTitle = "Duplica", infoBody = "Crea una copia dell'elemento corrente", allowLongPressInInfo = false )
                    }

// PUNTINO 1
                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val left = coords.positionInRoot().x - containerLeftInRoot
                            val width = coords.size.width.toFloat()
                            firstDotCenter = left + width / 2f
                        }
                    ) { dividerDot() }

                    // BLOCCO INTERMEDIO (PAGINE E MENU'™)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val left = coords.positionInRoot().x - containerLeftInRoot
                            middleBlockLeftEdge = left
                        }
                    ) {
                        ToolbarIconButton(EditorIcons.Settings, "Proprietà ", onClick = onProperties,
                            infoTitle = "Proprietà ", infoBody = "Impostazioni dell'elemento corrente")
                        ToolbarIconButton(EditorIcons.Layout, "Layout pagina", onClick = onLayout,
                            infoTitle = "Layout pagina", infoBody = "Apri le impostazioni di layout")
                        ToolbarIconButton(EditorIcons.Save, "Salva pagina", onClick = onSaveFile,
                            infoTitle = "Salva", infoBody = "Salva l'elemento corrente")
                    }
                }

// --------- GRUPPO DESTRO ---------
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {

// BLOCCO PRE-SECONDO PUNTINO: CREA + LISTA
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val left = coords.positionInRoot().x - containerLeftInRoot
                            val width = coords.size.width.toFloat()
                            preSecondBlockRightEdge = left + width
                        }
                    ) {
// CREA
                        DropdownMenu(expanded = showCreateMenu, onDismissRequest = { showCreateMenu = false }) {
                            DropdownMenuItem(text = { Text("Nuova pagina") },
                                onClick = { showCreateMenu = false; onCreatePage() })
                            DropdownMenuItem(text = { Text("Nuovo avviso") },
                                onClick = { showCreateMenu = false; onCreateAlert() })
                            DropdownMenuItem(text = { Text("Menù laterale") },
                                onClick = { showCreateMenu = false; onCreateMenuLaterale() })
                            DropdownMenuItem(text = { Text("Menù centrale") },
                                onClick = { showCreateMenu = false; onCreateMenuCentrale() })
                        }

                        Box {
                            ToolbarIconButton(EditorIcons.Insert, "Crea", onClick = { showCreateMenu = true })
                            DropdownMenu(expanded = showCreateMenu, onDismissRequest = { showCreateMenu = false }) {
                                DropdownMenuItem(text = { Text("Nuova pagina")  }, onClick = { showCreateMenu = false; onCreatePage() })
                                DropdownMenuItem(text = { Text("Nuovo avviso")  }, onClick = { showCreateMenu = false; onCreateAlert() })
                                DropdownMenuItem(text = { Text("Menù laterale") }, onClick = { showCreateMenu = false; onCreateMenuLaterale() })
                                DropdownMenuItem(text = { Text("Menù centrale") }, onClick = { showCreateMenu = false; onCreateMenuCentrale() })
                            }
                        }

// LISTA
                        Box {
                            ToolbarIconButton(Icons.Outlined.List, "Lista", onClick = { showListMenu = true; onOpenList() })
                            DropdownMenu(expanded = showListMenu, onDismissRequest = { showListMenu = false }) {
                                DropdownMenuItem(text = { Text("Pagine...") }, onClick = { showListMenu = false })
                                DropdownMenuItem(text = { Text("Avvisi...") }, onClick = { showListMenu = false })
                                DropdownMenuItem(text = { Text("Menu laterali...") }, onClick = { showListMenu = false })
                                DropdownMenuItem(text = { Text("Menu centrali...") }, onClick = { showListMenu = false })
                            }
                        }
                    }

// PUNTINO 2
                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val left = coords.positionInRoot().x - containerLeftInRoot
                            val width = coords.size.width.toFloat()
                            secondDotCenter = left + width / 2f
                        }
                    ) { dividerDot() }

// BLOCCO PROGETTI / PROGRESSI
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val left = coords.positionInRoot().x - containerLeftInRoot
                            val width = coords.size.width.toFloat()
                            lastBlockCenter = left + width / 2f
                            lastBlockLeftEdge = left
                        }
                    ) {
                        ToolbarIconButton(Icons.Outlined.Save, "Salva progetto", onClick = onSaveProject,
                            infoTitle = "Salva progetto", infoBody = "Salva lo stato del progetto")
                        ToolbarIconButton(Icons.Outlined.FolderOpen, "Apri", onClick = onOpenProject,
                            infoTitle = "Apri progetto", infoBody = "Apri un progetto esistente")
                        ToolbarIconButton(Icons.Outlined.CreateNewFolder, "Nuovo progetto", onClick = onNewProject,
                            infoTitle = "Nuovo progetto", infoBody = "Crea un nuovo progetto")
                    }
                }
            }

// ---------- BORDO BIANCO: top/sinistra/destra SEMPRE continui; bottom condizionale ----------
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                val stroke = with(localDensity) { underlineStroke.toPx() }
                val y = containerHeightPx - stroke / 2f
                val pad = with(localDensity) { 6.dp.toPx() }
                val extra = with(localDensity) { extraGapPad.toPx() }

// TOP (curvo + segmento) — segue gli angoli arrotondati della Surface
                val corner = with(localDensity) { 18.dp.toPx() } // deve combaciare con RoundedCornerShape(topStart/topEnd)
                val topY = stroke / 2f
// segmento centrale
                drawLine(
                    color = lineAccent,
                    start = androidx.compose.ui.geometry.Offset(corner, topY),
                    end   = androidx.compose.ui.geometry.Offset(size.width - corner, topY),
                    strokeWidth = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Butt
                )

                drawArc(
                    color = lineAccent,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, stroke / 2f),
                    size = androidx.compose.ui.geometry.Size(2 * corner - stroke, 2 * corner - stroke),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )

                drawArc(
                    color = lineAccent,
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width - (2 * corner) + stroke / 2f, stroke / 2f),
                    size = androidx.compose.ui.geometry.Size(2 * corner - stroke, 2 * corner - stroke),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
// LEFT
                drawLine(
                    color = lineAccent,
                    start = androidx.compose.ui.geometry.Offset(stroke / 2f, 0f),
                    end   = androidx.compose.ui.geometry.Offset(stroke / 2f, size.height),
                    strokeWidth = stroke
                )
// RIGHT
                drawLine(
                    color = lineAccent,
                    start = androidx.compose.ui.geometry.Offset(size.width - stroke / 2f, 0f),
                    end   = androidx.compose.ui.geometry.Offset(size.width - stroke / 2f, size.height),
                    strokeWidth = stroke
                )

                if (!discontinuousBottom) {
// PATH ATTIVO → bordo inferiore CONTINUO
                    drawLine(
                        color = lineAccent,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end   = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    return@Canvas
                }

// STATO HOME → bordo inferiore con GAP sotto le scritte e tra i gruppi (come ora)
                val gaps = mutableListOf<Pair<Float, Float>>()

                firstBlockCenter?.let { cx ->
                    if (wElementi > 0f) gaps += (cx - wElementi / 2f - pad) to (cx + wElementi / 2f + pad)
                }
                if (firstDotCenter != null && secondDotCenter != null && wPagine > 0f) {
                    val cx = (firstDotCenter!! + secondDotCenter!!) / 2f
                    gaps += (cx - wPagine / 2f - pad) to (cx + wPagine / 2f + pad)
                }
                lastBlockCenter?.let { cx ->
                    if (wProgressi > 0f) gaps += (cx - wProgressi / 2f - pad) to (cx + wProgressi / 2f + pad)
                }
                if (firstBlockRightEdge != null && middleBlockLeftEdge != null) {
                    val s = (firstBlockRightEdge!! - extra).coerceAtLeast(0f)
                    val e = (middleBlockLeftEdge!! + extra).coerceAtMost(size.width)
                    if (e > s) gaps += s to e
                }
                if (preSecondBlockRightEdge != null && lastBlockLeftEdge != null) {
                    val s = (preSecondBlockRightEdge!! - extra).coerceAtLeast(0f)
                    val e = (lastBlockLeftEdge!! + extra).coerceAtMost(size.width)
                    if (e > s) gaps += s to e
                }

                gaps.sortBy { it.first }
                var x0 = 0f
                for ((gs, ge) in gaps) {
                    val startX = gs.coerceAtLeast(0f)
                    if (startX > x0) {
                        drawLine(
                            color = lineAccent,
                            start = androidx.compose.ui.geometry.Offset(x0, y),
                            end   = androidx.compose.ui.geometry.Offset(startX, y),
                            strokeWidth = stroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                    x0 = ge.coerceAtLeast(x0)
                }
                if (x0 < size.width) {
                    drawLine(
                        color = lineAccent,
                        start = androidx.compose.ui.geometry.Offset(x0, y),
                        end   = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
// ---------- ETICHETTE (baseline allineata alla linea inferiore) ----------
// "elementi"
            if (firstBlockCenter != null) {
                var baselineElemPx by remember { mutableStateOf(0f) }
                Text(
                    "elementi",
                    style = labelStyle,
                    onTextLayout = { tlr -> baselineElemPx = tlr.getLineBaseline(0).toFloat() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .onGloballyPositioned { wElementi = it.size.width.toFloat() }
                        .offset {
                            val lineY = containerHeightPx - with(localDensity) { underlineStroke.toPx() } / 2f
                            val y = (lineY - baselineElemPx - with(localDensity) { labelLift.toPx() }).toInt()
                            val x = ((firstBlockCenter ?: 0f) - wElementi / 2f).toInt()
                            IntOffset(x, y)
                        }
                )
            }
// "pagine e menù"
            if (firstDotCenter != null && secondDotCenter != null) {
                var baselinePagPx by remember { mutableStateOf(0f) }
                Text(
                    "pagine e menù",
                    style = labelStyle,
                    onTextLayout = { tlr -> baselinePagPx = tlr.getLineBaseline(0).toFloat() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .onGloballyPositioned { wPagine = it.size.width.toFloat() }
                        .offset {
                            val cx = (firstDotCenter!! + secondDotCenter!!) / 2f
                            val lineY = containerHeightPx - with(localDensity) { underlineStroke.toPx() } / 2f
                            val y = (lineY - baselinePagPx - with(localDensity) { labelLift.toPx() }).toInt()
                            val x = (cx - wPagine / 2f).toInt()
                            IntOffset(x, y)
                        }
                )
            }
// "progressi"
            if (lastBlockCenter != null) {
                var baselineProgPx by remember { mutableStateOf(0f) }
                Text(
                    "progetti",
                    style = labelStyle,
                    onTextLayout = { tlr -> baselineProgPx = tlr.getLineBaseline(0).toFloat() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .onGloballyPositioned { wProgressi = it.size.width.toFloat() }
                        .offset {
                            val lineY = containerHeightPx - with(localDensity) { underlineStroke.toPx() } / 2f
                            val y = (lineY - baselineProgPx - with(localDensity) { labelLift.toPx() }).toInt()
                            val x = ((lastBlockCenter ?: 0f) - wProgressi / 2f).toInt()
                            IntOffset(x, y)
                        }
                )
            }
        }
    }
}


/* Barretta categorie (sopra la barra principale), icone-only */
@Composable
private fun BoxScope.MainMenuBar(
    onLayout: () -> Unit,
    onContainer: () -> Unit,
    onText: () -> Unit,
    onAdd: () -> Unit,
    bottomBarHeightPx: Int
) {
    val dy = with(LocalDensity.current) {
        (if (bottomBarHeightPx > 0) bottomBarHeightPx.toDp() else BOTTOM_BAR_HEIGHT) +
                BARS_GAP + SAFE_BOTTOM_MARGIN
    }
    Surface(
        color = Color(0xFF111621),
        contentColor = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .offset { IntOffset(0, -dy.roundToPx()) }
            .height(TOP_BAR_HEIGHT)
    ) {
        val scroll = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (LocalSecondBarMode.current) {
                SecondBarMode.Classic -> {
                    LocalExitClassic.current?.let { exitClassic ->
                        ToolbarIconButton(
                            icon = Icons.Outlined.ArrowBack,
                            contentDescription = "Indietro",
                            onClick = LocalExitClassic.current
                        )
                    }

                    ToolbarIconButton(EditorIcons.Text, "Testo", onClick = onText,
                        infoTitle = "Testo", infoBody = "Stili e proprietà  del testo", allowLongPressInInfo = false)
                    ToolbarIconButton(EditorIcons.Container, "Contenitore", onClick = onContainer,
                        infoTitle = "Contenitore", infoBody = "Aspetto e comportamenti del contenitore", allowLongPressInInfo = false)
                    ToolbarIconButton(EditorIcons.Layout, "Layout", onClick = onLayout,
                        infoTitle = "Layout", infoBody = "Colori/immagini ed effetti dell'area", allowLongPressInInfo = false)
                    ToolbarIconButton(EditorIcons.Insert, "Aggiungi", onClick = onAdd,
                        infoTitle = "Aggiungi", infoBody = "Inserisci nuovi elementi", allowLongPressInInfo = false)
                    ToolbarIconButton(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_question),
                        contentDescription = "Info",
                        onClick = { /* stub */ },
                        infoTitle = "Info elemento", infoBody = "Dati descrittivi (stub)", allowLongPressInInfo = false
                    )
                }

                SecondBarMode.Deck -> {
// NUOVA ROOT — MADRI + CLUSTER, con hiding delle madri a destra
                    val deck = LocalDeckState.current
                    val controller = LocalDeckController.current

                    // Ordine e mapping: serve per stabilire cosa "sta a destra"
                    data class Mother(val key: String, val iconRes: Int, val root: DeckRoot, val sampleId: String)
                    val mothers = listOf(
                        Mother("pagina", R.drawable.ic_page, DeckRoot.PAGINA, "pg001"),
                        Mother("menuL", R.drawable.ic_menu_laterale, DeckRoot.MENU_LATERALE, "ml001"),
                        Mother("menuC", R.drawable.ic_menu_centrale, DeckRoot.MENU_CENTRALE, "mc001"),
                        Mother("avviso", R.drawable.ic_avviso, DeckRoot.AVVISO, "al001")
                    )
                    val activeIdx = mothers.indexOfFirst { it.key == deck.openKey }  // -1 = nessun cluster aperto

                    mothers.forEachIndexed { idx, m ->
                        val showMother = (activeIdx == -1) || (idx <= activeIdx)   // nascondi madri a destra
                        if (showMother) {
                            MotherIcon(
                                icon = ImageVector.vectorResource(id = m.iconRes),
                                contentDescription = when (m.root) {
                                    DeckRoot.PAGINA -> "Pagina"
                                    DeckRoot.MENU_LATERALE -> "Menù laterale"
                                    DeckRoot.MENU_CENTRALE -> "Menù centrale"
                                    DeckRoot.AVVISO -> "Avviso"
                                },
                                selected = deck.openKey == m.key,
                                onClick = { deck.toggle(m.key) },
                                infoTitle = when (m.root) {
                                    DeckRoot.PAGINA -> "Pagine"
                                    DeckRoot.MENU_LATERALE -> "Menù laterale"
                                    DeckRoot.MENU_CENTRALE -> "Menù centrale"
                                    DeckRoot.AVVISO -> "Avvisi"
                                },
                                infoBody = "Tieni premuto per mostrare/nascondere le icone figlie"
                            )

                            if (deck.openKey == m.key) {
                                CPlusIcon(
                                    onClick = { controller.openWizard(m.root) },
                                    infoTitle = "Crea",
                                    infoBody = "Crea un nuovo elemento in questo cluster"
                                )

                                val children: List<String> = LocalDeckItems.current[m.root].orEmpty()
                                children.forEach { childId ->
                                    ChildIconWithBadge(
                                        icon = ImageVector.vectorResource(id = m.iconRes),
                                        id = childId,
                                        onClick = { controller.openChild(m.root) }, // long-press lo richiama comunque
                                        badgeBg = DECK_BADGE_BG,
                                        badgeTxt = DECK_BADGE_TXT
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MotherIcon(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,                 // azione "apri/chiudi cluster"
    infoTitle: String? = null,
    infoBody: String? = null
) {
    val info = LocalInfoMode.current
    Surface(
        shape = CircleShape,
        color = Color(0xFF1B2334),
        contentColor = Color.White,
        tonalElevation = if (selected) 6.dp else 0.dp,
        shadowElevation = if (selected) 6.dp else 0.dp,
        border = if (selected) BorderStroke(2.dp, DECK_HIGHLIGHT) else null,
        modifier = Modifier.size(42.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = {
                        if (info.enabled) {
                            info.show(infoTitle ?: contentDescription, infoBody ?: "—")
                        } else {
                            onClick()
                        }
                    },
// IN INFO MODE: il cluster si apre solo con long-press
                    onLongClick = { onClick() }
                )
        ) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CPlusIcon(
    onClick: () -> Unit,
    icon: ImageVector = ImageVector.vectorResource(id = R.drawable.ic_add_circle),
    infoTitle: String = "Crea",
    infoBody: String = "Crea un nuovo elemento in questo cluster"
) {
    val info = LocalInfoMode.current
    Surface(shape = CircleShape, color = Color(0xFF1B2334), contentColor = Color.White) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .combinedClickable(
                    onClick = {
                        if (info.enabled) {
                            info.show(infoTitle, infoBody)  // in info mode: SOLO descrizione
                        } else {
                            onClick()
                        }
                    },
                    onLongClick = { /* nessuna azione */ } // • disabilitato
                )
        ) {
            Icon(icon, contentDescription = "Nuovo", modifier = Modifier.align(Alignment.Center))
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChildIconWithBadge(
    icon: ImageVector,
    id: String,
    onClick: () -> Unit,
    badgeBg: Color = DECK_BADGE_BG,
    badgeTxt: Color = DECK_BADGE_TXT
) {
    val info = LocalInfoMode.current
    val shown = id.take(8)
    Box(contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = Color(0xFF1B2334), contentColor = Color.White) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .combinedClickable(
                        onClick = {
                            if (info.enabled) {
                                info.show("Elemento $shown", "Tieni premuto per aprire l'editor classico")
                            } else {
                                onClick()
                            }
                        },
                        onLongClick = { onClick() }    // long-press = apri editor classico
                    )
            ) {
                Icon(icon, contentDescription = shown, modifier = Modifier.align(Alignment.Center))
            }
        }
        Surface(
            color = badgeBg,
            contentColor = badgeTxt,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, 14) }
        ) {
            Text(
                text = shown,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 10.sp
            )
        }
    }
}

/* =========================================================================================
*  SUBMENU — barra icone livello corrente (menù "ad albero")
*  SOLO UI: nessuna modifica applicata al documento.
* ========================================================================================= */

@Composable
private fun BoxScope.SubMenuBar(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onBack: () -> Unit,
    onEnter: (String) -> Unit,
    onToggle: (label: String, value: Boolean) -> Unit,
    onPick: (label: String, value: String) -> Unit,
    savedPresets: Map<String, MutableList<String>>,
    onFreeGate: () -> Unit
) {
    val offsetY = with(LocalDensity.current) { (BOTTOM_BAR_HEIGHT + BOTTOM_BAR_EXTRA + BARS_GAP + SAFE_BOTTOM_MARGIN).roundToPx() }
    Surface(
        color = Color(0xFF0F141E),
        contentColor = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .offset { IntOffset(0, -offsetY) }
            .height(TOP_BAR_HEIGHT)
    ) {
        val scroll = rememberScrollState()
        CompositionLocalProvider(
            LocalInfoMode provides InfoModeEnv(enabled = false) { _, _ -> }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .horizontalScroll(scroll)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
// back icon
                ToolbarIconButton(Icons.Outlined.ArrowBack, "Indietro", onClick = onBack)

                when (path.firstOrNull()) {
                    "Layout"       -> LayoutLevel(path, selections, onEnter, onToggle, onPick, savedPresets, onFreeGate)
                    "Contenitore"  -> ContainerLevel(path, selections, onEnter, onToggle, onPick, savedPresets, onFreeGate)
                    "Testo"        -> TextLevel(path, selections, onToggle, onPick, savedPresets, onFreeGate)
                    "Aggiungi" -> AddLevel(
                        path = path,
                        selections = selections,
                        onEnter = onEnter,
                        onFreeGate = onFreeGate
                    )
                }
            }
        }
    }
}

/* =========================================================================================
*  BREADCRUMB — path corrente + ultima opzione
* ========================================================================================= */
@Composable
private fun BoxScope.BreadcrumbBar(path: List<String>, lastChanged: String?) {
    Surface(
        color = Color(0xFF0B0F16),
        contentColor = Color(0xFF9BA3AF),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 12.dp, top = 0.dp, end = 12.dp, bottom = SAFE_BOTTOM_MARGIN)
            .height(BOTTOM_BAR_HEIGHT + BOTTOM_BAR_EXTRA)
    ) {
        val pretty = buildString {
            append(if (path.isEmpty()) "—" else path.joinToString("  →  "))
            lastChanged?.let { append("   •   "); append(it) } 
        }
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(pretty, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/* =========================================================================================
*  LIVELLI (Layout / Contenitore / Testo / Aggiungi)
*  Icone-only + dropdown con badge valore corrente
* ========================================================================================= */

/* ---------- LAYOUT ---------- */
/* ---------- LAYOUT ---------- */
@Composable
private fun LayoutLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onEnter: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onPick: (String, String) -> Unit,
    saved: Map<String, MutableList<String>>,
    onFreeGate: () -> Unit
) {
    val isFree = LocalIsFree.current
    fun get(keyLeaf: String) = selections[key(path, keyLeaf)] as? String

// Aree "wrappate" del Layout che devono riusare SOLO i sottomenu whitelist (Colore/Immagini)
    val layoutAreas = setOf("Bottom bar", "Top bar", "Menù centrale", "Menù laterale")

    when (path.getOrNull(1)) {
        null -> {
// ROOT Layout
            ToolbarIconButton(EditorIcons.Color, "Colore") { onEnter("Colore") }
            ToolbarIconButton(EditorIcons.Image, "Immagini") { onEnter("Immagini") }

// NEW: aree che riusano i sottomenu del Layout (whitelist)

            ToolbarIconButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_call_to_action),
                contentDescription = "Bottom bar",
                onClick = { onEnter("Bottom bar") }
            )
            ToolbarIconButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_ad_units),
                contentDescription = "Top bar",
                onClick = { onEnter("Top bar") }
            )

// DEFAULT
            IconDropdown(
                icon = Icons.Outlined.BookmarkAdd,
                contentDescription = "Scegli default",
                current = get("default") ?: saved["Layout"]?.firstOrNull(),
                options = saved["Layout"].orEmpty(),
                onSelected = { onPick("default", it) },
                locked = isFree,
                onLockedAttempt = onFreeGate
            )

// STILE (Outlined custom o Material)
            IconDropdown(
                icon = ImageVector.vectorResource(id = R.drawable.ic_style),
                contentDescription = "Stile",
                current = get("style") ?: "Nessuno",
                options = saved["Layout"].orEmpty(),
                onSelected = { onPick("style", it) }
            )

        }

// ----- WRAPPER AREE: whitelist solo Colore/Immagini (+ sottomenu relativi) -----
        in layoutAreas -> {
            when (path.getOrNull(2)) {
                null -> {
// Mostra SOLO le voci consentite (whitelist), non tutte quelle presenti/future
                    ToolbarIconButton(EditorIcons.Color, "Colore") { onEnter("Colore") }
                    ToolbarIconButton(EditorIcons.Image, "Immagini") { onEnter("Immagini") }
                }
                "Colore" -> {
                    val isFree = LocalIsFree.current

                    IconDropdown(EditorIcons.Colors1, "Colore 1",
                        current = get("col1") ?: "Bianco",
                        options = if (isFree) listOf("Bianco", "Grigio", "Nero") else listOf("Bianco","Grigio","Nero","Ciano"),
                        onSelected = { onPick("col1", it) }
                    )

                    IconDropdown(EditorIcons.Colors2, "Colore 2",
                        current = get("col2") ?: "Grigio chiaro",
                        options = listOf("Grigio chiaro", "Blu", "Verde", "Arancio"),
                        onSelected = { onPick("col2", it) },
                        locked = isFree,
                        onLockedAttempt = onFreeGate
                    )

                    IconDropdown(EditorIcons.Gradient, "Gradiente",
                        current = get("grad") ?: "Orizzontale",
                        options = listOf("Orizzontale", "Verticale"),
                        onSelected = { onPick("grad", it) },
                        locked = isFree,
                        onLockedAttempt = onFreeGate
                    )
                    IconDropdown(EditorIcons.Functions, "Effetti",
                        current = get("fx") ?: "Vignettatura",
                        options = listOf("Vignettatura", "Noise", "Strisce"),
                        onSelected = { onPick("fx", it) }
                    )
                }
                "Immagini" -> {
                    ToolbarIconButton(EditorIcons.AddPhotoAlternate, "Aggiungi immagine",
                        locked = isFree,
                        onLockedAttempt = onFreeGate
                    ) { onEnter("Aggiungi foto") }

                    ToolbarIconButton(EditorIcons.PermMedia, "Aggiungi album",
                        locked = isFree,
                        onLockedAttempt = onFreeGate
                    ) { onEnter("Aggiungi album") }
                }
                "Aggiungi foto" -> {

                    ToolbarIconButton(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_scissor),
                        contentDescription = "Crop"
                    ) { onEnter("Crop") }   // non apre menu; segnala un'azione

                    IconDropdown(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_adapt),
                        contentDescription = "Adatta",
                        current = get("fitCont") ?: "Cover",
                        options = listOf("Cover", "Contain", "Stretch"),
                        onSelected = { onPick("fitCont", it) }
                    )

                    IconDropdown(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_filter),
                        contentDescription = "Filtro",
                        current = get("filtro") ?: "Nessuno",
                        options = listOf("Nessuno", "B/N", "Seppia"),
                        onSelected = { onPick("filtro", it) }
                    )

                    IconDropdown(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_filter),
                        contentDescription = "Filtro",
                        current = get("filtro") ?: "Nessuno",
                        options = listOf("Nessuno", "B/N", "Vintage", "Vivido"),
                        onSelected = { onPick("filtro", it) }
                    )
                    IconDropdown(EditorIcons.Layout, "Cornice",
                        current = get("frame") ?: "Sottile",
                        options = listOf("Nessuna", "Sottile", "Marcata"),
                        onSelected = { onPick("frame", it) }
                    )
                }
                "Aggiungi album" -> {
                    ToolbarIconButton(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_scissor),
                        contentDescription = "Crop"
                    ) { onEnter("Crop") }   // non apre menu; segnala un'azione
                    IconDropdown(EditorIcons.Layout, "Cornice",
                        current = get("frameAlbum") ?: "Sottile",
                        options = listOf("Nessuna", "Sottile", "Marcata"),
                        onSelected = { onPick("frameAlbum", it) }
                    )
                    IconDropdown(EditorIcons.Layout, "Filtri album",
                        current = get("filtroAlbum") ?: "Nessuno",
                        options = listOf("Nessuno", "Tutte foto stesso filtro"),
                        onSelected = { onPick("filtroAlbum", it) }
                    )
                    IconDropdown(EditorIcons.Layout, "Adattamento",
                        current = get("fit") ?: "Cover",
                        options = listOf("Cover", "Contain", "Fill"),
                        onSelected = { onPick("fit", it) }
                    )
                    IconDropdown(EditorIcons.Layout, "Animazione",
                        current = get("anim") ?: "Slide",
                        options = listOf("Slide", "Fade", "Page flip"),
                        onSelected = { onPick("anim", it) }
                    )
                    IconDropdown(EditorIcons.Layout, "Velocità ",
                        current = get("speed") ?: "Media",
                        options = listOf("Lenta", "Media", "Veloce"),
                        onSelected = { onPick("speed", it) }
                    )
                }
            }
        }

// ----- Layout "piano" (già  esistente) -----
        "Colore" -> {
            val isFree = LocalIsFree.current

            IconDropdown(EditorIcons.Colors1, "Colore 1",
                current = get("col1") ?: "Bianco",
                options = if (isFree) listOf("Bianco", "Grigio", "Nero") else listOf("Bianco","Grigio","Nero","Ciano"),
                onSelected = { onPick("col1", it) }
            )

            IconDropdown(EditorIcons.Colors2, "Colore 2",
                current = get("col2") ?: "Grigio chiaro",
                options = listOf("Grigio chiaro", "Blu", "Verde", "Arancio"),
                onSelected = { onPick("col2", it) },
                locked = isFree,
                onLockedAttempt = onFreeGate
            )

            IconDropdown(EditorIcons.Gradient, "Gradiente",
                current = get("grad") ?: "Orizzontale",
                options = listOf("Orizzontale", "Verticale"),
                onSelected = { onPick("grad", it) },
                locked = isFree,
                onLockedAttempt = onFreeGate
            )
            IconDropdown(EditorIcons.Functions, "Effetti",
                current = get("fx") ?: "Vignettatura",
                options = listOf("Vignettatura", "Noise", "Strisce"),
                onSelected = { onPick("fx", it) }
            )
        }
        "Immagini" -> {
            ToolbarIconButton(EditorIcons.AddPhotoAlternate, "Aggiungi immagine") { onEnter("Aggiungi foto") }
            ToolbarIconButton(EditorIcons.PermMedia, "Aggiungi album") { onEnter("Aggiungi album") }
        }
        "Aggiungi foto" -> {
            ToolbarIconButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_scissor),
                contentDescription = "Crop"
            ) { onEnter("Crop") }   // non apre menu; segnala un'azione

            IconDropdown(
                icon = ImageVector.vectorResource(id = R.drawable.ic_adapt),
                contentDescription = "Adatta",
                current = get("fitCont") ?: "Cover",
                options = listOf("Cover", "Contain", "Stretch"),
                onSelected = { onPick("fitCont", it) }
            )

            IconDropdown(
                icon = ImageVector.vectorResource(id = R.drawable.ic_filter),
                contentDescription = "Filtro",
                current = get("filtro") ?: "Nessuno",
                options = listOf("Nessuno", "B/N", "Seppia"),
                onSelected = { onPick("filtro", it) }
            )
            IconDropdown(EditorIcons.Layout, "Cornice",
                current = get("frame") ?: "Sottile",
                options = listOf("Nessuna", "Sottile", "Marcata"),
                onSelected = { onPick("frame", it) }
            )
        }
        "Aggiungi album" -> {
            ToolbarIconButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_scissor),
                contentDescription = "Crop"
            ) { onEnter("Crop") }   // non apre menu; segnala un'azione
            IconDropdown(EditorIcons.Layout, "Cornice",
                current = get("frameAlbum") ?: "Sottile",
                options = listOf("Nessuna", "Sottile", "Marcata"),
                onSelected = { onPick("frameAlbum", it) }
            )
            IconDropdown(EditorIcons.Layout, "Filtri album",
                current = get("filtroAlbum") ?: "Nessuno",
                options = listOf("Nessuno", "Tutte foto stesso filtro"),
                onSelected = { onPick("filtroAlbum", it) }
            )
            IconDropdown(EditorIcons.Layout, "Adattamento",
                current = get("fit") ?: "Cover",
                options = listOf("Cover", "Contain", "Fill"),
                onSelected = { onPick("fit", it) }
            )
            IconDropdown(EditorIcons.Layout, "Animazione",
                current = get("anim") ?: "Slide",
                options = listOf("Slide", "Fade", "Page flip"),
                onSelected = { onPick("anim", it) }
            )
            IconDropdown(EditorIcons.Layout, "Velocità ",
                current = get("speed") ?: "Media",
                options = listOf("Lenta", "Media", "Veloce"),
                onSelected = { onPick("speed", it) }
            )
        }
    }
}


/* ---------- CONTENITORE ---------- */
@Composable
private fun ContainerLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onEnter: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onPick: (String, String) -> Unit,
    saved: Map<String, MutableList<String>>,
    onFreeGate: () -> Unit
) {
    val isFree = LocalIsFree.current
    fun get(keyLeaf: String) = selections[key(path, keyLeaf)] as? String
    when (path.getOrNull(1)) {
        null -> {
            fun get(keyLeaf: String) = selections[key(path, keyLeaf)] as? String

            ToolbarIconButton(EditorIcons.Color, "Colore") { onEnter("Colore") }
            ToolbarIconButton(EditorIcons.Image, "Immagini") { onEnter("Immagini") }
            ToolbarIconButton(EditorIcons.Square, "Angoli") { onEnter("Angoli") }

            IconDropdown(EditorIcons.SwipeVertical, "Scrollabilità ",
                current = get("scroll") ?: "Assente",
                options = listOf("Assente", "Verticale", "Orizzontale"),
                onSelected = { onPick("scroll", it) }
            )
            IconDropdown(EditorIcons.Square, "Shape",
                current = get("shape") ?: "Rettangolo",
                options = listOf("Rettangolo", "Cerchio", "Pillola", "Diamante"),
                onSelected = { onPick("shape", it) }
            )

            IconDropdown(EditorIcons.Variant, "Variant",
                current = get("variant") ?: "Full",
                options = listOf("Full", "Outlined", "Text", "TopBottom"),
                onSelected = { onPick("variant", it) }
            )
            IconDropdown(EditorIcons.LineWeight, "b_tick",
                current = get("b_thick") ?: "1dp",
                options = listOf("0dp", "1dp", "2dp", "3dp"),
                onSelected = { onPick("b_thick", it) }
            )
            IconDropdown(EditorIcons.SwipeRight, "Tipo",
                current = get("tipo") ?: "Normale",
                options = listOf("Normale", "Sfogliabile", "Tab"),
                onSelected = { onPick("tipo", it) }
            )


            IconDropdown(
                icon = Icons.Outlined.BookmarkAdd,
                contentDescription = "Scegli default",
                current = (selections[key(path, "default")] as? String) ?: saved["Testo"]?.firstOrNull(),
                options = saved["Testo"].orEmpty(),
                onSelected = { onPick("default", it) },
                locked = isFree,
                onLockedAttempt = onFreeGate
            )

            IconDropdown(
                icon = ImageVector.vectorResource(id = R.drawable.ic_style),
                contentDescription = "Stile",
                current = (selections[key(path, "style")] as? String) ?: "Nessuno",
                options = saved["Testo"].orEmpty(),
                onSelected = { onPick("style", it) }
            )

        }
        "Colore" -> {
            val isFree = LocalIsFree.current

            IconDropdown(EditorIcons.Colors1, "Colore 1",
                current = get("col1") ?: "Bianco",
                options = if (isFree) listOf("Bianco", "Grigio", "Nero") else listOf("Bianco","Grigio","Nero","Ciano"),
                onSelected = { onPick("col1", it) }
            )

            IconDropdown(EditorIcons.Colors2, "Colore 2",
                current = get("col2") ?: "Grigio chiaro",
                options = listOf("Grigio chiaro", "Blu", "Verde", "Arancio"),
                onSelected = { onPick("col2", it) },
                locked = isFree,
                onLockedAttempt = onFreeGate
            )

            IconDropdown(EditorIcons.Gradient, "Gradiente",
                current = get("grad") ?: "Orizzontale",
                options = listOf("Orizzontale", "Verticale"),
                onSelected = { onPick("grad", it) },
                locked = isFree,
                onLockedAttempt = onFreeGate
            )
            IconDropdown(EditorIcons.Functions, "FX",
                current = get("fx") ?: "Vignettatura",
                options = listOf("Vignettatura", "Noise", "Strisce"),
                onSelected = { onPick("fx", it) }
            )
        }
        "Angoli" -> {
            val dpOpts = listOf("0dp","4dp","8dp","12dp","16dp","24dp")
            IconDropdown(EditorIcons.Square, "ic_as",
                current = get("ic_as") ?: "0dp", options = dpOpts,
                onSelected = { onPick("ic_as", it) }
            )
            IconDropdown(EditorIcons.Square, "ic_ad",
                current = get("ic_ad") ?: "0dp", options = dpOpts,
                onSelected = { onPick("ic_ad", it) }
            )
            IconDropdown(EditorIcons.Square, "ic_bs",
                current = get("ic_bs") ?: "0dp", options = dpOpts,
                onSelected = { onPick("ic_bs", it) }
            )
            IconDropdown(EditorIcons.Square, "ic_bd",
                current = get("ic_bd") ?: "0dp", options = dpOpts,
                onSelected = { onPick("ic_bd", it) }
            )
        }
        "Immagini" -> {
            ToolbarIconButton(EditorIcons.AddPhotoAlternate, "Aggiungi immagine") { onEnter("Aggiungi foto") }
            ToolbarIconButton(EditorIcons.PermMedia, "Aggiungi album") { onEnter("Aggiungi album") }
        }
        "Aggiungi foto" -> {
            // Upload immagine (ic_uplo_photo) — uso un’icona già presente per evitare errori di risorsa
            ToolbarIconButton(
                icon = EditorIcons.AddPhotoAlternate,
                contentDescription = "Upload immagine"
            ) { onEnter("Upload") }

            // Ritaglia (ic_scissor): riapre il cropper per modificare il ritaglio
            ToolbarIconButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_scissor),
                contentDescription = "Crop"
            ) { onEnter("Crop") }

            // Adatta (ic_adapt): Cover/Contain/Stretch
            IconDropdown(
                icon = ImageVector.vectorResource(id = R.drawable.ic_adapt),
                contentDescription = "Adatta",
                current = get("fitCont") ?: "Cover",
                options = listOf("Cover", "Contain", "Stretch"),
                onSelected = { onPick("fitCont", it) }
            )

            // Filtro (ic_filter): Nessuno / B/N / Seppia
            IconDropdown(
                icon = ImageVector.vectorResource(id = R.drawable.ic_filter),
                contentDescription = "Filtro",
                current = get("filtro") ?: "Nessuno",
                options = listOf("Nessuno", "B/N", "Seppia"),
                onSelected = { onPick("filtro", it) }
            )

            // Cancella (ic_cancel): avvisa e rimuove la foto
            ToolbarIconButton(
                icon = EditorIcons.Cancel,
                contentDescription = "Cancella immagine"
            ) { onEnter("Cancella immagine") }
        }

        "Aggiungi album" -> {
            ToolbarIconButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_scissor),
                contentDescription = "Crop"
            ) { onEnter("Crop") }   // non apre menu; segnala un'azione
            IconDropdown(EditorIcons.Layout, "Cornice",
                current = get("frameAlbum") ?: "Sottile",
                options = listOf("Nessuna", "Sottile", "Marcata"),
                onSelected = { onPick("frameAlbum", it) }
            )
            IconDropdown(EditorIcons.Layout, "Filtri album",
                current = get("filtroAlbum") ?: "Nessuno",
                options = listOf("Nessuno", "Tutte foto stesso filtro"),
                onSelected = { onPick("filtroAlbum", it) }
            )
            IconDropdown(EditorIcons.Layout, "Adattamento",
                current = get("fit") ?: "Cover",
                options = listOf("Cover", "Contain", "Fill"),
                onSelected = { onPick("fit", it) }
            )
            IconDropdown(EditorIcons.Layout, "Animazione",
                current = get("anim") ?: "Slide",
                options = listOf("Slide", "Fade", "Page flip"),
                onSelected = { onPick("anim", it) }
            )
            IconDropdown(EditorIcons.Layout, "Velocità ",
                current = get("speed") ?: "Media",
                options = listOf("Lenta", "Media", "Veloce"),
                onSelected = { onPick("speed", it) }
            )
        }
    }
}

/* ---------- TESTO ---------- */
@Composable
private fun TextLevel(
    path: List<String>,
    selections: MutableMap<String, Any?>,
    onToggle: (String, Boolean) -> Unit,
    onPick: (String, String) -> Unit,
    saved: Map<String, MutableList<String>>,
    onFreeGate: () -> Unit                 // ⬅️ NEW
) {
    val isFree = LocalIsFree.current
// toggles (bordo più spesso se selezionati)
    val uKey = key(path, "Sottolinea")
    val iKey = key(path, "Corsivo")
// Sottolinea
    ToggleIcon(
        selected = (selections[uKey] as? Boolean) == true,
        onClick = { onToggle("Sottolinea", !((selections[uKey] as? Boolean) == true)) },
        icon = EditorIcons.Underline
    )
// Corsivo
    ToggleIcon(
        selected = (selections[iKey] as? Boolean) == true,
        onClick = { onToggle("Corsivo", !((selections[iKey] as? Boolean) == true)) },
        icon = EditorIcons.Italic
    )

// dropdown (font / weight / size / evidenzia / colore) — chiavi allineate a keysForRoot("Testo")
    IconDropdown(EditorIcons.Highlight, "Evidenzia",
        current = (selections[key(path, "Evidenzia")] as? String) ?: "Nessuna",
        options = listOf("Nessuna", "Marker", "Oblique", "Scribble"),
        onSelected = { onPick("Evidenzia", it) }
    )
    IconDropdown(EditorIcons.CustomTypography, "Font",
        current = (selections[key(path, "Font")] as? String) ?: "System",
        options = listOf("System", "Inter", "Roboto", "SF Pro"),
        onSelected = { onPick("Font", it) }
    )
    IconDropdown(EditorIcons.Bold, "Peso",
        current = (selections[key(path, "Weight")] as? String) ?: "Regular",
        options = listOf("Light", "Regular", "Medium", "Bold"),
        onSelected = { onPick("Weight", it) }
    )
    IconDropdown(EditorIcons.Size, "Size",
        current = (selections[key(path, "Size")] as? String) ?: "16sp",
        options = listOf("12sp", "14sp", "16sp", "18sp", "22sp"),
        onSelected = { onPick("Size", it) }
    )
    IconDropdown(EditorIcons.Brush, "Colore",
        current = (selections[key(path, "Colore")] as? String) ?: "Nero",
        options = listOf("Nero", "Bianco", "Blu", "Verde", "Rosso"),
        onSelected = { onPick("Colore", it) }
    )

    IconDropdown(
        icon = Icons.Outlined.BookmarkAdd,
        contentDescription = "Scegli default",
        current = (selections[key(path, "default")] as? String) ?: saved["Testo"]?.firstOrNull(),
        options = saved["Testo"].orEmpty(),
        onSelected = { onPick("default", it) },
        locked = isFree,
        onLockedAttempt = onFreeGate
    )

    IconDropdown(
        icon = ImageVector.vectorResource(id = R.drawable.ic_style),
        contentDescription = "Stile",
        current = (selections[key(path, "style")] as? String) ?: "Nessuno",
        options = saved["Testo"].orEmpty(),
        onSelected = { onPick("style", it) }
    )

    /* Variante senza risorsa:
       IconDropdown(
           icon = Icons.Outlined.Style,
           contentDescription = "Stile",
           current = (selections[key(path, "style")] as? String) ?: "Nessuno",
           options = saved["Testo"].orEmpty(),
           onSelected = { onPick("style", it) }
       )
       */
}


/* =========================================================================================
*  WIDGET MENU — pulsanti a icona, toggle con bordo spesso, dropdown con badge
* ========================================================================================= */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    infoTitle: String? = null,
    infoBody: String? = null,
    allowLongPressInInfo: Boolean = true,
    locked: Boolean = false,                         // ⟵ NEW
    onLockedAttempt: (() -> Unit)? = null,           // ⟵ NEW
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF1B2334),
        contentColor = Color.White,
        shape = CircleShape,
        tonalElevation = if (selected) 6.dp else 0.dp,
        shadowElevation = if (selected) 6.dp else 0.dp,
        modifier = modifier.size(42.dp)
    ) {
        val info = LocalInfoMode.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    enabled = enabled,
                    onClick = {
                        if (locked && !info.enabled) { onLockedAttempt?.invoke(); return@combinedClickable }
                        if (info.enabled) info.show(infoTitle ?: contentDescription, infoBody ?: "—")
                        else onClick()
                    },
                    onLongClick = {
                        if (locked && !info.enabled) { onLockedAttempt?.invoke(); return@combinedClickable }
                        if (info.enabled) {
                            if (allowLongPressInInfo) (onLongPress ?: onClick).invoke()
                            else info.show(infoTitle ?: contentDescription, infoBody ?: "—")
                        } else (onLongPress ?: onClick).invoke()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}


@Composable
private fun ToggleIcon(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF1B2334),
        contentColor = Color.White,
        tonalElevation = if (selected) 6.dp else 0.dp,
        shadowElevation = if (selected) 6.dp else 0.dp,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null,
        modifier = Modifier.size(42.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = null)
        }
    }
}

@Composable
private fun IconDropdown(
    icon: ImageVector,
    contentDescription: String,
    current: String?,
    options: List<String>,
    onSelected: (String) -> Unit,
    locked: Boolean = false,
    onLockedAttempt: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ToolbarIconButton(icon, contentDescription,
            locked = locked,
            onLockedAttempt = onLockedAttempt
        ) { expanded = true }
// badge numerico angolare per "Colore 1"/"Colore 2"
        if (!current.isNullOrBlank()) {
            Surface(
                color = Color(0xFF22304B),
                contentColor = Color.White,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, 14) } // poco sotto l'icona
            ) {
                Text(
                    text = current,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

/* =========================================================================================
*  MINI CONFIRM BAR
* ========================================================================================= */

@Composable
private fun BoxScope.ConfirmBar(
    onCancel: () -> Unit,
    onOk: () -> Unit,
    onSavePreset: () -> Unit
) {
    Surface(
        color = Color(0xFF0B1220),
        contentColor = Color.White,
        tonalElevation = 10.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Salvare le modifiche?",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium
            )
// icone-only
            ToolbarIconButton(EditorIcons.Cancel, "Annulla", onClick = onCancel)
            ToolbarIconButton(Icons.Outlined.BookmarkAdd, "Salva impostazioni", onClick = onSavePreset)
            ToolbarIconButton(EditorIcons.Ok, "OK", onClick = onOk, selected = true)
        }
    }
}

/* =========================================================================================
*  UTILITY
* ========================================================================================= */

private fun key(path: List<String>, leaf: String) = (path + leaf).joinToString(" / ")

@Composable
private fun dividerDot() {
    Box(
        Modifier
            .padding(horizontal = 6.dp)
            .size(6.dp)
            .clip(CircleShape)
            .background(Color(0xFF233049))
    )
}

// ======================================================================
// WIZARD OVERLAY — crea Pagina / Menù Laterale / Menù Centrale / Avviso
// Aspetto scuro, barre coperte da scrim, nessuna logica esterna.
// ======================================================================
private data class CreationResult(
    val kind: DeckRoot,
    val id: String,
    val title: String,
    val description: String?,
    val scroll: String,           // "Assente" | "Verticale" | "Orizzontale"
    val assocId: String?,         // eventuale associazione
    val side: String?,            // solo per menù laterale: "Sinistra"|"Destra"|"Alto"|"Basso"
    val setAsHome: Boolean        // solo per pagine
)

@Composable
private fun BoxScope.CreationWizardOverlay(
    visible: Boolean,
    target: DeckRoot?,
    existingIds: Set<String>,      // • NEW
    onDismiss: () -> Unit,
    onCreate: (WizardResult) -> Unit
) {
    if (!visible) return
    val darkBg  = Color(0xFF0D1117) // stile GitHub scuro
    val panelBg = Color(0xFF131A24)
    var idError by remember { mutableStateOf(false) }

    var showIdHelp by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center),
        color = darkBg.copy(alpha = 0.98f),
        contentColor = Color.White
    ) {
// stato campi
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var id by remember { mutableStateOf("") }
        var idEdited by remember { mutableStateOf(false) }

// associazione (ora dietro flag)
        var assocEnabled by remember { mutableStateOf(false) }
        var assocMode by remember { mutableStateOf("manual") } // "manual" | "tap3s"
        var assocId by remember { mutableStateOf("") }

// specifici per Pagine
        var scroll by remember { mutableStateOf("Nessuna") } // Nessuna | Verticale | Orizzontale
        var setAsHome by remember { mutableStateOf(false) }

// specifici per Menù laterale
        var side by remember { mutableStateOf("Sinistra") } // Sinistra | Destra | Alto | Basso

        // regole ID auto
        fun prefixFor(root: DeckRoot?) = when (root) {
            DeckRoot.PAGINA -> "pg"
            DeckRoot.MENU_LATERALE -> "ml"
            DeckRoot.MENU_CENTRALE -> "mc"
            DeckRoot.AVVISO -> "al"
            else -> "pg"
        }
        fun sanitize(s: String) = s.filter { it.isLetterOrDigit() }
        fun autoIdFrom(name: String, root: DeckRoot?): String {
            val p = prefixFor(root)
            val base = sanitize(name).lowercase()
            val candidate = if (base.length >= 5) base.take(8) else (base + "001").take(8)
            val withPrefix = (p + candidate).take(8)
            return if (withPrefix.length < 5) (withPrefix + "0".repeat(5 - withPrefix.length)) else withPrefix
        }
        LaunchedEffect(name, target) {
            if (!idEdited) id = autoIdFrom(name, target)
        }

// header
        Column(Modifier.fillMaxSize()) {
            Surface(color = panelBg, tonalElevation = 6.dp, shadowElevation = 8.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Chiudi")
                    }
                    val title = when (target) {
                        DeckRoot.PAGINA -> "Nuova pagina"
                        DeckRoot.MENU_LATERALE -> "Nuovo menù laterale"
                        DeckRoot.MENU_CENTRALE -> "Nuovo menù centrale"
                        DeckRoot.AVVISO -> "Nuovo avviso"
                        else -> "Nuovo elemento"
                    }
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showIdHelp = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Regole ID")
                    }
                }
            }

// pannello centrale
            Surface(
                color = panelBg,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
// --- Associazione (fissa in alto, fuori dallo scorrimento dei campi) ---
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Checkbox(
                            checked = assocEnabled,
                            onCheckedChange = { assocEnabled = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = WIZ_AZURE,
                                checkmarkColor = Color.Black
                            )
                        )
                        Text("Abilita associazione")
                    }
                    if (assocEnabled) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = assocMode == "manual",
                                    onClick = { assocMode = "manual" },
                                    colors = RadioButtonDefaults.colors(selectedColor = WIZ_AZURE)
                                )
                                Text("ID manuale")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = assocMode == "tap3s",
                                    onClick = { assocMode = "tap3s" },
                                    colors = RadioButtonDefaults.colors(selectedColor = WIZ_AZURE)
                                )
                                Text("Seleziona a schermo (3s)")
                            }
                        }
                        if (assocMode == "manual") {
                            OutlinedTextField(
                                value = assocId,
                                onValueChange = { assocId = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' }.take(32) },
                                singleLine = true,
                                label = { Text("ID elemento associato") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    cursorColor = WIZ_AZURE,
                                    focusedIndicatorColor = WIZ_AZURE,
                                    unfocusedIndicatorColor = Color(0xFF2A3B5B),
                                    focusedLabelColor = WIZ_AZURE,
                                    unfocusedLabelColor = Color(0xFF9BA3AF)
                                )
                            )
                        } else {
                            Text(
                                "Tieni premuto 3s sul componente desiderato (modalità  stub).",
                                fontSize = 12.sp,
                                color = Color(0xFF9BA3AF)
                            )
                            OutlinedButton(
                                onClick = { /* TODO: abilita selezione */ },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WIZ_AZURE),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WIZ_AZURE)
                            ) { Text("Avvia selezione") }
                        }
                    }

// --- Campi (scroll verticale) ---
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = WIZ_AZURE,
                                focusedIndicatorColor = WIZ_AZURE,
                                unfocusedIndicatorColor = Color(0xFF2A3B5B),
                                focusedLabelColor = WIZ_AZURE,
                                unfocusedLabelColor = Color(0xFF9BA3AF)
                            )
                        )
                        OutlinedTextField(
                            value = id,
                            onValueChange = {
                                id = it.lowercase()
                                    .replace(' ', '_')
                                    .filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' }
                                    .take(15)          // fino a 15 come da tue regole
                                idEdited = true
                                idError = false       // reset visuale all'editing
                            },
                            label = { Text("ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = idError,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = WIZ_AZURE,
                                focusedIndicatorColor = if (idError) Color.Red else WIZ_AZURE,
                                unfocusedIndicatorColor = if (idError) Color.Red else Color(0xFF2A3B5B),
                                focusedLabelColor = if (idError) Color.Red else WIZ_AZURE,
                                unfocusedLabelColor = if (idError) Color.Red else Color(0xFF9BA3AF)
                            )
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descrizione (opzionale)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = WIZ_AZURE,
                                focusedIndicatorColor = WIZ_AZURE,
                                unfocusedIndicatorColor = Color(0xFF2A3B5B),
                                focusedLabelColor = WIZ_AZURE,
                                unfocusedLabelColor = Color(0xFF9BA3AF)
                            )
                        )
// Campi specifici per tipo
                        when (target) {
                            DeckRoot.PAGINA -> {
                                Text("Opzioni pagina", fontWeight = FontWeight.SemiBold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    Text("Scrollabilità :")
                                    OptionPill(selected = scroll == "Nessuna", onClick = { scroll = "Nessuna" }, label = "Nessuna")
                                    OptionPill(selected = scroll == "Verticale", onClick = { scroll = "Verticale" }, label = "Verticale")
                                    OptionPill(selected = scroll == "Orizzontale", onClick = { scroll = "Orizzontale" }, label = "Orizzontale")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Switch(
                                        checked = setAsHome,
                                        onCheckedChange = { setAsHome = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = WIZ_AZURE,
                                            checkedTrackColor = WIZ_AZURE.copy(alpha = 0.4f)
                                        )
                                    )
                                    Text("Imposta come Home")
                                }
                            }
                            DeckRoot.MENU_LATERALE -> {
                                Text("Opzioni menù laterale", fontWeight = FontWeight.SemiBold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    Text("Lato:")
                                    OptionPill(selected = side == "Sinistra", onClick = { side = "Sinistra" }, label = "Sinistra")
                                    OptionPill(selected = side == "Destra",   onClick = { side = "Destra"   }, label = "Destra")
                                    OptionPill(selected = side == "Alto",     onClick = { side = "Alto"     }, label = "Alto")
                                    OptionPill(selected = side == "Basso",    onClick = { side = "Basso"    }, label = "Basso")
                                }
                            }
                            DeckRoot.MENU_CENTRALE -> {
                                Text("Opzioni menù centrale", fontWeight = FontWeight.SemiBold)
                                Text("Nessuna opzione speciale per ora.", color = Color(0xFF9BA3AF), fontSize = 12.sp)
                            }
                            DeckRoot.AVVISO -> {
                                Text("Opzioni avviso", fontWeight = FontWeight.SemiBold)
                                OutlinedButton(
                                    onClick = { /* apri ECA mode (stub) */ },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WIZ_AZURE),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, WIZ_AZURE)
                                ) {
                                    Text("Apri Event-Condition-Action mode (stub)")
                                }
                            }
                            else -> Unit
                        }
                    }

// footer fisso
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WIZ_AZURE),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, WIZ_AZURE)
                        ) {
                            Text("Annulla")
                        }
                        Button(
                            onClick = {
// Costruzione ID finale secondo le tue regole attuali del file (con take(15) se vuoi estenderle qui)
                                val computedId = if (id.isNotBlank()) id else /* autoIdFrom(name, target) o tua logica */
                                    (when (target) {
                                        DeckRoot.PAGINA        -> "pg"
                                        DeckRoot.MENU_LATERALE -> "ml"
                                        DeckRoot.MENU_CENTRALE -> "mc"
                                        DeckRoot.AVVISO        -> "al"
                                        else -> "pg"
                                    } + "-" + name.lowercase()
                                        .replace(' ', '_')
                                        .filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' }
                                        .take(15)
                                            ).ifBlank {
// fallback se nome corto/vuoto: prefisso-00001 (basic, non incrementale qui)
                                            val pref = when (target) {
                                                DeckRoot.PAGINA        -> "pg"
                                                DeckRoot.MENU_LATERALE -> "ml"
                                                DeckRoot.MENU_CENTRALE -> "mc"
                                                DeckRoot.AVVISO        -> "al"
                                                else -> "pg"
                                            }
                                            "${pref}-00001"
                                        }

                                val finalId = computedId
                                val finalName = if (name.isBlank()) finalId else name

                                if (existingIds.contains(finalId)) {
                                    idError = true
                                    return@Button
                                }

                                onCreate(
                                    WizardResult(
                                        root = target ?: DeckRoot.PAGINA,
                                        id = finalId,
                                        name = finalName,
                                        description = description.ifBlank { "n/a" },
                                        assocId = if (assocEnabled && assocMode == "manual" && assocId.isNotBlank()) assocId else null,
                                        scroll = scroll,
                                        setAsHome = (target == DeckRoot.PAGINA && setAsHome),
                                        side = if (target == DeckRoot.MENU_LATERALE) side else null
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = WIZ_AZURE, contentColor = Color.Black)
                        ) {
                            Text("Crea")
                        }

                    }
                }
            }

// Dialog help regole ID
            if (showIdHelp) {
                AlertDialog(
                    onDismissRequest = { showIdHelp = false },
                    title = { Text("Regole per l'ID") },
                    text = {
                        Text(
                            "• Se compili l'ID, viene usato quello.\n" +
                                    "• Altrimenti: prefisso per tipo (pg/ml/mc/al) + prime 5 lettere/cifre del Nome (solo alfanumerici).\n" +
                                    "• Se Nome è corto o vuoto: il sistema completa fino a 5-8 caratteri.\n" +
                                    "• L'ID deve essere univoco."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showIdHelp = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = WIZ_AZURE)
                        ) { Text("OK") }
                    }
                )
            }
        }
    }
}


@Composable
private fun OptionPill(selected: Boolean, onClick: () -> Unit, label: String) {
    val borderClr = if (selected) WIZ_AZURE else Color(0xFF2A3B5B)
    val txtClr    = if (selected) WIZ_AZURE else Color.White
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(36.dp)
            .padding(vertical = 0.dp),
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, borderClr),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = txtClr)
    ) {
        Text(label, maxLines = 1, softWrap = false)
    }
}


private data class WizardResult(
    val root: DeckRoot,
    val id: String,
    val name: String,
    val description: String,
    val assocId: String?,
    val scroll: String,
    val setAsHome: Boolean = false,
    val side: String? = null
)


// Piccolo dropdown "scuro" in linea con lo stile
@Composable
private fun DropdownSmall(
    current: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(current) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelected(opt); open = false })
            }
        }
    }
}

// Chip minimale, aspetto scuro, niente API sperimentali
@Composable
private fun FilterChipLike(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    val bg = if (selected) Color(0xFF22304B) else Color(0xFF1B2334)
    Surface(
        color = bg,
        contentColor = Color.White,
        shape = RoundedCornerShape(12.dp)
    ) {
        TextButton(onClick = onClick) {
            Text(label)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxScope.InfoEdgeDeck(
    open: Boolean,
    onToggleOpen: () -> Unit,
    infoEnabled: Boolean,
    onToggleInfo: () -> Unit,
    enabled: Boolean = true,

    // già presenti
    gridEnabled: Boolean,
    onToggleGrid: () -> Unit,
    levelEnabled: Boolean,
    onToggleLevel: () -> Unit,
    currentLevel: Int,

    // NUOVI parametri
    isContainerContext: Boolean,          // true <=> nel menù "Contenitore"
    toolMode: com.example.appbuilder.canvas.ToolMode,
    onCycleMode: () -> Unit               // cambia modalità senza chiudere il menù
) {
    // --- estetica ---
    val tileSize  = 56.dp
    val spacing   = 10.dp
    val corner    = 12.dp
    val peekWidth = 12.dp

    // larghezza animata
    val targetWidth = if (open) (tileSize + spacing + peekWidth) else peekWidth
    val width by animateDpAsState(targetValue = targetWidth, animationSpec = tween(220), label = "sideWidth")

    // visibilità a cascata
    var show1 by remember(open) { mutableStateOf(false) } // "?"
    var show2 by remember(open) { mutableStateOf(false) } // grid
    var show3 by remember(open) { mutableStateOf(false) } // level
    var show4 by remember(open) { mutableStateOf(false) } // mode (NUOVO)
    var show5 by remember(open) { mutableStateOf(false) } // gear
    LaunchedEffect(open) {
        if (open) {
            show1 = true; delay(50)
            show2 = true; delay(50)
            show3 = true; delay(50)
            show4 = true; delay(50)
            show5 = true
        } else {
            show5 = false; delay(40)
            show4 = false; delay(40)
            show3 = false; delay(40)
            show2 = false; delay(40)
            show1 = false
        }
    }

    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .width(width)
            .fillMaxHeight()
    ) {
        // fascia “peek” sempre visibile
        val shadeAlpha = if (open) 0.08f else 0.25f
        val peek = Modifier
            .align(Alignment.CenterEnd)
            .width(peekWidth)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x66000000).copy(alpha = shadeAlpha),
                        Color(0x00000000),
                        Color(0x66000000).copy(alpha = shadeAlpha)
                    )
                )
            )
            .pointerInput(open, isContainerContext) {
                detectHorizontalDragGestures { _, dx ->
                    // apertura: trascino a sinistra quando è chiuso
                    if (!open && dx < -18f) onToggleOpen()
                    // chiusura: sempre trascinando a destra
                    if (open && dx > 18f) onToggleOpen()
                }
            }
            .combinedClickable(
                // in "Contenitore" non chiudo/apro col tap (si apre già in automatico)
                onClick = { if (!isContainerContext) onToggleOpen() },
                onLongClick = { if (!isContainerContext) onToggleOpen() }
            )

        Box(peek) {} // disegna la fascia

        if (open) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(spacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1) "?" info
                AnimatedVisibility(
                    visible = show1,
                    enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.85f),
                    exit  = fadeOut(tween(120)) + scaleOut(tween(120))
                ) {
                    SquareTile(
                        size = tileSize,
                        corner = corner,
                        icon = Icons.Outlined.HelpOutline,
                        tint = when {
                            !enabled     -> Color(0xFF6B7280)
                            infoEnabled  -> WIZ_AZURE
                            else         -> Color.White
                        },
                        enabled = enabled,
                        onClick = {
                            if (enabled) {
                                onToggleInfo()
                                onToggleOpen() // chiudo dopo tap (comportamento storico)
                            }
                        }
                    )
                }

                // 2) griglia
                AnimatedVisibility(
                    visible = show2,
                    enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.85f),
                    exit  = fadeOut(tween(120)) + scaleOut(tween(120))
                ) {
                    SquareTile(
                        size = tileSize,
                        corner = corner,
                        icon = Icons.Outlined.GridOn, // niente vectorResource/try-catch
                        tint = Color.White,
                        enabled = true,
                        border = if (gridEnabled) BorderStroke(2.dp, WIZ_AZURE) else null,
                        onClick = {
                            onToggleGrid()
                            onToggleOpen() // richiudo (comportamento storico)
                        }
                    )
                }

                // 3) livelli
                AnimatedVisibility(
                    visible = show3,
                    enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.85f),
                    exit  = fadeOut(tween(120)) + scaleOut(tween(120))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // badge numerico a sinistra
                        Surface(
                            color = Color(0xFF0F141E),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            tonalElevation = 4.dp,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (-28).dp)
                        ) {
                            Text(
                                currentLevel.toString(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                fontSize = 12.sp
                            )
                        }
                        SquareTile(
                            size = tileSize,
                            corner = corner,
                            icon = Icons.Outlined.Layers, // più portabile di “Stairs”
                            tint = Color.White,
                            enabled = true,
                            border = if (levelEnabled) BorderStroke(2.dp, WIZ_AZURE) else null,
                            onClick = {
                                onToggleLevel()
                                onToggleOpen() // richiudo (comportamento storico)
                            }
                        )
                    }
                }

                // 4) NUOVO — pulsante "modalità"
                AnimatedVisibility(
                    visible = show4,
                    enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.85f),
                    exit  = fadeOut(tween(120)) + scaleOut(tween(120))
                ) {
                    // icona in base alla modalità
                    val modeIcon: ImageVector = when (toolMode) {
                        com.example.appbuilder.canvas.ToolMode.Create -> Icons.Outlined.AddBox   // ic_createcontainer
                        com.example.appbuilder.canvas.ToolMode.Point  -> Icons.Outlined.TouchApp// ic_point
                        com.example.appbuilder.canvas.ToolMode.Grab   -> Icons.Outlined.OpenWith// ic_grab
                        com.example.appbuilder.canvas.ToolMode.Resize -> Icons.Outlined.Crop    // ic_resize
                    }
                    SquareTile(
                        size = tileSize,
                        corner = corner,
                        icon = modeIcon,
                        tint = if (isContainerContext) WIZ_AZURE else Color(0xFF6B7280),
                        enabled = isContainerContext,               // cliccabile SOLO in "Contenitore"
                        border = if (isContainerContext) BorderStroke(2.dp, WIZ_AZURE) else null,
                        onClick = {
                            // NON chiudo il menù: alterno le modalità
                            if (isContainerContext) onCycleMode()
                        }
                    )
                }

                // 5) ingranaggio (stub)
                AnimatedVisibility(
                    visible = show5,
                    enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.85f),
                    exit  = fadeOut(tween(120)) + scaleOut(tween(120))
                ) {
                    SquareTile(
                        size = tileSize,
                        corner = corner,
                        icon = Icons.Outlined.Settings,
                        tint = Color.White,
                        enabled = true,
                        onClick = {
                            // TODO apri impostazioni
                            onToggleOpen()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SquareTile(
    size: Dp,
    corner: Dp,
    icon: ImageVector,
    tint: Color,
    enabled: Boolean,
    border: BorderStroke? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(corner),
        color = Color(0xFF0F141E),
        contentColor = tint,
        border = border,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null)
        }
    }
}


@Composable
private fun BoxScope.InfoToastCard(
    visible: Boolean,
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit  = fadeOut(tween(250)),
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        Surface(
            color = Color(0xFF0F141E),
            contentColor = Color.White,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, WIZ_AZURE),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .padding(top = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(body, color = Color(0xFF9BA3AF))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = WIZ_AZURE)
                    ) { Text("OK") }
                }
            }
        }
    }
}
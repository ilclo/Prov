package com.example.appbuilder.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/* ---------------------------------------------------------------------------------------------- */
/* Palette locali (indipendenti da EditorKit)                                                     */
/* ---------------------------------------------------------------------------------------------- */

private val AZURE = Color(0xFF3DA5FF)           // azzurro coerente con evidenziazioni
private val PANEL_BG_DARK = Color(0xFF0E1524)   // dark “GitHub-like”
private val PANEL_STROKE = Color(0xFF2A3B5B)

/* ---------------------------------------------------------------------------------------------- */
/* Overlay: Slider grammatura                                                                     */
/* ---------------------------------------------------------------------------------------------- */

/**
 * Pannello centrale per regolare la “grammatura” della pagina (densità griglia).
 * Mostra uno slider azzurro con alone, sfondo scurito/blur dello stage,
 * e callback di update live (onChange) + chiusura (onClose).
 *
 * @param value           valore normalizzato [0f..1f] → mappalo tu a righe/colonne
 * @param onValueChange   callback destinata ad aggiornare la preview (spotlight cell)
 */
@Composable
fun BoxScope.GridOverlayPanel(
    visible: Boolean,
    value: Float,
    onValueChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // sfondo scurito + leggero blur
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .blur(
                    radius = 12.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded
                )
                .padding(24.dp)
        ) {
            val cardColor = if (isSystemInDarkTheme())
                MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            else
                PANEL_BG_DARK

            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .shadow(16.dp, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Grammatura pagina",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Regola la densità della griglia. L’anteprima evidenzia la cella (0,0).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(18.dp))

                    // Slider azzurro con alone
                    val glow by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 300, easing = FastOutSlowInEasing
                        ),
                        label = "glow"
                    )

                    Box(
                        Modifier
                            .padding(horizontal = 8.dp)
                            .border(1.dp, PANEL_STROKE, RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .blur((12 * glow).dp)
                                .background(AZURE.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        )
                        Slider(
                            value = value.coerceIn(0f, 1f),
                            onValueChange = { onValueChange(it.coerceIn(0f, 1f)) },
                            colors = SliderDefaults.colors(
                                thumbColor = AZURE,
                                activeTrackColor = AZURE,
                                inactiveTrackColor = AZURE.copy(alpha = 0.25f)
                            )
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Transparent, CircleShape)
                                .border(1.dp, PANEL_STROKE, CircleShape)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "Chiudi")
                        }
                        Text(
                            text = "Tocca fuori per chiudere",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                }
            }
        }
    }
}

/* Alias compatibili con eventuali vecchi nomi usati in EditorKit */
@Composable fun BoxScope.GramOverlayPanel(
    visible: Boolean, value: Float, onValueChange: (Float) -> Unit, onClose: () -> Unit
) = GridOverlayPanel(visible, value, onValueChange, onClose)

/* ---------------------------------------------------------------------------------------------- */
/* Overlay: Selettore livelli (slot‑machine)                                                       */
/* ---------------------------------------------------------------------------------------------- */

/**
 * Picker livelli: mostra una “colonna” di numeri e permette di fermarsi sul livello desiderato.
 * Accetta numeri anche non interi (es. 2.2) per inserimenti tra livelli contigui.
 */
@Composable
fun BoxScope.LevelsPickerPanel(
    visible: Boolean,
    currentLevel: Float,
    window: Int = 2,                          // quanti “intorni” visualizzare
    onPick: (Float) -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(12.dp)
        ) {
            val bg = if (isSystemInDarkTheme())
                MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            else
                PANEL_BG_DARK

            Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = bg),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Selettore livello",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(10.dp))

                    // Finestra di livelli: current-2 .. current+2 e insert intermedi
                    val base = currentLevel.toInt()
                    val items = buildList {
                        for (i in (base - window)..(base + window)) {
                            add(i.toFloat())
                            // Inserto a metà tra i livelli contigui (i + 0.2)
                            if (i < base + window) add(i + 0.2f)
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.requiredWidth(120.dp)
                    ) {
                        items.forEach { lv ->
                            LevelPill(
                                level = lv,
                                selected = (lv == currentLevel),
                                onClick = { onPick(lv) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Transparent, CircleShape)
                            .border(1.dp, PANEL_STROKE, CircleShape)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Chiudi")
                    }
                }
            }
        }
    }
}

/* Alias compatibili con eventuali vecchi nomi usati in EditorKit */
@Composable fun BoxScope.LevelsChooserPanel(
    visible: Boolean, currentLevel: Float, window: Int = 2, onPick: (Float) -> Unit, onClose: () -> Unit
) = LevelsPickerPanel(visible, currentLevel, window, onPick, onClose)

@Composable fun BoxScope.LevelsPickerFlyout(
    visible: Boolean, currentLevel: Float, window: Int = 2, onPick: (Float) -> Unit, onClose: () -> Unit
) = LevelsPickerPanel(visible, currentLevel, window, onPick, onClose)

/* Pillola selezione livello */
@Composable
private fun LevelPill(
    level: Float,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) AZURE.copy(alpha = 0.18f) else Color.Transparent
    val stroke = if (selected) AZURE else PANEL_STROKE
    val txt = if (selected) Color.White else Color(0xFFBFC6D4)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, stroke, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .then(Modifier)
    ) {
        Text(
            text = prettyLevel(level),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = txt,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        )
        Spacer(Modifier.width(6.dp))
        // “slot window” fittizia per comunicare l’idea
        Box(
            Modifier
                .size(width = 28.dp, height = 14.dp)
                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .border(1.dp, PANEL_STROKE, RoundedCornerShape(8.dp))
        )
    }
}

private fun prettyLevel(f: Float): String =
    if (f % 1f == 0f) f.toInt().toString() else String.format("%.1f", f)
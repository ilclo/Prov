package com.example.appbuilder.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val AZURE = Color(0xFF3DA5FF)
private val PANEL_BG_DARK = Color(0xFF0E1524)
private val PANEL_STROKE = Color(0xFF2A3B5B)

/* ---------------------------------------------------------------------------------------------- */
/* Slider Grammatura: pannello centrale con alone azzurro                                         */
/* ---------------------------------------------------------------------------------------------- */

@Composable
private fun BoxScope.GridOverlayPanel(
    visible: Boolean,
    value: Float,
    onValueChange: (Float) -> Unit,
    onClose: () -> Unit,
    onStartDrag: (() -> Unit)? = null,
    onEndDrag: (() -> Unit)? = null
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .blur(12.dp, BlurredEdgeTreatment.Unbounded)
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

                    var dragging by remember(visible) { mutableStateOf(false) }
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
                            value = value.coerceIn(1f, 64f),
                            onValueChange = {
                                if (!dragging) {
                                    dragging = true
                                    onStartDrag?.invoke()
                                }
                                onValueChange(it.coerceIn(1f, 64f))
                            },
                            onValueChangeFinished = {
                                if (dragging) {
                                    dragging = false
                                    onEndDrag?.invoke()
                                }
                            },
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

/* Alias esatti che il tuo EditorKit sta importando/usi nei call-site */
@Composable
fun BoxScope.GridSliderOverlay(
    visible: Boolean,
    value: Int,
    onStartDrag: () -> Unit,
    onValueChange: (Int) -> Unit,
    onEndDrag: () -> Unit,
    onDismiss: () -> Unit
) {
    GridOverlayPanel(
        visible = visible,
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        onClose = onDismiss,
        onStartDrag = onStartDrag,
        onEndDrag = onEndDrag
    )
}

/* ---------------------------------------------------------------------------------------------- */
/* Selettore livelli (stile “slot”) — wrapper alias                                               */
/* ---------------------------------------------------------------------------------------------- */

@Composable
private fun BoxScope.LevelsPickerPanel(
    visible: Boolean,
    currentLevel: Float,
    window: Int = 2,
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

                    val base = currentLevel.toInt()
                    val items = buildList {
                        for (i in (base - window)..(base + window)) add(i.toFloat())
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(120.dp)
                    ) {
                        items.forEach { lv ->
                            val selected = (lv == currentLevel)
                            val bgPill = if (selected) AZURE.copy(alpha = 0.18f) else Color.Transparent
                            val stroke = if (selected) AZURE else PANEL_STROKE
                            val txt = if (selected) Color.White else Color(0xFFBFC6D4)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .background(bgPill, RoundedCornerShape(12.dp))
                                    .border(1.dp, stroke, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (lv % 1f == 0f) lv.toInt().toString() else String.format("%.1f", lv),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = txt,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Divider(color = PANEL_STROKE.copy(alpha = 0.5f))
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

/* Alias esatto usato in EditorKit */
@Composable
fun BoxScope.LevelPickerOverlay(
    visible: Boolean,
    current: Int,
    minLevel: Int,
    maxLevel: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Finestra di default: +/- 2 intorno al corrente (o copri intera escursione min..max)
    val window = maxOf(2, (maxLevel - minLevel))
    LevelsPickerPanel(
        visible = visible,
        currentLevel = current.toFloat(),
        window = window,
        onPick = { onPick(it.roundToInt()) },
        onClose = onDismiss
    )
}
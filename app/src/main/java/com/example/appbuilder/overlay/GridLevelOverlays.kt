package com.example.appbuilder.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Stesso azzurro usato in EditorKit */
private val WIZ_AZURE = Color(0xFF58A6FF)

/** Valori discreti concessi per la “grammatura” (densità griglia). */
private val DENSITY_STEPS = listOf(3, 4, 6, 8, 10, 12, 16, 20, 24)

/**
 * Overlay centrale per la densità griglia.
 *
 * - Sfondo scuro semitrasparente.
 * - Pannello al centro con slider M3, accento azzurro.
 * - onStartDrag/onEndDrag per pilotare preview (cella singola) e full grid dopo 0.5s (EditorKit).
 */
@Composable
fun GridSliderOverlay(
    visible: Boolean,
    value: Int,
    onStartDrag: () -> Unit,
    onValueChange: (Int) -> Unit,
    onEndDrag: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    // Mappo il valore alla posizione nello step array
    var sentStart by remember { mutableStateOf(false) }
    val idx = DENSITY_STEPS.indexOf(value).let { if (it >= 0) it else DENSITY_STEPS.indexOf(6).coerceAtLeast(0) }
    var index by remember(value) { mutableStateOf(idx) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000)) // “sfocato” simulato con scrim scuro (blur globale non applicabile al contenuto sottostante)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0F141E),
            contentColor = Color.White,
            tonalElevation = 12.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Densità griglia", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text("${DENSITY_STEPS[index]} × ${DENSITY_STEPS[index]}", color = Color(0xFF9BA3AF))

                Slider(
                    value = index.toFloat(),
                    onValueChange = { f ->
                        val newIdx = f.toInt().coerceIn(0, DENSITY_STEPS.lastIndex)
                        if (!sentStart) { onStartDrag(); sentStart = true }
                        index = newIdx
                        onValueChange(DENSITY_STEPS[newIdx])
                    },
                    onValueChangeFinished = {
                        onEndDrag()
                        sentStart = false
                    },
                    valueRange = 0f..DENSITY_STEPS.lastIndex.toFloat(),
                    steps = (DENSITY_STEPS.size - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = WIZ_AZURE,
                        activeTrackColor = WIZ_AZURE,
                        inactiveTrackColor = Color(0xFF2A3B5B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WIZ_AZURE)
                    ) { Text("Chiudi") }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = WIZ_AZURE, contentColor = Color.Black)
                    ) { Text("OK") }
                }
            }
        }
    }
}

/**
 * Overlay selettore livelli “a slot”.
 * Mostra una colonna con snap al centro. Conferma con il bottone “Seleziona”.
 *
 * NB: adesso i livelli sono interi; l’inserimento di “mezzi livelli” tra due consecutivi
 * lo lasciamo per lo step successivo, quando passeremo a una struttura livelli più ricca.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LevelPickerOverlay(
    visible: Boolean,
    current: Int,
    minLevel: Int,
    maxLevel: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    // Costruiamo una finestra [-2, +2] intorno agli estremi noti
    val list = remember(minLevel, maxLevel) {
        val start = minLevel - 2
        val end = maxLevel + 2
        (start..end).toList()
    }
    val startIndex = list.indexOf(current).let { if (it >= 0) it else list.indexOf(minLevel).coerceAtLeast(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0F141E),
            contentColor = Color.White,
            tonalElevation = 12.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Seleziona livello", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

                val itemHeight = 40.dp
                val state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
                val fling = rememberSnapFlingBehavior(lazyListState = state)

                Box(
                    modifier = Modifier
                        .height(itemHeight * 5) // 5 righe visibili
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    // finestra centrale evidenziata
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(itemHeight)
                            .background(Color(0x3322304B))
                    )

                    LazyColumn(
                        state = state,
                        flingBehavior = fling,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = itemHeight * 2)
                    ) {
                        itemsIndexed(list) { _, lvl ->
                            Box(
                                modifier = Modifier
                                    .height(itemHeight)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    lvl.toString(),
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WIZ_AZURE)
                    ) { Text("Annulla") }

                    Button(
                        onClick = {
                            // index dell’elemento “centrale”
                            val center = state.firstVisibleItemIndex + 2
                            val pick = list.getOrNull(center) ?: current
                            onPick(pick)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = WIZ_AZURE, contentColor = Color.Black)
                    ) { Text("Seleziona") }
                }
            }
        }
    }
}

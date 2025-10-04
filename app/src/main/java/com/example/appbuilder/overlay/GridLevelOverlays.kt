package com.example.appbuilder.overlay

import kotlin.math.roundToInt
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
    allowedValues: List<Int> = listOf(4, 6, 8, 10, 12),
    onStartDrag: () -> Unit,
    onValueChange: (Int) -> Unit,
    onEndDrag: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000)) // scrim scuro
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

                // Mappa il "value" sull'indice della lista di passi ammessi
                val initialIndex = allowedValues.indexOf(value).let { if (it < 0) 0 else it }
                var sliderIndex by remember(value, allowedValues) { mutableStateOf(initialIndex) }
                var started by remember { mutableStateOf(false) }

                // Mostra l'attuale passo in forma "N × N"
                Text(
                    "${allowedValues[sliderIndex]} × ${allowedValues[sliderIndex]}",
                    color = Color(0xFF9BA3AF)
                )

                Slider(
                    value = sliderIndex.toFloat(),
                    onValueChange = { f ->
                        val i = f.roundToInt().coerceIn(0, allowedValues.lastIndex)
                        if (!started) { onStartDrag(); started = true }
                        if (i != sliderIndex) {
                            sliderIndex = i
                            onValueChange(allowedValues[i]) // restituisce il passo "non arbitrario"
                        }
                    },
                    onValueChangeFinished = {
                        started = false
                        onEndDrag()
                    },
                    steps = (allowedValues.size - 2).coerceAtLeast(0),
                    valueRange = 0f..allowedValues.lastIndex.toFloat(),
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WIZ_AZURE,
                            contentColor = Color.Black
                        )
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

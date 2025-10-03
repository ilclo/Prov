package com.example.appbuilder.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AZURE = Color(0xFF58A6FF)

/** Overlay centrale: slider densità griglia. */
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
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))      // scrim scuro
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0F141E),
            contentColor = Color.White,
            shape = MaterialTheme.shapes.large,
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = null, color = AZURE),
            tonalElevation = 10.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Densità griglia", style = MaterialTheme.typography.titleMedium)
                var v by remember { mutableStateOf(value.toFloat()) }
                Slider(
                    value = v,
                    onValueChange = {
                        if (!isSystemInDarkTheme()) {} // no-op, solo per evitare warning
                        onStartDrag()
                        v = it
                        onValueChange(it.toInt().coerceIn(2, 40))
                    },
                    onValueChangeFinished = onEndDrag,
                    valueRange = 2f..40f,
                    colors = SliderDefaults.colors(
                        thumbColor = AZURE,
                        activeTrackColor = AZURE,
                        inactiveTrackColor = AZURE.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.width(280.dp)
                )
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = AZURE)
                ) { Text("Chiudi") }
            }
        }
    }
}

/** Overlay “a slot” per scegliere il livello corrente. */
@Composable
fun LevelPickerOverlay(
    visible: Boolean,
    current: Int,
    minLevel: Int,
    maxLevel: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                color = Color(0xFF0F141E),
                contentColor = Color.White,
                shape = MaterialTheme.shapes.large,
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = null, color = AZURE),
                tonalElevation = 10.dp,
                shadowElevation = 10.dp,
                modifier = Modifier.widthIn(min = 220.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Livello di lavoro", style = MaterialTheme.typography.titleMedium)
                    val from = minLevel - 2
                    val to = maxLevel + 2
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (lvl in from..to) {
                            val selected = (lvl == current)
                            OutlinedButton(
                                onClick = { onPick(lvl) },
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    width = if (selected) 2.dp else 1.dp,
                                    brush = null,
                                    color = if (selected) AZURE else Color(0xFF2A3B5B)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selected) AZURE else Color.White
                                ),
                                modifier = Modifier.height(36.dp)
                            ) { Text(lvl.toString()) }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = AZURE)
                        ) { Text("Chiudi") }
                    }
                }
            }
        }
    }
}

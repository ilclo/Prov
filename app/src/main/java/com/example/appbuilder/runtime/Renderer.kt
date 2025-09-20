package com.example.appbuilder.runtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RenderNode(node: Node, state: Map<String, String>) {
    when (node.type) {
        "Page" -> {
            val bg = parseColorHex(node.prop("bg"))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (bg != null) Modifier.background(bg) else Modifier)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    node.children.forEach { RenderNode(it, state) }
                }
            }
        }
        "Column" -> {
            val gap = node.prop("gap", "8").toFloatOrNull()?.dp ?: 8.dp
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                node.children.forEach { RenderNode(it, state) }
            }
        }
        "Row" -> {
            val gap = node.prop("gap", "8").toFloatOrNull()?.dp ?: 8.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                node.children.forEach { RenderNode(it, state) }
            }
        }
        "Text" -> {
            val raw = node.prop("text")
            val txt = resolveBindings(raw, state)
            val size = node.prop("sizeSp", "16").toFloatOrNull()?.sp ?: 16.sp
            val color = parseColorHex(node.prop("color"))
            val weight = when (node.prop("weight")) {
                "w300" -> FontWeight.W300
                "w400" -> FontWeight.W400
                "w500" -> FontWeight.W500
                "w600" -> FontWeight.W600
                "w700" -> FontWeight.W700
                else -> FontWeight.Normal
            }
            val align = when (node.prop("align")) {
                "center" -> TextAlign.Center
                "end" -> TextAlign.End
                else -> TextAlign.Start
            }
            Text(
                text = txt,
                fontSize = size,
                fontWeight = weight,
                color = color ?: androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                textAlign = align
            )
        }
        "Button" -> {
            val label = resolveBindings(node.prop("text", "Button"), state)
            val onTap = parseAction(node.prop("onTap"))
            val handler = LocalActionHandler.current
            Button(onClick = { onTap?.let(handler::handle) }) {
                Text(label)
            }
        }
        "Spacer" -> {
            val h: Dp = node.prop("heightDp", "8").toFloatOrNull()?.dp ?: 8.dp
            Spacer(Modifier.height(h))
        }
        else -> {
            // ignora per v0
        }
    }
}

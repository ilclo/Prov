package com.example.appbuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.appbuilder.runtime.*
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val json = remember { Json { ignoreUnknownKeys = true } }
                val root = remember { json.decodeFromString(Node.serializer(), SAMPLE_APP_JSON) }
                val state = remember { mutableStateMapOf<String, String>().apply { this["name"] = "tu" } }
                val ctx = LocalContext.current

                val handler = remember {
                    DefaultActionHandler(
                        context = ctx,
                        state = state,
                        navigate = { /* TODO nav stub */ }
                    )
                }

                CompositionLocalProvider(LocalActionHandler provides handler) {
                    Surface(Modifier.fillMaxSize()) {
                        RenderNode(root, state)
                    }
                }
            }
        }
    }
}

private const val SAMPLE_APP_JSON = """
{
  "type": "Page",
  "props": { "bg": "#101114", "title": "Demo v0" },
  "children": [
    {
      "type": "Column",
      "props": { "gap": "12" },
      "children": [
        { "type": "Text", "props": { "text": "Ciao {{name}} 👋", "sizeSp": "22" } },
        { "type": "Row", "props": { "gap": "8" }, "children": [
          { "type": "Button", "props": { "text": "Imposta nome", "onTap": "set:name=Mario" } },
          { "type": "Button", "props": { "text": "Saluta", "onTap": "toast:Ciao da Runtime!" } }
        ]},
        { "type": "Spacer", "props": { "heightDp": "12" } },
        { "type": "Text", "props": { "text": "Rotta fittizia → premimi", "color": "#1E88E5" } },
        { "type": "Button", "props": { "text": "Vai a /details", "onTap": "nav:/details" } }
      ]
    }
  ]
}
"""

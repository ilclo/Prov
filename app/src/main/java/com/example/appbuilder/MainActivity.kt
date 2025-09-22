package com.example.appbuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.example.appbuilder.editor.EditorScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val dark = true // forziamo dark, stile “GitHub scuro” per i menù
            val colors = if (dark) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colors) {
                EditorScreen() // schermo demo con solo menù/estetica richiesti
            }
        }
    }
}

package com.example.appbuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.appbuilder.ui.theme.AppBuilderTheme
import com.example.appbuilder.editor.EditorDemoScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { // il tuo tema Material3
                com.example.appbuilder.editor.EditorMenuOnlyDemo()
            }
        }
    }
}

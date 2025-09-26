@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appbuilder.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.appbuilder.core.AppState
import com.example.appbuilder.core.Binding
import com.example.appbuilder.core.AppBindings
import com.example.appbuilder.data.DataRegistry
import com.example.appbuilder.data.DataState
import com.example.appbuilder.eca.Event
import com.example.appbuilder.eca.RuleEngine
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Simple bound dropdown MVP:
 * - Loads data from DataRegistry via sourceId in Binding
 * - Emits OnLoadSuccess/OnLoadError + OnChange events to RuleEngine
 * - Writes selection to AppState at "${componentPath}.selected"
 */
@Composable
fun BoundDropdown(
    binding: Binding,
    data: DataRegistry,
    engine: RuleEngine,
    label: String = "Seleziona"
) {
    val stateFlow = data.stateOf(binding.sourceId)
    var items by remember { mutableStateOf<List<Pair<String,String>>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load when entering (MVP)
    LaunchedEffect(binding.sourceId) { data.refresh(binding.sourceId) }

    // Observe data state
    LaunchedEffect(stateFlow) {
        stateFlow.collectLatest { st ->
            when (st) {
                is DataState.Idle -> { /* noop */ }
                is DataState.Loading -> { loading = true; error = null }
                is DataState.Empty -> {
                    loading = false; error = null; items = emptyList()
                    engine.emit(Event.OnLoadSuccess(binding.componentPath))
                }
                is DataState.Success -> {
                    loading = false; error = null
                    items = st.rows.map { row ->
                        (row["label"]?.toString() ?: "") to (row["value"]?.toString() ?: "")
                    }
                    engine.emit(Event.OnLoadSuccess(binding.componentPath))
                }
                is DataState.Error -> {
                    loading = false; error = st.throwable.message ?: "Errore"
                    engine.emit(Event.OnLoadError(binding.componentPath))
                }
            }
        }
    }

    Box(Modifier.fillMaxWidth().padding(8.dp)) {
        var tfExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = tfExpanded, onExpandedChange = { tfExpanded = !tfExpanded }) {
            OutlinedTextField(
                readOnly = true,
                value = selected ?: "",
                onValueChange = { },
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tfExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = tfExpanded, onDismissRequest = { tfExpanded = false }) {
                if (loading) {
                    DropdownMenuItem(text = { CircularProgressIndicator() }, onClick = { })
                } else if (error != null) {
                    DropdownMenuItem(text = { Text(error!!) }, onClick = { })
                } else if (items.isEmpty()) {
                    DropdownMenuItem(text = { Text("Nessun elemento") }, onClick = { })
                } else {
                    items.forEach { (labelTxt, value) ->
                        DropdownMenuItem(
                            text = { Text(labelTxt) },
                            onClick = {
                                selected = labelTxt
                                AppState.store.set("${binding.componentPath}.selected", value)
                                engine.emit(Event.OnChange(binding.componentPath))
                                tfExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
package com.example.appbuilder.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple key-value store addressed by string Paths (e.g., "Home/Lista.count").
 * Backed by a StateFlow so UI and engines can observe changes.
 */
class StateStore {
    private val _values = mutableMapOf<String, Any?>()
    private val _flow = MutableStateFlow<Map<String, Any?>>(emptyMap())

    val flow: StateFlow<Map<String, Any?>> = _flow.asStateFlow()

    fun get(key: String): Any? = synchronized(this) { _values[key] }
    fun getString(key: String): String? = get(key) as? String
    fun getInt(key: String): Int? = when (val v = get(key)) {
        is Int -> v
        is Number -> v.toInt()
        is String -> v.toIntOrNull()
        else -> null
    }

    fun set(key: String, value: Any?) {
        synchronized(this) {
            _values[key] = value
            _flow.value = _values.toMap()
        }
    }

    fun setAll(map: Map<String, Any?>) {
        synchronized(this) {
            _values.putAll(map)
            _flow.value = _values.toMap()
        }
    }
}

object AppState {
    val store = StateStore()
}
package com.example.appbuilder.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Uniform "Row" representation so UI templates can bind with few assumptions.
 * Recommended keys: "label", "value", "type", "url", "thumb".
 */
typealias Row = Map<String, Any?>

sealed class DataState {
    object Idle : DataState()
    object Loading : DataState()
    data class Success(val rows: List<Row>) : DataState()
    data class Empty(val message: String = "Nessun elemento") : DataState()
    data class Error(val throwable: Throwable) : DataState()
}

interface DataSource {
    val id: String
    suspend fun load(): Result<List<Row>>
}

/** Registry that holds sources and their latest state (Flow). */
class DataRegistry {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val sources = mutableMapOf<String, DataSource>()
    private val states = mutableMapOf<String, MutableStateFlow<DataState>>()

    fun register(source: DataSource) {
        sources[source.id] = source
        states.getOrPut(source.id) { MutableStateFlow(DataState.Idle) }
    }

    fun stateOf(id: String): StateFlow<DataState> =
        states.getOrPut(id) { MutableStateFlow(DataState.Idle) }.asStateFlow()

    fun refresh(id: String) {
        val src = sources[id] ?: return
        val state = states.getOrPut(id) { MutableStateFlow(DataState.Idle) }
        state.value = DataState.Loading
        scope.launch {
            try {
                val result = src.load()
                state.value = result.fold(
                    onSuccess = { rows -> if (rows.isEmpty()) DataState.Empty() else DataState.Success(rows) },
                    onFailure = { DataState.Error(it) }
                )
            } catch (t: Throwable) {
                state.value = DataState.Error(t)
            }
        }
    }
}

/* ----------------------- Implementations ----------------------- */

/** HTTP GET that expects a plain text body split by a separator into rows with "label" and "value". */
class HttpTextSource(
    override val id: String,
    private val url: String,
    private val headers: Map<String, String> = emptyMap(),
    private val separator: Char = '|'
) : DataSource {
    override suspend fun load(): Result<List<Row>> = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        if (code !in 200..299) error("HTTP $code: $text")
        val rows = text.split(separator).mapNotNull { raw ->
            val v = raw.trim()
            if (v.isEmpty()) null else mapOf("label" to v, "value" to v)
        }
        rows
    }
}

/**
 * Placeholder folder lister for MVP. On Android, real implementation should use SAF/MediaStore.
 * For now, this FakeFolderSource takes a provided list (useful for demo/preview).
 */
class FakeFolderSource(
    override val id: String,
    private val items: List<String>
) : DataSource {
    override suspend fun load(): Result<List<Row>> = runCatching {
        items.map { name ->
            val type = when (name.substringAfterLast('.', "").lowercase()) {
                "mp4", "mov", "m4v" -> "video"
                "jpg", "jpeg", "png", "gif", "webp" -> "image"
                "mp3", "wav", "aac" -> "audio"
                else -> "text"
            }
            mapOf("label" to name.substringBeforeLast('.', name), "value" to name, "type" to type)
        }
    }
}
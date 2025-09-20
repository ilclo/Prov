package com.example.appbuilder.runtime

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.staticCompositionLocalOf

sealed interface Action {
    data class ToastMsg(val message: String) : Action
    data class SetState(val key: String, val value: String) : Action
    data class Nav(val route: String) : Action
}

fun parseAction(raw: String?): Action? {
    if (raw.isNullOrBlank()) return null
    val s = raw.trim()
    return when {
        s.startsWith("toast:") -> Action.ToastMsg(s.removePrefix("toast:"))
        s.startsWith("set:") -> {
            val payload = s.removePrefix("set:")
            val (k, v) = payload.split("=", limit = 2).let {
                it.getOrNull(0).orEmpty() to it.getOrNull(1).orEmpty()
            }
            Action.SetState(k, v)
        }
        s.startsWith("nav:") -> Action.Nav(s.removePrefix("nav:"))
        else -> null
    }
}

interface ActionHandler {
    fun handle(action: Action)
}

class DefaultActionHandler(
    private val context: Context,
    private val state: MutableMap<String, String>,
    private val navigate: (String) -> Unit
) : ActionHandler {
    override fun handle(action: Action) {
        when (action) {
            is Action.ToastMsg -> Toast.makeText(context, action.message, Toast.LENGTH_SHORT).show()
            is Action.SetState -> state[action.key] = action.value
            is Action.Nav -> navigate(action.route)
        }
    }
}

val LocalActionHandler = staticCompositionLocalOf<ActionHandler> {
    error("ActionHandler mancante")
}

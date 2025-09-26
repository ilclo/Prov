package com.example.appbuilder

import com.example.appbuilder.core.AppBindings
import com.example.appbuilder.core.AppState
import com.example.appbuilder.data.DataRegistry
import com.example.appbuilder.eca.ActionHandler
import com.example.appbuilder.eca.RuleEngine

object AppEngine {
    val state = AppState.store
    val bindings = AppBindings.registry
    val data = DataRegistry()

    // Handler needs to be provided by the app shell (to show banners, navigate, etc.)
    lateinit var engine: RuleEngine
    fun init(handler: ActionHandler) {
        engine = RuleEngine(state = state, data = data, handler = handler)
    }
}
package com.example.appbuilder.eca

import com.example.appbuilder.core.StateStore
import com.example.appbuilder.data.DataRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Event(val name: String, val path: String? = null) {
    class OnLoadSuccess(path: String): Event("onLoadSuccess", path)
    class OnLoadError(path: String): Event("onLoadError", path)
    class OnChange(path: String): Event("onChange", path)
    class OnEnterPage(path: String): Event("onEnterPage", path)
    class Custom(name: String, path: String? = null): Event(name, path)
}

data class Rule(
    val on: String,
    val condition: String? = null, // simple expression (see Expr)
    val actions: List<Action>
)

sealed class Action {
    data class ShowBanner(val text: String): Action()
    data class SetVar(val key: String, val value: String): Action()
    data class Refetch(val sourceId: String): Action()
    data class OpenMenu(val name: String): Action()
    data class Navigate(val path: String): Action()
    data class SwapTemplate(val component: String, val template: String): Action()
}

interface ActionHandler {
    fun showBanner(text: String)
    fun openMenu(name: String)
    fun navigate(path: String)
    fun swapTemplate(component: String, template: String)
}

/** Orchestrates rules → actions */
class RuleEngine(
    private val state: StateStore,
    private val data: DataRegistry,
    private val handler: ActionHandler
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val rules = mutableListOf<Rule>()

    fun setRules(list: List<Rule>) { rules.clear(); rules.addAll(list) }

    fun emit(event: Event) {
        val matched = rules.filter { it.on.equals(event.name, ignoreCase = true) }
        matched.forEach { rule ->
            val ok = rule.condition?.let { Expr.eval(it, state) } ?: true
            if (ok) applyActions(rule.actions)
        }
    }

    private fun applyActions(actions: List<Action>) {
        actions.forEach { act ->
            when (act) {
                is Action.ShowBanner -> handler.showBanner(act.text)
                is Action.OpenMenu -> handler.openMenu(act.name)
                is Action.Navigate -> handler.navigate(act.path)
                is Action.SwapTemplate -> handler.swapTemplate(act.component, act.template)
                is Action.SetVar -> state.set(act.key, act.value)
                is Action.Refetch -> scope.launch { data.refresh(act.sourceId) }
            }
        }
    }
}
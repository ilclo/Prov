package com.example.appbuilder.core

enum class RefreshPolicy { MANUAL, ON_ENTER_PAGE, INTERVAL }

data class Binding(
    val componentPath: String,
    val sourceId: String,
    val refresh: RefreshPolicy = RefreshPolicy.ON_ENTER_PAGE,
    val intervalSec: Int = 0
)

/** Registry of bindings (component -> source). */
class BindingRegistry {
    private val bindings = mutableMapOf<String, Binding>()
    fun set(binding: Binding) { bindings[binding.componentPath] = binding }
    fun get(path: String): Binding? = bindings[path]
    fun all(): List<Binding> = bindings.values.toList()
}

object AppBindings {
    val registry = BindingRegistry()
}
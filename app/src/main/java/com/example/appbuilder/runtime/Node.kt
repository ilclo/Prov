package com.example.appbuilder.runtime

import kotlinx.serialization.Serializable

@Serializable
data class Node(
    val type: String,
    val id: String? = null,
    val props: Map<String, String> = emptyMap(),
    val children: List<Node> = emptyList()
)

fun Node.prop(key: String, default: String = ""): String =
    props[key]?.trim()?.takeIf { it.isNotEmpty() } ?: default

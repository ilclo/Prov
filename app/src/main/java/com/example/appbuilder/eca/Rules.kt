package com.example.appbuilder.eca

import com.example.appbuilder.core.StateStore

/** Minimal expression evaluator for conditions like:
 *  ${Home/Lista.count} == 0
 *  ${App/user.tier} == "premium"
 *  ${X} < 10
 */
object Expr {
    private val varRegex = Regex("\\$\\{([^}]+)\\}")

    fun eval(expr: String, state: StateStore): Boolean {
        return try {
            val m = varRegex.find(expr) ?: return false
            val key = m.groupValues[1]
            val lhs = state.get(key)
            val rest = expr.removeRange(m.range).trim()
            val parts = rest.split(' ').filter { it.isNotBlank() }
            if (parts.size < 2) return false
            val op = parts[0]
            val rhsRaw = parts.drop(1).joinToString(" ")
            val rhs = rhsRaw.trim().trim('"')

            when (op) {
                "==" -> compareEq(lhs, rhs)
                "!=" -> !compareEq(lhs, rhs)
                "<"  -> compareNum(lhs, rhs) { a, b -> a < b }
                ">"  -> compareNum(lhs, rhs) { a, b -> a > b }
                "<=" -> compareNum(lhs, rhs) { a, b -> a <= b }
                ">=" -> compareNum(lhs, rhs) { a, b -> a >= b }
                else -> false
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun compareEq(lhs: Any?, rhs: String): Boolean =
        when (lhs) {
            is Number -> lhs.toDouble() == rhs.toDoubleOrNull()
            is Boolean -> lhs == rhs.equals("true", true)
            else -> lhs?.toString() == rhs
        }

    private inline fun compareNum(lhs: Any?, rhs: String, cmp: (Double, Double) -> Boolean): Boolean {
        val a = when (lhs) {
            is Number -> lhs.toDouble()
            is String -> lhs.toDoubleOrNull()
            else -> null
        } ?: return false
        val b = rhs.toDoubleOrNull() ?: return false
        return cmp(a, b)
    }
}

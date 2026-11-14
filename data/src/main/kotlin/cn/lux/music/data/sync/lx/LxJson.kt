package cn.lux.music.data.sync.lx

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Wire-only ECMAScript property ordering, strings and number formatting. */
internal object LxJson {
    fun parse(text: String): JsonElement {
        checkDepth(text)
        val value =
            try {
                Json.parseToJsonElement(text)
            } catch (_: IllegalArgumentException) {
                throw LxProtocolException("Invalid LX JSON")
            }
        checkTokens(value)
        return value
    }

    private fun checkTokens(value: JsonElement) {
        when (value) {
            JsonNull -> Unit
            is JsonObject -> value.values.forEach(::checkTokens)
            is JsonArray -> value.forEach(::checkTokens)
            is JsonPrimitive -> {
                if (!value.isString && value.content !in setOf("true", "false") &&
                    !EcmaNumber.isValidToken(value.content)
                ) {
                    throw LxProtocolException("Invalid LX JSON primitive")
                }
            }
        }
    }

    fun stringify(value: JsonElement): String = buildString { appendValue(value) }

    private fun StringBuilder.appendValue(value: JsonElement) {
        when (value) {
            JsonNull -> append("null")
            is JsonObject -> appendObject(value)
            is JsonArray -> {
                append('[')
                value.forEachIndexed { index, element ->
                    if (index > 0) append(',')
                    appendValue(element)
                }
                append(']')
            }
            is JsonPrimitive ->
                when {
                    value.isString -> appendQuoted(value.content)
                    value.content in setOf("true", "false") -> append(value.content)
                    else -> append(EcmaNumber.stringify(value.content))
                }
        }
    }

    private fun StringBuilder.appendObject(value: JsonObject) {
        val indexed = value.keys.mapNotNull { key -> arrayIndex(key)?.let { it to key } }.sortedBy { it.first }
        val ordered = indexed.map { it.second } + value.keys.filter { arrayIndex(it) == null }
        append('{')
        ordered.forEachIndexed { index, key ->
            if (index > 0) append(',')
            appendQuoted(key)
            append(':')
            appendValue(value.getValue(key))
        }
        append('}')
    }

    private fun arrayIndex(key: String): Long? =
        key.toLongOrNull()?.takeIf {
            it in 0..4294967294L && it.toString() == key
        }

    private fun StringBuilder.appendQuoted(text: String) {
        append('"')
        text.forEachIndexed { index, char ->
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (char.code < 32 || isLoneSurrogate(text, index, char)) {
                        append("\\u").append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        append(char)
                    }
                }
            }
        }
        append('"')
    }

    private fun isLoneSurrogate(
        text: String,
        index: Int,
        char: Char,
    ): Boolean =
        when {
            char.isHighSurrogate() -> text.getOrNull(index + 1)?.isLowSurrogate() != true
            char.isLowSurrogate() -> text.getOrNull(index - 1)?.isHighSurrogate() != true
            else -> false
        }

    private fun checkDepth(text: String) {
        var depth = 0
        var quoted = false
        var escaped = false
        for (char in text) {
            when {
                escaped -> escaped = false
                quoted && char == '\\' -> escaped = true
                char == '"' -> quoted = !quoted
                !quoted && (char == '{' || char == '[') -> depth++
                !quoted && (char == '}' || char == ']') -> depth--
            }
            if (depth > 64) throw LxProtocolException("LX JSON nesting limit exceeded")
        }
    }
}

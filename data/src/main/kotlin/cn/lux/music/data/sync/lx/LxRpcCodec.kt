package cn.lux.music.data.sync.lx

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal sealed interface LxRpcResult {
    data object Absent : LxRpcResult

    data class Value(
        val value: JsonElement,
    ) : LxRpcResult
}

internal sealed interface LxRpcFrame {
    data object Heartbeat : LxRpcFrame

    data class Call(
        val id: String,
        val path: List<String>,
        val arguments: List<JsonElement>,
    ) : LxRpcFrame

    data class Reply(
        val id: String,
        val error: String?,
        val result: LxRpcResult,
    ) : LxRpcFrame
}

internal class LxRpcCodec(
    private val frames: LxFrameCodec = LxFrameCodec(),
) {
    fun decode(frame: String): LxRpcFrame {
        val text = frames.decode(frame)
        if (text == "ping") return LxRpcFrame.Heartbeat
        val value = LxJson.parse(text) as? JsonObject ?: throw LxProtocolException("Invalid LX RPC envelope")
        val id = string(value["name"])
        if (id.isEmpty()) throw LxProtocolException("Invalid LX RPC identifier")
        return if (value.containsKey("path")) readCall(id, value) else readReply(id, value)
    }

    private fun readCall(
        id: String,
        value: JsonObject,
    ): LxRpcFrame.Call {
        val path = readPath(value["path"])
        val args =
            (value["data"] ?: JsonArray(emptyList())) as? JsonArray
                ?: throw LxProtocolException("Invalid LX RPC arguments")
        return LxRpcFrame.Call(id, path, args.toList())
    }

    private fun readPath(value: JsonElement?): List<String> {
        val path = value as? JsonArray ?: throw LxProtocolException("Invalid LX RPC path")
        if (path.isEmpty() || path.size > 8) throw LxProtocolException("Invalid LX RPC path")
        return path.map { string(it) }
    }

    private fun readReply(
        id: String,
        value: JsonObject,
    ): LxRpcFrame.Reply {
        val error = value["error"]?.takeUnless { it == JsonNull }?.let(::string)
        val result = if (value.containsKey("data")) LxRpcResult.Value(value.getValue("data")) else LxRpcResult.Absent
        return LxRpcFrame.Reply(id, error, result)
    }

    fun encode(frame: LxRpcFrame): String {
        if (frame == LxRpcFrame.Heartbeat) return "ping"
        val value =
            when (frame) {
                is LxRpcFrame.Call ->
                    linkedMapOf(
                        "name" to JsonPrimitive(frame.id),
                        "path" to JsonArray(frame.path.map(::JsonPrimitive)),
                        "data" to JsonArray(frame.arguments),
                    )
                is LxRpcFrame.Reply ->
                    linkedMapOf<String, JsonElement>(
                        "name" to JsonPrimitive(frame.id),
                        "error" to (frame.error?.let(::JsonPrimitive) ?: JsonNull),
                    ).apply { if (frame.result is LxRpcResult.Value) put("data", frame.result.value) }
                else -> throw LxProtocolException("Invalid LX RPC envelope")
            }
        return frames.encode(LxJson.stringify(JsonObject(value)))
    }

    private fun string(value: JsonElement?): String =
        (value as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: throw LxProtocolException("Invalid LX RPC string")
}

package cn.lux.music.data.sync.lx

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Opaque wire data retains unknown fields; domain mapping stays at the data boundary. */
internal class LxSnapshot private constructor(
    internal val value: JsonObject,
) {
    fun canonical(): String = LxJson.stringify(value)

    fun md5(): String = LxCrypto.md5(canonical())

    override fun toString(): String = "LxSnapshot"

    companion object {
        fun parse(text: String): LxSnapshot = from(LxJson.parse(text))

        fun from(value: JsonElement): LxSnapshot {
            val root = value as? JsonObject ?: throw LxProtocolException("Invalid LX snapshot")
            if ("tempList" in root || listOf("defaultList", "loveList", "userList").any { root[it] !is JsonArray }) {
                throw LxProtocolException("Invalid LX snapshot")
            }
            return LxSnapshot(root)
        }
    }
}

/** Names use the device's view; the server reverses these through TRANS_MODE. */
internal enum class LxSyncMode(
    val wire: String,
) {
    MERGE_DEVICE_FIRST("merge_local_remote"),
    MERGE_SERVER_FIRST("merge_remote_local"),
    OVERWRITE_SERVER_KEEP_UNIQUE_LISTS("overwrite_local_remote"),
    OVERWRITE_DEVICE_KEEP_UNIQUE_LISTS("overwrite_remote_local"),
    REPLACE_SERVER("overwrite_local_remote_full"),
    REPLACE_DEVICE("overwrite_remote_local_full"),
    CANCEL("cancel"),
}

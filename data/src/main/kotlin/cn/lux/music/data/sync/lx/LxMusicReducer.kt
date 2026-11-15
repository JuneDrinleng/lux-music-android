package cn.lux.music.data.sync.lx

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Mutates only the private snapshot copy owned by LxListReducer. */
internal class LxMusicReducer(
    private val root: MutableMap<String, JsonElement>,
    private val users: MutableList<JsonObject>,
) {
    fun apply(
        action: String,
        raw: JsonElement,
    ) {
        if (action == "list_music_clear") {
            raw.jsonArray.forEach { setMusic(it.jsonPrimitive.content, emptyList()) }
        } else if (action == "list_music_update") {
            raw.jsonArray.forEach { updateMusic(it.jsonObject) }
        } else {
            val data = raw.jsonObject
            when (action) {
                "list_music_add" -> addMusic(data.getValue("id").jsonPrimitive.content, data)
                "list_music_move" -> {
                    removeMusic(
                        data
                            .getValue(
                                "fromId",
                            ).jsonPrimitive.content,
                        data.getValue("musicInfos").jsonArray.map {
                            it.jsonObject.getValue("id")
                        },
                    )
                    addMusic(data.getValue("toId").jsonPrimitive.content, data)
                }
                "list_music_remove" ->
                    removeMusic(
                        data.getValue("listId").jsonPrimitive.content,
                        data.getValue("ids").jsonArray,
                    )
                "list_music_overwrite" ->
                    setMusic(
                        data.getValue("listId").jsonPrimitive.content,
                        data.getValue("musicInfos").jsonArray,
                    )
                "list_music_update_position" -> reorderMusic(data)
                else -> throw LxProtocolException("Unsupported LX list action")
            }
        }
    }

    private fun music(id: String): List<JsonElement> =
        when (id) {
            "default" -> root.getValue("defaultList").jsonArray
            "love" -> root.getValue("loveList").jsonArray
            else ->
                users
                    .firstOrNull { it["id"]?.jsonPrimitive?.content == id }
                    ?.get("list")
                    ?.jsonArray
                    .orEmpty()
        }

    private fun setMusic(
        id: String,
        values: List<JsonElement>,
    ) {
        when (id) {
            "default" -> root["defaultList"] = JsonArray(values)
            "love" -> root["loveList"] = JsonArray(values)
            else -> {
                val index = users.indexOfFirst { it["id"]?.jsonPrimitive?.content == id }
                if (index >= 0) users[index] = JsonObject(users[index] + ("list" to JsonArray(values)))
            }
        }
    }

    private fun identity(value: JsonElement): String = LxJson.stringify(value)

    private fun addMusic(
        id: String,
        data: JsonObject,
    ) {
        val current = music(id)
        val known = current.map { identity(it.jsonObject.getValue("id")) }.toMutableSet()
        val additions =
            data
                .getValue(
                    "musicInfos",
                ).jsonArray
                .filter { known.add(identity(it.jsonObject.getValue("id"))) }
        setMusic(
            id,
            if (data["addMusicLocationType"]?.jsonPrimitive?.content ==
                "top"
            ) {
                additions + current
            } else {
                current + additions
            },
        )
    }

    private fun removeMusic(
        id: String,
        ids: List<JsonElement>,
    ) {
        val removed = ids.map(::identity).toSet()
        setMusic(id, music(id).filter { identity(it.jsonObject.getValue("id")) !in removed })
    }

    private fun updateMusic(data: JsonObject) {
        val id = data.getValue("id").jsonPrimitive.content
        val info = data.getValue("musicInfo").jsonObject
        val updated =
            music(id).map { value ->
                if (identity(value.jsonObject.getValue("id")) != identity(info.getValue("id"))) {
                    value
                } else {
                    val item = value.jsonObject.toMutableMap()
                    for (field in listOf("name", "singer", "source", "interval", "meta")) {
                        if (field in info) item[field] = info.getValue(field) else item.remove(field)
                    }
                    JsonObject(item)
                }
            }
        setMusic(id, updated)
    }

    private fun reorderMusic(data: JsonObject) {
        val id = data.getValue("listId").jsonPrimitive.content
        val ids = data.getValue("ids").jsonArray.map(::identity)
        val current = music(id)
        val keyed = current.associateBy { identity(it.jsonObject.getValue("id")) }
        val moved = ids.map { keyed[it] ?: throw LxProtocolException("Unknown LX song") }
        val remaining = current.filter { identity(it.jsonObject.getValue("id")) !in ids }.toMutableList()
        remaining.addAll(spliceIndex(data.getValue("position").jsonPrimitive.int, remaining.size), moved)
        setMusic(id, remaining)
    }

    private fun spliceIndex(
        position: Int,
        size: Int,
    ): Int = if (position < 0) (size + position).coerceAtLeast(0) else position.coerceAtMost(size)
}

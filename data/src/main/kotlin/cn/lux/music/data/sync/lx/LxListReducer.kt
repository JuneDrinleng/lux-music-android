package cn.lux.music.data.sync.lx

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Applies one action to a private copy; callers commit the complete result atomically. */
internal class LxListReducer(
    snapshot: LxSnapshot,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val root = snapshot.value.toMutableMap()
    private val users =
        root
            .getValue("userList")
            .jsonArray
            .map { it.jsonObject }
            .toMutableList()

    fun apply(value: JsonElement): LxSnapshot {
        val action =
            value.jsonObject
                .getValue("action")
                .jsonPrimitive.content
        val data = value.jsonObject.getValue("data")
        if (action == "list_data_overwrite") {
            return LxSnapshot.from(JsonObject(data.jsonObject.filterKeys { it != "tempList" }))
        }
        when (action) {
            "list_create" -> create(data.jsonObject)
            "list_remove" -> users.removeAll { it["id"] in data.jsonArray }
            "list_update" -> update(data.jsonArray)
            "list_update_position" -> reorderLists(data.jsonObject)
            else -> LxMusicReducer(root, users).apply(action, data)
        }
        root["userList"] = JsonArray(users.map(::canonicalList))
        return LxSnapshot.from(JsonObject(root))
    }

    private fun create(data: JsonObject) {
        val position = data.getValue("position").jsonPrimitive.int
        for (info in data.getValue("listInfos").jsonArray) {
            val item = info.jsonObject
            val id = item.getValue("id").jsonPrimitive.content
            if (id in setOf("default", "love", "temp") || users.any { it["id"] == item["id"] }) continue
            val ordered = linkedMapOf<String, JsonElement>()
            listOf("name", "id", "source", "sourceListId", "locationUpdateTime").forEach {
                if (it in item) ordered[it] = item.getValue(it)
            }
            item.filterKeys { it !in ordered && it != "list" }.forEach { (key, value) -> ordered[key] = value }
            val list = JsonObject(ordered + ("list" to JsonArray(emptyList())))
            users.add(if (position < 0 || position >= users.size) users.size else position, list)
        }
    }

    private fun update(values: JsonArray) {
        for (value in values) {
            val info = value.jsonObject
            val index = users.indexOfFirst { it["id"] == info["id"] }
            if (index < 0) continue
            val item = users[index].toMutableMap()
            for (field in listOf("name", "source", "sourceListId", "locationUpdateTime")) {
                if (field in info) item[field] = info.getValue(field) else item.remove(field)
            }
            users[index] = JsonObject(item)
        }
    }

    private fun reorderLists(data: JsonObject) {
        val ids = data.getValue("ids").jsonArray
        val moved =
            ids.map { id ->
                val item = users.firstOrNull { it["id"] == id } ?: throw LxProtocolException("Unknown LX list")
                JsonObject(item + ("locationUpdateTime" to JsonPrimitive(now())))
            }
        users.removeAll { it["id"] in ids }
        users.addAll(spliceIndex(data.getValue("position").jsonPrimitive.int, users.size), moved)
    }

    private fun canonicalList(info: JsonObject): JsonObject =
        JsonObject(info.filterKeys { it != "list" } + ("list" to (info["list"] ?: JsonArray(emptyList()))))

    private fun spliceIndex(
        position: Int,
        size: Int,
    ): Int =
        if (position <
            0
        ) {
            (size + position).coerceAtLeast(0)
        } else {
            position.coerceAtMost(size)
        }
}

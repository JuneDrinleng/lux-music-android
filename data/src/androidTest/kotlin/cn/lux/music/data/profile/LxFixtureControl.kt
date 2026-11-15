package cn.lux.music.data.profile

import cn.lux.music.data.sync.lx.LxJson
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Only in androidTest; talks to the separate Node fixture client, not an invented LX endpoint. */
internal class LxFixtureControl {
    private val client = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
    private val address = "http://10.0.2.2:19529"

    fun post(
        route: String,
        body: String,
    ): String = request(route, body)

    fun ids(): List<String> =
        LxJson
            .parse(request("/state", null))
            .jsonObject
            .getValue("defaultList")
            .jsonArray
            .map {
                it.jsonObject
                    .getValue("id")
                    .jsonPrimitive.content
            }

    fun counter(key: String): Int =
        LxJson
            .parse(request("/stats", null))
            .jsonObject
            .getValue(key)
            .jsonPrimitive.content
            .toInt()

    suspend fun awaitIds(expected: List<String>) {
        withTimeout(20_000) { while (!ids().containsAll(expected)) delay(50) }
    }

    private fun request(
        route: String,
        body: String?,
    ): String {
        val request =
            Request
                .Builder()
                .url(address + route)
                .apply {
                    if (body != null) post(body.toRequestBody("application/json".toMediaType()))
                }.build()
        return client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Fixture control failed" }
            response.body!!.string()
        }
    }
}

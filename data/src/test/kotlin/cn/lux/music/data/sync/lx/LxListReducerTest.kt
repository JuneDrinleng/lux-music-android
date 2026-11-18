package cn.lux.music.data.sync.lx

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

internal class LxListReducerTest {
    private val empty = """{"defaultList":[],"loveList":[],"userList":[]}"""
    private val song =
        """
        {"id":"a","name":"歌曲🎵","singer":"Fixture","source":"kw","interval":null,
        "meta":{"songId":123,"albumName":"","future":{"preserved":true}}}
        """.trimIndent()

    @Test
    fun allTwelveActionsMatchOriginalNodeManagerCanonicalAndMd5() {
        assumeTrue("Set LUX_LX_SOURCE for original manager oracle", System.getenv("LUX_LX_SOURCE") != null)
        val actions = contractActions()
        val input =
            JsonObject(
                mapOf(
                    "initial" to LxJson.parse(empty),
                    "actions" to JsonArray(actions),
                    "now" to JsonPrimitive(1234),
                ),
            )
        val root = File(requireNotNull(System.getProperty("lux.root")))
        val process =
            ProcessBuilder(
                "node",
                "scripts/lx-contract/action-oracle.cjs",
            ).directory(root).redirectErrorStream(true).start()
        process.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(LxJson.stringify(input)) }
        assertTrue("Node action oracle timed out", process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS))
        val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        assertEquals(output, 0, process.waitFor())
        val expected = LxJson.parse(output).jsonArray
        var snapshot = LxSnapshot.parse(empty)
        actions.forEachIndexed { index, action ->
            snapshot = LxListReducer(snapshot) { 1234L }.apply(action)
            assertEquals(
                "action $index",
                expected[index]
                    .jsonObject
                    .getValue("canonical")
                    .jsonPrimitive.content,
                snapshot.canonical(),
            )
            assertEquals(
                expected[index]
                    .jsonObject
                    .getValue("md5")
                    .jsonPrimitive.content,
                snapshot.md5(),
            )
        }
    }

    @Test
    fun unrelatedOpaqueMetadataSurvivesAndInputIsNotMutated() {
        val snapshot = LxSnapshot.parse("""{"defaultList":[],"loveList":[],"userList":[],"futureRoot":true}""")
        val before = snapshot.canonical()
        val result =
            LxListReducer(snapshot).apply(
                LxJson.parse(
                    """{"action":"list_music_add","data":{"id":"default","musicInfos":[$song],
                    "addMusicLocationType":"top"}}""",
                ),
            )
        assertEquals(before, snapshot.canonical())
        assertTrue(result.canonical().contains("\"futureRoot\":true"))
        assertTrue(result.canonical().contains("\"preserved\":true"))
    }

    private fun contractActions() =
        listOf(
            """{"action":"list_data_overwrite","data":$empty}""",
            """{"action":"list_create","data":{"position":0,"listInfos":[
                {"id":"one","name":"一","locationUpdateTime":null},
                {"id":"two","name":"二","locationUpdateTime":null}]}}""",
            """{"action":"list_update","data":[{"id":"one","name":"新名称","locationUpdateTime":2}]}""",
            """{"action":"list_update_position","data":{"position":0,"ids":["one"]}}""",
            """{"action":"list_music_add","data":{"id":"default","musicInfos":[$song,$song],
                "addMusicLocationType":"bottom"}}""",
            """{"action":"list_music_move","data":{"fromId":"default","toId":"one","musicInfos":[$song],
                "addMusicLocationType":"top"}}""",
            """{"action":"list_music_update","data":[{"id":"one","musicInfo":$song}]}""",
            """{"action":"list_music_overwrite","data":{"listId":"default","musicInfos":[$song]}}""",
            """{"action":"list_music_update_position","data":{"listId":"default","position":-1,"ids":["a"]}}""",
            """{"action":"list_music_remove","data":{"listId":"one","ids":["a"]}}""",
            """{"action":"list_music_clear","data":["default","love","one"]}""",
            """{"action":"list_remove","data":["two","one"]}""",
        ).map(LxJson::parse)
}

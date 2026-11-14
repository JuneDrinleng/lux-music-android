package cn.lux.music.data.sync.lx

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/** Live execution of pinned upstream code is separate from offline golden regression tests. */
class NodeInteropTest {
    @Test
    fun kotlinHeadersAreAcceptedByActualLxAuthenticationAndSocketCode() {
        val code =
            Fixtures.root
                .getValue("auth")
                .jsonObject
                .text("code")
        val headers = LxAuth.initialHeaders(code, Fixtures.publicKey, "测试 Android")
        val initial =
            query(
                buildJsonObject {
                    put("operation", JsonPrimitive("auth"))
                    put("headers", JsonObject(headers.mapValues { JsonPrimitive(it.value) }))
                },
            )
        assertEquals("200", initial.text("status"))
        val credentials = LxAuth.readInitialResponse(initial.text("body"), Fixtures.privateKey)
        assertEquals("fixture-client", credentials.clientId)
        assertEquals("true", initial.text("mobile"))
        val reconnect = LxAuth.reconnectHeaders(credentials, "恢复设备")
        val reply =
            query(
                buildJsonObject {
                    put("operation", JsonPrimitive("auth"))
                    put("headers", JsonObject(reconnect.mapValues { JsonPrimitive(it.value) }))
                },
            )
        assertEquals("200", reply.text("status"))
        LxAuth.verifyReconnect(reply.text("body"), credentials)
        val socket =
            query(
                buildJsonObject {
                    put("operation", JsonPrimitive("socket"))
                    put("url", JsonPrimitive(LxEndpoint("https://example.org/base/").socket(credentials)))
                },
            )
        assertEquals("true", socket.text("accepted"))
        val bad =
            query(
                buildJsonObject {
                    put("operation", JsonPrimitive("auth"))
                    put(
                        "headers",
                        JsonObject(
                            mapOf("m" to JsonPrimitive("not-a-real-code")),
                        ),
                    )
                },
            )
        assertEquals("401", bad.text("status"))
    }

    @Test
    fun nodeAcceptsKotlinCryptoGzipAndRpc() {
        val credentials = Fixtures.credentials()
        val payload = "音乐🎵".repeat(600)
        val crypto =
            query(
                buildJsonObject {
                    put("operation", JsonPrimitive("crypto"))
                    put("aes", JsonPrimitive(LxCrypto.encrypt(payload, credentials.key)))
                    put("rsa", JsonPrimitive(LxCrypto.rsaEncrypt("Kotlin → Node", Fixtures.publicKey)))
                },
            )
        assertEquals(payload, crypto.text("aes"))
        assertEquals("Kotlin → Node", crypto.text("rsa"))
        val frame =
            query(
                buildJsonObject {
                    put("operation", JsonPrimitive("frame"))
                    put("frame", JsonPrimitive(LxFrameCodec().encode(payload)))
                },
            )
        assertEquals(payload, frame.text("text"))
        val codec = LxRpcCodec()
        val request = LxRpcFrame.Call("kotlin", listOf("echo"), listOf(JsonPrimitive(payload)))
        val rpc =
            query(
                buildJsonObject {
                    put("operation", JsonPrimitive("rpc"))
                    put("frame", JsonPrimitive(codec.encode(request)))
                },
            )
        val reply = codec.decode(rpc.text("frame")) as LxRpcFrame.Reply
        assertEquals("kotlin", reply.id)
        assertEquals(LxRpcResult.Value(JsonArray(listOf(JsonPrimitive(payload)))), reply.result)
    }

    @Test
    fun upstreamConfirmsEveryDirectionSnapshotDeletionCancellationAndRecovery() {
        val modes = Fixtures.root.getValue("modes").jsonObject
        assertEquals(modes.keys, LxSyncMode.entries.map { it.wire }.toSet())
        for (case in Fixtures.cases("syncCases")) {
            val result =
                query(
                    buildJsonObject {
                        put("operation", JsonPrimitive("sync"))
                        case.getValue("input").jsonObject.forEach { (key, value) -> put(key, value) }
                    },
                )
            assertEquals(case.text("name"), case.getValue("result"), result)
        }
        val cancelled =
            Fixtures
                .cases("syncCases")
                .first { it.text("name") == "cancel" }
                .getValue("result")
                .jsonObject
        assertEquals("false", cancelled.text("ready"))
        assertTrue((cancelled.getValue("calls") as JsonArray).contains(JsonPrimitive("finished")))
        val deleted =
            Fixtures
                .cases("syncCases")
                .first {
                    it.text("name") == "three-way-deletions"
                }.getValue("result")
                .jsonObject
        val songs = deleted.getValue("server").jsonObject.getValue("defaultList") as JsonArray
        assertEquals(setOf("4", "5"), songs.map { it.jsonObject.text("id") }.toSet())
        val failure =
            Fixtures
                .cases(
                    "syncCases",
                ).first { it.text("name") == "response-failure" }
                .getValue("result")
                .jsonObject
        assertEquals("false", failure.text("ready"))
        assertFalse(failure.text("error").isEmpty())
        val recovered =
            Fixtures
                .cases("syncCases")
                .first {
                    it.text("name") == "reconnect-reconcile"
                }.getValue("result")
                .jsonObject
        assertEquals(recovered["server"], recovered["client"])
        assertEquals("true", recovered.text("ready"))
    }

    private fun query(input: JsonObject): JsonObject {
        val source = System.getenv("LUX_LX_SOURCE")
        assumeTrue(
            "Live oracle requires pinned source checkouts; offline golden tests still run",
            source != null,
        )
        val root = File(checkNotNull(System.getProperty("lux.root")))
        val stderr = File.createTempFile("lx-oracle-", ".log")
        val stdout = File.createTempFile("lx-oracle-", ".json")
        return try {
            val process =
                ProcessBuilder("node", "--experimental-vm-modules", "scripts/lx-contract/oracle.mjs", "query")
                    .directory(root)
                    .redirectError(stderr)
                    .redirectOutput(stdout)
                    .start()
            process.outputStream.bufferedWriter().use { it.write(input.toString()) }

            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                error("Node oracle timed out")
            }
            check(process.exitValue() == 0) { "Node oracle failed: " + stderr.readText() }
            LxJson.parse(stdout.readText()).jsonObject
        } finally {
            stderr.delete()
            stdout.delete()
        }
    }
}

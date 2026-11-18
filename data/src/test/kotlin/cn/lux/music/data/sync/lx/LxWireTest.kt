package cn.lux.music.data.sync.lx

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LxWireTest {
    @Test
    fun jsonAndMd5MatchUpstreamIncludingIeee754AndLoneSurrogates() {
        for (case in Fixtures.cases("jsonCases")) {
            val encoded = LxJson.stringify(LxJson.parse(case.text("input")))
            assertEquals(case.text("name"), case.text("canonical"), encoded)
            assertEquals(case.text("md5"), LxCrypto.md5(encoded))
        }
    }

    @Test
    fun snapshotKeepsUnknownFieldsButExcludesTemporaryQueue() {
        val case = Fixtures.cases("jsonCases").first { it.text("name") == "snapshot" }
        val snapshot = LxSnapshot.parse(case.text("input"))
        assertEquals(case.text("canonical"), snapshot.canonical())
        assertEquals(case.text("md5"), snapshot.md5())
        assertTrue(snapshot.canonical().contains("extraField"))
        rejects { LxSnapshot.parse("{\"defaultList\":[],\"loveList\":[],\"userList\":[],\"tempList\":[]}") }
        rejects { LxSnapshot.parse("{\"defaultList\":null,\"loveList\":[],\"userList\":[]}") }
    }

    @Test
    fun aesAndRsaMatchUnmodifiedNodeHelpers() {
        for (case in Fixtures.cases("aes")) {
            assertEquals(case.text("ciphertext"), LxCrypto.encrypt(case.text("text"), case.text("key")))
            assertEquals(case.text("text"), LxCrypto.decrypt(case.text("ciphertext"), case.text("key")))
        }
        val auth = Fixtures.root.getValue("auth").jsonObject
        assertEquals(auth.text("derived"), LxCrypto.deriveCodeKey(auth.text("code")))
        assertEquals(
            auth.text("initialHeader"),
            LxAuth.initialHeaders(auth.text("code"), Fixtures.publicKey, "测试 Android").getValue("m"),
        )
        val credentials = LxAuth.readInitialResponse(auth.text("rsaCiphertext"), Fixtures.privateKey)
        assertEquals("fixture-client", credentials.clientId)
        assertFalse(credentials.toString().contains(credentials.key))
        rejects { LxCrypto.decrypt("broken", credentials.key) }
        rejects { LxCrypto.encrypt("test", "invalid-key") }
        assertTrue(runCatching { LxAuth.initialHeaders("fixture", Fixtures.publicKey, "bad\nname") }.isFailure)
    }

    @Test
    fun framesUseUtf16ThresholdAndDecodeNodeGzip() {
        val codec = LxFrameCodec()
        for (case in Fixtures.cases("frames")) {
            assertEquals(case.text("text"), codec.decode(case.text("frame")))
            val encoded = codec.encode(case.text("text"))
            assertEquals(case.text("compressed").toBoolean(), encoded.startsWith("cg_"))
            assertEquals(case.text("text"), codec.decode(encoded))
        }
    }

    @Test
    fun rejectsMalformedOversizedAndDeepFrames() {
        val tiny = LxFrameCodec(maxEncodedBytes = 512, maxDecodedBytes = 128)
        rejects { tiny.decode(LxFrameCodec().encode("x".repeat(5000))) }
        rejects { tiny.decode("x".repeat(513)) }
        rejects { tiny.decode("cg_not-base64") }
        rejects { tiny.decode("cg_AAAA") }
        rejects { LxJson.parse("[".repeat(65) + "0" + "]".repeat(65)) }
        rejects { LxJson.parse("NaN") }
        rejects { LxJson.parse("{\"nested\":[Infinity]}") }
    }

    @Test
    fun rpcMatchesPublished013FramesAndDistinguishesAbsentFromNull() {
        val codec = LxRpcCodec()
        for (case in Fixtures.cases("rpc")) {
            val request = codec.decode(case.getValue("request").toString()) as LxRpcFrame.Call
            assertEquals(case.getValue("request").toString(), codec.encode(request))
            val reply = codec.decode(case.getValue("response").toString()) as LxRpcFrame.Reply
            assertEquals(case.getValue("response").toString(), codec.encode(reply))
        }
        val absent = codec.decode("{\"name\":\"x\",\"error\":null}") as LxRpcFrame.Reply
        val explicit = codec.decode("{\"name\":\"x\",\"error\":null,\"data\":null}") as LxRpcFrame.Reply
        assertEquals(LxRpcResult.Absent, absent.result)
        assertEquals(LxRpcResult.Value(JsonNull), explicit.result)
        assertEquals(LxRpcFrame.Heartbeat, codec.decode("ping"))
        assertEquals(
            listOf("literal:name"),
            (codec.decode("{\"name\":\"x\",\"path\":[\"literal:name\"]}") as LxRpcFrame.Call).path,
        )
        rejects { codec.decode("{\"jsonrpc\":\"2.0\",\"id\":1}") }
        rejects { codec.decode("{\"name\":\"x\",\"path\":[]}") }
        rejects { codec.decode("{\"name\":\"x\",\"path\":[1]}") }
    }

    @Test
    fun endpointPreservesProxyPathsAndEncodesWebsocketQuery() {
        val endpoint = LxEndpoint("https://example.org:8443/sync/%E9%9F%B3/")
        assertEquals("https://example.org:8443/sync/%E9%9F%B3/ah", endpoint.http("ah"))
        assertTrue(
            endpoint.socket(Fixtures.credentials()).startsWith("wss://example.org:8443/sync/%E9%9F%B3/socket?i="),
        )
        val constants = Fixtures.root.getValue("constants").jsonObject
        assertEquals("server-id", LxAuth.serverId(constants.text("hello"), constants.text("idPrefix") + "server-id"))
        rejects { LxAuth.serverId("wrong", "wrong") }
        assertTrue(runCatching { LxEndpoint("https://user:password@example.org/") }.isFailure)
        assertTrue(runCatching { LxEndpoint("https://example.org/?token=secret") }.isFailure)
    }

    @Test
    fun modeNamesUseDevicePerspectiveAndReadySnapshotsHaveMatchingHashes() {
        val replaceServer = syncCase(LxSyncMode.REPLACE_SERVER)
        assertEquals(
            replaceServer.getValue("input").jsonObject.getValue("client"),
            replaceServer.getValue("result").jsonObject.getValue("server"),
        )
        val replaceDevice = syncCase(LxSyncMode.REPLACE_DEVICE)
        assertEquals(
            replaceDevice.getValue("input").jsonObject.getValue("server"),
            replaceDevice.getValue("result").jsonObject.getValue("client"),
        )
        for (case in Fixtures.cases("syncCases")) {
            val result = case.getValue("result").jsonObject
            if (result.text("ready") == "true") {
                val server = LxSnapshot.from(result.getValue("server"))
                val client = LxSnapshot.from(result.getValue("client"))
                assertEquals(case.text("name"), server.md5(), client.md5())
            }
        }
    }

    private fun syncCase(mode: LxSyncMode) =
        Fixtures.cases("syncCases").first {
            it.getValue("input").jsonObject.text("mode") == mode.wire
        }

    private fun rejects(block: () -> Unit) {
        assertTrue("Expected a typed protocol rejection", runCatching(block).exceptionOrNull() is LxProtocolException)
    }
}

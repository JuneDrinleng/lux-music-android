package cn.lux.music.data.sync.lx

import cn.lux.music.model.LxServer
import cn.lux.music.model.ProfileFailure
import cn.lux.music.model.ProfileSyncState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

internal class LxSyncSessionTest {
    @Test
    fun listActionsAreAcceptedAfterModuleFinishedBeforeOverallFinished() =
        runBlocking {
            exercise(completeList = true)
        }

    @Test
    fun overallFinishedCannotHideMissingListCompletion() =
        runBlocking {
            exercise(completeList = false)
        }

    private suspend fun exercise(completeList: Boolean) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val storage = MemorySyncStorage()
        val credentials = LxDeviceCredentials("public-device", "MDEyMzQ1Njc4OWFiY2RlZg==", "LX")
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(LxAuth.HELLO))
            server.enqueue(MockResponse().setBody(LxAuth.ID_PREFIX + "server"))
            server.enqueue(MockResponse().setBody(LxCrypto.encrypt(LxAuth.HELLO, credentials.key)))
            val scripted = CompletableDeferred<Unit>()
            server.enqueue(scriptedSocket(scope, storage, scripted, completeList))
            val session =
                LxSyncSession(
                    scope,
                    LxHttp(),
                    LxServer(server.url("/").toString(), "server", true),
                    credentials,
                    "fixture",
                    storage,
                )
            try {
                withTimeout(10_000) {
                    scripted.await()
                    val result = storage.result.await()
                    assertEquals(if (completeList) ProfileSyncState.READY else ProfileSyncState.FAILED, result)
                }
                assertEquals(completeList, storage.applied)
                assertEquals(completeList, storage.reconciled)
                if (!completeList) assertEquals(ProfileFailure.INCOMPATIBLE_SERVER, storage.failure)
            } finally {
                session.close()
                scope.cancel()
            }
        }
    }

    private fun scriptedSocket(
        scope: CoroutineScope,
        storage: MemorySyncStorage,
        scripted: CompletableDeferred<Unit>,
        completeList: Boolean,
    ): MockResponse =
        MockResponse().withWebSocketUpgrade(
            object : WebSocketListener() {
                lateinit var rpc: LxRpcPeer

                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response,
                ) {
                    rpc = LxRpcPeer(scope, { check(webSocket.send(it)) }, { _, _ -> LxRpcResult.Absent })
                    scope.launch {
                        try {
                            if (completeList) {
                                rpc.call(listOf("list_sync_finished"))
                                rpc.call(listOf("onListSyncAction"), listOf(LxJson.parse("{}")))
                                assertFalse(storage.ready)
                            }
                            rpc.call(listOf("finished"))
                            scripted.complete(Unit)
                        } catch (error: Exception) {
                            if (completeList) scripted.completeExceptionally(error) else scripted.complete(Unit)
                        }
                    }
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String,
                ) {
                    rpc.receive(text)
                }
            },
        )
}

private class MemorySyncStorage : LxSyncStorage {
    val result = CompletableDeferred<ProfileSyncState>()
    var applied = false
    var reconciled = false
    var ready = false
    var failure: ProfileFailure? = null

    override suspend fun beginReconciliation() = Unit

    override suspend fun snapshot(): LxSnapshot = error("No snapshot requested by this protocol scenario")

    override suspend fun replace(snapshot: LxSnapshot): Unit = error("No replacement requested")

    override suspend fun apply(action: JsonElement) {
        applied = true
    }

    override suspend fun reconciled() {
        reconciled = true
    }

    override suspend fun pending(): List<LxPendingAction> = emptyList()

    override suspend fun acknowledge(id: Long): Unit = error("No local intent exists")

    override suspend fun status(
        value: ProfileSyncState,
        failure: ProfileFailure?,
    ) {
        this.failure = failure
        if (value == ProfileSyncState.READY || value == ProfileSyncState.FAILED) {
            ready = value == ProfileSyncState.READY
            result.complete(value)
        }
    }
}

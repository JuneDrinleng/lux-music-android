package cn.lux.music.data.sync.lx

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LxRpcPeerTest {
    @Test
    fun nestedCallCanReceiveReplyWhileOriginalHandlerIsSuspended() =
        runTest {
            lateinit var first: LxRpcPeer
            lateinit var second: LxRpcPeer
            first =
                LxRpcPeer(
                    backgroundScope,
                    { second.receive(it) },
                    { _, _ -> LxRpcResult.Value(JsonPrimitive("nested")) },
                )
            second = LxRpcPeer(backgroundScope, { first.receive(it) }, { _, _ -> second.call(listOf("nested")) })
            val result = async { first.call(listOf("outer")) }
            runCurrent()
            assertEquals(LxRpcResult.Value(JsonPrimitive("nested")), result.await())
            first.close()
            second.close()
        }

    @Test
    fun timeoutCancellationAndCloseRemovePendingRequests() =
        runTest {
            val sent = mutableListOf<String>()
            val peer = LxRpcPeer(backgroundScope, { sent.add(it) }, { _, _ -> LxRpcResult.Absent }, timeoutMillis = 100)
            val timeout = async { runCatching { peer.call(listOf("wait")) } }
            runCurrent()
            advanceTimeBy(101)
            runCurrent()
            assertTrue(timeout.await().isFailure)
            val cancelled = launch { peer.call(listOf("cancel")) }
            runCurrent()
            cancelled.cancel()
            runCurrent()
            val closed = async { runCatching { peer.call(listOf("close")) } }
            runCurrent()
            peer.close()
            assertTrue(closed.await().exceptionOrNull() is LxConnectionClosed)
            assertTrue(runCatching { peer.call(listOf("again")) }.exceptionOrNull() is LxConnectionClosed)
            for (frame in sent) {
                val call = LxRpcCodec().decode(frame) as LxRpcFrame.Call
                peer.receive(LxRpcCodec().encode(LxRpcFrame.Reply(call.id, null, LxRpcResult.Absent)))
            }
        }

    @Test
    fun lateReplyFromOldConnectionCannotCompleteNewConnectionCall() =
        runTest {
            val oldSent = mutableListOf<String>()
            val old = LxRpcPeer(backgroundScope, { oldSent.add(it) }, { _, _ -> LxRpcResult.Absent })
            val oldRequest = async { runCatching { old.call(listOf("old")) } }
            runCurrent()
            old.close()
            oldRequest.await()
            val freshSent = mutableListOf<String>()
            val fresh = LxRpcPeer(backgroundScope, { freshSent.add(it) }, { _, _ -> LxRpcResult.Absent })
            val result = async { fresh.call(listOf("new")) }
            runCurrent()
            val codec = LxRpcCodec()
            val oldId = (codec.decode(oldSent.single()) as LxRpcFrame.Call).id
            fresh.receive(codec.encode(LxRpcFrame.Reply(oldId, null, LxRpcResult.Absent)))
            runCurrent()
            assertFalse(result.isCompleted)
            val newId = (codec.decode(freshSent.single()) as LxRpcFrame.Call).id
            fresh.receive(codec.encode(LxRpcFrame.Reply(newId, null, LxRpcResult.Value(JsonPrimitive(7)))))
            assertEquals(LxRpcResult.Value(JsonPrimitive(7)), result.await())
            fresh.close()
        }

    @Test
    fun parentCancellationAndMalformedFrameFailWaitingCall() =
        runTest {
            val parent = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            val peer = LxRpcPeer(parent, {}, { _, _ -> LxRpcResult.Absent })
            val waiting = async { runCatching { peer.call(listOf("pending")) } }
            runCurrent()
            parent.cancel()
            runCurrent()
            assertTrue(waiting.await().exceptionOrNull() is LxConnectionClosed)
            val next = LxRpcPeer(backgroundScope, {}, { _, _ -> LxRpcResult.Absent })
            val invalid = async { runCatching { next.call(listOf("pending")) } }
            runCurrent()
            assertTrue(runCatching { next.receive("invalid") }.exceptionOrNull() is LxProtocolException)
            assertTrue(invalid.await().exceptionOrNull() is LxConnectionClosed)
        }

    @Test
    fun timeoutIncludesSendingAndRemoteErrorsAreTyped() =
        runTest {
            val stalled =
                LxRpcPeer(
                    backgroundScope,
                    { CompletableDeferred<Unit>().await() },
                    { _, _ -> LxRpcResult.Absent },
                    100,
                )
            val result = async { runCatching { stalled.call(listOf("stalled")) } }
            runCurrent()
            advanceTimeBy(101)
            runCurrent()
            assertTrue(result.await().isFailure)
            stalled.close()
            val frames = mutableListOf<String>()
            val peer = LxRpcPeer(backgroundScope, { frames.add(it) }, { _, _ -> LxRpcResult.Absent })
            val failed = async { runCatching { peer.call(listOf("fail")) } }
            runCurrent()
            val id = (LxRpcCodec().decode(frames.single()) as LxRpcFrame.Call).id
            peer.receive(LxRpcCodec().encode(LxRpcFrame.Reply(id, "fixture rejection", LxRpcResult.Absent)))
            assertTrue(failed.await().exceptionOrNull() is LxRemoteException)
            peer.close()
        }

    @Test
    fun sendFailuresCloseConnectionWithoutUncaughtHandlerExceptions() =
        runTest {
            val outgoing =
                LxRpcPeer(
                    backgroundScope,
                    { error("fixture send failure") },
                    { _, _ -> LxRpcResult.Absent },
                )
            assertTrue(runCatching { outgoing.call(listOf("fail")) }.exceptionOrNull() is LxConnectionClosed)
            val incoming =
                LxRpcPeer(
                    backgroundScope,
                    { error("fixture reply failure") },
                    { _, _ -> LxRpcResult.Absent },
                )
            incoming.receive(LxRpcCodec().encode(LxRpcFrame.Call("incoming", listOf("test"), emptyList())))
            runCurrent()
            assertTrue(runCatching { incoming.call(listOf("again")) }.exceptionOrNull() is LxConnectionClosed)
        }

    @Test
    fun pendingAndIncomingWorkAreBounded() =
        runTest {
            val peer = LxRpcPeer(backgroundScope, {}, { _, _ -> CompletableDeferred<LxRpcResult>().await() })
            val pending = (1..128).map { async { runCatching { peer.call(listOf("pending")) } } }
            runCurrent()
            assertTrue(runCatching { peer.call(listOf("over-limit")) }.exceptionOrNull() is LxProtocolException)
            val codec = LxRpcCodec()
            repeat(32) { peer.receive(codec.encode(LxRpcFrame.Call("incoming-$it", listOf("wait"), emptyList()))) }
            assertTrue(
                runCatching {
                    peer.receive(codec.encode(LxRpcFrame.Call("too-many", listOf("wait"), emptyList())))
                }.exceptionOrNull() is LxProtocolException,
            )
            for (waiting in pending) assertTrue(waiting.await().exceptionOrNull() is LxConnectionClosed)
        }
}

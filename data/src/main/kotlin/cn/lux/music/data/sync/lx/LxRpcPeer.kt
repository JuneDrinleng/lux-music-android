package cn.lux.music.data.sync.lx

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import java.util.UUID

/** One instance per connection. Receiving a call never blocks receiving its nested replies. */
internal class LxRpcPeer(
    parent: CoroutineScope,
    private val send: suspend (String) -> Unit,
    private val handler: suspend (List<String>, List<JsonElement>) -> LxRpcResult,
    private val timeoutMillis: Long = 30_000,
) {
    private val codec = LxRpcCodec()
    private val lock = Any()
    private val prefix = UUID.randomUUID().toString()
    private var counter = 0L
    private var closed = false
    private var activeHandlers = 0
    private val pending = mutableMapOf<String, CompletableDeferred<LxRpcResult>>()
    private val scope = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext.job))

    init {
        require(timeoutMillis > 0)
        scope.coroutineContext.job.invokeOnCompletion { close() }
    }

    suspend fun call(
        path: List<String>,
        arguments: List<JsonElement> = emptyList(),
    ): LxRpcResult {
        require(path.isNotEmpty() && path.size <= 8 && path.none(String::isEmpty))
        val response = CompletableDeferred<LxRpcResult>()
        val id = register(response)
        return try {
            withTimeout(timeoutMillis) {
                val frame = codec.encode(LxRpcFrame.Call(id, path.toList(), arguments.toList()))
                sendFrame(frame)
                response.await()
            }
        } finally {
            synchronized(lock) { pending.remove(id) }
        }
    }

    private fun register(response: CompletableDeferred<LxRpcResult>): String =
        synchronized(lock) {
            if (closed) throw LxConnectionClosed()
            if (pending.size >= 128) throw LxProtocolException("LX pending request limit exceeded")
            val id = "$prefix-${++counter}"
            pending[id] = response
            id
        }

    private suspend fun sendFrame(frame: String) {
        try {
            send(frame)
        } catch (
            error: CancellationException,
        ) {
            throw error
        } catch (_: Exception) {
            close()
            throw LxConnectionClosed()
        }
    }

    fun receive(frame: String) {
        if (synchronized(lock) { closed }) return
        val message =
            try {
                codec.decode(frame)
            } catch (error: LxProtocolException) {
                close()
                throw error
            }
        when (message) {
            LxRpcFrame.Heartbeat -> Unit
            is LxRpcFrame.Reply -> {
                val response = synchronized(lock) { pending.remove(message.id) } ?: return
                if (message.error == null) {
                    response.complete(message.result)
                } else {
                    response.completeExceptionally(LxRemoteException(message.error))
                }
            }
            is LxRpcFrame.Call -> dispatch(message)
        }
    }

    private fun dispatch(call: LxRpcFrame.Call) {
        synchronized(lock) {
            if (closed) return
            if (activeHandlers >= 32) {
                close()
                throw LxProtocolException("LX handler limit exceeded")
            }
            activeHandlers++
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { respond(call) }
    }

    private suspend fun respond(call: LxRpcFrame.Call) {
        try {
            val reply =
                try {
                    LxRpcFrame.Reply(call.id, null, handler(call.path, call.arguments))
                } catch (
                    error: CancellationException,
                ) {
                    throw error
                } catch (
                    _: Exception,
                ) {
                    LxRpcFrame.Reply(call.id, "LX handler failed", LxRpcResult.Absent)
                }
            if (!synchronized(lock) { closed }) send(codec.encode(reply))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            close()
        } finally {
            synchronized(lock) { activeHandlers-- }
        }
    }

    fun close() {
        val waiting =
            synchronized(lock) {
                if (closed) return
                closed = true
                pending.values.toList().also { pending.clear() }
            }
        waiting.forEach { it.completeExceptionally(LxConnectionClosed()) }
        scope.cancel()
    }
}

package cn.lux.music.data.sync.lx

import cn.lux.music.api.ProfileException
import cn.lux.music.model.InitialSyncChoice
import cn.lux.music.model.LxServer
import cn.lux.music.model.ProfileFailure
import cn.lux.music.model.ProfileSyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class LxSyncSession(
    parent: CoroutineScope,
    private val http: LxHttp,
    private val server: LxServer,
    private val credentials: LxDeviceCredentials,
    private val deviceName: String,
    private val storage: LxSyncStorage,
) : AutoCloseable {
    private val scope = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext.job))

    @Volatile private var choice: CompletableDeferred<InitialSyncChoice>? = null

    init {
        scope.launch { connect() }
    }

    fun choose(value: InitialSyncChoice) {
        if (choice?.complete(value) != true) throw ProfileException(ProfileFailure.STALE_OPERATION)
    }

    // Translate heterogeneous transport/protocol failures at this session boundary.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun connect() {
        for (attempt in 0..4) {
            try {
                storage.status(ProfileSyncState.CONNECTING)
                storage.beginReconciliation()
                http.reconnect(server, credentials, deviceName)
                Attempt().run()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val failure =
                    when (error) {
                        is ProfileException -> error.failure
                        is LxConnectionClosed -> ProfileFailure.NETWORK
                        else -> ProfileFailure.INCOMPATIBLE_SERVER
                    }
                if (failure == ProfileFailure.STALE_OPERATION || !reportFailure(failure)) return
                if (failure != ProfileFailure.NETWORK || attempt == 4) return
                delay(1000L * (1 shl attempt))
            }
        }
    }

    private suspend fun reportFailure(failure: ProfileFailure): Boolean =
        try {
            storage.status(ProfileSyncState.FAILED, failure)
            true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // The captured profile may already be closed. Never report into a replacement profile.
            false
        }

    override fun close() {
        scope.cancel()
    }

    private data class Invocation(
        val path: List<String>,
        val arguments: List<JsonElement>,
        val result: CompletableDeferred<LxRpcResult>,
    )

    private inner class Attempt {
        private val calls = Channel<Invocation>(32)
        private val complete = CompletableDeferred<Unit>()
        private val failed = CompletableDeferred<ProfileFailure>()
        private var listFinished = false
        private var overallFinished = false
        private val attemptScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext.job))
        private var socket: LxSocket? = null
        private var peer: LxRpcPeer? = null

        suspend fun run() {
            try {
                val wire = http.openSocket(server, credentials)
                socket = wire
                awaitOpen(wire)
                storage.status(ProfileSyncState.NEGOTIATING)
                val rpc = LxRpcPeer(attemptScope, { wire.send(it) }, ::enqueue, timeoutMillis = 120_000)
                peer = rpc
                attemptScope.launch { receive(wire, rpc) }
                attemptScope.launch { handleCalls() }
                withTimeout(120_000) { complete.await() }
                // This loop only sends committed intents. Any uncertain failure reconnects/reconciles first.
                while (true) {
                    if (failed.isCompleted) throw ProfileException(failed.await())
                    sendPending(rpc)
                    delay(250)
                }
            } catch (_: TimeoutCancellationException) {
                throw ProfileException(ProfileFailure.NETWORK)
            } finally {
                choice?.cancel()
                choice = null
                calls.cancel()
                attemptScope.cancel()
                peer?.close()
                socket?.close()
                withContext(NonCancellable) { attemptScope.coroutineContext.job.cancelAndJoin() }
            }
        }

        private suspend fun awaitOpen(wire: LxSocket) {
            try {
                withTimeout(15_000) { wire.opened.await() }
            } catch (
                _: TimeoutCancellationException,
            ) {
                throw ProfileException(ProfileFailure.NETWORK)
            }
        }

        private suspend fun sendPending(rpc: LxRpcPeer) {
            try {
                for (action in storage.pending()) {
                    rpc.call(listOf("onListSyncAction"), listOf(LxJson.parse(action.wire)))
                    storage.acknowledge(action.id)
                }
            } catch (_: LxConnectionClosed) {
                throw ProfileException(if (failed.isCompleted) failed.await() else ProfileFailure.NETWORK)
            }
        }

        private suspend fun receive(
            wire: LxSocket,
            rpc: LxRpcPeer,
        ) {
            try {
                for (frame in wire.incoming) rpc.receive(frame)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                complete.completeExceptionally(ProfileException(ProfileFailure.NETWORK))
                failed.complete(ProfileFailure.NETWORK)
                rpc.close()
            }
        }

        private suspend fun enqueue(
            path: List<String>,
            arguments: List<JsonElement>,
        ): LxRpcResult {
            val result = CompletableDeferred<LxRpcResult>()
            if (!calls
                    .trySend(
                        Invocation(path, arguments, result),
                    ).isSuccess
            ) {
                throw LxProtocolException("LX ingress overflow")
            }
            return result.await()
        }

        // Keep all RPC/Room failures inside this actor and return a sanitized classification.
        @Suppress("TooGenericExceptionCaught")
        private suspend fun handleCalls() {
            for (call in calls) {
                try {
                    call.result.complete(handle(call.path, call.arguments))
                } catch (
                    error: CancellationException,
                ) {
                    call.result.cancel()
                    throw error
                } catch (error: Exception) {
                    val failure = (error as? ProfileException)?.failure ?: ProfileFailure.INCOMPATIBLE_SERVER
                    call.result.completeExceptionally(LxProtocolException("LX action failed"))
                    complete.completeExceptionally(ProfileException(failure))
                    failed.complete(failure)
                    peer?.close()
                    socket?.close()
                }
            }
        }

        private suspend fun handle(
            path: List<String>,
            arguments: List<JsonElement>,
        ): LxRpcResult {
            if (path.size != 1) throw LxProtocolException("Unsupported LX operation")
            return when (path.single()) {
                "getEnabledFeatures" -> features(arguments)
                "list_sync_get_md5" -> LxRpcResult.Value(JsonPrimitive(storage.snapshot().md5()))
                "list_sync_get_list_data" -> LxRpcResult.Value(storage.snapshot().value)
                "list_sync_set_list_data" -> {
                    storage.status(ProfileSyncState.SYNCING)
                    storage.replace(LxSnapshot.from(arguments.single()))
                    LxRpcResult.Absent
                }
                "list_sync_get_sync_mode" -> selectMode()
                "onListSyncAction" -> {
                    check(listFinished)
                    storage.apply(arguments.single())
                    LxRpcResult.Absent
                }
                "list_sync_finished" -> {
                    listFinished = true
                    ready()
                    LxRpcResult.Absent
                }
                "finished" -> {
                    check(listFinished)
                    overallFinished = true
                    ready()
                    LxRpcResult.Absent
                }
                else -> throw LxProtocolException("Unsupported LX operation")
            }
        }

        private fun features(arguments: List<JsonElement>): LxRpcResult {
            if (arguments.size != 2 ||
                arguments[0].jsonPrimitive.content != "server"
            ) {
                return LxRpcResult.Value(JsonObject(emptyMap()))
            }
            val version = arguments[1].jsonObject["list"]?.jsonPrimitive?.intOrNull
            val features =
                if (version == 1) {
                    mapOf("list" to JsonObject(mapOf("skipSnapshot" to JsonPrimitive(false))))
                } else {
                    emptyMap()
                }
            return LxRpcResult.Value(JsonObject(features))
        }

        private suspend fun selectMode(): LxRpcResult {
            val pending = CompletableDeferred<InitialSyncChoice>()
            choice = pending
            storage.status(ProfileSyncState.AWAITING_CHOICE)
            val mode =
                try {
                    withTimeout(90_000) { pending.await() }
                } catch (
                    _: TimeoutCancellationException,
                ) {
                    throw ProfileException(ProfileFailure.CHOICE_EXPIRED)
                }
            choice = null
            storage.status(ProfileSyncState.SYNCING)
            val wire =
                when (mode) {
                    InitialSyncChoice.MERGE_DEVICE_FIRST -> LxSyncMode.MERGE_DEVICE_FIRST
                    InitialSyncChoice.MERGE_SERVER_FIRST -> LxSyncMode.MERGE_SERVER_FIRST
                    InitialSyncChoice.REPLACE_DEVICE -> LxSyncMode.REPLACE_DEVICE
                    InitialSyncChoice.REPLACE_SERVER -> LxSyncMode.REPLACE_SERVER
                    InitialSyncChoice.CANCEL -> LxSyncMode.CANCEL
                }.wire
            return LxRpcResult.Value(JsonPrimitive(wire))
        }

        private suspend fun ready() {
            if (listFinished && overallFinished && !complete.isCompleted) {
                storage.reconciled()
                storage.status(ProfileSyncState.READY)
                complete.complete(Unit)
            }
        }
    }
}

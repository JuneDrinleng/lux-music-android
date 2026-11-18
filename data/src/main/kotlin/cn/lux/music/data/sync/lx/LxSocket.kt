package cn.lux.music.data.sync.lx

import cn.lux.music.api.ProfileException
import cn.lux.music.model.ProfileFailure
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/** Ordered, bounded ingress. Overflow closes the connection rather than dropping a mutation. */
internal class LxSocket(
    client: OkHttpClient,
    private val address: String,
) : AutoCloseable {
    val opened = CompletableDeferred<Unit>()
    val incoming = Channel<String>(64)
    private val socket =
        client.newWebSocket(
            Request.Builder().url(address).build(),
            object : WebSocketListener() {
                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response,
                ) {
                    opened.complete(Unit)
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String,
                ) {
                    if (text.length > 8 * 1024 * 1024 || !incoming.trySend(text).isSuccess) {
                        webSocket.cancel()
                        fail()
                    }
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    bytes: ByteString,
                ) {
                    webSocket.cancel()
                    fail()
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String,
                ) {
                    fail()
                }

                override fun onClosing(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String,
                ) {
                    webSocket.close(code, null)
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?,
                ) {
                    response?.close()
                    fail()
                }
            },
        )

    private fun fail() {
        val failure = ProfileException(ProfileFailure.NETWORK)
        opened.completeExceptionally(failure)
        incoming.close(failure)
    }

    fun send(frame: String) {
        if (!socket.send(frame)) throw ProfileException(ProfileFailure.NETWORK)
    }

    override fun close() {
        socket.cancel()
        opened.cancel()
        incoming.cancel()
    }

    override fun toString(): String = "LxSocket([redacted])"
}

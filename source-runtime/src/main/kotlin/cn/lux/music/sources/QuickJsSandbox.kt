package cn.lux.music.sources

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Stage 3 validation boundary. No business SourceResolver is exposed before device gates pass. */
internal class QuickJsSandbox(
    context: Context,
) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    suspend fun run(
        script: ScriptAdmission.ApprovedScript,
        expression: String,
        executionMillis: Int = 3000,
    ): String {
        require(expression.length <= SandboxWire.MAX_EXPRESSION_CHARS)
        require(executionMillis in 100..SandboxWire.MAX_EXECUTION_MS)
        // Host deadline also bounds binding/native loader failures and lost Binder replies.
        return withTimeout(executionMillis.toLong() + 5000) {
            suspendCancellableCoroutine { continuation ->
                val session =
                    Session(script, expression, executionMillis) { value, error ->
                        if (continuation.isActive) {
                            if (error != null) {
                                continuation.resumeWithException(error)
                            } else {
                                continuation.resume(value.orEmpty())
                            }
                        }
                    }
                continuation.invokeOnCancellation { main.post { session.close() } }
                main.post {
                    if (continuation.isActive) session.bind()
                }
            }
        }
    }

    private inner class Session(
        private val script: ScriptAdmission.ApprovedScript,
        private val expression: String,
        private val executionMillis: Int,
        private val completed: (String?, RuntimeException?) -> Unit,
    ) : ServiceConnection {
        private var bound = false
        private var closed = false
        private var remote: Messenger? = null
        private val reply =
            Messenger(
                Handler(Looper.getMainLooper()) { message ->
                    if (message.what == SandboxWire.RESULT && !closed) {
                        val error = message.data.getString(SandboxWire.ERROR)
                        finish(message.data.getString(SandboxWire.VALUE), error?.let(::IllegalStateException))
                    }
                    true
                },
            )

        fun bind() {
            try {
                bound =
                    context.bindIsolatedService(
                        Intent(context, QuickJsSandboxService::class.java),
                        Context.BIND_AUTO_CREATE,
                        UUID.randomUUID().toString().replace("-", ""),
                        context.mainExecutor,
                        this,
                    )
                if (!bound) finish(null, IllegalStateException("Script process unavailable"))
            } catch (_: RuntimeException) {
                finish(null, IllegalStateException("Script process unavailable"))
            }
        }

        override fun onServiceConnected(
            name: ComponentName,
            service: IBinder,
        ) {
            if (closed) return
            remote = Messenger(service)
            try {
                remote?.send(
                    Message.obtain(null, SandboxWire.RUN).apply {
                        replyTo = reply
                        data =
                            Bundle().apply {
                                putString(SandboxWire.SCRIPT, script.code)
                                putString(SandboxWire.EXPRESSION, expression)
                                putInt(SandboxWire.DEADLINE, executionMillis)
                            }
                    },
                )
            } catch (_: RemoteException) {
                finish(null, IllegalStateException("Script process died"))
            }
        }

        override fun onServiceDisconnected(name: ComponentName) = died()

        override fun onBindingDied(name: ComponentName) = died()

        override fun onNullBinding(name: ComponentName) = died()

        private fun died() = finish(null, IllegalStateException("Script process died"))

        private fun finish(
            value: String?,
            error: RuntimeException?,
        ) {
            if (closed) return
            close()
            completed(value, error)
        }

        fun close() {
            if (closed) return
            closed = true
            try {
                remote?.send(Message.obtain(null, SandboxWire.STOP))
            } catch (_: RemoteException) {
                // Already dead; unbind still releases the framework connection.
            }
            if (bound) context.unbindService(this)
            remote = null
        }
    }
}

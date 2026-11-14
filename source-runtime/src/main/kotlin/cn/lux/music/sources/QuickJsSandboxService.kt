package cn.lux.music.sources

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.os.RemoteException
import com.whl.quickjs.android.QuickJSLoader
import com.whl.quickjs.wrapper.JSObject
import com.whl.quickjs.wrapper.QuickJSContext
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** One isolated UID/process per bindIsolatedService instance, one context on one thread. */
internal class QuickJsSandboxService : Service() {
    private val worker = HandlerThread("lux-script")
    private val watchdog = Executors.newSingleThreadScheduledExecutor()
    private var used = false
    private val endpoint = Messenger(Handler(Looper.getMainLooper(), ::handle))

    override fun onCreate() {
        super.onCreate()
        worker.start()
    }

    override fun onBind(intent: Intent): IBinder = endpoint.binder

    private fun handle(message: Message): Boolean {
        when (message.what) {
            SandboxWire.STOP -> terminate()
            SandboxWire.RUN -> {
                val reply = message.replyTo
                if (!used && reply != null) {
                    used = true
                    val input = message.data
                    val deadline = input.getInt(SandboxWire.DEADLINE).coerceIn(100, SandboxWire.MAX_EXECUTION_MS)
                    // Independent of the JS thread, Binder callback and coroutine cancellation.
                    watchdog.schedule(::terminate, deadline.toLong(), TimeUnit.MILLISECONDS)
                    Handler(worker.looper).post { execute(input, reply) }
                }
            }
        }
        return true
    }

    private fun execute(
        input: Bundle,
        reply: Messenger,
    ) {
        val output = Bundle()
        try {
            val script = input.getString(SandboxWire.SCRIPT).orEmpty()
            val expression = input.getString(SandboxWire.EXPRESSION).orEmpty()
            require(script.toByteArray(Charsets.UTF_8).size <= ScriptAdmission.MAX_SCRIPT_BYTES)
            require(expression.length <= SandboxWire.MAX_EXPRESSION_CHARS)
            output.putString(SandboxWire.VALUE, evaluate(script, expression))
        } catch (_: RuntimeException) {
            // Do not expose script text, request URLs, credentials or JS error payloads.
            output.putString(SandboxWire.ERROR, "Script execution failed")
        } catch (_: LinkageError) {
            output.putString(SandboxWire.ERROR, "Native binding unavailable")
        }
        try {
            reply.send(Message.obtain(null, SandboxWire.RESULT).apply { data = output })
        } catch (_: RemoteException) {
            terminate()
        }
        // Stay under the original watchdog until the host sends STOP/unbinds.
    }

    private fun evaluate(
        script: String,
        expression: String,
    ): String {
        check(Process.myUid() % 100_000 in 90_000..99_999) { "Isolated UID required" }
        QuickJSLoader.init()
        return QuickJSContext.create().use { context ->
            context.setMemoryLimit(32 * 1024 * 1024)
            context.setMaxStackSize(512 * 1024)
            // No Java callbacks, module loader, std/os module, network or filesystem bridge.
            val initial = context.evaluate(script, "source.js")
            if (initial is JSObject) initial.release()
            val value = context.evaluate(expression, "result.js")
            try {
                require(value is String && value.length <= SandboxWire.MAX_RESULT_CHARS)
                value
            } finally {
                if (value is JSObject) value.release()
            }
        }
    }

    private fun terminate() {
        Process.killProcess(Process.myPid())
    }

    override fun onDestroy() {
        // Never wait on or destroy a context from a different thread.
        terminate()
        super.onDestroy()
    }
}

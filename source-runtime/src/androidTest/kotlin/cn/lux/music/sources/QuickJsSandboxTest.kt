package cn.lux.music.sources

import android.os.Process
import android.system.Os
import android.system.OsConstants
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
internal class QuickJsSandboxTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val sandbox = QuickJsSandbox(instrumentation.targetContext)

    @Test
    fun nativePromisesUnicodeAndIsolation() =
        runBlocking {
            val value = run("globalThis.secret = '中文🎵'; Promise.resolve().then(() => secret += 'ok');", "secret")
            assertEquals("中文🎵ok", value)
            assertEquals("undefined", run("", "typeof secret"))
            assertEquals("undefined,undefined,undefined", run("", "[typeof Java,typeof std,typeof os].join(',')"))
            // Each run is a fresh isolated Android UID; loading native code really happened.
            assertTrue(Process.myUid() < 90_000)
            val expected = InstrumentationRegistry.getArguments().getString("expectedPageSize")?.toLong()
            if (expected != null) assertEquals(expected, Os.sysconf(OsConstants._SC_PAGESIZE))
        }

    @Test
    fun infiniteLoopAndPromiseStarvationAreKilledAndNextScriptWorks() =
        runBlocking {
            expectFailure("while (true) {}")
            expectFailure("function spin(){Promise.resolve().then(spin)}; spin();")
            assertEquals("alive", run("", "'alive'"))
        }

    @Test
    fun cancellationAndConcurrentScriptsDoNotBlockHost() =
        runBlocking {
            val looping = async { run("while (true) {}", "''", 4000) }
            delay(250)
            assertEquals("other", run("", "'other'"))
            looping.cancelAndJoin()
            assertEquals("recovered", run("", "'recovered'"))
        }

    @Test
    fun memoryStackAndOutputAreBounded() =
        runBlocking {
            assertEquals("Script execution failed", expectFailure("new Uint8Array(48*1024*1024)"))
            expectFailure("function recurse(){return recurse()+1};recurse();")
            expectFailure("", "'x'.repeat(32769)")
            expectFailure("throw new Error('private-code')")
            assertEquals("ok", run("", "'ok'"))
        }

    @Test
    fun actualMobilePreloadInitializesAndResolvesMusicUrl() =
        runBlocking {
            val script =
                mobilePrelude() +
                    """
                            if (lx.version !== '2.0.0' || lx.env !== 'mobile') throw Error('ABI mismatch');
                    lx.on(lx.EVENT_NAMES.request, ({source, action, info}) => Promise.resolve(
                                'https://example.invalid/' + info.musicInfo.id));
                            lx.send(lx.EVENT_NAMES.inited, {sources: {kw: {
                                type:'music', actions:['musicUrl','search'], qualitys:['128k','not-a-quality']
                            }}});
                            __lx_native__('probe-key','request',JSON.stringify({requestKey:'r1',data:{
                                source:'kw',action:'musicUrl',info:{type:'128k',musicInfo:{id:'中文'}}
                            }}));
                    """.trimIndent()
            val events = JSONArray(run(script, "JSON.stringify(__events)"))
            val init = events.getJSONObject(0).getJSONObject("data")
            assertTrue(init.getBoolean("status"))
            val source = init.getJSONObject("info").getJSONObject("sources").getJSONObject("kw")
            assertEquals("[\"musicUrl\"]", source.getJSONArray("actions").toString())
            assertEquals("[\"128k\"]", source.getJSONArray("qualitys").toString())
            val response = events.getJSONObject(1).getJSONObject("data")
            assertEquals("r1", response.getString("requestKey"))
            assertTrue(response.getBoolean("status"))
            assertEquals(
                "https://example.invalid/中文",
                response.getJSONObject("result").getJSONObject("data").getString("url"),
            )
        }

    @Test
    fun actualMobilePreloadBridgesRequestResponseAndRejectsInvalidUrl() =
        runBlocking {
            val script =
                mobilePrelude() +
                    """
                    lx.on(lx.EVENT_NAMES.request, () => new Promise(resolve => {
                        lx.request('https://example.invalid', {method:'get'}, (err, response, body) => resolve(body));
                    }));
                    __lx_native__('probe-key','request',JSON.stringify({requestKey:'r2',data:{
                        source:'kw',action:'musicUrl',info:{type:'128k',musicInfo:{id:'1'}}
                    }}));
                    const outgoing=__events.find(e => e.action === 'request').data;
                    __lx_native__('probe-key','response',JSON.stringify({requestKey:outgoing.requestKey,
                        error:null,response:{statusCode:200,statusMessage:'OK',headers:{},body:'file:///private'}}));
                    """.trimIndent()
            val events = JSONArray(run(script, "JSON.stringify(__events)"))
            assertEquals("request", events.getJSONObject(0).getString("action"))
            assertFalse(events.getJSONObject(1).getJSONObject("data").getBoolean("status"))
        }

    private suspend fun expectFailure(
        script: String,
        expression: String = "''",
    ): String {
        val error = withTimeout(15.seconds) { runCatching { run(script, expression, 500) }.exceptionOrNull() }
        assertTrue("execution should fail", error is IllegalStateException)
        assertFalse(error?.message.orEmpty().contains("private-code"))
        return error?.message.orEmpty()
    }

    private suspend fun run(
        script: String,
        expression: String,
        timeout: Int = 3000,
    ): String =
        sandbox.run(ScriptAdmission.approve(script, "2.0.0", ScriptAdmission.fingerprint(script)), expression, timeout)

    private fun mobilePrelude(): String {
        val preload =
            instrumentation.context.assets
                .open(
                    "mobile-user-api-preload.js",
                ).bufferedReader()
                .use { it.readText() }
        // Test host only: records ABI messages. No network, cryptography or JavaScript polyfill is faked as production.
        return """
            const __events = [];
            globalThis.__lx_native_call__ = (key,action,data) => __events.push({action,data:JSON.parse(data)});
            for (const name of ['set_timeout','utils_str2b64','utils_b642buf','utils_str2md5',
                'utils_aes_encrypt','utils_rsa_encrypt']) {
                globalThis['__lx_native_call__'+name] = () => {throw Error('not available in this probe')};
            }
            """.trimIndent() + "\n" + preload + "\nlx_setup('probe-key','id','name','','1','','','');\n"
    }
}

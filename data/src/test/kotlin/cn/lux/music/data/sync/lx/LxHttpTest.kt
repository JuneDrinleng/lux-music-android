package cn.lux.music.data.sync.lx

import cn.lux.music.api.ProfileException
import cn.lux.music.model.ProfileFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class LxHttpTest {
    @Test
    fun probeRejectionIsNotMisreportedAsExpiredDeviceAuthorization() =
        runBlocking {
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setResponseCode(401))
                val error = runCatching { LxHttp().inspect(server.url("/").toString(), true) }.exceptionOrNull()
                assertEquals(ProfileFailure.SERVER, (error as ProfileException).failure)
            }
        }

    @Test
    fun normalizationRequiresExplicitCleartextAndPreservesProxyPath() =
        runBlocking {
            val http = LxHttp()
            assertEquals("https://example.com/Case/Path", http.normalized("HTTPS://EXAMPLE.COM:443/Case/Path/", false))
            for (invalid in listOf(
                "not an address",
                "https://u:p@example.com/",
                "https://example.com/?token=x",
                "ftp://example.com",
            )) {
                assertEquals(
                    ProfileFailure.INVALID_ADDRESS,
                    (
                        runCatching {
                            http.inspect(invalid, false)
                        }.exceptionOrNull() as ProfileException
                    ).failure,
                )
            }
            assertEquals(
                ProfileFailure.CLEARTEXT_CONFIRMATION_REQUIRED,
                (runCatching { http.inspect("http://localhost", false) }.exceptionOrNull() as ProfileException).failure,
            )
        }

    @Test
    fun redirectsOversizedResponsesAndWrongServersAreRejected() =
        runBlocking {
            MockWebServer().use { server ->
                val http = LxHttp()
                server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "/elsewhere"))
                assertEquals(
                    ProfileFailure.SERVER,
                    (
                        runCatching {
                            http.inspect(server.url("/proxy").toString(), true)
                        }.exceptionOrNull() as ProfileException
                    ).failure,
                )
                assertEquals(1, server.requestCount)
                server.enqueue(MockResponse().setBody("x".repeat(65_537)))
                assertEquals(
                    ProfileFailure.INCOMPATIBLE_SERVER,
                    (
                        runCatching {
                            http.inspect(server.url("/").toString(), true)
                        }.exceptionOrNull() as ProfileException
                    ).failure,
                )
                server.enqueue(MockResponse().setBody("wrong"))
                server.enqueue(MockResponse().setBody("OjppZDo6server"))
                assertEquals(
                    ProfileFailure.INCOMPATIBLE_SERVER,
                    (
                        runCatching {
                            http.inspect(server.url("/").toString(), true)
                        }.exceptionOrNull() as ProfileException
                    ).failure,
                )
            }
        }

    @Test
    fun actualHttpAuthUsesDynamicRsaAndClearsCode() =
        runBlocking {
            MockWebServer().use { server ->
                server.dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse =
                            when (request.path) {
                                "/proxy/hello" -> MockResponse().setBody(LxAuth.HELLO)
                                "/proxy/id" -> MockResponse().setBody(LxAuth.ID_PREFIX + "server")
                                "/proxy/ah" -> {
                                    val plain =
                                        LxCrypto.decrypt(
                                            request.getHeader("m").orEmpty(),
                                            LxCrypto.deriveCodeKey("public-fixture"),
                                        )
                                    val parts = plain.split('\n')
                                    assertEquals("lx_music_mobile", parts[3])
                                    val key =
                                        KeyFactory
                                            .getInstance(
                                                "RSA",
                                            ).generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(parts[1])))
                                    MockResponse().setBody(
                                        LxCrypto.rsaEncrypt(
                                            """{"clientId":"public-device",
                                                "key":"MDEyMzQ1Njc4OWFiY2RlZg==","serverName":"LX"}""",
                                            key,
                                        ),
                                    )
                                }
                                else -> MockResponse().setResponseCode(404)
                            }
                    }
                val http = LxHttp()
                val target = http.inspect(server.url("/proxy").toString(), true)
                val code = "public-fixture".toCharArray()
                val credentials = http.authenticate(target, code, "Android fixture")
                assertEquals("public-device", credentials.clientId)
                assertTrue(code.all { it == '\u0000' })
                assertEquals("LxDeviceCredentials([redacted])", credentials.toString())
            }
        }

    @Test
    fun cancellingAuthenticationConsumesCodeAndStopsRequest() =
        runBlocking {
            MockWebServer().use { server ->
                val waiting = CountDownLatch(1)
                server.dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse =
                            when (request.path) {
                                "/hello" -> MockResponse().setBody(LxAuth.HELLO)
                                "/id" -> MockResponse().setBody(LxAuth.ID_PREFIX + "server")
                                else -> {
                                    waiting.countDown()
                                    MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
                                }
                            }
                    }
                val http = LxHttp()
                val target = http.inspect(server.url("/").toString(), true)
                val code = "public-fixture".toCharArray()
                val job = async(Dispatchers.IO) { http.authenticate(target, code, "fixture") }
                assertTrue(withContext(Dispatchers.IO) { waiting.await(5, TimeUnit.SECONDS) })
                job.cancelAndJoin()
                assertTrue(code.all { it == '\u0000' })
            }
        }
}

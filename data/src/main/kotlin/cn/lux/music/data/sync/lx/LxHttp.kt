package cn.lux.music.data.sync.lx

import cn.lux.music.api.ProfileException
import cn.lux.music.model.LxServer
import cn.lux.music.model.ProfileFailure
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.security.KeyPairGenerator
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class LxHttp {
    private val client =
        OkHttpClient
            .Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .callTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    fun normalized(
        address: String,
        cleartextConfirmed: Boolean,
    ): String {
        val result =
            runCatching {
                require(address.length <= 4096)
                LxEndpoint(address).http("hello").removeSuffix("hello").toHttpUrl()
            }.getOrNull() ?: throw ProfileException(ProfileFailure.INVALID_ADDRESS)
        if (result.scheme == "http" && !cleartextConfirmed) {
            throw ProfileException(ProfileFailure.CLEARTEXT_CONFIRMATION_REQUIRED)
        }
        return result.toString().removeSuffix("/")
    }

    suspend fun inspect(
        address: String,
        cleartextConfirmed: Boolean,
    ): LxServer {
        val base = normalized(address, cleartextConfirmed)
        val endpoint = LxEndpoint(base)
        val hello = get(endpoint.http("hello"))
        val id = get(endpoint.http("id"))
        val serverId =
            try {
                LxAuth.serverId(hello, id)
            } catch (_: LxProtocolException) {
                throw ProfileException(ProfileFailure.INCOMPATIBLE_SERVER)
            }
        return LxServer(base, serverId, cleartextConfirmed)
    }

    suspend fun authenticate(
        server: LxServer,
        code: CharArray,
        deviceName: String,
    ): LxDeviceCredentials {
        try {
            validateLogin(code, deviceName)
            val current = inspect(server.endpoint, server.cleartextConfirmed)
            if (current.serverId != server.serverId) throw ProfileException(ProfileFailure.INCOMPATIBLE_SERVER)
            val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            val headers = LxAuth.initialHeaders(String(code), pair.public, deviceName)
            val body = get(LxEndpoint(current.endpoint).http("ah"), headers, authentication = true)
            return decodeAuth(body, pair.private)
        } finally {
            code.fill('\u0000')
        }
    }

    private fun decodeAuth(
        body: String,
        key: java.security.PrivateKey,
    ): LxDeviceCredentials =
        runCatching { LxAuth.readInitialResponse(body, key) }
            .getOrElse { throw ProfileException(ProfileFailure.INCOMPATIBLE_SERVER) }

    private fun validateLogin(
        code: CharArray,
        name: String,
    ) {
        val validCode = code.isNotEmpty() && code.size <= 512
        val validName = name.isNotBlank() && name.length <= 128 && !name.contains('\n') && !name.contains('\r')
        if (!validCode || !validName) throw ProfileException(ProfileFailure.INVALID_INPUT)
    }

    suspend fun reconnect(
        server: LxServer,
        credentials: LxDeviceCredentials,
        deviceName: String,
    ) {
        val current = inspect(server.endpoint, server.cleartextConfirmed)
        if (current.serverId != server.serverId) throw ProfileException(ProfileFailure.INCOMPATIBLE_SERVER)
        val response =
            get(
                LxEndpoint(current.endpoint).http("ah"),
                LxAuth.reconnectHeaders(credentials, deviceName),
                authentication = true,
            )
        try {
            LxAuth.verifyReconnect(response, credentials)
        } catch (_: LxProtocolException) {
            throw ProfileException(ProfileFailure.INCOMPATIBLE_SERVER)
        }
    }

    fun openSocket(
        server: LxServer,
        credentials: LxDeviceCredentials,
    ): LxSocket {
        normalized(server.endpoint, server.cleartextConfirmed)
        return LxSocket(client, LxEndpoint(server.endpoint).socket(credentials))
    }

    private fun failureForStatus(
        status: Int,
        authentication: Boolean,
    ): ProfileFailure =
        when (status) {
            401 -> if (authentication) ProfileFailure.AUTHORIZATION_REJECTED else ProfileFailure.SERVER
            403 -> if (authentication) ProfileFailure.RATE_LIMITED else ProfileFailure.SERVER
            429 -> ProfileFailure.RATE_LIMITED
            else -> ProfileFailure.SERVER
        }

    private suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        authentication: Boolean = false,
    ): String =
        suspendCancellableCoroutine { continuation ->
            val request =
                Request
                    .Builder()
                    .url(url)
                    .apply {
                        headers.forEach { (name, value) ->
                            header(name, value)
                        }
                    }.build()
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                ProfileException(if (e is SSLException) ProfileFailure.TLS else ProfileFailure.NETWORK),
                            )
                        }
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        try {
                            val text =
                                response.use { result ->
                                    if (!result.isSuccessful) {
                                        throw ProfileException(
                                            failureForStatus(result.code, authentication),
                                        )
                                    }
                                    val body = result.body ?: throw ProfileException(ProfileFailure.SERVER)
                                    if (body.source().request(
                                            65_537,
                                        )
                                    ) {
                                        throw ProfileException(ProfileFailure.INCOMPATIBLE_SERVER)
                                    }
                                    body.string()
                                }
                            if (continuation.isActive) continuation.resume(text)
                        } catch (_: IOException) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    ProfileException(ProfileFailure.NETWORK),
                                )
                            }
                        } catch (error: ProfileException) {
                            if (continuation.isActive) continuation.resumeWithException(error)
                        }
                    }
                },
            )
        }
}

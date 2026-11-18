package cn.lux.music.data.sync.lx

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URI
import java.net.URLEncoder
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Base64
import java.util.Locale

internal class LxDeviceCredentials(
    val clientId: String,
    internal val key: String,
    val serverName: String,
) {
    init {
        require(clientId.isNotEmpty() && !clientId.contains('\n') && !clientId.contains('\r'))
        require(Base64.getDecoder().decode(key).size == 16)
    }

    override fun toString(): String = "LxDeviceCredentials([redacted])"
}

internal object LxAuth {
    const val HELLO = "Hello~::^-^::~v4~"
    const val ID_PREFIX = "OjppZDo6"

    fun serverId(
        hello: String,
        id: String,
    ): String {
        if (hello != HELLO || !id.startsWith(ID_PREFIX) || id.length == ID_PREFIX.length) {
            throw LxProtocolException("Incompatible LX server identity")
        }
        return id.removePrefix(ID_PREFIX)
    }

    fun initialHeaders(
        code: String,
        publicKey: PublicKey,
        deviceName: String,
    ): Map<String, String> {
        checkName(deviceName)
        val body = "lx-music auth::\n${Base64.getEncoder().encodeToString(
            publicKey.encoded,
        )}\n$deviceName\nlx_music_mobile"
        return mapOf("m" to LxCrypto.encrypt(body, LxCrypto.deriveCodeKey(code)))
    }

    fun reconnectHeaders(
        credentials: LxDeviceCredentials,
        deviceName: String,
    ): Map<String, String> {
        checkName(deviceName)
        return mapOf(
            "i" to credentials.clientId,
            "m" to LxCrypto.encrypt("lx-music auth::$deviceName", credentials.key),
        )
    }

    fun readInitialResponse(
        body: String,
        privateKey: PrivateKey,
    ): LxDeviceCredentials {
        val value =
            LxJson.parse(LxCrypto.rsaDecrypt(body, privateKey)) as? JsonObject
                ?: throw LxProtocolException("Invalid LX authentication response")

        fun field(name: String): String =
            (value[name] as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: throw LxProtocolException("Invalid LX authentication response")
        return try {
            LxDeviceCredentials(field("clientId"), field("key"), field("serverName"))
        } catch (
            _: IllegalArgumentException,
        ) {
            throw LxProtocolException("Invalid LX authentication response")
        }
    }

    fun verifyReconnect(
        body: String,
        credentials: LxDeviceCredentials,
    ) {
        if (LxCrypto.decrypt(body, credentials.key) != HELLO) throw LxProtocolException("Invalid LX reconnect response")
    }

    private fun checkName(name: String) {
        require(name.isNotBlank() && !name.contains('\n') && !name.contains('\r')) { "Invalid device name" }
    }
}

internal class LxEndpoint(
    address: String,
) {
    private val base: String
    private val secure: Boolean

    init {
        val uri =
            try {
                URI(address)
            } catch (_: Exception) {
                throw LxProtocolException("Invalid LX endpoint")
            }
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        require(
            scheme in setOf("https", "http") && uri.host != null && uri.rawUserInfo == null &&
                uri.rawQuery == null && uri.rawFragment == null,
        ) { "Invalid LX endpoint" }
        base = scheme + ":" + uri.toASCIIString().substringAfter(':').trimEnd('/') + "/"
        secure = scheme == "https"
    }

    fun http(resource: String): String {
        require(resource in setOf("hello", "id", "ah"))
        return base + resource
    }

    fun socket(credentials: LxDeviceCredentials): String {
        val prefix = if (secure) "wss" else "ws"
        val endpoint = prefix + ":" + base.substringAfter(':')
        val token = LxCrypto.encrypt("lx-music connect", credentials.key)
        return endpoint + "socket?i=" + URLEncoder.encode(credentials.clientId, "UTF-8") +
            "&t=" + URLEncoder.encode(token, "UTF-8")
    }
}

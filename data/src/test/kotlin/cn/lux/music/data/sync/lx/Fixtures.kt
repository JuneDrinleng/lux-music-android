package cn.lux.music.data.sync.lx

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

internal object Fixtures {
    val root = Json.parseToJsonElement(checkNotNull(javaClass.getResource("/lx/vectors.json")).readText()).jsonObject
    val keys = root.getValue("keys").jsonObject
    val publicKey =
        KeyFactory
            .getInstance(
                "RSA",
            ).generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(keys.text("publicDer"))))
    val privateKey =
        KeyFactory
            .getInstance(
                "RSA",
            ).generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(keys.text("privateDer"))))

    fun cases(name: String) = (root.getValue(name) as JsonArray).map { it.jsonObject }

    fun credentials(): LxDeviceCredentials =
        root.getValue("auth").jsonObject.getValue("decoded").jsonObject.let {
            LxDeviceCredentials(it.text("clientId"), it.text("key"), it.text("serverName"))
        }
}

internal fun JsonObject.text(name: String): String = getValue(name).jsonPrimitive.content

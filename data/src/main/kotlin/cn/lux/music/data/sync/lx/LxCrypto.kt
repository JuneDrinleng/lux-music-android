package cn.lux.music.data.sync.lx

import android.annotation.SuppressLint
import java.nio.charset.StandardCharsets.UTF_8
import java.security.GeneralSecurityException
import java.security.Key
import java.security.MessageDigest
import java.security.spec.MGF1ParameterSpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

/** Legacy wire crypto only; never used as the local credential vault. */
internal object LxCrypto {
    fun md5(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun deriveCodeKey(code: String): String {
        require(code.isNotEmpty()) { "Connection code required" }
        val characters = md5(code).take(16).toByteArray(UTF_8)
        return Base64.getEncoder().encodeToString(characters)
    }

    fun encrypt(
        text: String,
        key: String,
    ): String {
        val encrypted = crypt(Cipher.ENCRYPT_MODE, text.toByteArray(UTF_8), key)
        return Base64.getEncoder().encodeToString(encrypted)
    }

    fun decrypt(
        text: String,
        key: String,
    ): String =
        try {
            val bytes = Base64.getDecoder().decode(text)
            crypt(Cipher.DECRYPT_MODE, bytes, key).toString(UTF_8)
        } catch (_: IllegalArgumentException) {
            throw LxProtocolException("Invalid LX ciphertext")
        }

    // LX v4 requires this exact mode; never use it for the credential vault.
    @SuppressLint("GetInstance")
    private fun crypt(
        mode: Int,
        bytes: ByteArray,
        key: String,
    ): ByteArray =
        try {
            val decoded = Base64.getDecoder().decode(key)
            if (decoded.size != 16) throw LxProtocolException("Invalid LX AES key")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(mode, SecretKeySpec(decoded, "AES"))
            cipher.doFinal(bytes)
        } catch (_: GeneralSecurityException) {
            throw LxProtocolException("LX AES operation failed")
        } catch (_: IllegalArgumentException) {
            throw LxProtocolException("Invalid LX AES key")
        }

    fun rsaEncrypt(
        text: String,
        key: Key,
    ): String {
        val encrypted = rsa(Cipher.ENCRYPT_MODE, text.toByteArray(UTF_8), key)
        return Base64.getEncoder().encodeToString(encrypted)
    }

    fun rsaDecrypt(
        text: String,
        key: Key,
    ): String =
        try {
            val bytes = Base64.getDecoder().decode(text)
            rsa(Cipher.DECRYPT_MODE, bytes, key).toString(UTF_8)
        } catch (_: IllegalArgumentException) {
            throw LxProtocolException("Invalid LX RSA ciphertext")
        }

    private fun rsa(
        mode: Int,
        bytes: ByteArray,
        key: Key,
    ): ByteArray =
        try {
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
            val parameters = OAEPParameterSpec("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
            cipher.init(mode, key, parameters)
            cipher.doFinal(bytes)
        } catch (_: GeneralSecurityException) {
            throw LxProtocolException("LX RSA operation failed")
        }
}

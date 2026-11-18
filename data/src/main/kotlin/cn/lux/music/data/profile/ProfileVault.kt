package cn.lux.music.data.profile

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import cn.lux.music.data.sync.lx.LxDeviceCredentials
import cn.lux.music.model.ProfileId
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class ProfileVault {
    private val alias = "lux.profile.credentials.v1"

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (store.containsAlias(alias)) return store.getKey(alias, null) as SecretKey
        check(create) { "Profile key is unavailable" }
        return KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec
                        .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
            }.generateKey()
    }

    private fun aad(id: ProfileId): ByteArray = "${id.value}\u0000lx-device\u00001".toByteArray(Charsets.UTF_8)

    @Synchronized
    fun encrypt(
        id: ProfileId,
        credentials: LxDeviceCredentials,
    ): ByteArray {
        val bytes =
            ByteArrayOutputStream().use { output ->
                DataOutputStream(output).use {
                    it.writeUTF(credentials.clientId)
                    it.writeUTF(credentials.key)
                    it.writeUTF(credentials.serverName)
                }
                output.toByteArray()
            }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key(create = true))
            cipher.updateAAD(aad(id))
            check(cipher.iv.size == 12)
            byteArrayOf(1) + cipher.iv + cipher.doFinal(bytes)
        } finally {
            bytes.fill(0)
        }
    }

    @Synchronized
    fun decrypt(
        id: ProfileId,
        value: ByteArray,
    ): LxDeviceCredentials {
        require(value.size >= 29 && value[0] == 1.toByte())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, value.copyOfRange(1, 13)))
        cipher.updateAAD(aad(id))
        val bytes = cipher.doFinal(value.copyOfRange(13, value.size))
        return try {
            DataInputStream(ByteArrayInputStream(bytes)).use { input ->
                val result = LxDeviceCredentials(input.readUTF(), input.readUTF(), input.readUTF())
                require(input.available() == 0)
                result
            }
        } finally {
            bytes.fill(0)
        }
    }
}

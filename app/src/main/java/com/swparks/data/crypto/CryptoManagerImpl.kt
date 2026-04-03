package com.swparks.data.crypto

import android.content.Context
import android.util.Log
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

class CryptoManagerImpl(
    private val context: Context,
    private val keysetName: String = "auth_token_keyset",
    private val prefFileName: String = "tink_prefs"
) : CryptoManager {
    private companion object {
        private const val TAG = "CryptoManager"
    }

    @Suppress("DEPRECATION")
    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager
            .Builder()
            .withSharedPref(context, keysetName, prefFileName)
            .withKeyTemplate(KeyTemplates.get("AES128_GCM"))
            .withMasterKeyUri("android-keystore://master_key_alias")
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    override fun encrypt(data: ByteArray): ByteArray {
        val encrypted = aead.encrypt(data, null)
        Log.d(TAG, "Encrypted ${data.size} -> ${encrypted.size} bytes")
        return encrypted
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray =
        try {
            val decrypted = aead.decrypt(ciphertext, null)
            Log.d(TAG, "Decrypted ${ciphertext.size} -> ${decrypted.size} bytes")
            decrypted
        } catch (e: SecurityException) {
            Log.e(TAG, "Decryption failed", e)
            throw e
        }
}

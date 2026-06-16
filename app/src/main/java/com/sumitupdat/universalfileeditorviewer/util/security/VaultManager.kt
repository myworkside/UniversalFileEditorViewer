package com.sumitupdat.universalfileeditorviewer.util.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Enterprise-grade VaultManager with Key Versioning and StrongBox support.
 */
object VaultManager {
    private const val TAG = "VaultManager"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128
    
    const val LATEST_KEY_VERSION = 2

    init {
        ensureKeyGenerated(LATEST_KEY_VERSION)
    }

    fun getKeyAlias(version: Int) = "UniversalVaultKey_v$version"

    fun isKeyHealthy(version: Int = LATEST_KEY_VERSION): Boolean {
        return try {
            getSecretKey(version)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun ensureKeyGenerated(version: Int) {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val alias = getKeyAlias(version)
            if (!keyStore.containsAlias(alias)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
                )
                
                val specBuilder = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        specBuilder.setIsStrongBoxBacked(true)
                        keyGenerator.init(specBuilder.build())
                        keyGenerator.generateKey()
                    } catch (e: Exception) {
                        specBuilder.setIsStrongBoxBacked(false)
                        keyGenerator.init(specBuilder.build())
                        keyGenerator.generateKey()
                    }
                } else {
                    keyGenerator.init(specBuilder.build())
                    keyGenerator.generateKey()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Key generation failed for v$version", e)
        }
    }

    private fun getSecretKey(version: Int): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(getKeyAlias(version), null) as SecretKey
    }

    fun encryptStream(inputStream: InputStream, outputStream: OutputStream, version: Int = LATEST_KEY_VERSION): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(version))
        val iv = cipher.iv

        CipherOutputStream(outputStream, cipher).use { cos ->
            inputStream.copyTo(cos)
            cos.flush()
        }
        return iv
    }

    fun decryptStream(inputStream: InputStream, outputStream: OutputStream, iv: ByteArray, version: Int) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(version), spec)

        CipherInputStream(inputStream, cipher).use { cis ->
            cis.copyTo(outputStream)
            outputStream.flush()
        }
    }
}

package com.sumitupdat.universalfileeditorviewer.util.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "vault_security")

@Singleton
class VaultSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "VAULT_SEC"
    private val KEY_ALIAS = "VaultPinKey"
    private val TRANSFORMATION = "AES/GCM/NoPadding"
    private val ITERATIONS = 10000
    private val KEY_LENGTH = 256
    
    private val PIN_HASH_ENC = stringPreferencesKey("pin_hash_enc")
    private val PIN_IV = stringPreferencesKey("pin_iv")
    private val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
    private val LOCKOUT_UNTIL = longPreferencesKey("lockout_until")

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    init {
        generateSecretKey()
    }

    private fun generateSecretKey() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun encrypt(data: ByteArray): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val encrypted = cipher.doFinal(data)
        val iv = cipher.iv
        return Base64.encodeToString(encrypted, Base64.NO_WRAP) to Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String, ivBase64: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, Base64.decode(ivBase64, Base64.NO_WRAP))
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher.doFinal(Base64.decode(encryptedBase64, Base64.NO_WRAP))
    }

    suspend fun isPinSet(): Boolean {
        return context.securityDataStore.data.map { it[PIN_HASH_ENC] != null }.first()
    }

    suspend fun setPin(pin: String): Boolean {
        try {
            Log.d(TAG, "Setting new PIN...")
            val salt = generateSalt()
            val hash = hashPin(pin, salt)
            
            // For maximum security, combine hash and salt then encrypt with Keystore
            val combined = hash + salt
            val (encCombined, iv) = encrypt(combined)

            context.securityDataStore.edit { prefs ->
                prefs[PIN_HASH_ENC] = encCombined
                prefs[PIN_IV] = iv
                prefs[FAILED_ATTEMPTS] = 0
                prefs[LOCKOUT_UNTIL] = 0L
            }
            Log.d(TAG, "Encrypted PIN hash and salt stored successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting PIN", e)
            return false
        }
    }

    suspend fun verifyPin(pin: String): PinVerificationResult {
        Log.d(TAG, "Verifying PIN entered...")
        val prefs = context.securityDataStore.data.first()
        val lockoutUntil = prefs[LOCKOUT_UNTIL] ?: 0L
        
        if (System.currentTimeMillis() < lockoutUntil) {
            Log.w(TAG, "Verification blocked: Locked out")
            return PinVerificationResult.Locked(lockoutUntil)
        }

        val encCombined = prefs[PIN_HASH_ENC] ?: return PinVerificationResult.Error("PIN not set")
        val iv = prefs[PIN_IV] ?: return PinVerificationResult.Error("IV missing")
        
        return try {
            val combined = decrypt(encCombined, iv)
            val storedHash = combined.sliceArray(0 until 32) // PBKDF2 with 256 bits = 32 bytes
            val salt = combined.sliceArray(32 until combined.size)
            
            val inputHash = hashPin(pin, salt)

            if (MessageDigest.isEqual(storedHash, inputHash)) {
                Log.d(TAG, "PIN verified successfully")
                context.securityDataStore.edit { it[FAILED_ATTEMPTS] = 0 }
                PinVerificationResult.Success
            } else {
                val failedCount = (prefs[FAILED_ATTEMPTS] ?: 0) + 1
                Log.w(TAG, "Invalid PIN attempt: $failedCount")
                context.securityDataStore.edit { it[FAILED_ATTEMPTS] = failedCount }
                
                if (failedCount >= 5) {
                    val lockoutTime = System.currentTimeMillis() + (30 * 60 * 1000)
                    context.securityDataStore.edit { it[LOCKOUT_UNTIL] = lockoutTime }
                    PinVerificationResult.Locked(lockoutTime)
                } else {
                    PinVerificationResult.Invalid(5 - failedCount)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed during verification", e)
            PinVerificationResult.Error("Security error: ${e.message}")
        }
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    sealed class PinVerificationResult {
        object Success : PinVerificationResult()
        data class Invalid(val remainingAttempts: Int) : PinVerificationResult()
        data class Locked(val until: Long) : PinVerificationResult()
        data class Error(val message: String) : PinVerificationResult()
    }
}

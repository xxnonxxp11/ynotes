package app.uamo.ynotes.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoManager {
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    private const val KEY_ALIAS = "ynotes_safe_zone_key"

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private fun getSecretKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createSecretKey()
    }

    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, "AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .build()
        )
        return keyGenerator.generateKey()
    }

    fun encrypt(text: String): String {
        if (text.isBlank()) return text
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedText = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
            
            // Combine IV and encryptedText, then encode to Base64
            val combined = ByteArray(iv.size + encryptedText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedText, 0, combined, iv.size, encryptedText.size)
            
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            // If encryption fails, fallback to plaintext
            return text
        }
    }

    fun decrypt(encryptedTextBase64: String): String {
        if (encryptedTextBase64.isBlank()) return encryptedTextBase64
        try {
            val combined = Base64.decode(encryptedTextBase64, Base64.DEFAULT)
            // GCM IV is 12 bytes
            if (combined.size < 12) return encryptedTextBase64
            
            val iv = combined.copyOfRange(0, 12)
            val encryptedText = combined.copyOfRange(12, combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val decryptedBytes = cipher.doFinal(encryptedText)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            // If decryption fails (e.g. text wasn't encrypted), fallback to returning original string
            return encryptedTextBase64
        }
    }
}

package app.uamo.ynotes.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.uamo.ynotes.data.NoteEntity
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
            
            val combined = ByteArray(iv.size + encryptedText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedText, 0, combined, iv.size, encryptedText.size)
            
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return text
        }
    }

    fun decrypt(encryptedTextBase64: String): String {
        if (encryptedTextBase64.isBlank()) return encryptedTextBase64
        try {
            val combined = Base64.decode(encryptedTextBase64, Base64.DEFAULT)
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
            return encryptedTextBase64
        }
    }

    /**
     * Batch-decrypt a list of NoteEntity objects.
     * Resolves the key once and reuses it for all notes — much faster than
     * calling decrypt() individually for each field.
     */
    fun decryptBatch(notes: List<NoteEntity>): List<NoteEntity> {
        if (notes.isEmpty()) return notes
        return try {
            val key = getSecretKey()
            notes.map { note ->
                note.copy(
                    title = decryptWithKey(note.title, key),
                    body = decryptWithKey(note.body, key)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            notes // Fallback: return as-is
        }
    }

    /**
     * Batch-encrypt: resolves key once, encrypts title + body for each note.
     */
    fun encryptFields(title: String, body: String): Pair<String, String> {
        return try {
            val key = getSecretKey()
            Pair(encryptWithKey(title, key), encryptWithKey(body, key))
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(title, body)
        }
    }

    private fun encryptWithKey(text: String, key: SecretKey): String {
        if (text.isBlank()) return text
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encryptedText = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        
        val combined = ByteArray(iv.size + encryptedText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedText, 0, combined, iv.size, encryptedText.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    private fun decryptWithKey(encryptedTextBase64: String, key: SecretKey): String {
        if (encryptedTextBase64.isBlank()) return encryptedTextBase64
        try {
            val combined = Base64.decode(encryptedTextBase64, Base64.DEFAULT)
            if (combined.size < 12) return encryptedTextBase64
            
            val iv = combined.copyOfRange(0, 12)
            val encryptedText = combined.copyOfRange(12, combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedText)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return encryptedTextBase64
        }
    }
}

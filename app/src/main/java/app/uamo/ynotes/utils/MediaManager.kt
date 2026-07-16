package app.uamo.ynotes.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

object MediaManager {

    private const val MEDIA_DIR = "media"
    private const val MEDIA_ENCRYPTED_DIR = "media_encrypted"

    private fun getMediaDir(context: Context, noteId: String, isSecret: Boolean): File {
        val baseName = if (isSecret) MEDIA_ENCRYPTED_DIR else MEDIA_DIR
        val dir = File(context.filesDir, "$baseName/$noteId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Save a media file from a content URI to the note's media directory.
     * If isSecret, encrypts the file on write using streaming AES-GCM.
     * Returns the generated filename.
     */
    fun saveMedia(context: Context, noteId: String, uri: Uri, isSecret: Boolean): String? {
        return try {
            val fileName = "img_${UUID.randomUUID().toString().take(8)}.jpg"
            val dir = getMediaDir(context, noteId, isSecret)
            val targetFile = File(dir, if (isSecret) fileName.replace(".jpg", ".enc") else fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                if (isSecret) {
                    FileOutputStream(targetFile).use { output ->
                        CryptoManager.encryptFile(input, output)
                    }
                } else {
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output, bufferSize = 8192)
                    }
                }
            } ?: return null

            fileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Load a bitmap from a note's media file.
     * If isSecret, decrypts the file first.
     * Uses inSampleSize for memory-efficient thumbnail loading.
     */
    fun loadMediaBitmap(
        context: Context,
        noteId: String,
        fileName: String,
        isSecret: Boolean,
        maxSize: Int = 512
    ): Bitmap? {
        return try {
            if (isSecret) {
                val encFile = File(getMediaDir(context, noteId, true), fileName.replace(".jpg", ".enc"))
                if (!encFile.exists()) return null
                // Decrypt to a temp byte array, then decode
                val decryptedBytes = java.io.ByteArrayOutputStream().use { baos ->
                    FileInputStream(encFile).use { input ->
                        CryptoManager.decryptFile(input, baos)
                    }
                    baos.toByteArray()
                }
                decodeSampledBitmap(decryptedBytes, maxSize)
            } else {
                val file = File(getMediaDir(context, noteId, false), fileName)
                if (!file.exists()) return null
                decodeSampledBitmapFromFile(file.absolutePath, maxSize)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Delete all media files for a note.
     */
    fun deleteNoteMedia(context: Context, noteId: String, isSecret: Boolean) {
        try {
            val dir = File(context.filesDir, "${if (isSecret) MEDIA_ENCRYPTED_DIR else MEDIA_DIR}/$noteId")
            if (dir.exists()) dir.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Delete a single media file.
     */
    fun deleteMediaFile(context: Context, noteId: String, fileName: String, isSecret: Boolean) {
        try {
            val dir = getMediaDir(context, noteId, isSecret)
            val file = if (isSecret) {
                File(dir, fileName.replace(".jpg", ".enc"))
            } else {
                File(dir, fileName)
            }
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun decodeSampledBitmapFromFile(path: String, maxSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    private fun decodeSampledBitmap(data: ByteArray, maxSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, options)
        options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(data, 0, data.size, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

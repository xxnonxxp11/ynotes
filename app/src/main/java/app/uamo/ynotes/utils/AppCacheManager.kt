package app.uamo.ynotes.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Manages a disk cache of hidden app info (name, package, icon) so the
 * SafeZone shortcut row can render instantly without scanning all installed apps.
 */
object AppCacheManager {

    private const val CACHE_FILE = "hidden_apps_cache.json"
    private val gson = Gson()

    /** Lightweight serializable DTO — icon stored as Base64 PNG */
    private data class CachedApp(
        val packageName: String,
        val name: String,
        val iconBase64: String
    )

    /** Save the current list of hidden apps to disk cache */
    fun saveHiddenApps(context: Context, apps: List<AppInfo>) {
        val cached = apps.map { app ->
            CachedApp(
                packageName = app.packageName,
                name = app.name,
                iconBase64 = imageBitmapToBase64(app.icon)
            )
        }
        val json = gson.toJson(cached)
        getCacheFile(context).writeText(json)
    }

    /** Load cached hidden apps — returns empty list if no cache exists */
    fun loadHiddenApps(context: Context): List<AppInfo> {
        val file = getCacheFile(context)
        if (!file.exists()) return emptyList()

        return try {
            val json = file.readText()
            val type = object : TypeToken<List<CachedApp>>() {}.type
            val cached: List<CachedApp> = gson.fromJson(json, type)
            cached.map { c ->
                AppInfo(
                    packageName = c.packageName,
                    name = c.name,
                    icon = base64ToImageBitmap(c.iconBase64)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Clear the cache file */
    fun clearCache(context: Context) {
        val file = getCacheFile(context)
        if (file.exists()) file.delete()
    }

    private fun getCacheFile(context: Context): File {
        return File(context.filesDir, CACHE_FILE)
    }

    private fun imageBitmapToBase64(imageBitmap: ImageBitmap): String {
        val bitmap = imageBitmap.asAndroidBitmap()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun base64ToImageBitmap(base64: String): ImageBitmap {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return bitmap.asImageBitmap()
    }
}

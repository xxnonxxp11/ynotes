package app.uamo.ynotes

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.uamo.ynotes.ui.AppNavigation
import app.uamo.ynotes.ui.NotesViewModel
import app.uamo.ynotes.ui.theme.YNotesTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private data class LegacyNote(
    val id: String,
    val title: String,
    val body: String,
    val isSecret: Boolean = false
)

class MainActivity : FragmentActivity() {
    
    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotesApp(viewModel, this)
                }
            }
        }
    }
}

@Composable
fun NotesApp(viewModel: NotesViewModel, activity: FragmentActivity) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE) }
    
    // Legacy Data Migration
    LaunchedEffect(Unit) {
        val hasMigrated = sharedPref.getBoolean("MIGRATED_TO_ROOM", false)
        if (!hasMigrated) {
            val json = sharedPref.getString("NOTES_LIST", "[]")
            if (json != null && json != "[]") {
                val gson = Gson()
                val type = object : TypeToken<List<LegacyNote>>() {}.type
                val legacyNotes: List<LegacyNote> = gson.fromJson(json, type)
                
                legacyNotes.forEach { note ->
                    viewModel.saveNote(note.id, note.title, note.body, note.isSecret)
                }
            }
            sharedPref.edit().putBoolean("MIGRATED_TO_ROOM", true).apply()
        }
    }

    val hasSeenWelcome = sharedPref.getBoolean("HAS_SEEN_WELCOME", false)
    val startDestination = if (hasSeenWelcome) "home" else "welcome"
    
    val safeZonePassword = remember { 
        mutableStateOf(sharedPref.getString("SAFE_ZONE_PWD", "") ?: "") 
    }
    
    val isBiometricEnabled = remember {
        mutableStateOf(sharedPref.getBoolean("BIOMETRIC_ENABLED", false))
    }

    AppNavigation(
        viewModel = viewModel,
        startDestination = startDestination,
        safeZonePassword = safeZonePassword,
        isBiometricEnabled = isBiometricEnabled,
        onSaveSafeZonePassword = { newPwd ->
            val editor = sharedPref.edit()
            if (safeZonePassword.value.isEmpty() && newPwd.isNotEmpty()) {
                editor.putBoolean("BIOMETRIC_ENABLED", true)
                isBiometricEnabled.value = true
            }
            editor.putString("SAFE_ZONE_PWD", newPwd)
            editor.apply()
            safeZonePassword.value = newPwd
        },
        onBiometricToggle = { enabled ->
            sharedPref.edit().putBoolean("BIOMETRIC_ENABLED", enabled).apply()
            isBiometricEnabled.value = enabled
        }
    )
}

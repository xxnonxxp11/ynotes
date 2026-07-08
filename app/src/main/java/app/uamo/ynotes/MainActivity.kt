package app.uamo.ynotes

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import app.uamo.ynotes.ui.AppNavigation
import app.uamo.ynotes.ui.NotesViewModel
import app.uamo.ynotes.ui.theme.YNotesTheme

class MainActivity : FragmentActivity() {

    private val notesViewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val sharedPref = remember { getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE) }
                    
                    val safeZonePassword = remember { 
                        mutableStateOf(sharedPref.getString("SAFE_ZONE_PWD", "") ?: "") 
                    }
                    
                    val isBiometricEnabled = remember {
                        mutableStateOf(sharedPref.getBoolean("BIOMETRIC_ENABLED", false))
                    }
                    
                    val hasSeenWelcome = sharedPref.getBoolean("HAS_SEEN_WELCOME", false)
                    val startDestination = if (hasSeenWelcome) "home" else "welcome"

                    AppNavigation(
                        viewModel = notesViewModel,
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
                        },
                        onWelcomeCompleted = {
                            sharedPref.edit().putBoolean("HAS_SEEN_WELCOME", true).apply()
                        }
                    )
                }
            }
        }
    }
}

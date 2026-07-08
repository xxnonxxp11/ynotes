package app.uamo.ynotes

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import app.uamo.ynotes.ui.AppNavigation
import app.uamo.ynotes.ui.NotesViewModel
import app.uamo.ynotes.ui.theme.YNotesTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YNotesTheme {
                val notesViewModel: NotesViewModel = viewModel()
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

                    var isAuthenticated by remember { mutableStateOf(!isBiometricEnabled.value) }

                    if (!isAuthenticated) {
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            val biometricManager = androidx.biometric.BiometricManager.from(this@MainActivity)
                            if (biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
                                val executor = androidx.core.content.ContextCompat.getMainExecutor(this@MainActivity)
                                val biometricPrompt = androidx.biometric.BiometricPrompt(this@MainActivity, executor, object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                        isAuthenticated = true
                                    }
                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED) {
                                            // Handle other errors or let user retry
                                            finish()
                                        } else {
                                            finish() // If they cancel, exit app
                                        }
                                    }
                                })
                                val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Desbloquear yNotes")
                                    .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                                    .build()
                                biometricPrompt.authenticate(promptInfo)
                            } else {
                                isAuthenticated = true
                            }
                        }
                        // Blank screen while authenticating
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
                    } else {
                        AppNavigation(
                            viewModel = notesViewModel,
                            startDestination = startDestination,
                            safeZonePassword = safeZonePassword,
                            isBiometricEnabled = isBiometricEnabled,
                            onSaveSafeZonePassword = { newPwd ->
                                val editor = sharedPref.edit()
                                // No longer auto-enabling biometric app lock here, just save the password
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
}

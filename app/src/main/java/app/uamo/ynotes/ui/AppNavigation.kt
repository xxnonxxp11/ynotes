package app.uamo.ynotes.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.uamo.ynotes.ui.screens.*

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun AppNavigation(
    viewModel: NotesViewModel,
    startDestination: String,
    safeZonePassword: MutableState<String>,
    isBiometricEnabled: MutableState<Boolean>,
    onSaveSafeZonePassword: (String) -> Unit,
    onBiometricToggle: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    val publicNotes by viewModel.publicNotes.collectAsState()
    val secretNotes by viewModel.secretNotes.collectAsState()
    
    val executor = ContextCompat.getMainExecutor(context)
    val onRequestSafeZone: () -> Unit = {
        if (isBiometricEnabled.value && activity != null) {
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        navController.navigate("safe_zone")
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Zona Segura")
                .setSubtitle("Confirma tu identidad para acceder")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            navController.navigate("safe_zone")
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        
        composable("welcome") {
            WelcomeScreen(
                onStart = {
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                notes = publicNotes,
                safeZonePassword = safeZonePassword.value,
                onRequestSafeZone = onRequestSafeZone,
                onAddNote = {
                    navController.navigate("editor/public/new")
                },
                onNoteClick = { note ->
                    navController.navigate("editor/public/${note.id}")
                },
                onSettingsClick = {
                    navController.navigate("settings/home")
                }
            )
        }

        composable("safe_zone") {
            SafeZoneScreen(
                notes = secretNotes,
                onDeactivateSafeZone = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onAddNote = {
                    navController.navigate("editor/secret/new")
                },
                onNoteClick = { note ->
                    navController.navigate("editor/secret/${note.id}")
                },
                onSettingsClick = {
                    navController.navigate("settings/safe_zone")
                },
                onBooksClick = {
                    navController.navigate("books")
                }
            )
        }

        composable(
            route = "editor/{type}/{noteId}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("noteId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "public"
            val isSecret = type == "secret"
            val noteId = backStackEntry.arguments?.getString("noteId")
            
            val editingNote = if (noteId != "new") {
                if (isSecret) secretNotes.find { it.id == noteId }
                else publicNotes.find { it.id == noteId }
            } else null

            EditorScreen(
                editingNote = editingNote,
                isSecret = isSecret,
                onSave = { id, title, body ->
                    viewModel.saveNote(id, title, body, isSecret)
                },
                onDelete = { id ->
                    viewModel.deleteNote(id)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "settings/{from}",
            arguments = listOf(navArgument("from") { type = NavType.StringType })
        ) { backStackEntry ->
            val from = backStackEntry.arguments?.getString("from") ?: "home"
            val isFromSafeZone = from == "safe_zone"
            
            SettingsScreen(
                currentPassword = safeZonePassword.value,
                onSavePassword = { newPwd ->
                    onSaveSafeZonePassword(newPwd)
                    if (newPwd.isEmpty() && isFromSafeZone) {
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                isBiometricEnabled = isBiometricEnabled.value,
                onBiometricToggle = { enabled ->
                    onBiometricToggle(enabled)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("books") {
            BooksScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

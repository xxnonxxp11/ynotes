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
    safeZoneTriggerMode: State<Int>,
    isBiometricEnabled: MutableState<Boolean>,
    isAppHidingEnabled: MutableState<Boolean>,
    onSaveSafeZone: (String, Int) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onAppHidingToggle: (Boolean) -> Unit,
    onWelcomeCompleted: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    val publicNotes by viewModel.publicNotes.collectAsState()
    val secretNotes by viewModel.secretNotes.collectAsState()
    
    val executor = ContextCompat.getMainExecutor(context)

    // Full safe zone request (respects biometric setting)
    val onRequestSafeZone: () -> Unit = {
        if (activity != null) {
            val biometricManager = BiometricManager.from(context)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                val biometricPrompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            viewModel.unlockSafeZone()
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
                viewModel.unlockSafeZone()
                navController.navigate("safe_zone")
            }
        } else {
            viewModel.unlockSafeZone()
            navController.navigate("safe_zone")
        }
    }

    // Biometric-only: skips password, goes straight to fingerprint
    val onRequestSafeZoneBiometric: () -> Unit = {
        if (activity != null) {
            val biometricManager = BiometricManager.from(context)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                val biometricPrompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            viewModel.unlockSafeZone()
                            navController.navigate("safe_zone")
                        }
                    })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Zona Segura")
                    .setSubtitle("Confirma tu huella para acceder")
                    .setNegativeButtonText("Cancelar")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            } else {
                // Fallback if biometric not available
                viewModel.unlockSafeZone()
                navController.navigate("safe_zone")
            }
        } else {
            viewModel.unlockSafeZone()
            navController.navigate("safe_zone")
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        
        composable("welcome") {
            WelcomeScreen(
                onStart = {
                    onWelcomeCompleted()
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
                safeZoneTriggerMode = safeZoneTriggerMode.value,
                isBiometricEnabled = isBiometricEnabled.value,
                onRequestSafeZone = onRequestSafeZone,
                onRequestSafeZoneBiometric = onRequestSafeZoneBiometric,
                onAddNote = {
                    navController.navigate("editor/public/new")
                },
                onNoteClick = { note ->
                    navController.navigate("editor/public/${note.id}")
                },
                onSettingsClick = {
                    navController.navigate("settings/home")
                },
                onBooksClick = {
                    navController.navigate("books/public")
                },
                onTrashClick = {
                    navController.navigate("trash")
                }
            )
        }

        composable("safe_zone") {
            SafeZoneScreen(
                notes = secretNotes,
                isAppHidingEnabled = isAppHidingEnabled.value,
                onDeactivateSafeZone = {
                    viewModel.lockSafeZone()
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
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
                    navController.navigate("books/secret")
                },
                onTrashClick = {
                    navController.navigate("trash")
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

            val books by viewModel.books.collectAsState()

            EditorScreen(
                editingNote = editingNote,
                isSecret = isSecret,
                books = books.filter { it.isSecret == isSecret },
                onSave = { id, title, body, color, isPinned, bookId, isBodyHidden ->
                    // id is always the stableNoteId from EditorScreen (never null)
                    // Preserve createdAt from existing note if it exists
                    val existingCreatedAt = if (isSecret) secretNotes.find { it.id == id }?.createdAt
                                           else publicNotes.find { it.id == id }?.createdAt
                    viewModel.saveNote(
                        id = id,
                        title = title,
                        body = body,
                        isSecret = isSecret,
                        color = color,
                        isPinned = isPinned,
                        bookId = bookId,
                        isBodyHidden = isBodyHidden,
                        existingCreatedAt = existingCreatedAt
                    )
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
                isFromSafeZone = isFromSafeZone,
                currentPassword = safeZonePassword.value,
                currentTriggerMode = safeZoneTriggerMode.value,
                onSaveSafeZone = { newPwd, mode ->
                    onSaveSafeZone(newPwd, mode)
                    if (newPwd.isEmpty() && isFromSafeZone) {
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                isBiometricEnabled = isBiometricEnabled.value,
                isAppHidingEnabled = isAppHidingEnabled.value,
                onBiometricToggle = { enabled ->
                    onBiometricToggle(enabled)
                },
                onAppHidingToggle = { enabled ->
                    onAppHidingToggle(enabled)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "books/{zone}",
            arguments = listOf(navArgument("zone") { type = NavType.StringType })
        ) { backStackEntry ->
            val zone = backStackEntry.arguments?.getString("zone") ?: "public"
            val isSecret = zone == "secret"
            val allBooks by viewModel.books.collectAsState()
            val books = allBooks.filter { it.isSecret == isSecret }
            
            BooksScreen(
                books = books,
                onAddBook = { name, color ->
                    viewModel.saveBook(id = null, name = name, color = color, iconName = "MenuBook", isSecret = isSecret)
                },
                onDeleteBook = { id ->
                    viewModel.deleteBook(id)
                },
                onBookClick = { bookId ->
                    navController.navigate("book_notes/$bookId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "book_notes/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
            val books by viewModel.books.collectAsState()
            val book = books.find { it.id == bookId } ?: return@composable

            val allNotes = if (book.isSecret) secretNotes else publicNotes
            val bookNotes = allNotes.filter { it.bookId == bookId }

            BookNotesScreen(
                book = book,
                notes = bookNotes,
                onNoteClick = { note ->
                    navController.navigate("editor/${if (book.isSecret) "secret" else "public"}/${note.id}")
                },
                onAddNote = {
                    navController.navigate("editor/${if (book.isSecret) "secret" else "public"}/new")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("trash") {
            val deletedNotes by viewModel.deletedNotes.collectAsState()
            TrashScreen(
                deletedNotes = deletedNotes,
                onRestore = { id ->
                    viewModel.restoreFromTrash(id)
                },
                onEmptyTrash = {
                    viewModel.emptyTrash()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

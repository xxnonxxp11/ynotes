package com.example.ynotes

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ynotes.ui.theme.YNotesTheme
import java.util.UUID

data class Note(
    val id: String,
    val title: String,
    val body: String,
    val isSecret: Boolean = false
)

sealed class Screen {
    object Home : Screen()
    data class Editor(val note: Note? = null, val isSecret: Boolean = false) : Screen()
    object Settings : Screen()
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotesApp()
                }
            }
        }
    }
}

@Composable
fun NotesApp() {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE) }
    
    var safeZonePassword by remember { 
        mutableStateOf(sharedPref.getString("SAFE_ZONE_PWD", "") ?: "") 
    }
    
    var isBiometricEnabled by remember {
        mutableStateOf(sharedPref.getBoolean("BIOMETRIC_ENABLED", false))
    }
    
    // Si la biometría está habilitada, la app arranca bloqueada
    var isUnlocked by remember { mutableStateOf(!isBiometricEnabled) }

    val notes = remember { mutableStateListOf<Note>() }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var isSafeZoneActive by remember { mutableStateOf(false) }

    if (isBiometricEnabled && !isUnlocked) {
        // Lanzamos el prompt de autenticación inmediatamente
        LaunchedEffect(Unit) {
            val fragmentActivity = context as FragmentActivity
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(fragmentActivity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isUnlocked = true
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Si falla o el usuario cancela, no lo dejamos entrar. 
                        // Dejamos la pantalla con el candado.
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear yNotes")
                .setSubtitle("Usa tu huella o PIN para acceder a tus notas")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        }

        // Pantalla de bloqueo
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Lock, 
                    contentDescription = "Bloqueado", 
                    modifier = Modifier.size(80.dp), 
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "yNotes está bloqueado",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        return // No renderizamos nada más hasta que se desbloquee
    }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                notes = notes,
                safeZonePassword = safeZonePassword,
                isSafeZoneActive = isSafeZoneActive,
                onActivateSafeZone = { isSafeZoneActive = true },
                onDeactivateSafeZone = { isSafeZoneActive = false },
                onAddNote = { currentScreen = Screen.Editor(note = null, isSecret = isSafeZoneActive) },
                onNoteClick = { note -> currentScreen = Screen.Editor(note = note, isSecret = isSafeZoneActive) },
                onSettingsClick = { currentScreen = Screen.Settings }
            )
        }
        is Screen.Settings -> {
            SettingsScreen(
                currentPassword = safeZonePassword,
                onSavePassword = { newPwd ->
                    sharedPref.edit().putString("SAFE_ZONE_PWD", newPwd).apply()
                    safeZonePassword = newPwd
                },
                isBiometricEnabled = isBiometricEnabled,
                onBiometricToggle = { enabled ->
                    sharedPref.edit().putBoolean("BIOMETRIC_ENABLED", enabled).apply()
                    isBiometricEnabled = enabled
                },
                onNavigateBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.Editor -> {
            EditorScreen(
                editingNote = screen.note,
                onSave = { id, title, body ->
                    val idx = notes.indexOfFirst { it.id == id }
                    if (idx >= 0) {
                        if (title.isNotBlank() || body.isNotBlank()) {
                            notes[idx] = notes[idx].copy(title = title, body = body)
                        } else {
                            notes.removeAt(idx)
                        }
                    } else {
                        if (title.isNotBlank() || body.isNotBlank()) {
                            notes.add(Note(id = id, title = title, body = body, isSecret = screen.isSecret))
                        }
                    }
                },
                onDelete = { idToDelete ->
                    notes.removeIf { it.id == idToDelete }
                },
                onNavigateBack = { currentScreen = Screen.Home }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notes: List<Note>,
    safeZonePassword: String,
    isSafeZoneActive: Boolean,
    onActivateSafeZone: () -> Unit,
    onDeactivateSafeZone: () -> Unit,
    onAddNote: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onSettingsClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        if (safeZonePassword.isNotEmpty() && searchQuery == safeZonePassword && !isSafeZoneActive) {
            searchQuery = ""
            onActivateSafeZone()
        }
    }

    val visibleNotes = remember(notes, isSafeZoneActive) {
        notes.filter { it.isSecret == isSafeZoneActive }
    }

    val filteredNotes = remember(visibleNotes, searchQuery) {
        if (searchQuery.isBlank()) visibleNotes
        else visibleNotes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.body.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNote,
                shape = CircleShape,
                containerColor = if (isSafeZoneActive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Añadir Nota",
                    tint = if (isSafeZoneActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar notas",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSafeZoneActive) "yNotes - Zona Segura" else "yNotes",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSafeZoneActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                if (isSafeZoneActive) {
                    TextButton(onClick = onDeactivateSafeZone) {
                        Icon(Icons.Default.Close, contentDescription = "Salir", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salir")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            if (isSafeZoneActive) "Bóveda vacía. ¡Añade tu primer secreto!" else "Todavía no hay notas. ¡Añade una!"
                        } else "Sin resultados para \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCard(note = note, onClick = { onNoteClick(note) })
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (note.title.isNotEmpty()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (note.body.isNotEmpty()) {
                if (note.title.isNotEmpty()) Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    editingNote: Note?,
    onSave: (id: String, title: String, body: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val currentNoteId = remember { editingNote?.id ?: UUID.randomUUID().toString() }
    var titleText by remember { mutableStateOf(editingNote?.title ?: "") }
    var bodyText by remember { mutableStateOf(editingNote?.body ?: "") }
    var isDeleted by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (!isDeleted) {
                onSave(currentNoteId, titleText.trim(), bodyText.trim())
            }
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (editingNote != null) {
                        IconButton(onClick = { 
                            isDeleted = true
                            onDelete(currentNoteId)
                            onNavigateBack()
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar Nota",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar Nota")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                if (titleText.isEmpty()) {
                    Text(
                        text = "Título",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                }
                BasicTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                if (bodyText.isEmpty()) {
                    Text(
                        text = "Escribe tu nota...",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                }
                BasicTextField(
                    value = bodyText,
                    onValueChange = { bodyText = it },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxSize(),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentPassword: String,
    onSavePassword: (String) -> Unit,
    isBiometricEnabled: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var inputPassword by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticate = remember {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    BackHandler {
        onNavigateBack()
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (currentPassword.isEmpty()) "Crear Contraseña" else "Cambiar Contraseña") },
            text = {
                Column {
                    Text("Para acceder a la Zona Segura, deberás buscar esta palabra exacta en la barra de búsqueda principal.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputPassword,
                        onValueChange = { inputPassword = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (currentPassword.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nota: Guarda con el campo vacío para desactivar la Zona Segura.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSavePassword(inputPassword.trim())
                    showDialog = false
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            
            Text(
                text = "Ajustes",
                style = TextStyle(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // Biometric Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Bloqueo por huella", 
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (canAuthenticate) "Usa tu huella o PIN para abrir la app" else "No disponible en este dispositivo", 
                                style = MaterialTheme.typography.bodyMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = onBiometricToggle,
                            enabled = canAuthenticate
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                    
                    SettingItem(
                        title = "Configurar Zona Segura", 
                        subtitle = if (currentPassword.isEmpty()) "Inactiva (Toca para crear contraseña)" else "Activa (Búscala para acceder)",
                        onClick = { 
                            inputPassword = currentPassword
                            showDialog = true 
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                    SettingItem(title = "Acerca de la app", subtitle = "yNotes desarrollada para ti", onClick = {})
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                    SettingItem(title = "Versión", subtitle = "1.0.0 (AMOLED Edition)", onClick = {})
                }
            }
        }
    }
}

@Composable
fun SettingItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
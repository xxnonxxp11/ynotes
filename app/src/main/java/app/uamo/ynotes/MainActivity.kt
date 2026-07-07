package app.uamo.ynotes

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.uamo.ynotes.ui.theme.YNotesTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class Note(
    val id: String,
    val title: String,
    val body: String,
    val isSecret: Boolean = false
)

sealed class Screen {
    object Welcome : Screen()
    object Home : Screen()
    object SafeZoneHome : Screen()
    data class Editor(val note: Note? = null, val isSecret: Boolean = false) : Screen()
    data class Settings(val isFromSafeZone: Boolean) : Screen()
    object Books : Screen()
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
    val gson = remember { Gson() }
    
    var safeZonePassword by remember { 
        mutableStateOf(sharedPref.getString("SAFE_ZONE_PWD", "") ?: "") 
    }
    
    var isBiometricEnabled by remember {
        mutableStateOf(sharedPref.getBoolean("BIOMETRIC_ENABLED", false))
    }
    
    val notes = remember { 
        val json = sharedPref.getString("NOTES_LIST", "[]")
        val type = object : TypeToken<List<Note>>() {}.type
        val loadedNotes: List<Note> = gson.fromJson(json, type)
        mutableStateListOf(*loadedNotes.toTypedArray())
    }

    val persistNotes = {
        sharedPref.edit().putString("NOTES_LIST", gson.toJson(notes)).apply()
    }

    val hasSeenWelcome = sharedPref.getBoolean("HAS_SEEN_WELCOME", false)
    var currentScreen by remember { mutableStateOf<Screen>(if (hasSeenWelcome) Screen.Home else Screen.Welcome) }
    
    val fragmentActivity = context as? FragmentActivity
    val executor = ContextCompat.getMainExecutor(context)

    val onRequestSafeZone: () -> Unit = {
        if (isBiometricEnabled && fragmentActivity != null) {
            val biometricPrompt = BiometricPrompt(fragmentActivity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        currentScreen = Screen.SafeZoneHome
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Zona Segura")
                .setSubtitle("Confirma tu identidad para acceder")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            currentScreen = Screen.SafeZoneHome
        }
    }

    when (val screen = currentScreen) {
        is Screen.Welcome -> {
            WelcomeScreen(
                onStart = {
                    sharedPref.edit().putBoolean("HAS_SEEN_WELCOME", true).apply()
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.Home -> {
            HomeScreen(
                notes = notes,
                safeZonePassword = safeZonePassword,
                onRequestSafeZone = onRequestSafeZone,
                onAddNote = { currentScreen = Screen.Editor(note = null, isSecret = false) },
                onNoteClick = { note -> currentScreen = Screen.Editor(note = note, isSecret = false) },
                onSettingsClick = { currentScreen = Screen.Settings(isFromSafeZone = false) }
            )
        }
        is Screen.SafeZoneHome -> {
            SafeZoneScreen(
                notes = notes,
                onDeactivateSafeZone = { currentScreen = Screen.Home },
                onAddNote = { currentScreen = Screen.Editor(note = null, isSecret = true) },
                onNoteClick = { note -> currentScreen = Screen.Editor(note = note, isSecret = true) },
                onSettingsClick = { currentScreen = Screen.Settings(isFromSafeZone = true) },
                onBooksClick = { currentScreen = Screen.Books }
            )
        }
        is Screen.Books -> {
            BooksScreen(
                onNavigateBack = { currentScreen = Screen.SafeZoneHome }
            )
        }
        is Screen.Settings -> {
            SettingsScreen(
                currentPassword = safeZonePassword,
                isSafeZoneActive = screen.isFromSafeZone,
                onSavePassword = { newPwd ->
                    val editor = sharedPref.edit()
                    if (safeZonePassword.isEmpty() && newPwd.isNotEmpty()) {
                        editor.putBoolean("BIOMETRIC_ENABLED", true)
                        isBiometricEnabled = true
                    }
                    editor.putString("SAFE_ZONE_PWD", newPwd)
                    editor.apply()
                    safeZonePassword = newPwd

                    if (newPwd.isEmpty() && screen.isFromSafeZone) {
                        currentScreen = Screen.Home 
                    }
                },
                isBiometricEnabled = isBiometricEnabled,
                onBiometricToggle = { enabled ->
                    sharedPref.edit().putBoolean("BIOMETRIC_ENABLED", enabled).apply()
                    isBiometricEnabled = enabled
                },
                onNavigateBack = { 
                    currentScreen = if (screen.isFromSafeZone) Screen.SafeZoneHome else Screen.Home
                }
            )
        }
        is Screen.Editor -> {
            EditorScreen(
                editingNote = screen.note,
                isSecret = screen.isSecret,
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
                    persistNotes()
                },
                onDelete = { idToDelete ->
                    notes.removeIf { it.id == idToDelete }
                    persistNotes() 
                },
                onNavigateBack = { 
                    currentScreen = if (screen.isSecret) Screen.SafeZoneHome else Screen.Home
                }
            )
        }
    }
}

// ==========================================
// HOME SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notes: List<Note>,
    safeZonePassword: String,
    onRequestSafeZone: () -> Unit,
    onAddNote: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onSettingsClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        if (safeZonePassword.isNotEmpty() && searchQuery == safeZonePassword) {
            searchQuery = "" 
            onRequestSafeZone()
        }
    }

    val visibleNotes = remember(notes) {
        notes.filter { !it.isSecret }
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
            ExtendedFloatingActionButton(
                onClick = onAddNote,
                icon = { Icon(Icons.Default.Edit, "Añadir Nota") },
                text = { Text("Nueva nota") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
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
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "yNotes",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredNotes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = "Sin notas",
                        modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                    Text(
                        text = if (searchQuery.isBlank()) "Todavía no hay notas. ¡Añade una!" else "Sin resultados para \"$searchQuery\"",
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

// ==========================================
// SAFE ZONE SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeZoneScreen(
    notes: List<Note>,
    onDeactivateSafeZone: () -> Unit,
    onAddNote: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onSettingsClick: () -> Unit,
    onBooksClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val visibleNotes = remember(notes) {
        notes.filter { it.isSecret }
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
            ExtendedFloatingActionButton(
                onClick = onAddNote,
                icon = { Icon(Icons.Default.Lock, "Añadir Secreto") },
                text = { Text("Nuevo secreto") },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(16.dp)
            )
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
                            contentDescription = "Buscar secreto",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar en bóveda",
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
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.error),
                                singleLine = true
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
                    IconButton(onClick = onBooksClick) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Sistema de Libros",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Bóveda",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Zona Segura",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = onDeactivateSafeZone,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Salir", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cerrar")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredNotes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = "Bóveda vacía",
                        modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    )
                    Text(
                        text = if (searchQuery.isBlank()) "Bóveda vacía. ¡Añade tu primer secreto!" else "Sin resultados para \"$searchQuery\"",
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
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (note.isSecret) Icons.Default.VpnKey else Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = (if (note.isSecret) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ==========================================
// EDITOR SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    editingNote: Note?,
    isSecret: Boolean, 
    onSave: (id: String, title: String, body: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val currentNoteId = remember { editingNote?.id ?: UUID.randomUUID().toString() }
    var titleText by remember { mutableStateOf(editingNote?.title ?: "") }
    var bodyText by remember { mutableStateOf(editingNote?.body ?: "") }
    var isDeleted by remember { mutableStateOf(false) }

    val cursorColor = if (isSecret) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

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
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.PushPin, contentDescription = "Fijar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Palette, contentDescription = "Color", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                    onValueChange = { 
                        titleText = it 
                        if (!isDeleted) onSave(currentNoteId, titleText.trim(), bodyText.trim())
                    },
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    cursorBrush = SolidColor(cursorColor)
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
                    onValueChange = { 
                        bodyText = it 
                        if (!isDeleted) onSave(currentNoteId, titleText.trim(), bodyText.trim())
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxSize(),
                    cursorBrush = SolidColor(cursorColor)
                )
            }
        }
    }
}

// ==========================================
// SETTINGS SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentPassword: String,
    isSafeZoneActive: Boolean,
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

    val showSecurityOptions = true

    BackHandler {
        onNavigateBack()
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(if (currentPassword.isEmpty()) "Crear Zona Segura" else "Cambiar Contraseña") },
            text = {
                Column {
                    Text("Para acceder a la Zona Segura, deberás buscar esta palabra exacta en la barra de búsqueda principal.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputPassword,
                        onValueChange = { inputPassword = it },
                        label = { Text("Contraseña secreta") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (currentPassword.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nota: Guarda con el campo vacío para eliminar la Zona Segura.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
            Spacer(modifier = Modifier.height(60.dp))
            
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
                    
                    if (showSecurityOptions) {
                        if (currentPassword.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Bloqueo por huella", 
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (canAuthenticate) "Usa huella para entrar a la zona segura" else "No disponible en este dispositivo", 
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
                        }
                        
                        SettingItem(
                            title = if (currentPassword.isEmpty()) "Crear Zona Segura" else "Cambiar Contraseña", 
                            subtitle = if (currentPassword.isEmpty()) "Inactiva (Toca para crear)" else "Activa (La Zona Segura está protegida)",
                            icon = Icons.Default.EnhancedEncryption,
                            iconTint = if (currentPassword.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            onClick = { 
                                inputPassword = currentPassword
                                showDialog = true 
                            }
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                    }

                    SettingItem(
                        title = "Acerca de la app", 
                        subtitle = "yNotes desarrollada para ti", 
                        icon = Icons.Default.Info,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = {}
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                    SettingItem(
                        title = "Versión", 
                        subtitle = "1.0.0 (AMOLED Edition)", 
                        icon = Icons.Default.Build,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Composable
fun SettingItem(title: String, subtitle: String, icon: ImageVector, iconTint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.padding(8.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
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
}

// ==========================================
// WELCOME SCREEN
// ==========================================
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(100.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Bienvenido a yNotes",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Captura tus ideas rápidamente y protege tus secretos en la Zona Segura.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Security, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Zona Segura",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Busca tu contraseña secreta para acceder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Empezar",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==========================================
// BOOKS SYSTEM SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = { Text("Sistema de Libros", style = MaterialTheme.typography.titleLarge) },
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Book,
                contentDescription = "Sin libros",
                modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            )
            Text(
                text = "Tu sistema de libros está vacío.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

package app.uamo.ynotes.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.ui.components.NoteCard
import app.uamo.ynotes.utils.AppInfo
import app.uamo.ynotes.utils.getInstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeZoneScreen(
    notes: List<NoteEntity>,
    onDeactivateSafeZone: () -> Unit,
    onAddNote: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onSettingsClick: () -> Unit,
    onBooksClick: () -> Unit,
    onTrashClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = remember { context.getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE) }
    
    var searchQuery by remember { mutableStateOf("") }
    
    // Hidden Apps State
    var hiddenAppPackages by remember { 
        mutableStateOf(sharedPrefs.getStringSet("HIDDEN_APPS", emptySet())?.toSet() ?: emptySet()) 
    }
    var allInstalledApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

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

    val pinnedNotes = remember(filteredNotes) { filteredNotes.filter { it.isPinned } }
    val unpinnedNotes = remember(filteredNotes) { filteredNotes.filter { !it.isPinned } }

    LaunchedEffect(showAppPicker) {
        if (showAppPicker && allInstalledApps.isEmpty()) {
            isLoadingApps = true
            allInstalledApps = withContext(Dispatchers.IO) {
                getInstalledApps(context)
            }
            isLoadingApps = false
        }
    }

    if (showAppPicker) {
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text("Ocultar Aplicaciones") },
            text = {
                if (isLoadingApps) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(allInstalledApps, key = { it.packageName }) { app ->
                            val isSelected = hiddenAppPackages.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSet = if (isSelected) hiddenAppPackages - app.packageName else hiddenAppPackages + app.packageName
                                        hiddenAppPackages = newSet
                                        sharedPrefs.edit().putStringSet("HIDDEN_APPS", newSet).apply()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = app.name,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(app.name, modifier = Modifier.weight(1f))
                                Checkbox(checked = isSelected, onCheckedChange = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppPicker = false }) {
                    Text("Cerrar")
                }
            }
        )
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
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onTrashClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Papelera",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onBooksClick) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Libros",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuración",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

            val isAppHiderEnabled = sharedPrefs.getBoolean("ENABLE_APP_HIDER", true)
            
            if (isAppHiderEnabled) {
                if (hiddenAppPackages.isNotEmpty() || showAppPicker) {
                    // If we don't have the apps loaded but we have packages, we should load them silently to show the icons
                    LaunchedEffect(hiddenAppPackages) {
                        if (allInstalledApps.isEmpty() && hiddenAppPackages.isNotEmpty()) {
                            allInstalledApps = withContext(Dispatchers.IO) {
                                getInstalledApps(context)
                            }
                        }
                    }

                    val appsToShow = allInstalledApps.filter { hiddenAppPackages.contains(it.packageName) }
                    
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(appsToShow, key = { it.packageName }) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                        }
                                    }
                                    .padding(4.dp)
                            ) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = app.name,
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.name, 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { showAppPicker = true }
                                    .padding(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add, 
                                        contentDescription = "Añadir app", 
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Añadir", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                } else {
                    TextButton(
                        onClick = { showAppPicker = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir Apps Ocultas")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

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
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                "FIJADAS", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 8.dp)
                            )
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(note = note, onClick = { onNoteClick(note) })
                        }
                    }

                    if (unpinnedNotes.isNotEmpty()) {
                        if (pinnedNotes.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    "OTRAS", 
                                    style = MaterialTheme.typography.labelMedium, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 16.dp)
                                )
                            }
                        }
                        items(unpinnedNotes, key = { it.id }) { note ->
                            NoteCard(note = note, onClick = { onNoteClick(note) })
                        }
                    }
                }
        }
    }
}

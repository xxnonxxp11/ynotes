package app.uamo.ynotes.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.uamo.ynotes.data.BookEntity
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.utils.MediaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

val NoteColors = listOf(
    0L, // Default (uses surfaceVariant)
    0xFF4A1C1C, // Dark Red
    0xFF4A1C3B, // Dark Pink
    0xFF3B1C4A, // Dark Purple
    0xFF2C1C4A, // Dark Deep Purple
    0xFF1C2A4A, // Dark Indigo
    0xFF1C3A4A, // Dark Blue
    0xFF1C4A47, // Dark Cyan
    0xFF1C4A3B, // Dark Teal
    0xFF1C4A22, // Dark Green
    0xFF2F4A1C, // Dark Light Green
    0xFF4A471C, // Dark Lime
    0xFF4A421C, // Dark Yellow
    0xFF4A351C, // Dark Amber
    0xFF4A2B1C, // Dark Orange
    0xFF4A221C  // Dark Deep Orange
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    editingNote: NoteEntity?,
    isSecret: Boolean,
    books: List<BookEntity>,
    onSave: (id: String?, title: String, body: String, color: Long, isPinned: Boolean, bookId: String?, isBodyHidden: Boolean, mediaFiles: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    // Stable ID: reuse the existing note's ID, or generate ONE new UUID for this session
    val stableNoteId = remember { editingNote?.id ?: UUID.randomUUID().toString() }

    var titleText by remember { mutableStateOf(editingNote?.title ?: "") }
    var bodyText by remember { mutableStateOf(editingNote?.body ?: "") }
    var noteColor by remember { mutableStateOf(editingNote?.color ?: 0L) }
    var isPinned by remember { mutableStateOf(editingNote?.isPinned ?: false) }
    var bookId by remember { mutableStateOf(editingNote?.bookId) }
    var isBodyHidden by remember { mutableStateOf(editingNote?.isBodyHidden ?: false) }
    var isDeleted by remember { mutableStateOf(false) }

    // Media state
    var mediaFileNames by remember {
        mutableStateOf(
            editingNote?.mediaFiles?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }
    val mediaBitmaps = remember { mutableStateMapOf<String, Bitmap>() }

    var showColorPicker by remember { mutableStateOf(false) }
    var showBookMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    val cursorColor = if (isSecret) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val backgroundColor = if (noteColor == 0L) MaterialTheme.colorScheme.background else Color(noteColor)

    fun mediaFilesString(): String = mediaFileNames.joinToString("|")

    // Load existing media thumbnails
    LaunchedEffect(mediaFileNames) {
        mediaFileNames.forEach { fileName ->
            if (!mediaBitmaps.containsKey(fileName)) {
                withContext(Dispatchers.IO) {
                    MediaManager.loadMediaBitmap(context, stableNoteId, fileName, isSecret)
                }?.let { bitmap ->
                    mediaBitmaps[fileName] = bitmap
                }
            }
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val newNames = mutableListOf<String>()
                uris.forEach { uri ->
                    withContext(Dispatchers.IO) {
                        MediaManager.saveMedia(context, stableNoteId, uri, isSecret)
                    }?.let { name ->
                        newNames.add(name)
                        // Load thumbnail immediately
                        withContext(Dispatchers.IO) {
                            MediaManager.loadMediaBitmap(context, stableNoteId, name, isSecret)
                        }?.let { bitmap ->
                            mediaBitmaps[name] = bitmap
                        }
                    }
                }
                if (newNames.isNotEmpty()) {
                    mediaFileNames = mediaFileNames + newNames
                    // Save immediately after adding media
                    if (!isDeleted && (titleText.trim().isNotBlank() || bodyText.trim().isNotBlank() || mediaFileNames.isNotEmpty())) {
                        onSave(stableNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId, isBodyHidden, mediaFilesString())
                    }
                }
            }
        }
    }

    // Debounced save: cancels the previous pending save and waits 500ms of silence
    fun scheduleSave() {
        if (isDeleted) return
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(500)
            if (!isDeleted && (titleText.trim().isNotBlank() || bodyText.trim().isNotBlank() || mediaFileNames.isNotEmpty())) {
                onSave(stableNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId, isBodyHidden, mediaFilesString())
            }
        }
    }

    // Immediate save for buttons (pin, color, book, back, check)
    fun saveNow() {
        if (isDeleted) return
        debounceJob?.cancel()
        if (titleText.trim().isNotBlank() || bodyText.trim().isNotBlank() || mediaFileNames.isNotEmpty()) {
            onSave(stableNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId, isBodyHidden, mediaFilesString())
        }
    }

    BackHandler {
        saveNow()
        onNavigateBack()
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        saveNow()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isPinned = !isPinned
                        saveNow()
                    }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Fijar",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showColorPicker = !showColorPicker }) {
                        Icon(Icons.Default.Palette, contentDescription = "Color", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "Adjuntar imagen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        isBodyHidden = !isBodyHidden
                        saveNow()
                    }) {
                        Icon(
                            if (isBodyHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Ocultar descripción",
                            tint = if (isBodyHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (editingNote != null) {
                        IconButton(onClick = {
                            isDeleted = true
                            debounceJob?.cancel()
                            onDelete(editingNote.id)
                            onNavigateBack()
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar Nota",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = {
                        saveNow()
                        onNavigateBack()
                    }) {
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
        ) {
            if (showColorPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NoteColors.forEach { colorValue ->
                        val isSelected = noteColor == colorValue
                        val displayColor = if (colorValue == 0L)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            Color(colorValue)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(displayColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else app.uamo.ynotes.ui.theme.GlassBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    noteColor = colorValue
                                    showColorPicker = false
                                    saveNow()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Media thumbnails strip
            if (mediaFileNames.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mediaFileNames) { fileName ->
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            mediaBitmaps[fileName]?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Imagen adjunta",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } ?: Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            // Delete button
                            IconButton(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        MediaManager.deleteMediaFile(context, stableNoteId, fileName, isSecret)
                                    }
                                    mediaBitmaps.remove(fileName)
                                    mediaFileNames = mediaFileNames - fileName
                                    saveNow()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Eliminar imagen",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp).weight(1f)) {
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
                            scheduleSave()
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
                            scheduleSave()
                        },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.fillMaxSize(),
                        cursorBrush = SolidColor(cursorColor),
                        visualTransformation = app.uamo.ynotes.utils.MarkdownVisualTransformation(MaterialTheme.colorScheme.onBackground)
                    )
                }
            }
        }
    }
}

package app.uamo.ynotes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.uamo.ynotes.data.BookEntity
import app.uamo.ynotes.data.NoteEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onSave: (id: String?, title: String, body: String, color: Long, isPinned: Boolean, bookId: String?, isBodyHidden: Boolean) -> Unit,
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

    var showColorPicker by remember { mutableStateOf(false) }
    var showBookMenu by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    val cursorColor = if (isSecret) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val backgroundColor = if (noteColor == 0L) MaterialTheme.colorScheme.background else Color(noteColor.toULong())
    val context = androidx.compose.ui.platform.LocalContext.current

    // Debounced save: cancels the previous pending save and waits 500ms of silence
    fun scheduleSave() {
        if (isDeleted) return
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(500)
            if (!isDeleted && (titleText.trim().isNotBlank() || bodyText.trim().isNotBlank())) {
                onSave(stableNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId, isBodyHidden)
            }
        }
    }

    // Immediate save for buttons (pin, color, book, back, check)
    fun saveNow() {
        if (isDeleted) return
        debounceJob?.cancel()
        if (titleText.trim().isNotBlank() || bodyText.trim().isNotBlank()) {
            onSave(stableNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId, isBodyHidden)
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
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NoteColors.forEach { colorValue ->
                        val isSelected = noteColor == colorValue
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (colorValue == 0L) MaterialTheme.colorScheme.surfaceVariant
                                    else Color(colorValue.toULong())
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else app.uamo.ynotes.ui.theme.GlassBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    noteColor = colorValue
                                    saveNow()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
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

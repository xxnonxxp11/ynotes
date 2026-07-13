package app.uamo.ynotes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

val NoteColors = listOf(
    0L, // Default
    0xFFFFCDD2, // Red
    0xFFF8BBD0, // Pink
    0xFFE1BEE7, // Purple
    0xFFD1C4E9, // Deep Purple
    0xFFC5CAE9, // Indigo
    0xFFBBDEFB, // Blue
    0xFFB2EBF2, // Cyan
    0xFFB2DFDB, // Teal
    0xFFC8E6C9, // Green
    0xFFDCEDC8, // Light Green
    0xFFF0F4C3, // Lime
    0xFFFFF9C4, // Yellow
    0xFFFFECB3, // Amber
    0xFFFFE0B2, // Orange
    0xFFFFCCBC  // Deep Orange
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    editingNote: NoteEntity?,
    isSecret: Boolean, 
    books: List<BookEntity>,
    onSave: (id: String?, title: String, body: String, color: Long, isPinned: Boolean, bookId: String?) -> Unit,
    onDelete: (id: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val currentNoteId = remember { editingNote?.id }
    var titleText by remember { mutableStateOf(editingNote?.title ?: "") }
    var bodyText by remember { mutableStateOf(editingNote?.body ?: "") }
    var noteColor by remember { mutableStateOf(editingNote?.color ?: 0L) }
    var isPinned by remember { mutableStateOf(editingNote?.isPinned ?: false) }
    var bookId by remember { mutableStateOf(editingNote?.bookId) }
    var isDeleted by remember { mutableStateOf(false) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showBookMenu by remember { mutableStateOf(false) }

    val cursorColor = if (isSecret) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val backgroundColor = if (noteColor == 0L) MaterialTheme.colorScheme.background else Color(noteColor.toULong())

    val context = androidx.compose.ui.platform.LocalContext.current

    BackHandler {
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TITLE, titleText)
                            putExtra(android.content.Intent.EXTRA_TEXT, "$titleText\n\n$bodyText")
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        IconButton(onClick = { showBookMenu = true }) {
                            Icon(Icons.Default.MenuBook, contentDescription = "Libro", tint = if (bookId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = showBookMenu,
                            onDismissRequest = { showBookMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sin libro") },
                                onClick = { 
                                    bookId = null
                                    showBookMenu = false
                                    if (!isDeleted) onSave(currentNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId)
                                }
                            )
                            books.forEach { book ->
                                DropdownMenuItem(
                                    text = { Text(book.name) },
                                    onClick = {
                                        bookId = book.id
                                        showBookMenu = false
                                        if (!isDeleted) onSave(currentNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId)
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { 
                        isPinned = !isPinned
                        if (!isDeleted) onSave(currentNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId)
                    }) {
                        Icon(
                            if (isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = "Fijar", 
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showColorPicker = !showColorPicker }) {
                        Icon(Icons.Default.Palette, contentDescription = "Color", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (editingNote != null) {
                        IconButton(onClick = { 
                            isDeleted = true
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
        ) {
            if (showColorPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NoteColors.forEach { colorValue ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (colorValue == 0L) MaterialTheme.colorScheme.surfaceVariant else Color(colorValue.toULong()))
                                        .border(
                                    width = if (noteColor == colorValue) 3.dp else 1.dp,
                                    color = if (noteColor == colorValue) MaterialTheme.colorScheme.primary else app.uamo.ynotes.ui.theme.GlassBorder,
                                    shape = CircleShape
                                )
                                .clickable { 
                                    noteColor = colorValue
                                    if (!isDeleted) onSave(currentNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId)
                                }
                        ) {
                            if (noteColor == colorValue) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f))
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
                            if (!isDeleted) onSave(currentNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId)
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
                            if (!isDeleted) onSave(currentNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId)
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

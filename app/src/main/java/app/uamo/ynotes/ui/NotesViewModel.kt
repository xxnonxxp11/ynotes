package app.uamo.ynotes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.uamo.ynotes.data.BookEntity
import app.uamo.ynotes.data.NoteDatabase
import app.uamo.ynotes.data.NoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val noteDao = NoteDatabase.getDatabase(application).noteDao()

    private val _publicNotes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val publicNotes: StateFlow<List<NoteEntity>> = _publicNotes.asStateFlow()

    private val _secretNotes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val secretNotes: StateFlow<List<NoteEntity>> = _secretNotes.asStateFlow()

    private val _deletedNotes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val deletedNotes: StateFlow<List<NoteEntity>> = _deletedNotes.asStateFlow()

    private val _books = MutableStateFlow<List<BookEntity>>(emptyList())
    val books: StateFlow<List<BookEntity>> = _books.asStateFlow()

    init {
        viewModelScope.launch {
            noteDao.getPublicNotes().collectLatest { notes ->
                _publicNotes.value = notes
            }
        }
        viewModelScope.launch {
            noteDao.getSecretNotes().collectLatest { notes ->
                _secretNotes.value = notes
            }
        }
        viewModelScope.launch {
            noteDao.getDeletedNotes().collectLatest { notes ->
                _deletedNotes.value = notes
            }
        }
        viewModelScope.launch {
            noteDao.getAllBooks().collectLatest { booksList ->
                _books.value = booksList
            }
        }
    }

    fun saveNote(
        id: String?, 
        title: String, 
        body: String, 
        isSecret: Boolean, 
        color: Long = 0L, 
        isPinned: Boolean = false, 
        bookId: String? = null,
        existingCreatedAt: Long? = null
    ) {
        if (title.isBlank() && body.isBlank()) return
        
        val noteId = id ?: UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            noteDao.insertNote(
                NoteEntity(
                    id = noteId,
                    title = title,
                    body = body,
                    isSecret = isSecret,
                    color = color,
                    createdAt = existingCreatedAt ?: currentTime,
                    updatedAt = currentTime,
                    isPinned = isPinned,
                    bookId = bookId,
                    isDeleted = false
                )
            )
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            noteDao.moveToTrash(id)
        }
    }

    fun restoreFromTrash(id: String) {
        viewModelScope.launch {
            noteDao.restoreFromTrash(id)
        }
    }

    fun deleteNotePermanently(id: String) {
        viewModelScope.launch {
            noteDao.deleteNote(id)
        }
    }
    
    fun emptyTrash() {
        viewModelScope.launch {
            noteDao.emptyTrash()
        }
    }

    fun saveBook(id: String?, name: String, color: Long, iconName: String, isSecret: Boolean = false) {
        if (name.isBlank()) return
        val bookId = id ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            noteDao.insertBook(
                BookEntity(
                    id = bookId,
                    name = name,
                    color = color,
                    iconName = iconName,
                    isSecret = isSecret
                )
            )
        }
    }

    fun deleteBook(id: String) {
        viewModelScope.launch {
            noteDao.deleteBook(id)
        }
    }
}

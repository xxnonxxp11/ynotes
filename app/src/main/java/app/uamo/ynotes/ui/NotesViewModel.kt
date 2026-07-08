package app.uamo.ynotes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    }

    fun saveNote(id: String?, title: String, body: String, isSecret: Boolean) {
        if (title.isBlank() && body.isBlank()) return
        
        val noteId = id ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            noteDao.insertNote(
                NoteEntity(
                    id = noteId,
                    title = title,
                    body = body,
                    isSecret = isSecret
                )
            )
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            noteDao.deleteNote(id)
        }
    }
}

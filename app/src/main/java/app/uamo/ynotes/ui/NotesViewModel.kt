package app.uamo.ynotes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.uamo.ynotes.data.BookEntity
import app.uamo.ynotes.data.NoteDatabase
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.data.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import app.uamo.ynotes.utils.CryptoManager
import java.util.UUID

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val noteDao = NoteDatabase.getDatabase(application).noteDao()

    // ──────────────────────────────────────────────
    // SORT ORDER — Persisted in SharedPreferences
    // ──────────────────────────────────────────────
    private val prefs = application.getSharedPreferences("yNotesPrefs", android.content.Context.MODE_PRIVATE)
    
    private val _sortOrder = MutableStateFlow(
        try { SortOrder.valueOf(prefs.getString("SORT_ORDER", null) ?: SortOrder.UPDATED_DESC.name) }
        catch (_: Exception) { SortOrder.UPDATED_DESC }
    )
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        prefs.edit().putString("SORT_ORDER", order.name).apply()
    }

    private fun List<NoteEntity>.applySortOrder(order: SortOrder): List<NoteEntity> {
        val pinned = this.filter { it.isPinned }.sortedWith(order.comparator())
        val unpinned = this.filter { !it.isPinned }.sortedWith(order.comparator())
        return pinned + unpinned
    }

    // ──────────────────────────────────────────────
    // PUBLIC NOTES — Eagerly cached for instant load
    // ──────────────────────────────────────────────
    private val _rawPublicNotes: StateFlow<List<NoteEntity>> = noteDao.getPublicNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,  // Always in memory — instant UI
            initialValue = emptyList()
        )

    val publicNotes: StateFlow<List<NoteEntity>> = combine(
        _rawPublicNotes, _sortOrder
    ) { notes, order ->
        notes.applySortOrder(order)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // ──────────────────────────────────────────────
    // SAFE ZONE — Locked by default, decrypt on unlock
    // ──────────────────────────────────────────────
    private val _isSafeZoneUnlocked = MutableStateFlow(false)
    val isSafeZoneUnlocked: StateFlow<Boolean> = _isSafeZoneUnlocked.asStateFlow()

    // Raw encrypted notes from DB (always flowing)
    private val _rawSecretNotes: StateFlow<List<NoteEntity>> = noteDao.getSecretNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Decrypted cache — only populated when unlocked
    private val _decryptedSecretNotes = MutableStateFlow<List<NoteEntity>>(emptyList())

    // Public-facing: combines lock state with decrypted cache + sort
    val secretNotes: StateFlow<List<NoteEntity>> = combine(
        _isSafeZoneUnlocked,
        _decryptedSecretNotes,
        _sortOrder
    ) { unlocked, notes, order ->
        if (unlocked) notes.applySortOrder(order) else emptyList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ──────────────────────────────────────────────
    // DELETED NOTES
    // ──────────────────────────────────────────────
    val deletedNotes: StateFlow<List<NoteEntity>> = noteDao.getDeletedNotes()
        .combine(_isSafeZoneUnlocked) { notes, unlocked ->
            notes.map { note ->
                if (note.isSecret && unlocked) {
                    note.copy(
                        title = CryptoManager.decrypt(note.title),
                        body = CryptoManager.decrypt(note.body)
                    )
                } else if (note.isSecret) {
                    // Keep secret deleted notes hidden when locked
                    note.copy(title = "🔒", body = "")
                } else {
                    note
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val books: StateFlow<List<BookEntity>> = noteDao.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,  // Also cached eagerly
            initialValue = emptyList()
        )

    // ──────────────────────────────────────────────
    // SAFE ZONE SESSION CONTROL
    // ──────────────────────────────────────────────

    /**
     * Called after successful biometric authentication.
     * Decrypts all secret notes into memory.
     */
    fun unlockSafeZone() {
        _isSafeZoneUnlocked.value = true
        // Decrypt current raw notes in batch (key lookup once)
        viewModelScope.launch(Dispatchers.Default) {
            val raw = _rawSecretNotes.value
            val decrypted = CryptoManager.decryptBatch(raw)
            _decryptedSecretNotes.value = decrypted
        }
        // Keep decrypted cache in sync while unlocked
        viewModelScope.launch {
            _rawSecretNotes.collect { rawNotes ->
                if (_isSafeZoneUnlocked.value) {
                    val decrypted = CryptoManager.decryptBatch(rawNotes)
                    _decryptedSecretNotes.value = decrypted
                }
            }
        }
    }

    /**
     * Called when user exits Safe Zone.
     * Clears decrypted data from memory immediately.
     */
    fun lockSafeZone() {
        _isSafeZoneUnlocked.value = false
        _decryptedSecretNotes.value = emptyList()  // Wipe from memory
    }

    // ──────────────────────────────────────────────
    // MULTI-SELECT
    // ──────────────────────────────────────────────
    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNoteIds: StateFlow<Set<String>> = _selectedNoteIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun toggleNoteSelection(noteId: String) {
        val current = _selectedNoteIds.value
        _selectedNoteIds.value = if (noteId in current) current - noteId else current + noteId
        if (_selectedNoteIds.value.isEmpty()) _isSelectionMode.value = false
    }

    fun startSelection(noteId: String) {
        _isSelectionMode.value = true
        _selectedNoteIds.value = setOf(noteId)
    }

    fun selectAll(noteIds: List<String>) {
        _selectedNoteIds.value = noteIds.toSet()
    }

    fun clearSelection() {
        _isSelectionMode.value = false
        _selectedNoteIds.value = emptySet()
    }

    fun deleteSelectedNotes() {
        viewModelScope.launch {
            _selectedNoteIds.value.forEach { id ->
                noteDao.moveToTrash(id)
            }
            clearSelection()
        }
    }

    // ──────────────────────────────────────────────
    // NOTE OPERATIONS
    // ──────────────────────────────────────────────

    fun saveNote(
        id: String?, 
        title: String, 
        body: String, 
        isSecret: Boolean, 
        color: Long = 0L, 
        isPinned: Boolean = false, 
        bookId: String? = null,
        isBodyHidden: Boolean = false,
        existingCreatedAt: Long? = null
    ) {
        if (title.isBlank() && body.isBlank()) return
        
        val noteId = id ?: UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        
        // Use batch encrypt for secret notes (single key lookup)
        val (finalTitle, finalBody) = if (isSecret) {
            CryptoManager.encryptFields(title, body)
        } else {
            Pair(title, body)
        }
        
        viewModelScope.launch {
            noteDao.insertNote(
                NoteEntity(
                    id = noteId,
                    title = finalTitle,
                    body = finalBody,
                    isSecret = isSecret,
                    color = color,
                    createdAt = existingCreatedAt ?: currentTime,
                    updatedAt = currentTime,
                    isPinned = isPinned,
                    bookId = bookId,
                    isDeleted = false,
                    isBodyHidden = isBodyHidden
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

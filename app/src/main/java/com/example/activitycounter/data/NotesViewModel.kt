package com.example.activitycounter.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.activitycounter.model.Note
import com.example.activitycounter.model.NoteDraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ActivityDatabase.getDatabase(application).noteDao()

    // null until the database has loaded, so the UI doesn't flash the empty state on startup
    val notes: StateFlow<List<Note>?> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // The note open in the editor, or null when the editor is closed. Compose state rather
    // than a flow so text fields update synchronously while typing.
    var draft by mutableStateOf<NoteDraft?>(null)
        private set

    // noteId is 0 until a new note's first save; lastSaved avoids rewriting unchanged notes
    private class EditSession(var noteId: Int, var lastSaved: NoteDraft)

    private var session: EditSession? = null

    // Saves run one at a time so a new note is inserted only once
    private val saveMutex = Mutex()

    fun newNote() {
        val empty = NoteDraft()
        session = EditSession(noteId = 0, lastSaved = empty)
        draft = empty
    }

    fun openNote(note: Note) {
        val opened = NoteDraft.from(note)
        session = EditSession(noteId = note.id, lastSaved = opened)
        draft = opened
    }

    fun updateDraft(transform: (NoteDraft) -> NoteDraft) {
        draft = draft?.let(transform)
    }

    // Called when the app goes to the background, so edits survive the process being killed
    fun saveDraft() {
        val d = draft ?: return
        val s = session ?: return
        persist(d, s)
    }

    fun closeEditor() {
        val d = draft ?: return
        val s = session ?: return
        draft = null
        session = null
        persist(d, s)
    }

    fun deleteDraft() {
        val s = session ?: return
        draft = null
        session = null
        viewModelScope.launch {
            saveMutex.withLock {
                if (s.noteId != 0) dao.deleteById(s.noteId)
            }
        }
    }

    private fun persist(d: NoteDraft, s: EditSession) {
        if (d == s.lastSaved) return
        s.lastSaved = d
        viewModelScope.launch {
            saveMutex.withLock {
                when {
                    // Blank notes aren't kept
                    d.isBlank -> {
                        if (s.noteId != 0) dao.deleteById(s.noteId)
                        s.noteId = 0
                    }
                    s.noteId == 0 -> s.noteId = dao.insert(d.toNote(0, System.currentTimeMillis())).toInt()
                    else -> dao.update(d.toNote(s.noteId, System.currentTimeMillis()))
                }
            }
        }
    }
}

package com.example.activitycounter.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.atomic.AtomicLong

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    // Plain text, or one "[ ] item" / "[x] item" line per checklist item
    val content: String,
    val isChecklist: Boolean,
    // 0 = default, 1..7 = palette colors
    val colorIndex: Int,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class ChecklistItem(
    val text: String,
    val checked: Boolean = false,
    // Only used as a stable UI key while editing; not stored
    val id: Long = nextItemId.incrementAndGet()
)

private val nextItemId = AtomicLong()

private const val CHECKED_PREFIX = "[x] "
private const val UNCHECKED_PREFIX = "[ ] "

fun parseChecklist(content: String): List<ChecklistItem> =
    if (content.isEmpty()) emptyList()
    else content.lines().map { line ->
        when {
            line.startsWith(CHECKED_PREFIX) -> ChecklistItem(line.removePrefix(CHECKED_PREFIX), checked = true)
            line.startsWith(UNCHECKED_PREFIX) -> ChecklistItem(line.removePrefix(UNCHECKED_PREFIX))
            else -> ChecklistItem(line)
        }
    }

fun serializeChecklist(items: List<ChecklistItem>): String =
    items.joinToString("\n") { (if (it.checked) CHECKED_PREFIX else UNCHECKED_PREFIX) + it.text }

// The note being edited. Text and checklist content are kept separately so switching type is lossless
// until the note is saved.
data class NoteDraft(
    val title: String = "",
    val text: String = "",
    val isChecklist: Boolean = false,
    val items: List<ChecklistItem> = emptyList(),
    val colorIndex: Int = 0,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isBlank: Boolean
        get() = title.isBlank() && if (isChecklist) items.all { it.text.isBlank() } else text.isBlank()

    // Text becomes one item per non-blank line; a checklist becomes one line per item.
    fun withChecklist(checklist: Boolean): NoteDraft = when {
        checklist == isChecklist -> this
        checklist -> copy(
            isChecklist = true,
            items = text.lines().filter { it.isNotBlank() }.map { ChecklistItem(it.trim()) }
                .ifEmpty { listOf(ChecklistItem("")) }
        )
        else -> copy(
            isChecklist = false,
            text = items.filter { it.text.isNotBlank() }.joinToString("\n") { it.text }
        )
    }

    fun toNote(id: Int, updatedAt: Long) = Note(
        id = id,
        title = title.trim(),
        content = if (isChecklist) serializeChecklist(items.filter { it.text.isNotBlank() }) else text.trimEnd(),
        isChecklist = isChecklist,
        colorIndex = colorIndex,
        isPinned = isPinned,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun from(note: Note) = NoteDraft(
            title = note.title,
            text = if (note.isChecklist) "" else note.content,
            isChecklist = note.isChecklist,
            items = if (note.isChecklist) parseChecklist(note.content) else emptyList(),
            colorIndex = note.colorIndex,
            isPinned = note.isPinned,
            createdAt = note.createdAt
        )
    }
}

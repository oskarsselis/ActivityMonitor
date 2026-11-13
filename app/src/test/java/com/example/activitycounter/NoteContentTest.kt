package com.example.activitycounter

import com.example.activitycounter.model.ChecklistItem
import com.example.activitycounter.model.Note
import com.example.activitycounter.model.NoteDraft
import com.example.activitycounter.model.parseChecklist
import com.example.activitycounter.model.serializeChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteContentTest {
    private fun List<ChecklistItem>.view() = map { it.text to it.checked }

    @Test
    fun checklist_roundTripsThroughContent() {
        val items = listOf(ChecklistItem("Milk"), ChecklistItem("Eggs", checked = true), ChecklistItem("[x] literal"))
        val content = serializeChecklist(items)
        assertEquals("[ ] Milk\n[x] Eggs\n[ ] [x] literal", content)
        assertEquals(items.view(), parseChecklist(content).view())
    }

    @Test
    fun parseChecklist_handlesEmptyAndUnprefixedLines() {
        assertEquals(emptyList<Pair<String, Boolean>>(), parseChecklist("").view())
        assertEquals(listOf("plain line" to false), parseChecklist("plain line").view())
    }

    @Test
    fun textToChecklist_makesOneItemPerNonBlankLine() {
        val draft = NoteDraft(text = "Milk\n\n  Eggs  \n").withChecklist(true)
        assertTrue(draft.isChecklist)
        assertEquals(listOf("Milk" to false, "Eggs" to false), draft.items.view())
    }

    @Test
    fun emptyTextToChecklist_startsWithOneEmptyItem() {
        assertEquals(listOf("" to false), NoteDraft().withChecklist(true).items.view())
    }

    @Test
    fun checklistToText_joinsItemsAndDropsBlanks() {
        val draft = NoteDraft(
            isChecklist = true,
            items = listOf(ChecklistItem("Milk", checked = true), ChecklistItem(" "), ChecklistItem("Eggs"))
        ).withChecklist(false)
        assertFalse(draft.isChecklist)
        assertEquals("Milk\nEggs", draft.text)
    }

    @Test
    fun isBlank_looksAtTitleAndActiveContentOnly() {
        assertTrue(NoteDraft().isBlank)
        assertTrue(NoteDraft(isChecklist = true, items = listOf(ChecklistItem("  "))).isBlank)
        // Leftover text from before switching to checklist doesn't count
        assertTrue(NoteDraft(text = "old", isChecklist = true).isBlank)
        assertFalse(NoteDraft(title = "Shopping").isBlank)
        assertFalse(NoteDraft(text = "x").isBlank)
    }

    @Test
    fun noteRoundTrip_keepsEverything() {
        val draft = NoteDraft(
            title = "Groceries",
            isChecklist = true,
            items = listOf(ChecklistItem("Milk"), ChecklistItem("Eggs", checked = true)),
            colorIndex = 3,
            isPinned = true,
            createdAt = 1000L
        )
        val note = draft.toNote(id = 7, updatedAt = 2000L)
        assertEquals(
            Note(7, "Groceries", "[ ] Milk\n[x] Eggs", true, 3, true, 1000L, 2000L),
            note
        )

        val reopened = NoteDraft.from(note)
        assertEquals(draft.title, reopened.title)
        assertEquals(draft.items.view(), reopened.items.view())
        assertEquals(draft.colorIndex, reopened.colorIndex)
        assertEquals(draft.isPinned, reopened.isPinned)
        assertEquals(draft.createdAt, reopened.createdAt)
    }

    @Test
    fun toNote_dropsBlankItemsAndTrims() {
        val note = NoteDraft(
            title = "  Trip  ",
            isChecklist = true,
            items = listOf(ChecklistItem("Passport"), ChecklistItem(""))
        ).toNote(id = 1, updatedAt = 0L)
        assertEquals("Trip", note.title)
        assertEquals("[ ] Passport", note.content)
    }
}

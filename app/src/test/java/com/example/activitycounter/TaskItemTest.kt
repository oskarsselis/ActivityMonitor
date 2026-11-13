package com.example.activitycounter

import com.example.activitycounter.model.TaskItem
import com.example.activitycounter.model.splitByDone
import com.example.activitycounter.model.withDone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskItemTest {
    @Test
    fun withDone_setsAndClearsCompletionTime() {
        val task = TaskItem(id = 1, title = "Renew passport", createdAt = 100L)

        val done = task.withDone(true, now = 500L)
        assertTrue(done.isDone)
        assertEquals(500L, done.completedAt)

        val reopened = done.withDone(false, now = 900L)
        assertFalse(reopened.isDone)
        assertEquals(0L, reopened.completedAt)
        assertEquals(task, reopened)
    }

    @Test
    fun splitByDone_openNewestFirstThenCompletedMostRecentFirst() {
        val tasks = listOf(
            TaskItem(id = 1, title = "old open", createdAt = 100L),
            TaskItem(id = 2, title = "done early", createdAt = 200L, isDone = true, completedAt = 1000L),
            TaskItem(id = 3, title = "new open", createdAt = 300L),
            TaskItem(id = 4, title = "done late", createdAt = 150L, isDone = true, completedAt = 2000L),
            // Same creation time as "new open": the later-added one (higher id) comes first
            TaskItem(id = 5, title = "newest open", createdAt = 300L)
        )

        val (open, done) = tasks.splitByDone()

        assertEquals(listOf("newest open", "new open", "old open"), open.map { it.title })
        assertEquals(listOf("done late", "done early"), done.map { it.title })
    }

    @Test
    fun splitByDone_emptyList() {
        val (open, done) = emptyList<TaskItem>().splitByDone()
        assertTrue(open.isEmpty())
        assertTrue(done.isEmpty())
    }
}

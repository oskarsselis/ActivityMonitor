package com.example.activitycounter

import com.example.activitycounter.model.TaskGroup
import com.example.activitycounter.model.TaskItem
import com.example.activitycounter.model.buildTaskBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskBoardTest {
    private val work = TaskGroup(id = 1, name = "Work", createdAt = 200L)
    private val home = TaskGroup(id = 2, name = "Home", createdAt = 100L)

    private fun task(id: Int, groupId: Int?, createdAt: Long, done: Boolean = false, completedAt: Long = 0L) =
        TaskItem(id = id, title = "t$id", groupId = groupId, createdAt = createdAt, isDone = done, completedAt = completedAt)

    @Test
    fun tasksGoUnderTheirGroup_groupsInCreationOrder() {
        val board = buildTaskBoard(
            tasks = listOf(
                task(1, groupId = null, createdAt = 10L),
                task(2, groupId = 1, createdAt = 20L),
                task(3, groupId = 2, createdAt = 30L),
                task(4, groupId = 1, createdAt = 40L)
            ),
            groups = listOf(work, home)
        )

        assertEquals(listOf(1), board.ungrouped.map { it.id })
        // Home was created first; tasks inside a group are newest first
        assertEquals(listOf("Home", "Work"), board.sections.map { it.group.name })
        assertEquals(listOf(3), board.sections[0].openTasks.map { it.id })
        assertEquals(listOf(4, 2), board.sections[1].openTasks.map { it.id })
        assertEquals(4, board.openCount)
    }

    @Test
    fun completedTasksAreListedTogether() {
        val board = buildTaskBoard(
            tasks = listOf(
                task(1, groupId = 1, createdAt = 10L, done = true, completedAt = 500L),
                task(2, groupId = null, createdAt = 20L, done = true, completedAt = 900L),
                task(3, groupId = 1, createdAt = 30L)
            ),
            groups = listOf(work)
        )

        assertEquals(listOf(2, 1), board.completed.map { it.id })
        assertEquals(listOf(3), board.sections.single().openTasks.map { it.id })
        assertEquals(1, board.openCount)
    }

    @Test
    fun emptyGroupsStillGetASection() {
        val board = buildTaskBoard(tasks = emptyList(), groups = listOf(work))
        assertEquals(listOf("Work"), board.sections.map { it.group.name })
        assertTrue(board.sections.single().openTasks.isEmpty())
        assertEquals(0, board.openCount)
    }

    @Test
    fun taskWithMissingGroupIsUngrouped() {
        // e.g. a deleted task restored with Undo after its group was deleted
        val board = buildTaskBoard(tasks = listOf(task(1, groupId = 99, createdAt = 10L)), groups = listOf(work))
        assertEquals(listOf(1), board.ungrouped.map { it.id })
        assertTrue(board.sections.single().openTasks.isEmpty())
    }
}

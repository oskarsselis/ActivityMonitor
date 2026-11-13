package com.example.activitycounter.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// A one-off task with no deadline; it stays on the list until it's ticked off
@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // When the task was ticked off, 0 while it's open; orders the Completed section
    val completedAt: Long = 0L,
    // null = not in a group
    val groupId: Int? = null
)

fun TaskItem.withDone(done: Boolean, now: Long): TaskItem =
    copy(isDone = done, completedAt = if (done) now else 0L)

// Open tasks newest first, and completed tasks most recently completed first
fun List<TaskItem>.splitByDone(): Pair<List<TaskItem>, List<TaskItem>> {
    val (done, open) = partition { it.isDone }
    return open.sortedWith(compareByDescending<TaskItem> { it.createdAt }.thenByDescending { it.id }) to
        done.sortedWith(compareByDescending<TaskItem> { it.completedAt }.thenByDescending { it.id })
}

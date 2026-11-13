package com.example.activitycounter.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_groups")
data class TaskGroup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    // Stored so a folded group stays folded after the app restarts
    val isCollapsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class TaskSection(val group: TaskGroup, val openTasks: List<TaskItem>)

// Everything the Tasks screen shows, in display order
data class TaskBoard(
    val ungrouped: List<TaskItem>,
    val sections: List<TaskSection>,
    val completed: List<TaskItem>
) {
    val openCount: Int get() = ungrouped.size + sections.sumOf { it.openTasks.size }
}

// Groups are shown in the order they were created
fun List<TaskGroup>.inDisplayOrder(): List<TaskGroup> =
    sortedWith(compareBy<TaskGroup> { it.createdAt }.thenBy { it.id })

// Open tasks go under their group; tasks without a group, or whose group no longer exists,
// stay ungrouped. Completed tasks are listed together at the end.
fun buildTaskBoard(tasks: List<TaskItem>, groups: List<TaskGroup>): TaskBoard {
    val (open, completed) = tasks.splitByDone()
    val orderedGroups = groups.inDisplayOrder()
    val groupIds = orderedGroups.map { it.id }.toSet()
    val openByGroup = open.groupBy { task -> task.groupId?.takeIf { it in groupIds } }
    return TaskBoard(
        ungrouped = openByGroup[null].orEmpty(),
        sections = orderedGroups.map { TaskSection(it, openByGroup[it.id].orEmpty()) },
        completed = completed
    )
}

package com.example.activitycounter.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.activitycounter.model.TaskGroup
import com.example.activitycounter.model.TaskItem
import com.example.activitycounter.model.withDone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ActivityDatabase.getDatabase(application)
    private val dao = db.taskDao()

    // null until the database has loaded, so the UI doesn't flash the empty state on startup
    val tasks: StateFlow<List<TaskItem>?> = dao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val groups: StateFlow<List<TaskGroup>?> = dao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun addTask(title: String, groupId: Int?) = viewModelScope.launch {
        dao.insert(TaskItem(title = title.trim(), groupId = groupId))
    }

    fun setDone(task: TaskItem, done: Boolean) = viewModelScope.launch {
        dao.update(task.withDone(done, System.currentTimeMillis()))
    }

    fun editTask(task: TaskItem, title: String, groupId: Int?) = viewModelScope.launch {
        dao.update(task.copy(title = title.trim(), groupId = groupId))
    }

    fun deleteTasks(tasks: List<TaskItem>) = viewModelScope.launch {
        dao.deleteAll(tasks)
    }

    // Undo for deleteTasks
    fun restoreTasks(tasks: List<TaskItem>) = viewModelScope.launch {
        dao.insertAll(tasks)
    }

    fun addGroup(name: String, onCreated: (groupId: Int) -> Unit = {}) = viewModelScope.launch {
        val id = dao.insertGroup(TaskGroup(name = name.trim())).toInt()
        onCreated(id)
    }

    fun renameGroup(group: TaskGroup, name: String) = viewModelScope.launch {
        dao.updateGroup(group.copy(name = name.trim()))
    }

    fun setGroupCollapsed(group: TaskGroup, collapsed: Boolean) = viewModelScope.launch {
        dao.updateGroup(group.copy(isCollapsed = collapsed))
    }

    // Either deletes the group's tasks too, or keeps them without a group
    fun deleteGroup(group: TaskGroup, deleteItsTasks: Boolean) = viewModelScope.launch {
        db.withTransaction {
            if (deleteItsTasks) dao.deleteTasksInGroup(group.id) else dao.ungroupTasks(group.id)
            dao.deleteGroup(group)
        }
    }

    fun isGroupNameTaken(name: String, except: TaskGroup? = null): Boolean =
        groups.value.orEmpty().any { it.id != except?.id && it.name.equals(name.trim(), ignoreCase = true) }
}

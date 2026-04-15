package com.example.activitycounter.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.activitycounter.model.ActivityItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ActivityDatabase.getDatabase(application).activityDao()

    val activities: StateFlow<List<ActivityItem>> = dao.getAllActivities()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addActivity(name: String) = viewModelScope.launch {
        dao.insert(ActivityItem(name = name))
    }

    fun updateCount(activity: ActivityItem, newCount: Int) = viewModelScope.launch {
        dao.update(activity.copy(count = newCount))
    }

    fun deleteActivity(activity: ActivityItem) = viewModelScope.launch {
        dao.delete(activity)
    }

    fun isNameDuplicate(name: String): Boolean {
        return activities.value.any { it.name.equals(name.trim(), ignoreCase = true) }
    }

    fun renameActivity(activity: ActivityItem, newName: String) = viewModelScope.launch {
        dao.update(activity.copy(name = newName.trim()))
    }
}
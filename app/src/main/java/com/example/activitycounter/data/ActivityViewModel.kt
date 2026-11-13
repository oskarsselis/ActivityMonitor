package com.example.activitycounter.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.activitycounter.model.ActivityItem
import com.example.activitycounter.model.toggleCompleted
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ActivityDatabase.getDatabase(application).activityDao()

    // null until the database has loaded, so the UI doesn't flash the empty state on startup
    val activities: StateFlow<List<ActivityItem>?> = dao.getAllActivities()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun addActivity(name: String, periodDays: Int) = viewModelScope.launch {
        dao.insert(ActivityItem(name = name.trim(), periodDays = periodDays))
    }

    fun toggleCompleted(activity: ActivityItem) = viewModelScope.launch {
        dao.update(activity.toggleCompleted(System.currentTimeMillis()))
    }

    fun deleteActivity(activity: ActivityItem) = viewModelScope.launch {
        dao.delete(activity)
    }

    fun resetAll() = viewModelScope.launch {
        dao.resetAll(System.currentTimeMillis())
    }

    fun isNameDuplicate(name: String): Boolean {
        return activities.value.orEmpty().any { it.name.equals(name.trim(), ignoreCase = true) }
    }

    fun editActivity(activity: ActivityItem, newName: String, periodDays: Int) = viewModelScope.launch {
        dao.update(activity.copy(name = newName.trim(), periodDays = periodDays))
    }
}

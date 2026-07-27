package com.example.activitycounter.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.activitycounter.model.ActivityItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class ActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ActivityDatabase.getDatabase(application).activityDao()

    val activities: StateFlow<List<ActivityItem>> = dao.getAllActivities()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addActivity(name: String) = viewModelScope.launch {
        dao.insert(ActivityItem(name = name))
    }

    fun incrementCount(activity: ActivityItem) = viewModelScope.launch {
        dao.update(
            activity.copy(
                count = activity.count + 1,
                lastIncrementTimestamp = System.currentTimeMillis()
            )
        )
    }

    fun decrementCount(activity: ActivityItem) = viewModelScope.launch {
        if (activity.count > 0) {
            dao.update(
                activity.copy(
                    count = activity.count - 1,
                    lastIncrementTimestamp = 0L
                )
            )
        }
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

    companion object {
        fun startOfCurrentWeekMillis(now: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // Calendar.DAY_OF_WEEK: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
            val daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
            return cal.timeInMillis
        }

        fun canIncrement(activity: ActivityItem, now: Long = System.currentTimeMillis()): Boolean {
            return activity.lastIncrementTimestamp < startOfCurrentWeekMillis(now)
        }
    }
}
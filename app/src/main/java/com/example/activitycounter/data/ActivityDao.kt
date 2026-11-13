package com.example.activitycounter.data

import androidx.room.*
import com.example.activitycounter.model.ActivityItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    // Urgency sorting depends on the current time, so it happens in the UI.
    @Query("SELECT * FROM activities ORDER BY id ASC")
    fun getAllActivities(): Flow<List<ActivityItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityItem)

    @Update
    suspend fun update(activity: ActivityItem)

    @Delete
    suspend fun delete(activity: ActivityItem)

    // Every activity starts over as if it was just added; names and periods are kept.
    @Query("UPDATE activities SET createdAt = :now, lastCompletedAt = 0, previousCompletedAt = 0")
    suspend fun resetAll(now: Long)
}
package com.example.activitycounter.data

import androidx.room.*
import com.example.activitycounter.model.ActivityItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY id ASC")
    fun getAllActivities(): Flow<List<ActivityItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityItem)

    @Update
    suspend fun update(activity: ActivityItem)

    @Delete
    suspend fun delete(activity: ActivityItem)
}
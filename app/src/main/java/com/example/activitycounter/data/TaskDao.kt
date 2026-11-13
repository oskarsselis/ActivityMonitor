package com.example.activitycounter.data

import androidx.room.*
import com.example.activitycounter.model.TaskGroup
import com.example.activitycounter.model.TaskItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // Sorting and grouping happen in buildTaskBoard
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Insert
    suspend fun insert(task: TaskItem)

    // REPLACE so deleted tasks can be put back with their original ids
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskItem>)

    @Update
    suspend fun update(task: TaskItem)

    @Delete
    suspend fun deleteAll(tasks: List<TaskItem>)

    @Query("SELECT * FROM task_groups")
    fun getAllGroups(): Flow<List<TaskGroup>>

    @Insert
    suspend fun insertGroup(group: TaskGroup): Long

    @Update
    suspend fun updateGroup(group: TaskGroup)

    @Delete
    suspend fun deleteGroup(group: TaskGroup)

    @Query("UPDATE tasks SET groupId = NULL WHERE groupId = :groupId")
    suspend fun ungroupTasks(groupId: Int)

    @Query("DELETE FROM tasks WHERE groupId = :groupId")
    suspend fun deleteTasksInGroup(groupId: Int)
}

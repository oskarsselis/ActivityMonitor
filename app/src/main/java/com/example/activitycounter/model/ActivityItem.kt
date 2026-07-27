package com.example.activitycounter.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val count: Int = 0,
    val lastIncrementTimestamp: Long = 0L
)
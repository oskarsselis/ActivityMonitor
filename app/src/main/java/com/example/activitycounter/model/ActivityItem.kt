package com.example.activitycounter.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val periodDays: Int = 7,
    // Anchors the period schedule. Set when the activity is created, and again on "reset all".
    val createdAt: Long = System.currentTimeMillis(),
    val lastCompletedAt: Long = 0L,
    // Restored on undo so the activity returns to the color it had before completion.
    val previousCompletedAt: Long = 0L
)

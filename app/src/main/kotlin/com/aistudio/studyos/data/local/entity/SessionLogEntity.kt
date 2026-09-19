package com.aistudio.studyos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_logs")
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val chapter: String,
    val durationMinutes: Int,
    val mode: String,
    val xpEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

package com.aistudio.studyos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String,
    val chapter: String,
    val mode: String, // "regular", "exam", "cram"
    val totalBlocks: Int,
    val currentBlockIndex: Int = 0,
    val durationPerBlockMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val remainingSecondsInBlock: Int = 25 * 60,
    val isBreakPhase: Boolean = false,
    val isCompleted: Boolean = false,
    val isDraft: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

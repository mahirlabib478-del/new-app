package com.aistudio.studyos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val examDate: String, // e.g. "Tomorrow", "In 3 days", "2025-10-15"
    val daysRemaining: Int = 1,
    val priority: String = "High", // "High", "Medium", "Normal"
    val syllabusTopics: String = "",
    val confidenceLevel: Int = 50, // 0 - 100%
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

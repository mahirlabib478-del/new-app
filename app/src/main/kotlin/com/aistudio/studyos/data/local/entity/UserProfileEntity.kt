package com.aistudio.studyos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aistudio.studyos.data.local.ThemeCatalog

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val streakDays: Int = 0,
    val totalStudyMinutes: Int = 0,
    val totalXP: Int = 0,
    val totalXpSpent: Int = 0,
    val totalXpEarned: Int = 0,
    val currentLevel: Int = 1,
    val dailyGoalMinutes: Int = 60,
    val themePreset: String = ThemeCatalog.DEFAULT_THEME,
    val lastActiveDate: String = ""
)

package com.aistudio.studyos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aistudio.studyos.data.local.dao.ExamDao
import com.aistudio.studyos.data.local.dao.SessionLogDao
import com.aistudio.studyos.data.local.dao.StudyPlanDao
import com.aistudio.studyos.data.local.dao.UserProfileDao
import com.aistudio.studyos.data.local.entity.ExamEntity
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.local.entity.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        StudyPlanEntity::class,
        ExamEntity::class,
        SessionLogEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun examDao(): ExamDao
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: StudyDatabase? = null

        fun getInstance(context: Context): StudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "study_os_database"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial data
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            database.userProfileDao().insertOrUpdate(
                                UserProfileEntity(
                                    id = 1,
                                    streakDays = 3,
                                    totalStudyMinutes = 150,
                                    totalXP = 450,
                                    currentLevel = 2,
                                    dailyGoalMinutes = 60,
                                    themePreset = "midnight",
                                    lastActiveDate = "Today"
                                )
                            )
                            database.studyPlanDao().insertPlan(
                                StudyPlanEntity(
                                    title = "Calculus & Linear Algebra",
                                    subject = "Mathematics",
                                    chapter = "Multivariable Derivatives",
                                    mode = "regular",
                                    totalBlocks = 4,
                                    currentBlockIndex = 1,
                                    durationPerBlockMinutes = 25,
                                    breakMinutes = 5,
                                    isCompleted = false,
                                    isDraft = false
                                )
                            )
                            database.examDao().insertExam(
                                ExamEntity(
                                    subject = "Physics: Quantum Mechanics",
                                    examDate = "Tomorrow",
                                    daysRemaining = 1,
                                    priority = "High",
                                    syllabusTopics = "Wave functions, Schrödinger equation, Probability density",
                                    confidenceLevel = 75,
                                    isCompleted = false
                                )
                            )
                            database.examDao().insertExam(
                                ExamEntity(
                                    subject = "Data Structures & Algorithms",
                                    examDate = "In 4 days",
                                    daysRemaining = 4,
                                    priority = "Medium",
                                    syllabusTopics = "Dynamic Programming, Graph algorithms (Dijkstra, Prim)",
                                    confidenceLevel = 60,
                                    isCompleted = false
                                )
                            )
                            database.sessionLogDao().insertLog(
                                SessionLogEntity(
                                    subject = "Mathematics",
                                    chapter = "Partial Differentiation",
                                    durationMinutes = 25,
                                    mode = "regular",
                                    xpEarned = 75,
                                    timestamp = System.currentTimeMillis() - 86400000L
                                )
                            )
                            database.sessionLogDao().insertLog(
                                SessionLogEntity(
                                    subject = "Physics",
                                    chapter = "Photoelectric Effect",
                                    durationMinutes = 50,
                                    mode = "exam",
                                    xpEarned = 150,
                                    timestamp = System.currentTimeMillis() - 43200000L
                                )
                            )
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

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
    version = 3,
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
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Fresh start: only insert a clean initial profile (0 streak, 0 XP, Level 1)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            database.userProfileDao().insertOrUpdate(
                                UserProfileEntity(
                                    id = 1,
                                    streakDays = 0,
                                    totalStudyMinutes = 0,
                                    totalXP = 0,
                                    currentLevel = 1,
                                    dailyGoalMinutes = 60,
                                    themePreset = "midnight",
                                    lastActiveDate = ""
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

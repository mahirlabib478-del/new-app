package com.aistudio.studyos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aistudio.studyos.data.local.dao.ExamDao
import com.aistudio.studyos.data.local.dao.SessionLogDao
import com.aistudio.studyos.data.local.dao.StudyPlanDao
import com.aistudio.studyos.data.local.dao.UserProfileDao
import com.aistudio.studyos.data.local.entity.ExamEntity
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.local.entity.UserProfileEntity

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
.build()
                INSTANCE = instance
                instance
            }
        }
    }
}

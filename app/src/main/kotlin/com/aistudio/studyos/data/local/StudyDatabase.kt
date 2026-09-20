package com.aistudio.studyos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 6,
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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE study_plans ADD COLUMN accumulatedStudiedSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_plans ADD COLUMN accumulatedBillableMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE study_plans ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_plans ADD COLUMN isTimerRunning INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_plans ADD COLUMN endAtElapsedRealtime INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_plans ADD COLUMN endAtWallClockMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_plans ADD COLUMN timerBootCount INTEGER NOT NULL DEFAULT -1")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE study_plans ADD COLUMN planItems TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE study_plans ADD COLUMN totalDurationMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE study_plans SET totalDurationMinutes = durationPerBlockMinutes * totalBlocks " +
                        "WHERE totalDurationMinutes = 0"
                )
            }
        }

        fun getInstance(context: Context): StudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "study_os_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

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
import java.security.MessageDigest

@Database(
    entities = [
        StudyPlanEntity::class,
        ExamEntity::class,
        SessionLogEntity::class,
        UserProfileEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun examDao(): ExamDao
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        private const val LEGACY_DATABASE_NAME = "study_os_database"

        @Volatile
        private var INSTANCE: StudyDatabase? = null

        private val accountInstances = mutableMapOf<String, StudyDatabase>()

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN totalXpSpent INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN levelStartStudyMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN levelStartXpEarned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN levelStartXpSpent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN levelStartedAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE user_profile SET levelStartStudyMinutes = totalStudyMinutes, levelStartXpEarned = totalXpEarned, levelStartXpSpent = totalXpSpent, levelStartedAtMillis = CAST(strftime('%s','now') AS INTEGER) * 1000")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN totalXpEarned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE user_profile SET totalXpEarned = totalXP")
            }
        }

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

        private val ALL_MIGRATIONS = arrayOf(
            MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
            MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9
        )

        private fun build(context: Context, name: String): StudyDatabase =
            Room.databaseBuilder(context.applicationContext, StudyDatabase::class.java, name)
                // Never silently destroy user data when a future migration is missing.
                .addMigrations(*ALL_MIGRATIONS)
                .build()

        /** Opens the original database unchanged; retained for legacy/guest data. */
        fun getInstance(context: Context): StudyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context, LEGACY_DATABASE_NAME).also { INSTANCE = it }
            }
        }

        /**
         * Returns a database isolated to a Firebase UID. The UID is hashed before
         * becoming part of a filename; raw account identifiers are never persisted
         * in the filename. This does not migrate or import legacy data.
         */
        fun getAccountInstance(context: Context, uid: String): StudyDatabase {
            require(uid.isNotBlank()) { "A non-empty authenticated UID is required" }
            val key = accountDatabaseName(uid)
            return synchronized(accountInstances) {
                accountInstances.getOrPut(key) { build(context, key) }
            }
        }

        internal fun accountDatabaseName(uid: String): String {
            require(uid.isNotBlank())
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(uid.toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString("") { "%02x".format(it) }
            return "study_os_account_$hex"
        }
    }
}

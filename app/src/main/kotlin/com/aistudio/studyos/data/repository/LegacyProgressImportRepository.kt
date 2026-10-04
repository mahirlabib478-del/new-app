package com.aistudio.studyos.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.aistudio.studyos.data.local.StudyDatabase
import com.google.firebase.auth.FirebaseAuth

data class LegacyImportSummary(
    val plans: Int,
    val exams: Int,
    val sessions: Int,
    val importedProfile: Boolean
) {
    val isEmpty: Boolean get() = plans == 0 && exams == 0 && sessions == 0 && !importedProfile
}

/**
 * Copies legacy device progress only after an explicit user action.
 * The source database is read-only from this class and is never cleared.
 */
class LegacyProgressImportRepository(
    context: Context,
    private val uid: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val appContext = context.applicationContext

    suspend fun importLegacyProgress(): LegacyImportSummary {
        require(uid.isNotBlank()) { "Sign in before importing local progress." }
        require(auth.currentUser?.uid == uid && auth.currentUser?.isEmailVerified == true) {
            "A verified account is required to import progress."
        }

        val legacy = StudyDatabase.getInstance(appContext)
        val account = StudyDatabase.getAccountInstance(appContext, uid)

        val plans = legacy.studyPlanDao().getAllForBackup()
        val exams = legacy.examDao().getAllForBackup()
        val sessions = legacy.sessionLogDao().getAllForBackup()
        val oldProfiles = legacy.userProfileDao().getAllForBackup()
        if (plans.isEmpty() && exams.isEmpty() && sessions.isEmpty() &&
            oldProfiles.none { it.totalStudyMinutes > 0 || it.totalXP > 0 || it.totalXpEarned > 0 || it.totalXpSpent > 0 || it.streakDays > 0 }
        ) {
            return LegacyImportSummary(0, 0, 0, false)
        }

        require(auth.currentUser?.uid == uid && auth.currentUser?.isEmailVerified == true) {
            "Account changed during import. Please try again."
        }

        account.withTransaction {
            require(auth.currentUser?.uid == uid && auth.currentUser?.isEmailVerified == true) {
                "Account changed during import. No records were imported."
            }

            val existingPlans = account.studyPlanDao().getAllForBackup()
            val existingExams = account.examDao().getAllForBackup()
            val existingSessions = account.sessionLogDao().getAllForBackup()
            val existingProfiles = account.userProfileDao().getAllForBackup()
            require(existingPlans.isEmpty() && existingExams.isEmpty() && existingSessions.isEmpty()) {
                "This account already has study records. Import was cancelled and nothing was changed."
            }
            val existingProfile = existingProfiles.firstOrNull()
            require(existingProfile == null ||
                (existingProfile.totalStudyMinutes == 0 && existingProfile.totalXP == 0 &&
                    existingProfile.totalXpEarned == 0 && existingProfile.totalXpSpent == 0 &&
                    existingProfile.streakDays == 0)
            ) {
                "This account already has progress. Import was cancelled and nothing was changed."
            }

            account.studyPlanDao().insertAllForRestore(plans)
            account.examDao().insertAllForRestore(exams)
            account.sessionLogDao().insertAllForRestore(sessions)

            val legacyProfile = oldProfiles.firstOrNull {
                it.totalStudyMinutes > 0 || it.totalXP > 0 || it.totalXpEarned > 0 ||
                    it.totalXpSpent > 0 || it.streakDays > 0
            }
            if (legacyProfile != null) {
                val profileToImport = if (existingProfile != null) {
                    legacyProfile.copy(
                        id = 1,
                        themePreset = existingProfile.themePreset,
                        dailyGoalMinutes = existingProfile.dailyGoalMinutes
                    )
                } else {
                    legacyProfile.copy(id = 1)
                }
                account.userProfileDao().insertOrUpdate(profileToImport)
            }
        }

        return LegacyImportSummary(
            plans = plans.size,
            exams = exams.size,
            sessions = sessions.size,
            importedProfile = oldProfiles.any {
                it.totalStudyMinutes > 0 || it.totalXP > 0 || it.totalXpEarned > 0 ||
                    it.totalXpSpent > 0 || it.streakDays > 0
            }
        )
    }
}

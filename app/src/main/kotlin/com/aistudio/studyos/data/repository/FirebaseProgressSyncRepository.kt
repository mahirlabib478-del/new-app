package com.aistudio.studyos.data.repository

import com.aistudio.studyos.data.local.StudyDatabase
import androidx.room.InvalidationTracker
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Uploads a complete local progress snapshot to the signed-in user's private Firestore document.
 * This is an upload-only safety checkpoint; restore/merge is intentionally separate so a sign-in
 * cannot silently overwrite either device's data.
 */
class FirebaseProgressSyncRepository(
    private val database: StudyDatabase,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun startAutomaticUpload(scope: CoroutineScope) {
        var pendingUpload: Job? = null
        val observer = object : InvalidationTracker.Observer("study_plans", "exams", "session_logs", "user_profile") {
            override fun onInvalidated(tables: Set<String>) {
                if (auth.currentUser == null) return
                pendingUpload?.cancel()
                pendingUpload = scope.launch {
                    delay(1800)
                    runCatching { uploadLocalSnapshot() }
                }
            }
        }
        database.invalidationTracker.addObserver(observer)
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) pendingUpload?.cancel()
        }
    }

    suspend fun uploadLocalSnapshot() {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Sign in before syncing progress.")
        val plans = database.studyPlanDao().getAllForBackup()
        val exams = database.examDao().getAllForBackup()
        val sessions = database.sessionLogDao().getAllForBackup()
        val profiles = database.userProfileDao().getAllForBackup()

        val snapshot = hashMapOf<String, Any>(
            "schemaVersion" to 1,
            "studyPlans" to plans.map { it.toCloudMap() },
            "exams" to exams.map { it.toCloudMap() },
            "sessionLogs" to sessions.map { it.toCloudMap() },
            "profiles" to profiles.map { it.toCloudMap() },
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(uid)
            .collection("progress").document("current")
            .set(snapshot)
            .asSuspendUnit()
    }

    private suspend fun Task<Void>.asSuspendUnit() = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) continuation.resume(Unit)
            else continuation.resumeWithException(task.exception ?: IllegalStateException("Cloud progress upload failed."))
        }
    }

    private fun com.aistudio.studyos.data.local.entity.StudyPlanEntity.toCloudMap() = mapOf(
        "id" to id, "title" to title, "subject" to subject, "chapter" to chapter,
        "mode" to mode, "totalBlocks" to totalBlocks, "currentBlockIndex" to currentBlockIndex,
        "durationPerBlockMinutes" to durationPerBlockMinutes, "breakMinutes" to breakMinutes,
        "remainingSecondsInBlock" to remainingSecondsInBlock, "isBreakPhase" to isBreakPhase,
        "isCompleted" to isCompleted, "isDraft" to isDraft, "isArchived" to isArchived,
        "isTimerRunning" to isTimerRunning, "endAtElapsedRealtime" to endAtElapsedRealtime,
        "endAtWallClockMillis" to endAtWallClockMillis, "timerBootCount" to timerBootCount,
        "accumulatedStudiedSeconds" to accumulatedStudiedSeconds,
        "accumulatedBillableMinutes" to accumulatedBillableMinutes,
        "createdAt" to createdAt, "lastUpdated" to lastUpdated, "planItems" to planItems,
        "totalDurationMinutes" to totalDurationMinutes
    )

    private fun com.aistudio.studyos.data.local.entity.ExamEntity.toCloudMap() = mapOf(
        "id" to id, "subject" to subject, "examDate" to examDate,
        "daysRemaining" to daysRemaining, "priority" to priority,
        "syllabusTopics" to syllabusTopics, "confidenceLevel" to confidenceLevel,
        "isCompleted" to isCompleted, "createdAt" to createdAt
    )

    private fun com.aistudio.studyos.data.local.entity.SessionLogEntity.toCloudMap() = mapOf(
        "id" to id, "subject" to subject, "chapter" to chapter,
        "durationMinutes" to durationMinutes, "mode" to mode, "xpEarned" to xpEarned,
        "timestamp" to timestamp
    )

    private fun com.aistudio.studyos.data.local.entity.UserProfileEntity.toCloudMap() = mapOf(
        "id" to id, "streakDays" to streakDays, "totalStudyMinutes" to totalStudyMinutes,
        "totalXP" to totalXP, "totalXpSpent" to totalXpSpent, "totalXpEarned" to totalXpEarned,
        "levelStartStudyMinutes" to levelStartStudyMinutes,
        "levelStartXpEarned" to levelStartXpEarned, "levelStartXpSpent" to levelStartXpSpent,
        "levelStartedAtMillis" to levelStartedAtMillis, "currentLevel" to currentLevel,
        "dailyGoalMinutes" to dailyGoalMinutes, "themePreset" to themePreset,
        "lastActiveDate" to lastActiveDate
    )
}

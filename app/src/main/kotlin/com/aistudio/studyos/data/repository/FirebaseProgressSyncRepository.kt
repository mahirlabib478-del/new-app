package com.aistudio.studyos.data.repository

import com.aistudio.studyos.data.local.StudyDatabase
import androidx.room.withTransaction
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.local.entity.ExamEntity
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.data.local.entity.UserProfileEntity
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
    @Volatile private var cloudUploadUid: String? = null

    fun startAutomaticUpload(scope: CoroutineScope) {
        var pendingUpload: Job? = null
        val observer = object : InvalidationTracker.Observer("study_plans", "exams", "session_logs", "user_profile") {
            override fun onInvalidated(tables: Set<String>) {
                if (auth.currentUser?.uid == null || auth.currentUser?.uid != cloudUploadUid) return
                pendingUpload?.cancel()
                pendingUpload = scope.launch {
                    delay(1800)
                    runCatching { uploadLocalSnapshot() }
                }
            }
        }
        database.invalidationTracker.addObserver(observer)
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser?.uid != cloudUploadUid) {
                cloudUploadUid = null
                pendingUpload?.cancel()
            }
        }
    }

    /** Restores cloud data only when this device has no local progress. Never overwrites populated local data. */
    suspend fun restoreIfLocalEmpty(): String {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Sign in before syncing progress.")
        val ref = firestore.collection("users").document(uid).collection("progress").document("current")
        val cloud = ref.get().asSuspendResult().data ?: run {
            if (auth.currentUser?.uid != uid) throw IllegalStateException("Account changed during sync. Please retry.")
            val hasLocalProgress =
                database.studyPlanDao().getAllForBackup().isNotEmpty() ||
                database.examDao().getAllForBackup().isNotEmpty() ||
                database.sessionLogDao().getAllForBackup().isNotEmpty() ||
                database.userProfileDao().getAllForBackup().any {
                    it.totalStudyMinutes > 0 || it.totalXP > 0 || it.streakDays > 0
                }
            if (hasLocalProgress) {
                cloudUploadUid = null
                return "No cloud backup exists, but local progress is present. Automatic upload is paused to prevent transferring another account's data."
            }
            cloudUploadUid = uid
            return "No cloud backup found and this device has no progress. Sync is ready for this account."
        }
        if (auth.currentUser?.uid != uid) throw IllegalStateException("Account changed during sync. Please retry.")
        val localPlans = database.studyPlanDao().getAllForBackup()
        val localExams = database.examDao().getAllForBackup()
        val localSessions = database.sessionLogDao().getAllForBackup()
        val localProfiles = database.userProfileDao().getAllForBackup()
        val cloudPlans = (cloud["studyPlans"] as? List<*>)?.mapNotNull { it.asMap()?.toStudyPlan() }.orEmpty()
        val cloudExams = (cloud["exams"] as? List<*>)?.mapNotNull { it.asMap()?.toExam() }.orEmpty()
        val cloudSessions = (cloud["sessionLogs"] as? List<*>)?.mapNotNull { it.asMap()?.toSession() }.orEmpty()
        val cloudProfiles = (cloud["profiles"] as? List<*>)?.mapNotNull { it.asMap()?.toProfile() }.orEmpty()
        val hasLocal = localPlans.isNotEmpty() || localExams.isNotEmpty() || localSessions.isNotEmpty() || localProfiles.any { it.totalStudyMinutes > 0 || it.totalXP > 0 || it.streakDays > 0 }

        // The local Room database is shared across Firebase accounts. If both sides
        // contain data, we cannot prove the local records belong to this UID. Do not
        // merge or upload them automatically: that could leak one account's progress
        // into another account. Keep uploads disabled until an explicit account-scoped
        // migration/merge flow is implemented.
        val hasCloud = cloudPlans.isNotEmpty() || cloudExams.isNotEmpty() ||
            cloudSessions.isNotEmpty() || cloudProfiles.any {
                it.totalStudyMinutes > 0 || it.totalXP > 0 || it.streakDays > 0
            }
        if (hasLocal) {
            cloudUploadUid = null
            return if (hasCloud) {
                "Both local and cloud progress exist. Automatic merge is paused to protect account data; no records were changed."
            } else {
                "This device has local progress, but the signed-in account's cloud document has no progress records. Automatic upload is paused to prevent transferring another account's data."
            }
        }

        val newPlans = cloudPlans.filter { remote -> localPlans.none { it.createdAt == remote.createdAt && it.title == remote.title && it.subject == remote.subject } }.map { it.copy(id = 0L) }
        val newExams = cloudExams.filter { remote -> localExams.none { it.createdAt == remote.createdAt && it.subject == remote.subject && it.examDate == remote.examDate } }.map { it.copy(id = 0L) }
        val newSessions = cloudSessions.filter { remote -> localSessions.none { it.timestamp == remote.timestamp && it.subject == remote.subject && it.chapter == remote.chapter && it.durationMinutes == remote.durationMinutes && it.mode == remote.mode && it.xpEarned == remote.xpEarned } }.map { it.copy(id = 0L) }
        val mergedProfile = (localProfiles.firstOrNull() ?: cloudProfiles.firstOrNull())?.let { local ->
            val remote = cloudProfiles.firstOrNull() ?: local
            // Keep level-mission baselines from the more advanced/newer profile. Taking
            // max(currentLevel) while retaining the other device's baselines can reset
            // or prematurely complete missions after a restore.
            val levelState = when {
                remote.currentLevel > local.currentLevel -> remote
                local.currentLevel > remote.currentLevel -> local
                remote.levelStartedAtMillis > local.levelStartedAtMillis -> remote
                else -> local
            }
            levelState.copy(
                id = local.id,
                streakDays = maxOf(local.streakDays, remote.streakDays),
                totalStudyMinutes = maxOf(local.totalStudyMinutes, remote.totalStudyMinutes),
                totalXP = maxOf(local.totalXP, remote.totalXP),
                totalXpSpent = maxOf(local.totalXpSpent, remote.totalXpSpent),
                totalXpEarned = maxOf(local.totalXpEarned, remote.totalXpEarned),
                currentLevel = maxOf(local.currentLevel, remote.currentLevel),
                dailyGoalMinutes = local.dailyGoalMinutes
            )
        }
        if (auth.currentUser?.uid != uid) throw IllegalStateException("Account changed during sync. Please retry.")
        database.withTransaction {
            database.studyPlanDao().insertAllForRestore(newPlans)
            database.examDao().insertAllForRestore(newExams)
            database.sessionLogDao().insertAllForRestore(newSessions)
            mergedProfile?.let { database.userProfileDao().insertOrUpdate(it) }
        }
        if (auth.currentUser?.uid != uid) throw IllegalStateException("Account changed during sync. Please retry.")
        cloudUploadUid = uid
        if (hasLocal) {
            uploadLocalSnapshot()
            return "Local and cloud records were merged without matching duplicate sessions; merged snapshot uploaded."
        }
        return "Cloud progress restored to this empty device."
    }

    private suspend fun <T> Task<T>.asSuspendResult(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) continuation.resume(task.result)
            else continuation.resumeWithException(task.exception ?: IllegalStateException("Cloud request failed."))
        }
    }
    private fun Any?.asMap(): Map<String, Any?>? = this as? Map<String, Any?>
    private fun Map<String, Any?>.n(key: String, d: Long = 0L) = (this[key] as? Number)?.toLong() ?: d
    private fun Map<String, Any?>.i(key: String, d: Int = 0) = n(key, d.toLong()).toInt()
    private fun Map<String, Any?>.s(key: String, d: String = "") = this[key] as? String ?: d
    private fun Map<String, Any?>.b(key: String, d: Boolean = false) = this[key] as? Boolean ?: d
    private fun Map<String, Any?>.toStudyPlan() = StudyPlanEntity(id=n("id"), title=s("title"), subject=s("subject"), chapter=s("chapter"), mode=s("mode"), totalBlocks=i("totalBlocks"), currentBlockIndex=i("currentBlockIndex"), durationPerBlockMinutes=i("durationPerBlockMinutes",25), breakMinutes=i("breakMinutes",5), remainingSecondsInBlock=i("remainingSecondsInBlock",1500), isBreakPhase=b("isBreakPhase"), isCompleted=b("isCompleted"), isDraft=b("isDraft"), isArchived=b("isArchived"), isTimerRunning=b("isTimerRunning"), endAtElapsedRealtime=n("endAtElapsedRealtime"), endAtWallClockMillis=n("endAtWallClockMillis"), timerBootCount=i("timerBootCount",-1), accumulatedStudiedSeconds=i("accumulatedStudiedSeconds"), accumulatedBillableMinutes=i("accumulatedBillableMinutes"), createdAt=n("createdAt"), lastUpdated=n("lastUpdated"), planItems=s("planItems"), totalDurationMinutes=i("totalDurationMinutes"))
    private fun Map<String, Any?>.toExam() = ExamEntity(id=n("id"), subject=s("subject"), examDate=s("examDate"), daysRemaining=i("daysRemaining",1), priority=s("priority","High"), syllabusTopics=s("syllabusTopics"), confidenceLevel=i("confidenceLevel",50), isCompleted=b("isCompleted"), createdAt=n("createdAt"))
    private fun Map<String, Any?>.toSession() = SessionLogEntity(id=n("id"), subject=s("subject"), chapter=s("chapter"), durationMinutes=i("durationMinutes"), mode=s("mode"), xpEarned=i("xpEarned"), timestamp=n("timestamp"))
    private fun Map<String, Any?>.toProfile() = UserProfileEntity(id=i("id",1), streakDays=i("streakDays"), totalStudyMinutes=i("totalStudyMinutes"), totalXP=i("totalXP"), totalXpSpent=i("totalXpSpent"), totalXpEarned=i("totalXpEarned"), levelStartStudyMinutes=i("levelStartStudyMinutes"), levelStartXpEarned=i("levelStartXpEarned"), levelStartXpSpent=i("levelStartXpSpent"), levelStartedAtMillis=n("levelStartedAtMillis"), currentLevel=i("currentLevel",1), dailyGoalMinutes=i("dailyGoalMinutes",60), themePreset=s("themePreset"), lastActiveDate=s("lastActiveDate"))

    suspend fun uploadLocalSnapshot() {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Sign in before syncing progress.")
        if (cloudUploadUid != uid) throw IllegalStateException("Sync is paused until cloud and local progress are safely reconciled.")
        val plans = database.studyPlanDao().getAllForBackup()
        val exams = database.examDao().getAllForBackup()
        val sessions = database.sessionLogDao().getAllForBackup()
        val profiles = database.userProfileDao().getAllForBackup()

        // Account state may change while Room is being read. Revalidate ownership
        // immediately before writing the snapshot to the captured UID's document.
        if (auth.currentUser?.uid != uid || cloudUploadUid != uid) {
            throw IllegalStateException("Account changed during sync. Please retry.")
        }

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

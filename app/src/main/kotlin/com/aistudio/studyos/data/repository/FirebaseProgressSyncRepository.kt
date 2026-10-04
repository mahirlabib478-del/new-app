package com.aistudio.studyos.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    context: Context,
    private val database: StudyDatabase,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val ownershipPrefs = context.applicationContext.getSharedPreferences("progress_sync_ownership", Context.MODE_PRIVATE)
    @Volatile private var cloudUploadUid: String? = null
    private val _syncStatus = MutableStateFlow("Sync not enabled")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()
    @Volatile private var lastSuccessfulSyncMillis: Long = 0L
    private var pendingUpload: Job? = null
    private var invalidationObserver: InvalidationTracker.Observer? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null
    private var uploadScope: CoroutineScope? = null

    @Synchronized
    fun startAutomaticUpload(scope: CoroutineScope) {
        stopAutomaticUpload()
        uploadScope = scope
        cloudUploadUid = auth.currentUser
            ?.takeIf { it.isEmailVerified && ownershipPrefs.getBoolean(ownerKey(it.uid), false) }
            ?.uid
        _syncStatus.value = if (cloudUploadUid != null) "Sync ready" else "Enable cloud sync to protect your progress"
        val observer = object : InvalidationTracker.Observer("study_plans", "exams", "session_logs", "user_profile") {
            override fun onInvalidated(tables: Set<String>) {
                scheduleSnapshotUpload()
            }
        }
        invalidationObserver = observer
        database.invalidationTracker.addObserver(observer)
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Connectivity has returned: immediately retry the latest local snapshot.
                scheduleSnapshotUpload()
            }
        }
        connectivityCallback = networkCallback
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (_: Exception) {
            _syncStatus.value = if (cloudUploadUid != null) "Waiting for network" else "Enable cloud sync to protect your progress"
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val activeUser = firebaseAuth.currentUser
            val activeUid = activeUser?.takeIf {
                it.isEmailVerified && ownershipPrefs.getBoolean(ownerKey(it.uid), false)
            }?.uid
            if (activeUid == null || activeUid != cloudUploadUid) {
                cloudUploadUid = null
                pendingUpload?.cancel()
                _syncStatus.value = if (activeUser == null) "Signed out" else "Cloud sync paused"
            } else {
                cloudUploadUid = activeUid
                _syncStatus.value = "Sync ready"
            }
        }
        authStateListener = listener
        auth.addAuthStateListener(listener)
    }

    private fun scheduleSnapshotUpload() {
        val active = auth.currentUser
        if (active?.isEmailVerified != true || active.uid != cloudUploadUid) return
        val scope = uploadScope ?: return
        pendingUpload?.cancel()
        pendingUpload = scope.launch {
            delay(500)
            var lastError: Throwable? = null
            for (attempt in 0 until 5) {
                if (auth.currentUser?.uid != cloudUploadUid || auth.currentUser?.isEmailVerified != true) return@launch
                _syncStatus.value = if (attempt == 0) "Syncing…" else "Retrying cloud sync (" + (attempt + 1) + "/5)…"
                try {
                    uploadLocalSnapshot()
                    lastSuccessfulSyncMillis = System.currentTimeMillis()
                    _syncStatus.value = "Synced just now"
                    lastError = null
                    break
                } catch (error: Exception) {
                    lastError = error
                    if (attempt < 4) delay(1000L * (attempt + 1))
                }
            }
            if (lastError != null) _syncStatus.value = "Sync failed. Changes remain on this device; waiting for network or another change."
        }
    }

    @Synchronized
    fun stopAutomaticUpload() {
        pendingUpload?.cancel()
        pendingUpload = null
        invalidationObserver?.let { database.invalidationTracker.removeObserver(it) }
        invalidationObserver = null
        authStateListener?.let { auth.removeAuthStateListener(it) }
        authStateListener = null
        connectivityCallback?.let {
            try { connectivityManager.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        connectivityCallback = null
        uploadScope = null
        cloudUploadUid = null
        _syncStatus.value = "Cloud sync paused"
    }

    private fun ownerKey(uid: String) = "owner_$uid"

    /** Restores cloud data only when this device has no local progress. Never overwrites populated local data. */
    suspend fun restoreIfLocalEmpty(): String {
        // Pause observer-driven uploads while reconciliation is in progress. If any
        // read, validation, or restore step fails, uploads stay paused until a later
        // explicit successful sync decision re-arms this UID.
        cloudUploadUid = null
        val user = auth.currentUser ?: throw IllegalStateException("Sign in before syncing progress.")
        if (!user.isEmailVerified) throw IllegalStateException("Verify your email before syncing progress.")
        val uid = user.uid
        // A manual restore attempt invalidates prior upload authorization until this
        // restore safely completes; failures must not re-enable uploads on next launch.
        ownershipPrefs.edit().remove(ownerKey(uid)).apply()
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
            // Room reads are asynchronous boundaries: never arm upload for a UID that
            // signed out or changed while the local ownership check was running.
            if (auth.currentUser?.uid != uid) {
                cloudUploadUid = null
                throw IllegalStateException("Account changed during sync. Please retry.")
            }
            if (hasLocalProgress) {
                cloudUploadUid = null
                return "No cloud backup exists, but local progress is present. Automatic upload is paused to prevent transferring another account's data."
            }
            cloudUploadUid = null
            return "No cloud backup found and this device has no progress. Automatic sync remains paused until an explicit account-link action is completed."
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
        // Revalidate after all local reads, before deciding whether this account may
        // restore or enable future automatic uploads.
        if (auth.currentUser?.uid != uid) {
            cloudUploadUid = null
            throw IllegalStateException("Account changed during sync. Please retry.")
        }
        val hasLocal = localPlans.isNotEmpty() || localExams.isNotEmpty() || localSessions.isNotEmpty() || localProfiles.any { it.totalStudyMinutes > 0 || it.totalXP > 0 || it.streakDays > 0 }

        // Even in this UID-scoped database, populated local and cloud snapshots may
        // have diverged. Do not silently merge or overwrite either copy; keep uploads
        // disabled until the user explicitly resolves the conflict.
        val hasCloud = cloudPlans.isNotEmpty() || cloudExams.isNotEmpty() ||
            cloudSessions.isNotEmpty() || cloudProfiles.any {
                it.totalStudyMinutes > 0 || it.totalXP > 0 || it.streakDays > 0
            }
        if (hasLocal) {
            cloudUploadUid = null
            return if (hasCloud) {
                "Both local and cloud progress exist. Automatic merge is paused to protect both copies; no records were changed."
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
            // Recheck ownership inside the transaction as well as before it. If the
            // account changed while Room was preparing the restore, throwing here
            // rolls back the whole restore instead of leaving a partial import.
            if (auth.currentUser?.uid != uid) {
                throw IllegalStateException("Account changed during sync. Please retry.")
            }
            database.studyPlanDao().insertAllForRestore(newPlans)
            database.examDao().insertAllForRestore(newExams)
            database.sessionLogDao().insertAllForRestore(newSessions)
            mergedProfile?.let { database.userProfileDao().insertOrUpdate(it) }
            if (auth.currentUser?.uid != uid) {
                throw IllegalStateException("Account changed during sync. Please retry.")
            }
        }
        if (hasCloud && auth.currentUser?.uid == uid) {
            ownershipPrefs.edit().putBoolean(ownerKey(uid), true).apply()
            cloudUploadUid = uid
            lastSuccessfulSyncMillis = System.currentTimeMillis()
            _syncStatus.value = "Synced just now"
            return "Cloud progress restored to this empty device. Automatic cloud sync is enabled."
        }
        cloudUploadUid = null
        return "The cloud document has no progress records. Automatic sync remains paused until an explicit account-link action is completed."
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

    /**
     * Creates the first cloud backup only when no cloud document exists.
     * Existing cloud data is never overwritten by this action.
     */
    suspend fun createInitialCloudBackup(): String {
        val user = auth.currentUser
            ?: throw IllegalStateException("Sign in before backing up progress.")
        val uid = user.uid
        if (!user.isEmailVerified) throw IllegalStateException("Verify your email before syncing progress.")
        cloudUploadUid = null
        // If the initial-backup attempt fails or finds an existing cloud document,
        // keep uploads disabled rather than letting a stale link overwrite that data.
        ownershipPrefs.edit().remove(ownerKey(uid)).apply()

        val plans = database.studyPlanDao().getAllForBackup()
        val exams = database.examDao().getAllForBackup()
        val sessions = database.sessionLogDao().getAllForBackup()
        val profiles = database.userProfileDao().getAllForBackup()
        if (auth.currentUser?.uid != uid || auth.currentUser?.isEmailVerified != true) {
            throw IllegalStateException("Account changed during backup. Please retry.")
        }

        val snapshot = hashMapOf<String, Any>(
            "schemaVersion" to 1,
            "studyPlans" to plans.map { it.toCloudMap() },
            "exams" to exams.map { it.toCloudMap() },
            "sessionLogs" to sessions.map { it.toCloudMap() },
            "profiles" to profiles.map { it.toCloudMap() },
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        val ref = firestore.collection("users").document(uid)
            .collection("progress").document("current")

        firestore.runTransaction { transaction ->
            if (auth.currentUser?.uid != uid || auth.currentUser?.isEmailVerified != true) {
                throw IllegalStateException("Account changed during backup. Please retry.")
            }
            val existing = transaction.get(ref)
            if (existing.exists()) {
                throw IllegalStateException("A cloud backup already exists. Restore it first; this action will not overwrite it.")
            }
            transaction.set(ref, snapshot)
            null
        }.asSuspendResult()

        if (auth.currentUser?.uid != uid) {
            cloudUploadUid = null
            throw IllegalStateException("Account changed after backup. Please sign in again.")
        }
        ownershipPrefs.edit().putBoolean(ownerKey(uid), true).apply()
        cloudUploadUid = uid
        lastSuccessfulSyncMillis = System.currentTimeMillis()
        _syncStatus.value = "Synced just now"
        return "Initial cloud backup created. Automatic cloud sync is now enabled."
    }

    suspend fun uploadLocalSnapshot() {
        val user = auth.currentUser
            ?: throw IllegalStateException("Sign in before syncing progress.")
        if (!user.isEmailVerified) {
            throw IllegalStateException("Verify your email before syncing progress.")
        }
        val uid = user.uid
        if (cloudUploadUid != uid) throw IllegalStateException("Sync is paused until cloud and local progress are safely reconciled.")
        val plans = database.studyPlanDao().getAllForBackup()
        val exams = database.examDao().getAllForBackup()
        val sessions = database.sessionLogDao().getAllForBackup()
        val profiles = database.userProfileDao().getAllForBackup()

        // Account state may change while Room is being read. Revalidate ownership
        // immediately before writing the snapshot to the captured UID's document.
        if (auth.currentUser?.uid != uid ||
            auth.currentUser?.isEmailVerified != true ||
            cloudUploadUid != uid
        ) {
            throw IllegalStateException("Account changed or is no longer verified during sync. Please retry.")
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
        if (auth.currentUser?.uid != uid || auth.currentUser?.isEmailVerified != true || cloudUploadUid != uid) {
            throw IllegalStateException("Account changed after upload. Please check sync status.")
        }
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

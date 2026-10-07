package com.aistudio.studyos.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.aistudio.studyos.data.local.StudyDatabase
import com.aistudio.studyos.data.local.ThemePreferences
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
    private val themePreferences: ThemePreferences = ThemePreferences(context),
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
    private var preferenceChangeListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

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
        val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            scheduleSnapshotUpload()
        }
        preferenceChangeListener = prefListener
        themePreferences.registerCloudSyncListener(prefListener)
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
        preferenceChangeListener?.let { themePreferences.unregisterCloudSyncListener(it) }
        preferenceChangeListener = null
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

    /**
     * Flushes the latest local snapshot before an intentional sign-out.
     * Debounced observer uploads are otherwise cancelled by the auth listener when
     * the user signs out immediately after an activity.
     */
    suspend fun syncNowBeforeSignOut(): String {
        val user = auth.currentUser ?: return "Already signed out."
        if (!user.isEmailVerified) {
            throw IllegalStateException("Verify your email before syncing progress.")
        }
        if (cloudUploadUid != user.uid ||
            !ownershipPrefs.getBoolean(ownerKey(user.uid), false)
        ) {
            throw IllegalStateException(
                "Cloud sync is not ready for this account yet. Stay signed in and wait until Cloud sync reports success, then sign out. Your local progress has not been intentionally deleted."
            )
        }

        pendingUpload?.cancel()
        pendingUpload = null
        _syncStatus.value = "Syncing before sign-out…"
        return try {
            uploadLocalSnapshot()
            lastSuccessfulSyncMillis = System.currentTimeMillis()
            _syncStatus.value = "Synced just now"
            "Latest progress synced."
        } catch (error: Exception) {
            _syncStatus.value = "Sync failed before sign-out"
            throw IllegalStateException(
                "Could not sync your latest progress. Check internet and try again before signing out.",
                error
            )
        }
    }

    fun reportBootstrapFailure(error: Throwable? = null) {
        val detail = error?.localizedMessage?.takeIf { it.isNotBlank() }
        _syncStatus.value = if (detail == null) {
            "Cloud sync failed. Your local progress is still on this device; check internet and retry."
        } else {
            "Cloud sync failed: $detail"
        }
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
        val cloudShopPreferences = cloud["shopPreferences"].asMap().orEmpty()
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
            cloudSessions.isNotEmpty() || cloudShopPreferences.isNotEmpty() || cloudProfiles.any {
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
            val mergedEarned = maxOf(
                local.totalXpEarned,
                remote.totalXpEarned,
                local.totalXP + local.totalXpSpent,
                remote.totalXP + remote.totalXpSpent
            )
            val mergedSpent = maxOf(local.totalXpSpent, remote.totalXpSpent)
            levelState.copy(
                id = local.id,
                streakDays = maxOf(local.streakDays, remote.streakDays),
                totalStudyMinutes = maxOf(local.totalStudyMinutes, remote.totalStudyMinutes),
                totalXP = (mergedEarned - mergedSpent).coerceAtLeast(0),
                totalXpSpent = mergedSpent,
                totalXpEarned = mergedEarned,
                currentLevel = maxOf(local.currentLevel, remote.currentLevel),
                dailyGoalMinutes = local.dailyGoalMinutes,
                lastActiveDate = selectStreakDate(local, remote)
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
            themePreferences.restoreCloudSyncPreferences(cloudShopPreferences)
            ownershipPrefs.edit().putBoolean(ownerKey(uid), true).apply()
            cloudUploadUid = uid
            lastSuccessfulSyncMillis = System.currentTimeMillis()
            _syncStatus.value = "Synced just now"
            return "Cloud progress restored to this empty device. Automatic cloud sync is enabled."
        }
        cloudUploadUid = null
        return "The cloud document has no progress records. Automatic sync remains paused until an explicit account-link action is completed."
    }

    /**
     * Explicit conflict resolution: merge cloud records into this account's local database,
     * keep local settings when preferences differ, then upload the merged snapshot.
     * Cloud records receive fresh Room IDs and logical duplicates are skipped, so existing
     * local rows are never replaced by a remote row with a colliding numeric ID.
     */
    suspend fun mergeCloudIntoLocalAndUpload(): String {
        val user = auth.currentUser
            ?: throw IllegalStateException("Sign in before resolving progress.")
        if (!user.isEmailVerified) throw IllegalStateException("Verify your email before syncing progress.")
        val uid = user.uid
        cloudUploadUid = null
        ownershipPrefs.edit().remove(ownerKey(uid)).apply()
        pendingUpload?.cancel()
        pendingUpload = null

        val ref = firestore.collection("users").document(uid)
            .collection("progress").document("current")
        val cloud = ref.get().asSuspendResult().data
            ?: throw IllegalStateException("No cloud backup exists for this account. Nothing was changed.")

        val localPlans = database.studyPlanDao().getAllForBackup()
        val localExams = database.examDao().getAllForBackup()
        val localSessions = database.sessionLogDao().getAllForBackup()
        val localProfiles = database.userProfileDao().getAllForBackup()
        val cloudPlans = (cloud["studyPlans"] as? List<*>)?.mapNotNull { it.asMap()?.toStudyPlan() }.orEmpty()
        val cloudExams = (cloud["exams"] as? List<*>)?.mapNotNull { it.asMap()?.toExam() }.orEmpty()
        val cloudSessions = (cloud["sessionLogs"] as? List<*>)?.mapNotNull { it.asMap()?.toSession() }.orEmpty()
        val cloudProfiles = (cloud["profiles"] as? List<*>)?.mapNotNull { it.asMap()?.toProfile() }.orEmpty()

        if (auth.currentUser?.uid != uid || auth.currentUser?.isEmailVerified != true) {
            throw IllegalStateException("Account changed during merge. Please sign in again.")
        }

        val newPlans = cloudPlans.filter { remote ->
            localPlans.none { it.createdAt == remote.createdAt && it.title == remote.title && it.subject == remote.subject }
        }.map { it.copy(id = 0L, isTimerRunning = false, endAtElapsedRealtime = 0L, endAtWallClockMillis = 0L) }
        val newExams = cloudExams.filter { remote ->
            localExams.none { it.createdAt == remote.createdAt && it.subject == remote.subject && it.examDate == remote.examDate }
        }.map { it.copy(id = 0L) }
        val newSessions = cloudSessions.filter { remote ->
            localSessions.none {
                it.timestamp == remote.timestamp && it.subject == remote.subject &&
                    it.chapter == remote.chapter && it.durationMinutes == remote.durationMinutes &&
                    it.mode == remote.mode && it.xpEarned == remote.xpEarned
            }
        }.map { it.copy(id = 0L) }

        val localProfile = localProfiles.firstOrNull()
        val remoteProfile = cloudProfiles.firstOrNull()
        val mergedProfile = when {
            localProfile == null -> remoteProfile?.copy(id = 1)
            remoteProfile == null -> localProfile
            else -> {
                val levelState = when {
                    remoteProfile.currentLevel > localProfile.currentLevel -> remoteProfile
                    localProfile.currentLevel > remoteProfile.currentLevel -> localProfile
                    remoteProfile.levelStartedAtMillis > localProfile.levelStartedAtMillis -> remoteProfile
                    else -> localProfile
                }
                val mergedEarned = maxOf(
                    localProfile.totalXpEarned,
                    remoteProfile.totalXpEarned,
                    localProfile.totalXP + localProfile.totalXpSpent,
                    remoteProfile.totalXP + remoteProfile.totalXpSpent
                )
                val mergedSpent = maxOf(localProfile.totalXpSpent, remoteProfile.totalXpSpent)
                levelState.copy(
                    id = 1,
                    streakDays = maxOf(localProfile.streakDays, remoteProfile.streakDays),
                    totalStudyMinutes = maxOf(localProfile.totalStudyMinutes, remoteProfile.totalStudyMinutes),
                    totalXP = (mergedEarned - mergedSpent).coerceAtLeast(0),
                    totalXpSpent = mergedSpent,
                    totalXpEarned = mergedEarned,
                    currentLevel = maxOf(localProfile.currentLevel, remoteProfile.currentLevel),
                    dailyGoalMinutes = localProfile.dailyGoalMinutes
                )
            }
        }

        database.withTransaction {
            if (auth.currentUser?.uid != uid || auth.currentUser?.isEmailVerified != true) {
                throw IllegalStateException("Account changed during merge. No local changes were committed.")
            }
            database.studyPlanDao().insertAllForRestore(newPlans)
            database.examDao().insertAllForRestore(newExams)
            database.sessionLogDao().insertAllForRestore(newSessions)
            mergedProfile?.let { database.userProfileDao().insertOrUpdate(it) }
        }

        if (auth.currentUser?.uid != uid || auth.currentUser?.isEmailVerified != true) {
            throw IllegalStateException("Account changed after local merge. Local progress remains on this device; cloud upload was not enabled.")
        }

        // Preference conflicts are deliberately not auto-merged: preserve this device's
        // settings and make that policy clear in the confirmation UI. Temporarily authorize
        // only this explicit upload; revoke it again if the upload fails.
        cloudUploadUid = uid
        try {
            uploadLocalSnapshot()
        } catch (error: Exception) {
            cloudUploadUid = null
            throw error
        }
        if (auth.currentUser?.uid != uid || auth.currentUser?.isEmailVerified != true) {
            cloudUploadUid = null
            throw IllegalStateException("Account changed during cloud upload. Local merged progress is preserved; please retry sync.")
        }
        ownershipPrefs.edit().putBoolean(ownerKey(uid), true).apply()
        cloudUploadUid = uid
        lastSuccessfulSyncMillis = System.currentTimeMillis()
        _syncStatus.value = "Synced just now"
        return "Merge complete: added ${newPlans.size} plans, ${newExams.size} exams and ${newSessions.size} study sessions. Progress totals were reconciled. This device's theme/settings were kept, and the merged progress was uploaded."
    }

    private fun selectStreakDate(local: UserProfileEntity, remote: UserProfileEntity): String {
        return when {
            remote.streakDays > local.streakDays && remote.lastActiveDate.isNotBlank() ->
                remote.lastActiveDate
            local.streakDays > remote.streakDays && local.lastActiveDate.isNotBlank() ->
                local.lastActiveDate
            remote.lastActiveDate.isNotBlank() -> remote.lastActiveDate
            else -> local.lastActiveDate
        }
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
     * Runs on every verified sign-in/app start. Existing cloud data is restored to a
     * truly empty account database. If no cloud document exists, safely create the
     * first snapshot automatically instead of requiring the user to find a backup button.
     */
    suspend fun bootstrapOnVerifiedSignIn(): String {
        val user = auth.currentUser
            ?: throw IllegalStateException("Sign in before syncing progress.")
        if (!user.isEmailVerified) throw IllegalStateException("Verify your email before syncing progress.")
        val uid = user.uid
        val ref = firestore.collection("users").document(uid)
            .collection("progress").document("current")
        val remote = ref.get().asSuspendResult()
        if (auth.currentUser?.uid != uid || auth.currentUser?.isEmailVerified != true) {
            throw IllegalStateException("Account changed during cloud sync. Please retry.")
        }
        if (!remote.exists()) {
            // createInitialCloudBackup uses a Firestore transaction and refuses to
            // overwrite a document created by a concurrent device.
            return createInitialCloudBackup()
        }

        val localPlans = database.studyPlanDao().getAllForBackup()
        val localExams = database.examDao().getAllForBackup()
        val localSessions = database.sessionLogDao().getAllForBackup()
        val localProfiles = database.userProfileDao().getAllForBackup()
        val hasLocal = localPlans.isNotEmpty() || localExams.isNotEmpty() ||
            localSessions.isNotEmpty() || localProfiles.any {
                it.totalStudyMinutes > 0 || it.totalXP > 0 || it.streakDays > 0
            }
        if (!hasLocal) {
            val result = restoreIfLocalEmpty()
            // An existing but empty cloud snapshot is still a safe account-owned
            // starting point; enable uploads only after confirming this same UID.
            if (auth.currentUser?.uid == uid && auth.currentUser?.isEmailVerified == true &&
                result.startsWith("The cloud document has no progress records.")
            ) {
                ownershipPrefs.edit().putBoolean(ownerKey(uid), true).apply()
                cloudUploadUid = uid
                _syncStatus.value = "Sync ready"
                return "Cloud account connected. Automatic sync is enabled."
            }
            return result
        }

        if (ownershipPrefs.getBoolean(ownerKey(uid), false) && auth.currentUser?.uid == uid) {
            cloudUploadUid = uid
            scheduleSnapshotUpload()
            return "Cloud sync connected. Checking latest progress…"
        }

        cloudUploadUid = null
        _syncStatus.value = "Progress needs reconciliation"
        return "This device and cloud both contain progress. Nothing was overwritten. Use Restore only on an empty device, or explicitly resolve the copies before enabling sync."
    }

    /** Returns true when the account already has meaningful cloud-owned progress. */
    suspend fun hasMeaningfulCloudProgress(): Boolean {
        val user = auth.currentUser
            ?: throw IllegalStateException("Sign in before checking cloud progress.")
        if (!user.isEmailVerified) throw IllegalStateException("Verify your email before checking cloud progress.")
        val uid = user.uid
        val ref = firestore.collection("users").document(uid)
            .collection("progress").document("current")
        val data = ref.get().asSuspendResult().data ?: return false
        if (auth.currentUser?.uid != uid || auth.currentUser?.isEmailVerified != true) {
            throw IllegalStateException("Account changed during cloud progress check. Please retry.")
        }
        val plans = (data["studyPlans"] as? List<*>)?.filterNotNull().orEmpty()
        val exams = (data["exams"] as? List<*>)?.filterNotNull().orEmpty()
        val sessions = (data["sessionLogs"] as? List<*>)?.filterNotNull().orEmpty()
        val profiles = (data["profiles"] as? List<*>)?.filterNotNull().orEmpty()
        val preferences = data["shopPreferences"].asMap().orEmpty()
        return plans.isNotEmpty() || exams.isNotEmpty() || sessions.isNotEmpty() ||
            preferences.isNotEmpty() || profiles.isNotEmpty()
    }

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
            "shopPreferences" to themePreferences.exportCloudSyncPreferences(),
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

    /**
     * Uploads an imported guest snapshot only when the account's cloud document
     * is empty or absent. This prevents guest migration from overwriting an
     * existing account's cloud progress.
     */
    suspend fun uploadImportedGuestSnapshot(): String {
        val user = auth.currentUser
            ?: throw IllegalStateException("Sign in before syncing progress.")
        if (!user.isEmailVerified) {
            throw IllegalStateException("Verify your email before syncing progress.")
        }
        val uid = user.uid
        val ref = firestore.collection("users").document(uid)
            .collection("progress").document("current")
        val existing = ref.get().asSuspendResult().data
        if (auth.currentUser?.uid != uid || auth.currentUser?.isEmailVerified != true) {
            throw IllegalStateException("Account changed during guest import. Please retry.")
        }

        if (existing != null) {
            val cloudPlans = (existing["studyPlans"] as? List<*>)?.filterNotNull().orEmpty()
            val cloudExams = (existing["exams"] as? List<*>)?.filterNotNull().orEmpty()
            val cloudSessions = (existing["sessionLogs"] as? List<*>)?.filterNotNull().orEmpty()
            val cloudProfiles = (existing["profiles"] as? List<*>)?.filterNotNull().orEmpty()
            val cloudPreferences = existing["shopPreferences"].asMap().orEmpty()
            val cloudHasMeaningfulData =
                cloudPlans.isNotEmpty() || cloudExams.isNotEmpty() || cloudSessions.isNotEmpty() ||
                    cloudPreferences.isNotEmpty() || cloudProfiles.isNotEmpty()
            if (cloudHasMeaningfulData) {
                throw IllegalStateException(
                    "This account already has cloud progress. Guest import was cancelled to protect the existing cloud data."
                )
            }
        }

        ownershipPrefs.edit().putBoolean(ownerKey(uid), true).apply()
        cloudUploadUid = uid
        return try {
            uploadLocalSnapshot()
            lastSuccessfulSyncMillis = System.currentTimeMillis()
            _syncStatus.value = "Synced just now"
            "Guest progress imported and synced to the cloud."
        } catch (error: Exception) {
            ownershipPrefs.edit().remove(ownerKey(uid)).apply()
            cloudUploadUid = null
            throw error
        }
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

        val ref = firestore.collection("users").document(uid)
            .collection("progress").document("current")

        // Safe multi-device sync: never replace a parallel device snapshot with a
        // stale full snapshot. Firestore transaction retries when the document changes.
        firestore.runTransaction { transaction ->
            if (auth.currentUser?.uid != uid ||
                auth.currentUser?.isEmailVerified != true ||
                cloudUploadUid != uid
            ) {
                throw IllegalStateException("Account changed during sync. Please retry.")
            }

            val remote = transaction.get(ref).data.orEmpty()
            val remotePlans = (remote["studyPlans"] as? List<*>)?.mapNotNull { it.asMap()?.toStudyPlan() }.orEmpty()
            val remoteExams = (remote["exams"] as? List<*>)?.mapNotNull { it.asMap()?.toExam() }.orEmpty()
            val remoteSessions = (remote["sessionLogs"] as? List<*>)?.mapNotNull { it.asMap()?.toSession() }.orEmpty()
            val remoteProfiles = (remote["profiles"] as? List<*>)?.mapNotNull { it.asMap()?.toProfile() }.orEmpty()
            val localPrefs = themePreferences.exportCloudSyncPreferences()
            val remotePrefs = remote["shopPreferences"].asMap().orEmpty()

            val mergedPlans = (remotePlans + plans).groupBy {
                "${it.createdAt}|${it.title}|${it.subject}"
            }.values.map { candidates ->
                candidates.maxByOrNull { it.lastUpdated } ?: candidates.first()
            }
            val mergedExams = (remoteExams + exams).groupBy {
                "${it.createdAt}|${it.subject}|${it.examDate}"
            }.values.map { candidates ->
                candidates.maxByOrNull { it.createdAt } ?: candidates.first()
            }
            val mergedSessions = (remoteSessions + sessions).distinctBy {
                "${it.timestamp}|${it.subject}|${it.chapter}|${it.durationMinutes}|${it.mode}|${it.xpEarned}"
            }
            val mergedProfile = mergeProfiles(remoteProfiles.firstOrNull(), profiles.firstOrNull())
            val mergedPrefs = remotePrefs.toMutableMap().apply { putAll(localPrefs) }

            val mergedSnapshot = hashMapOf<String, Any>(
                "schemaVersion" to maxOf((remote["schemaVersion"] as? Number)?.toInt() ?: 1, 1),
                "studyPlans" to mergedPlans.map { it.toCloudMap() },
                "exams" to mergedExams.map { it.toCloudMap() },
                "sessionLogs" to mergedSessions.map { it.toCloudMap() },
                "profiles" to listOfNotNull(mergedProfile?.toCloudMap()),
                "shopPreferences" to mergedPrefs,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            transaction.set(ref, mergedSnapshot)
            null
        }.asSuspendResult()
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

    private fun mergeProfiles(
        remote: UserProfileEntity?,
        local: UserProfileEntity?
    ): UserProfileEntity? {
        if (remote == null) return local
        if (local == null) return remote
        val mergedEarned = maxOf(
            local.totalXpEarned,
            remote.totalXpEarned,
            local.totalXP + local.totalXpSpent,
            remote.totalXP + remote.totalXpSpent
        )
        val mergedSpent = maxOf(local.totalXpSpent, remote.totalXpSpent)
        return local.copy(
            id = local.id,
            streakDays = maxOf(local.streakDays, remote.streakDays),
            totalStudyMinutes = maxOf(local.totalStudyMinutes, remote.totalStudyMinutes),
            totalXP = (mergedEarned - mergedSpent).coerceAtLeast(0),
            totalXpSpent = mergedSpent,
            totalXpEarned = mergedEarned,
            currentLevel = maxOf(local.currentLevel, remote.currentLevel),
            levelStartStudyMinutes = maxOf(local.levelStartStudyMinutes, remote.levelStartStudyMinutes),
            levelStartXpEarned = maxOf(local.levelStartXpEarned, remote.levelStartXpEarned),
            levelStartXpSpent = maxOf(local.levelStartXpSpent, remote.levelStartXpSpent),
            levelStartedAtMillis = maxOf(local.levelStartedAtMillis, remote.levelStartedAtMillis),
            dailyGoalMinutes = local.dailyGoalMinutes,
            themePreset = local.themePreset.ifBlank { remote.themePreset },
            lastActiveDate = maxOf(local.lastActiveDate, remote.lastActiveDate)
        )
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

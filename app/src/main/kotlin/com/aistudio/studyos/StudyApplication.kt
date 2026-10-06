package com.aistudio.studyos

import android.app.Application
import com.aistudio.studyos.data.local.StudyDatabase
import com.aistudio.studyos.data.local.ThemePreferences
import com.aistudio.studyos.data.repository.StudyRepository
import com.aistudio.studyos.data.repository.FirebaseProgressSyncRepository
import com.aistudio.studyos.data.repository.LegacyProgressImportRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StudyApplication : Application() {
    companion object {
        lateinit var instance: StudyApplication
            private set
    }

    /** Legacy/guest store; authenticated screens must use databaseFor(uid). */
    val database: StudyDatabase by lazy { StudyDatabase.getInstance(this) }
    val themePreferences: ThemePreferences by lazy { ThemePreferences(this) }
    val repository: StudyRepository by lazy { StudyRepository(database, themePreferences) }
    val cloudProgressSync: FirebaseProgressSyncRepository by lazy { FirebaseProgressSyncRepository(this, database) }
    private val accountCloudSyncs = mutableMapOf<String, FirebaseProgressSyncRepository>()
    private val bootstrapInFlight = mutableSetOf<String>()
    @Volatile private var activeCloudUid: String? = null
    private val appScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    @Synchronized
    fun databaseFor(uid: String?): StudyDatabase =
        if (uid.isNullOrBlank()) database else StudyDatabase.getAccountInstance(this, uid)

    fun preferencesFor(uid: String?): ThemePreferences =
        if (uid.isNullOrBlank()) themePreferences
        else ThemePreferences(this, ThemePreferences.accountStorageName(uid))

    fun repositoryFor(uid: String?): StudyRepository =
        if (uid.isNullOrBlank()) repository
        else StudyRepository(databaseFor(uid), preferencesFor(uid))

    @Synchronized
    fun cloudSyncFor(uid: String): FirebaseProgressSyncRepository {
        require(uid.isNotBlank()) { "A verified account UID is required for cloud sync." }
        return accountCloudSyncs.getOrPut(uid) {
            FirebaseProgressSyncRepository(this, databaseFor(uid), preferencesFor(uid))
        }
    }

    /**
     * Offer guest migration only when the target account is locally empty and its cloud
     * snapshot is also empty. Existing account data is never silently replaced.
     */
    suspend fun shouldOfferGuestImport(uid: String): Boolean {
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser?.uid != uid || !authUser.isEmailVerified) return false

        val accountDb = databaseFor(uid)
        val localHasProgress =
            accountDb.studyPlanDao().getAllForBackup().isNotEmpty() ||
                accountDb.examDao().getAllForBackup().isNotEmpty() ||
                accountDb.sessionLogDao().getAllForBackup().isNotEmpty() ||
                accountDb.userProfileDao().getAllForBackup().any {
                    it.totalStudyMinutes > 0 || it.totalXP > 0 || it.totalXpEarned > 0 ||
                        it.totalXpSpent > 0 || it.streakDays > 0
                }
        if (localHasProgress) return false

        if (!LegacyProgressImportRepository(this, uid).hasLegacyProgress()) return false
        return !cloudSyncFor(uid).hasMeaningfulCloudProgress()
    }

    /**
     * Explicitly imports the isolated guest store into the UID-scoped account and then
     * uploads the imported snapshot. Guest data is never deleted by this operation.
     */
    suspend fun importGuestProgressToAccount(uid: String): String {
        val authUser = FirebaseAuth.getInstance().currentUser
        require(authUser?.uid == uid && authUser.isEmailVerified) {
            "A verified account is required to import guest progress."
        }
        val sync = cloudSyncFor(uid)
        if (sync.hasMeaningfulCloudProgress()) {
            throw IllegalStateException(
                "This account already has cloud progress. Guest import was cancelled to protect existing data."
            )
        }

        val accountDb = databaseFor(uid)
        val accountAlreadyPopulated =
            accountDb.studyPlanDao().getAllForBackup().isNotEmpty() ||
                accountDb.examDao().getAllForBackup().isNotEmpty() ||
                accountDb.sessionLogDao().getAllForBackup().isNotEmpty() ||
                accountDb.userProfileDao().getAllForBackup().any {
                    it.totalStudyMinutes > 0 || it.totalXP > 0 || it.totalXpEarned > 0 ||
                        it.totalXpSpent > 0 || it.streakDays > 0
                }

        // A previous local import may have succeeded while its cloud upload failed.
        // In that case retry only the upload instead of duplicating guest rows.
        if (accountAlreadyPopulated) {
            sync.uploadImportedGuestSnapshot()
            return "Guest progress was already imported locally and is now synced."
        }

        val summary = LegacyProgressImportRepository(this, uid).importLegacyProgress()
        if (summary.isEmpty) return "No guest progress was found to import."
        sync.uploadImportedGuestSnapshot()
        return "Guest progress imported and synced."
    }

    @Synchronized
    fun activateCloudSync(uid: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (user.uid != uid || !user.isEmailVerified) return
        val sync = cloudSyncFor(uid)
        if (activeCloudUid != uid) {
            sync.startAutomaticUpload(appScope)
            activeCloudUid = uid
        }
        // MainActivity and FirebaseAuth's listener can both announce the same sign-in.
        // Keep only one bootstrap running per UID to avoid competing initial snapshots.
        if (!bootstrapInFlight.add(uid)) return
        appScope.launch {
            try {
                sync.bootstrapOnVerifiedSignIn()
            } catch (error: Exception) {
                sync.reportBootstrapFailure(error)
                android.util.Log.e("StudyOSCloudSync", "Cloud bootstrap failed for the active account", error)
            } finally {
                synchronized(this@StudyApplication) { bootstrapInFlight.remove(uid) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Only UID-scoped databases can participate in cloud sync. The legacy guest
        // database is never attached to an authenticated account's upload observer.
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser?.takeIf { it.isEmailVerified }
            synchronized(this) {
                accountCloudSyncs.forEach { (uid, sync) ->
                    if (uid != user?.uid) sync.stopAutomaticUpload()
                }
                if (user?.uid != activeCloudUid) activeCloudUid = null
                // MainActivity gates activation so guest migration can be explicitly approved first.
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.getCachedRecentLogs()
                database.openHelper.readableDatabase
            } catch (_: Exception) {
            }
        }
    }
}

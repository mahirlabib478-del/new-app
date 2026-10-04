package com.aistudio.studyos

import android.app.Application
import com.aistudio.studyos.data.local.StudyDatabase
import com.aistudio.studyos.data.local.ThemePreferences
import com.aistudio.studyos.data.repository.StudyRepository
import com.aistudio.studyos.data.repository.FirebaseProgressSyncRepository
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
            FirebaseProgressSyncRepository(this, databaseFor(uid))
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
                user?.let { cloudSyncFor(it.uid).startAutomaticUpload(appScope) }
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

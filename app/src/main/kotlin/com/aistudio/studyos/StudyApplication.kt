package com.aistudio.studyos

import android.app.Application
import com.aistudio.studyos.data.local.StudyDatabase
import com.aistudio.studyos.data.local.ThemePreferences
import com.aistudio.studyos.data.repository.StudyRepository
import com.aistudio.studyos.data.repository.FirebaseProgressSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    fun databaseFor(uid: String?): StudyDatabase =
        if (uid.isNullOrBlank()) database else StudyDatabase.getAccountInstance(this, uid)

    fun preferencesFor(uid: String?): ThemePreferences =
        if (uid.isNullOrBlank()) themePreferences
        else ThemePreferences(this, ThemePreferences.accountStorageName(uid))

    fun repositoryFor(uid: String?): StudyRepository =
        if (uid.isNullOrBlank()) repository
        else StudyRepository(databaseFor(uid), preferencesFor(uid))

    fun cloudSyncFor(uid: String): FirebaseProgressSyncRepository =
        FirebaseProgressSyncRepository(this, databaseFor(uid))

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Legacy shared storage must never auto-upload into a signed-in account.
        // Account-scoped cloud sync will be enabled only after its own explicit-link flow is wired.

        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.getCachedRecentLogs()
                database.openHelper.readableDatabase
            } catch (_: Exception) {
            }
        }
    }
}

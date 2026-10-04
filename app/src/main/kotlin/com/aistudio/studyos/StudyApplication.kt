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

    /** Legacy/guest store; authenticated screens must use databaseFor(uid). */\n    val database: StudyDatabase by lazy { StudyDatabase.getInstance(this) }\n\n    fun databaseFor(uid: String?): StudyDatabase =\n        if (uid.isNullOrBlank()) database else StudyDatabase.getAccountInstance(this, uid)\n\n    fun preferencesFor(uid: String?): ThemePreferences =\n        if (uid.isNullOrBlank()) themePreferences\n        else ThemePreferences(this, ThemePreferences.accountStorageName(uid))\n\n    fun repositoryFor(uid: String?): StudyRepository =\n        if (uid.isNullOrBlank()) repository\n        else StudyRepository(databaseFor(uid), preferencesFor(uid))\n\n    fun cloudSyncFor(uid: String): FirebaseProgressSyncRepository =\n        FirebaseProgressSyncRepository(this, databaseFor(uid))
    val themePreferences: ThemePreferences by lazy { ThemePreferences(this) }
    val repository: StudyRepository by lazy { StudyRepository(database, themePreferences) }
    val cloudProgressSync: FirebaseProgressSyncRepository by lazy { FirebaseProgressSyncRepository(this, database) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        cloudProgressSync.startAutomaticUpload(CoroutineScope(Dispatchers.IO))

        // Pre-warm Room database connection and cached data in background immediately
        // so that by the time UI/HomeScreen opens, SQLite is already initialized and fast
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.getCachedRecentLogs()
                database.openHelper.readableDatabase
            } catch (_: Exception) {
            }
        }
    }
}


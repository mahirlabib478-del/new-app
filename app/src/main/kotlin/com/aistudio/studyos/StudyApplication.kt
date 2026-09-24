package com.aistudio.studyos

import android.app.Application
import com.aistudio.studyos.data.local.StudyDatabase
import com.aistudio.studyos.data.local.ThemePreferences
import com.aistudio.studyos.data.repository.StudyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyApplication : Application() {
    companion object {
        lateinit var instance: StudyApplication
            private set
    }

    val database: StudyDatabase by lazy { StudyDatabase.getInstance(this) }
    val themePreferences: ThemePreferences by lazy { ThemePreferences(this) }
    val repository: StudyRepository by lazy { StudyRepository(database, themePreferences) }

    override fun onCreate() {
        super.onCreate()
        instance = this

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


package com.aistudio.studyos

import android.app.Application
import com.aistudio.studyos.data.local.StudyDatabase
import com.aistudio.studyos.data.local.ThemePreferences
import com.aistudio.studyos.data.repository.StudyRepository
import com.aistudio.studyos.service.AdManager
import com.google.android.gms.ads.MobileAds

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

        // Initialize Google Mobile Ads SDK on a background thread
        MobileAds.initialize(this) {
            AdManager.loadInterstitial(this)
        }
    }
}

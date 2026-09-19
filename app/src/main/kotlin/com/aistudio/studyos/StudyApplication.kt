package com.aistudio.studyos

import android.app.Application
import com.aistudio.studyos.data.local.StudyDatabase
import com.aistudio.studyos.data.repository.StudyRepository

class StudyApplication : Application() {
    val database: StudyDatabase by lazy { StudyDatabase.getInstance(this) }
    val repository: StudyRepository by lazy { StudyRepository(database) }

    override fun onCreate() {
        super.onCreate()
    }
}

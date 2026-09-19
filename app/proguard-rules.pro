# ProGuard / R8 rules for StudyOS

# Keep Room database models and DAOs
-keep class com.aistudio.studyos.data.local.** { *; }
-dontwarn androidx.room.**

# Keep Update model entities and JSON models
-keep class com.aistudio.studyos.data.update.** { *; }

# Keep ViewModel
-keep class com.aistudio.studyos.ui.viewmodel.** { *; }

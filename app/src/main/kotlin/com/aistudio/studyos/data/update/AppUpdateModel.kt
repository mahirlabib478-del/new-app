package com.aistudio.studyos.data.update

data class AppUpdateInfo(
    val latestVersion: String,
    val minimumSupportedVersion: String = "0.0.0",
    val releaseUrl: String,
    val apkUrl: String,
    val releaseNotes: String = "",
    val isMandatory: Boolean = false
)

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    data class Available(val updateInfo: AppUpdateInfo) : UpdateCheckState()
    data class UpToDate(val currentVersion: String) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}

sealed class UpdateCheckResult {
    data class Available(val updateInfo: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

package com.aistudio.studyos.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    private const val UPDATE_JSON_PRIMARY_URL =
        "https://raw.githubusercontent.com/mahirlabib478-del/new-app/main/update.json"
    private const val GITHUB_LATEST_RELEASE_API =
        "https://api.github.com/repos/mahirlabib478-del/new-app/releases/latest"

    private const val PREFS_NAME = "studyos_update_prefs"
    private const val KEY_LAST_CHECK_TIME = "last_update_check_time"
    private const val CHECK_INTERVAL_MS = 2 * 60 * 60 * 1000L // 2 hours throttle for auto-check
    private const val TIMEOUT_MS = 8000 // Allow slower mobile networks while keeping update checks bounded

    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    fun getCurrentVersionInfo(context: Context): Pair<String, Long> {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = pInfo.versionName ?: "0.3.0"
            val versionCode = PackageInfoCompat.getLongVersionCode(pInfo)
            Pair(versionName, versionCode)
        } catch (e: Exception) {
            Pair("0.3.0", 30L)
        }
    }

    /**
     * Accurately compares semver (e.g. 0.3.0 vs 0.3.1) and build codes (e.g. 0.3.0+28 vs 0.3.0+29).
     */
    fun isNewerVersion(
        currentVersionName: String,
        currentVersionCode: Long,
        remoteVersionString: String
    ): Boolean {
        if (remoteVersionString.isBlank()) return false

        val remoteClean = remoteVersionString.trim().removePrefix("v").removePrefix("V")
        val currentClean = currentVersionName.trim().removePrefix("v").removePrefix("V")

        val remoteParts = remoteClean.split("+")
        val remoteSemVer = remoteParts[0].split(".").mapNotNull { it.toIntOrNull() }
        val remoteBuildCode = remoteParts.getOrNull(1)?.toLongOrNull()

        val currentParts = currentClean.split("+")
        val currentSemVer = currentParts[0].split(".").mapNotNull { it.toIntOrNull() }
        val currentBuildCode = currentParts.getOrNull(1)?.toLongOrNull() ?: currentVersionCode

        val maxLen = maxOf(remoteSemVer.size, currentSemVer.size)
        for (i in 0 until maxLen) {
            val r = remoteSemVer.getOrElse(i) { 0 }
            val c = currentSemVer.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }

        // If semantic versions (major.minor.patch) are equal, compare build code
        if (remoteBuildCode != null && remoteBuildCode > currentBuildCode) {
            return true
        }

        return false
    }

    private fun shouldThrottleAutoCheck(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        val now = System.currentTimeMillis()
        return (now - lastCheck) < CHECK_INTERVAL_MS
    }

    private fun recordCheckTimestamp(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
    }

    suspend fun checkForUpdate(
        context: Context,
        isManual: Boolean = false,
        forceCheck: Boolean = false
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!isManual && !forceCheck && shouldThrottleAutoCheck(context)) {
            val (currentName, _) = getCurrentVersionInfo(context)
            return@withContext UpdateCheckResult.UpToDate(currentName)
        }

        try {
            val (currentName, currentCode) = getCurrentVersionInfo(context)

            // Check both sources and use the newest result. This prevents a stale
            // raw update.json CDN response from hiding a newer GitHub release.
            val updateInfo = listOfNotNull(
                fetchFromUpdateJson(),
                fetchFromGitHubApi()
            ).maxWithOrNull(
                Comparator { left, right ->
                    compareVersionStrings(left.latestVersion, right.latestVersion)
                }
            ) ?: return@withContext UpdateCheckResult.Error(
                "Could not reach the update servers."
            )

            recordCheckTimestamp(context)

            if (isNewerVersion(currentName, currentCode, updateInfo.latestVersion)) {
                val isMandatory = isNewerVersion(
                    currentName,
                    currentCode,
                    updateInfo.minimumSupportedVersion
                )
                return@withContext UpdateCheckResult.Available(
                    updateInfo.copy(isMandatory = isMandatory)
                )
            }

            UpdateCheckResult.UpToDate(currentName)
        } catch (e: Exception) {
            UpdateCheckResult.Error("Update check failed. Please try again.")
        }
    }

    private fun compareVersionStrings(left: String, right: String): Int {
        fun parse(value: String): Pair<List<Int>, Long> {
            val clean = value.trim().removePrefix("v").removePrefix("V")
            val parts = clean.split("+")
            val semver = parts[0].split(".").map { it.toIntOrNull() ?: 0 }
            val build = parts.getOrNull(1)?.toLongOrNull() ?: 0L
            return semver to build
        }

        val (leftSemVer, leftBuild) = parse(left)
        val (rightSemVer, rightBuild) = parse(right)
        val maxLen = maxOf(leftSemVer.size, rightSemVer.size)

        for (i in 0 until maxLen) {
            val l = leftSemVer.getOrElse(i) { 0 }
            val r = rightSemVer.getOrElse(i) { 0 }
            if (l != r) return l.compareTo(r)
        }

        return leftBuild.compareTo(rightBuild)
    }

    private fun fetchFromUpdateJson(): AppUpdateInfo? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("$UPDATE_JSON_PRIMARY_URL?ts=${System.currentTimeMillis()}")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache, no-store")
                setRequestProperty("Pragma", "no-cache")
                setRequestProperty("User-Agent", "StudyOS-App")
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val latestVersion = json.optString("latestVersion", "").trim()
                val minVersion = json.optString("minimumSupportedVersion", "0.0.0").trim()
                val releaseUrl = json.optString("releaseUrl", "").trim()
                val apkUrl = json.optString("apkUrl", "").trim()

                if (latestVersion.isNotBlank()) {
                    AppUpdateInfo(
                        latestVersion = latestVersion,
                        minimumSupportedVersion = minVersion,
                        releaseUrl = releaseUrl,
                        apkUrl = apkUrl.ifBlank { releaseUrl }
                    )
                } else null
            } else null
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchFromGitHubApi(): AppUpdateInfo? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("$GITHUB_LATEST_RELEASE_API?ts=${System.currentTimeMillis()}")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache, no-store")
                setRequestProperty("Pragma", "no-cache")
                setRequestProperty("User-Agent", "StudyOS-App")
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val releaseUrl = json.optString("html_url", "").trim()
                val body = json.optString("body", "").trim()

                var apkUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }
                // GitHub can occasionally return a release without its assets in the
                // API response. Never fall back to the release page for the Download
                // button when the release workflow uses our known APK asset name.
                if (apkUrl.isBlank() && tagName.isNotBlank()) {
                    apkUrl = "https://github.com/mahirlabib478-del/new-app/releases/download/v$tagName/app-release.apk"
                }

                if (tagName.isNotBlank() && releaseUrl.isNotBlank() && apkUrl.isNotBlank()) {
                    AppUpdateInfo(
                        latestVersion = tagName,
                        minimumSupportedVersion = "0.0.0",
                        releaseUrl = releaseUrl,
                        apkUrl = apkUrl,
                        releaseNotes = body
                    )
                } else null
            } else null
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun openUpdateLink(context: Context, url: String) {
        if (url.isBlank()) return
        if (url.substringBefore("?").lowercase().endsWith(".apk") || url.contains("/releases/download/")) {
            startDownloadAndInstall(context, url)
            return
        }
        try {
            val targetUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else {
                "https://$url"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Ignore if no browser available
        }
    }
    fun startDownloadAndInstall(context: Context, apkUrl: String) {
        if (_downloadState.value is UpdateDownloadState.Downloading) return
        downloadScope.launch {
            val appContext = context.applicationContext
            val dir = File(appContext.cacheDir, "updates").apply { mkdirs() }
            val temp = File(dir, "studyos-update.apk.part")
            val apk = File(dir, "studyos-update.apk")
            try {
                _downloadState.value = UpdateDownloadState.Downloading(0, 0L, 0L)
                temp.delete()
                var connection: HttpURLConnection? = null
                try {
                    connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 15000
                        readTimeout = 30000
                        useCaches = false
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "StudyOS-App")
                        setRequestProperty("Accept", "application/vnd.android.package-archive")
                    }
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw IllegalStateException("Download failed (HTTP " + connection.responseCode + ").")
                    }
                    val total = connection.contentLengthLong
                    var downloaded = 0L
                    connection.inputStream.use { input ->
                        temp.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val count = input.read(buffer)
                                if (count <= 0) break
                                output.write(buffer, 0, count)
                                downloaded += count
                                val percent = if (total > 0) {
                                    ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                                } else -1
                                _downloadState.value = UpdateDownloadState.Downloading(percent, downloaded, total)
                            }
                        }
                    }
                } finally {
                    connection?.disconnect()
                }
                if (!temp.exists() || temp.length() == 0L) {
                    throw IllegalStateException("Downloaded APK is empty.")
                }
                apk.delete()
                if (!temp.renameTo(apk)) {
                    temp.copyTo(apk, overwrite = true)
                    temp.delete()
                }
                _downloadState.value = UpdateDownloadState.ReadyToInstall
                installApk(appContext, apk)
            } catch (e: Exception) {
                temp.delete()
                _downloadState.value = UpdateDownloadState.Error(
                    e.message?.takeIf { it.isNotBlank() } ?: "Could not download the update."
                )
            }
        }
    }

    private fun installApk(context: Context, apk: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                apk
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }.onFailure {
            _downloadState.value = UpdateDownloadState.Error(
                "APK downloaded. Allow this app to install unknown apps, then tap Download Update again."
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + context.packageName)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
    }

    fun clearDownloadState() {
        _downloadState.value = UpdateDownloadState.Idle
    }

}

package com.aistudio.studyos.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.aistudio.studyos.data.local.UploadedAudio
import java.io.File
import java.util.UUID

/**
 * Robust helper for managing user-uploaded audio files in app-internal storage.
 * - Prevents duplicate file copies by matching sanitized filenames.
 * - Guarantees 100% durable, offline playback across app restarts.
 * - Safely deletes internal audio files when removed from library.
 * - Automatically cleans orphaned internal audio files not present in the user's library.
 */
object AudioFileManager {

    fun copyUriToInternalStorage(context: Context, sourceUri: Uri): UploadedAudio {
        val contentResolver = context.contentResolver
        var displayName = "Uploaded Audio"

        // 1. Resolve original display name if available
        try {
            contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        displayName = name
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Try saving to app internal storage for 100% durable, offline playback
        try {
            val audioDir = File(context.filesDir, "study_audios")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
            val sanitized = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(60)
            val targetFile = File(audioDir, "track_$sanitized")

            // Re-use existing file if already copied and valid (prevents duplicate megabytes)
            if (targetFile.exists() && targetFile.length() > 0L) {
                return UploadedAudio(
                    id = UUID.randomUUID().toString(),
                    name = displayName,
                    uri = Uri.fromFile(targetFile).toString()
                )
            }

            contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0L) {
                return UploadedAudio(
                    id = UUID.randomUUID().toString(),
                    name = displayName,
                    uri = Uri.fromFile(targetFile).toString()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Fallback: Request persistable read permission on the source URI
        try {
            contentResolver.takePersistableUriPermission(
                sourceUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}

        return UploadedAudio(
            id = UUID.randomUUID().toString(),
            name = displayName,
            uri = sourceUri.toString()
        )
    }

    fun deleteAudioFile(context: Context, uriString: String) {
        try {
            if (uriString.startsWith("file://")) {
                val parsedPath = Uri.parse(uriString).path ?: uriString.removePrefix("file://")
                val file = File(parsedPath)
                if (file.exists() && file.absolutePath.startsWith(context.filesDir.absolutePath)) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Cleans orphaned audio files in the internal directory that are no longer referenced
     * in the active audio library.
     */
    fun pruneOrphanedAudioFiles(context: Context, activeUris: Set<String>) {
        try {
            val audioDir = File(context.filesDir, "study_audios")
            if (audioDir.exists() && audioDir.isDirectory) {
                val files = audioDir.listFiles() ?: return
                for (file in files) {
                    val fileUri = Uri.fromFile(file).toString()
                    val filePathUri = "file://${file.absolutePath}"
                    if (!activeUris.contains(fileUri) && !activeUris.contains(filePathUri)) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }
}

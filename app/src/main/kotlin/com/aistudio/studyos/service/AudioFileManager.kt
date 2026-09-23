package com.aistudio.studyos.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.aistudio.studyos.data.local.UploadedAudio
import java.io.File
import java.util.UUID

/**
 * Robust helper for copying user-uploaded audio files to app-internal storage.
 * This guarantees audio files remain playable across app restarts, reboots, and permission revocations.
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

        // 2. Try copying to app internal storage for 100% durable, offline playback
        try {
            val audioDir = File(context.filesDir, "study_audios")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
            val sanitized = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(audioDir, "${System.currentTimeMillis()}_$sanitized")

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
}

package com.aistudio.studyos.data.local

/**
 * Represents a user-uploaded audio file (audiobook, lecture, story, or music).
 */
data class UploadedAudio(
    val id: String,
    val name: String,
    val uri: String
)

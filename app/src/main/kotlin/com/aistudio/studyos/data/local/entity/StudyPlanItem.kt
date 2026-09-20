package com.aistudio.studyos.data.local.entity

import java.util.Base64

data class StudyPlanItem(
    val subject: String,
    val topic: String,
    val minutes: Int
)

object StudyPlanItemCodec {
    fun encode(items: List<StudyPlanItem>): String =
        items.joinToString("\n") { item ->
            listOf(item.subject, item.topic, item.minutes.toString())
                .joinToString("|") { value ->
                    Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
                }
        }

    fun decode(raw: String): List<StudyPlanItem> =
        raw.lineSequence().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size != 3) return@mapNotNull null
            runCatching {
                StudyPlanItem(
                    subject = String(Base64.getDecoder().decode(parts[0]), Charsets.UTF_8),
                    topic = String(Base64.getDecoder().decode(parts[1]), Charsets.UTF_8),
                    minutes = parts[2].toInt().coerceIn(1, 720)
                )
            }.getOrNull()
        }.toList()

    fun decodeStrict(raw: String): List<StudyPlanItem>? {
        if (raw.isBlank()) return emptyList()
        val nonBlankLines = raw.lineSequence().filter { it.isNotBlank() }.toList()
        val decoded = decode(raw)
        return if (decoded.size == nonBlankLines.size) decoded else null
    }
}

package com.aistudio.studyos.data.repository

/**
 * Pure policy gate for a future legacy-progress import.
 *
 * This does not move data. Callers must keep legacy records untouched until a
 * separately implemented, transactional import has been verified.
 */
object LegacyProgressImportPolicy {
    enum class Decision {
        NO_LEGACY_DATA,
        SIGN_IN_REQUIRED,
        EXPLICIT_CONFIRMATION_REQUIRED,
        TARGET_ALREADY_HAS_DATA,
        IMPORT_ALLOWED
    }

    fun decide(
        targetUid: String?,
        hasLegacyData: Boolean,
        targetAlreadyHasData: Boolean,
        userConfirmedImport: Boolean
    ): Decision {
        if (!hasLegacyData) return Decision.NO_LEGACY_DATA
        if (targetUid.isNullOrBlank()) return Decision.SIGN_IN_REQUIRED
        if (targetAlreadyHasData) return Decision.TARGET_ALREADY_HAS_DATA
        if (!userConfirmedImport) return Decision.EXPLICIT_CONFIRMATION_REQUIRED
        return Decision.IMPORT_ALLOWED
    }
}

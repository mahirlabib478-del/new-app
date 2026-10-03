package com.aistudio.studyos.data.repository

/**
 * Pure routing policy for future account-scoped local storage.
 *
 * It selects visibility only; it never copies, deletes, or migrates data.
 * A missing account namespace must remain empty rather than falling back to
 * another account's or the legacy device-local dataset.
 */
object AccountDataIsolationPolicy {
    enum class Dataset {
        SIGNED_OUT,
        ACCOUNT_SCOPED,
        EMPTY_ACCOUNT_SCOPE,
        LEGACY_REQUIRES_EXPLICIT_IMPORT
    }

    fun datasetFor(
        activeUid: String?,
        requestedUid: String?,
        requestedAccountHasData: Boolean,
        hasLegacyData: Boolean
    ): Dataset {
        if (requestedUid.isNullOrBlank()) return Dataset.SIGNED_OUT
        if (!activeUid.isNullOrBlank() && activeUid != requestedUid) {
            return if (requestedAccountHasData) Dataset.ACCOUNT_SCOPED
            else Dataset.EMPTY_ACCOUNT_SCOPE
        }
        if (requestedAccountHasData) return Dataset.ACCOUNT_SCOPED
        return if (hasLegacyData) Dataset.LEGACY_REQUIRES_EXPLICIT_IMPORT
        else Dataset.EMPTY_ACCOUNT_SCOPE
    }
}

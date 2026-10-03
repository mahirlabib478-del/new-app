package com.aistudio.studyos.data.repository

/**
 * Pure gate for associating the shared legacy Room dataset with a Firebase UID.
 * It does not persist ownership or perform a cloud write.
 */
object AccountDatasetOwnershipPolicy {
    enum class State {
        SIGNED_OUT,
        UNCLAIMED,
        OWNED_BY_ACTIVE_ACCOUNT,
        OWNED_BY_ANOTHER_ACCOUNT
    }

    enum class Decision {
        BLOCK,
        REQUIRE_EXPLICIT_LINK,
        ALLOW_AUTOMATIC_SYNC
    }

    fun state(activeUid: String?, ownerUid: String?): State {
        if (activeUid.isNullOrBlank()) return State.SIGNED_OUT
        if (ownerUid.isNullOrBlank()) return State.UNCLAIMED
        return if (activeUid == ownerUid) State.OWNED_BY_ACTIVE_ACCOUNT
        else State.OWNED_BY_ANOTHER_ACCOUNT
    }

    fun decision(
        activeUid: String?,
        ownerUid: String?,
        explicitLinkConfirmed: Boolean,
        cloudWriteAcknowledged: Boolean,
        uidUnchangedThroughoutOperation: Boolean
    ): Decision {
        if (activeUid.isNullOrBlank() || !uidUnchangedThroughoutOperation) return Decision.BLOCK
        if (!ownerUid.isNullOrBlank()) {
            return if (ownerUid == activeUid) Decision.ALLOW_AUTOMATIC_SYNC else Decision.BLOCK
        }
        if (!explicitLinkConfirmed || !cloudWriteAcknowledged) return Decision.REQUIRE_EXPLICIT_LINK
        return Decision.ALLOW_AUTOMATIC_SYNC
    }
}

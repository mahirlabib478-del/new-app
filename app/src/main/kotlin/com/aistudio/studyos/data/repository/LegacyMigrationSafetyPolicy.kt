package com.aistudio.studyos.data.repository

/**
 * Pure safety gate for a future legacy-data migration.
 *
 * This policy never copies, deletes, or commits data. The caller must persist
 * each stage and keep the legacy source recoverable until commit is verified.
 */
object LegacyMigrationSafetyPolicy {
    enum class Stage {
        NOT_STARTED,
        BACKUP_CREATED,
        TARGET_COPIED,
        TARGET_VERIFIED,
        COMMITTED,
        ROLLED_BACK
    }

    enum class Action {
        CREATE_BACKUP,
        COPY_TO_TARGET,
        VERIFY_TARGET,
        COMMIT_MARKER,
        RESTORE_SOURCE,
        ALREADY_COMPLETE,
        BLOCKED
    }

    fun nextAction(
        stage: Stage,
        sourcePreserved: Boolean,
        backupVerified: Boolean,
        targetVerified: Boolean,
        sameAccount: Boolean
    ): Action {
        if (stage == Stage.COMMITTED) return Action.ALREADY_COMPLETE
        if (!sameAccount) return if (sourcePreserved) Action.BLOCKED else Action.RESTORE_SOURCE
        if (stage == Stage.ROLLED_BACK) return Action.BLOCKED
        if (!sourcePreserved) return Action.RESTORE_SOURCE
        return when (stage) {
            Stage.NOT_STARTED -> Action.CREATE_BACKUP
            Stage.BACKUP_CREATED ->
                if (backupVerified) Action.COPY_TO_TARGET else Action.BLOCKED
            Stage.TARGET_COPIED ->
                if (backupVerified) Action.VERIFY_TARGET else Action.BLOCKED
            Stage.TARGET_VERIFIED ->
                if (backupVerified && targetVerified) Action.COMMIT_MARKER else Action.BLOCKED
            Stage.COMMITTED -> Action.ALREADY_COMPLETE
            Stage.ROLLED_BACK -> Action.BLOCKED
        }
    }
}

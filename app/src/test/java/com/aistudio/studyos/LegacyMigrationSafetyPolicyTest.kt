package com.aistudio.studyos

import com.aistudio.studyos.data.repository.LegacyMigrationSafetyPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyMigrationSafetyPolicyTest {
    private fun action(
        stage: LegacyMigrationSafetyPolicy.Stage,
        sourcePreserved: Boolean = true,
        backupVerified: Boolean = true,
        targetVerified: Boolean = true,
        sameAccount: Boolean = true
    ) = LegacyMigrationSafetyPolicy.nextAction(
        stage, sourcePreserved, backupVerified, targetVerified, sameAccount
    )

    @Test fun migrationStartsByCreatingBackup() {
        assertEquals(LegacyMigrationSafetyPolicy.Action.CREATE_BACKUP,
            action(LegacyMigrationSafetyPolicy.Stage.NOT_STARTED))
    }

    @Test fun unverifiedBackupBlocksCopy() {
        assertEquals(LegacyMigrationSafetyPolicy.Action.BLOCKED,
            action(LegacyMigrationSafetyPolicy.Stage.BACKUP_CREATED, backupVerified = false))
    }

    @Test fun verifiedBackupAllowsCopyAndThenVerification() {
        assertEquals(LegacyMigrationSafetyPolicy.Action.COPY_TO_TARGET,
            action(LegacyMigrationSafetyPolicy.Stage.BACKUP_CREATED))
        assertEquals(LegacyMigrationSafetyPolicy.Action.VERIFY_TARGET,
            action(LegacyMigrationSafetyPolicy.Stage.TARGET_COPIED))
    }

    @Test fun commitRequiresVerifiedBackupAndTarget() {
        assertEquals(LegacyMigrationSafetyPolicy.Action.COMMIT_MARKER,
            action(LegacyMigrationSafetyPolicy.Stage.TARGET_VERIFIED))
        assertEquals(LegacyMigrationSafetyPolicy.Action.BLOCKED,
            action(LegacyMigrationSafetyPolicy.Stage.TARGET_VERIFIED, targetVerified = false))
        assertEquals(LegacyMigrationSafetyPolicy.Action.BLOCKED,
            action(LegacyMigrationSafetyPolicy.Stage.TARGET_VERIFIED, backupVerified = false))
    }

    @Test fun missingSourceRequiresRecoveryAndAccountSwitchBlocks() {
        assertEquals(LegacyMigrationSafetyPolicy.Action.RESTORE_SOURCE,
            action(LegacyMigrationSafetyPolicy.Stage.TARGET_COPIED, sourcePreserved = false))
        assertEquals(LegacyMigrationSafetyPolicy.Action.BLOCKED,
            action(LegacyMigrationSafetyPolicy.Stage.TARGET_COPIED, sameAccount = false))
        assertEquals(LegacyMigrationSafetyPolicy.Action.RESTORE_SOURCE,
            action(LegacyMigrationSafetyPolicy.Stage.TARGET_COPIED,
                sourcePreserved = false, sameAccount = false))
    }

    @Test fun committedMigrationIsIdempotentlyComplete() {
        assertEquals(LegacyMigrationSafetyPolicy.Action.ALREADY_COMPLETE,
            action(LegacyMigrationSafetyPolicy.Stage.COMMITTED))
    }

    @Test fun rolledBackMigrationCannotContinue() {
        assertEquals(LegacyMigrationSafetyPolicy.Action.BLOCKED,
            action(LegacyMigrationSafetyPolicy.Stage.ROLLED_BACK))
    }
}

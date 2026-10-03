package com.aistudio.studyos

import com.aistudio.studyos.data.repository.LegacyProgressImportPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyProgressImportPolicyTest {
    @Test
    fun missingLegacyDataDoesNotStartImport() {
        assertEquals(
            LegacyProgressImportPolicy.Decision.NO_LEGACY_DATA,
            LegacyProgressImportPolicy.decide("uid-a", false, false, true)
        )
    }

    @Test
    fun legacyDataCannotBeAssignedWithoutSignedInAccount() {
        assertEquals(
            LegacyProgressImportPolicy.Decision.SIGN_IN_REQUIRED,
            LegacyProgressImportPolicy.decide(null, true, false, true)
        )
        assertEquals(
            LegacyProgressImportPolicy.Decision.SIGN_IN_REQUIRED,
            LegacyProgressImportPolicy.decide(" ", true, false, true)
        )
    }

    @Test
    fun existingAccountDataRequiresSeparateConflictHandling() {
        assertEquals(
            LegacyProgressImportPolicy.Decision.TARGET_ALREADY_HAS_DATA,
            LegacyProgressImportPolicy.decide("uid-a", true, true, true)
        )
    }

    @Test
    fun importRequiresExplicitConfirmation() {
        assertEquals(
            LegacyProgressImportPolicy.Decision.EXPLICIT_CONFIRMATION_REQUIRED,
            LegacyProgressImportPolicy.decide("uid-a", true, false, false)
        )
        assertEquals(
            LegacyProgressImportPolicy.Decision.IMPORT_ALLOWED,
            LegacyProgressImportPolicy.decide("uid-a", true, false, true)
        )
    }
}

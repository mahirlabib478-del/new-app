package com.aistudio.studyos

import com.aistudio.studyos.data.repository.AccountDataIsolationPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountDataIsolationPolicyTest {
    @Test
    fun signedOutDoesNotExposeAnyDataset() {
        assertEquals(
            AccountDataIsolationPolicy.Dataset.SIGNED_OUT,
            AccountDataIsolationPolicy.datasetFor("uid-a", null, true, true)
        )
    }

    @Test
    fun firstSignInDoesNotSilentlyClaimLegacyData() {
        assertEquals(
            AccountDataIsolationPolicy.Dataset.LEGACY_REQUIRES_EXPLICIT_IMPORT,
            AccountDataIsolationPolicy.datasetFor(null, "uid-a", false, true)
        )
    }

    @Test
    fun sameAccountUsesOnlyItsScopedDataWhenPresent() {
        assertEquals(
            AccountDataIsolationPolicy.Dataset.ACCOUNT_SCOPED,
            AccountDataIsolationPolicy.datasetFor("uid-a", "uid-a", true, true)
        )
    }

    @Test
    fun switchingToEmptyAccountNeverFallsBackToPreviousOrLegacyData() {
        assertEquals(
            AccountDataIsolationPolicy.Dataset.EMPTY_ACCOUNT_SCOPE,
            AccountDataIsolationPolicy.datasetFor("uid-a", "uid-b", false, true)
        )
    }

    @Test
    fun switchingToExistingAccountSelectsItsScopedDataset() {
        assertEquals(
            AccountDataIsolationPolicy.Dataset.ACCOUNT_SCOPED,
            AccountDataIsolationPolicy.datasetFor("uid-a", "uid-b", true, true)
        )
    }

    @Test
    fun newAccountWithoutLegacyDataStartsEmpty() {
        assertEquals(
            AccountDataIsolationPolicy.Dataset.EMPTY_ACCOUNT_SCOPE,
            AccountDataIsolationPolicy.datasetFor(null, "uid-a", false, false)
        )
    }
}

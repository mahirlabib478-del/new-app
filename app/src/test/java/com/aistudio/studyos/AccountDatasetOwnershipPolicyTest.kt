package com.aistudio.studyos

import com.aistudio.studyos.data.repository.AccountDatasetOwnershipPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountDatasetOwnershipPolicyTest {
    @Test fun signedOutIsBlocked() {
        assertEquals(AccountDatasetOwnershipPolicy.Decision.BLOCK,
            AccountDatasetOwnershipPolicy.decision(null, null, true, true, true))
    }

    @Test fun unclaimedDatasetNeedsExplicitLinkAndAcknowledgedWrite() {
        assertEquals(AccountDatasetOwnershipPolicy.Decision.REQUIRE_EXPLICIT_LINK,
            AccountDatasetOwnershipPolicy.decision("uid-a", null, false, true, true))
        assertEquals(AccountDatasetOwnershipPolicy.Decision.REQUIRE_EXPLICIT_LINK,
            AccountDatasetOwnershipPolicy.decision("uid-a", null, true, false, true))
    }

    @Test fun explicitLinkOnlyArmsAfterSuccessfulWriteAndStableUid() {
        assertEquals(AccountDatasetOwnershipPolicy.Decision.ALLOW_AUTOMATIC_SYNC,
            AccountDatasetOwnershipPolicy.decision("uid-a", null, true, true, true))
        assertEquals(AccountDatasetOwnershipPolicy.Decision.BLOCK,
            AccountDatasetOwnershipPolicy.decision("uid-a", null, true, true, false))
    }

    @Test fun matchingOwnerAllowsAndDifferentOwnerBlocks() {
        assertEquals(AccountDatasetOwnershipPolicy.Decision.ALLOW_AUTOMATIC_SYNC,
            AccountDatasetOwnershipPolicy.decision("uid-a", "uid-a", false, false, true))
        assertEquals(AccountDatasetOwnershipPolicy.Decision.BLOCK,
            AccountDatasetOwnershipPolicy.decision("uid-b", "uid-a", true, true, true))
    }

    @Test fun stateDistinguishesUnclaimedAndForeignOwnership() {
        assertEquals(AccountDatasetOwnershipPolicy.State.UNCLAIMED,
            AccountDatasetOwnershipPolicy.state("uid-a", null))
        assertEquals(AccountDatasetOwnershipPolicy.State.OWNED_BY_ANOTHER_ACCOUNT,
            AccountDatasetOwnershipPolicy.state("uid-b", "uid-a"))
    }
}

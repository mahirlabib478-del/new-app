package com.aistudio.studyos

import com.aistudio.studyos.adsterra.AdsterraManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdsterraPostSessionAdTest {

    @Before
    fun setup() {
        AdsterraManager.dismissAd()
    }

    @Test
    fun testDirectLinkUrlConfiguredCorrectly() {
        val expected = "https://www.profitableratecpmnetwork.com/e8vebdqa?key=daa23000512567adaa7bbb3efc276252"
        assertEquals(expected, AdsterraManager.DIRECT_LINK_URL)
        assertTrue(AdsterraManager.DIRECT_LINK_URL.startsWith("https://"))
        assertTrue(AdsterraManager.DIRECT_LINK_URL.contains("key=daa23000512567adaa7bbb3efc276252"))
    }

    @Test
    fun testInitialAdStateIsHidden() {
        assertFalse("Ad should be hidden initially", AdsterraManager.isAdVisible.value)
    }

    @Test
    fun testDismissHidesAd() {
        AdsterraManager.dismissAd()
        assertFalse(AdsterraManager.isAdVisible.value)
    }
}

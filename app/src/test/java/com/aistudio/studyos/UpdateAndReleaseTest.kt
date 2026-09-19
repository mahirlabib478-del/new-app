package com.aistudio.studyos

import com.aistudio.studyos.data.update.UpdateManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying update comparison and release readiness logic:
 * 1. SemVer comparison (e.g. 0.3.0 vs 0.3.1, 0.4.0, 1.0.0)
 * 2. Build code comparison (e.g. 0.3.0+28 vs 0.3.0+29)
 * 3. Prefix stripping ('v', 'V', whitespace)
 * 4. Preventing false update triggers when version is same or older
 */
class UpdateAndReleaseTest {

    @Test
    fun testUpdateDetectionNewerPatch() {
        val hasUpdate = UpdateManager.isNewerVersion(
            currentVersionName = "0.3.0",
            currentVersionCode = 28L,
            remoteVersionString = "0.3.1"
        )
        assertTrue(hasUpdate)
    }

    @Test
    fun testUpdateDetectionWithVPrefix() {
        val hasUpdate = UpdateManager.isNewerVersion(
            currentVersionName = "0.3.0",
            currentVersionCode = 28L,
            remoteVersionString = "v0.3.2"
        )
        assertTrue(hasUpdate)
    }

    @Test
    fun testUpdateDetectionSameVersion() {
        val hasUpdate = UpdateManager.isNewerVersion(
            currentVersionName = "0.3.0",
            currentVersionCode = 28L,
            remoteVersionString = "v0.3.0"
        )
        assertFalse(hasUpdate)
    }

    @Test
    fun testUpdateDetectionOlderVersion() {
        val hasUpdate = UpdateManager.isNewerVersion(
            currentVersionName = "0.3.0",
            currentVersionCode = 28L,
            remoteVersionString = "0.2.9"
        )
        assertFalse(hasUpdate)
    }

    @Test
    fun testUpdateDetectionSameSemVerHigherBuildCode() {
        val hasUpdate = UpdateManager.isNewerVersion(
            currentVersionName = "0.3.0",
            currentVersionCode = 28L,
            remoteVersionString = "0.3.0+29"
        )
        assertTrue(hasUpdate)
    }

    @Test
    fun testUpdateDetectionLatestBuildCodeIsNotShownAgain() {
        val hasUpdate = UpdateManager.isNewerVersion(
            currentVersionName = "0.3.0",
            currentVersionCode = 29L,
            remoteVersionString = "0.3.0+29"
        )
        assertFalse(hasUpdate)
    }

    @Test
    fun testUpdateDetectionSameSemVerSameBuildCode() {
        val hasUpdate = UpdateManager.isNewerVersion(
            currentVersionName = "0.3.0",
            currentVersionCode = 28L,
            remoteVersionString = "0.3.0+28"
        )
        assertFalse(hasUpdate)
    }

    @Test
    fun testUpdateDetectionMajorVersionBump() {
        val hasUpdate = UpdateManager.isNewerVersion(
            currentVersionName = "0.3.0",
            currentVersionCode = 28L,
            remoteVersionString = "1.0.0"
        )
        assertTrue(hasUpdate)
    }
}

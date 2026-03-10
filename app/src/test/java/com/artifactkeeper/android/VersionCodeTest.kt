package com.artifactkeeper.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the versionCode calculation logic defined in build.gradle.kts.
 *
 * The actual resolvedVersionCode() function runs at Gradle evaluation time
 * and cannot be called from unit tests. Instead we verify the underlying
 * logic branches:
 *   1. GITHUB_RUN_NUMBER env var -> parse as Int
 *   2. git rev-list --count HEAD -> parse as Int
 *   3. Fallback -> 1
 *
 * Similarly, resolvedVersionName() reads VERSION_NAME env or defaults to "1.0.0".
 */
class VersionCodeTest {

    // Replicates the logic of resolvedVersionCode()
    private fun resolvedVersionCode(envRunNumber: String?, gitCommitCount: String?): Int {
        if (!envRunNumber.isNullOrBlank()) {
            return envRunNumber.toIntOrNull() ?: 1
        }
        if (gitCommitCount != null) {
            return gitCommitCount.trim().toIntOrNull() ?: 1
        }
        return 1
    }

    // Replicates the logic of resolvedVersionName()
    private fun resolvedVersionName(envVersionName: String?): String {
        return envVersionName ?: "1.0.0"
    }

    // =========================================================================
    // GITHUB_RUN_NUMBER path
    // =========================================================================

    @Test
    fun `CI run number is used when set`() {
        assertEquals(42, resolvedVersionCode("42", null))
    }

    @Test
    fun `CI run number handles large values`() {
        assertEquals(99999, resolvedVersionCode("99999", null))
    }

    @Test
    fun `CI run number handles value of 1`() {
        assertEquals(1, resolvedVersionCode("1", null))
    }

    @Test
    fun `invalid CI run number falls back to 1`() {
        assertEquals(1, resolvedVersionCode("not-a-number", null))
    }

    @Test
    fun `empty CI run number falls through to git count`() {
        assertEquals(150, resolvedVersionCode("", "150"))
    }

    @Test
    fun `blank CI run number falls through to git count`() {
        assertEquals(200, resolvedVersionCode("   ", "200"))
    }

    @Test
    fun `null CI run number falls through to git count`() {
        assertEquals(300, resolvedVersionCode(null, "300"))
    }

    // =========================================================================
    // Git commit count path
    // =========================================================================

    @Test
    fun `git commit count is used as version code`() {
        assertEquals(547, resolvedVersionCode(null, "547"))
    }

    @Test
    fun `git commit count with leading whitespace is trimmed`() {
        assertEquals(123, resolvedVersionCode(null, "  123  "))
    }

    @Test
    fun `git commit count with newline is trimmed`() {
        assertEquals(456, resolvedVersionCode(null, "456\n"))
    }

    @Test
    fun `invalid git output falls back to 1`() {
        assertEquals(1, resolvedVersionCode(null, "fatal: not a git repository"))
    }

    @Test
    fun `empty git output falls back to 1`() {
        assertEquals(1, resolvedVersionCode(null, ""))
    }

    // =========================================================================
    // Full fallback
    // =========================================================================

    @Test
    fun `both null returns fallback 1`() {
        assertEquals(1, resolvedVersionCode(null, null))
    }

    // =========================================================================
    // CI run number takes priority over git count
    // =========================================================================

    @Test
    fun `CI run number takes priority over git count`() {
        assertEquals(42, resolvedVersionCode("42", "999"))
    }

    // =========================================================================
    // resolvedVersionName
    // =========================================================================

    @Test
    fun `VERSION_NAME env is used when set`() {
        assertEquals("2.0.0-beta.1", resolvedVersionName("2.0.0-beta.1"))
    }

    @Test
    fun `VERSION_NAME defaults to 1_0_0 when null`() {
        assertEquals("1.0.0", resolvedVersionName(null))
    }

    @Test
    fun `VERSION_NAME accepts any string`() {
        assertEquals("custom-version", resolvedVersionName("custom-version"))
    }

    // =========================================================================
    // versionCode should always be positive
    // =========================================================================

    @Test
    fun `result is always at least 1`() {
        assertTrue(resolvedVersionCode(null, null) >= 1)
        assertTrue(resolvedVersionCode("", null) >= 1)
        assertTrue(resolvedVersionCode(null, "") >= 1)
        assertTrue(resolvedVersionCode("abc", null) >= 1)
        assertTrue(resolvedVersionCode(null, "xyz") >= 1)
    }

    // =========================================================================
    // toIntOrNull behavior for edge cases
    // =========================================================================

    @Test
    fun `negative number parses correctly`() {
        // The Gradle function does not guard against negative values,
        // but toIntOrNull will parse them. This documents the behavior.
        assertEquals(-5, "-5".toIntOrNull())
    }

    @Test
    fun `zero parses correctly`() {
        assertEquals(0, "0".toIntOrNull())
    }

    @Test
    fun `overflow returns null from toIntOrNull`() {
        assertNull("99999999999999".toIntOrNull())
    }
}

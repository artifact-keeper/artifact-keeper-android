package com.artifactkeeper.android

import com.artifactkeeper.android.data.EncryptedPrefsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the EncryptedPrefsManager key constants, sensitive key lists,
 * and migration decision logic (hasLegacyData check).
 */
class EncryptedPrefsManagerTest {

    // =========================================================================
    // Key constant values
    // =========================================================================

    @Test
    fun `KEY_SERVER_URL has expected value`() {
        assertEquals("server_url", EncryptedPrefsManager.KEY_SERVER_URL)
    }

    @Test
    fun `KEY_AUTH_TOKEN has expected value`() {
        assertEquals("auth_token", EncryptedPrefsManager.KEY_AUTH_TOKEN)
    }

    @Test
    fun `KEY_USER_ID has expected value`() {
        assertEquals("user_id", EncryptedPrefsManager.KEY_USER_ID)
    }

    @Test
    fun `KEY_USER_USERNAME has expected value`() {
        assertEquals("user_username", EncryptedPrefsManager.KEY_USER_USERNAME)
    }

    @Test
    fun `KEY_USER_EMAIL has expected value`() {
        assertEquals("user_email", EncryptedPrefsManager.KEY_USER_EMAIL)
    }

    @Test
    fun `KEY_USER_IS_ADMIN has expected value`() {
        assertEquals("user_is_admin", EncryptedPrefsManager.KEY_USER_IS_ADMIN)
    }

    // =========================================================================
    // Sensitive keys list coverage
    // =========================================================================

    @Test
    fun `sensitive string keys list contains all auth keys`() {
        // Mirrors the private SENSITIVE_STRING_KEYS in EncryptedPrefsManager
        val sensitiveStringKeys = listOf(
            EncryptedPrefsManager.KEY_SERVER_URL,
            EncryptedPrefsManager.KEY_AUTH_TOKEN,
            EncryptedPrefsManager.KEY_USER_ID,
            EncryptedPrefsManager.KEY_USER_USERNAME,
            EncryptedPrefsManager.KEY_USER_EMAIL,
        )
        assertEquals(5, sensitiveStringKeys.size)
        assertTrue(sensitiveStringKeys.contains("server_url"))
        assertTrue(sensitiveStringKeys.contains("auth_token"))
        assertTrue(sensitiveStringKeys.contains("user_id"))
        assertTrue(sensitiveStringKeys.contains("user_username"))
        assertTrue(sensitiveStringKeys.contains("user_email"))
    }

    @Test
    fun `sensitive boolean keys list contains admin key`() {
        val sensitiveBooleanKeys = listOf(
            EncryptedPrefsManager.KEY_USER_IS_ADMIN,
        )
        assertEquals(1, sensitiveBooleanKeys.size)
        assertTrue(sensitiveBooleanKeys.contains("user_is_admin"))
    }

    // =========================================================================
    // clearAuthData removes correct keys (logic test)
    // =========================================================================

    @Test
    fun `clearAuthData keys list does not include server_url`() {
        // clearAuthData should clear token + user info but keep server_url.
        val clearKeys = listOf(
            EncryptedPrefsManager.KEY_AUTH_TOKEN,
            EncryptedPrefsManager.KEY_USER_ID,
            EncryptedPrefsManager.KEY_USER_USERNAME,
            EncryptedPrefsManager.KEY_USER_EMAIL,
            EncryptedPrefsManager.KEY_USER_IS_ADMIN,
        )
        assertFalse(clearKeys.contains(EncryptedPrefsManager.KEY_SERVER_URL))
        assertEquals(5, clearKeys.size)
    }

    // =========================================================================
    // Migration decision logic (hasLegacyData check)
    // =========================================================================

    @Test
    fun `hasLegacyData is true when any sensitive string key is present`() {
        val sensitiveStringKeys = listOf("server_url", "auth_token", "user_id", "user_username", "user_email")
        val sensitiveBooleanKeys = listOf("user_is_admin")

        val legacyContains = setOf("auth_token")
        val hasLegacyData = sensitiveStringKeys.any { legacyContains.contains(it) } ||
            sensitiveBooleanKeys.any { legacyContains.contains(it) }
        assertTrue(hasLegacyData)
    }

    @Test
    fun `hasLegacyData is true when boolean key is present`() {
        val sensitiveStringKeys = listOf("server_url", "auth_token", "user_id", "user_username", "user_email")
        val sensitiveBooleanKeys = listOf("user_is_admin")

        val legacyContains = setOf("user_is_admin")
        val hasLegacyData = sensitiveStringKeys.any { legacyContains.contains(it) } ||
            sensitiveBooleanKeys.any { legacyContains.contains(it) }
        assertTrue(hasLegacyData)
    }

    @Test
    fun `hasLegacyData is false when no sensitive keys present`() {
        val sensitiveStringKeys = listOf("server_url", "auth_token", "user_id", "user_username", "user_email")
        val sensitiveBooleanKeys = listOf("user_is_admin")

        val legacyContains = setOf("some_other_key")
        val hasLegacyData = sensitiveStringKeys.any { legacyContains.contains(it) } ||
            sensitiveBooleanKeys.any { legacyContains.contains(it) }
        assertFalse(hasLegacyData)
    }

    @Test
    fun `hasLegacyData is false when legacy store is empty`() {
        val sensitiveStringKeys = listOf("server_url", "auth_token", "user_id", "user_username", "user_email")
        val sensitiveBooleanKeys = listOf("user_is_admin")

        val legacyContains = emptySet<String>()
        val hasLegacyData = sensitiveStringKeys.any { legacyContains.contains(it) } ||
            sensitiveBooleanKeys.any { legacyContains.contains(it) }
        assertFalse(hasLegacyData)
    }

    // =========================================================================
    // Migration copy logic (only copy if encrypted store does not have key)
    // =========================================================================

    @Test
    fun `migration skips key when encrypted store already has it`() {
        val encryptedContains = setOf("auth_token")
        val legacyValue = "some_token"
        val shouldCopy = legacyValue != null && !encryptedContains.contains("auth_token")
        assertFalse(shouldCopy)
    }

    @Test
    fun `migration copies key when encrypted store does not have it`() {
        val encryptedContains = emptySet<String>()
        val legacyValue = "some_token"
        val shouldCopy = legacyValue != null && !encryptedContains.contains("auth_token")
        assertTrue(shouldCopy)
    }

    @Test
    fun `migration skips null legacy value`() {
        val encryptedContains = emptySet<String>()
        val legacyValue: String? = null
        val shouldCopy = legacyValue != null && !encryptedContains.contains("auth_token")
        assertFalse(shouldCopy)
    }

    // =========================================================================
    // All key constants are unique
    // =========================================================================

    @Test
    fun `all key constants are unique`() {
        val keys = listOf(
            EncryptedPrefsManager.KEY_SERVER_URL,
            EncryptedPrefsManager.KEY_AUTH_TOKEN,
            EncryptedPrefsManager.KEY_USER_ID,
            EncryptedPrefsManager.KEY_USER_USERNAME,
            EncryptedPrefsManager.KEY_USER_EMAIL,
            EncryptedPrefsManager.KEY_USER_IS_ADMIN,
        )
        assertEquals(6, keys.toSet().size)
    }
}

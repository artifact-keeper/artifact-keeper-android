package com.artifactkeeper.android

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.artifactkeeper.android.data.EncryptedPrefsManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for EncryptedPrefsManager that exercise the real methods by injecting
 * a mock SharedPreferences via [EncryptedPrefsManager.setPrefsForTesting].
 */
class EncryptedPrefsManagerTest {

    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0

        mockEditor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { putString(any(), any()) } returns this
            every { putBoolean(any(), any()) } returns this
            every { remove(any()) } returns this
            every { clear() } returns this
            every { apply() } returns Unit
        }
        mockPrefs = mockk {
            every { edit() } returns mockEditor
        }
        mockContext = mockk()

        EncryptedPrefsManager.setPrefsForTesting(mockPrefs)
    }

    @After
    fun tearDown() {
        EncryptedPrefsManager.resetForTesting()
        unmockkStatic(Log::class)
    }

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

    // =========================================================================
    // getString
    // =========================================================================

    @Test
    fun `getString returns value from prefs`() {
        every { mockPrefs.getString("auth_token", null) } returns "tok_abc123"
        val result = EncryptedPrefsManager.getString(mockContext, "auth_token")
        assertEquals("tok_abc123", result)
    }

    @Test
    fun `getString returns null when key not present`() {
        every { mockPrefs.getString("missing_key", null) } returns null
        val result = EncryptedPrefsManager.getString(mockContext, "missing_key")
        assertNull(result)
    }

    // =========================================================================
    // getBoolean
    // =========================================================================

    @Test
    fun `getBoolean returns value from prefs`() {
        every { mockPrefs.getBoolean("user_is_admin", false) } returns true
        val result = EncryptedPrefsManager.getBoolean(mockContext, "user_is_admin")
        assertTrue(result)
    }

    @Test
    fun `getBoolean uses provided default value`() {
        every { mockPrefs.getBoolean("unknown_key", true) } returns true
        val result = EncryptedPrefsManager.getBoolean(mockContext, "unknown_key", default = true)
        assertTrue(result)
    }

    @Test
    fun `getBoolean returns false by default`() {
        every { mockPrefs.getBoolean("user_is_admin", false) } returns false
        val result = EncryptedPrefsManager.getBoolean(mockContext, "user_is_admin")
        assertFalse(result)
    }

    // =========================================================================
    // putString
    // =========================================================================

    @Test
    fun `putString writes string via editor`() {
        EncryptedPrefsManager.putString(mockContext, "server_url", "https://example.com/")

        verify { mockEditor.putString("server_url", "https://example.com/") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `putString writes null value`() {
        EncryptedPrefsManager.putString(mockContext, "auth_token", null)

        verify { mockEditor.putString("auth_token", null) }
        verify { mockEditor.apply() }
    }

    // =========================================================================
    // putBoolean
    // =========================================================================

    @Test
    fun `putBoolean writes boolean via editor`() {
        EncryptedPrefsManager.putBoolean(mockContext, "user_is_admin", true)

        verify { mockEditor.putBoolean("user_is_admin", true) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `putBoolean writes false via editor`() {
        EncryptedPrefsManager.putBoolean(mockContext, "user_is_admin", false)

        verify { mockEditor.putBoolean("user_is_admin", false) }
        verify { mockEditor.apply() }
    }

    // =========================================================================
    // remove
    // =========================================================================

    @Test
    fun `remove removes single key`() {
        EncryptedPrefsManager.remove(mockContext, "auth_token")

        verify { mockEditor.remove("auth_token") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `remove removes multiple keys`() {
        EncryptedPrefsManager.remove(
            mockContext,
            "auth_token",
            "user_id",
            "user_email",
        )

        verify { mockEditor.remove("auth_token") }
        verify { mockEditor.remove("user_id") }
        verify { mockEditor.remove("user_email") }
        verify { mockEditor.apply() }
    }

    // =========================================================================
    // clearAuthData
    // =========================================================================

    @Test
    fun `clearAuthData removes auth keys but not server_url`() {
        EncryptedPrefsManager.clearAuthData(mockContext)

        verify { mockEditor.remove("auth_token") }
        verify { mockEditor.remove("user_id") }
        verify { mockEditor.remove("user_username") }
        verify { mockEditor.remove("user_email") }
        verify { mockEditor.remove("user_is_admin") }
        verify(exactly = 0) { mockEditor.remove("server_url") }
        verify { mockEditor.apply() }
    }

    // =========================================================================
    // clearAll
    // =========================================================================

    @Test
    fun `clearAll clears all prefs`() {
        EncryptedPrefsManager.clearAll(mockContext)

        verify { mockEditor.clear() }
        verify { mockEditor.apply() }
    }

    // =========================================================================
    // saveLoginData
    // =========================================================================

    @Test
    fun `saveLoginData writes all login fields`() {
        EncryptedPrefsManager.saveLoginData(
            context = mockContext,
            token = "jwt_xyz",
            userId = "user-123",
            username = "alice",
            email = "alice@example.com",
            isAdmin = true,
        )

        verify { mockEditor.putString("auth_token", "jwt_xyz") }
        verify { mockEditor.putString("user_id", "user-123") }
        verify { mockEditor.putString("user_username", "alice") }
        verify { mockEditor.putString("user_email", "alice@example.com") }
        verify { mockEditor.putBoolean("user_is_admin", true) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `saveLoginData writes non-admin user`() {
        EncryptedPrefsManager.saveLoginData(
            context = mockContext,
            token = "jwt_abc",
            userId = "user-456",
            username = "bob",
            email = "bob@example.com",
            isAdmin = false,
        )

        verify { mockEditor.putBoolean("user_is_admin", false) }
        verify { mockEditor.apply() }
    }

    // =========================================================================
    // migrateFromPlaintext -- no legacy data
    // =========================================================================

    @Test
    fun `migrateFromPlaintext skips when no sensitive keys in legacy store`() {
        val legacyPrefs = mockk<SharedPreferences> {
            every { contains(any()) } returns false
        }
        val legacyEditor = mockk<SharedPreferences.Editor>(relaxed = true)

        val encPrefs = mockk<SharedPreferences> {
            every { edit() } returns mockk(relaxed = true)
        }

        every { mockContext.getSharedPreferences("artifact_keeper_prefs", Context.MODE_PRIVATE) } returns legacyPrefs

        EncryptedPrefsManager.migrateFromPlaintext(mockContext, encPrefs)

        // Should not call edit() on encPrefs since there's nothing to migrate
        verify(exactly = 0) { encPrefs.edit() }
    }

    // =========================================================================
    // migrateFromPlaintext -- with legacy string keys
    // =========================================================================

    @Test
    fun `migrateFromPlaintext copies string keys from legacy to encrypted store`() {
        val legacyEditor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { remove(any()) } returns this
            every { apply() } returns Unit
        }
        val legacyPrefs = mockk<SharedPreferences> {
            every { contains("server_url") } returns true
            every { contains("auth_token") } returns true
            every { contains("user_id") } returns false
            every { contains("user_username") } returns false
            every { contains("user_email") } returns false
            every { contains("user_is_admin") } returns false
            every { getString("server_url", null) } returns "https://old.example.com/"
            every { getString("auth_token", null) } returns "old_token"
            every { getString("user_id", null) } returns null
            every { getString("user_username", null) } returns null
            every { getString("user_email", null) } returns null
            every { edit() } returns legacyEditor
        }

        val encEditor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { putString(any(), any()) } returns this
            every { putBoolean(any(), any()) } returns this
            every { apply() } returns Unit
        }
        val encPrefs = mockk<SharedPreferences> {
            every { contains(any()) } returns false
            every { edit() } returns encEditor
        }

        every { mockContext.getSharedPreferences("artifact_keeper_prefs", Context.MODE_PRIVATE) } returns legacyPrefs

        EncryptedPrefsManager.migrateFromPlaintext(mockContext, encPrefs)

        verify { encEditor.putString("server_url", "https://old.example.com/") }
        verify { encEditor.putString("auth_token", "old_token") }
        verify { legacyEditor.remove("server_url") }
        verify { legacyEditor.remove("auth_token") }
        verify { encEditor.apply() }
        verify { legacyEditor.apply() }
    }

    // =========================================================================
    // migrateFromPlaintext -- with legacy boolean keys
    // =========================================================================

    @Test
    fun `migrateFromPlaintext copies boolean keys from legacy to encrypted store`() {
        val legacyEditor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { remove(any()) } returns this
            every { apply() } returns Unit
        }
        val legacyPrefs = mockk<SharedPreferences> {
            every { contains("server_url") } returns false
            every { contains("auth_token") } returns false
            every { contains("user_id") } returns false
            every { contains("user_username") } returns false
            every { contains("user_email") } returns false
            every { contains("user_is_admin") } returns true
            every { getString(any(), null) } returns null
            every { getBoolean("user_is_admin", false) } returns true
            every { edit() } returns legacyEditor
        }

        val encEditor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { putString(any(), any()) } returns this
            every { putBoolean(any(), any()) } returns this
            every { apply() } returns Unit
        }
        val encPrefs = mockk<SharedPreferences> {
            every { contains(any()) } returns false
            every { edit() } returns encEditor
        }

        every { mockContext.getSharedPreferences("artifact_keeper_prefs", Context.MODE_PRIVATE) } returns legacyPrefs

        EncryptedPrefsManager.migrateFromPlaintext(mockContext, encPrefs)

        verify { encEditor.putBoolean("user_is_admin", true) }
        verify { legacyEditor.remove("user_is_admin") }
        verify { encEditor.apply() }
        verify { legacyEditor.apply() }
    }

    // =========================================================================
    // migrateFromPlaintext -- encrypted store already has key (skip)
    // =========================================================================

    @Test
    fun `migrateFromPlaintext skips key when encrypted store already has it`() {
        val legacyEditor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { remove(any()) } returns this
            every { apply() } returns Unit
        }
        val legacyPrefs = mockk<SharedPreferences> {
            every { contains("server_url") } returns true
            every { contains("auth_token") } returns false
            every { contains("user_id") } returns false
            every { contains("user_username") } returns false
            every { contains("user_email") } returns false
            every { contains("user_is_admin") } returns true
            every { getString("server_url", null) } returns "https://old.example.com/"
            every { getString("auth_token", null) } returns null
            every { getString("user_id", null) } returns null
            every { getString("user_username", null) } returns null
            every { getString("user_email", null) } returns null
            every { getBoolean("user_is_admin", false) } returns true
            every { edit() } returns legacyEditor
        }

        val encEditor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { putString(any(), any()) } returns this
            every { putBoolean(any(), any()) } returns this
            every { apply() } returns Unit
        }
        val encPrefs = mockk<SharedPreferences> {
            // Encrypted store already has server_url and user_is_admin
            every { contains(any()) } returns false
            every { contains("server_url") } returns true
            every { contains("user_is_admin") } returns true
            every { edit() } returns encEditor
        }

        every { mockContext.getSharedPreferences("artifact_keeper_prefs", Context.MODE_PRIVATE) } returns legacyPrefs

        EncryptedPrefsManager.migrateFromPlaintext(mockContext, encPrefs)

        // Should NOT write to encrypted store because the keys already exist
        verify(exactly = 0) { encEditor.putString("server_url", any()) }
        verify(exactly = 0) { encEditor.putBoolean("user_is_admin", any()) }
        // But should still remove from legacy
        verify { legacyEditor.remove("server_url") }
        verify { legacyEditor.remove("user_is_admin") }
    }

    // =========================================================================
    // migrateFromPlaintext -- full migration with all keys
    // =========================================================================

    @Test
    fun `migrateFromPlaintext copies all sensitive keys in full migration`() {
        val legacyEditor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { remove(any()) } returns this
            every { apply() } returns Unit
        }
        val legacyPrefs = mockk<SharedPreferences> {
            every { contains("server_url") } returns true
            every { contains("auth_token") } returns true
            every { contains("user_id") } returns true
            every { contains("user_username") } returns true
            every { contains("user_email") } returns true
            every { contains("user_is_admin") } returns true
            every { getString("server_url", null) } returns "https://srv.example.com/"
            every { getString("auth_token", null) } returns "my_token"
            every { getString("user_id", null) } returns "u-001"
            every { getString("user_username", null) } returns "admin"
            every { getString("user_email", null) } returns "admin@example.com"
            every { getBoolean("user_is_admin", false) } returns true
            every { edit() } returns legacyEditor
        }

        val encEditor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { putString(any(), any()) } returns this
            every { putBoolean(any(), any()) } returns this
            every { apply() } returns Unit
        }
        val encPrefs = mockk<SharedPreferences> {
            every { contains(any()) } returns false
            every { edit() } returns encEditor
        }

        every { mockContext.getSharedPreferences("artifact_keeper_prefs", Context.MODE_PRIVATE) } returns legacyPrefs

        EncryptedPrefsManager.migrateFromPlaintext(mockContext, encPrefs)

        verify { encEditor.putString("server_url", "https://srv.example.com/") }
        verify { encEditor.putString("auth_token", "my_token") }
        verify { encEditor.putString("user_id", "u-001") }
        verify { encEditor.putString("user_username", "admin") }
        verify { encEditor.putString("user_email", "admin@example.com") }
        verify { encEditor.putBoolean("user_is_admin", true) }
        verify { legacyEditor.remove("server_url") }
        verify { legacyEditor.remove("auth_token") }
        verify { legacyEditor.remove("user_id") }
        verify { legacyEditor.remove("user_username") }
        verify { legacyEditor.remove("user_email") }
        verify { legacyEditor.remove("user_is_admin") }
        verify { encEditor.apply() }
        verify { legacyEditor.apply() }
    }
}

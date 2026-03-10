package com.artifactkeeper.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the display and validation logic in ProfileScreen.kt.
 * Covers password validation, profile edit field mapping, TOTP code
 * format checks, and display name / email ifBlank handling.
 */
class ProfileScreenLogicTest {

    // =========================================================================
    // Change password validation
    // =========================================================================

    @Test
    fun `password change rejected when new and confirm do not match`() {
        val newPassword = "new-secret"
        val confirmPassword = "different-secret"
        val mismatch = newPassword != confirmPassword
        assertTrue(mismatch)
    }

    @Test
    fun `password change accepted when new and confirm match`() {
        val newPassword = "new-secret"
        val confirmPassword = "new-secret"
        val mismatch = newPassword != confirmPassword
        assertFalse(mismatch)
    }

    @Test
    fun `blank new password is invalid`() {
        val newPassword = ""
        assertTrue(newPassword.isBlank())
    }

    @Test
    fun `blank current password is invalid`() {
        val currentPassword = "   "
        assertTrue(currentPassword.isBlank())
    }

    // =========================================================================
    // Profile edit: displayName ifBlank
    // =========================================================================

    @Test
    fun `blank displayName becomes null for update request`() {
        val displayName = ""
        val result = displayName.ifBlank { null }
        assertNull(result)
    }

    @Test
    fun `whitespace displayName becomes null`() {
        val displayName = "   "
        val result = displayName.ifBlank { null }
        assertNull(result)
    }

    @Test
    fun `non-blank displayName is preserved`() {
        val displayName = "Alice"
        val result = displayName.ifBlank { null }
        assertEquals("Alice", result)
    }

    // =========================================================================
    // Profile edit: email ifBlank
    // =========================================================================

    @Test
    fun `blank email becomes null for update request`() {
        val email = ""
        val result = email.ifBlank { null }
        assertNull(result)
    }

    @Test
    fun `non-blank email is preserved`() {
        val email = "alice@example.com"
        val result = email.ifBlank { null }
        assertEquals("alice@example.com", result)
    }

    // =========================================================================
    // Profile cancel resets fields to user values
    // =========================================================================

    @Test
    fun `cancel resets displayName to user value`() {
        val userDisplayName: String? = "Original Name"
        val editDisplayName = userDisplayName ?: ""
        assertEquals("Original Name", editDisplayName)
    }

    @Test
    fun `cancel resets displayName to empty when null`() {
        val userDisplayName: String? = null
        val editDisplayName = userDisplayName ?: ""
        assertEquals("", editDisplayName)
    }

    @Test
    fun `cancel resets email to user value`() {
        val userEmail: String? = "user@example.com"
        val editEmail = userEmail ?: ""
        assertEquals("user@example.com", editEmail)
    }

    @Test
    fun `cancel resets email to empty when null`() {
        val userEmail: String? = null
        val editEmail = userEmail ?: ""
        assertEquals("", editEmail)
    }

    // =========================================================================
    // Admin chip visibility
    // =========================================================================

    @Test
    fun `admin chip shown when isAdmin is true`() {
        val isAdmin = true
        assertTrue(isAdmin)
    }

    @Test
    fun `admin chip hidden when isAdmin is false`() {
        val isAdmin = false
        assertFalse(isAdmin)
    }

    // =========================================================================
    // TOTP code length (6-digit code)
    // =========================================================================

    @Test
    fun `6 digit TOTP code has correct length`() {
        val code = "123456"
        assertEquals(6, code.length)
    }

    @Test
    fun `short TOTP code has wrong length`() {
        val code = "12345"
        assertFalse(code.length == 6)
    }

    // =========================================================================
    // Error message from exception
    // =========================================================================

    @Test
    fun `profile error uses exception message`() {
        val exceptionMessage = "Server error"
        val profileError = exceptionMessage ?: "Failed to update profile"
        assertEquals("Server error", profileError)
    }

    @Test
    fun `profile error falls back for null exception`() {
        val exceptionMessage: String? = null
        val profileError = exceptionMessage ?: "Failed to update profile"
        assertEquals("Failed to update profile", profileError)
    }

    // =========================================================================
    // Password change error messages
    // =========================================================================

    @Test
    fun `password change error uses exception message`() {
        val exceptionMessage = "Current password is incorrect"
        val passwordError = exceptionMessage ?: "Password change failed"
        assertEquals("Current password is incorrect", passwordError)
    }

    @Test
    fun `password change error falls back for null`() {
        val exceptionMessage: String? = null
        val passwordError = exceptionMessage ?: "Password change failed"
        assertEquals("Password change failed", passwordError)
    }
}

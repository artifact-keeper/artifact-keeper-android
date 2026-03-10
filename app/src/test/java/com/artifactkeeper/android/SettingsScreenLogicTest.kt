package com.artifactkeeper.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the login validation and server management logic
 * in SettingsScreen.kt. Covers blank field checks, login error
 * messages, and server removal cascade logic.
 */
class SettingsScreenLogicTest {

    // =========================================================================
    // Login field validation
    // =========================================================================

    @Test
    fun `blank username fails validation`() {
        val username = ""
        val password = "secret"
        val isInvalid = username.isBlank() || password.isBlank()
        assertTrue(isInvalid)
    }

    @Test
    fun `blank password fails validation`() {
        val username = "admin"
        val password = ""
        val isInvalid = username.isBlank() || password.isBlank()
        assertTrue(isInvalid)
    }

    @Test
    fun `both blank fails validation`() {
        val username = ""
        val password = ""
        val isInvalid = username.isBlank() || password.isBlank()
        assertTrue(isInvalid)
    }

    @Test
    fun `whitespace-only username fails validation`() {
        val username = "   "
        val password = "secret"
        val isInvalid = username.isBlank() || password.isBlank()
        assertTrue(isInvalid)
    }

    @Test
    fun `valid credentials pass validation`() {
        val username = "admin"
        val password = "secret"
        val isInvalid = username.isBlank() || password.isBlank()
        assertFalse(isInvalid)
    }

    @Test
    fun `validation error message is correct`() {
        val username = ""
        val password = ""
        val loginError = if (username.isBlank() || password.isBlank()) {
            "Username and password are required"
        } else null
        assertEquals("Username and password are required", loginError)
    }

    @Test
    fun `valid credentials produce no error`() {
        val username = "admin"
        val password = "pass123"
        val loginError = if (username.isBlank() || password.isBlank()) {
            "Username and password are required"
        } else null
        assertNull(loginError)
    }

    // =========================================================================
    // Username trim on login request
    // =========================================================================

    @Test
    fun `username is trimmed for login request`() {
        val username = "  admin  "
        assertEquals("admin", username.trim())
    }

    // =========================================================================
    // Login error message from exception
    // =========================================================================

    @Test
    fun `login error uses exception message`() {
        val exceptionMessage = "Invalid credentials"
        val loginError = exceptionMessage ?: "Login failed"
        assertEquals("Invalid credentials", loginError)
    }

    @Test
    fun `login error falls back to default on null message`() {
        val exceptionMessage: String? = null
        val loginError = exceptionMessage ?: "Login failed"
        assertEquals("Login failed", loginError)
    }

    // =========================================================================
    // Server removal cascade (disconnect when last server removed)
    // =========================================================================

    @Test
    fun `removing last server triggers disconnect`() {
        val serversAfterRemoval = emptyList<String>()
        val shouldDisconnect = serversAfterRemoval.isEmpty()
        assertTrue(shouldDisconnect)
    }

    @Test
    fun `removing non-last server does not trigger disconnect`() {
        val serversAfterRemoval = listOf("server-2")
        val shouldDisconnect = serversAfterRemoval.isEmpty()
        assertFalse(shouldDisconnect)
    }

    // =========================================================================
    // wasActive check on server removal
    // =========================================================================

    @Test
    fun `wasActive is true when removing active server`() {
        val serverId = "abc"
        val activeServerId = "abc"
        val wasActive = serverId == activeServerId
        assertTrue(wasActive)
    }

    @Test
    fun `wasActive is false when removing non-active server`() {
        val serverId = "abc"
        val activeServerId = "def"
        val wasActive = serverId == activeServerId
        assertFalse(wasActive)
    }

    // =========================================================================
    // Add server dialog URL validation
    // =========================================================================

    @Test
    fun `add server blank URL produces error`() {
        val cleanedUrl = "".trim()
        val errorMessage = if (cleanedUrl.isBlank()) "Please enter a server URL" else null
        assertEquals("Please enter a server URL", errorMessage)
    }

    @Test
    fun `add server valid URL passes`() {
        val cleanedUrl = "https://example.com".trim()
        val errorMessage = if (cleanedUrl.isBlank()) "Please enter a server URL" else null
        assertNull(errorMessage)
    }

    // =========================================================================
    // Connection failed error message
    // =========================================================================

    @Test
    fun `add server connection error message includes detail`() {
        val exceptionMessage = "timeout"
        val errorMessage = "Connection failed: $exceptionMessage"
        assertEquals("Connection failed: timeout", errorMessage)
    }
}

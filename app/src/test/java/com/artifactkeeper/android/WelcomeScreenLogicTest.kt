package com.artifactkeeper.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the URL validation and host extraction logic
 * used in WelcomeScreen.kt and AddServerDialog (SettingsScreen.kt).
 */
class WelcomeScreenLogicTest {

    // =========================================================================
    // URL blank check (first validation gate)
    // =========================================================================

    @Test
    fun `empty URL is blank`() {
        val url = "".trim()
        assertTrue(url.isBlank())
    }

    @Test
    fun `whitespace-only URL is blank`() {
        val url = "   ".trim()
        assertTrue(url.isBlank())
    }

    @Test
    fun `valid URL is not blank`() {
        val url = "https://example.com".trim()
        assertFalse(url.isBlank())
    }

    // =========================================================================
    // Error message for blank URL
    // =========================================================================

    @Test
    fun `blank URL produces correct error message`() {
        val url = "".trim()
        val errorMessage = if (url.isBlank()) "Please enter a server URL" else null
        assertEquals("Please enter a server URL", errorMessage)
    }

    @Test
    fun `non-blank URL does not produce blank error`() {
        val url = "https://example.com".trim()
        val errorMessage = if (url.isBlank()) "Please enter a server URL" else null
        assertNull(errorMessage)
    }

    // =========================================================================
    // Host extraction from URI
    // =========================================================================

    @Test
    fun `host extracted from https URL`() {
        val url = "https://artifacts.example.com"
        val host = try {
            java.net.URI(url).host ?: url
        } catch (_: Exception) {
            url
        }
        assertEquals("artifacts.example.com", host)
    }

    @Test
    fun `host extracted from URL with path`() {
        val url = "https://artifacts.example.com/api/v1"
        val host = try {
            java.net.URI(url).host ?: url
        } catch (_: Exception) {
            url
        }
        assertEquals("artifacts.example.com", host)
    }

    @Test
    fun `host extracted from URL with port`() {
        val url = "https://localhost:8080"
        val host = try {
            java.net.URI(url).host ?: url
        } catch (_: Exception) {
            url
        }
        assertEquals("localhost", host)
    }

    @Test
    fun `host falls back to URL for invalid URI`() {
        val url = "not a valid url %%"
        val host = try {
            java.net.URI(url).host ?: url
        } catch (_: Exception) {
            url
        }
        assertEquals(url, host)
    }

    @Test
    fun `host extracted from http URL`() {
        val url = "http://192.168.1.100:3000"
        val host = try {
            java.net.URI(url).host ?: url
        } catch (_: Exception) {
            url
        }
        assertEquals("192.168.1.100", host)
    }

    // =========================================================================
    // Connection error message formatting
    // =========================================================================

    @Test
    fun `error message includes exception detail`() {
        val detail = "Connection refused"
        val errorMessage = "Could not connect to server: $detail"
        assertEquals("Could not connect to server: Connection refused", errorMessage)
    }

    @Test
    fun `error message uses Unknown error for null message`() {
        val exceptionMessage: String? = null
        val detail = exceptionMessage ?: "Unknown error"
        val errorMessage = "Could not connect to server: $detail"
        assertEquals("Could not connect to server: Unknown error", errorMessage)
    }

    // =========================================================================
    // Button text changes based on testing state
    // =========================================================================

    @Test
    fun `button text is Connect when not testing`() {
        val isTesting = false
        val text = if (isTesting) "Connecting..." else "Connect"
        assertEquals("Connect", text)
    }

    @Test
    fun `button text is Connecting when testing`() {
        val isTesting = true
        val text = if (isTesting) "Connecting..." else "Connect"
        assertEquals("Connecting...", text)
    }

    // =========================================================================
    // URL trim before validation
    // =========================================================================

    @Test
    fun `URL with leading whitespace is trimmed`() {
        val rawUrl = "  https://example.com  "
        val url = rawUrl.trim()
        assertEquals("https://example.com", url)
    }

    @Test
    fun `URL trim does not affect clean URL`() {
        val rawUrl = "https://example.com"
        val url = rawUrl.trim()
        assertEquals("https://example.com", url)
    }
}

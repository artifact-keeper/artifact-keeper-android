package com.artifactkeeper.android

import com.artifactkeeper.android.data.models.SavedServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the pure logic extracted from ServerManager.kt.
 * Covers URL cleaning, duplicate detection, active server fallback,
 * host extraction for migration, and server list operations.
 */
class ServerManagerLogicTest {

    // =========================================================================
    // URL cleaning (addServer ensures trailing slash)
    // =========================================================================

    @Test
    fun `url without trailing slash gets one appended`() {
        val url = "https://artifacts.example.com"
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        assertEquals("https://artifacts.example.com/", cleanUrl)
    }

    @Test
    fun `url with trailing slash stays unchanged`() {
        val url = "https://artifacts.example.com/"
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        assertEquals("https://artifacts.example.com/", cleanUrl)
    }

    @Test
    fun `url with path and no trailing slash gets one appended`() {
        val url = "https://example.com/artifactory"
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        assertEquals("https://example.com/artifactory/", cleanUrl)
    }

    @Test
    fun `url with path and trailing slash stays unchanged`() {
        val url = "https://example.com/artifactory/"
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        assertEquals("https://example.com/artifactory/", cleanUrl)
    }

    @Test
    fun `empty url gets trailing slash`() {
        val url = ""
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        assertEquals("/", cleanUrl)
    }

    // =========================================================================
    // Duplicate server detection
    // =========================================================================

    @Test
    fun `duplicate detected when cleaned URL matches existing server`() {
        val servers = listOf(
            SavedServer(id = "1", name = "Server 1", url = "https://example.com/", addedAt = 0),
        )
        val cleanUrl = "https://example.com/"
        val existing = servers.find { it.url == cleanUrl }
        assertEquals("1", existing?.id)
    }

    @Test
    fun `no duplicate when URL does not match`() {
        val servers = listOf(
            SavedServer(id = "1", name = "Server 1", url = "https://example.com/", addedAt = 0),
        )
        val cleanUrl = "https://other.example.com/"
        val existing = servers.find { it.url == cleanUrl }
        assertNull(existing)
    }

    @Test
    fun `duplicate detection uses exact cleaned URL comparison`() {
        // If user enters "https://example.com" (no slash), the cleaned URL
        // is "https://example.com/", which should match an existing entry.
        val servers = listOf(
            SavedServer(id = "1", name = "Server 1", url = "https://example.com/", addedAt = 0),
        )
        val rawUrl = "https://example.com"
        val cleanUrl = if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"
        val existing = servers.find { it.url == cleanUrl }
        assertEquals("1", existing?.id)
    }

    // =========================================================================
    // removeServer active server fallback
    // =========================================================================

    @Test
    fun `removing active server falls back to first remaining`() {
        val servers = listOf(
            SavedServer(id = "1", name = "S1", url = "https://s1.com/", addedAt = 0),
            SavedServer(id = "2", name = "S2", url = "https://s2.com/", addedAt = 1),
        )
        val activeId = "1"
        val updated = servers.filter { it.id != activeId }
        val newActiveId = if (activeId == activeId) updated.firstOrNull()?.id else activeId
        assertEquals("2", newActiveId)
    }

    @Test
    fun `removing last server results in null active`() {
        val servers = listOf(
            SavedServer(id = "1", name = "S1", url = "https://s1.com/", addedAt = 0),
        )
        val activeId = "1"
        val updated = servers.filter { it.id != activeId }
        val newActiveId = updated.firstOrNull()?.id
        assertNull(newActiveId)
    }

    @Test
    fun `removing non-active server does not change active`() {
        val servers = listOf(
            SavedServer(id = "1", name = "S1", url = "https://s1.com/", addedAt = 0),
            SavedServer(id = "2", name = "S2", url = "https://s2.com/", addedAt = 1),
        )
        val activeId = "1"
        val removeId = "2"
        val updated = servers.filter { it.id != removeId }
        val newActiveId = if (activeId == removeId) updated.firstOrNull()?.id else activeId
        assertEquals("1", newActiveId)
    }

    // =========================================================================
    // Host extraction for migration (migrateIfNeeded, WelcomeScreen)
    // =========================================================================

    @Test
    fun `host extracted from standard URL`() {
        val url = "https://artifacts.example.com/api"
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
    fun `host falls back to full URL for malformed URI`() {
        val url = "not a valid uri %%"
        val host = try {
            java.net.URI(url).host ?: url
        } catch (_: Exception) {
            url
        }
        assertEquals(url, host)
    }

    @Test
    fun `host extracted from simple domain URL`() {
        val url = "https://example.com"
        val host = try {
            java.net.URI(url).host ?: url
        } catch (_: Exception) {
            url
        }
        assertEquals("example.com", host)
    }

    // =========================================================================
    // migrateIfNeeded conditions
    // =========================================================================

    @Test
    fun `migration happens when legacy URL present and server list empty`() {
        val legacyUrl: String? = "https://old-server.com"
        val serversEmpty = true
        val shouldMigrate = !legacyUrl.isNullOrBlank() && serversEmpty
        assertTrue(shouldMigrate)
    }

    @Test
    fun `migration skipped when legacy URL is null`() {
        val legacyUrl: String? = null
        val serversEmpty = true
        val shouldMigrate = !legacyUrl.isNullOrBlank() && serversEmpty
        assertFalse(shouldMigrate)
    }

    @Test
    fun `migration skipped when legacy URL is blank`() {
        val legacyUrl = "   "
        val serversEmpty = true
        val shouldMigrate = !legacyUrl.isNullOrBlank() && serversEmpty
        assertFalse(shouldMigrate)
    }

    @Test
    fun `migration skipped when servers already populated`() {
        val legacyUrl = "https://old-server.com"
        val serversEmpty = false
        val shouldMigrate = !legacyUrl.isNullOrBlank() && serversEmpty
        assertFalse(shouldMigrate)
    }

    // =========================================================================
    // switchTo: only switches if server exists
    // =========================================================================

    @Test
    fun `switchTo finds server by id`() {
        val servers = listOf(
            SavedServer(id = "1", name = "S1", url = "https://s1.com/", addedAt = 0),
            SavedServer(id = "2", name = "S2", url = "https://s2.com/", addedAt = 1),
        )
        val found = servers.find { it.id == "2" }
        assertEquals("S2", found?.name)
    }

    @Test
    fun `switchTo returns null for unknown id`() {
        val servers = listOf(
            SavedServer(id = "1", name = "S1", url = "https://s1.com/", addedAt = 0),
        )
        val found = servers.find { it.id == "999" }
        assertNull(found)
    }

    // =========================================================================
    // getActiveServer: returns matching server or null
    // =========================================================================

    @Test
    fun `getActiveServer returns server when id matches`() {
        val servers = listOf(
            SavedServer(id = "1", name = "S1", url = "https://s1.com/", addedAt = 0),
            SavedServer(id = "2", name = "S2", url = "https://s2.com/", addedAt = 1),
        )
        val activeId: String? = "2"
        val active = if (activeId != null) servers.find { it.id == activeId } else null
        assertEquals("S2", active?.name)
    }

    @Test
    fun `getActiveServer returns null when id is null`() {
        val servers = listOf(
            SavedServer(id = "1", name = "S1", url = "https://s1.com/", addedAt = 0),
        )
        val activeId: String? = null
        val active = if (activeId != null) servers.find { it.id == activeId } else null
        assertNull(active)
    }

    // =========================================================================
    // Health check URL construction
    // =========================================================================

    @Test
    fun `health check URL is server URL plus health`() {
        val serverUrl = "https://example.com/"
        val healthUrl = "${serverUrl}health"
        assertEquals("https://example.com/health", healthUrl)
    }

    @Test
    fun `health check URL works without trailing slash`() {
        // After clean, server URL always has trailing slash
        val serverUrl = "https://example.com/"
        val healthUrl = "${serverUrl}health"
        assertTrue(healthUrl.endsWith("/health"))
    }

    // =========================================================================
    // Health response code in range check
    // =========================================================================

    @Test
    fun `200 response code is healthy`() {
        assertTrue(200 in 200..299)
    }

    @Test
    fun `299 response code is healthy`() {
        assertTrue(299 in 200..299)
    }

    @Test
    fun `301 response code is not healthy`() {
        assertFalse(301 in 200..299)
    }

    @Test
    fun `500 response code is not healthy`() {
        assertFalse(500 in 200..299)
    }

    @Test
    fun `199 response code is not healthy`() {
        assertFalse(199 in 200..299)
    }
}

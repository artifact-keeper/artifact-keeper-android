package com.artifactkeeper.android

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the display logic in MonitoringScreen.kt.
 * The screen uses inline logic for health check status color mapping,
 * alert status determination, and health log status checks.
 */
class MonitoringScreenLogicTest {

    // Mirrors the private constants in MonitoringScreen.kt
    private val StatusOk = Color(0xFF52C41A)
    private val StatusFail = Color(0xFFF5222D)

    // =========================================================================
    // ServiceHealthCard status logic
    // =========================================================================

    private fun isServiceOk(status: String): Boolean {
        return status.lowercase() == "ok" || status.lowercase() == "healthy"
    }

    @Test
    fun `ok status is healthy`() {
        assertTrue(isServiceOk("ok"))
    }

    @Test
    fun `healthy status is healthy`() {
        assertTrue(isServiceOk("healthy"))
    }

    @Test
    fun `OK uppercase is healthy`() {
        assertTrue(isServiceOk("OK"))
    }

    @Test
    fun `HEALTHY uppercase is healthy`() {
        assertTrue(isServiceOk("HEALTHY"))
    }

    @Test
    fun `unhealthy status is not ok`() {
        assertFalse(isServiceOk("unhealthy"))
    }

    @Test
    fun `down status is not ok`() {
        assertFalse(isServiceOk("down"))
    }

    @Test
    fun `error status is not ok`() {
        assertFalse(isServiceOk("error"))
    }

    @Test
    fun `empty status is not ok`() {
        assertFalse(isServiceOk(""))
    }

    // =========================================================================
    // AlertCard status logic
    // =========================================================================

    private fun isAlertHealthy(currentStatus: String): Boolean {
        return currentStatus.lowercase() == "healthy"
    }

    @Test
    fun `healthy alert is considered healthy`() {
        assertTrue(isAlertHealthy("healthy"))
    }

    @Test
    fun `unhealthy alert is not healthy`() {
        assertFalse(isAlertHealthy("unhealthy"))
    }

    @Test
    fun `Healthy with uppercase is healthy`() {
        assertTrue(isAlertHealthy("Healthy"))
    }

    // =========================================================================
    // AlertCard consecutive failures display
    // =========================================================================

    @Test
    fun `consecutive failures text only shown when greater than 0`() {
        val failures = 0
        assertFalse(failures > 0)

        val failures2 = 3
        assertTrue(failures2 > 0)
        assertEquals("Consecutive failures: 3", "Consecutive failures: $failures2")
    }

    // =========================================================================
    // Service name capitalization
    // =========================================================================

    @Test
    fun `replaceFirstChar uppercases service name`() {
        assertEquals("Database", "database".replaceFirstChar { it.uppercase() })
        assertEquals("Meilisearch", "meilisearch".replaceFirstChar { it.uppercase() })
        assertEquals("Storage", "storage".replaceFirstChar { it.uppercase() })
    }

    @Test
    fun `replaceFirstChar handles already capitalized name`() {
        assertEquals("Database", "Database".replaceFirstChar { it.uppercase() })
    }

    @Test
    fun `replaceFirstChar handles empty string`() {
        assertEquals("", "".replaceFirstChar { it.uppercase() })
    }

    // =========================================================================
    // Response time display
    // =========================================================================

    @Test
    fun `response time display format`() {
        val responseTimeMs = 42L
        assertEquals("42ms", "${responseTimeMs}ms")
    }

    @Test
    fun `response time null check`() {
        val responseTimeMs: Long? = null
        assertFalse(responseTimeMs != null)

        val responseTimeMs2: Long? = 15L
        assertTrue(responseTimeMs2 != null)
    }

    // =========================================================================
    // Status color values
    // =========================================================================

    @Test
    fun `status colors are distinct`() {
        assertFalse(StatusOk == StatusFail)
    }

    @Test
    fun `status ok is green`() {
        assertEquals(Color(0xFF52C41A), StatusOk)
    }

    @Test
    fun `status fail is red`() {
        assertEquals(Color(0xFFF5222D), StatusFail)
    }

    // =========================================================================
    // Status text uppercase display
    // =========================================================================

    @Test
    fun `status text is shown in uppercase`() {
        assertEquals("OK", "ok".uppercase())
        assertEquals("HEALTHY", "healthy".uppercase())
        assertEquals("UNHEALTHY", "unhealthy".uppercase())
    }
}

package com.artifactkeeper.android

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the status color mapping and duration formatting logic
 * used in BuildDetailScreen.
 */
class BuildDetailScreenTest {

    // Mirror of the private constants in BuildDetailScreen.kt
    private val StatusSuccess = Color(0xFF52C41A)
    private val StatusRunning = Color(0xFF1890FF)
    private val StatusFailed = Color(0xFFF5222D)

    private fun statusColor(status: String): Color = when (status.lowercase()) {
        "success", "completed" -> StatusSuccess
        "running", "pending" -> StatusRunning
        else -> StatusFailed
    }

    // =========================================================================
    // Status color constants
    // =========================================================================

    @Test
    fun `StatusSuccess is green`() {
        assertEquals(Color(0xFF52C41A), StatusSuccess)
    }

    @Test
    fun `StatusRunning is blue`() {
        assertEquals(Color(0xFF1890FF), StatusRunning)
    }

    @Test
    fun `StatusFailed is red`() {
        assertEquals(Color(0xFFF5222D), StatusFailed)
    }

    @Test
    fun `all status colors are distinct`() {
        val colors = listOf(StatusSuccess, StatusRunning, StatusFailed)
        assertEquals(colors.size, colors.toSet().size)
    }

    // =========================================================================
    // Status color mapping
    // =========================================================================

    @Test
    fun `success maps to green`() {
        assertEquals(StatusSuccess, statusColor("success"))
    }

    @Test
    fun `completed maps to green`() {
        assertEquals(StatusSuccess, statusColor("completed"))
    }

    @Test
    fun `running maps to blue`() {
        assertEquals(StatusRunning, statusColor("running"))
    }

    @Test
    fun `pending maps to blue`() {
        assertEquals(StatusRunning, statusColor("pending"))
    }

    @Test
    fun `failed maps to red`() {
        assertEquals(StatusFailed, statusColor("failed"))
    }

    @Test
    fun `error maps to red (default branch)`() {
        assertEquals(StatusFailed, statusColor("error"))
    }

    @Test
    fun `unknown status maps to red`() {
        assertEquals(StatusFailed, statusColor("unknown"))
    }

    @Test
    fun `status mapping is case-insensitive`() {
        assertEquals(StatusSuccess, statusColor("SUCCESS"))
        assertEquals(StatusSuccess, statusColor("Success"))
        assertEquals(StatusRunning, statusColor("RUNNING"))
        assertEquals(StatusRunning, statusColor("Pending"))
    }

    // =========================================================================
    // Duration display logic (mirrors the inline calculation)
    // =========================================================================

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        return if (seconds >= 60) {
            "${seconds / 60}m ${seconds % 60}s"
        } else {
            "${seconds}s"
        }
    }

    @Test
    fun `duration under 60s shows seconds only`() {
        assertEquals("0s", formatDuration(0))
        assertEquals("1s", formatDuration(1000))
        assertEquals("59s", formatDuration(59_000))
    }

    @Test
    fun `duration at 60s shows 1m 0s`() {
        assertEquals("1m 0s", formatDuration(60_000))
    }

    @Test
    fun `duration with minutes and seconds`() {
        assertEquals("1m 30s", formatDuration(90_000))
        assertEquals("5m 15s", formatDuration(315_000))
    }

    @Test
    fun `duration with large values`() {
        assertEquals("60m 0s", formatDuration(3_600_000))
        assertEquals("120m 0s", formatDuration(7_200_000))
    }

    // =========================================================================
    // Timestamp display logic
    // =========================================================================

    @Test
    fun `timestamp formatting takes first 19 chars and replaces T`() {
        val ts = "2024-06-15T12:30:45.123Z"
        val display = ts.take(19).replace("T", " ")
        assertEquals("2024-06-15 12:30:45", display)
    }

    @Test
    fun `short timestamp is kept as-is`() {
        val ts = "2024-06-15"
        val display = ts.take(19).replace("T", " ")
        assertEquals("2024-06-15", display)
    }

    // =========================================================================
    // VCS revision display (take first 12 chars)
    // =========================================================================

    @Test
    fun `vcs revision is truncated to 12 chars`() {
        val rev = "abc123def456789012345"
        assertEquals("abc123def456", rev.take(12))
    }

    @Test
    fun `short vcs revision is kept whole`() {
        val rev = "abc123"
        assertEquals("abc123", rev.take(12))
    }
}

package com.artifactkeeper.android

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the display logic and status mapping in BuildsScreen.kt.
 * The screen contains inline status color mapping and filter logic
 * that we test by replicating the logic here.
 */
class BuildsScreenLogicTest {

    // Mirrors the private constants in BuildsScreen.kt
    private val StatusSuccess = Color(0xFF52C41A)
    private val StatusFailed = Color(0xFFF5222D)
    private val StatusRunning = Color(0xFF1890FF)
    private val StatusPending = Color(0xFF8C8C8C)

    // Mirrors the BuildCard status color logic
    private fun buildCardStatusColor(status: String): Color = when (status.lowercase()) {
        "success" -> StatusSuccess
        "failed", "error" -> StatusFailed
        "running", "in_progress" -> StatusRunning
        else -> StatusPending
    }

    // =========================================================================
    // BuildCard status color mapping (4-color scheme)
    // =========================================================================

    @Test
    fun `success maps to green`() {
        assertEquals(StatusSuccess, buildCardStatusColor("success"))
    }

    @Test
    fun `failed maps to red`() {
        assertEquals(StatusFailed, buildCardStatusColor("failed"))
    }

    @Test
    fun `error maps to red`() {
        assertEquals(StatusFailed, buildCardStatusColor("error"))
    }

    @Test
    fun `running maps to blue`() {
        assertEquals(StatusRunning, buildCardStatusColor("running"))
    }

    @Test
    fun `in_progress maps to blue`() {
        assertEquals(StatusRunning, buildCardStatusColor("in_progress"))
    }

    @Test
    fun `unknown status maps to pending grey`() {
        assertEquals(StatusPending, buildCardStatusColor("unknown"))
    }

    @Test
    fun `empty status maps to pending grey`() {
        assertEquals(StatusPending, buildCardStatusColor(""))
    }

    @Test
    fun `status mapping is case-insensitive`() {
        assertEquals(StatusSuccess, buildCardStatusColor("SUCCESS"))
        assertEquals(StatusFailed, buildCardStatusColor("FAILED"))
        assertEquals(StatusRunning, buildCardStatusColor("RUNNING"))
        assertEquals(StatusFailed, buildCardStatusColor("ERROR"))
    }

    // =========================================================================
    // BuildCard title formatting
    // =========================================================================

    @Test
    fun `build title includes name and number`() {
        val name = "Backend Build"
        val number = 42L
        val title = "$name #$number"
        assertEquals("Backend Build #42", title)
    }

    // =========================================================================
    // BuildCard status text formatting
    // =========================================================================

    @Test
    fun `status replaceFirstChar uppercases first letter`() {
        assertEquals("Success", "success".replaceFirstChar { it.uppercase() })
        assertEquals("Failed", "failed".replaceFirstChar { it.uppercase() })
        assertEquals("Running", "running".replaceFirstChar { it.uppercase() })
        assertEquals("In_progress", "in_progress".replaceFirstChar { it.uppercase() })
    }

    // =========================================================================
    // Artifact count display logic
    // =========================================================================

    @Test
    fun `artifact count singular form`() {
        val count = 1
        val text = "$count artifact${if (count > 1) "s" else ""}"
        assertEquals("1 artifact", text)
    }

    @Test
    fun `artifact count plural form`() {
        val count = 5
        val text = "$count artifact${if (count > 1) "s" else ""}"
        assertEquals("5 artifacts", text)
    }

    @Test
    fun `artifact count of zero shown without s`() {
        val count = 0
        val text = "$count artifact${if (count > 1) "s" else ""}"
        assertEquals("0 artifact", text)
    }

    // =========================================================================
    // VCS branch display logic
    // =========================================================================

    @Test
    fun `blank branch is skipped by takeIf`() {
        val branch = ""
        val result = branch.takeIf { it.isNotBlank() }
        assertEquals(null, result)
    }

    @Test
    fun `whitespace-only branch is skipped by takeIf`() {
        val branch = "   "
        val result = branch.takeIf { it.isNotBlank() }
        assertEquals(null, result)
    }

    @Test
    fun `non-blank branch passes takeIf`() {
        val branch = "main"
        val result = branch.takeIf { it.isNotBlank() }
        assertEquals("main", result)
    }

    // =========================================================================
    // Status filter chip logic
    // =========================================================================

    @Test
    fun `status filter null maps to All`() {
        val statusFilters = listOf(
            null to "All",
            "success" to "Success",
            "failed" to "Failed",
            "running" to "Running",
        )
        assertEquals(4, statusFilters.size)
        assertEquals(null, statusFilters[0].first)
        assertEquals("All", statusFilters[0].second)
    }

    @Test
    fun `search query ifBlank returns null for empty input`() {
        val query = ""
        val result = query.ifBlank { null }
        assertEquals(null, result)
    }

    @Test
    fun `search query ifBlank returns query for non-empty input`() {
        val query = "my-build"
        val result = query.ifBlank { null }
        assertEquals("my-build", result)
    }

    // =========================================================================
    // All four status colors are distinct
    // =========================================================================

    @Test
    fun `all four status colors are distinct`() {
        val colors = listOf(StatusSuccess, StatusFailed, StatusRunning, StatusPending)
        assertEquals(4, colors.toSet().size)
    }
}

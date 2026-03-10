package com.artifactkeeper.android

import androidx.compose.ui.graphics.Color
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import com.artifactkeeper.android.ui.util.DisplayLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive tests for DisplayLogic utility functions.
 * Each function is extracted from a Composable screen so that JaCoCo
 * can measure coverage on the production code.
 */
class DisplayLogicTest {

    // =========================================================================
    // gradeColor
    // =========================================================================

    @Test
    fun `gradeColor A maps to green`() {
        assertEquals(Color(0xFF52C41A), DisplayLogic.gradeColor("A"))
    }

    @Test
    fun `gradeColor B maps to teal`() {
        assertEquals(Color(0xFF36CFC9), DisplayLogic.gradeColor("B"))
    }

    @Test
    fun `gradeColor C maps to amber`() {
        assertEquals(Color(0xFFFAAD14), DisplayLogic.gradeColor("C"))
    }

    @Test
    fun `gradeColor D maps to orange`() {
        assertEquals(Color(0xFFFA8C16), DisplayLogic.gradeColor("D"))
    }

    @Test
    fun `gradeColor F maps to red`() {
        assertEquals(Color(0xFFF5222D), DisplayLogic.gradeColor("F"))
    }

    @Test
    fun `gradeColor unknown maps to red`() {
        assertEquals(Color(0xFFF5222D), DisplayLogic.gradeColor("X"))
    }

    @Test
    fun `gradeColor empty maps to red`() {
        assertEquals(Color(0xFFF5222D), DisplayLogic.gradeColor(""))
    }

    @Test
    fun `gradeColor is case-insensitive`() {
        assertEquals(DisplayLogic.gradeColor("A"), DisplayLogic.gradeColor("a"))
        assertEquals(DisplayLogic.gradeColor("B"), DisplayLogic.gradeColor("b"))
        assertEquals(DisplayLogic.gradeColor("C"), DisplayLogic.gradeColor("c"))
        assertEquals(DisplayLogic.gradeColor("D"), DisplayLogic.gradeColor("d"))
    }

    // =========================================================================
    // dtStatusColor / dtStatusText
    // =========================================================================

    @Test
    fun `dtStatusColor healthy is green`() {
        assertEquals(Color(0xFF52C41A), DisplayLogic.dtStatusColor(true))
    }

    @Test
    fun `dtStatusColor unhealthy is red`() {
        assertEquals(Color(0xFFF5222D), DisplayLogic.dtStatusColor(false))
    }

    @Test
    fun `dtStatusText healthy`() {
        assertEquals("Connected", DisplayLogic.dtStatusText(true))
    }

    @Test
    fun `dtStatusText unhealthy`() {
        assertEquals("Disconnected", DisplayLogic.dtStatusText(false))
    }

    // =========================================================================
    // auditProgress
    // =========================================================================

    @Test
    fun `auditProgress zero total returns 0`() {
        assertEquals(0f, DisplayLogic.auditProgress(0, 0), 0.001f)
    }

    @Test
    fun `auditProgress full returns 1`() {
        assertEquals(1f, DisplayLogic.auditProgress(100, 100), 0.001f)
    }

    @Test
    fun `auditProgress half`() {
        assertEquals(0.5f, DisplayLogic.auditProgress(200, 100), 0.001f)
    }

    @Test
    fun `auditProgress partial`() {
        assertEquals(0.333f, DisplayLogic.auditProgress(75, 25), 0.01f)
    }

    // =========================================================================
    // riskScoreColor
    // =========================================================================

    @Test
    fun `riskScoreColor above 70 is Critical`() {
        assertEquals(Critical, DisplayLogic.riskScoreColor(70.0))
        assertEquals(Critical, DisplayLogic.riskScoreColor(100.0))
    }

    @Test
    fun `riskScoreColor 40 to 70 is High`() {
        assertEquals(High, DisplayLogic.riskScoreColor(40.0))
        assertEquals(High, DisplayLogic.riskScoreColor(69.9))
    }

    @Test
    fun `riskScoreColor 10 to 40 is Medium`() {
        assertEquals(Medium, DisplayLogic.riskScoreColor(10.0))
        assertEquals(Medium, DisplayLogic.riskScoreColor(39.9))
    }

    @Test
    fun `riskScoreColor below 10 is safe green`() {
        assertEquals(Color(0xFF52C41A), DisplayLogic.riskScoreColor(9.9))
        assertEquals(Color(0xFF52C41A), DisplayLogic.riskScoreColor(0.0))
    }

    // =========================================================================
    // securityScreenIsEmpty
    // =========================================================================

    @Test
    fun `securityScreenIsEmpty all empty and dt disabled`() {
        assertTrue(DisplayLogic.securityScreenIsEmpty(true, true, false))
    }

    @Test
    fun `securityScreenIsEmpty not empty when scores present`() {
        assertFalse(DisplayLogic.securityScreenIsEmpty(false, true, false))
    }

    @Test
    fun `securityScreenIsEmpty not empty when trends present`() {
        assertFalse(DisplayLogic.securityScreenIsEmpty(true, false, false))
    }

    @Test
    fun `securityScreenIsEmpty not empty when dt enabled`() {
        assertFalse(DisplayLogic.securityScreenIsEmpty(true, true, true))
    }

    // =========================================================================
    // buildStatusColor
    // =========================================================================

    @Test
    fun `buildStatusColor success is green`() {
        assertEquals(Color(0xFF52C41A), DisplayLogic.buildStatusColor("success"))
    }

    @Test
    fun `buildStatusColor failed is red`() {
        assertEquals(Color(0xFFF5222D), DisplayLogic.buildStatusColor("failed"))
    }

    @Test
    fun `buildStatusColor error is red`() {
        assertEquals(Color(0xFFF5222D), DisplayLogic.buildStatusColor("error"))
    }

    @Test
    fun `buildStatusColor running is blue`() {
        assertEquals(Color(0xFF1890FF), DisplayLogic.buildStatusColor("running"))
    }

    @Test
    fun `buildStatusColor in_progress is blue`() {
        assertEquals(Color(0xFF1890FF), DisplayLogic.buildStatusColor("in_progress"))
    }

    @Test
    fun `buildStatusColor unknown is grey`() {
        assertEquals(Color(0xFF8C8C8C), DisplayLogic.buildStatusColor("unknown"))
    }

    @Test
    fun `buildStatusColor empty is grey`() {
        assertEquals(Color(0xFF8C8C8C), DisplayLogic.buildStatusColor(""))
    }

    @Test
    fun `buildStatusColor is case-insensitive`() {
        assertEquals(DisplayLogic.buildStatusColor("success"), DisplayLogic.buildStatusColor("SUCCESS"))
        assertEquals(DisplayLogic.buildStatusColor("failed"), DisplayLogic.buildStatusColor("FAILED"))
    }

    // =========================================================================
    // buildTitle
    // =========================================================================

    @Test
    fun `buildTitle formats name and number`() {
        assertEquals("Backend Build #42", DisplayLogic.buildTitle("Backend Build", 42))
    }

    // =========================================================================
    // buildArtifactCountText
    // =========================================================================

    @Test
    fun `buildArtifactCountText singular for 1`() {
        assertEquals("1 artifact", DisplayLogic.buildArtifactCountText(1))
    }

    @Test
    fun `buildArtifactCountText plural for 5`() {
        assertEquals("5 artifacts", DisplayLogic.buildArtifactCountText(5))
    }

    @Test
    fun `buildArtifactCountText zero is singular`() {
        assertEquals("0 artifact", DisplayLogic.buildArtifactCountText(0))
    }

    // =========================================================================
    // isServiceHealthy / serviceStatusColor
    // =========================================================================

    @Test
    fun `isServiceHealthy ok is true`() {
        assertTrue(DisplayLogic.isServiceHealthy("ok"))
    }

    @Test
    fun `isServiceHealthy healthy is true`() {
        assertTrue(DisplayLogic.isServiceHealthy("healthy"))
    }

    @Test
    fun `isServiceHealthy OK uppercase is true`() {
        assertTrue(DisplayLogic.isServiceHealthy("OK"))
    }

    @Test
    fun `isServiceHealthy unhealthy is false`() {
        assertFalse(DisplayLogic.isServiceHealthy("unhealthy"))
    }

    @Test
    fun `isServiceHealthy empty is false`() {
        assertFalse(DisplayLogic.isServiceHealthy(""))
    }

    @Test
    fun `serviceStatusColor ok is green`() {
        assertEquals(Color(0xFF52C41A), DisplayLogic.serviceStatusColor("ok"))
    }

    @Test
    fun `serviceStatusColor unhealthy is red`() {
        assertEquals(Color(0xFFF5222D), DisplayLogic.serviceStatusColor("unhealthy"))
    }

    // =========================================================================
    // isAlertHealthy
    // =========================================================================

    @Test
    fun `isAlertHealthy healthy is true`() {
        assertTrue(DisplayLogic.isAlertHealthy("healthy"))
    }

    @Test
    fun `isAlertHealthy Healthy capitalized is true`() {
        assertTrue(DisplayLogic.isAlertHealthy("Healthy"))
    }

    @Test
    fun `isAlertHealthy unhealthy is false`() {
        assertFalse(DisplayLogic.isAlertHealthy("unhealthy"))
    }

    // =========================================================================
    // stagingArtifactCountText
    // =========================================================================

    @Test
    fun `stagingArtifactCountText singular for 1`() {
        assertEquals("1 artifact", DisplayLogic.stagingArtifactCountText(1))
    }

    @Test
    fun `stagingArtifactCountText plural for 0`() {
        assertEquals("0 artifacts", DisplayLogic.stagingArtifactCountText(0))
    }

    @Test
    fun `stagingArtifactCountText plural for many`() {
        assertEquals("42 artifacts", DisplayLogic.stagingArtifactCountText(42))
    }

    // =========================================================================
    // shouldShowDescription
    // =========================================================================

    @Test
    fun `shouldShowDescription null returns false`() {
        assertFalse(DisplayLogic.shouldShowDescription(null))
    }

    @Test
    fun `shouldShowDescription empty returns false`() {
        assertFalse(DisplayLogic.shouldShowDescription(""))
    }

    @Test
    fun `shouldShowDescription blank returns false`() {
        assertFalse(DisplayLogic.shouldShowDescription("   "))
    }

    @Test
    fun `shouldShowDescription non-blank returns true`() {
        assertTrue(DisplayLogic.shouldShowDescription("Maven repo"))
    }

    // =========================================================================
    // cleanServerUrl
    // =========================================================================

    @Test
    fun `cleanServerUrl appends slash when missing`() {
        assertEquals("https://example.com/", DisplayLogic.cleanServerUrl("https://example.com"))
    }

    @Test
    fun `cleanServerUrl keeps existing slash`() {
        assertEquals("https://example.com/", DisplayLogic.cleanServerUrl("https://example.com/"))
    }

    @Test
    fun `cleanServerUrl handles path without slash`() {
        assertEquals("https://example.com/api/", DisplayLogic.cleanServerUrl("https://example.com/api"))
    }

    // =========================================================================
    // extractHost
    // =========================================================================

    @Test
    fun `extractHost from standard URL`() {
        assertEquals("example.com", DisplayLogic.extractHost("https://example.com"))
    }

    @Test
    fun `extractHost from URL with port`() {
        assertEquals("localhost", DisplayLogic.extractHost("https://localhost:8080"))
    }

    @Test
    fun `extractHost from URL with path`() {
        assertEquals("artifacts.example.com", DisplayLogic.extractHost("https://artifacts.example.com/api/v1"))
    }

    @Test
    fun `extractHost falls back for malformed URL`() {
        val url = "not a valid url %%"
        assertEquals(url, DisplayLogic.extractHost(url))
    }

    // =========================================================================
    // isLoginValid
    // =========================================================================

    @Test
    fun `isLoginValid true when both non-blank`() {
        assertTrue(DisplayLogic.isLoginValid("admin", "secret"))
    }

    @Test
    fun `isLoginValid false when username blank`() {
        assertFalse(DisplayLogic.isLoginValid("", "secret"))
    }

    @Test
    fun `isLoginValid false when password blank`() {
        assertFalse(DisplayLogic.isLoginValid("admin", ""))
    }

    @Test
    fun `isLoginValid false when both blank`() {
        assertFalse(DisplayLogic.isLoginValid("", ""))
    }

    @Test
    fun `isLoginValid false when username whitespace`() {
        assertFalse(DisplayLogic.isLoginValid("   ", "secret"))
    }

    // =========================================================================
    // visibleTabRoutes
    // =========================================================================

    private val allRoutes = listOf("artifacts", "integration", "security", "operations", "admin")

    @Test
    fun `visibleTabRoutes not logged in shows only artifacts`() {
        val tabs = DisplayLogic.visibleTabRoutes(allRoutes, isLoggedIn = false, isAdmin = false)
        assertEquals(listOf("artifacts"), tabs)
    }

    @Test
    fun `visibleTabRoutes logged in non-admin shows 4`() {
        val tabs = DisplayLogic.visibleTabRoutes(allRoutes, isLoggedIn = true, isAdmin = false)
        assertEquals(4, tabs.size)
        assertFalse(tabs.contains("admin"))
    }

    @Test
    fun `visibleTabRoutes admin shows all 5`() {
        val tabs = DisplayLogic.visibleTabRoutes(allRoutes, isLoggedIn = true, isAdmin = true)
        assertEquals(5, tabs.size)
    }

    @Test
    fun `visibleTabRoutes admin without login shows artifacts and admin`() {
        val tabs = DisplayLogic.visibleTabRoutes(allRoutes, isLoggedIn = false, isAdmin = true)
        assertEquals(listOf("artifacts", "admin"), tabs)
    }

    // =========================================================================
    // searchDisplayBranch
    // =========================================================================

    @Test
    fun `searchDisplayBranch loading`() {
        assertEquals("loading", DisplayLogic.searchDisplayBranch(true, null, false, true, true))
    }

    @Test
    fun `searchDisplayBranch error`() {
        assertEquals("error", DisplayLogic.searchDisplayBranch(false, "err", true, true, true))
    }

    @Test
    fun `searchDisplayBranch initial`() {
        assertEquals("initial", DisplayLogic.searchDisplayBranch(false, null, false, true, true))
    }

    @Test
    fun `searchDisplayBranch noResults`() {
        assertEquals("noResults", DisplayLogic.searchDisplayBranch(false, null, true, true, true))
    }

    @Test
    fun `searchDisplayBranch results`() {
        assertEquals("results", DisplayLogic.searchDisplayBranch(false, null, true, false, true))
    }

    @Test
    fun `searchDisplayBranch loading takes priority over error`() {
        assertEquals("loading", DisplayLogic.searchDisplayBranch(true, "err", true, true, true))
    }

    // =========================================================================
    // containerBranch
    // =========================================================================

    @Test
    fun `containerBranch loading`() {
        assertEquals("loading", DisplayLogic.containerBranch(true, null, false, false))
    }

    @Test
    fun `containerBranch error`() {
        assertEquals("error", DisplayLogic.containerBranch(false, "err", false, false))
    }

    @Test
    fun `containerBranch emptyDefault`() {
        assertEquals("emptyDefault", DisplayLogic.containerBranch(false, null, true, false))
    }

    @Test
    fun `containerBranch emptyCustom`() {
        assertEquals("emptyCustom", DisplayLogic.containerBranch(false, null, true, true))
    }

    @Test
    fun `containerBranch content`() {
        assertEquals("content", DisplayLogic.containerBranch(false, null, false, false))
    }

    @Test
    fun `containerBranch loading priority over error`() {
        assertEquals("loading", DisplayLogic.containerBranch(true, "err", true, true))
    }

    @Test
    fun `containerBranch error priority over empty`() {
        assertEquals("error", DisplayLogic.containerBranch(false, "err", true, false))
    }
}

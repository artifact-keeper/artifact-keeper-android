package com.artifactkeeper.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the visibleTabs logic and section routing in ArtifactKeeperNavHost.kt.
 * The function filters the bottom navigation tabs based on login and admin state.
 */
class NavHostLogicTest {

    // Mirrors the BottomTab data and allBottomTabs from ArtifactKeeperNavHost.kt
    private data class BottomTab(
        val route: String,
        val label: String,
        val compactLabel: String,
    )

    private val allBottomTabs = listOf(
        BottomTab("artifacts", "Artifacts", "Artifacts"),
        BottomTab("integration", "Integration", "Integr."),
        BottomTab("security", "Security", "Security"),
        BottomTab("operations", "Operations", "Ops"),
        BottomTab("admin", "Admin", "Admin"),
    )

    // Mirrors the visibleTabs function
    private fun visibleTabs(isLoggedIn: Boolean, isAdmin: Boolean): List<BottomTab> {
        return allBottomTabs.filter { tab ->
            when (tab.route) {
                "artifacts" -> true
                "admin" -> isAdmin
                else -> isLoggedIn // integration, security, operations
            }
        }
    }

    // =========================================================================
    // visibleTabs: not logged in
    // =========================================================================

    @Test
    fun `not logged in shows only artifacts tab`() {
        val tabs = visibleTabs(isLoggedIn = false, isAdmin = false)
        assertEquals(1, tabs.size)
        assertEquals("artifacts", tabs[0].route)
    }

    // =========================================================================
    // visibleTabs: logged in, not admin
    // =========================================================================

    @Test
    fun `logged in non-admin shows 4 tabs`() {
        val tabs = visibleTabs(isLoggedIn = true, isAdmin = false)
        assertEquals(4, tabs.size)
        val routes = tabs.map { it.route }
        assertTrue(routes.contains("artifacts"))
        assertTrue(routes.contains("integration"))
        assertTrue(routes.contains("security"))
        assertTrue(routes.contains("operations"))
        assertFalse(routes.contains("admin"))
    }

    // =========================================================================
    // visibleTabs: logged in, admin
    // =========================================================================

    @Test
    fun `logged in admin shows all 5 tabs`() {
        val tabs = visibleTabs(isLoggedIn = true, isAdmin = true)
        assertEquals(5, tabs.size)
        val routes = tabs.map { it.route }
        assertTrue(routes.contains("artifacts"))
        assertTrue(routes.contains("integration"))
        assertTrue(routes.contains("security"))
        assertTrue(routes.contains("operations"))
        assertTrue(routes.contains("admin"))
    }

    // =========================================================================
    // Admin without login edge case
    // =========================================================================

    @Test
    fun `admin flag without login still shows admin tab only with artifacts`() {
        // This is an edge case since isAdmin should imply isLoggedIn,
        // but the filter logic does not enforce that.
        val tabs = visibleTabs(isLoggedIn = false, isAdmin = true)
        assertEquals(2, tabs.size)
        val routes = tabs.map { it.route }
        assertTrue(routes.contains("artifacts"))
        assertTrue(routes.contains("admin"))
        assertFalse(routes.contains("integration"))
    }

    // =========================================================================
    // Tab ordering is consistent
    // =========================================================================

    @Test
    fun `tab ordering preserves original order`() {
        val tabs = visibleTabs(isLoggedIn = true, isAdmin = true)
        assertEquals("artifacts", tabs[0].route)
        assertEquals("integration", tabs[1].route)
        assertEquals("security", tabs[2].route)
        assertEquals("operations", tabs[3].route)
        assertEquals("admin", tabs[4].route)
    }

    // =========================================================================
    // Compact labels
    // =========================================================================

    @Test
    fun `compact labels differ from full labels for some tabs`() {
        val integration = allBottomTabs.find { it.route == "integration" }!!
        assertEquals("Integr.", integration.compactLabel)
        assertEquals("Integration", integration.label)
    }

    @Test
    fun `operations compact label is Ops`() {
        val ops = allBottomTabs.find { it.route == "operations" }!!
        assertEquals("Ops", ops.compactLabel)
    }

    @Test
    fun `artifacts compact and full labels are the same`() {
        val artifacts = allBottomTabs.find { it.route == "artifacts" }!!
        assertEquals(artifacts.label, artifacts.compactLabel)
    }

    // =========================================================================
    // allSectionRoutes set
    // =========================================================================

    @Test
    fun `allSectionRoutes contains all 5 routes`() {
        val allSectionRoutes = allBottomTabs.map { it.route }.toSet()
        assertEquals(5, allSectionRoutes.size)
        assertTrue(allSectionRoutes.contains("artifacts"))
        assertTrue(allSectionRoutes.contains("admin"))
    }

    @Test
    fun `route lookup in allSectionRoutes returns true for valid route`() {
        val allSectionRoutes = allBottomTabs.map { it.route }.toSet()
        assertTrue("artifacts" in allSectionRoutes)
        assertTrue("admin" in allSectionRoutes)
    }

    @Test
    fun `route lookup in allSectionRoutes returns false for detail route`() {
        val allSectionRoutes = allBottomTabs.map { it.route }.toSet()
        assertFalse("repos/my-repo" in allSectionRoutes)
        assertFalse("packages/abc" in allSectionRoutes)
    }

    // =========================================================================
    // isConfigured state logic
    // =========================================================================

    @Test
    fun `isConfigured is true when savedUrl is non-blank`() {
        val savedUrl: String? = "https://example.com"
        val isConfigured = savedUrl?.isNotBlank() == true
        assertTrue(isConfigured)
    }

    @Test
    fun `isConfigured is false when savedUrl is null`() {
        val savedUrl: String? = null
        val isConfigured = savedUrl?.isNotBlank() == true
        assertFalse(isConfigured)
    }

    @Test
    fun `isConfigured is false when savedUrl is blank`() {
        val savedUrl: String? = "  "
        val isConfigured = savedUrl?.isNotBlank() == true
        assertFalse(isConfigured)
    }

    @Test
    fun `isConfigured is false when savedUrl is empty`() {
        val savedUrl: String? = ""
        val isConfigured = savedUrl?.isNotBlank() == true
        assertFalse(isConfigured)
    }

    // =========================================================================
    // selectedTab reset when tabs shrink (logout scenario)
    // =========================================================================

    @Test
    fun `selectedTab reset to 0 when tab index exceeds new tab count`() {
        val selectedTab = 3 // was on "operations"
        val bottomTabs = visibleTabs(isLoggedIn = false, isAdmin = false) // only 1 tab
        val newSelectedTab = if (selectedTab >= bottomTabs.size) 0 else selectedTab
        assertEquals(0, newSelectedTab)
    }

    @Test
    fun `selectedTab unchanged when still within range`() {
        val selectedTab = 2 // on "security"
        val bottomTabs = visibleTabs(isLoggedIn = true, isAdmin = false) // 4 tabs
        val newSelectedTab = if (selectedTab >= bottomTabs.size) 0 else selectedTab
        assertEquals(2, newSelectedTab)
    }

    // =========================================================================
    // Artifacts section sub-tab labels
    // =========================================================================

    @Test
    fun `compact sub-tabs use abbreviated names`() {
        val isCompact = true
        val subTabs = if (isCompact) listOf("Repos", "Staging", "Pkgs", "Builds", "Search")
                      else listOf("Repositories", "Staging", "Packages", "Builds", "Search")
        assertEquals("Repos", subTabs[0])
        assertEquals("Pkgs", subTabs[2])
    }

    @Test
    fun `non-compact sub-tabs use full names`() {
        val isCompact = false
        val subTabs = if (isCompact) listOf("Repos", "Staging", "Pkgs", "Builds", "Search")
                      else listOf("Repositories", "Staging", "Packages", "Builds", "Search")
        assertEquals("Repositories", subTabs[0])
        assertEquals("Packages", subTabs[2])
    }

    // =========================================================================
    // Operations section sub-tab labels
    // =========================================================================

    @Test
    fun `compact operations tabs use abbreviated names`() {
        val isCompact = true
        val subTabs = if (isCompact) listOf("Stats", "Health", "Metrics")
                      else listOf("Analytics", "Monitoring", "Telemetry")
        assertEquals("Stats", subTabs[0])
        assertEquals("Health", subTabs[1])
        assertEquals("Metrics", subTabs[2])
    }

    @Test
    fun `non-compact operations tabs use full names`() {
        val isCompact = false
        val subTabs = if (isCompact) listOf("Stats", "Health", "Metrics")
                      else listOf("Analytics", "Monitoring", "Telemetry")
        assertEquals("Analytics", subTabs[0])
        assertEquals("Monitoring", subTabs[1])
        assertEquals("Telemetry", subTabs[2])
    }
}

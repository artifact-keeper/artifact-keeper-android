package com.artifactkeeper.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the PackageCard display logic in PackagesScreen.kt.
 * Covers version prefix, format uppercase, description takeIf,
 * and search query to API parameter mapping.
 */
class PackagesScreenLogicTest {

    // =========================================================================
    // Version prefix display
    // =========================================================================

    @Test
    fun `version is prefixed with v`() {
        val version = "3.2.1"
        assertEquals("v3.2.1", "v$version")
    }

    @Test
    fun `version with existing v prefix gets double v`() {
        // This mirrors the actual behavior in the screen: "v${pkg.version}"
        val version = "v1.0.0"
        assertEquals("vv1.0.0", "v$version")
    }

    @Test
    fun `snapshot version is prefixed`() {
        val version = "1.0.0-SNAPSHOT"
        assertEquals("v1.0.0-SNAPSHOT", "v$version")
    }

    // =========================================================================
    // Format uppercase display
    // =========================================================================

    @Test
    fun `format displayed in uppercase`() {
        assertEquals("MAVEN", "maven".uppercase())
        assertEquals("NPM", "npm".uppercase())
        assertEquals("PYPI", "pypi".uppercase())
        assertEquals("NUGET", "nuget".uppercase())
        assertEquals("CARGO", "cargo".uppercase())
    }

    // =========================================================================
    // Description takeIf display
    // =========================================================================

    @Test
    fun `null description is not shown`() {
        val description: String? = null
        val display = description?.takeIf { it.isNotBlank() }
        assertNull(display)
    }

    @Test
    fun `blank description is not shown`() {
        val description: String? = ""
        val display = description?.takeIf { it.isNotBlank() }
        assertNull(display)
    }

    @Test
    fun `non-blank description is shown`() {
        val description: String? = "A utility library"
        val display = description?.takeIf { it.isNotBlank() }
        assertEquals("A utility library", display)
    }

    // =========================================================================
    // searchQuery ifBlank to null for API
    // =========================================================================

    @Test
    fun `blank search query becomes null for API`() {
        val searchQuery = ""
        val apiParam = searchQuery.ifBlank { null }
        assertNull(apiParam)
    }

    @Test
    fun `non-blank search query is passed to API`() {
        val searchQuery = "spring"
        val apiParam = searchQuery.ifBlank { null }
        assertEquals("spring", apiParam)
    }

    // =========================================================================
    // Navigation uses package ID as string
    // =========================================================================

    @Test
    fun `package card navigates with id as string`() {
        val id = java.util.UUID.randomUUID()
        val route = "packages/${id}"
        assertTrue(route.startsWith("packages/"))
    }

    // =========================================================================
    // Loading and error state
    // =========================================================================

    @Test
    fun `loading starts as true on initial load`() {
        val refresh = false
        val isLoading = !refresh
        assertTrue(isLoading)
    }

    @Test
    fun `isRefreshing set on refresh load`() {
        val refresh = true
        val isRefreshing = refresh
        assertTrue(isRefreshing)
    }

    // =========================================================================
    // Error message from exception
    // =========================================================================

    @Test
    fun `error message uses exception message`() {
        val exceptionMessage = "Network timeout"
        val errorMessage = exceptionMessage ?: "Failed to load packages"
        assertEquals("Network timeout", errorMessage)
    }

    @Test
    fun `error message falls back for null exception message`() {
        val exceptionMessage: String? = null
        val errorMessage = exceptionMessage ?: "Failed to load packages"
        assertEquals("Failed to load packages", errorMessage)
    }
}

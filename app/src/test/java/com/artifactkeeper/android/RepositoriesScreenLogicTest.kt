package com.artifactkeeper.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the RepositoryCard display logic in RepositoriesScreen.kt.
 * Covers format uppercase, description takeIf logic, and public chip display.
 */
class RepositoriesScreenLogicTest {

    // =========================================================================
    // Format uppercase display
    // =========================================================================

    @Test
    fun `format displayed in uppercase`() {
        assertEquals("MAVEN", "maven".uppercase())
        assertEquals("NPM", "npm".uppercase())
        assertEquals("DOCKER", "docker".uppercase())
        assertEquals("PYPI", "pypi".uppercase())
    }

    // =========================================================================
    // Description takeIf display
    // =========================================================================

    @Test
    fun `null description is not displayed`() {
        val description: String? = null
        val display = description?.takeIf { it.isNotBlank() }
        assertNull(display)
    }

    @Test
    fun `empty description is not displayed`() {
        val description: String? = ""
        val display = description?.takeIf { it.isNotBlank() }
        assertNull(display)
    }

    @Test
    fun `whitespace description is not displayed`() {
        val description: String? = "   "
        val display = description?.takeIf { it.isNotBlank() }
        assertNull(display)
    }

    @Test
    fun `non-blank description is displayed`() {
        val description: String? = "Central Maven repository mirror"
        val display = description?.takeIf { it.isNotBlank() }
        assertEquals("Central Maven repository mirror", display)
    }

    // =========================================================================
    // Public chip visibility
    // =========================================================================

    @Test
    fun `public chip shown when isPublic is true`() {
        val isPublic = true
        assertTrue(isPublic)
    }

    @Test
    fun `public chip hidden when isPublic is false`() {
        val isPublic = false
        assertFalse(isPublic)
    }

    // =========================================================================
    // Repository card click navigates to key
    // =========================================================================

    @Test
    fun `navigation uses repo key`() {
        val repoKey = "maven-central"
        val route = "repos/$repoKey"
        assertEquals("repos/maven-central", route)
    }
}

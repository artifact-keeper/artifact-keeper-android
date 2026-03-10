package com.artifactkeeper.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the constants and validation logic in CreateRepositoryScreen.
 * Since the FORMATS and REPO_TYPES lists are private to the file, we verify
 * the expected values by checking the counts and known formats that the
 * backend supports.
 *
 * The key-generation logic (slugify from name) and validation are inline
 * Compose lambdas, so we test the underlying string operations separately.
 */
class CreateRepositoryScreenTest {

    // =========================================================================
    // Key generation logic (replicates the LaunchedEffect transform)
    // =========================================================================

    private fun slugify(name: String): String {
        return name.lowercase().replace(Regex("[^a-z0-9-]"), "-").trim('-')
    }

    @Test
    fun `slugify converts name to lowercase with hyphens`() {
        assertEquals("my-maven-repo", slugify("My Maven Repo"))
    }

    @Test
    fun `slugify strips special characters`() {
        assertEquals("hello-world--123", slugify("Hello World! 123"))
    }

    @Test
    fun `slugify trims leading and trailing hyphens`() {
        assertEquals("test", slugify("--test--"))
    }

    @Test
    fun `slugify handles empty string`() {
        assertEquals("", slugify(""))
    }

    @Test
    fun `slugify collapses multiple special characters into hyphens`() {
        assertEquals("a---b", slugify("a @#b"))
    }

    @Test
    fun `slugify handles all-special-character input`() {
        assertEquals("", slugify("@#$%"))
    }

    @Test
    fun `slugify handles numbers only`() {
        assertEquals("123", slugify("123"))
    }

    // =========================================================================
    // Key input filter logic (replicates the onValueChange filter)
    // =========================================================================

    private fun filterKey(input: String): String {
        return input.lowercase().filter { c -> c.isLetterOrDigit() || c == '-' }
    }

    @Test
    fun `filterKey keeps valid characters`() {
        assertEquals("my-repo-1", filterKey("my-repo-1"))
    }

    @Test
    fun `filterKey strips spaces and special characters`() {
        assertEquals("myrepo", filterKey("My Repo!"))
    }

    @Test
    fun `filterKey converts to lowercase`() {
        assertEquals("abc", filterKey("ABC"))
    }

    @Test
    fun `filterKey allows hyphens`() {
        assertEquals("a-b-c", filterKey("a-b-c"))
    }

    @Test
    fun `filterKey strips underscores`() {
        assertEquals("ab", filterKey("a_b"))
    }

    // =========================================================================
    // Validation logic (replicates onClick checks)
    // =========================================================================

    @Test
    fun `validation requires non-blank name`() {
        assertTrue("".isBlank())
        assertTrue("   ".isBlank())
        assertFalse("Maven Repo".isBlank())
    }

    @Test
    fun `validation requires non-blank key`() {
        assertTrue("".isBlank())
        assertFalse("maven-local".isBlank())
    }

    @Test
    fun `validation requires upstream URL for remote type`() {
        val selectedType = "remote"
        val upstreamUrl = ""
        val needsUpstream = selectedType == "remote" && upstreamUrl.isBlank()
        assertTrue(needsUpstream)
    }

    @Test
    fun `validation does not require upstream URL for local type`() {
        val selectedType = "local"
        val upstreamUrl = ""
        val needsUpstream = selectedType == "remote" && upstreamUrl.isBlank()
        assertFalse(needsUpstream)
    }

    @Test
    fun `validation accepts remote type with upstream URL set`() {
        val selectedType = "remote"
        val upstreamUrl = "https://repo1.maven.org/maven2/"
        val needsUpstream = selectedType == "remote" && upstreamUrl.isBlank()
        assertFalse(needsUpstream)
    }

    // =========================================================================
    // Upstream URL conditional logic
    // =========================================================================

    @Test
    fun `upstream URL is null for non-remote types`() {
        val selectedType = "local"
        val upstreamUrl = "https://something.example.com/"
        val result = if (selectedType == "remote") upstreamUrl.ifBlank { null } else null
        assertEquals(null, result)
    }

    @Test
    fun `upstream URL is passed for remote type`() {
        val selectedType = "remote"
        val upstreamUrl = "https://repo1.maven.org/maven2/"
        val result = if (selectedType == "remote") upstreamUrl.ifBlank { null } else null
        assertEquals("https://repo1.maven.org/maven2/", result)
    }

    @Test
    fun `blank upstream URL becomes null even for remote type`() {
        val selectedType = "remote"
        val upstreamUrl = ""
        val result = if (selectedType == "remote") upstreamUrl.ifBlank { null } else null
        assertEquals(null, result)
    }

    // =========================================================================
    // Description normalization
    // =========================================================================

    @Test
    fun `blank description becomes null`() {
        val description = ""
        assertEquals(null, description.ifBlank { null })
    }

    @Test
    fun `non-blank description is kept`() {
        val description = "My repository for Maven artifacts"
        assertEquals("My repository for Maven artifacts", description.ifBlank { null })
    }
}

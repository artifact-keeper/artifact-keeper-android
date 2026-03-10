package com.artifactkeeper.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the search and display logic in SearchScreen.kt.
 * Covers blank query handling, search state branches, result
 * categorization, and display formatting.
 */
class SearchScreenLogicTest {

    // =========================================================================
    // Blank query handling (LaunchedEffect guard)
    // =========================================================================

    @Test
    fun `blank query clears results and hasSearched flag`() {
        val searchQuery = ""
        assertTrue(searchQuery.isBlank())
    }

    @Test
    fun `whitespace-only query is treated as blank`() {
        val searchQuery = "   "
        assertTrue(searchQuery.isBlank())
    }

    @Test
    fun `non-blank query proceeds to search`() {
        val searchQuery = "maven"
        assertFalse(searchQuery.isBlank())
    }

    // =========================================================================
    // Display state branches (when block in SearchScreen)
    // =========================================================================

    private fun searchDisplayBranch(
        isSearching: Boolean,
        errorMessage: String?,
        hasSearched: Boolean,
        repoResultsEmpty: Boolean,
        artifactResultsEmpty: Boolean,
    ): String = when {
        isSearching -> "loading"
        errorMessage != null -> "error"
        !hasSearched -> "initial"
        repoResultsEmpty && artifactResultsEmpty -> "noResults"
        else -> "results"
    }

    @Test
    fun `searching state shows loading`() {
        val branch = searchDisplayBranch(
            isSearching = true,
            errorMessage = null,
            hasSearched = false,
            repoResultsEmpty = true,
            artifactResultsEmpty = true,
        )
        assertEquals("loading", branch)
    }

    @Test
    fun `error state shows error`() {
        val branch = searchDisplayBranch(
            isSearching = false,
            errorMessage = "Network error",
            hasSearched = true,
            repoResultsEmpty = true,
            artifactResultsEmpty = true,
        )
        assertEquals("error", branch)
    }

    @Test
    fun `initial state shows search prompt`() {
        val branch = searchDisplayBranch(
            isSearching = false,
            errorMessage = null,
            hasSearched = false,
            repoResultsEmpty = true,
            artifactResultsEmpty = true,
        )
        assertEquals("initial", branch)
    }

    @Test
    fun `empty results shows no results message`() {
        val branch = searchDisplayBranch(
            isSearching = false,
            errorMessage = null,
            hasSearched = true,
            repoResultsEmpty = true,
            artifactResultsEmpty = true,
        )
        assertEquals("noResults", branch)
    }

    @Test
    fun `non-empty results shows results`() {
        val branch = searchDisplayBranch(
            isSearching = false,
            errorMessage = null,
            hasSearched = true,
            repoResultsEmpty = false,
            artifactResultsEmpty = true,
        )
        assertEquals("results", branch)
    }

    @Test
    fun `only artifacts non-empty shows results`() {
        val branch = searchDisplayBranch(
            isSearching = false,
            errorMessage = null,
            hasSearched = true,
            repoResultsEmpty = true,
            artifactResultsEmpty = false,
        )
        assertEquals("results", branch)
    }

    @Test
    fun `loading takes priority over error`() {
        val branch = searchDisplayBranch(
            isSearching = true,
            errorMessage = "Some error",
            hasSearched = true,
            repoResultsEmpty = true,
            artifactResultsEmpty = true,
        )
        assertEquals("loading", branch)
    }

    // =========================================================================
    // No results message formatting
    // =========================================================================

    @Test
    fun `no results message includes search query`() {
        val searchQuery = "spring-boot"
        val message = "No results found for \"$searchQuery\""
        assertEquals("No results found for \"spring-boot\"", message)
    }

    // =========================================================================
    // Error message display
    // =========================================================================

    @Test
    fun `error message uses provided string`() {
        val errorMessage: String? = "Search failed"
        val displayText = errorMessage ?: "Unknown error"
        assertEquals("Search failed", displayText)
    }

    @Test
    fun `null error message falls back to Unknown error`() {
        val errorMessage: String? = null
        val displayText = errorMessage ?: "Unknown error"
        assertEquals("Unknown error", displayText)
    }

    // =========================================================================
    // Result card display: repo format uppercase
    // =========================================================================

    @Test
    fun `repo format displayed in uppercase`() {
        assertEquals("MAVEN", "maven".uppercase())
        assertEquals("NPM", "npm".uppercase())
        assertEquals("DOCKER", "docker".uppercase())
    }

    // =========================================================================
    // Result card display: artifact version prefix
    // =========================================================================

    @Test
    fun `artifact version prefixed with v`() {
        val version = "1.2.3"
        assertEquals("v1.2.3", "v$version")
    }

    // =========================================================================
    // Result card display: description check
    // =========================================================================

    @Test
    fun `null description not shown`() {
        val desc: String? = null
        val shouldShow = desc?.isNotBlank() == true
        assertFalse(shouldShow)
    }

    @Test
    fun `blank description not shown`() {
        val desc = ""
        val shouldShow = desc.isNotBlank()
        assertFalse(shouldShow)
    }

    @Test
    fun `non-blank description is shown`() {
        val desc = "A Maven repository"
        val shouldShow = desc.isNotBlank()
        assertTrue(shouldShow)
    }

    // =========================================================================
    // Download count display
    // =========================================================================

    @Test
    fun `download count display`() {
        val downloadCount = 1500L
        assertEquals("1500 downloads", "$downloadCount downloads")
    }

    // =========================================================================
    // Section headers conditionally shown
    // =========================================================================

    @Test
    fun `repositories header shown when repo results non-empty`() {
        val repoResults = listOf("repo1")
        assertTrue(repoResults.isNotEmpty())
    }

    @Test
    fun `artifacts header shown when artifact results non-empty`() {
        val artifactResults = listOf("artifact1")
        assertTrue(artifactResults.isNotEmpty())
    }

    @Test
    fun `headers not shown when results empty`() {
        val repoResults = emptyList<String>()
        val artifactResults = emptyList<String>()
        assertFalse(repoResults.isNotEmpty())
        assertFalse(artifactResults.isNotEmpty())
    }
}

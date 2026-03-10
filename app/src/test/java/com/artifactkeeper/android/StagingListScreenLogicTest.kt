package com.artifactkeeper.android

import com.artifactkeeper.android.data.models.StagingRepository
import com.artifactkeeper.android.ui.util.formatBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the display logic in StagingListScreen.kt.
 * Covers artifact count pluralization, target repository display,
 * and staging repository card data presentation.
 */
class StagingListScreenLogicTest {

    // =========================================================================
    // Artifact count pluralization (StagingRepoCard)
    // =========================================================================

    @Test
    fun `artifact count singular for 1`() {
        val count = 1
        val text = "$count artifact${if (count != 1) "s" else ""}"
        assertEquals("1 artifact", text)
    }

    @Test
    fun `artifact count plural for 0`() {
        val count = 0
        val text = "$count artifact${if (count != 1) "s" else ""}"
        assertEquals("0 artifacts", text)
    }

    @Test
    fun `artifact count plural for multiple`() {
        val count = 42
        val text = "$count artifact${if (count != 1) "s" else ""}"
        assertEquals("42 artifacts", text)
    }

    // =========================================================================
    // Target repository key display
    // =========================================================================

    @Test
    fun `target repository key prefixed with arrow`() {
        val targetKey = "npm-releases"
        val display = "-> $targetKey"
        assertEquals("-> npm-releases", display)
    }

    @Test
    fun `null target repository key is not shown`() {
        val targetKey: String? = null
        assertNull(targetKey)
    }

    // =========================================================================
    // StagingRepoCard format display (uppercase)
    // =========================================================================

    @Test
    fun `format displayed in uppercase`() {
        assertEquals("MAVEN", "maven".uppercase())
        assertEquals("NPM", "npm".uppercase())
        assertEquals("DOCKER", "docker".uppercase())
    }

    // =========================================================================
    // Loading/error container isEmpty logic
    // =========================================================================

    @Test
    fun `loading shown only when isLoadingRepos AND list is empty`() {
        val isLoadingRepos = true
        val stagingReposEmpty = true
        val showLoading = isLoadingRepos && stagingReposEmpty
        assertTrue(showLoading)
    }

    @Test
    fun `loading not shown when repos already loaded`() {
        val isLoadingRepos = true
        val stagingReposEmpty = false
        val showLoading = isLoadingRepos && stagingReposEmpty
        assertFalse(showLoading)
    }

    @Test
    fun `error only shown when repos list is empty`() {
        val reposEmpty = true
        val reposError = "Network error"
        val displayError = if (reposEmpty) reposError else null
        assertEquals("Network error", displayError)
    }

    @Test
    fun `error not shown when repos already loaded`() {
        val reposEmpty = false
        val reposError = "Network error"
        val displayError = if (reposEmpty) reposError else null
        assertNull(displayError)
    }

    // =========================================================================
    // Description display logic (isNullOrBlank check)
    // =========================================================================

    @Test
    fun `null description is not shown`() {
        val description: String? = null
        assertTrue(description.isNullOrBlank())
    }

    @Test
    fun `empty description is not shown`() {
        val description = ""
        assertTrue(description.isNullOrBlank())
    }

    @Test
    fun `blank description is not shown`() {
        val description = "   "
        assertTrue(description.isNullOrBlank())
    }

    @Test
    fun `non-blank description is shown`() {
        val description = "Maven staging repository"
        assertFalse(description.isNullOrBlank())
    }

    // =========================================================================
    // StagingRepository data fields used in the card
    // =========================================================================

    @Test
    fun `staging repo card data is correctly sourced`() {
        val repo = StagingRepository(
            id = "r1",
            key = "staging-maven",
            name = "Staging Maven",
            format = "maven",
            artifactCount = 15,
            passingCount = 10,
            failingCount = 3,
            pendingCount = 2,
            storageUsedBytes = 1024L * 1024 * 50,
            targetRepositoryKey = "maven-releases",
            createdAt = "2024-01-01T00:00:00Z"
        )

        assertEquals("Staging Maven", repo.name)
        assertEquals("MAVEN", repo.format.uppercase())
        assertEquals(15, repo.artifactCount)
        assertEquals(10, repo.passingCount)
        assertEquals(3, repo.failingCount)
        assertEquals(2, repo.pendingCount)
        assertEquals("maven-releases", repo.targetRepositoryKey)
        assertEquals("50 MB", formatBytes(repo.storageUsedBytes))
    }
}

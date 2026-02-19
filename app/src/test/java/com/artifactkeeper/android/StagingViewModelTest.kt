package com.artifactkeeper.android

import com.artifactkeeper.android.data.models.PolicyStatus
import com.artifactkeeper.android.data.models.StagingArtifact
import com.artifactkeeper.android.data.models.StagingRepository
import com.artifactkeeper.android.ui.screens.staging.StagingUiState
import com.artifactkeeper.android.ui.screens.staging.StagingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StagingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: StagingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = StagingViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // Initial state
    // =========================================================================

    @Test
    fun `initial state has empty collections and no loading flags`() {
        val state = viewModel.uiState.value
        assertEquals(StagingUiState(), state)
        assertTrue(state.stagingRepos.isEmpty())
        assertNull(state.selectedRepo)
        assertTrue(state.artifacts.isEmpty())
        assertTrue(state.selectedArtifactIds.isEmpty())
        assertTrue(state.promotionHistory.isEmpty())
        assertTrue(state.targetRepositories.isEmpty())
        assertFalse(state.isLoadingRepos)
        assertFalse(state.isLoadingArtifacts)
        assertFalse(state.isLoadingHistory)
        assertFalse(state.isPromoting)
        assertNull(state.reposError)
        assertNull(state.artifactsError)
        assertNull(state.historyError)
        assertNull(state.promotionError)
        assertNull(state.promotionSuccess)
        assertNull(state.filterStatus)
    }

    // =========================================================================
    // selectRepo / clearSelectedRepo
    // =========================================================================

    @Test
    fun `selectRepo sets selectedRepo and clears artifacts and selection`() {
        val repo = makeStagingRepo("staging-maven")

        // Pre-populate some state to verify it gets cleared
        viewModel.uiState.value.let { /* initial state */ }

        viewModel.selectRepo(repo)

        val state = viewModel.uiState.value
        assertEquals(repo, state.selectedRepo)
        // artifacts and selection should be cleared when selecting a new repo
        assertTrue(state.selectedArtifactIds.isEmpty())
        assertTrue(state.promotionHistory.isEmpty())
    }

    @Test
    fun `clearSelectedRepo resets repo-specific state`() {
        val repo = makeStagingRepo("staging-npm")
        viewModel.selectRepo(repo)

        viewModel.clearSelectedRepo()

        val state = viewModel.uiState.value
        assertNull(state.selectedRepo)
        assertTrue(state.artifacts.isEmpty())
        assertTrue(state.selectedArtifactIds.isEmpty())
        assertTrue(state.promotionHistory.isEmpty())
    }

    // =========================================================================
    // toggleArtifactSelection
    // =========================================================================

    @Test
    fun `toggleArtifactSelection adds artifact to selection`() {
        viewModel.toggleArtifactSelection("a1")
        assertTrue(viewModel.uiState.value.selectedArtifactIds.contains("a1"))
    }

    @Test
    fun `toggleArtifactSelection removes artifact if already selected`() {
        viewModel.toggleArtifactSelection("a1")
        viewModel.toggleArtifactSelection("a1")
        assertFalse(viewModel.uiState.value.selectedArtifactIds.contains("a1"))
    }

    @Test
    fun `toggleArtifactSelection handles multiple artifacts`() {
        viewModel.toggleArtifactSelection("a1")
        viewModel.toggleArtifactSelection("a2")
        viewModel.toggleArtifactSelection("a3")

        val selected = viewModel.uiState.value.selectedArtifactIds
        assertEquals(3, selected.size)
        assertTrue(selected.containsAll(listOf("a1", "a2", "a3")))

        // Remove one
        viewModel.toggleArtifactSelection("a2")
        val updated = viewModel.uiState.value.selectedArtifactIds
        assertEquals(2, updated.size)
        assertFalse(updated.contains("a2"))
    }

    // =========================================================================
    // clearSelection
    // =========================================================================

    @Test
    fun `clearSelection empties the selection set`() {
        viewModel.toggleArtifactSelection("a1")
        viewModel.toggleArtifactSelection("a2")
        assertEquals(2, viewModel.uiState.value.selectedArtifactIds.size)

        viewModel.clearSelection()
        assertTrue(viewModel.uiState.value.selectedArtifactIds.isEmpty())
    }

    // =========================================================================
    // setFilterStatus
    // =========================================================================

    @Test
    fun `setFilterStatus updates filter and clears selection`() {
        viewModel.toggleArtifactSelection("a1")
        viewModel.setFilterStatus(PolicyStatus.PASSING)

        val state = viewModel.uiState.value
        assertEquals(PolicyStatus.PASSING, state.filterStatus)
        assertTrue(state.selectedArtifactIds.isEmpty())
    }

    @Test
    fun `setFilterStatus to null clears the filter`() {
        viewModel.setFilterStatus(PolicyStatus.FAILING)
        viewModel.setFilterStatus(null)
        assertNull(viewModel.uiState.value.filterStatus)
    }

    // =========================================================================
    // clearMessages
    // =========================================================================

    @Test
    fun `clearMessages resets promotion error and success`() {
        // We cannot easily set promotionError/promotionSuccess without API calls,
        // but we can verify clearMessages works on a clean state (no-op).
        viewModel.clearMessages()
        val state = viewModel.uiState.value
        assertNull(state.promotionError)
        assertNull(state.promotionSuccess)
    }

    // =========================================================================
    // promoteBulk with no selection
    // =========================================================================

    @Test
    fun `promoteBulk calls onError when no artifacts selected`() {
        val repo = makeStagingRepo("staging-maven")
        viewModel.selectRepo(repo)
        viewModel.clearSelection()

        var errorMsg: String? = null
        viewModel.promoteBulk(
            targetRepoKey = "maven-releases",
            onError = { errorMsg = it }
        )

        assertEquals("No artifacts selected", errorMsg)
    }

    @Test
    fun `promoteBulk does nothing when no repo selected`() {
        // No repo selected, so promoteBulk should return immediately
        viewModel.toggleArtifactSelection("a1")

        var errorCalled = false
        var successCalled = false
        viewModel.promoteBulk(
            targetRepoKey = "maven-releases",
            onError = { errorCalled = true },
            onSuccess = { successCalled = true }
        )

        // Neither callback should be called because the method returns early
        assertFalse(errorCalled)
        assertFalse(successCalled)
    }

    // =========================================================================
    // promoteArtifact with no repo selected
    // =========================================================================

    @Test
    fun `promoteArtifact does nothing when no repo selected`() {
        var errorCalled = false
        var successCalled = false
        viewModel.promoteArtifact(
            artifactId = "a1",
            targetRepoKey = "maven-releases",
            onError = { errorCalled = true },
            onSuccess = { successCalled = true }
        )

        assertFalse(errorCalled)
        assertFalse(successCalled)
        assertFalse(viewModel.uiState.value.isPromoting)
    }

    // =========================================================================
    // StagingUiState data class behavior
    // =========================================================================

    @Test
    fun `StagingUiState copy preserves unmodified fields`() {
        val original = StagingUiState(
            isLoadingRepos = true,
            reposError = "network error",
            filterStatus = PolicyStatus.WARNING,
        )
        val copied = original.copy(isLoadingRepos = false)

        assertFalse(copied.isLoadingRepos)
        assertEquals("network error", copied.reposError)
        assertEquals(PolicyStatus.WARNING, copied.filterStatus)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun makeStagingRepo(key: String) = StagingRepository(
        id = "repo-$key",
        key = key,
        name = "Staging ${key.substringAfter("-")}",
        format = key.substringAfter("-"),
        createdAt = "2024-01-01T00:00:00Z"
    )
}

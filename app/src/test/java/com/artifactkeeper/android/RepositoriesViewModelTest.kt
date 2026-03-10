package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.Repository
import com.artifactkeeper.android.data.models.RepositoryListResponse
import com.artifactkeeper.android.ui.screens.repositories.RepositoriesUiState
import com.artifactkeeper.android.ui.screens.repositories.RepositoriesViewModel
import com.artifactkeeper.client.apis.RepositoriesApi
import com.artifactkeeper.client.models.Pagination
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.OffsetDateTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoriesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockReposApi = mockk<RepositoriesApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { reposApi } returns mockReposApi
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makePagination() = Pagination(
        page = 1,
        perPage = 20,
        total = 0,
        totalPages = 1,
    )

    private fun makeRepo(key: String, format: String = "maven") = Repository(
        id = UUID.randomUUID(),
        key = key,
        name = "Repo $key",
        format = format,
        repoType = "local",
        isPublic = false,
        storageUsedBytes = 1024,
        createdAt = OffsetDateTime.now(),
        updatedAt = OffsetDateTime.now(),
    )

    // =========================================================================
    // RepositoriesUiState data class
    // =========================================================================

    @Test
    fun `RepositoriesUiState default values`() {
        val state = RepositoriesUiState()
        assertTrue(state.repositories.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    @Test
    fun `RepositoriesUiState copy preserves unmodified fields`() {
        val original = RepositoriesUiState(
            isLoading = true,
            error = "something failed",
        )
        val copied = original.copy(isLoading = false)
        assertFalse(copied.isLoading)
        assertEquals("something failed", copied.error)
    }

    @Test
    fun `RepositoriesUiState equality`() {
        val a = RepositoriesUiState()
        val b = RepositoriesUiState()
        assertEquals(a, b)
    }

    // =========================================================================
    // loadRepositories success
    // =========================================================================

    @Test
    fun `loadRepositories populates repositories on success`() = runTest {
        val repos = listOf(
            makeRepo("maven-local"),
            makeRepo("npm-local", "npm"),
        )
        val response = RepositoryListResponse(
            items = repos,
            pagination = makePagination(),
        )
        coEvery { mockReposApi.listRepositories(any(), any(), any(), any(), any()) } returns Response.success(response)

        val viewModel = RepositoriesViewModel(mockApiClient)

        val state = viewModel.uiState.value
        assertEquals(2, state.repositories.size)
        assertEquals("maven-local", state.repositories[0].key)
        assertEquals("npm-local", state.repositories[1].key)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    // =========================================================================
    // loadRepositories failure
    // =========================================================================

    @Test
    fun `loadRepositories sets error on failure`() = runTest {
        coEvery { mockReposApi.listRepositories(any(), any(), any(), any(), any()) } throws RuntimeException("Network error")

        val viewModel = RepositoriesViewModel(mockApiClient)

        val state = viewModel.uiState.value
        assertEquals("Network error", state.error)
        assertTrue(state.repositories.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `loadRepositories uses default message when exception has no message`() = runTest {
        coEvery { mockReposApi.listRepositories(any(), any(), any(), any(), any()) } throws RuntimeException()

        val viewModel = RepositoriesViewModel(mockApiClient)

        val state = viewModel.uiState.value
        assertEquals("Failed to load repositories", state.error)
    }

    // =========================================================================
    // refresh flag
    // =========================================================================

    @Test
    fun `loadRepositories with refresh true clears error and sets isRefreshing`() = runTest {
        val repos = listOf(makeRepo("docker-local", "docker"))
        val response = RepositoryListResponse(
            items = repos,
            pagination = makePagination(),
        )
        coEvery { mockReposApi.listRepositories(any(), any(), any(), any(), any()) } returns Response.success(response)

        val viewModel = RepositoriesViewModel(mockApiClient)
        // Call again with refresh = true
        viewModel.loadRepositories(refresh = true)

        val state = viewModel.uiState.value
        assertEquals(1, state.repositories.size)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    // =========================================================================
    // loadRepositories handles empty list
    // =========================================================================

    @Test
    fun `loadRepositories handles empty repository list`() = runTest {
        val response = RepositoryListResponse(
            items = emptyList(),
            pagination = makePagination(),
        )
        coEvery { mockReposApi.listRepositories(any(), any(), any(), any(), any()) } returns Response.success(response)

        val viewModel = RepositoriesViewModel(mockApiClient)

        val state = viewModel.uiState.value
        assertTrue(state.repositories.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }
}

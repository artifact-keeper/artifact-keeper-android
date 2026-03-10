package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.security.SecurityUiState
import com.artifactkeeper.android.ui.screens.security.SecurityViewModel
import com.artifactkeeper.client.apis.RepositoriesApi
import com.artifactkeeper.client.apis.SbomApi
import com.artifactkeeper.client.apis.SecurityApi
import com.artifactkeeper.client.models.DtPortfolioMetrics
import com.artifactkeeper.client.models.DtStatusResponse
import com.artifactkeeper.client.models.Pagination
import com.artifactkeeper.client.models.RepositoryListResponse
import com.artifactkeeper.client.models.ScoreResponse
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.OffsetDateTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockSecurityApi = mockk<SecurityApi>()
    private val mockReposApi = mockk<RepositoriesApi>()
    private val mockSbomApi = mockk<SbomApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { securityApi } returns mockSecurityApi
        every { reposApi } returns mockReposApi
        every { sbomApi } returns mockSbomApi
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // SecurityUiState data class
    // =========================================================================

    @Test
    fun `SecurityUiState default values`() {
        val state = SecurityUiState()
        assertTrue(state.scores.isEmpty())
        assertTrue(state.repoMap.isEmpty())
        assertNull(state.cveTrends)
        assertNull(state.dtStatus)
        assertNull(state.dtPortfolioMetrics)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    @Test
    fun `SecurityUiState copy preserves unmodified fields`() {
        val original = SecurityUiState(
            isLoading = true,
            error = "timeout",
        )
        val copied = original.copy(isLoading = false)
        assertFalse(copied.isLoading)
        assertEquals("timeout", copied.error)
    }

    @Test
    fun `SecurityUiState equality`() {
        assertEquals(SecurityUiState(), SecurityUiState())
    }

    // =========================================================================
    // loadData success
    // =========================================================================

    @Test
    fun `loadData populates scores and repoMap on success`() = runTest {
        val repoId = UUID.randomUUID()
        val score = ScoreResponse(
            id = UUID.randomUUID(),
            repositoryId = repoId,
            score = 85,
            grade = "B",
            totalFindings = 5,
            criticalCount = 0,
            highCount = 1,
            mediumCount = 2,
            lowCount = 2,
            acknowledgedCount = 0,
            calculatedAt = OffsetDateTime.now(),
        )
        val repo = com.artifactkeeper.client.models.RepositoryResponse(
            id = repoId,
            key = "maven-local",
            name = "Maven Local",
            format = "maven",
            repoType = "local",
            isPublic = false,
            storageUsedBytes = 1024,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
        )

        coEvery { mockSecurityApi.getAllScores() } returns Response.success(listOf(score))
        coEvery { mockReposApi.listRepositories(any(), perPage = 100, any(), any(), any()) } returns Response.success(
            RepositoryListResponse(
                items = listOf(repo),
                pagination = Pagination(page = 1, perPage = 100, total = 1, totalPages = 1),
            )
        )
        // CVE trends and DT status are optional, make them fail gracefully
        coEvery { mockSbomApi.getCveTrends(any(), any()) } throws RuntimeException("not available")
        coEvery { mockSecurityApi.dtStatus() } throws RuntimeException("not available")

        val viewModel = SecurityViewModel(mockApiClient)

        val state = viewModel.uiState.value
        assertEquals(1, state.scores.size)
        assertEquals(85, state.scores[0].score)
        assertEquals(1, state.repoMap.size)
        assertTrue(state.repoMap.containsKey(repoId))
        assertNull(state.cveTrends)
        assertNull(state.dtStatus)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    // =========================================================================
    // loadData failure
    // =========================================================================

    @Test
    fun `loadData sets error when primary API call fails`() = runTest {
        coEvery { mockSecurityApi.getAllScores() } throws RuntimeException("Unauthorized")

        val viewModel = SecurityViewModel(mockApiClient)

        val state = viewModel.uiState.value
        assertEquals("Unauthorized", state.error)
        assertTrue(state.scores.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadData uses default message when exception has no message`() = runTest {
        coEvery { mockSecurityApi.getAllScores() } throws RuntimeException()

        val viewModel = SecurityViewModel(mockApiClient)

        assertEquals("Failed to load security data", viewModel.uiState.value.error)
    }

    // =========================================================================
    // refresh flag
    // =========================================================================

    // =========================================================================
    // DT status success with portfolio metrics
    // =========================================================================

    @Test
    fun `loadData populates dtStatus and dtPortfolioMetrics when DT is enabled and healthy`() = runTest {
        val score = ScoreResponse(
            id = UUID.randomUUID(),
            repositoryId = UUID.randomUUID(),
            score = 90,
            grade = "A",
            totalFindings = 1,
            criticalCount = 0,
            highCount = 0,
            mediumCount = 1,
            lowCount = 0,
            acknowledgedCount = 0,
            calculatedAt = OffsetDateTime.now(),
        )
        coEvery { mockSecurityApi.getAllScores() } returns Response.success(listOf(score))
        coEvery { mockReposApi.listRepositories(any(), perPage = 100, any(), any(), any()) } returns Response.success(
            RepositoryListResponse(
                items = emptyList(),
                pagination = Pagination(page = 1, perPage = 100, total = 0, totalPages = 1),
            )
        )
        coEvery { mockSbomApi.getCveTrends(any(), any()) } throws RuntimeException("N/A")

        val dtStatus = DtStatusResponse(enabled = true, healthy = true, url = "https://dt.example.com")
        coEvery { mockSecurityApi.dtStatus() } returns Response.success(dtStatus)

        val portfolioMetrics = DtPortfolioMetrics(
            critical = 2,
            high = 5,
            medium = 10,
            low = 20,
            projects = 3,
        )
        coEvery { mockSecurityApi.getPortfolioMetrics() } returns Response.success(portfolioMetrics)

        val viewModel = SecurityViewModel(mockApiClient)
        val state = viewModel.uiState.value

        assertNotNull(state.dtStatus)
        assertTrue(state.dtStatus!!.enabled)
        assertTrue(state.dtStatus!!.healthy)
        assertNotNull(state.dtPortfolioMetrics)
        assertEquals(2L, state.dtPortfolioMetrics!!.critical)
        assertEquals(3L, state.dtPortfolioMetrics!!.projects)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadData skips portfolio metrics when DT is not healthy`() = runTest {
        val score = ScoreResponse(
            id = UUID.randomUUID(),
            repositoryId = UUID.randomUUID(),
            score = 70,
            grade = "C",
            totalFindings = 10,
            criticalCount = 1,
            highCount = 3,
            mediumCount = 4,
            lowCount = 2,
            acknowledgedCount = 0,
            calculatedAt = OffsetDateTime.now(),
        )
        coEvery { mockSecurityApi.getAllScores() } returns Response.success(listOf(score))
        coEvery { mockReposApi.listRepositories(any(), perPage = 100, any(), any(), any()) } returns Response.success(
            RepositoryListResponse(
                items = emptyList(),
                pagination = Pagination(page = 1, perPage = 100, total = 0, totalPages = 1),
            )
        )
        coEvery { mockSbomApi.getCveTrends(any(), any()) } throws RuntimeException("N/A")

        val dtStatus = DtStatusResponse(enabled = true, healthy = false, url = null)
        coEvery { mockSecurityApi.dtStatus() } returns Response.success(dtStatus)

        val viewModel = SecurityViewModel(mockApiClient)
        val state = viewModel.uiState.value

        assertNotNull(state.dtStatus)
        assertTrue(state.dtStatus!!.enabled)
        assertFalse(state.dtStatus!!.healthy)
        assertNull(state.dtPortfolioMetrics)
    }

    // =========================================================================
    // refresh flag
    // =========================================================================

    @Test
    fun `loadData with refresh clears error and reloads`() = runTest {
        val score = ScoreResponse(
            id = UUID.randomUUID(),
            repositoryId = UUID.randomUUID(),
            score = 100,
            grade = "A",
            totalFindings = 0,
            criticalCount = 0,
            highCount = 0,
            mediumCount = 0,
            lowCount = 0,
            acknowledgedCount = 0,
            calculatedAt = OffsetDateTime.now(),
        )
        coEvery { mockSecurityApi.getAllScores() } returns Response.success(listOf(score))
        coEvery { mockReposApi.listRepositories(any(), perPage = 100, any(), any(), any()) } returns Response.success(
            RepositoryListResponse(
                items = emptyList(),
                pagination = Pagination(page = 1, perPage = 100, total = 0, totalPages = 1),
            )
        )
        coEvery { mockSbomApi.getCveTrends(any(), any()) } throws RuntimeException("N/A")
        coEvery { mockSecurityApi.dtStatus() } throws RuntimeException("N/A")

        val viewModel = SecurityViewModel(mockApiClient)
        viewModel.loadData(refresh = true)

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }
}

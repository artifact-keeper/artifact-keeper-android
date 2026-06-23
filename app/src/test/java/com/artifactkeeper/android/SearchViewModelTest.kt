package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.search.SearchMode
import com.artifactkeeper.android.ui.screens.search.SearchUiState
import com.artifactkeeper.android.ui.screens.search.SearchViewModel
import com.artifactkeeper.client.apis.SearchApi
import com.artifactkeeper.client.models.ChecksumArtifact
import com.artifactkeeper.client.models.ChecksumSearchResponse
import com.artifactkeeper.client.models.QuickSearchResponse
import com.artifactkeeper.client.models.SearchResultItem
import com.artifactkeeper.client.models.SuggestResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
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
class SearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockSearchApi = mockk<SearchApi>(relaxed = false)
    private val mockApiClient = mockk<ApiClient> {
        every { searchApi } returns mockSearchApi
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty text mode`() {
        val vm = SearchViewModel(mockApiClient)
        val state = vm.uiState.value
        assertEquals(SearchUiState(), state)
        assertEquals(SearchMode.TEXT, state.mode)
        assertFalse(state.hasSearched)
        assertTrue(state.results.isEmpty())
    }

    @Test
    fun `loadInitial populates recent and trending`() {
        coEvery { mockSearchApi.recent(any()) } returns Response.success(listOf(item("recent-pkg")))
        coEvery { mockSearchApi.trending(any(), any()) } returns Response.success(listOf(item("trending-pkg")))
        val vm = SearchViewModel(mockApiClient)

        vm.loadInitial()

        val state = vm.uiState.value
        assertEquals(1, state.recent.size)
        assertEquals("recent-pkg", state.recent.first().name)
        assertEquals(1, state.trending.size)
        assertEquals("trending-pkg", state.trending.first().name)
    }

    @Test
    fun `loadInitial tolerates recent and trending failures`() {
        coEvery { mockSearchApi.recent(any()) } throws RuntimeException("nope")
        coEvery { mockSearchApi.trending(any(), any()) } throws RuntimeException("nope")
        val vm = SearchViewModel(mockApiClient)

        vm.loadInitial()

        assertNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.recent.isEmpty())
        assertTrue(vm.uiState.value.trending.isEmpty())
    }

    @Test
    fun `quickSearch populates results and marks searched`() {
        coEvery { mockSearchApi.quickSearch("lib", any(), any()) } returns
            Response.success(QuickSearchResponse(results = listOf(item("libfoo"), item("libbar"))))
        val vm = SearchViewModel(mockApiClient)

        vm.quickSearch("lib")

        val state = vm.uiState.value
        assertFalse(state.isSearching)
        assertTrue(state.hasSearched)
        assertEquals(2, state.results.size)
        coVerify { mockSearchApi.quickSearch("lib", any(), any()) }
    }

    @Test
    fun `quickSearch with blank query clears results and does not call the api`() {
        val vm = SearchViewModel(mockApiClient)

        vm.quickSearch("   ")

        assertTrue(vm.uiState.value.results.isEmpty())
        assertFalse(vm.uiState.value.hasSearched)
        coVerify(exactly = 0) { mockSearchApi.quickSearch(any(), any(), any()) }
    }

    @Test
    fun `quickSearch sets error on failure`() {
        coEvery { mockSearchApi.quickSearch(any(), any(), any()) } returns Response.error(500, errorBody())
        val vm = SearchViewModel(mockApiClient)

        vm.quickSearch("lib")

        assertEquals(true, vm.uiState.value.error?.isNotBlank())
        assertTrue(vm.uiState.value.hasSearched)
    }

    @Test
    fun `loadSuggestions populates suggestions`() {
        coEvery { mockSearchApi.suggest("lo", any()) } returns
            Response.success(SuggestResponse(suggestions = listOf("log4j", "logback")))
        val vm = SearchViewModel(mockApiClient)

        vm.loadSuggestions("lo")

        assertEquals(listOf("log4j", "logback"), vm.uiState.value.suggestions)
    }

    @Test
    fun `loadSuggestions clears suggestions for short prefix`() {
        val vm = SearchViewModel(mockApiClient)

        vm.loadSuggestions("")

        assertTrue(vm.uiState.value.suggestions.isEmpty())
        coVerify(exactly = 0) { mockSearchApi.suggest(any(), any()) }
    }

    @Test
    fun `setMode to checksum clears text results`() {
        coEvery { mockSearchApi.quickSearch(any(), any(), any()) } returns
            Response.success(QuickSearchResponse(results = listOf(item("libfoo"))))
        val vm = SearchViewModel(mockApiClient)
        vm.quickSearch("lib")

        vm.setMode(SearchMode.CHECKSUM)

        val state = vm.uiState.value
        assertEquals(SearchMode.CHECKSUM, state.mode)
        assertTrue(state.results.isEmpty())
        assertFalse(state.hasSearched)
    }

    @Test
    fun `checksumSearch populates checksum results`() {
        coEvery { mockSearchApi.checksumSearch("abc123", any()) } returns
            Response.success(ChecksumSearchResponse(artifacts = listOf(checksumArtifact("match.jar"))))
        val vm = SearchViewModel(mockApiClient)
        vm.setMode(SearchMode.CHECKSUM)

        vm.checksumSearch("abc123")

        val state = vm.uiState.value
        assertTrue(state.hasSearched)
        assertEquals(1, state.checksumResults.size)
        assertEquals("match.jar", state.checksumResults.first().name)
    }

    @Test
    fun `checksumSearch with blank input does not call the api`() {
        val vm = SearchViewModel(mockApiClient)
        vm.setMode(SearchMode.CHECKSUM)

        vm.checksumSearch("  ")

        coVerify(exactly = 0) { mockSearchApi.checksumSearch(any(), any()) }
    }

    @Test
    fun `advancedSearch applies filters and populates results`() {
        coEvery {
            mockSearchApi.advancedSearch(
                query = "lib",
                format = "maven",
                repositoryKey = null,
                name = null,
                path = null,
                version = null,
                minSize = null,
                maxSize = null,
                createdAfter = null,
                createdBefore = null,
                page = any(),
                perPage = any(),
                sortBy = null,
                sortOrder = null,
            )
        } returns Response.success(
            com.artifactkeeper.client.models.AdvancedSearchResponse(
                facets = com.artifactkeeper.client.models.FacetsResponse(
                    contentTypes = emptyList(),
                    formats = emptyList(),
                    repositories = emptyList(),
                ),
                items = listOf(item("libfoo")),
                pagination = com.artifactkeeper.client.models.PaginationInfo(
                    page = 1,
                    perPage = 20,
                    total = 1L,
                    totalPages = 1,
                ),
            ),
        )
        val vm = SearchViewModel(mockApiClient)

        vm.advancedSearch(query = "lib", format = "maven")

        assertEquals(1, vm.uiState.value.results.size)
        assertTrue(vm.uiState.value.hasSearched)
    }

    @Test
    fun `clear resets query and results`() {
        coEvery { mockSearchApi.quickSearch(any(), any(), any()) } returns
            Response.success(QuickSearchResponse(results = listOf(item("libfoo"))))
        val vm = SearchViewModel(mockApiClient)
        vm.quickSearch("lib")

        vm.clear()

        val state = vm.uiState.value
        assertTrue(state.results.isEmpty())
        assertTrue(state.suggestions.isEmpty())
        assertFalse(state.hasSearched)
    }

    // Helpers

    private fun item(name: String) = SearchResultItem(
        createdAt = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        id = UUID.randomUUID(),
        name = name,
        repositoryKey = "maven-local",
        type = "artifact",
        format = "maven",
        version = "1.0",
        sizeBytes = 1024,
    )

    private fun checksumArtifact(name: String) = ChecksumArtifact(
        checksumSha256 = "abc123",
        contentType = "application/java-archive",
        createdAt = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        downloadCount = 3,
        id = UUID.randomUUID(),
        name = name,
        path = "com/example/$name",
        repositoryKey = "maven-local",
        sizeBytes = 2048,
        version = "1.0",
    )

    private fun errorBody() = "{\"error\":\"boom\"}".toResponseBody("application/json".toMediaType())
}

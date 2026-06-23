package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.artifacts.ArtifactDetailUiState
import com.artifactkeeper.android.ui.screens.artifacts.ArtifactDetailViewModel
import com.artifactkeeper.client.apis.ArtifactLabelsApi
import com.artifactkeeper.client.apis.ArtifactsApi
import com.artifactkeeper.client.models.ArtifactLabelResponse
import com.artifactkeeper.client.models.ArtifactLabelsListResponse
import com.artifactkeeper.client.models.ArtifactMetadataResponse
import com.artifactkeeper.client.models.ArtifactResponse
import com.artifactkeeper.client.models.ArtifactStatsResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
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
import retrofit2.Response
import java.time.OffsetDateTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ArtifactDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockArtifactsApi = mockk<ArtifactsApi>()
    private val mockLabelsApi = mockk<ArtifactLabelsApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { artifactsApi } returns mockArtifactsApi
        every { artifactLabelsApi } returns mockLabelsApi
    }

    private val artifactId = UUID.randomUUID()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // Initial state
    // =========================================================================

    @Test
    fun `initial state is empty and not loading`() {
        val vm = ArtifactDetailViewModel(mockApiClient)
        val state = vm.uiState.value
        assertEquals(ArtifactDetailUiState(), state)
        assertNull(state.artifact)
        assertNull(state.metadata)
        assertNull(state.stats)
        assertTrue(state.labels.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    // =========================================================================
    // load
    // =========================================================================

    @Test
    fun `load populates artifact metadata stats and labels`() {
        stubLoad()
        val vm = ArtifactDetailViewModel(mockApiClient)

        vm.load(artifactId.toString())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("app.jar", state.artifact?.name)
        assertEquals("maven", state.metadata?.format)
        assertEquals(42L, state.stats?.downloadCount)
        assertEquals(1, state.labels.size)
        assertEquals("env", state.labels.first().key)
        assertEquals("prod", state.labels.first().value)
    }

    @Test
    fun `load sets error when the artifact call fails`() {
        coEvery { mockArtifactsApi.getArtifact(any()) } returns Response.error(404, errorBody())
        val vm = ArtifactDetailViewModel(mockApiClient)

        vm.load(artifactId.toString())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(true, state.error?.isNotBlank())
        assertNull(state.artifact)
    }

    @Test
    fun `load tolerates optional metadata and stats failures`() {
        coEvery { mockArtifactsApi.getArtifact(artifactId) } returns Response.success(artifact())
        coEvery { mockArtifactsApi.getArtifactMetadata(artifactId) } throws RuntimeException("no metadata")
        coEvery { mockArtifactsApi.getArtifactStats(artifactId) } throws RuntimeException("no stats")
        coEvery { mockLabelsApi.listArtifactLabels(artifactId) } returns
            Response.success(ArtifactLabelsListResponse(items = emptyList(), total = 0))
        val vm = ArtifactDetailViewModel(mockApiClient)

        vm.load(artifactId.toString())

        val state = vm.uiState.value
        assertNull(state.error)
        assertEquals("app.jar", state.artifact?.name)
        assertNull(state.metadata)
        assertNull(state.stats)
    }

    @Test
    fun `load sets error for a malformed artifact id`() {
        val vm = ArtifactDetailViewModel(mockApiClient)

        vm.load("not-a-uuid")

        assertEquals(true, vm.uiState.value.error?.isNotBlank())
    }

    // =========================================================================
    // addLabel
    // =========================================================================

    @Test
    fun `addLabel posts the label and refreshes the label list`() {
        stubLoad()
        coEvery { mockLabelsApi.addArtifactLabel(any(), any(), any()) } returns
            Response.success(label("team", "platform"))
        coEvery { mockLabelsApi.listArtifactLabels(artifactId) } returnsMany listOf(
            Response.success(ArtifactLabelsListResponse(items = listOf(label("env", "prod")), total = 1)),
            Response.success(
                ArtifactLabelsListResponse(
                    items = listOf(label("env", "prod"), label("team", "platform")),
                    total = 2,
                ),
            ),
        )
        val vm = ArtifactDetailViewModel(mockApiClient)
        vm.load(artifactId.toString())

        vm.addLabel("team", "platform")

        coVerify { mockLabelsApi.addArtifactLabel(artifactId, "team", any()) }
        assertEquals(2, vm.uiState.value.labels.size)
    }

    // =========================================================================
    // deleteLabel
    // =========================================================================

    @Test
    fun `deleteLabel removes the label and refreshes the list`() {
        stubLoad()
        coEvery { mockLabelsApi.deleteArtifactLabel(artifactId, "env") } returns Response.success(Unit)
        coEvery { mockLabelsApi.listArtifactLabels(artifactId) } returnsMany listOf(
            Response.success(ArtifactLabelsListResponse(items = listOf(label("env", "prod")), total = 1)),
            Response.success(ArtifactLabelsListResponse(items = emptyList(), total = 0)),
        )
        val vm = ArtifactDetailViewModel(mockApiClient)
        vm.load(artifactId.toString())

        vm.deleteLabel("env")

        coVerify { mockLabelsApi.deleteArtifactLabel(artifactId, "env") }
        assertTrue(vm.uiState.value.labels.isEmpty())
    }

    @Test
    fun `addLabel surfaces an error without clobbering existing labels`() {
        stubLoad()
        coEvery { mockLabelsApi.addArtifactLabel(any(), any(), any()) } returns Response.error(500, errorBody())
        val vm = ArtifactDetailViewModel(mockApiClient)
        vm.load(artifactId.toString())

        vm.addLabel("team", "platform")

        val state = vm.uiState.value
        assertEquals(true, state.labelError?.isNotBlank())
        assertEquals(1, state.labels.size)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun stubLoad() {
        coEvery { mockArtifactsApi.getArtifact(artifactId) } returns Response.success(artifact())
        coEvery { mockArtifactsApi.getArtifactMetadata(artifactId) } returns Response.success(metadata())
        coEvery { mockArtifactsApi.getArtifactStats(artifactId) } returns Response.success(stats())
        coEvery { mockLabelsApi.listArtifactLabels(artifactId) } returns
            Response.success(ArtifactLabelsListResponse(items = listOf(label("env", "prod")), total = 1))
    }

    private fun artifact() = ArtifactResponse(
        checksumSha256 = "abc123",
        contentType = "application/java-archive",
        createdAt = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        downloadCount = 42,
        id = artifactId,
        name = "app.jar",
        path = "com/example/app/1.0/app.jar",
        repositoryKey = "maven-local",
        sizeBytes = 2048,
        version = "1.0",
    )

    private fun metadata() = ArtifactMetadataResponse(
        artifactId = artifactId,
        format = "maven",
        metadata = null,
        properties = null,
    )

    private fun stats() = ArtifactStatsResponse(
        artifactId = artifactId,
        downloadCount = 42,
        firstDownloaded = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        lastDownloaded = OffsetDateTime.parse("2026-02-01T00:00:00Z"),
    )

    private fun label(key: String, value: String) = ArtifactLabelResponse(
        artifactId = artifactId,
        createdAt = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        id = UUID.randomUUID(),
        key = key,
        value = value,
    )

    private fun errorBody() =
        "{\"error\":\"boom\"}".toResponseBody("application/json".toMediaType())
}

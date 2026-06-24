package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.repositories.RepoLabelsUiState
import com.artifactkeeper.android.ui.screens.repositories.RepoLabelsViewModel
import com.artifactkeeper.client.apis.RepositoryLabelsApi
import com.artifactkeeper.client.models.AddLabelRequest
import com.artifactkeeper.client.models.LabelResponse
import com.artifactkeeper.client.models.LabelsListResponse
import io.mockk.coEvery
import io.mockk.coVerify
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
class RepoLabelsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockLabelsApi = mockk<RepositoryLabelsApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { repositoryLabelsApi } returns mockLabelsApi
    }

    private val repoKey = "maven-releases"
    private val now: OffsetDateTime = OffsetDateTime.parse("2026-06-22T10:00:00Z")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun label(key: String, value: String = "v-$key") = LabelResponse(
        createdAt = now,
        id = UUID.randomUUID(),
        key = key,
        repositoryId = UUID.randomUUID(),
        `value` = value,
    )

    @Test
    fun `initial state is empty`() {
        val state = RepoLabelsUiState()
        assertTrue(state.labels.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `load populates labels sorted by key`() = runTest {
        coEvery { mockLabelsApi.listRepoLabels(repoKey) } returns Response.success(
            LabelsListResponse(items = listOf(label("team"), label("env")), total = 2),
        )

        val vm = RepoLabelsViewModel(mockApiClient)
        vm.load(repoKey)

        coVerify { mockLabelsApi.listRepoLabels(repoKey) }
        assertEquals(listOf("env", "team"), vm.uiState.value.labels.map { it.key })
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `load sets error on failure`() = runTest {
        coEvery { mockLabelsApi.listRepoLabels(repoKey) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = RepoLabelsViewModel(mockApiClient)
        vm.load(repoKey)

        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `addLabel posts key and value then reloads`() = runTest {
        coEvery { mockLabelsApi.addRepoLabel(repoKey, "env", AddLabelRequest(value = "prod")) } returns
            Response.success(label("env", "prod"))
        coEvery { mockLabelsApi.listRepoLabels(repoKey) } returns Response.success(
            LabelsListResponse(items = listOf(label("env", "prod")), total = 1),
        )

        val vm = RepoLabelsViewModel(mockApiClient)
        vm.addLabel(repoKey, key = "env", value = "prod")

        coVerify { mockLabelsApi.addRepoLabel(repoKey, "env", AddLabelRequest(value = "prod")) }
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `addLabel sets error on failure`() = runTest {
        coEvery { mockLabelsApi.addRepoLabel(repoKey, "env", any()) } returns
            Response.error(400, okhttp3.ResponseBody.create(null, "bad"))

        val vm = RepoLabelsViewModel(mockApiClient)
        vm.addLabel(repoKey, key = "env", value = "prod")

        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `deleteLabel calls api with key then reloads`() = runTest {
        coEvery { mockLabelsApi.deleteRepoLabel(repoKey, "env") } returns Response.success(Unit)
        coEvery { mockLabelsApi.listRepoLabels(repoKey) } returns Response.success(
            LabelsListResponse(items = emptyList(), total = 0),
        )

        val vm = RepoLabelsViewModel(mockApiClient)
        vm.deleteLabel(repoKey, key = "env")

        coVerify { mockLabelsApi.deleteRepoLabel(repoKey, "env") }
        assertTrue(vm.uiState.value.labels.isEmpty())
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `deleteLabel sets error on failure`() = runTest {
        coEvery { mockLabelsApi.deleteRepoLabel(repoKey, "env") } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "missing"))

        val vm = RepoLabelsViewModel(mockApiClient)
        vm.deleteLabel(repoKey, key = "env")

        assertNotNull(vm.uiState.value.error)
    }
}

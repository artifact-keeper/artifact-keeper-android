package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.integration.MigrationDetailUiState
import com.artifactkeeper.android.ui.screens.integration.MigrationListUiState
import com.artifactkeeper.android.ui.screens.integration.MigrationViewModel
import com.artifactkeeper.client.apis.MigrationApi
import com.artifactkeeper.client.models.AssessmentResult
import com.artifactkeeper.client.models.MigrationItemResponse
import com.artifactkeeper.client.models.MigrationJobResponse
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
class MigrationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockMigrationApi = mockk<MigrationApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { migrationApi } returns mockMigrationApi
    }

    private val now: OffsetDateTime = OffsetDateTime.parse("2026-06-22T10:00:00Z")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun job(
        status: String = "running",
        id: UUID = UUID.randomUUID(),
        progress: Double = 50.0,
    ) = MigrationJobResponse(
        completedItems = 5,
        config = emptyMap<String, String>(),
        createdAt = now,
        failedItems = 0,
        id = id,
        jobType = "artifactory",
        progressPercent = progress,
        skippedItems = 0,
        sourceConnectionId = UUID.randomUUID(),
        status = status,
        totalBytes = 1000,
        totalItems = 10,
        transferredBytes = 500,
    )

    private fun item(status: String = "completed") = MigrationItemResponse(
        id = UUID.randomUUID(),
        itemType = "artifact",
        jobId = UUID.randomUUID(),
        retryCount = 0,
        sizeBytes = 123,
        sourcePath = "repo/a.jar",
        status = status,
    )

    private fun assessment(jobId: UUID) = AssessmentResult(
        blockers = emptyList(),
        estimatedDurationSeconds = 600,
        groupsCount = 2,
        jobId = jobId,
        permissionsCount = 3,
        repositories = emptyList(),
        status = "ready",
        totalArtifacts = 100,
        totalSizeBytes = 5000,
        usersCount = 4,
        warnings = listOf("slow source"),
    )

    // =========================================================================
    // list
    // =========================================================================

    @Test
    fun `initial list state is empty`() {
        val state = MigrationListUiState()
        assertTrue(state.jobs.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadJobs populates jobs`() = runTest {
        coEvery { mockMigrationApi.listMigrations(null, null, null) } returns Response.success(
            listOf(job(status = "running"), job(status = "completed")),
        )

        val vm = MigrationViewModel(mockApiClient)
        vm.loadJobs()

        assertEquals(2, vm.listState.value.jobs.size)
        assertFalse(vm.listState.value.isLoading)
        assertNull(vm.listState.value.error)
    }

    @Test
    fun `loadJobs sets error on failure`() = runTest {
        coEvery { mockMigrationApi.listMigrations(null, null, null) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = MigrationViewModel(mockApiClient)
        vm.loadJobs()

        assertNotNull(vm.listState.value.error)
    }

    // =========================================================================
    // detail (per-id fetch, items + assessment best-effort)
    // =========================================================================

    @Test
    fun `loadDetail fetches job and items by id`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockMigrationApi.getMigration(id) } returns Response.success(job(id = id))
        coEvery { mockMigrationApi.listMigrationItems(id, null, null, null, null) } returns
            Response.success(listOf(item("completed"), item("failed")))

        val vm = MigrationViewModel(mockApiClient)
        vm.loadDetail(id)

        coVerify { mockMigrationApi.getMigration(id) }
        coVerify { mockMigrationApi.listMigrationItems(id, null, null, null, null) }
        assertEquals(id, vm.detailState.value.job?.id)
        assertEquals(2, vm.detailState.value.items.size)
        assertFalse(vm.detailState.value.isLoading)
    }

    @Test
    fun `loadDetail tolerates missing items`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockMigrationApi.getMigration(id) } returns Response.success(job(id = id))
        coEvery { mockMigrationApi.listMigrationItems(id, null, null, null, null) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "none"))

        val vm = MigrationViewModel(mockApiClient)
        vm.loadDetail(id)

        assertNotNull(vm.detailState.value.job)
        assertTrue(vm.detailState.value.items.isEmpty())
        assertNull(vm.detailState.value.error)
    }

    @Test
    fun `loadDetail sets error when job fetch fails`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockMigrationApi.getMigration(id) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "missing"))

        val vm = MigrationViewModel(mockApiClient)
        vm.loadDetail(id)

        assertNotNull(vm.detailState.value.error)
        assertNull(vm.detailState.value.job)
    }

    @Test
    fun `loadAssessment fetches assessment by id`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockMigrationApi.getAssessment(id) } returns Response.success(assessment(id))

        val vm = MigrationViewModel(mockApiClient)
        vm.loadAssessment(id)

        coVerify { mockMigrationApi.getAssessment(id) }
        assertEquals(id, vm.detailState.value.assessment?.jobId)
    }

    // =========================================================================
    // operate actions
    // =========================================================================

    private fun stubDetailReload(id: UUID) {
        coEvery { mockMigrationApi.getMigration(id) } returns Response.success(job(id = id))
        coEvery { mockMigrationApi.listMigrationItems(id, null, null, null, null) } returns
            Response.success(emptyList())
    }

    @Test
    fun `start calls api and reloads detail`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockMigrationApi.startMigration(id) } returns Response.success(job(id = id, status = "running"))
        stubDetailReload(id)

        val vm = MigrationViewModel(mockApiClient)
        vm.start(id)

        coVerify { mockMigrationApi.startMigration(id) }
        assertNotNull(vm.detailState.value.message)
        assertFalse(vm.detailState.value.isMutating)
    }

    @Test
    fun `assess calls runAssessment and reloads detail`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockMigrationApi.runAssessment(id) } returns Response.success(job(id = id))
        stubDetailReload(id)

        val vm = MigrationViewModel(mockApiClient)
        vm.assess(id)

        coVerify { mockMigrationApi.runAssessment(id) }
        assertNotNull(vm.detailState.value.message)
    }

    @Test
    fun `pause calls api and reloads detail`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockMigrationApi.pauseMigration(id) } returns Response.success(job(id = id, status = "paused"))
        stubDetailReload(id)

        val vm = MigrationViewModel(mockApiClient)
        vm.pause(id)

        coVerify { mockMigrationApi.pauseMigration(id) }
        assertNotNull(vm.detailState.value.message)
    }

    @Test
    fun `resume calls api and reloads detail`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockMigrationApi.resumeMigration(id) } returns Response.success(job(id = id, status = "running"))
        stubDetailReload(id)

        val vm = MigrationViewModel(mockApiClient)
        vm.resume(id)

        coVerify { mockMigrationApi.resumeMigration(id) }
        assertNotNull(vm.detailState.value.message)
    }

    @Test
    fun `cancel calls api and reloads detail`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockMigrationApi.cancelMigration(id) } returns Response.success(job(id = id, status = "cancelled"))
        stubDetailReload(id)

        val vm = MigrationViewModel(mockApiClient)
        vm.cancel(id)

        coVerify { mockMigrationApi.cancelMigration(id) }
        assertNotNull(vm.detailState.value.message)
    }

    @Test
    fun `cancel sets error on failure`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockMigrationApi.cancelMigration(id) } returns
            Response.error(409, okhttp3.ResponseBody.create(null, "conflict"))

        val vm = MigrationViewModel(mockApiClient)
        vm.cancel(id)

        assertNotNull(vm.detailState.value.error)
        assertFalse(vm.detailState.value.isMutating)
    }

    @Test
    fun `initial detail state is empty`() {
        val state = MigrationDetailUiState()
        assertNull(state.job)
        assertTrue(state.items.isEmpty())
        assertNull(state.assessment)
    }
}

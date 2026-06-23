package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.integration.PeerDetailUiState
import com.artifactkeeper.android.ui.screens.integration.PeerDetailViewModel
import com.artifactkeeper.client.apis.PeersApi
import com.artifactkeeper.client.models.PeerInstanceResponse
import com.artifactkeeper.client.models.RunNowResponse
import com.artifactkeeper.client.models.SyncTaskResponse
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
class PeerDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockPeersApi = mockk<PeersApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { peersApi } returns mockPeersApi
    }

    private val peerId = UUID.randomUUID()
    private val now: OffsetDateTime = OffsetDateTime.parse("2026-06-22T10:00:00Z")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun peer() = PeerInstanceResponse(
        cacheSizeBytes = 1_000_000,
        cacheUsagePercent = 42.0,
        cacheUsedBytes = 420_000,
        createdAt = now,
        endpointUrl = "https://peer.test",
        id = peerId,
        isLocal = false,
        name = "edge-1",
        status = "online",
    )

    private fun task(status: String) = SyncTaskResponse(
        artifactId = UUID.randomUUID(),
        artifactSize = 123,
        createdAt = now,
        id = UUID.randomUUID(),
        priority = 1,
        status = status,
        storageKey = "k",
    )

    @Test
    fun `initial state is empty`() {
        val state = PeerDetailUiState()
        assertNull(state.peer)
        assertTrue(state.syncTasks.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `load populates peer sync tasks and assigned repos`() = runTest {
        val repoId = UUID.randomUUID()
        coEvery { mockPeersApi.getPeer(peerId) } returns Response.success(peer())
        coEvery { mockPeersApi.getSyncTasks(peerId, null, null, null, null) } returns Response.success(
            listOf(task("queued"), task("completed")),
        )
        coEvery { mockPeersApi.getAssignedRepos(peerId) } returns Response.success(listOf(repoId))

        val vm = PeerDetailViewModel(mockApiClient)
        vm.load(peerId)

        val state = vm.uiState.value
        assertEquals(peerId, state.peer?.id)
        assertEquals(2, state.syncTasks.size)
        assertEquals(listOf(repoId), state.assignedRepoIds)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `load tolerates missing sync tasks`() = runTest {
        coEvery { mockPeersApi.getPeer(peerId) } returns Response.success(peer())
        coEvery { mockPeersApi.getSyncTasks(peerId, null, null, null, null) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "none"))

        val vm = PeerDetailViewModel(mockApiClient)
        vm.load(peerId)

        assertNotNull(vm.uiState.value.peer)
        assertTrue(vm.uiState.value.syncTasks.isEmpty())
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `load sets error when peer fetch fails`() = runTest {
        coEvery { mockPeersApi.getPeer(peerId) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "missing"))

        val vm = PeerDetailViewModel(mockApiClient)
        vm.load(peerId)

        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.peer)
    }

    @Test
    fun `triggerSync calls api and reloads`() = runTest {
        coEvery { mockPeersApi.triggerSync(peerId) } returns Response.success(Unit)
        coEvery { mockPeersApi.getPeer(peerId) } returns Response.success(peer())
        coEvery { mockPeersApi.getSyncTasks(peerId, null, null, null, null) } returns Response.success(emptyList())

        val vm = PeerDetailViewModel(mockApiClient)
        vm.triggerSync(peerId)

        coVerify { mockPeersApi.triggerSync(peerId) }
        assertNotNull(vm.uiState.value.message)
        assertFalse(vm.uiState.value.isMutating)
    }

    @Test
    fun `triggerSync sets error on failure`() = runTest {
        coEvery { mockPeersApi.triggerSync(peerId) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = PeerDetailViewModel(mockApiClient)
        vm.triggerSync(peerId)

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isMutating)
    }

    @Test
    fun `runSubscriptionNow calls api with repo id`() = runTest {
        val repoId = UUID.randomUUID()
        coEvery { mockPeersApi.runSubscriptionNow(peerId, repoId) } returns Response.success(
            RunNowResponse(status = "queued", tasksQueued = 3),
        )
        coEvery { mockPeersApi.getPeer(peerId) } returns Response.success(peer())
        coEvery { mockPeersApi.getSyncTasks(peerId, null, null, null, null) } returns Response.success(emptyList())

        val vm = PeerDetailViewModel(mockApiClient)
        vm.runSubscriptionNow(peerId, repoId)

        coVerify { mockPeersApi.runSubscriptionNow(peerId, repoId) }
        assertNotNull(vm.uiState.value.message)
    }
}

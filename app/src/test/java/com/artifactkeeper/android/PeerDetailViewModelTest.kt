package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.integration.PeerDetailUiState
import com.artifactkeeper.android.ui.screens.integration.PeerDetailViewModel
import com.artifactkeeper.client.apis.PeerInstanceLabelsApi
import com.artifactkeeper.client.apis.PeersApi
import com.artifactkeeper.client.models.AddPeerLabelRequest
import com.artifactkeeper.client.models.PeerInstanceResponse
import com.artifactkeeper.client.models.PeerLabelEntrySchema
import com.artifactkeeper.client.models.PeerLabelResponse
import com.artifactkeeper.client.models.PeerLabelsListResponse
import com.artifactkeeper.client.models.RunNowResponse
import com.artifactkeeper.client.models.SetPeerLabelsRequest
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
    private val mockLabelsApi = mockk<PeerInstanceLabelsApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { peersApi } returns mockPeersApi
        every { peerInstanceLabelsApi } returns mockLabelsApi
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

    private fun label(key: String, value: String = "v-$key") = PeerLabelResponse(
        createdAt = now,
        id = UUID.randomUUID(),
        key = key,
        peerInstanceId = peerId,
        `value` = value,
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
        coEvery { mockLabelsApi.listLabels(peerId) } returns Response.success(
            PeerLabelsListResponse(items = listOf(label("region"), label("env")), total = 2),
        )

        val vm = PeerDetailViewModel(mockApiClient)
        vm.load(peerId)

        val state = vm.uiState.value
        assertEquals(peerId, state.peer?.id)
        assertEquals(2, state.syncTasks.size)
        assertEquals(listOf(repoId), state.assignedRepoIds)
        // labels sorted by key
        assertEquals(listOf("env", "region"), state.labels.map { it.key })
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

    // =========================================================================
    // peer instance labels
    // =========================================================================

    private fun stubReload() {
        coEvery { mockPeersApi.getPeer(peerId) } returns Response.success(peer())
        coEvery { mockPeersApi.getSyncTasks(peerId, null, null, null, null) } returns Response.success(emptyList())
        coEvery { mockPeersApi.getAssignedRepos(peerId) } returns Response.success(emptyList())
        coEvery { mockLabelsApi.listLabels(peerId) } returns Response.success(
            PeerLabelsListResponse(items = emptyList(), total = 0),
        )
    }

    @Test
    fun `addLabel posts key and value then reloads`() = runTest {
        coEvery { mockLabelsApi.addLabel(peerId, "env", AddPeerLabelRequest(value = "prod")) } returns
            Response.success(label("env", "prod"))
        stubReload()

        val vm = PeerDetailViewModel(mockApiClient)
        vm.addLabel(peerId, key = "env", value = "prod")

        coVerify { mockLabelsApi.addLabel(peerId, "env", AddPeerLabelRequest(value = "prod")) }
        coVerify { mockLabelsApi.listLabels(peerId) }
        assertNotNull(vm.uiState.value.message)
        assertFalse(vm.uiState.value.isMutating)
    }

    @Test
    fun `addLabel sets error on failure`() = runTest {
        coEvery { mockLabelsApi.addLabel(peerId, "env", any()) } returns
            Response.error(400, okhttp3.ResponseBody.create(null, "bad"))

        val vm = PeerDetailViewModel(mockApiClient)
        vm.addLabel(peerId, key = "env", value = "prod")

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isMutating)
    }

    @Test
    fun `deleteLabel calls api with key then reloads`() = runTest {
        coEvery { mockLabelsApi.deleteLabel(peerId, "env") } returns Response.success(Unit)
        stubReload()

        val vm = PeerDetailViewModel(mockApiClient)
        vm.deleteLabel(peerId, key = "env")

        coVerify { mockLabelsApi.deleteLabel(peerId, "env") }
        coVerify { mockLabelsApi.listLabels(peerId) }
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `deleteLabel sets error on failure`() = runTest {
        coEvery { mockLabelsApi.deleteLabel(peerId, "env") } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "missing"))

        val vm = PeerDetailViewModel(mockApiClient)
        vm.deleteLabel(peerId, key = "env")

        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `setLabels replaces full label set then reloads`() = runTest {
        val entries = listOf(
            PeerLabelEntrySchema(key = "env", `value` = "prod"),
            PeerLabelEntrySchema(key = "region", `value` = "us-east"),
        )
        coEvery { mockLabelsApi.setLabels(peerId, SetPeerLabelsRequest(labels = entries)) } returns
            Response.success(
                PeerLabelsListResponse(items = listOf(label("env", "prod"), label("region", "us-east")), total = 2),
            )
        stubReload()

        val vm = PeerDetailViewModel(mockApiClient)
        vm.setLabels(peerId, entries)

        coVerify { mockLabelsApi.setLabels(peerId, SetPeerLabelsRequest(labels = entries)) }
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `setLabels sets error on failure`() = runTest {
        coEvery { mockLabelsApi.setLabels(peerId, any()) } returns
            Response.error(422, okhttp3.ResponseBody.create(null, "invalid"))

        val vm = PeerDetailViewModel(mockApiClient)
        vm.setLabels(peerId, listOf(PeerLabelEntrySchema(key = "env", `value` = "prod")))

        assertNotNull(vm.uiState.value.error)
    }
}

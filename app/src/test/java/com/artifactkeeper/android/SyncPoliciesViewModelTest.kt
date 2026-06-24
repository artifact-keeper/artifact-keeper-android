package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.integration.SyncPoliciesUiState
import com.artifactkeeper.android.ui.screens.integration.SyncPoliciesViewModel
import com.artifactkeeper.client.apis.PeersApi
import com.artifactkeeper.client.models.EvaluationResultResponse
import com.artifactkeeper.client.models.SyncPolicyListResponse
import com.artifactkeeper.client.models.SyncPolicyResponse
import com.artifactkeeper.client.models.TogglePolicyPayload
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
class SyncPoliciesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockPeersApi = mockk<PeersApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { peersApi } returns mockPeersApi
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

    private fun policy(
        name: String,
        enabled: Boolean = true,
        id: UUID = UUID.randomUUID(),
        priority: Int = 0,
    ) = SyncPolicyResponse(
        artifactFilter = emptyMap<String, String>(),
        createdAt = now,
        description = "desc for $name",
        enabled = enabled,
        filter = "*.tar.gz",
        id = id,
        name = name,
        peerSelector = emptyMap<String, String>(),
        precedence = 0,
        priority = priority,
        replicationMode = "pull",
        repoSelector = emptyMap<String, String>(),
        updatedAt = now,
    )

    // =========================================================================
    // UI state
    // =========================================================================

    @Test
    fun `initial state is empty`() {
        val state = SyncPoliciesUiState()
        assertTrue(state.policies.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNull(state.message)
    }

    // =========================================================================
    // loadPolicies
    // =========================================================================

    @Test
    fun `loadPolicies populates policies sorted by priority`() = runTest {
        coEvery { mockPeersApi.listSyncPolicies() } returns Response.success(
            SyncPolicyListResponse(
                items = listOf(
                    policy("low", priority = 1),
                    policy("high", priority = 10),
                ),
                total = 2,
            ),
        )

        val vm = SyncPoliciesViewModel(mockApiClient)
        vm.loadPolicies()

        val state = vm.uiState.value
        assertEquals(2, state.policies.size)
        // higher priority sorts first
        assertEquals("high", state.policies.first().name)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadPolicies sets error on failure`() = runTest {
        coEvery { mockPeersApi.listSyncPolicies() } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = SyncPoliciesViewModel(mockApiClient)
        vm.loadPolicies()

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    // =========================================================================
    // loadPolicyDetail (per-id call, not list reuse)
    // =========================================================================

    @Test
    fun `loadPolicyDetail fetches the policy by id`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockPeersApi.getSyncPolicy(id) } returns Response.success(policy("detail", id = id))

        val vm = SyncPoliciesViewModel(mockApiClient)
        vm.loadPolicyDetail(id)

        coVerify { mockPeersApi.getSyncPolicy(id) }
        assertEquals("detail", vm.detailState.value.policy?.name)
        assertFalse(vm.detailState.value.isLoading)
    }

    @Test
    fun `loadPolicyDetail sets error on failure`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockPeersApi.getSyncPolicy(id) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "missing"))

        val vm = SyncPoliciesViewModel(mockApiClient)
        vm.loadPolicyDetail(id)

        assertNotNull(vm.detailState.value.error)
    }

    // =========================================================================
    // togglePolicy
    // =========================================================================

    @Test
    fun `togglePolicy posts enabled payload and reloads`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockPeersApi.togglePolicy(id, TogglePolicyPayload(enabled = false)) } returns
            Response.success(policy("p", enabled = false, id = id))
        coEvery { mockPeersApi.listSyncPolicies() } returns Response.success(
            SyncPolicyListResponse(items = listOf(policy("p", enabled = false, id = id)), total = 1),
        )

        val vm = SyncPoliciesViewModel(mockApiClient)
        vm.togglePolicy(id, enabled = false)

        coVerify { mockPeersApi.togglePolicy(id, TogglePolicyPayload(enabled = false)) }
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `togglePolicy sets error on failure`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockPeersApi.togglePolicy(id, any()) } returns
            Response.error(409, okhttp3.ResponseBody.create(null, "conflict"))

        val vm = SyncPoliciesViewModel(mockApiClient)
        vm.togglePolicy(id, enabled = true)

        assertNotNull(vm.uiState.value.error)
    }

    // =========================================================================
    // evaluatePolicies
    // =========================================================================

    @Test
    fun `evaluatePolicies calls api and surfaces result summary`() = runTest {
        coEvery { mockPeersApi.evaluatePolicies() } returns Response.success(
            EvaluationResultResponse(
                created = 3,
                policiesEvaluated = 5,
                removed = 1,
                retroactiveTasksQueued = 2,
                updated = 4,
            ),
        )
        coEvery { mockPeersApi.listSyncPolicies() } returns Response.success(
            SyncPolicyListResponse(items = emptyList(), total = 0),
        )

        val vm = SyncPoliciesViewModel(mockApiClient)
        vm.evaluatePolicies()

        coVerify { mockPeersApi.evaluatePolicies() }
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `evaluatePolicies sets error on failure`() = runTest {
        coEvery { mockPeersApi.evaluatePolicies() } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = SyncPoliciesViewModel(mockApiClient)
        vm.evaluatePolicies()

        assertNotNull(vm.uiState.value.error)
    }
}

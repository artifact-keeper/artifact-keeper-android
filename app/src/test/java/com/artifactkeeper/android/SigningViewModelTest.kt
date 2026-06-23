package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.security.SigningUiState
import com.artifactkeeper.android.ui.screens.security.SigningViewModel
import com.artifactkeeper.client.apis.SigningApi
import com.artifactkeeper.client.models.CreateKeyPayload
import com.artifactkeeper.client.models.KeyListResponse
import com.artifactkeeper.client.models.SigningKeyPublic
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
class SigningViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockSigningApi = mockk<SigningApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { signingApi } returns mockSigningApi
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

    private fun key(name: String, active: Boolean = true, id: UUID = UUID.randomUUID()) = SigningKeyPublic(
        algorithm = "ed25519",
        createdAt = now,
        id = id,
        isActive = active,
        keyType = "gpg",
        name = name,
        publicKeyPem = "-----BEGIN PUBLIC KEY-----\nABC\n-----END PUBLIC KEY-----",
    )

    // =========================================================================
    // UI state
    // =========================================================================

    @Test
    fun `initial state is empty`() {
        val state = SigningUiState()
        assertTrue(state.keys.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNull(state.message)
    }

    // =========================================================================
    // loadKeys
    // =========================================================================

    @Test
    fun `loadKeys populates keys with active first`() = runTest {
        coEvery { mockSigningApi.listKeys(null) } returns Response.success(
            KeyListResponse(
                propertyKeys = listOf(key("revoked", active = false), key("live", active = true)),
                total = 2,
            ),
        )

        val vm = SigningViewModel(mockApiClient)
        vm.loadKeys()

        val state = vm.uiState.value
        assertEquals(2, state.keys.size)
        // active sorts before inactive
        assertTrue(state.keys.first().isActive)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadKeys sets error on failure`() = runTest {
        coEvery { mockSigningApi.listKeys(null) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = SigningViewModel(mockApiClient)
        vm.loadKeys()

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    // =========================================================================
    // createKey
    // =========================================================================

    @Test
    fun `createKey posts payload and reloads`() = runTest {
        coEvery { mockSigningApi.createKey(any()) } returns Response.success(key("new"))
        coEvery { mockSigningApi.listKeys(null) } returns Response.success(
            KeyListResponse(propertyKeys = listOf(key("new")), total = 1),
        )

        val vm = SigningViewModel(mockApiClient)
        vm.createKey(name = "new", algorithm = "ed25519", keyType = "gpg")

        coVerify {
            mockSigningApi.createKey(
                CreateKeyPayload(name = "new", algorithm = "ed25519", keyType = "gpg"),
            )
        }
        assertEquals(1, vm.uiState.value.keys.size)
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `createKey sets error on failure`() = runTest {
        coEvery { mockSigningApi.createKey(any()) } returns
            Response.error(400, okhttp3.ResponseBody.create(null, "bad"))

        val vm = SigningViewModel(mockApiClient)
        vm.createKey(name = "x", algorithm = null, keyType = null)

        assertNotNull(vm.uiState.value.error)
    }

    // =========================================================================
    // revoke / rotate / delete
    // =========================================================================

    @Test
    fun `revokeKey calls api and reloads`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockSigningApi.revokeKey(id) } returns Response.success(Unit)
        coEvery { mockSigningApi.listKeys(null) } returns Response.success(
            KeyListResponse(propertyKeys = emptyList(), total = 0),
        )

        val vm = SigningViewModel(mockApiClient)
        vm.revokeKey(id)

        coVerify { mockSigningApi.revokeKey(id) }
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `rotateKey calls api and reloads`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockSigningApi.rotateKey(id) } returns Response.success(key("rotated"))
        coEvery { mockSigningApi.listKeys(null) } returns Response.success(
            KeyListResponse(propertyKeys = listOf(key("rotated")), total = 1),
        )

        val vm = SigningViewModel(mockApiClient)
        vm.rotateKey(id)

        coVerify { mockSigningApi.rotateKey(id) }
        assertEquals(1, vm.uiState.value.keys.size)
    }

    @Test
    fun `deleteKey calls api and reloads`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockSigningApi.deleteKey(id) } returns Response.success(Unit)
        coEvery { mockSigningApi.listKeys(null) } returns Response.success(
            KeyListResponse(propertyKeys = emptyList(), total = 0),
        )

        val vm = SigningViewModel(mockApiClient)
        vm.deleteKey(id)

        coVerify { mockSigningApi.deleteKey(id) }
        assertTrue(vm.uiState.value.keys.isEmpty())
    }

    @Test
    fun `revokeKey sets error on failure`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockSigningApi.revokeKey(id) } returns
            Response.error(409, okhttp3.ResponseBody.create(null, "conflict"))

        val vm = SigningViewModel(mockApiClient)
        vm.revokeKey(id)

        assertNotNull(vm.uiState.value.error)
    }
}

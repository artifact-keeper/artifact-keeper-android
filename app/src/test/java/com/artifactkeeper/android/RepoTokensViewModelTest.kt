package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.repositories.RepoTokensUiState
import com.artifactkeeper.android.ui.screens.repositories.RepoTokensViewModel
import com.artifactkeeper.client.apis.RepositoryTokensApi
import com.artifactkeeper.client.models.CreateRepoTokenRequest
import com.artifactkeeper.client.models.CreateRepoTokenResponse
import com.artifactkeeper.client.models.RepoTokenListResponse
import com.artifactkeeper.client.models.RepoTokenResponse
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
class RepoTokensViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockTokensApi = mockk<RepositoryTokensApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { repositoryTokensApi } returns mockTokensApi
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

    private fun token(
        name: String,
        revoked: Boolean = false,
        id: UUID = UUID.randomUUID(),
    ) = RepoTokenResponse(
        createdAt = now,
        id = id,
        isExpired = false,
        isRevoked = revoked,
        name = name,
        scopes = listOf("read", "write"),
        tokenPrefix = "ak_${name.take(4)}",
    )

    @Test
    fun `initial state is empty`() {
        val state = RepoTokensUiState()
        assertTrue(state.tokens.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNull(state.newTokenSecret)
    }

    @Test
    fun `load populates tokens active first`() = runTest {
        coEvery { mockTokensApi.listRepoTokens(repoKey) } returns Response.success(
            RepoTokenListResponse(items = listOf(token("revoked", revoked = true), token("live"))),
        )

        val vm = RepoTokensViewModel(mockApiClient)
        vm.load(repoKey)

        coVerify { mockTokensApi.listRepoTokens(repoKey) }
        assertEquals(2, vm.uiState.value.tokens.size)
        assertFalse(vm.uiState.value.tokens.first().isRevoked)
    }

    @Test
    fun `load sets error on failure`() = runTest {
        coEvery { mockTokensApi.listRepoTokens(repoKey) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = RepoTokensViewModel(mockApiClient)
        vm.load(repoKey)

        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `loadTokenDetail fetches a single token by id`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockTokensApi.getRepoToken(repoKey, id) } returns Response.success(token("detail", id = id))

        val vm = RepoTokensViewModel(mockApiClient)
        vm.loadTokenDetail(repoKey, id)

        coVerify { mockTokensApi.getRepoToken(repoKey, id) }
        assertEquals("detail", vm.tokenDetailState.value.token?.name)
    }

    @Test
    fun `loadTokenDetail sets error on failure`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockTokensApi.getRepoToken(repoKey, id) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "missing"))

        val vm = RepoTokensViewModel(mockApiClient)
        vm.loadTokenDetail(repoKey, id)

        assertNotNull(vm.tokenDetailState.value.error)
    }

    @Test
    fun `createToken posts request and exposes the one-time secret`() = runTest {
        val request = CreateRepoTokenRequest(
            name = "ci",
            scopes = listOf("read", "write"),
            expiresInDays = 30,
        )
        coEvery { mockTokensApi.createRepoToken(repoKey, request) } returns Response.success(
            CreateRepoTokenResponse(
                id = UUID.randomUUID(),
                name = "ci",
                repositoryKey = repoKey,
                token = "ak_secret_value",
            ),
        )
        coEvery { mockTokensApi.listRepoTokens(repoKey) } returns Response.success(
            RepoTokenListResponse(items = listOf(token("ci"))),
        )

        val vm = RepoTokensViewModel(mockApiClient)
        vm.createToken(repoKey, name = "ci", scopes = listOf("read", "write"), expiresInDays = 30)

        coVerify { mockTokensApi.createRepoToken(repoKey, request) }
        assertEquals("ak_secret_value", vm.uiState.value.newTokenSecret)
    }

    @Test
    fun `createToken sets error on failure`() = runTest {
        coEvery { mockTokensApi.createRepoToken(repoKey, any()) } returns
            Response.error(400, okhttp3.ResponseBody.create(null, "bad"))

        val vm = RepoTokensViewModel(mockApiClient)
        vm.createToken(repoKey, name = "ci", scopes = listOf("read"), expiresInDays = null)

        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `revokeToken calls api with id then reloads`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockTokensApi.revokeRepoToken(repoKey, id) } returns Response.success(Unit)
        coEvery { mockTokensApi.listRepoTokens(repoKey) } returns Response.success(
            RepoTokenListResponse(items = listOf(token("ci", revoked = true, id = id))),
        )

        val vm = RepoTokensViewModel(mockApiClient)
        vm.revokeToken(repoKey, id)

        coVerify { mockTokensApi.revokeRepoToken(repoKey, id) }
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `revokeToken sets error on failure`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockTokensApi.revokeRepoToken(repoKey, id) } returns
            Response.error(409, okhttp3.ResponseBody.create(null, "conflict"))

        val vm = RepoTokensViewModel(mockApiClient)
        vm.revokeToken(repoKey, id)

        assertNotNull(vm.uiState.value.error)
    }
}

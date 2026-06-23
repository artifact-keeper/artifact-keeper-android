package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.security.QualityViewModel
import com.artifactkeeper.android.ui.screens.security.RepoSecurityViewModel
import com.artifactkeeper.android.ui.screens.security.SigningViewModel
import com.artifactkeeper.client.apis.QualityApi
import com.artifactkeeper.client.apis.SecurityApi
import com.artifactkeeper.client.apis.SigningApi
import com.artifactkeeper.client.models.CheckResponse
import com.artifactkeeper.client.models.GateEvaluationResponse
import com.artifactkeeper.client.models.RepoHealthResponse
import com.artifactkeeper.client.models.RepoSecurityResponse
import com.artifactkeeper.client.models.ScanListResponse
import com.artifactkeeper.client.models.ScoreResponse
import com.artifactkeeper.client.models.SigningConfigResponse
import com.artifactkeeper.client.models.SigningKeyPublic
import com.artifactkeeper.client.models.TriggerChecksRequest
import com.artifactkeeper.client.models.TriggerChecksResponse
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
class SecurityReadsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockSigningApi = mockk<SigningApi>()
    private val mockSecurityApi = mockk<SecurityApi>()
    private val mockQualityApi = mockk<QualityApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { signingApi } returns mockSigningApi
        every { securityApi } returns mockSecurityApi
        every { qualityApi } returns mockQualityApi
    }

    private val now: OffsetDateTime = OffsetDateTime.parse("2026-06-22T10:00:00Z")
    private val repoId = UUID.randomUUID()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun key(id: UUID) = SigningKeyPublic(
        algorithm = "ed25519",
        createdAt = now,
        id = id,
        isActive = true,
        keyType = "gpg",
        name = "release",
        publicKeyPem = "PEM-IN-LIST",
    )

    private fun score() = ScoreResponse(
        acknowledgedCount = 0,
        calculatedAt = now,
        criticalCount = 0,
        grade = "A",
        highCount = 0,
        id = UUID.randomUUID(),
        lowCount = 0,
        mediumCount = 0,
        repositoryId = repoId,
        score = 95,
        totalFindings = 0,
    )

    private fun signingConfig() = SigningConfigResponse(
        repositoryId = repoId,
        requireSignatures = true,
        signMetadata = true,
        signPackages = true,
    )

    private fun check(id: UUID) = CheckResponse(
        artifactId = UUID.randomUUID(),
        checkType = "metadata",
        createdAt = now,
        criticalCount = 0,
        highCount = 1,
        id = id,
        infoCount = 0,
        issuesCount = 1,
        lowCount = 0,
        mediumCount = 0,
        repositoryId = repoId,
        status = "completed",
    )

    private fun repoHealth(key: String) = RepoHealthResponse(
        artifactsEvaluated = 5,
        artifactsFailing = 1,
        artifactsPassing = 4,
        healthGrade = "B",
        healthScore = 75,
        repositoryId = repoId,
        repositoryKey = key,
    )

    private fun gateEval(passed: Boolean) = GateEvaluationResponse(
        action = "block",
        componentScores = emptyMap<String, Any>(),
        gateName = "Release Gate",
        healthGrade = "B",
        healthScore = 75,
        passed = passed,
        violations = emptyList(),
    )

    // =========================================================================
    // SigningViewModel.loadKeyDetail (get_key + get_public_key)
    // =========================================================================

    @Test
    fun `loadKeyDetail populates key and public key pem`() = runTest {
        val keyId = UUID.randomUUID()
        coEvery { mockSigningApi.getKey(keyId) } returns Response.success(key(keyId))
        coEvery { mockSigningApi.getPublicKey(keyId) } returns Response.success("PEM-FETCHED")

        val vm = SigningViewModel(mockApiClient)
        vm.loadKeyDetail(keyId)

        val state = vm.keyDetailState.value
        assertEquals(keyId, state.key?.id)
        assertEquals("PEM-FETCHED", state.publicKeyPem)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadKeyDetail sets error when key fetch fails`() = runTest {
        val keyId = UUID.randomUUID()
        coEvery { mockSigningApi.getKey(keyId) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "missing"))

        val vm = SigningViewModel(mockApiClient)
        vm.loadKeyDetail(keyId)

        assertNotNull(vm.keyDetailState.value.error)
        assertNull(vm.keyDetailState.value.key)
    }

    // =========================================================================
    // RepoSecurityViewModel signing config (get_repo_signing_config + get_repo_public_key)
    // =========================================================================

    @Test
    fun `loadRepoSecurity also loads signing config and repo public key`() = runTest {
        coEvery { mockSecurityApi.getRepoSecurity("maven") } returns Response.success(
            RepoSecurityResponse(config = null, score = score()),
        )
        coEvery { mockSecurityApi.listRepoScans("maven") } returns Response.success(
            ScanListResponse(items = emptyList(), total = 0),
        )
        coEvery { mockSigningApi.getRepoSigningConfig(repoId) } returns Response.success(signingConfig())
        coEvery { mockSigningApi.getRepoPublicKey(repoId) } returns Response.success("REPO-PEM")

        val vm = RepoSecurityViewModel(mockApiClient)
        vm.loadRepoSecurity("maven")

        val state = vm.uiState.value
        assertNotNull(state.signingConfig)
        assertEquals("REPO-PEM", state.repoPublicKey)
        assertNull(state.error)
    }

    @Test
    fun `loadRepoSecurity tolerates missing signing config`() = runTest {
        coEvery { mockSecurityApi.getRepoSecurity("maven") } returns Response.success(
            RepoSecurityResponse(config = null, score = score()),
        )
        coEvery { mockSecurityApi.listRepoScans("maven") } returns Response.success(
            ScanListResponse(items = emptyList(), total = 0),
        )
        coEvery { mockSigningApi.getRepoSigningConfig(repoId) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "none"))

        val vm = RepoSecurityViewModel(mockApiClient)
        vm.loadRepoSecurity("maven")

        assertNull(vm.uiState.value.signingConfig)
        assertNull(vm.uiState.value.error)
    }

    // =========================================================================
    // QualityViewModel check detail (get_check)
    // =========================================================================

    @Test
    fun `loadCheckDetail populates selected check`() = runTest {
        val checkId = UUID.randomUUID()
        coEvery { mockQualityApi.getCheck(checkId) } returns Response.success(check(checkId))

        val vm = QualityViewModel(mockApiClient)
        vm.loadCheckDetail(checkId)

        assertEquals(checkId, vm.artifactState.value.selectedCheck?.id)
    }

    // =========================================================================
    // QualityViewModel triggerChecks + evaluateGate (operate actions)
    // =========================================================================

    @Test
    fun `triggerChecks posts request for the artifact and reloads`() = runTest {
        val artifactId = UUID.randomUUID()
        coEvery { mockQualityApi.triggerChecks(any()) } returns
            Response.success(TriggerChecksResponse(artifactsQueued = 1, message = "queued"))
        coEvery { mockQualityApi.getArtifactHealth(artifactId) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "pending"))
        coEvery { mockQualityApi.listChecks(artifactId, null) } returns Response.success(emptyList())

        val vm = QualityViewModel(mockApiClient)
        vm.triggerChecks(artifactId)

        coVerify { mockQualityApi.triggerChecks(TriggerChecksRequest(artifactId = artifactId)) }
        assertNotNull(vm.artifactState.value.message)
        assertFalse(vm.artifactState.value.isMutating)
    }

    @Test
    fun `evaluateGate populates gate evaluation`() = runTest {
        val artifactId = UUID.randomUUID()
        coEvery { mockQualityApi.evaluateGate(artifactId, null) } returns Response.success(gateEval(passed = true))

        val vm = QualityViewModel(mockApiClient)
        vm.evaluateGate(artifactId)

        val eval = vm.artifactState.value.gateEvaluation
        assertNotNull(eval)
        assertTrue(eval!!.passed)
        assertFalse(vm.artifactState.value.isMutating)
    }

    @Test
    fun `evaluateGate sets error on failure`() = runTest {
        val artifactId = UUID.randomUUID()
        coEvery { mockQualityApi.evaluateGate(artifactId, null) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = QualityViewModel(mockApiClient)
        vm.evaluateGate(artifactId)

        assertNotNull(vm.artifactState.value.error)
        assertFalse(vm.artifactState.value.isMutating)
    }

    // =========================================================================
    // QualityViewModel repo health (get_repo_health)
    // =========================================================================

    @Test
    fun `loadRepoHealth populates repo health detail`() = runTest {
        coEvery { mockQualityApi.getRepoHealth("npm") } returns Response.success(repoHealth("npm"))

        val vm = QualityViewModel(mockApiClient)
        vm.loadRepoHealth("npm")

        assertEquals("npm", vm.repoHealthState.value.health?.repositoryKey)
        assertFalse(vm.repoHealthState.value.isLoading)
    }

    @Test
    fun `loadRepoHealth sets error on failure`() = runTest {
        coEvery { mockQualityApi.getRepoHealth("npm") } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "missing"))

        val vm = QualityViewModel(mockApiClient)
        vm.loadRepoHealth("npm")

        assertNotNull(vm.repoHealthState.value.error)
    }
}

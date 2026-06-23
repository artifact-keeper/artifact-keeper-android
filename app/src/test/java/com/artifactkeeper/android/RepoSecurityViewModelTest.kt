package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.security.RepoSecurityUiState
import com.artifactkeeper.android.ui.screens.security.RepoSecurityViewModel
import com.artifactkeeper.android.ui.screens.security.SecurityDashboardUiState
import com.artifactkeeper.client.apis.SecurityApi
import com.artifactkeeper.client.models.DashboardResponse
import com.artifactkeeper.client.models.RepoSecurityResponse
import com.artifactkeeper.client.models.ScanConfigResponse
import com.artifactkeeper.client.models.ScanListResponse
import com.artifactkeeper.client.models.ScanResponse
import com.artifactkeeper.client.models.ScoreResponse
import io.mockk.coEvery
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
class RepoSecurityViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockSecurityApi = mockk<SecurityApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { securityApi } returns mockSecurityApi
    }

    private val repoId = UUID.randomUUID()
    private val now: OffsetDateTime = OffsetDateTime.parse("2026-06-22T10:00:00Z")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun score(grade: String = "B", critical: Int = 1) = ScoreResponse(
        acknowledgedCount = 0,
        calculatedAt = now,
        criticalCount = critical,
        grade = grade,
        highCount = 2,
        id = UUID.randomUUID(),
        lowCount = 0,
        mediumCount = 1,
        repositoryId = repoId,
        score = 80,
        totalFindings = critical + 3,
    )

    private fun config() = ScanConfigResponse(
        blockOnPolicyViolation = true,
        createdAt = now,
        id = UUID.randomUUID(),
        repositoryId = repoId,
        scanEnabled = true,
        scanOnProxy = false,
        scanOnUpload = true,
        severityThreshold = "high",
        updatedAt = now,
    )

    private fun scan() = ScanResponse(
        artifactId = UUID.randomUUID(),
        createdAt = now,
        criticalCount = 1,
        findingsCount = 3,
        highCount = 2,
        id = UUID.randomUUID(),
        infoCount = 0,
        isReused = false,
        lowCount = 0,
        mediumCount = 0,
        repositoryId = repoId,
        scanType = "vulnerability",
        status = "completed",
    )

    private fun dashboard() = DashboardResponse(
        criticalFindings = 5,
        highFindings = 12,
        policyViolationsBlocked = 2,
        reposGradeA = 3,
        reposGradeF = 1,
        reposWithScanning = 7,
        totalFindings = 40,
        totalScans = 100,
    )

    // =========================================================================
    // UI state
    // =========================================================================

    @Test
    fun `initial repo state is empty`() {
        val state = RepoSecurityUiState()
        assertNull(state.config)
        assertNull(state.score)
        assertTrue(state.scans.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `initial dashboard state is empty`() {
        val state = SecurityDashboardUiState()
        assertNull(state.dashboard)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    // =========================================================================
    // loadRepoSecurity
    // =========================================================================

    @Test
    fun `loadRepoSecurity populates config score and scans`() = runTest {
        coEvery { mockSecurityApi.getRepoSecurity("maven-local") } returns Response.success(
            RepoSecurityResponse(config = config(), score = score()),
        )
        coEvery { mockSecurityApi.listRepoScans("maven-local") } returns Response.success(
            ScanListResponse(items = listOf(scan(), scan()), total = 2),
        )

        val vm = RepoSecurityViewModel(mockApiClient)
        vm.loadRepoSecurity("maven-local")

        val state = vm.uiState.value
        assertNotNull(state.config)
        assertNotNull(state.score)
        assertEquals(2, state.scans.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadRepoSecurity tolerates missing scans`() = runTest {
        coEvery { mockSecurityApi.getRepoSecurity("maven-local") } returns Response.success(
            RepoSecurityResponse(config = config(), score = score()),
        )
        coEvery { mockSecurityApi.listRepoScans("maven-local") } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "no scans"))

        val vm = RepoSecurityViewModel(mockApiClient)
        vm.loadRepoSecurity("maven-local")

        val state = vm.uiState.value
        assertNotNull(state.score)
        assertTrue(state.scans.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `loadRepoSecurity sets error when security fetch fails`() = runTest {
        coEvery { mockSecurityApi.getRepoSecurity("maven-local") } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = RepoSecurityViewModel(mockApiClient)
        vm.loadRepoSecurity("maven-local")

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    // =========================================================================
    // loadDashboard
    // =========================================================================

    @Test
    fun `loadDashboard populates the portfolio dashboard`() = runTest {
        coEvery { mockSecurityApi.getDashboard() } returns Response.success(dashboard())

        val vm = RepoSecurityViewModel(mockApiClient)
        vm.loadDashboard()

        val state = vm.dashboardState.value
        assertNotNull(state.dashboard)
        assertEquals(40L, state.dashboard?.totalFindings)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadDashboard sets error on failure`() = runTest {
        coEvery { mockSecurityApi.getDashboard() } returns
            Response.error(503, okhttp3.ResponseBody.create(null, "unavailable"))

        val vm = RepoSecurityViewModel(mockApiClient)
        vm.loadDashboard()

        assertNotNull(vm.dashboardState.value.error)
        assertFalse(vm.dashboardState.value.isLoading)
    }
}

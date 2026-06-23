package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.security.QualityHealthUiState
import com.artifactkeeper.android.ui.screens.security.QualityArtifactUiState
import com.artifactkeeper.android.ui.screens.security.QualityViewModel
import com.artifactkeeper.client.apis.QualityApi
import com.artifactkeeper.client.models.ArtifactHealthResponse
import com.artifactkeeper.client.models.CheckResponse
import com.artifactkeeper.client.models.GateResponse
import com.artifactkeeper.client.models.HealthDashboardResponse
import com.artifactkeeper.client.models.IssueResponse
import com.artifactkeeper.client.models.RepoHealthResponse
import com.artifactkeeper.client.models.SuppressIssueRequest
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
class QualityViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockQualityApi = mockk<QualityApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { qualityApi } returns mockQualityApi
    }

    private val artifactId = UUID.randomUUID()
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

    private fun repoHealth(key: String, grade: String) = RepoHealthResponse(
        artifactsEvaluated = 10,
        artifactsFailing = 2,
        artifactsPassing = 8,
        healthGrade = grade,
        healthScore = 80,
        repositoryId = UUID.randomUUID(),
        repositoryKey = key,
    )

    private fun dashboard() = HealthDashboardResponse(
        avgHealthScore = 78,
        reposGradeA = 2,
        reposGradeB = 3,
        reposGradeC = 1,
        reposGradeD = 0,
        reposGradeF = 1,
        repositories = listOf(repoHealth("maven", "A"), repoHealth("npm", "C")),
        totalArtifactsEvaluated = 100,
        totalRepositories = 7,
    )

    private fun gate(name: String) = GateResponse(
        action = "block",
        createdAt = now,
        enforceOnDownload = true,
        enforceOnPromotion = true,
        id = UUID.randomUUID(),
        isEnabled = true,
        name = name,
        requiredChecks = listOf("license", "metadata"),
        updatedAt = now,
    )

    private fun check(id: UUID = UUID.randomUUID()) = CheckResponse(
        artifactId = artifactId,
        checkType = "metadata",
        createdAt = now,
        criticalCount = 0,
        highCount = 1,
        id = id,
        infoCount = 0,
        issuesCount = 2,
        lowCount = 1,
        mediumCount = 0,
        repositoryId = repoId,
        status = "completed",
    )

    private fun artifactHealth() = ArtifactHealthResponse(
        artifactId = artifactId,
        checks = emptyList(),
        checksPassed = 3,
        checksTotal = 4,
        criticalIssues = 0,
        healthGrade = "B",
        healthScore = 82,
        totalIssues = 3,
    )

    private fun issue(suppressed: Boolean = false) = IssueResponse(
        artifactId = artifactId,
        category = "metadata",
        checkResultId = UUID.randomUUID(),
        createdAt = now,
        id = UUID.randomUUID(),
        isSuppressed = suppressed,
        severity = "high",
        title = "Missing license",
    )

    // =========================================================================
    // UI state
    // =========================================================================

    @Test
    fun `initial health state is empty`() {
        val state = QualityHealthUiState()
        assertNull(state.dashboard)
        assertTrue(state.gates.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `initial artifact state is empty`() {
        val state = QualityArtifactUiState()
        assertNull(state.health)
        assertTrue(state.checks.isEmpty())
        assertTrue(state.issues.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    // =========================================================================
    // loadHealth
    // =========================================================================

    @Test
    fun `loadHealth populates dashboard and gates`() = runTest {
        coEvery { mockQualityApi.getHealthDashboard() } returns Response.success(dashboard())
        coEvery { mockQualityApi.listGates() } returns Response.success(listOf(gate("Release Gate")))

        val vm = QualityViewModel(mockApiClient)
        vm.loadHealth()

        val state = vm.healthState.value
        assertNotNull(state.dashboard)
        assertEquals(1, state.gates.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadHealth tolerates missing gates`() = runTest {
        coEvery { mockQualityApi.getHealthDashboard() } returns Response.success(dashboard())
        coEvery { mockQualityApi.listGates() } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "no gates"))

        val vm = QualityViewModel(mockApiClient)
        vm.loadHealth()

        assertNotNull(vm.healthState.value.dashboard)
        assertTrue(vm.healthState.value.gates.isEmpty())
        assertNull(vm.healthState.value.error)
    }

    @Test
    fun `loadHealth sets error when dashboard fails`() = runTest {
        coEvery { mockQualityApi.getHealthDashboard() } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = QualityViewModel(mockApiClient)
        vm.loadHealth()

        assertNotNull(vm.healthState.value.error)
        assertFalse(vm.healthState.value.isLoading)
    }

    // =========================================================================
    // loadArtifactQuality
    // =========================================================================

    @Test
    fun `loadArtifactQuality aggregates health checks and issues`() = runTest {
        val checkId = UUID.randomUUID()
        coEvery { mockQualityApi.getArtifactHealth(artifactId) } returns Response.success(artifactHealth())
        coEvery { mockQualityApi.listChecks(artifactId, null) } returns Response.success(listOf(check(checkId)))
        coEvery { mockQualityApi.listCheckIssues(checkId) } returns Response.success(listOf(issue(), issue()))

        val vm = QualityViewModel(mockApiClient)
        vm.loadArtifactQuality(artifactId)

        val state = vm.artifactState.value
        assertNotNull(state.health)
        assertEquals(1, state.checks.size)
        assertEquals(2, state.issues.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadArtifactQuality sets error when health fails`() = runTest {
        coEvery { mockQualityApi.getArtifactHealth(artifactId) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = QualityViewModel(mockApiClient)
        vm.loadArtifactQuality(artifactId)

        assertNotNull(vm.artifactState.value.error)
        assertNull(vm.artifactState.value.health)
    }

    // =========================================================================
    // suppress / unsuppress
    // =========================================================================

    @Test
    fun `suppressIssue sends reason and reloads artifact quality`() = runTest {
        val issueId = UUID.randomUUID()
        coEvery { mockQualityApi.suppressIssue(issueId, any()) } returns Response.success(issue(suppressed = true))
        coEvery { mockQualityApi.getArtifactHealth(artifactId) } returns Response.success(artifactHealth())
        coEvery { mockQualityApi.listChecks(artifactId, null) } returns Response.success(emptyList())

        val vm = QualityViewModel(mockApiClient)
        vm.suppressIssue(artifactId, issueId, "accepted risk")

        coVerify { mockQualityApi.suppressIssue(issueId, SuppressIssueRequest(reason = "accepted risk")) }
        assertNull(vm.artifactState.value.error)
    }

    @Test
    fun `unsuppressIssue calls api`() = runTest {
        val issueId = UUID.randomUUID()
        coEvery { mockQualityApi.unsuppressIssue(issueId) } returns Response.success(issue(suppressed = false))
        coEvery { mockQualityApi.getArtifactHealth(artifactId) } returns Response.success(artifactHealth())
        coEvery { mockQualityApi.listChecks(artifactId, null) } returns Response.success(emptyList())

        val vm = QualityViewModel(mockApiClient)
        vm.unsuppressIssue(artifactId, issueId)

        coVerify { mockQualityApi.unsuppressIssue(issueId) }
    }

    @Test
    fun `qualityIssueSeverityRank orders critical first`() {
        assertTrue(
            QualityViewModel.qualityIssueSeverityRank("critical") <
                QualityViewModel.qualityIssueSeverityRank("low"),
        )
    }
}

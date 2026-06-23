package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.security.DependencyTrackUiState
import com.artifactkeeper.android.ui.screens.security.DependencyTrackViewModel
import com.artifactkeeper.android.ui.screens.security.DtProjectDetailUiState
import com.artifactkeeper.client.apis.SecurityApi
import com.artifactkeeper.client.models.DtComponent
import com.artifactkeeper.client.models.DtFinding
import com.artifactkeeper.client.models.DtPolicy
import com.artifactkeeper.client.models.DtPolicyCondition
import com.artifactkeeper.client.models.DtPolicyViolation
import com.artifactkeeper.client.models.DtProject
import com.artifactkeeper.client.models.DtProjectMetrics
import com.artifactkeeper.client.models.DtVulnerability
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

@OptIn(ExperimentalCoroutinesApi::class)
class DependencyTrackViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockSecurityApi = mockk<SecurityApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { securityApi } returns mockSecurityApi
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun project(uuid: String, name: String) = DtProject(name = name, uuid = uuid)

    private fun metrics(critical: Long = 2, high: Long = 3) = DtProjectMetrics(
        critical = critical,
        high = high,
        medium = 1,
        low = 0,
        findingsTotal = critical + high + 1,
        findingsAudited = 1,
        vulnerabilities = critical + high + 1,
        policyViolationsTotal = 0,
    )

    private fun finding(severity: String, vulnId: String) = DtFinding(
        component = DtComponent(name = "libfoo", uuid = "c-1", version = "1.0.0"),
        vulnerability = DtVulnerability(
            severity = severity,
            source = "NVD",
            uuid = "v-$vulnId",
            vulnId = vulnId,
            title = "Issue $vulnId",
        ),
    )

    private fun violation(uuid: String, type: String) = DtPolicyViolation(
        component = DtComponent(name = "libbar", uuid = "c-2"),
        policyCondition = DtPolicyCondition(
            operator = "IS",
            policy = DtPolicy(name = "License", uuid = "pol-1", violationState = "FAIL"),
            subject = "LICENSE",
            uuid = "pc-$uuid",
            value = "GPL-3.0",
        ),
        type = type,
        uuid = uuid,
    )

    // =========================================================================
    // UI state
    // =========================================================================

    @Test
    fun `initial list state is empty`() {
        val state = DependencyTrackUiState()
        assertTrue(state.projects.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `initial detail state is empty`() {
        val state = DtProjectDetailUiState()
        assertNull(state.metrics)
        assertTrue(state.findings.isEmpty())
        assertTrue(state.violations.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    // =========================================================================
    // loadProjects
    // =========================================================================

    @Test
    fun `loadProjects populates the project list`() = runTest {
        coEvery { mockSecurityApi.listProjects() } returns Response.success(
            listOf(project("p-1", "alpha"), project("p-2", "beta")),
        )

        val vm = DependencyTrackViewModel(mockApiClient)
        vm.loadProjects()

        val state = vm.uiState.value
        assertEquals(2, state.projects.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadProjects sets error on failure`() = runTest {
        coEvery { mockSecurityApi.listProjects() } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = DependencyTrackViewModel(mockApiClient)
        vm.loadProjects()

        val state = vm.uiState.value
        assertTrue(state.projects.isEmpty())
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }

    // =========================================================================
    // loadProjectDetail
    // =========================================================================

    @Test
    fun `loadProjectDetail aggregates metrics findings and violations`() = runTest {
        coEvery { mockSecurityApi.getProjectMetrics("p-1") } returns Response.success(metrics())
        coEvery { mockSecurityApi.getProjectFindings("p-1") } returns Response.success(
            listOf(finding("LOW", "CVE-1"), finding("CRITICAL", "CVE-2")),
        )
        coEvery { mockSecurityApi.getProjectViolations("p-1") } returns Response.success(
            listOf(violation("vio-1", "SECURITY")),
        )

        val vm = DependencyTrackViewModel(mockApiClient)
        vm.loadProjectDetail("p-1")

        val state = vm.projectDetailState.value
        assertNotNull(state.metrics)
        assertEquals(2, state.findings.size)
        assertEquals(1, state.violations.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadProjectDetail orders findings most severe first`() = runTest {
        coEvery { mockSecurityApi.getProjectMetrics("p-1") } returns Response.success(metrics())
        coEvery { mockSecurityApi.getProjectFindings("p-1") } returns Response.success(
            listOf(finding("LOW", "CVE-1"), finding("CRITICAL", "CVE-2"), finding("MEDIUM", "CVE-3")),
        )
        coEvery { mockSecurityApi.getProjectViolations("p-1") } returns Response.success(emptyList())

        val vm = DependencyTrackViewModel(mockApiClient)
        vm.loadProjectDetail("p-1")

        val severities = vm.projectDetailState.value.findings.map { it.vulnerability.severity }
        assertEquals(listOf("CRITICAL", "MEDIUM", "LOW"), severities)
    }

    @Test
    fun `loadProjectDetail tolerates missing violations`() = runTest {
        coEvery { mockSecurityApi.getProjectMetrics("p-1") } returns Response.success(metrics())
        coEvery { mockSecurityApi.getProjectFindings("p-1") } returns Response.success(
            listOf(finding("HIGH", "CVE-9")),
        )
        // Policy violations endpoint not available -> 404
        coEvery { mockSecurityApi.getProjectViolations("p-1") } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "no policies"))

        val vm = DependencyTrackViewModel(mockApiClient)
        vm.loadProjectDetail("p-1")

        val state = vm.projectDetailState.value
        assertEquals(1, state.findings.size)
        assertTrue(state.violations.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `loadProjectDetail sets error when metrics fail`() = runTest {
        coEvery { mockSecurityApi.getProjectMetrics("p-1") } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = DependencyTrackViewModel(mockApiClient)
        vm.loadProjectDetail("p-1")

        val state = vm.projectDetailState.value
        assertNotNull(state.error)
        assertNull(state.metrics)
    }

    @Test
    fun `dtSeverityRank orders critical before low`() {
        assertTrue(
            DependencyTrackViewModel.dtSeverityRank("CRITICAL") <
                DependencyTrackViewModel.dtSeverityRank("LOW"),
        )
        assertTrue(
            DependencyTrackViewModel.dtSeverityRank("HIGH") <
                DependencyTrackViewModel.dtSeverityRank("MEDIUM"),
        )
    }
}

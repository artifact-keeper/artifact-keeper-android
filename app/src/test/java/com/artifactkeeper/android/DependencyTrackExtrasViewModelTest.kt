package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.security.DependencyTrackViewModel
import com.artifactkeeper.android.ui.screens.security.DtPoliciesUiState
import com.artifactkeeper.client.apis.SecurityApi
import com.artifactkeeper.client.models.DtComponent
import com.artifactkeeper.client.models.DtComponentFull
import com.artifactkeeper.client.models.DtFinding
import com.artifactkeeper.client.models.DtAnalysisResponse
import com.artifactkeeper.client.models.DtPolicy
import com.artifactkeeper.client.models.DtPolicyConditionFull
import com.artifactkeeper.client.models.DtPolicyFull
import com.artifactkeeper.client.models.DtProjectMetrics
import com.artifactkeeper.client.models.DtVulnerability
import com.artifactkeeper.client.models.UpdateAnalysisBody
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

@OptIn(ExperimentalCoroutinesApi::class)
class DependencyTrackExtrasViewModelTest {

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

    private fun metrics() = DtProjectMetrics(critical = 1, high = 2, findingsTotal = 5, findingsAudited = 2)

    private fun finding(severity: String) = DtFinding(
        component = DtComponent(name = "libfoo", uuid = "c-1", version = "1.0.0"),
        vulnerability = DtVulnerability(severity = severity, source = "NVD", uuid = "v-1", vulnId = "CVE-1"),
    )

    private fun component(name: String) = DtComponentFull(name = name, uuid = "cf-$name", version = "1.0.0")

    private fun policy(name: String) = DtPolicyFull(
        name = name,
        policyConditions = emptyList<DtPolicyConditionFull>(),
        projects = emptyList(),
        tags = emptyList<Any>(),
        uuid = "pol-$name",
        violationState = "FAIL",
    )

    private fun analysisResponse() = DtAnalysisResponse(
        analysisState = "NOT_AFFECTED",
        isSuppressed = true,
    )

    // detail now includes components + metrics history (both optional)

    @Test
    fun `loadProjectDetail also loads components and metrics history`() = runTest {
        coEvery { mockSecurityApi.getProjectMetrics("p-1") } returns Response.success(metrics())
        coEvery { mockSecurityApi.getProjectFindings("p-1") } returns Response.success(listOf(finding("HIGH")))
        coEvery { mockSecurityApi.getProjectViolations("p-1") } returns Response.success(emptyList())
        coEvery { mockSecurityApi.getProjectComponents("p-1") } returns
            Response.success(listOf(component("a"), component("b")))
        coEvery { mockSecurityApi.getProjectMetricsHistory("p-1", any()) } returns
            Response.success(listOf(metrics(), metrics(), metrics()))

        val vm = DependencyTrackViewModel(mockApiClient)
        vm.loadProjectDetail("p-1")

        val state = vm.projectDetailState.value
        assertEquals(2, state.components.size)
        assertEquals(3, state.metricsHistory.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadProjectDetail tolerates missing components and history`() = runTest {
        coEvery { mockSecurityApi.getProjectMetrics("p-1") } returns Response.success(metrics())
        coEvery { mockSecurityApi.getProjectFindings("p-1") } returns Response.success(listOf(finding("HIGH")))
        coEvery { mockSecurityApi.getProjectViolations("p-1") } returns Response.success(emptyList())
        coEvery { mockSecurityApi.getProjectComponents("p-1") } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "no components"))
        coEvery { mockSecurityApi.getProjectMetricsHistory("p-1", any()) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "no history"))

        val vm = DependencyTrackViewModel(mockApiClient)
        vm.loadProjectDetail("p-1")

        val state = vm.projectDetailState.value
        assertNotNull(state.metrics)
        assertTrue(state.components.isEmpty())
        assertTrue(state.metricsHistory.isEmpty())
        assertNull(state.error)
    }

    // policies

    @Test
    fun `initial policies state is empty`() {
        val state = DtPoliciesUiState()
        assertTrue(state.policies.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadPolicies populates policy list`() = runTest {
        coEvery { mockSecurityApi.listDependencyTrackPolicies() } returns
            Response.success(listOf(policy("License"), policy("Severity")))

        val vm = DependencyTrackViewModel(mockApiClient)
        vm.loadPolicies()

        assertEquals(2, vm.policiesState.value.policies.size)
        assertNull(vm.policiesState.value.error)
    }

    @Test
    fun `loadPolicies sets error on failure`() = runTest {
        coEvery { mockSecurityApi.listDependencyTrackPolicies() } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = DependencyTrackViewModel(mockApiClient)
        vm.loadPolicies()

        assertNotNull(vm.policiesState.value.error)
    }

    // update analysis

    @Test
    fun `updateFindingAnalysis sends request and reloads detail`() = runTest {
        coEvery { mockSecurityApi.updateAnalysis(any()) } returns Response.success(analysisResponse())
        // reload after update
        coEvery { mockSecurityApi.getProjectMetrics("p-1") } returns Response.success(metrics())
        coEvery { mockSecurityApi.getProjectFindings("p-1") } returns Response.success(listOf(finding("HIGH")))
        coEvery { mockSecurityApi.getProjectViolations("p-1") } returns Response.success(emptyList())
        coEvery { mockSecurityApi.getProjectComponents("p-1") } returns Response.success(emptyList())
        coEvery { mockSecurityApi.getProjectMetricsHistory("p-1", any()) } returns Response.success(emptyList())

        val vm = DependencyTrackViewModel(mockApiClient)
        vm.updateFindingAnalysis(
            projectUuid = "p-1",
            componentUuid = "c-1",
            vulnerabilityUuid = "v-1",
            state = "NOT_AFFECTED",
            suppressed = true,
            justification = "code not reachable",
        )

        coVerify {
            mockSecurityApi.updateAnalysis(
                UpdateAnalysisBody(
                    componentUuid = "c-1",
                    projectUuid = "p-1",
                    state = "NOT_AFFECTED",
                    vulnerabilityUuid = "v-1",
                    justification = "code not reachable",
                    suppressed = true,
                ),
            )
        }
        assertFalse(vm.projectDetailState.value.isUpdating)
    }

    @Test
    fun `updateFindingAnalysis sets error on failure`() = runTest {
        coEvery { mockSecurityApi.updateAnalysis(any()) } returns
            Response.error(400, okhttp3.ResponseBody.create(null, "bad"))

        val vm = DependencyTrackViewModel(mockApiClient)
        vm.updateFindingAnalysis("p-1", "c-1", "v-1", "EXPLOITABLE", false, null)

        assertNotNull(vm.projectDetailState.value.error)
        assertFalse(vm.projectDetailState.value.isUpdating)
    }
}

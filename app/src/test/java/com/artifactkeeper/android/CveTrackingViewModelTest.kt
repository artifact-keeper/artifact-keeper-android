package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.security.CveDetailUiState
import com.artifactkeeper.android.ui.screens.security.CveTrackingUiState
import com.artifactkeeper.android.ui.screens.security.CveTrackingViewModel
import com.artifactkeeper.client.apis.SbomApi
import com.artifactkeeper.client.apis.SecurityApi
import com.artifactkeeper.client.models.CveHistoryEntry
import com.artifactkeeper.client.models.FindingResponse
import com.artifactkeeper.client.models.UpdateCveStatusRequest
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
class CveTrackingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockSecurityApi = mockk<SecurityApi>()
    private val mockSbomApi = mockk<SbomApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { securityApi } returns mockSecurityApi
        every { sbomApi } returns mockSbomApi
    }

    private val artifactId = UUID.randomUUID()
    private val now: OffsetDateTime = OffsetDateTime.parse("2026-06-22T10:00:00Z")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun cve(
        cveId: String,
        status: String = "open",
        severity: String? = "high",
        score: Double? = 7.5,
    ) = CveHistoryEntry(
        artifactId = artifactId,
        createdAt = now,
        cveId = cveId,
        firstDetectedAt = now,
        id = UUID.randomUUID(),
        lastDetectedAt = now,
        status = status,
        updatedAt = now,
        cvssScore = score,
        severity = severity,
    )

    private fun finding() = FindingResponse(
        artifactId = artifactId,
        createdAt = now,
        id = UUID.randomUUID(),
        isAcknowledged = false,
        scanResultId = UUID.randomUUID(),
        severity = "high",
        title = "Some vuln",
    )

    // =========================================================================
    // UI state
    // =========================================================================

    @Test
    fun `initial tracking state is empty`() {
        val state = CveTrackingUiState()
        assertTrue(state.entries.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `initial detail state is empty`() {
        val state = CveDetailUiState()
        assertTrue(state.history.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isUpdating)
        assertNull(state.error)
    }

    // =========================================================================
    // loadArtifactCves
    // =========================================================================

    @Test
    fun `loadArtifactCves populates entries sorted by severity`() = runTest {
        coEvery { mockSbomApi.getCveHistoryByArtifact(artifactId) } returns Response.success(
            listOf(
                cve("CVE-1", severity = "low", score = 3.0),
                cve("CVE-2", severity = "critical", score = 9.8),
                cve("CVE-3", severity = "medium", score = 5.0),
            ),
        )

        val vm = CveTrackingViewModel(mockApiClient)
        vm.loadArtifactCves(artifactId)

        val severities = vm.uiState.value.entries.map { it.severity }
        assertEquals(listOf("critical", "medium", "low"), severities)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `loadArtifactCves sets error on failure`() = runTest {
        coEvery { mockSbomApi.getCveHistoryByArtifact(artifactId) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = CveTrackingViewModel(mockApiClient)
        vm.loadArtifactCves(artifactId)

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    // =========================================================================
    // loadCveDetail
    // =========================================================================

    @Test
    fun `loadCveDetail populates history for a cve`() = runTest {
        coEvery { mockSbomApi.getCveHistoryByCve("CVE-2") } returns Response.success(
            listOf(cve("CVE-2"), cve("CVE-2")),
        )

        val vm = CveTrackingViewModel(mockApiClient)
        vm.loadCveDetail(artifactId, "CVE-2")

        assertEquals(2, vm.cveDetailState.value.history.size)
        assertFalse(vm.cveDetailState.value.isLoading)
    }

    // =========================================================================
    // updateCveStatus
    // =========================================================================

    @Test
    fun `updateCveStatus sends request and reloads history`() = runTest {
        coEvery {
            mockSbomApi.updateCveStatusByArtifactCve(artifactId, "CVE-2", any())
        } returns Response.success(cve("CVE-2", status = "acknowledged"))
        coEvery { mockSbomApi.getCveHistoryByCve("CVE-2") } returns Response.success(
            listOf(cve("CVE-2", status = "acknowledged")),
        )

        val vm = CveTrackingViewModel(mockApiClient)
        vm.updateCveStatus(artifactId, "CVE-2", "acknowledged", "false positive")

        coVerify {
            mockSbomApi.updateCveStatusByArtifactCve(
                artifactId,
                "CVE-2",
                UpdateCveStatusRequest(status = "acknowledged", reason = "false positive"),
            )
        }
        assertFalse(vm.cveDetailState.value.isUpdating)
        assertEquals(1, vm.cveDetailState.value.history.size)
    }

    @Test
    fun `updateCveStatus sets error on failure`() = runTest {
        coEvery {
            mockSbomApi.updateCveStatusByArtifactCve(artifactId, "CVE-2", any())
        } returns Response.error(400, okhttp3.ResponseBody.create(null, "bad status"))

        val vm = CveTrackingViewModel(mockApiClient)
        vm.updateCveStatus(artifactId, "CVE-2", "bogus", null)

        assertNotNull(vm.cveDetailState.value.error)
        assertFalse(vm.cveDetailState.value.isUpdating)
    }

    // =========================================================================
    // acknowledgeFinding / revokeAcknowledgment
    // =========================================================================

    @Test
    fun `acknowledgeFinding calls api with reason`() = runTest {
        val findingId = UUID.randomUUID()
        coEvery { mockSecurityApi.acknowledgeFinding(findingId, any()) } returns
            Response.success(finding())

        val vm = CveTrackingViewModel(mockApiClient)
        vm.acknowledgeFinding(findingId, "accepted risk")

        coVerify { mockSecurityApi.acknowledgeFinding(findingId, any()) }
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `revokeAcknowledgment calls api`() = runTest {
        val findingId = UUID.randomUUID()
        coEvery { mockSecurityApi.revokeAcknowledgment(findingId) } returns
            Response.success(finding())

        val vm = CveTrackingViewModel(mockApiClient)
        vm.revokeAcknowledgment(findingId)

        coVerify { mockSecurityApi.revokeAcknowledgment(findingId) }
    }

    @Test
    fun `cveSeverityRank orders critical before low`() {
        assertTrue(
            CveTrackingViewModel.cveSeverityRank("critical") <
                CveTrackingViewModel.cveSeverityRank("low"),
        )
        assertTrue(
            CveTrackingViewModel.cveSeverityRank(null) >
                CveTrackingViewModel.cveSeverityRank("high"),
        )
    }
}

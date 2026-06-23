package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.security.ArtifactSecurityUiState
import com.artifactkeeper.android.ui.screens.security.ArtifactSecurityViewModel
import com.artifactkeeper.client.apis.SbomApi
import com.artifactkeeper.client.apis.SecurityApi
import com.artifactkeeper.client.models.ComponentResponse
import com.artifactkeeper.client.models.FindingListResponse
import com.artifactkeeper.client.models.FindingResponse
import com.artifactkeeper.client.models.ScanListResponse
import com.artifactkeeper.client.models.ScanResponse
import com.artifactkeeper.client.models.SbomContentResponse
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
class ArtifactSecurityViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockSecurityApi = mockk<SecurityApi>()
    private val mockSbomApi = mockk<SbomApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { securityApi } returns mockSecurityApi
        every { sbomApi } returns mockSbomApi
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

    private fun scan(
        id: UUID = UUID.randomUUID(),
        status: String = "completed",
        critical: Int = 1,
        high: Int = 2,
    ) = ScanResponse(
        artifactId = artifactId,
        createdAt = now,
        criticalCount = critical,
        findingsCount = critical + high,
        highCount = high,
        id = id,
        infoCount = 0,
        isReused = false,
        lowCount = 0,
        mediumCount = 0,
        repositoryId = repoId,
        scanType = "vulnerability",
        status = status,
    )

    private fun sbomContent() = SbomContentResponse(
        artifactId = artifactId,
        componentCount = 3,
        contentHash = "abc123",
        createdAt = now,
        dependencyCount = 5,
        format = "cyclonedx",
        formatVersion = "1.5",
        generatedAt = now,
        id = UUID.randomUUID(),
        licenseCount = 2,
        licenses = listOf("MIT", "Apache-2.0"),
        repositoryId = repoId,
        content = emptyMap<String, Any>(),
    )

    private fun component(name: String) = ComponentResponse(
        id = UUID.randomUUID(),
        licenses = listOf("MIT"),
        name = name,
        sbomId = UUID.randomUUID(),
    )

    private fun finding(severity: String) = FindingResponse(
        artifactId = artifactId,
        createdAt = now,
        id = UUID.randomUUID(),
        isAcknowledged = false,
        scanResultId = UUID.randomUUID(),
        severity = severity,
        title = "Vuln in $severity",
    )

    // =========================================================================
    // ArtifactSecurityUiState data class
    // =========================================================================

    @Test
    fun `initial state is empty with no loading flags`() {
        val state = ArtifactSecurityUiState()
        assertTrue(state.scans.isEmpty())
        assertNull(state.sbom)
        assertTrue(state.components.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `ui state copy preserves unmodified fields`() {
        val original = ArtifactSecurityUiState(isLoading = true, error = "boom")
        val copied = original.copy(isLoading = false)
        assertFalse(copied.isLoading)
        assertEquals("boom", copied.error)
    }

    // =========================================================================
    // loadArtifactSecurity: scans + sbom
    // =========================================================================

    @Test
    fun `loadArtifactSecurity populates scans and sbom on success`() = runTest {
        val scans = listOf(scan(critical = 3, high = 1), scan(critical = 0, high = 0))
        coEvery { mockSecurityApi.listArtifactScans(artifactId) } returns
            Response.success(ScanListResponse(items = scans, total = 2))
        coEvery { mockSbomApi.getSbomByArtifact(artifactId) } returns
            Response.success(sbomContent())
        coEvery { mockSbomApi.getSbomComponents(any()) } returns
            Response.success(listOf(component("libfoo"), component("libbar")))

        val vm = ArtifactSecurityViewModel(mockApiClient)
        vm.loadArtifactSecurity(artifactId)

        val state = vm.uiState.value
        assertEquals(2, state.scans.size)
        assertNotNull(state.sbom)
        assertEquals(2, state.components.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadArtifactSecurity tolerates missing sbom and still shows scans`() = runTest {
        coEvery { mockSecurityApi.listArtifactScans(artifactId) } returns
            Response.success(ScanListResponse(items = listOf(scan()), total = 1))
        // No SBOM generated for this artifact yet -> 404
        coEvery { mockSbomApi.getSbomByArtifact(artifactId) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "not found"))

        val vm = ArtifactSecurityViewModel(mockApiClient)
        vm.loadArtifactSecurity(artifactId)

        val state = vm.uiState.value
        assertEquals(1, state.scans.size)
        assertNull(state.sbom)
        assertTrue(state.components.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `loadArtifactSecurity sets error when scan listing fails`() = runTest {
        coEvery { mockSecurityApi.listArtifactScans(artifactId) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "server error"))

        val vm = ArtifactSecurityViewModel(mockApiClient)
        vm.loadArtifactSecurity(artifactId)

        val state = vm.uiState.value
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }

    // =========================================================================
    // loadScanDetail: get_scan + list_findings
    // =========================================================================

    @Test
    fun `loadScanDetail populates scan and findings`() = runTest {
        val scanId = UUID.randomUUID()
        coEvery { mockSecurityApi.getScan(scanId) } returns Response.success(scan(id = scanId))
        coEvery { mockSecurityApi.listFindings(scanId) } returns Response.success(
            FindingListResponse(
                items = listOf(finding("critical"), finding("high"), finding("low")),
                total = 3,
            ),
        )

        val vm = ArtifactSecurityViewModel(mockApiClient)
        vm.loadScanDetail(scanId)

        val state = vm.scanDetailState.value
        assertEquals(scanId, state.scan?.id)
        assertEquals(3, state.findings.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadScanDetail sets error when get scan fails`() = runTest {
        val scanId = UUID.randomUUID()
        coEvery { mockSecurityApi.getScan(scanId) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "missing"))

        val vm = ArtifactSecurityViewModel(mockApiClient)
        vm.loadScanDetail(scanId)

        val state = vm.scanDetailState.value
        assertNotNull(state.error)
        assertNull(state.scan)
    }

    // =========================================================================
    // findings are sorted by severity for display
    // =========================================================================

    @Test
    fun `severity rank orders critical before low`() {
        assertTrue(
            ArtifactSecurityViewModel.severityRank("critical") <
                ArtifactSecurityViewModel.severityRank("low"),
        )
        assertTrue(
            ArtifactSecurityViewModel.severityRank("high") <
                ArtifactSecurityViewModel.severityRank("medium"),
        )
    }

    @Test
    fun `loadScanDetail sorts findings by descending severity`() = runTest {
        val scanId = UUID.randomUUID()
        coEvery { mockSecurityApi.getScan(scanId) } returns Response.success(scan(id = scanId))
        coEvery { mockSecurityApi.listFindings(scanId) } returns Response.success(
            FindingListResponse(
                items = listOf(finding("low"), finding("critical"), finding("medium")),
                total = 3,
            ),
        )

        val vm = ArtifactSecurityViewModel(mockApiClient)
        vm.loadScanDetail(scanId)

        val severities = vm.scanDetailState.value.findings.map { it.severity }
        assertEquals(listOf("critical", "medium", "low"), severities)
    }
}

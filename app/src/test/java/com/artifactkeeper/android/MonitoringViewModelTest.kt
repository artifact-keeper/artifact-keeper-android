package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.operations.MonitoringUiState
import com.artifactkeeper.android.ui.screens.operations.MonitoringViewModel
import com.artifactkeeper.client.apis.MonitoringApi
import com.artifactkeeper.client.apis.SecurityApi
import com.artifactkeeper.client.models.AlertState
import com.artifactkeeper.client.models.DtStatusResponse
import com.artifactkeeper.client.models.ServiceHealthEntry
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class MonitoringViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mockSecurityApi: SecurityApi
    private lateinit var mockMonitoringApi: MonitoringApi
    private lateinit var mockOkHttpClient: OkHttpClient
    private lateinit var mockApiClient: ApiClient

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockSecurityApi = mockk()
        mockMonitoringApi = mockk()
        mockOkHttpClient = mockk()
        mockApiClient = mockk {
            every { securityApi } returns mockSecurityApi
            every { monitoringApi } returns mockMonitoringApi
            every { baseUrl } returns "https://example.com/"
            every { httpClient } returns mockOkHttpClient
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Build a fake OkHttp response returning the given JSON body. */
    private fun fakeOkHttpResponse(requestUrl: String, body: String): Response {
        return Response.Builder()
            .request(Request.Builder().url(requestUrl).build())
            .protocol(Protocol.HTTP_2)
            .code(200)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    /** Stub the OkHttpClient to return a fixed health JSON response. Creates a new response on each call. */
    private fun stubHealthEndpoint(healthJson: String) {
        val mockCall = mockk<Call>(relaxed = true) {
            every { execute() } answers {
                fakeOkHttpResponse("https://example.com/health", healthJson)
            }
        }
        every { mockOkHttpClient.newCall(any()) } returns mockCall
    }

    // =========================================================================
    // MonitoringUiState data class
    // =========================================================================

    @Test
    fun `MonitoringUiState default values`() {
        val state = MonitoringUiState()
        assertNull(state.health)
        assertNull(state.dtStatus)
        assertTrue(state.alerts.isEmpty())
        assertTrue(state.healthLog.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    @Test
    fun `MonitoringUiState copy preserves unmodified fields`() {
        val original = MonitoringUiState(
            isLoading = true,
            error = "timeout",
        )
        val copied = original.copy(isLoading = false)
        assertFalse(copied.isLoading)
        assertEquals("timeout", copied.error)
    }

    @Test
    fun `MonitoringUiState equality`() {
        assertEquals(MonitoringUiState(), MonitoringUiState())
    }

    @Test
    fun `MonitoringUiState with all fields populated`() {
        val state = MonitoringUiState(
            health = null,
            dtStatus = null,
            alerts = emptyList(),
            healthLog = emptyList(),
            isLoading = true,
            isRefreshing = true,
            error = "error",
        )
        assertTrue(state.isLoading)
        assertTrue(state.isRefreshing)
        assertEquals("error", state.error)
    }

    @Test
    fun `MonitoringUiState copy for refresh clears error and sets isRefreshing`() {
        val state = MonitoringUiState(error = "old error", isLoading = false)
        val refreshing = state.copy(isRefreshing = true, error = null)
        assertTrue(refreshing.isRefreshing)
        assertNull(refreshing.error)
    }

    @Test
    fun `MonitoringUiState copy for initial load sets isLoading`() {
        val state = MonitoringUiState()
        val loading = state.copy(isLoading = true, error = null)
        assertTrue(loading.isLoading)
        assertNull(loading.error)
    }

    @Test
    fun `MonitoringUiState can represent completed load with health data`() {
        val state = MonitoringUiState(
            isLoading = false,
            isRefreshing = false,
            error = null,
        )
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    @Test
    fun `MonitoringUiState can represent error state`() {
        val state = MonitoringUiState(
            isLoading = false,
            isRefreshing = false,
            error = "Failed to load monitoring data",
        )
        assertFalse(state.isLoading)
        assertEquals("Failed to load monitoring data", state.error)
    }

    // =========================================================================
    // loadData success - full happy path
    // =========================================================================

    @Test
    fun `loadData populates all fields on success`() = runTest {
        val healthJson = """{"status":"ok","checks":{"database":{"status":"ok","response_time_ms":5}}}"""
        stubHealthEndpoint(healthJson)

        val dtStatus = DtStatusResponse(
            enabled = true,
            healthy = true,
            url = "https://dt.example.com",
        )
        coEvery { mockSecurityApi.dtStatus() } returns retrofit2.Response.success(dtStatus)

        val alert = AlertState(
            consecutiveFailures = 0,
            currentStatus = "ok",
            serviceName = "database",
            updatedAt = OffsetDateTime.now(),
        )
        coEvery { mockMonitoringApi.getAlertStates() } returns retrofit2.Response.success(listOf(alert))

        val healthEntry = ServiceHealthEntry(
            serviceName = "database",
            status = "ok",
            checkedAt = OffsetDateTime.now(),
        )
        coEvery { mockMonitoringApi.getHealthLog(any(), any()) } returns retrofit2.Response.success(listOf(healthEntry))

        val viewModel = MonitoringViewModel(mockApiClient, testDispatcher)

        val state = viewModel.uiState.value
        assertNotNull(state.health)
        assertEquals("ok", state.health?.status)
        assertNotNull(state.dtStatus)
        assertTrue(state.dtStatus!!.enabled)
        assertEquals(1, state.alerts.size)
        assertEquals("database", state.alerts[0].serviceName)
        assertEquals(1, state.healthLog.size)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    // =========================================================================
    // loadData with optional DT failure (graceful degradation)
    // =========================================================================

    @Test
    fun `loadData succeeds when DT status throws`() = runTest {
        val healthJson = """{"status":"ok","checks":{}}"""
        stubHealthEndpoint(healthJson)

        coEvery { mockSecurityApi.dtStatus() } throws RuntimeException("DT unavailable")
        coEvery { mockMonitoringApi.getAlertStates() } returns retrofit2.Response.success(emptyList())
        coEvery { mockMonitoringApi.getHealthLog(any(), any()) } returns retrofit2.Response.success(emptyList())

        val viewModel = MonitoringViewModel(mockApiClient, testDispatcher)

        val state = viewModel.uiState.value
        assertNotNull(state.health)
        assertNull(state.dtStatus)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    // =========================================================================
    // loadData failure (primary call throws)
    // =========================================================================

    @Test
    fun `loadData sets error when health fetch fails`() = runTest {
        val mockCall = mockk<Call>(relaxed = true) {
            every { execute() } throws RuntimeException("Connection refused")
        }
        every { mockOkHttpClient.newCall(any()) } returns mockCall

        val viewModel = MonitoringViewModel(mockApiClient, testDispatcher)

        val state = viewModel.uiState.value
        assertEquals("Connection refused", state.error)
        assertNull(state.health)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `loadData sets error when alerts API fails`() = runTest {
        val healthJson = """{"status":"ok","checks":{}}"""
        stubHealthEndpoint(healthJson)

        coEvery { mockSecurityApi.dtStatus() } throws RuntimeException("N/A")
        coEvery { mockMonitoringApi.getAlertStates() } throws RuntimeException("Unauthorized")

        val viewModel = MonitoringViewModel(mockApiClient, testDispatcher)

        val state = viewModel.uiState.value
        assertEquals("Unauthorized", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadData uses default message when exception has no message`() = runTest {
        val mockCall = mockk<Call>(relaxed = true) {
            every { execute() } throws RuntimeException()
        }
        every { mockOkHttpClient.newCall(any()) } returns mockCall

        val viewModel = MonitoringViewModel(mockApiClient, testDispatcher)

        assertEquals("Failed to load monitoring data", viewModel.uiState.value.error)
    }

    // =========================================================================
    // loadData with refresh=true
    // =========================================================================

    @Test
    fun `loadData with refresh reloads data successfully`() = runTest {
        val healthJson = """{"status":"ok","checks":{}}"""
        stubHealthEndpoint(healthJson)

        coEvery { mockSecurityApi.dtStatus() } throws RuntimeException("N/A")
        coEvery { mockMonitoringApi.getAlertStates() } returns retrofit2.Response.success(emptyList())
        coEvery { mockMonitoringApi.getHealthLog(any(), any()) } returns retrofit2.Response.success(emptyList())

        val viewModel = MonitoringViewModel(mockApiClient, testDispatcher)

        viewModel.loadData(refresh = true)

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    @Test
    fun `loadData with refresh sets error on failure`() = runTest {
        // First init succeeds
        val healthJson = """{"status":"ok","checks":{}}"""
        stubHealthEndpoint(healthJson)
        coEvery { mockSecurityApi.dtStatus() } throws RuntimeException("N/A")
        coEvery { mockMonitoringApi.getAlertStates() } returns retrofit2.Response.success(emptyList())
        coEvery { mockMonitoringApi.getHealthLog(any(), any()) } returns retrofit2.Response.success(emptyList())

        val viewModel = MonitoringViewModel(mockApiClient, testDispatcher)

        // Now make refresh fail
        val failCall = mockk<Call>(relaxed = true) {
            every { execute() } throws RuntimeException("Network error")
        }
        every { mockOkHttpClient.newCall(any()) } returns failCall

        viewModel.loadData(refresh = true)

        val state = viewModel.uiState.value
        assertEquals("Network error", state.error)
        assertFalse(state.isRefreshing)
    }

    // =========================================================================
    // loadData health JSON parsing
    // =========================================================================

    @Test
    fun `loadData parses health checks map correctly`() = runTest {
        val healthJson = """{"status":"degraded","checks":{"database":{"status":"ok","response_time_ms":12},"meilisearch":{"status":"unhealthy","response_time_ms":500}}}"""
        stubHealthEndpoint(healthJson)

        coEvery { mockSecurityApi.dtStatus() } throws RuntimeException("N/A")
        coEvery { mockMonitoringApi.getAlertStates() } returns retrofit2.Response.success(emptyList())
        coEvery { mockMonitoringApi.getHealthLog(any(), any()) } returns retrofit2.Response.success(emptyList())

        val viewModel = MonitoringViewModel(mockApiClient, testDispatcher)

        val health = viewModel.uiState.value.health
        assertNotNull(health)
        assertEquals("degraded", health!!.status)
        assertEquals(2, health.checks.size)
        assertEquals("ok", health.checks["database"]?.status)
        assertEquals("unhealthy", health.checks["meilisearch"]?.status)
    }

    @Test
    fun `loadData handles minimal valid health body`() = runTest {
        stubHealthEndpoint("""{"status":"ok"}""")
        coEvery { mockSecurityApi.dtStatus() } throws RuntimeException("N/A")
        coEvery { mockMonitoringApi.getAlertStates() } returns retrofit2.Response.success(emptyList())
        coEvery { mockMonitoringApi.getHealthLog(any(), any()) } returns retrofit2.Response.success(emptyList())

        val viewModel = MonitoringViewModel(mockApiClient, testDispatcher)

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertFalse(state.isLoading)
        assertNotNull(state.health)
        assertEquals("ok", state.health!!.status)
        assertTrue(state.health!!.checks.isEmpty())
    }

    @Test
    fun `loadData reports error for invalid health JSON`() = runTest {
        stubHealthEndpoint("{}")
        coEvery { mockSecurityApi.dtStatus() } throws RuntimeException("N/A")

        val viewModel = MonitoringViewModel(mockApiClient, testDispatcher)

        val state = viewModel.uiState.value
        // Empty JSON {} is missing required "status" field, so deserialization fails
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }
}

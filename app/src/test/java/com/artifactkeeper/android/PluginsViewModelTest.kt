package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.integration.PluginDetailUiState
import com.artifactkeeper.android.ui.screens.integration.PluginsUiState
import com.artifactkeeper.android.ui.screens.integration.PluginsViewModel
import com.artifactkeeper.client.apis.PluginsApi
import com.artifactkeeper.client.models.InstallFromGitRequest
import com.artifactkeeper.client.models.PluginInstallResponse
import com.artifactkeeper.client.models.PluginListResponse
import com.artifactkeeper.client.models.PluginResponse
import com.artifactkeeper.client.models.WasmPluginResponse
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
class PluginsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockPluginsApi = mockk<PluginsApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { pluginsApi } returns mockPluginsApi
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

    private fun plugin(name: String, status: String = "enabled", id: UUID = UUID.randomUUID()) = PluginResponse(
        configSchema = emptyMap<String, Any>(),
        displayName = name,
        id = id,
        installedAt = now,
        name = name.lowercase(),
        pluginType = "format",
        status = status,
        version = "1.0.0",
    )

    @Test
    fun `initial list state is empty`() {
        val state = PluginsUiState()
        assertTrue(state.plugins.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNull(state.message)
    }

    @Test
    fun `initial detail state is empty`() {
        val state = PluginDetailUiState()
        assertNull(state.plugin)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadPlugins populates list sorted by name`() = runTest {
        coEvery { mockPluginsApi.listPlugins(null, null) } returns Response.success(
            PluginListResponse(items = listOf(plugin("Zebra"), plugin("Alpha"))),
        )

        val vm = PluginsViewModel(mockApiClient)
        vm.loadPlugins()

        val names = vm.uiState.value.plugins.map { it.displayName }
        assertEquals(listOf("Alpha", "Zebra"), names)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `loadPlugins sets error on failure`() = runTest {
        coEvery { mockPluginsApi.listPlugins(null, null) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))

        val vm = PluginsViewModel(mockApiClient)
        vm.loadPlugins()

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `loadPluginDetail calls getPlugin and populates detail`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockPluginsApi.getPlugin(id) } returns Response.success(plugin("Maven", id = id))

        val vm = PluginsViewModel(mockApiClient)
        vm.loadPluginDetail(id)

        coVerify { mockPluginsApi.getPlugin(id) }
        assertEquals(id, vm.detailState.value.plugin?.id)
        assertFalse(vm.detailState.value.isLoading)
    }

    @Test
    fun `loadPluginDetail sets error on failure`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockPluginsApi.getPlugin(id) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "missing"))

        val vm = PluginsViewModel(mockApiClient)
        vm.loadPluginDetail(id)

        assertNotNull(vm.detailState.value.error)
        assertNull(vm.detailState.value.plugin)
    }

    @Test
    fun `enablePlugin calls api and reloads`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockPluginsApi.enablePlugin(id) } returns Response.success(Unit)
        coEvery { mockPluginsApi.listPlugins(null, null) } returns Response.success(
            PluginListResponse(items = listOf(plugin("Maven", status = "enabled", id = id))),
        )

        val vm = PluginsViewModel(mockApiClient)
        vm.enablePlugin(id)

        coVerify { mockPluginsApi.enablePlugin(id) }
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `disablePlugin calls api`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockPluginsApi.disablePlugin(id) } returns Response.success(Unit)
        coEvery { mockPluginsApi.listPlugins(null, null) } returns Response.success(
            PluginListResponse(items = emptyList()),
        )

        val vm = PluginsViewModel(mockApiClient)
        vm.disablePlugin(id)

        coVerify { mockPluginsApi.disablePlugin(id) }
    }

    @Test
    fun `reloadPlugin calls api`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockPluginsApi.reloadPlugin(id) } returns Response.success(
            WasmPluginResponse(
                capabilities = emptyMap<String, Any>(),
                displayName = "Maven",
                id = id,
                installedAt = now,
                name = "maven",
                pluginType = "format",
                resourceLimits = emptyMap<String, Any>(),
                sourceType = "git",
                status = "enabled",
                updatedAt = now,
                version = "1.0.0",
            ),
        )
        coEvery { mockPluginsApi.listPlugins(null, null) } returns Response.success(
            PluginListResponse(items = emptyList()),
        )

        val vm = PluginsViewModel(mockApiClient)
        vm.reloadPlugin(id)

        coVerify { mockPluginsApi.reloadPlugin(id) }
    }

    @Test
    fun `uninstallPlugin calls api and reloads`() = runTest {
        val id = UUID.randomUUID()
        coEvery { mockPluginsApi.uninstallPlugin(id) } returns Response.success(Unit)
        coEvery { mockPluginsApi.listPlugins(null, null) } returns Response.success(
            PluginListResponse(items = emptyList()),
        )

        val vm = PluginsViewModel(mockApiClient)
        vm.uninstallPlugin(id)

        coVerify { mockPluginsApi.uninstallPlugin(id) }
        assertTrue(vm.uiState.value.plugins.isEmpty())
    }

    @Test
    fun `installFromGit posts url and ref then reloads`() = runTest {
        coEvery { mockPluginsApi.installFromGit(any()) } returns Response.success(
            PluginInstallResponse(
                formatKey = "unity",
                message = "installed",
                name = "unity",
                pluginId = UUID.randomUUID(),
                version = "1.0.0",
            ),
        )
        coEvery { mockPluginsApi.listPlugins(null, null) } returns Response.success(
            PluginListResponse(items = listOf(plugin("Unity"))),
        )

        val vm = PluginsViewModel(mockApiClient)
        vm.installFromGit("https://github.com/x/y", "main")

        coVerify {
            mockPluginsApi.installFromGit(
                InstallFromGitRequest(url = "https://github.com/x/y", ref = "main"),
            )
        }
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `installFromGit sets error on failure`() = runTest {
        coEvery { mockPluginsApi.installFromGit(any()) } returns
            Response.error(400, okhttp3.ResponseBody.create(null, "bad url"))

        val vm = PluginsViewModel(mockApiClient)
        vm.installFromGit("not-a-url", null)

        assertNotNull(vm.uiState.value.error)
    }
}

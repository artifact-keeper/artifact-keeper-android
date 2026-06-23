package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.artifacts.RepositoryBrowseUiState
import com.artifactkeeper.android.ui.screens.artifacts.RepositoryBrowseViewModel
import com.artifactkeeper.client.apis.RepositoriesApi
import com.artifactkeeper.client.models.ArtifactResponse
import com.artifactkeeper.client.models.TreeNodeResponse
import com.artifactkeeper.client.models.TreeResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.OffsetDateTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryBrowseViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockReposApi = mockk<RepositoriesApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { reposApi } returns mockReposApi
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() {
        val vm = RepositoryBrowseViewModel(mockApiClient)
        assertEquals(RepositoryBrowseUiState(), vm.uiState.value)
        assertTrue(vm.uiState.value.nodes.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `load populates nodes for the repository root`() {
        coEvery { mockReposApi.getTree("maven-local", null, true) } returns
            Response.success(tree(folder("com"), file("readme.txt", "readme.txt")))
        val vm = RepositoryBrowseViewModel(mockApiClient)

        vm.load("maven-local")

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("maven-local", state.repoKey)
        assertEquals("", state.currentPath)
        assertEquals(2, state.nodes.size)
    }

    @Test
    fun `load sorts folders before files`() {
        coEvery { mockReposApi.getTree(any(), any(), any()) } returns
            Response.success(tree(file("a.txt", "a.txt"), folder("zeta"), file("b.txt", "b.txt")))
        val vm = RepositoryBrowseViewModel(mockApiClient)

        vm.load("maven-local")

        val nodes = vm.uiState.value.nodes
        assertEquals("zeta", nodes.first().name)
        assertEquals("folder", nodes.first().type)
    }

    @Test
    fun `openFolder browses into the folder path`() {
        coEvery { mockReposApi.getTree("maven-local", null, true) } returns
            Response.success(tree(folder("com")))
        coEvery { mockReposApi.getTree("maven-local", "com", true) } returns
            Response.success(tree(folder("com/example")))
        val vm = RepositoryBrowseViewModel(mockApiClient)
        vm.load("maven-local")

        vm.openFolder(folder("com"))

        val state = vm.uiState.value
        assertEquals("com", state.currentPath)
        assertEquals(1, state.nodes.size)
        assertEquals("com/example", state.nodes.first().path)
    }

    @Test
    fun `breadcrumbs reflect the current path`() {
        coEvery { mockReposApi.getTree(any(), any(), any()) } returns Response.success(tree())
        val vm = RepositoryBrowseViewModel(mockApiClient)
        vm.load("maven-local")

        vm.openFolder(folder("com", path = "com"))
        vm.openFolder(folder("example", path = "com/example"))

        val crumbs = vm.uiState.value.breadcrumbs
        assertEquals(listOf("com", "example"), crumbs.map { it.name })
        assertEquals(listOf("com", "com/example"), crumbs.map { it.path })
    }

    @Test
    fun `navigateToCrumb truncates the path and reloads`() {
        coEvery { mockReposApi.getTree(any(), any(), any()) } returns Response.success(tree())
        val vm = RepositoryBrowseViewModel(mockApiClient)
        vm.load("maven-local")
        vm.openFolder(folder("com", path = "com"))
        vm.openFolder(folder("example", path = "com/example"))

        vm.navigateToCrumb("com")

        assertEquals("com", vm.uiState.value.currentPath)
        assertEquals(listOf("com"), vm.uiState.value.breadcrumbs.map { it.path })
    }

    @Test
    fun `navigateToRoot clears the path`() {
        coEvery { mockReposApi.getTree(any(), any(), any()) } returns Response.success(tree())
        val vm = RepositoryBrowseViewModel(mockApiClient)
        vm.load("maven-local")
        vm.openFolder(folder("com", path = "com"))

        vm.navigateToRoot()

        assertEquals("", vm.uiState.value.currentPath)
        assertTrue(vm.uiState.value.breadcrumbs.isEmpty())
    }

    @Test
    fun `openFile fetches artifact metadata`() {
        coEvery { mockReposApi.getTree(any(), any(), any()) } returns Response.success(tree())
        coEvery { mockReposApi.getRepositoryArtifactMetadata("maven-local", "com/app.jar") } returns
            Response.success(artifact("app.jar", "com/app.jar"))
        val vm = RepositoryBrowseViewModel(mockApiClient)
        vm.load("maven-local")

        vm.openFile(file("app.jar", "com/app.jar"))

        assertEquals("app.jar", vm.uiState.value.selectedFile?.name)
        coVerify { mockReposApi.getRepositoryArtifactMetadata("maven-local", "com/app.jar") }
    }

    @Test
    fun `dismissFile clears the selected file`() {
        coEvery { mockReposApi.getTree(any(), any(), any()) } returns Response.success(tree())
        coEvery { mockReposApi.getRepositoryArtifactMetadata(any(), any()) } returns
            Response.success(artifact("app.jar", "com/app.jar"))
        val vm = RepositoryBrowseViewModel(mockApiClient)
        vm.load("maven-local")
        vm.openFile(file("app.jar", "com/app.jar"))

        vm.dismissFile()

        assertNull(vm.uiState.value.selectedFile)
    }

    @Test
    fun `deleteArtifact deletes and reloads the current folder`() {
        coEvery { mockReposApi.getTree("maven-local", null, true) } returnsMany listOf(
            Response.success(tree(file("old.jar", "old.jar"))),
            Response.success(tree()),
        )
        coEvery { mockReposApi.deleteArtifact("maven-local", "old.jar") } returns Response.success(Unit)
        val vm = RepositoryBrowseViewModel(mockApiClient)
        vm.load("maven-local")

        vm.deleteArtifact("old.jar")

        coVerify { mockReposApi.deleteArtifact("maven-local", "old.jar") }
        assertTrue(vm.uiState.value.nodes.isEmpty())
    }

    @Test
    fun `deleteArtifact surfaces an error and keeps nodes`() {
        coEvery { mockReposApi.getTree("maven-local", null, true) } returns
            Response.success(tree(file("old.jar", "old.jar")))
        coEvery { mockReposApi.deleteArtifact("maven-local", "old.jar") } returns Response.error(500, errorBody())
        val vm = RepositoryBrowseViewModel(mockApiClient)
        vm.load("maven-local")

        vm.deleteArtifact("old.jar")

        assertEquals(true, vm.uiState.value.actionError?.isNotBlank())
        assertEquals(1, vm.uiState.value.nodes.size)
    }

    @Test
    fun `load sets error when the tree call fails`() {
        coEvery { mockReposApi.getTree(any(), any(), any()) } returns Response.error(404, errorBody())
        val vm = RepositoryBrowseViewModel(mockApiClient)

        vm.load("missing")

        assertEquals(true, vm.uiState.value.error?.isNotBlank())
        assertTrue(vm.uiState.value.nodes.isEmpty())
    }

    // Helpers

    private fun tree(vararg nodes: TreeNodeResponse) = TreeResponse(nodes = nodes.toList())

    private fun folder(name: String, path: String = name) = TreeNodeResponse(
        hasChildren = true,
        id = path,
        name = name,
        path = path,
        type = "folder",
        childrenCount = 1,
    )

    private fun file(name: String, path: String) = TreeNodeResponse(
        hasChildren = false,
        id = path,
        name = name,
        path = path,
        type = "file",
        sizeBytes = 1024,
    )

    private fun artifact(name: String, path: String) = ArtifactResponse(
        checksumSha256 = "abc",
        contentType = "application/octet-stream",
        createdAt = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        downloadCount = 0,
        id = UUID.randomUUID(),
        name = name,
        path = path,
        repositoryKey = "maven-local",
        sizeBytes = 1024,
    )

    private fun errorBody() = "{\"error\":\"boom\"}".toResponseBody("application/json".toMediaType())
}

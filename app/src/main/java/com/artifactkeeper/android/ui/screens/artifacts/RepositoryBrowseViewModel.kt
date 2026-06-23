package com.artifactkeeper.android.ui.screens.artifacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.ArtifactResponse
import com.artifactkeeper.client.models.TreeNodeResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A single segment of the browse path, used to render breadcrumbs. */
data class BrowseCrumb(
    val name: String,
    val path: String,
)

data class RepositoryBrowseUiState(
    val repoKey: String = "",
    val currentPath: String = "",
    val nodes: List<TreeNodeResponse> = emptyList(),
    val breadcrumbs: List<BrowseCrumb> = emptyList(),
    val selectedFile: ArtifactResponse? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isActionInProgress: Boolean = false,
    val error: String? = null,
    val actionError: String? = null,
)

@HiltViewModel
class RepositoryBrowseViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepositoryBrowseUiState())
    val uiState: StateFlow<RepositoryBrowseUiState> = _uiState.asStateFlow()

    fun load(repoKey: String, path: String = "", refresh: Boolean = false) {
        _uiState.update {
            it.copy(
                repoKey = repoKey,
                currentPath = path,
                breadcrumbs = crumbsFor(path),
            )
        }
        fetchTree(refresh)
    }

    fun openFolder(node: TreeNodeResponse) {
        if (node.type != "folder") return
        load(_uiState.value.repoKey, node.path)
    }

    fun navigateToCrumb(path: String) {
        load(_uiState.value.repoKey, path)
    }

    fun navigateToRoot() {
        load(_uiState.value.repoKey, "")
    }

    fun openFile(node: TreeNodeResponse) {
        if (node.type == "folder") return
        val repoKey = _uiState.value.repoKey
        viewModelScope.launch {
            try {
                val metadata = apiClient.reposApi
                    .getRepositoryArtifactMetadata(repoKey, node.path)
                    .unwrap()
                _uiState.update { it.copy(selectedFile = metadata, actionError = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(actionError = e.message ?: "Failed to load artifact") }
            }
        }
    }

    fun dismissFile() {
        _uiState.update { it.copy(selectedFile = null) }
    }

    fun deleteArtifact(path: String) {
        val repoKey = _uiState.value.repoKey
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, actionError = null) }
            try {
                apiClient.reposApi.deleteArtifact(repoKey, path).unwrap()
                _uiState.update { it.copy(isActionInProgress = false, selectedFile = null) }
                fetchTree(refresh = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionInProgress = false, actionError = e.message ?: "Failed to delete artifact")
                }
            }
        }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    private fun fetchTree(refresh: Boolean) {
        val state = _uiState.value
        val pathArg = state.currentPath.ifBlank { null }
        viewModelScope.launch {
            _uiState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val response = apiClient.reposApi
                    .getTree(state.repoKey, pathArg, true)
                    .unwrap()
                _uiState.update {
                    it.copy(
                        nodes = response.nodes.sortedWith(nodeOrder),
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        nodes = emptyList(),
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to load contents",
                    )
                }
            }
        }
    }

    private fun crumbsFor(path: String): List<BrowseCrumb> {
        if (path.isBlank()) return emptyList()
        val segments = path.split("/").filter { it.isNotBlank() }
        val crumbs = mutableListOf<BrowseCrumb>()
        val builder = StringBuilder()
        for (segment in segments) {
            if (builder.isNotEmpty()) builder.append("/")
            builder.append(segment)
            crumbs.add(BrowseCrumb(name = segment, path = builder.toString()))
        }
        return crumbs
    }

    private companion object {
        // Folders first, then files, each alphabetical by name (case-insensitive).
        val nodeOrder: Comparator<TreeNodeResponse> =
            compareByDescending<TreeNodeResponse> { it.type == "folder" }
                .thenBy { it.name.lowercase() }
    }
}

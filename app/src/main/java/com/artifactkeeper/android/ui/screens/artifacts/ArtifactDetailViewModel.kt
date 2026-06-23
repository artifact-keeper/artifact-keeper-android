package com.artifactkeeper.android.ui.screens.artifacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.AddArtifactLabelRequest
import com.artifactkeeper.client.models.ArtifactLabelResponse
import com.artifactkeeper.client.models.ArtifactMetadataResponse
import com.artifactkeeper.client.models.ArtifactResponse
import com.artifactkeeper.client.models.ArtifactStatsResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ArtifactDetailUiState(
    val artifact: ArtifactResponse? = null,
    val metadata: ArtifactMetadataResponse? = null,
    val stats: ArtifactStatsResponse? = null,
    val labels: List<ArtifactLabelResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLabelMutating: Boolean = false,
    val error: String? = null,
    val labelError: String? = null,
)

@HiltViewModel
class ArtifactDetailViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtifactDetailUiState())
    val uiState: StateFlow<ArtifactDetailUiState> = _uiState.asStateFlow()

    private var artifactId: UUID? = null

    fun load(id: String, refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            val uuid = id.toUuidOrNull()
            if (uuid == null) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = "Invalid artifact id")
                }
                return@launch
            }
            artifactId = uuid
            try {
                val artifact = apiClient.artifactsApi.getArtifact(uuid).unwrap()

                // Metadata and stats are best-effort; an artifact can exist without either.
                val metadata = runCatchingApi { apiClient.artifactsApi.getArtifactMetadata(uuid).unwrap() }
                val stats = runCatchingApi { apiClient.artifactsApi.getArtifactStats(uuid).unwrap() }
                val labels = runCatchingApi { apiClient.artifactLabelsApi.listArtifactLabels(uuid).unwrap()?.items }
                    ?: emptyList()

                _uiState.update {
                    it.copy(
                        artifact = artifact,
                        metadata = metadata,
                        stats = stats,
                        labels = labels,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to load artifact",
                    )
                }
            }
        }
    }

    fun addLabel(key: String, value: String?) {
        val uuid = artifactId ?: return
        if (key.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLabelMutating = true, labelError = null) }
            try {
                apiClient.artifactLabelsApi
                    .addArtifactLabel(uuid, key, AddArtifactLabelRequest(value = value))
                    .unwrap()
                refreshLabels(uuid)
                _uiState.update { it.copy(isLabelMutating = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLabelMutating = false, labelError = e.message ?: "Failed to add label")
                }
            }
        }
    }

    fun deleteLabel(key: String) {
        val uuid = artifactId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLabelMutating = true, labelError = null) }
            try {
                apiClient.artifactLabelsApi.deleteArtifactLabel(uuid, key).unwrap()
                refreshLabels(uuid)
                _uiState.update { it.copy(isLabelMutating = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLabelMutating = false, labelError = e.message ?: "Failed to delete label")
                }
            }
        }
    }

    fun clearLabelError() {
        _uiState.update { it.copy(labelError = null) }
    }

    private suspend fun refreshLabels(uuid: UUID) {
        val labels = apiClient.artifactLabelsApi.listArtifactLabels(uuid).unwrap().items
        _uiState.update { it.copy(labels = labels) }
    }

    private inline fun <T> runCatchingApi(block: () -> T?): T? =
        try {
            block()
        } catch (_: Exception) {
            null
        }

    private fun String.toUuidOrNull(): UUID? =
        try {
            UUID.fromString(this)
        } catch (_: IllegalArgumentException) {
            null
        }
}

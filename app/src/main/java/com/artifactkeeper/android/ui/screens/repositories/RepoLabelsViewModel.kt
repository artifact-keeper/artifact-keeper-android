package com.artifactkeeper.android.ui.screens.repositories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.AddLabelRequest
import com.artifactkeeper.client.models.LabelResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for a repository's labels.
 */
data class RepoLabelsUiState(
    val labels: List<LabelResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

/**
 * Repository labels: list a repo's labels, add or update a single label, and
 * remove a label by key. Bulk replacement (setRepoLabels) is deferred.
 */
@HiltViewModel
class RepoLabelsViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoLabelsUiState())
    val uiState: StateFlow<RepoLabelsUiState> = _uiState.asStateFlow()

    fun load(repoKey: String, refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val labels = apiClient.repositoryLabelsApi.listRepoLabels(repoKey).unwrap().items
                    .sortedBy { it.key }
                _uiState.update {
                    it.copy(labels = labels, isLoading = false, isRefreshing = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load labels",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    fun addLabel(repoKey: String, key: String, value: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                apiClient.repositoryLabelsApi
                    .addRepoLabel(repoKey, key, AddLabelRequest(value = value.ifBlank { null }))
                    .unwrap()
                _uiState.update { it.copy(isMutating = false, message = "Label \"$key\" saved") }
                load(repoKey, refresh = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to add label", isMutating = false)
                }
            }
        }
    }

    fun deleteLabel(repoKey: String, key: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                apiClient.repositoryLabelsApi.deleteRepoLabel(repoKey, key).unwrap()
                _uiState.update { it.copy(isMutating = false, message = "Label \"$key\" removed") }
                load(repoKey, refresh = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to delete label", isMutating = false)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}

package com.artifactkeeper.android.ui.screens.staging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.android.data.models.BulkPromoteRequest
import com.artifactkeeper.android.data.models.BulkPromotionResponse
import com.artifactkeeper.android.data.models.PolicyStatus
import com.artifactkeeper.android.data.models.PromoteArtifactRequest
import com.artifactkeeper.android.data.models.PromotionHistoryEntry
import com.artifactkeeper.android.data.models.PromotionResponse
import com.artifactkeeper.android.data.models.Repository
import com.artifactkeeper.android.data.models.StagingArtifact
import com.artifactkeeper.android.data.models.StagingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StagingUiState(
    val stagingRepos: List<StagingRepository> = emptyList(),
    val selectedRepo: StagingRepository? = null,
    val artifacts: List<StagingArtifact> = emptyList(),
    val selectedArtifactIds: Set<String> = emptySet(),
    val promotionHistory: List<PromotionHistoryEntry> = emptyList(),
    val targetRepositories: List<Repository> = emptyList(),
    val isLoadingRepos: Boolean = false,
    val isLoadingArtifacts: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val isPromoting: Boolean = false,
    val reposError: String? = null,
    val artifactsError: String? = null,
    val historyError: String? = null,
    val promotionError: String? = null,
    val promotionSuccess: String? = null,
    val filterStatus: PolicyStatus? = null,
)

class StagingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StagingUiState())
    val uiState: StateFlow<StagingUiState> = _uiState.asStateFlow()

    fun loadStagingRepos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRepos = true, reposError = null) }
            try {
                val response = ApiClient.stagingApi.listStagingRepos().unwrap()
                _uiState.update { it.copy(stagingRepos = response.items, isLoadingRepos = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        reposError = e.message ?: "Failed to load staging repositories",
                        isLoadingRepos = false
                    )
                }
            }
        }
    }

    fun selectRepo(repo: StagingRepository) {
        _uiState.update {
            it.copy(
                selectedRepo = repo,
                artifacts = emptyList(),
                selectedArtifactIds = emptySet(),
                promotionHistory = emptyList(),
            )
        }
        loadArtifacts(repo.key)
        loadTargetRepositories()
    }

    fun clearSelectedRepo() {
        _uiState.update {
            it.copy(
                selectedRepo = null,
                artifacts = emptyList(),
                selectedArtifactIds = emptySet(),
                promotionHistory = emptyList(),
            )
        }
    }

    fun loadArtifacts(repoKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingArtifacts = true, artifactsError = null) }
            try {
                val statusFilter = _uiState.value.filterStatus?.name?.lowercase()
                val response = ApiClient.stagingApi.listStagingArtifacts(
                    repoKey = repoKey,
                    policyStatus = statusFilter,
                ).unwrap()
                _uiState.update { it.copy(artifacts = response.items, isLoadingArtifacts = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        artifactsError = e.message ?: "Failed to load artifacts",
                        isLoadingArtifacts = false
                    )
                }
            }
        }
    }

    fun setFilterStatus(status: PolicyStatus?) {
        _uiState.update { it.copy(filterStatus = status, selectedArtifactIds = emptySet()) }
        _uiState.value.selectedRepo?.let { repo ->
            loadArtifacts(repo.key)
        }
    }

    fun loadPromotionHistory(repoKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true, historyError = null) }
            try {
                val response = ApiClient.stagingApi.getPromotionHistory(repoKey).unwrap()
                _uiState.update { it.copy(promotionHistory = response.items, isLoadingHistory = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        historyError = e.message ?: "Failed to load promotion history",
                        isLoadingHistory = false
                    )
                }
            }
        }
    }

    private fun loadTargetRepositories() {
        viewModelScope.launch {
            try {
                val response = ApiClient.reposApi.listRepositories().unwrap()
                // Filter to only show local repositories as promotion targets
                val localRepos = response.items.filter { it.repoType.equals("local", ignoreCase = true) }
                _uiState.update { it.copy(targetRepositories = localRepos) }
            } catch (e: Exception) {
                // Silently fail, user can retry
            }
        }
    }

    fun toggleArtifactSelection(artifactId: String) {
        _uiState.update { state ->
            val newSelection = if (artifactId in state.selectedArtifactIds) {
                state.selectedArtifactIds - artifactId
            } else {
                state.selectedArtifactIds + artifactId
            }
            state.copy(selectedArtifactIds = newSelection)
        }
    }

    fun selectAllArtifacts() {
        _uiState.update { state ->
            val promotableIds = state.artifacts
                .filter { it.canPromote }
                .map { it.id }
                .toSet()
            state.copy(selectedArtifactIds = promotableIds)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedArtifactIds = emptySet()) }
    }

    fun promoteArtifact(
        artifactId: String,
        targetRepoKey: String,
        force: Boolean = false,
        comment: String? = null,
        onSuccess: (PromotionResponse) -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        val repoKey = _uiState.value.selectedRepo?.key ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isPromoting = true, promotionError = null, promotionSuccess = null) }
            try {
                val request = PromoteArtifactRequest(
                    targetRepositoryKey = targetRepoKey,
                    force = force,
                    comment = comment,
                )
                val response = ApiClient.stagingApi.promoteArtifact(repoKey, artifactId, request).unwrap()
                _uiState.update {
                    it.copy(
                        isPromoting = false,
                        promotionSuccess = response.message,
                        selectedArtifactIds = it.selectedArtifactIds - artifactId,
                    )
                }
                // Reload artifacts to reflect the change
                loadArtifacts(repoKey)
                onSuccess(response)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Promotion failed"
                _uiState.update { it.copy(isPromoting = false, promotionError = errorMsg) }
                onError(errorMsg)
            }
        }
    }

    fun promoteBulk(
        targetRepoKey: String,
        force: Boolean = false,
        comment: String? = null,
        onSuccess: (BulkPromotionResponse) -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        val repoKey = _uiState.value.selectedRepo?.key ?: return
        val artifactIds = _uiState.value.selectedArtifactIds.toList()

        if (artifactIds.isEmpty()) {
            onError("No artifacts selected")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPromoting = true, promotionError = null, promotionSuccess = null) }
            try {
                val request = BulkPromoteRequest(
                    artifactIds = artifactIds,
                    targetRepositoryKey = targetRepoKey,
                    force = force,
                    comment = comment,
                )
                val response = ApiClient.stagingApi.promoteBulk(repoKey, request).unwrap()
                _uiState.update {
                    it.copy(
                        isPromoting = false,
                        promotionSuccess = "${response.totalSucceeded} of ${response.totalRequested} promoted",
                        selectedArtifactIds = emptySet(),
                    )
                }
                // Reload artifacts to reflect changes
                loadArtifacts(repoKey)
                onSuccess(response)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Bulk promotion failed"
                _uiState.update { it.copy(isPromoting = false, promotionError = errorMsg) }
                onError(errorMsg)
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(promotionError = null, promotionSuccess = null) }
    }
}

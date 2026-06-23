package com.artifactkeeper.android.ui.screens.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.PeerInstanceResponse
import com.artifactkeeper.client.models.SyncTaskResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * State for a single peer's detail: its instance status plus the sync tasks
 * queued or running against it.
 */
data class PeerDetailUiState(
    val peer: PeerInstanceResponse? = null,
    val syncTasks: List<SyncTaskResponse> = emptyList(),
    val assignedRepoIds: List<UUID> = emptyList(),
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

@HiltViewModel
class PeerDetailViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeerDetailUiState())
    val uiState: StateFlow<PeerDetailUiState> = _uiState.asStateFlow()

    /**
     * Load the peer instance and its sync tasks. The task list is optional: a
     * peer with no in-flight sync should still render its status.
     */
    fun load(peerId: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val peer = apiClient.peersApi.getPeer(peerId).unwrap()

                var tasks: List<SyncTaskResponse> = emptyList()
                try {
                    tasks = apiClient.peersApi.getSyncTasks(peerId).unwrap()
                } catch (_: Exception) {
                    // No sync tasks available.
                }

                var assignedRepoIds: List<UUID> = emptyList()
                try {
                    assignedRepoIds = apiClient.peersApi.getAssignedRepos(peerId).unwrap()
                } catch (_: Exception) {
                    // No assigned repositories available.
                }

                _uiState.update {
                    it.copy(
                        peer = peer,
                        syncTasks = tasks,
                        assignedRepoIds = assignedRepoIds,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load peer", isLoading = false)
                }
            }
        }
    }

    /** Trigger a full sync for the peer, then reload. */
    fun triggerSync(peerId: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                apiClient.peersApi.triggerSync(peerId).unwrap()
                _uiState.update { it.copy(isMutating = false, message = "Sync triggered") }
                load(peerId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to trigger sync", isMutating = false)
                }
            }
        }
    }

    /** Run a single repository subscription now, then reload. */
    fun runSubscriptionNow(peerId: UUID, repoId: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                val result = apiClient.peersApi.runSubscriptionNow(peerId, repoId).unwrap()
                _uiState.update {
                    it.copy(isMutating = false, message = "${result.status} (${result.tasksQueued} queued)")
                }
                load(peerId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to run subscription", isMutating = false)
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}

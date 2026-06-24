package com.artifactkeeper.android.ui.screens.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.EvaluationResultResponse
import com.artifactkeeper.client.models.SyncPolicyResponse
import com.artifactkeeper.client.models.TogglePolicyPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * State for the sync policies list screen.
 */
data class SyncPoliciesUiState(
    val policies: List<SyncPolicyResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

/**
 * State for a single sync policy's detail view, loaded by id on entry.
 */
data class SyncPolicyDetailUiState(
    val policy: SyncPolicyResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Read and operate actions for replication sync policies. Authoring (create,
 * update, delete, preview) is deferred; this screen lists policies, opens a
 * policy detail by id, toggles a policy's enabled flag, and triggers a global
 * policy evaluation.
 */
@HiltViewModel
class SyncPoliciesViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncPoliciesUiState())
    val uiState: StateFlow<SyncPoliciesUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow(SyncPolicyDetailUiState())
    val detailState: StateFlow<SyncPolicyDetailUiState> = _detailState.asStateFlow()

    fun loadPolicies(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val policies = apiClient.peersApi.listSyncPolicies().unwrap().items
                    .sortedByDescending { it.priority }
                _uiState.update {
                    it.copy(
                        policies = policies,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load sync policies",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    /**
     * Load a single policy fresh by id for the detail view. This intentionally
     * re-fetches rather than reusing the list item so detail reflects the
     * server's current state.
     */
    fun loadPolicyDetail(policyId: UUID) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }
            try {
                val policy = apiClient.peersApi.getSyncPolicy(policyId).unwrap()
                _detailState.update { it.copy(policy = policy, isLoading = false) }
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(error = e.message ?: "Failed to load policy", isLoading = false)
                }
            }
        }
    }

    fun togglePolicy(policyId: UUID, enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null) }
            try {
                apiClient.peersApi.togglePolicy(policyId, TogglePolicyPayload(enabled = enabled)).unwrap()
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        message = if (enabled) "Policy enabled" else "Policy disabled",
                    )
                }
                loadPolicies(refresh = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to toggle policy", isMutating = false)
                }
            }
        }
    }

    fun evaluatePolicies() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null) }
            try {
                val result = apiClient.peersApi.evaluatePolicies().unwrap()
                _uiState.update {
                    it.copy(isMutating = false, message = summarize(result))
                }
                loadPolicies(refresh = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to evaluate policies", isMutating = false)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    private fun summarize(result: EvaluationResultResponse): String =
        "Evaluated ${result.policiesEvaluated} policies: " +
            "${result.created} created, ${result.updated} updated, ${result.removed} removed, " +
            "${result.retroactiveTasksQueued} tasks queued"
}

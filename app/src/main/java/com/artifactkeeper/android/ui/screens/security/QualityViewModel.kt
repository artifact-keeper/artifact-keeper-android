package com.artifactkeeper.android.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.ArtifactHealthResponse
import com.artifactkeeper.client.models.CheckResponse
import com.artifactkeeper.client.models.GateResponse
import com.artifactkeeper.client.models.HealthDashboardResponse
import com.artifactkeeper.client.models.IssueResponse
import com.artifactkeeper.client.models.SuppressIssueRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * State for the quality health overview: the portfolio health dashboard and the
 * configured quality gates.
 */
data class QualityHealthUiState(
    val dashboard: HealthDashboardResponse? = null,
    val gates: List<GateResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

/**
 * State for a single artifact's quality: its health summary, the checks run
 * against it, and the issues those checks found.
 */
data class QualityArtifactUiState(
    val health: ArtifactHealthResponse? = null,
    val checks: List<CheckResponse> = emptyList(),
    val issues: List<IssueResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class QualityViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _healthState = MutableStateFlow(QualityHealthUiState())
    val healthState: StateFlow<QualityHealthUiState> = _healthState.asStateFlow()

    private val _artifactState = MutableStateFlow(QualityArtifactUiState())
    val artifactState: StateFlow<QualityArtifactUiState> = _artifactState.asStateFlow()

    /**
     * Load the portfolio health dashboard and the quality gates. Gates are
     * optional: a deployment may not define any, so a failure there does not
     * fail the screen.
     */
    fun loadHealth(refresh: Boolean = false) {
        viewModelScope.launch {
            _healthState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val dashboard = apiClient.qualityApi.getHealthDashboard().unwrap()

                var gates: List<GateResponse> = emptyList()
                try {
                    gates = apiClient.qualityApi.listGates().unwrap()
                } catch (_: Exception) {
                    // No quality gates configured.
                }

                _healthState.update {
                    it.copy(
                        dashboard = dashboard,
                        gates = gates,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            } catch (e: Exception) {
                _healthState.update {
                    it.copy(
                        error = e.message ?: "Failed to load quality health",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    /**
     * Load a single artifact's health, its checks, and the issues across those
     * checks (most severe first).
     */
    fun loadArtifactQuality(artifactId: UUID) {
        viewModelScope.launch {
            _artifactState.update { it.copy(isLoading = true, error = null) }
            try {
                val health = apiClient.qualityApi.getArtifactHealth(artifactId).unwrap()
                val checks = apiClient.qualityApi.listChecks(artifactId, null).unwrap()

                val issues = mutableListOf<IssueResponse>()
                for (check in checks) {
                    try {
                        issues += apiClient.qualityApi.listCheckIssues(check.id).unwrap()
                    } catch (_: Exception) {
                        // Skip checks whose issues cannot be loaded.
                    }
                }
                issues.sortBy { qualityIssueSeverityRank(it.severity) }

                _artifactState.update {
                    it.copy(health = health, checks = checks, issues = issues, isLoading = false)
                }
            } catch (e: Exception) {
                _artifactState.update {
                    it.copy(
                        error = e.message ?: "Failed to load artifact quality",
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun suppressIssue(artifactId: UUID, issueId: UUID, reason: String) {
        viewModelScope.launch {
            _artifactState.update { it.copy(isMutating = true, error = null) }
            try {
                apiClient.qualityApi.suppressIssue(issueId, SuppressIssueRequest(reason = reason)).unwrap()
                _artifactState.update { it.copy(isMutating = false) }
                loadArtifactQuality(artifactId)
            } catch (e: Exception) {
                _artifactState.update {
                    it.copy(error = e.message ?: "Failed to suppress issue", isMutating = false)
                }
            }
        }
    }

    fun unsuppressIssue(artifactId: UUID, issueId: UUID) {
        viewModelScope.launch {
            _artifactState.update { it.copy(isMutating = true, error = null) }
            try {
                apiClient.qualityApi.unsuppressIssue(issueId).unwrap()
                _artifactState.update { it.copy(isMutating = false) }
                loadArtifactQuality(artifactId)
            } catch (e: Exception) {
                _artifactState.update {
                    it.copy(error = e.message ?: "Failed to unsuppress issue", isMutating = false)
                }
            }
        }
    }

    companion object {
        /** Lower rank sorts first, so critical issues appear at the top. */
        fun qualityIssueSeverityRank(severity: String): Int = when (severity.lowercase()) {
            "critical" -> 0
            "high" -> 1
            "medium" -> 2
            "low" -> 3
            else -> 4
        }
    }
}

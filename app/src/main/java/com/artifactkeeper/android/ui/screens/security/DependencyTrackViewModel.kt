package com.artifactkeeper.android.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.DtComponentFull
import com.artifactkeeper.client.models.DtFinding
import com.artifactkeeper.client.models.DtPolicyFull
import com.artifactkeeper.client.models.DtPolicyViolation
import com.artifactkeeper.client.models.DtProject
import com.artifactkeeper.client.models.DtProjectMetrics
import com.artifactkeeper.client.models.UpdateAnalysisBody
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the Dependency-Track project list.
 */
data class DependencyTrackUiState(
    val projects: List<DtProject> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

/**
 * State for a single Dependency-Track project: its metrics, findings, and any
 * policy violations.
 */
data class DtProjectDetailUiState(
    val metrics: DtProjectMetrics? = null,
    val findings: List<DtFinding> = emptyList(),
    val violations: List<DtPolicyViolation> = emptyList(),
    val components: List<DtComponentFull> = emptyList(),
    val metricsHistory: List<DtProjectMetrics> = emptyList(),
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null,
)

/**
 * State for the Dependency-Track policy list.
 */
data class DtPoliciesUiState(
    val policies: List<DtPolicyFull> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DependencyTrackViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DependencyTrackUiState())
    val uiState: StateFlow<DependencyTrackUiState> = _uiState.asStateFlow()

    private val _projectDetailState = MutableStateFlow(DtProjectDetailUiState())
    val projectDetailState: StateFlow<DtProjectDetailUiState> = _projectDetailState.asStateFlow()

    private val _policiesState = MutableStateFlow(DtPoliciesUiState())
    val policiesState: StateFlow<DtPoliciesUiState> = _policiesState.asStateFlow()

    fun loadProjects(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val projects = apiClient.securityApi.listProjects().unwrap()
                _uiState.update {
                    it.copy(
                        projects = projects.sortedBy { p -> p.name.lowercase() },
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load Dependency-Track projects",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    /**
     * Load metrics, findings, and policy violations for a project. Violations are
     * optional: a project may have no policies bound, so a failure there does not
     * fail the screen.
     */
    fun loadProjectDetail(projectUuid: String) {
        viewModelScope.launch {
            _projectDetailState.update { it.copy(isLoading = true, error = null) }
            try {
                val metrics = apiClient.securityApi.getProjectMetrics(projectUuid).unwrap()
                val findings = apiClient.securityApi.getProjectFindings(projectUuid).unwrap()
                    .sortedBy { dtSeverityRank(it.vulnerability.severity) }

                var violations: List<DtPolicyViolation> = emptyList()
                try {
                    violations = apiClient.securityApi.getProjectViolations(projectUuid).unwrap()
                } catch (_: Exception) {
                    // No policy violations available for this project.
                }

                var components: List<DtComponentFull> = emptyList()
                try {
                    components = apiClient.securityApi.getProjectComponents(projectUuid).unwrap()
                } catch (_: Exception) {
                    // Component inventory not available for this project.
                }

                var metricsHistory: List<DtProjectMetrics> = emptyList()
                try {
                    metricsHistory = apiClient.securityApi.getProjectMetricsHistory(projectUuid).unwrap()
                } catch (_: Exception) {
                    // No metrics history available for this project.
                }

                _projectDetailState.update {
                    it.copy(
                        metrics = metrics,
                        findings = findings,
                        violations = violations,
                        components = components,
                        metricsHistory = metricsHistory,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _projectDetailState.update {
                    it.copy(
                        error = e.message ?: "Failed to load project detail",
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun loadPolicies() {
        viewModelScope.launch {
            _policiesState.update { it.copy(isLoading = true, error = null) }
            try {
                val policies = apiClient.securityApi.listDependencyTrackPolicies().unwrap()
                _policiesState.update { it.copy(policies = policies, isLoading = false) }
            } catch (e: Exception) {
                _policiesState.update {
                    it.copy(error = e.message ?: "Failed to load policies", isLoading = false)
                }
            }
        }
    }

    /**
     * Update the audit analysis for a finding (e.g. mark not affected and
     * suppress), then reload the project detail to reflect the change.
     */
    fun updateFindingAnalysis(
        projectUuid: String,
        componentUuid: String,
        vulnerabilityUuid: String,
        state: String,
        suppressed: Boolean,
        justification: String?,
    ) {
        viewModelScope.launch {
            _projectDetailState.update { it.copy(isUpdating = true, error = null) }
            try {
                apiClient.securityApi.updateAnalysis(
                    UpdateAnalysisBody(
                        componentUuid = componentUuid,
                        projectUuid = projectUuid,
                        state = state,
                        vulnerabilityUuid = vulnerabilityUuid,
                        justification = justification,
                        suppressed = suppressed,
                    ),
                ).unwrap()
                _projectDetailState.update { it.copy(isUpdating = false) }
                loadProjectDetail(projectUuid)
            } catch (e: Exception) {
                _projectDetailState.update {
                    it.copy(error = e.message ?: "Failed to update analysis", isUpdating = false)
                }
            }
        }
    }

    companion object {
        /** Lower rank sorts first, so critical findings appear at the top. */
        fun dtSeverityRank(severity: String): Int = when (severity.uppercase()) {
            "CRITICAL" -> 0
            "HIGH" -> 1
            "MEDIUM" -> 2
            "LOW" -> 3
            else -> 4
        }
    }
}

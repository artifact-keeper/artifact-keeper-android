package com.artifactkeeper.android.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.DashboardResponse
import com.artifactkeeper.client.models.ScanConfigResponse
import com.artifactkeeper.client.models.ScanResponse
import com.artifactkeeper.client.models.ScoreResponse
import com.artifactkeeper.client.models.SigningConfigResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for a single repository's security view: its scan config, security
 * score, and the scans run against it.
 */
data class RepoSecurityUiState(
    val config: ScanConfigResponse? = null,
    val score: ScoreResponse? = null,
    val scans: List<ScanResponse> = emptyList(),
    val signingConfig: SigningConfigResponse? = null,
    val repoPublicKey: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

/**
 * State for the portfolio security dashboard summary.
 */
data class SecurityDashboardUiState(
    val dashboard: DashboardResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class RepoSecurityViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoSecurityUiState())
    val uiState: StateFlow<RepoSecurityUiState> = _uiState.asStateFlow()

    private val _dashboardState = MutableStateFlow(SecurityDashboardUiState())
    val dashboardState: StateFlow<SecurityDashboardUiState> = _dashboardState.asStateFlow()

    /**
     * Load a repository's security config and score, plus its scans. The scan
     * list is optional: a repo may have no scans yet, so a failure there does
     * not fail the screen.
     */
    fun loadRepoSecurity(key: String, refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val security = apiClient.securityApi.getRepoSecurity(key).unwrap()

                var scans: List<ScanResponse> = emptyList()
                try {
                    scans = apiClient.securityApi.listRepoScans(key).unwrap().items
                } catch (_: Exception) {
                    // No scans available for this repository.
                }

                // Signing config and repo public key are optional reads keyed by
                // the repository's UUID (available from the config or score).
                val repoUuid = security.config?.repositoryId ?: security.score?.repositoryId
                var signingConfig: SigningConfigResponse? = null
                var repoPublicKey: String? = null
                if (repoUuid != null) {
                    try {
                        signingConfig = apiClient.signingApi.getRepoSigningConfig(repoUuid).unwrap()
                        repoPublicKey = try {
                            apiClient.signingApi.getRepoPublicKey(repoUuid).unwrap()
                        } catch (_: Exception) {
                            null
                        }
                    } catch (_: Exception) {
                        // No signing config bound to this repository.
                    }
                }

                _uiState.update {
                    it.copy(
                        config = security.config,
                        score = security.score,
                        scans = scans,
                        signingConfig = signingConfig,
                        repoPublicKey = repoPublicKey,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load repository security",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    /**
     * Load the portfolio-level security dashboard summary.
     */
    fun loadDashboard() {
        viewModelScope.launch {
            _dashboardState.update { it.copy(isLoading = true, error = null) }
            try {
                val dashboard = apiClient.securityApi.getDashboard().unwrap()
                _dashboardState.update { it.copy(dashboard = dashboard, isLoading = false) }
            } catch (e: Exception) {
                _dashboardState.update {
                    it.copy(
                        error = e.message ?: "Failed to load security dashboard",
                        isLoading = false,
                    )
                }
            }
        }
    }
}

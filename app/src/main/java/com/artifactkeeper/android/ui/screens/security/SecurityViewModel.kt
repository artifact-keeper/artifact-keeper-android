package com.artifactkeeper.android.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.android.data.models.CveTrends
import com.artifactkeeper.android.data.models.DtPortfolioMetrics
import com.artifactkeeper.android.data.models.DtStatus
import com.artifactkeeper.android.data.models.RepoSecurityScore
import com.artifactkeeper.android.data.models.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityUiState(
    val scores: List<RepoSecurityScore> = emptyList(),
    val repoMap: Map<java.util.UUID, Repository> = emptyMap(),
    val cveTrends: CveTrends? = null,
    val dtStatus: DtStatus? = null,
    val dtPortfolioMetrics: DtPortfolioMetrics? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val scores = apiClient.securityApi.getAllScores().unwrap()
                val repos = apiClient.reposApi.listRepositories(perPage = 100).unwrap().items
                val repoMap = repos.associateBy { it.id }

                var cveTrends: CveTrends? = null
                try {
                    cveTrends = apiClient.sbomApi.getCveTrends().unwrap()
                } catch (_: Exception) {
                    // CVE trends are optional
                }

                var dtStatus: DtStatus? = null
                var dtPortfolioMetrics: DtPortfolioMetrics? = null
                try {
                    val status = apiClient.securityApi.dtStatus().unwrap()
                    dtStatus = status
                    if (status.enabled && status.healthy) {
                        dtPortfolioMetrics = apiClient.securityApi.getPortfolioMetrics().unwrap()
                    }
                } catch (_: Exception) {
                    // Dependency-Track is optional
                }

                _uiState.update {
                    it.copy(
                        scores = scores,
                        repoMap = repoMap,
                        cveTrends = cveTrends,
                        dtStatus = dtStatus,
                        dtPortfolioMetrics = dtPortfolioMetrics,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load security data",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }
}

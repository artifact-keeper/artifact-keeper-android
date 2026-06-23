package com.artifactkeeper.android.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.CveHistoryEntry
import com.artifactkeeper.client.models.UpdateCveStatusRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * State for the CVE tracking list: the CVE history entries for an artifact.
 */
data class CveTrackingUiState(
    val entries: List<CveHistoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

/**
 * State for a single CVE: its detection/status history across an artifact, and
 * an in-flight flag while a status update is being applied.
 */
data class CveDetailUiState(
    val history: List<CveHistoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CveTrackingViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CveTrackingUiState())
    val uiState: StateFlow<CveTrackingUiState> = _uiState.asStateFlow()

    private val _cveDetailState = MutableStateFlow(CveDetailUiState())
    val cveDetailState: StateFlow<CveDetailUiState> = _cveDetailState.asStateFlow()

    fun loadArtifactCves(artifactId: UUID, refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val entries = apiClient.sbomApi.getCveHistoryByArtifact(artifactId).unwrap()
                    .sortedBy { cveSeverityRank(it.severity) }
                _uiState.update {
                    it.copy(entries = entries, isLoading = false, isRefreshing = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load CVE history",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    fun loadCveDetail(artifactId: UUID, cveId: String) {
        viewModelScope.launch {
            _cveDetailState.update { it.copy(isLoading = true, error = null) }
            try {
                val history = apiClient.sbomApi.getCveHistoryByCve(cveId).unwrap()
                    .filter { it.artifactId == artifactId }
                    .ifEmpty { apiClient.sbomApi.getCveHistoryByCve(cveId).unwrap() }
                _cveDetailState.update { it.copy(history = history, isLoading = false) }
            } catch (e: Exception) {
                _cveDetailState.update {
                    it.copy(error = e.message ?: "Failed to load CVE detail", isLoading = false)
                }
            }
        }
    }

    fun updateCveStatus(artifactId: UUID, cveId: String, status: String, reason: String?) {
        viewModelScope.launch {
            _cveDetailState.update { it.copy(isUpdating = true, error = null) }
            try {
                apiClient.sbomApi.updateCveStatusByArtifactCve(
                    artifactId,
                    cveId,
                    UpdateCveStatusRequest(status = status, reason = reason),
                ).unwrap()
                val history = apiClient.sbomApi.getCveHistoryByCve(cveId).unwrap()
                _cveDetailState.update { it.copy(history = history, isUpdating = false) }
            } catch (e: Exception) {
                _cveDetailState.update {
                    it.copy(error = e.message ?: "Failed to update CVE status", isUpdating = false)
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    companion object {
        /** Lower rank sorts first, so critical CVEs appear at the top; unknown last. */
        fun cveSeverityRank(severity: String?): Int = when (severity?.lowercase()) {
            "critical" -> 0
            "high" -> 1
            "medium" -> 2
            "low" -> 3
            else -> 4
        }
    }
}

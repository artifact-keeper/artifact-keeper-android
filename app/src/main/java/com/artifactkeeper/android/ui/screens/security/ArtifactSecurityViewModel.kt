package com.artifactkeeper.android.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.AcknowledgeRequest
import com.artifactkeeper.client.models.ComponentResponse
import com.artifactkeeper.client.models.FindingResponse
import com.artifactkeeper.client.models.ScanResponse
import com.artifactkeeper.client.models.SbomContentResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * State for the per-artifact security overview: the scans that ran against an
 * artifact plus its generated SBOM and component list.
 */
data class ArtifactSecurityUiState(
    val scans: List<ScanResponse> = emptyList(),
    val sbom: SbomContentResponse? = null,
    val components: List<ComponentResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

/**
 * State for a single scan: the scan summary plus its individual findings.
 */
data class ScanDetailUiState(
    val scan: ScanResponse? = null,
    val findings: List<FindingResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

@HiltViewModel
class ArtifactSecurityViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtifactSecurityUiState())
    val uiState: StateFlow<ArtifactSecurityUiState> = _uiState.asStateFlow()

    private val _scanDetailState = MutableStateFlow(ScanDetailUiState())
    val scanDetailState: StateFlow<ScanDetailUiState> = _scanDetailState.asStateFlow()

    /**
     * Load the scans and SBOM for an artifact. The SBOM is optional: an artifact
     * may not have one generated yet, so a failure there does not fail the screen.
     */
    fun loadArtifactSecurity(artifactId: UUID, refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val scans = apiClient.securityApi.listArtifactScans(artifactId).unwrap().items

                var sbom: SbomContentResponse? = null
                var components: List<ComponentResponse> = emptyList()
                try {
                    sbom = apiClient.sbomApi.getSbomByArtifact(artifactId).unwrap()
                    components = apiClient.sbomApi.getSbomComponents(sbom.id).unwrap()
                } catch (_: Exception) {
                    // No SBOM for this artifact yet; leave it absent.
                }

                _uiState.update {
                    it.copy(
                        scans = scans,
                        sbom = sbom,
                        components = components,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load artifact security data",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    /**
     * Load a single scan summary and its findings, sorted most-severe first.
     */
    fun loadScanDetail(scanId: UUID) {
        viewModelScope.launch {
            _scanDetailState.update { it.copy(isLoading = true, error = null) }
            try {
                val scan = apiClient.securityApi.getScan(scanId).unwrap()
                val findings = apiClient.securityApi.listFindings(scanId).unwrap().items
                    .sortedBy { severityRank(it.severity) }

                _scanDetailState.update {
                    it.copy(scan = scan, findings = findings, isLoading = false)
                }
            } catch (e: Exception) {
                _scanDetailState.update {
                    it.copy(
                        error = e.message ?: "Failed to load scan detail",
                        isLoading = false,
                    )
                }
            }
        }
    }

    /**
     * Acknowledge a finding with a reason, then reload the scan so the finding's
     * acknowledged state is reflected.
     */
    fun acknowledgeFinding(scanId: UUID, findingId: UUID, reason: String) {
        viewModelScope.launch {
            _scanDetailState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                apiClient.securityApi.acknowledgeFinding(
                    findingId,
                    AcknowledgeRequest(reason = reason),
                ).unwrap()
                _scanDetailState.update { it.copy(isMutating = false, message = "Finding acknowledged") }
                loadScanDetail(scanId)
            } catch (e: Exception) {
                _scanDetailState.update {
                    it.copy(error = e.message ?: "Failed to acknowledge finding", isMutating = false)
                }
            }
        }
    }

    /**
     * Revoke a finding's acknowledgment, then reload the scan.
     */
    fun revokeAcknowledgment(scanId: UUID, findingId: UUID) {
        viewModelScope.launch {
            _scanDetailState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                apiClient.securityApi.revokeAcknowledgment(findingId).unwrap()
                _scanDetailState.update { it.copy(isMutating = false, message = "Acknowledgment revoked") }
                loadScanDetail(scanId)
            } catch (e: Exception) {
                _scanDetailState.update {
                    it.copy(error = e.message ?: "Failed to revoke acknowledgment", isMutating = false)
                }
            }
        }
    }

    fun clearScanDetailMessage() {
        _scanDetailState.update { it.copy(message = null, error = null) }
    }

    companion object {
        /** Lower rank sorts first, so critical findings appear at the top. */
        fun severityRank(severity: String): Int = when (severity.lowercase()) {
            "critical" -> 0
            "high" -> 1
            "medium" -> 2
            "low" -> 3
            else -> 4
        }
    }
}

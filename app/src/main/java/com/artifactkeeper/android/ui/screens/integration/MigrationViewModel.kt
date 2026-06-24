package com.artifactkeeper.android.ui.screens.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.AssessmentResult
import com.artifactkeeper.client.models.MigrationItemResponse
import com.artifactkeeper.client.models.MigrationJobResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * State for the migration jobs list.
 */
data class MigrationListUiState(
    val jobs: List<MigrationJobResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

/**
 * State for a single migration job's detail: the job, its items, and an
 * optional assessment result.
 */
data class MigrationDetailUiState(
    val job: MigrationJobResponse? = null,
    val items: List<MigrationItemResponse> = emptyList(),
    val assessment: AssessmentResult? = null,
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

/**
 * Migration jobs: read the job list and a job's detail (items + assessment),
 * and operate on a job (start, assess, pause, resume, cancel). Connection and
 * migration authoring (create/delete) and progress streaming are deferred.
 */
@HiltViewModel
class MigrationViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _listState = MutableStateFlow(MigrationListUiState())
    val listState: StateFlow<MigrationListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(MigrationDetailUiState())
    val detailState: StateFlow<MigrationDetailUiState> = _detailState.asStateFlow()

    fun loadJobs(refresh: Boolean = false) {
        viewModelScope.launch {
            _listState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val jobs = apiClient.migrationApi.listMigrations().unwrap()
                _listState.update {
                    it.copy(jobs = jobs, isLoading = false, isRefreshing = false)
                }
            } catch (e: Exception) {
                _listState.update {
                    it.copy(
                        error = e.message ?: "Failed to load migrations",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    /**
     * Load a single job fresh by id for the detail view. Items are best-effort:
     * a job with no items should still render its status.
     */
    fun loadDetail(jobId: UUID) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }
            try {
                val job = apiClient.migrationApi.getMigration(jobId).unwrap()

                var items: List<MigrationItemResponse> = emptyList()
                try {
                    items = apiClient.migrationApi.listMigrationItems(jobId).unwrap()
                } catch (_: Exception) {
                    // No items available.
                }

                _detailState.update {
                    it.copy(job = job, items = items, isLoading = false)
                }
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(error = e.message ?: "Failed to load migration", isLoading = false)
                }
            }
        }
    }

    /** Load the latest assessment for the job into the detail state. */
    fun loadAssessment(jobId: UUID) {
        viewModelScope.launch {
            try {
                val assessment = apiClient.migrationApi.getAssessment(jobId).unwrap()
                _detailState.update { it.copy(assessment = assessment) }
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(error = e.message ?: "Failed to load assessment")
                }
            }
        }
    }

    fun start(jobId: UUID) = operate(jobId, "Migration started") {
        apiClient.migrationApi.startMigration(jobId).unwrap()
    }

    fun assess(jobId: UUID) = operate(jobId, "Assessment started") {
        apiClient.migrationApi.runAssessment(jobId).unwrap()
    }

    fun pause(jobId: UUID) = operate(jobId, "Migration paused") {
        apiClient.migrationApi.pauseMigration(jobId).unwrap()
    }

    fun resume(jobId: UUID) = operate(jobId, "Migration resumed") {
        apiClient.migrationApi.resumeMigration(jobId).unwrap()
    }

    fun cancel(jobId: UUID) = operate(jobId, "Migration cancelled") {
        apiClient.migrationApi.cancelMigration(jobId).unwrap()
    }

    fun clearMessage() {
        _detailState.update { it.copy(message = null, error = null) }
    }

    private fun operate(jobId: UUID, successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _detailState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                action()
                _detailState.update { it.copy(isMutating = false, message = successMessage) }
                loadDetail(jobId)
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(error = e.message ?: "Action failed", isMutating = false)
                }
            }
        }
    }
}

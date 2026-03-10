package com.artifactkeeper.android.ui.screens.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.android.data.models.AlertState
import com.artifactkeeper.android.data.models.DtStatus
import com.artifactkeeper.android.data.models.HealthLogEntry
import com.artifactkeeper.android.data.models.LocalHealthResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import javax.inject.Inject

data class MonitoringUiState(
    val health: LocalHealthResponse? = null,
    val dtStatus: DtStatus? = null,
    val alerts: List<AlertState> = emptyList(),
    val healthLog: List<HealthLogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

private val json = Json { ignoreUnknownKeys = true }

@HiltViewModel
class MonitoringViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonitoringUiState())
    val uiState: StateFlow<MonitoringUiState> = _uiState.asStateFlow()

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
                // Fetch health via OkHttp directly (not under /api/v1)
                val health = withContext(Dispatchers.IO) {
                    val healthUrl = apiClient.baseUrl + "health"
                    val client = apiClient.httpClient
                    val request = Request.Builder().url(healthUrl).build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: "{}"
                    json.decodeFromString<LocalHealthResponse>(body)
                }

                var dtStatus: DtStatus? = null
                try {
                    dtStatus = apiClient.securityApi.dtStatus().unwrap()
                } catch (_: Exception) {
                    // Dependency-Track is optional
                }

                val alerts = apiClient.monitoringApi.getAlertStates().unwrap()
                val healthLog = apiClient.monitoringApi.getHealthLog().unwrap()

                _uiState.update {
                    it.copy(
                        health = health,
                        dtStatus = dtStatus,
                        alerts = alerts,
                        healthLog = healthLog,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load monitoring data",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }
}

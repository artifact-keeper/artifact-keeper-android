package com.artifactkeeper.android.ui.screens.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.InstallFromGitRequest
import com.artifactkeeper.client.models.PluginResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * State for the plugins list.
 */
data class PluginsUiState(
    val plugins: List<PluginResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

/**
 * State for a single plugin's detail.
 */
data class PluginDetailUiState(
    val plugin: PluginResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PluginsViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PluginsUiState())
    val uiState: StateFlow<PluginsUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow(PluginDetailUiState())
    val detailState: StateFlow<PluginDetailUiState> = _detailState.asStateFlow()

    fun loadPlugins(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val plugins = apiClient.pluginsApi.listPlugins(null, null).unwrap().items
                    .sortedBy { it.displayName.lowercase() }
                _uiState.update {
                    it.copy(plugins = plugins, isLoading = false, isRefreshing = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load plugins",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    /** Load a single plugin's detail. Always hits getPlugin(id). */
    fun loadPluginDetail(id: UUID) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }
            try {
                val plugin = apiClient.pluginsApi.getPlugin(id).unwrap()
                _detailState.update { it.copy(plugin = plugin, isLoading = false) }
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(error = e.message ?: "Failed to load plugin", isLoading = false)
                }
            }
        }
    }

    fun enablePlugin(id: UUID) = mutate("Plugin enabled", "Failed to enable plugin") {
        apiClient.pluginsApi.enablePlugin(id).unwrap()
    }

    fun disablePlugin(id: UUID) = mutate("Plugin disabled", "Failed to disable plugin") {
        apiClient.pluginsApi.disablePlugin(id).unwrap()
    }

    fun reloadPlugin(id: UUID) = mutate("Plugin reloaded", "Failed to reload plugin") {
        apiClient.pluginsApi.reloadPlugin(id).unwrap()
    }

    fun uninstallPlugin(id: UUID) = mutate("Plugin uninstalled", "Failed to uninstall plugin") {
        apiClient.pluginsApi.uninstallPlugin(id).unwrap()
    }

    fun installFromGit(url: String, ref: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                val result = apiClient.pluginsApi.installFromGit(
                    InstallFromGitRequest(url = url, ref = ref),
                ).unwrap()
                _uiState.update { it.copy(isMutating = false, message = result.message) }
                loadPlugins()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to install plugin", isMutating = false)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, message = null) }
    }

    private fun mutate(successMessage: String, failureMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                block()
                _uiState.update { it.copy(isMutating = false, message = successMessage) }
                loadPlugins()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: failureMessage, isMutating = false)
                }
            }
        }
    }
}

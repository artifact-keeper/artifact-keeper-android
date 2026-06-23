package com.artifactkeeper.android.ui.screens.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.DeliveryResponse
import com.artifactkeeper.client.models.WebhookResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * State for a single webhook's detail: its configuration plus recent delivery
 * attempts, and the secret produced by a rotation (shown once).
 */
data class WebhookDetailUiState(
    val webhook: WebhookResponse? = null,
    val deliveries: List<DeliveryResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val newSecret: String? = null,
)

@HiltViewModel
class WebhookDetailViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebhookDetailUiState())
    val uiState: StateFlow<WebhookDetailUiState> = _uiState.asStateFlow()

    /**
     * Load the webhook configuration and its recent deliveries. The delivery
     * list is optional: an endpoint with no history (or that does not expose
     * deliveries) should still render the webhook config.
     */
    fun load(webhookId: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val webhook = apiClient.webhooksApi.getWebhook(webhookId).unwrap()

                var deliveries: List<DeliveryResponse> = emptyList()
                try {
                    deliveries = apiClient.webhooksApi.listDeliveries(webhookId).unwrap().items
                } catch (_: Exception) {
                    // No delivery history available.
                }

                _uiState.update {
                    it.copy(webhook = webhook, deliveries = deliveries, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load webhook", isLoading = false)
                }
            }
        }
    }

    /** Re-send a past delivery, then refresh the delivery list. */
    fun redeliver(webhookId: UUID, deliveryId: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                apiClient.webhooksApi.redeliver(webhookId, deliveryId).unwrap()
                _uiState.update { it.copy(isMutating = false, message = "Delivery re-sent") }
                load(webhookId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to redeliver", isMutating = false)
                }
            }
        }
    }

    /** Rotate the signing secret and surface the new value once. */
    fun rotateSecret(webhookId: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                val rotated = apiClient.webhooksApi.rotateWebhookSecret(webhookId).unwrap()
                _uiState.update { it.copy(isMutating = false, newSecret = rotated.secret) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to rotate secret", isMutating = false)
                }
            }
        }
    }

    fun clearNewSecret() {
        _uiState.update { it.copy(newSecret = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}

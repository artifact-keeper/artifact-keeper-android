package com.artifactkeeper.android.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.CreateKeyPayload
import com.artifactkeeper.client.models.SigningKeyPublic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * State for the signing keys screen.
 */
data class SigningUiState(
    val keys: List<SigningKeyPublic> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

/**
 * State for a single signing key's detail, including its public key PEM.
 */
data class KeyDetailUiState(
    val key: SigningKeyPublic? = null,
    val publicKeyPem: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SigningViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SigningUiState())
    val uiState: StateFlow<SigningUiState> = _uiState.asStateFlow()

    private val _keyDetailState = MutableStateFlow(KeyDetailUiState())
    val keyDetailState: StateFlow<KeyDetailUiState> = _keyDetailState.asStateFlow()

    /**
     * Load a single key's metadata and its public key PEM for the detail view.
     */
    fun loadKeyDetail(keyId: UUID) {
        viewModelScope.launch {
            _keyDetailState.update { it.copy(isLoading = true, error = null) }
            try {
                val key = apiClient.signingApi.getKey(keyId).unwrap()
                val pem = try {
                    apiClient.signingApi.getPublicKey(keyId).unwrap()
                } catch (_: Exception) {
                    key.publicKeyPem
                }
                _keyDetailState.update {
                    it.copy(key = key, publicKeyPem = pem, isLoading = false)
                }
            } catch (e: Exception) {
                _keyDetailState.update {
                    it.copy(error = e.message ?: "Failed to load key detail", isLoading = false)
                }
            }
        }
    }

    fun loadKeys(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val keys = apiClient.signingApi.listKeys(null).unwrap().propertyKeys
                    .sortedWith(compareByDescending<SigningKeyPublic> { it.isActive }.thenBy { it.name.lowercase() })
                _uiState.update {
                    it.copy(keys = keys, isLoading = false, isRefreshing = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load signing keys",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    fun createKey(name: String, algorithm: String?, keyType: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                apiClient.signingApi.createKey(
                    CreateKeyPayload(
                        name = name,
                        algorithm = algorithm,
                        keyType = keyType,
                    ),
                ).unwrap()
                _uiState.update { it.copy(isMutating = false, message = "Key created") }
                loadKeys()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to create key", isMutating = false)
                }
            }
        }
    }

    fun revokeKey(keyId: UUID) = mutate("Key revoked", "Failed to revoke key") {
        apiClient.signingApi.revokeKey(keyId).unwrap()
    }

    fun rotateKey(keyId: UUID) = mutate("Key rotated", "Failed to rotate key") {
        apiClient.signingApi.rotateKey(keyId).unwrap()
    }

    fun deleteKey(keyId: UUID) = mutate("Key deleted", "Failed to delete key") {
        apiClient.signingApi.deleteKey(keyId).unwrap()
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
                loadKeys()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: failureMessage, isMutating = false)
                }
            }
        }
    }
}

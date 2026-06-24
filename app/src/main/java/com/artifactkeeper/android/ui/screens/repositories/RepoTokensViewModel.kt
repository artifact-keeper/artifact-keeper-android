package com.artifactkeeper.android.ui.screens.repositories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.CreateRepoTokenRequest
import com.artifactkeeper.client.models.RepoTokenResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * State for a repository's access tokens. [newTokenSecret] holds the one-time
 * plaintext token returned on creation; it is shown once and then cleared.
 */
data class RepoTokensUiState(
    val tokens: List<RepoTokenResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val newTokenSecret: String? = null,
)

/**
 * State for a single token's detail, fetched fresh by id.
 */
data class RepoTokenDetailUiState(
    val token: RepoTokenResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Repository access tokens: list a repo's tokens, view a single token by id,
 * create a token (the plaintext is returned once on creation), and revoke a
 * token by id.
 */
@HiltViewModel
class RepoTokensViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoTokensUiState())
    val uiState: StateFlow<RepoTokensUiState> = _uiState.asStateFlow()

    private val _tokenDetailState = MutableStateFlow(RepoTokenDetailUiState())
    val tokenDetailState: StateFlow<RepoTokenDetailUiState> = _tokenDetailState.asStateFlow()

    fun load(repoKey: String, refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (refresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val tokens = apiClient.repositoryTokensApi.listRepoTokens(repoKey).unwrap().items
                    .sortedBy { it.isRevoked }
                _uiState.update {
                    it.copy(tokens = tokens, isLoading = false, isRefreshing = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load tokens",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    /** Load a single token fresh by id for the detail view. */
    fun loadTokenDetail(repoKey: String, tokenId: UUID) {
        viewModelScope.launch {
            _tokenDetailState.update { it.copy(isLoading = true, error = null) }
            try {
                val token = apiClient.repositoryTokensApi.getRepoToken(repoKey, tokenId).unwrap()
                _tokenDetailState.update { it.copy(token = token, isLoading = false) }
            } catch (e: Exception) {
                _tokenDetailState.update {
                    it.copy(error = e.message ?: "Failed to load token", isLoading = false)
                }
            }
        }
    }

    fun createToken(
        repoKey: String,
        name: String,
        scopes: List<String>,
        expiresInDays: Long?,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                val created = apiClient.repositoryTokensApi.createRepoToken(
                    repoKey,
                    CreateRepoTokenRequest(name = name, scopes = scopes, expiresInDays = expiresInDays),
                ).unwrap()
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        message = "Token \"$name\" created",
                        newTokenSecret = created.token,
                    )
                }
                load(repoKey, refresh = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to create token", isMutating = false)
                }
            }
        }
    }

    fun revokeToken(repoKey: String, tokenId: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, message = null) }
            try {
                apiClient.repositoryTokensApi.revokeRepoToken(repoKey, tokenId).unwrap()
                _uiState.update { it.copy(isMutating = false, message = "Token revoked") }
                load(repoKey, refresh = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to revoke token", isMutating = false)
                }
            }
        }
    }

    /** Clear the one-time plaintext token after the user has copied it. */
    fun clearNewTokenSecret() {
        _uiState.update { it.copy(newTokenSecret = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}

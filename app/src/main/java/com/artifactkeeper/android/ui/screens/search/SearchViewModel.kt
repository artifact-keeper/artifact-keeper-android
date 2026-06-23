package com.artifactkeeper.android.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.ChecksumArtifact
import com.artifactkeeper.client.models.SearchResultItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchMode {
    TEXT,
    CHECKSUM,
}

data class SearchUiState(
    val mode: SearchMode = SearchMode.TEXT,
    val results: List<SearchResultItem> = emptyList(),
    val checksumResults: List<ChecksumArtifact> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val recent: List<SearchResultItem> = emptyList(),
    val trending: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /** Loads the recent and trending lists shown before a search is run. */
    fun loadInitial() {
        viewModelScope.launch {
            val recent = runCatchingApi { apiClient.searchApi.recent(RECENT_LIMIT).unwrap() } ?: emptyList()
            val trending =
                runCatchingApi { apiClient.searchApi.trending(TRENDING_DAYS, TRENDING_LIMIT).unwrap() } ?: emptyList()
            _uiState.update { it.copy(recent = recent, trending = trending) }
        }
    }

    fun setMode(mode: SearchMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                results = emptyList(),
                checksumResults = emptyList(),
                suggestions = emptyList(),
                hasSearched = false,
                error = null,
            )
        }
    }

    fun quickSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), hasSearched = false, error = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                val results = apiClient.searchApi.quickSearch(trimmed, QUICK_LIMIT, null).unwrap().results
                _uiState.update {
                    it.copy(
                        results = results,
                        suggestions = emptyList(),
                        isSearching = false,
                        hasSearched = true,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, hasSearched = true, error = e.message ?: "Search failed")
                }
            }
        }
    }

    fun loadSuggestions(prefix: String) {
        val trimmed = prefix.trim()
        if (trimmed.length < MIN_SUGGEST_PREFIX) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }
        viewModelScope.launch {
            val suggestions =
                runCatchingApi { apiClient.searchApi.suggest(trimmed, SUGGEST_LIMIT).unwrap()?.suggestions }
                    ?: emptyList()
            _uiState.update { it.copy(suggestions = suggestions) }
        }
    }

    fun checksumSearch(checksum: String, algorithm: String? = null) {
        val trimmed = checksum.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(checksumResults = emptyList(), hasSearched = false, error = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                val artifacts = apiClient.searchApi.checksumSearch(trimmed, algorithm).unwrap().artifacts
                _uiState.update {
                    it.copy(checksumResults = artifacts, isSearching = false, hasSearched = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, hasSearched = true, error = e.message ?: "Checksum search failed")
                }
            }
        }
    }

    fun advancedSearch(
        query: String? = null,
        format: String? = null,
        repositoryKey: String? = null,
        minSize: Long? = null,
        maxSize: Long? = null,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                val response = apiClient.searchApi.advancedSearch(
                    query = query?.trim()?.ifBlank { null },
                    format = format,
                    repositoryKey = repositoryKey,
                    name = null,
                    path = null,
                    version = null,
                    minSize = minSize,
                    maxSize = maxSize,
                    createdAfter = null,
                    createdBefore = null,
                    page = 1,
                    perPage = ADVANCED_PER_PAGE,
                    sortBy = null,
                    sortOrder = null,
                ).unwrap()
                _uiState.update {
                    it.copy(results = response.items, isSearching = false, hasSearched = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, hasSearched = true, error = e.message ?: "Search failed")
                }
            }
        }
    }

    fun clear() {
        _uiState.update {
            it.copy(
                results = emptyList(),
                checksumResults = emptyList(),
                suggestions = emptyList(),
                hasSearched = false,
                error = null,
            )
        }
    }

    private inline fun <T> runCatchingApi(block: () -> T?): T? =
        try {
            block()
        } catch (_: Exception) {
            null
        }

    private companion object {
        const val QUICK_LIMIT = 25L
        const val SUGGEST_LIMIT = 8L
        const val RECENT_LIMIT = 10L
        const val TRENDING_LIMIT = 10L
        const val TRENDING_DAYS = 7
        const val ADVANCED_PER_PAGE = 20
        const val MIN_SUGGEST_PREFIX = 2
    }
}

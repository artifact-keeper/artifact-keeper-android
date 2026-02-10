package com.artifactkeeper.android.ui.screens.repositories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.android.data.models.AddMemberRequest
import com.artifactkeeper.android.data.models.Repository
import com.artifactkeeper.android.data.models.VirtualMember
import com.artifactkeeper.client.models.UpdateVirtualMembersRequest
import com.artifactkeeper.client.models.VirtualMemberPriority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VirtualMembersUiState(
    val members: List<VirtualMember> = emptyList(),
    val eligibleRepos: List<Repository> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingEligible: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

class VirtualMembersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VirtualMembersUiState())
    val uiState: StateFlow<VirtualMembersUiState> = _uiState.asStateFlow()

    private var currentRepoKey: String = ""

    fun loadMembers(repoKey: String) {
        currentRepoKey = repoKey
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = ApiClient.reposApi.listVirtualMembers(repoKey).unwrap()
                _uiState.update {
                    it.copy(
                        members = response.items.sortedBy { member -> member.priority },
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load members",
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun loadEligibleRepos(repoKey: String, repoFormat: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEligible = true) }
            try {
                val response = ApiClient.reposApi.listRepositories().unwrap()
                // Filter to local and remote repos of the same format
                // Exclude repos that are already members
                val currentMemberKeys = _uiState.value.members.map { it.memberRepoKey }.toSet()
                val eligible = response.items.filter { repo ->
                    repo.key != repoKey &&
                    repo.format.equals(repoFormat, ignoreCase = true) &&
                    (repo.repoType.equals("local", ignoreCase = true) ||
                     repo.repoType.equals("remote", ignoreCase = true)) &&
                    repo.key !in currentMemberKeys
                }
                _uiState.update { it.copy(eligibleRepos = eligible, isLoadingEligible = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingEligible = false) }
            }
        }
    }

    fun addMember(memberKey: String, priority: Int? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }
            try {
                val request = AddMemberRequest(memberKey = memberKey, priority = priority)
                ApiClient.reposApi.addVirtualMember(currentRepoKey, request).unwrap()
                _uiState.update { it.copy(isSaving = false, successMessage = "Member added") }
                loadMembers(currentRepoKey)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to add member",
                        isSaving = false,
                    )
                }
            }
        }
    }

    fun removeMember(memberKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }
            try {
                ApiClient.reposApi.removeVirtualMember(currentRepoKey, memberKey).unwrap()
                _uiState.update { it.copy(isSaving = false, successMessage = "Member removed") }
                loadMembers(currentRepoKey)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to remove member",
                        isSaving = false,
                    )
                }
            }
        }
    }

    fun reorderMembers(reorderedMembers: List<VirtualMember>) {
        // Optimistically update UI
        _uiState.update { it.copy(members = reorderedMembers) }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }
            try {
                val memberPriorities = reorderedMembers.mapIndexed { index, member ->
                    VirtualMemberPriority(memberKey = member.memberRepoKey, priority = index + 1)
                }
                val request = UpdateVirtualMembersRequest(members = memberPriorities)
                val response = ApiClient.reposApi.updateVirtualMembers(currentRepoKey, request).unwrap()
                _uiState.update {
                    it.copy(
                        members = response.items.sortedBy { member -> member.priority },
                        isSaving = false,
                        successMessage = "Order saved",
                    )
                }
            } catch (e: Exception) {
                // Reload to restore correct state
                loadMembers(currentRepoKey)
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to reorder members",
                        isSaving = false,
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}

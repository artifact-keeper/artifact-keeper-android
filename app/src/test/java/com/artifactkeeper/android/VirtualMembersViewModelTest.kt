package com.artifactkeeper.android

import com.artifactkeeper.android.ui.screens.repositories.VirtualMembersUiState
import com.artifactkeeper.android.ui.screens.repositories.VirtualMembersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VirtualMembersViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: VirtualMembersViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = VirtualMembersViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // Initial state
    // =========================================================================

    @Test
    fun `initial state has empty collections and no loading flags`() {
        val state = viewModel.uiState.value
        assertEquals(VirtualMembersUiState(), state)
        assertTrue(state.members.isEmpty())
        assertTrue(state.eligibleRepos.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isLoadingEligible)
        assertFalse(state.isSaving)
        assertNull(state.error)
        assertNull(state.successMessage)
    }

    // =========================================================================
    // clearMessages
    // =========================================================================

    @Test
    fun `clearMessages resets error and successMessage`() {
        viewModel.clearMessages()
        val state = viewModel.uiState.value
        assertNull(state.error)
        assertNull(state.successMessage)
    }

    // =========================================================================
    // VirtualMembersUiState data class
    // =========================================================================

    @Test
    fun `VirtualMembersUiState copy preserves unmodified fields`() {
        val original = VirtualMembersUiState(
            isLoading = true,
            error = "something failed",
            isSaving = true,
        )
        val copied = original.copy(isLoading = false)

        assertFalse(copied.isLoading)
        assertEquals("something failed", copied.error)
        assertTrue(copied.isSaving)
    }

    @Test
    fun `VirtualMembersUiState default equality`() {
        val a = VirtualMembersUiState()
        val b = VirtualMembersUiState()
        assertEquals(a, b)
    }

    @Test
    fun `VirtualMembersUiState with all fields set`() {
        val state = VirtualMembersUiState(
            members = emptyList(),
            eligibleRepos = emptyList(),
            isLoading = true,
            isLoadingEligible = true,
            isSaving = true,
            error = "error",
            successMessage = "success",
        )
        assertTrue(state.isLoading)
        assertTrue(state.isLoadingEligible)
        assertTrue(state.isSaving)
        assertEquals("error", state.error)
        assertEquals("success", state.successMessage)
    }
}

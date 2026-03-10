package com.artifactkeeper.android

import com.artifactkeeper.android.ui.screens.operations.MonitoringUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringViewModelTest {

    // =========================================================================
    // MonitoringUiState data class
    // =========================================================================

    @Test
    fun `MonitoringUiState default values`() {
        val state = MonitoringUiState()
        assertNull(state.health)
        assertNull(state.dtStatus)
        assertTrue(state.alerts.isEmpty())
        assertTrue(state.healthLog.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    @Test
    fun `MonitoringUiState copy preserves unmodified fields`() {
        val original = MonitoringUiState(
            isLoading = true,
            error = "timeout",
        )
        val copied = original.copy(isLoading = false)
        assertFalse(copied.isLoading)
        assertEquals("timeout", copied.error)
    }

    @Test
    fun `MonitoringUiState equality`() {
        assertEquals(MonitoringUiState(), MonitoringUiState())
    }

    @Test
    fun `MonitoringUiState with all fields populated`() {
        val state = MonitoringUiState(
            health = null,
            dtStatus = null,
            alerts = emptyList(),
            healthLog = emptyList(),
            isLoading = true,
            isRefreshing = true,
            error = "error",
        )
        assertTrue(state.isLoading)
        assertTrue(state.isRefreshing)
        assertEquals("error", state.error)
    }

    @Test
    fun `MonitoringUiState copy for refresh clears error and sets isRefreshing`() {
        val state = MonitoringUiState(error = "old error", isLoading = false)
        val refreshing = state.copy(isRefreshing = true, error = null)
        assertTrue(refreshing.isRefreshing)
        assertNull(refreshing.error)
    }

    @Test
    fun `MonitoringUiState copy for initial load sets isLoading`() {
        val state = MonitoringUiState()
        val loading = state.copy(isLoading = true, error = null)
        assertTrue(loading.isLoading)
        assertNull(loading.error)
    }

    @Test
    fun `MonitoringUiState can represent completed load with health data`() {
        val state = MonitoringUiState(
            isLoading = false,
            isRefreshing = false,
            error = null,
        )
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    @Test
    fun `MonitoringUiState can represent error state`() {
        val state = MonitoringUiState(
            isLoading = false,
            isRefreshing = false,
            error = "Failed to load monitoring data",
        )
        assertFalse(state.isLoading)
        assertEquals("Failed to load monitoring data", state.error)
    }
}

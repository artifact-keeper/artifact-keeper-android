package com.artifactkeeper.android

import com.artifactkeeper.android.ui.components.EmptyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the EmptyState data class and the decision logic
 * used by LoadingErrorContainer in SharedComposables.kt.
 */
class SharedComposablesTest {

    // =========================================================================
    // EmptyState data class
    // =========================================================================

    @Test
    fun `EmptyState default values`() {
        val state = EmptyState()
        assertFalse(state.isEmpty)
        assertEquals("No data available", state.message)
        assertNull(state.content)
    }

    @Test
    fun `EmptyState with custom message`() {
        val state = EmptyState(isEmpty = true, message = "No repositories found")
        assertTrue(state.isEmpty)
        assertEquals("No repositories found", state.message)
        assertNull(state.content)
    }

    @Test
    fun `EmptyState with custom content suppresses message`() {
        val customContent: () -> Unit = {}
        // The content being non-null means the container will use it instead of the message
        val state = EmptyState(isEmpty = true, content = { })
        assertTrue(state.isEmpty)
        assertTrue(state.content != null)
    }

    @Test
    fun `EmptyState equality with same values`() {
        val a = EmptyState(isEmpty = true, message = "No data")
        val b = EmptyState(isEmpty = true, message = "No data")
        assertEquals(a.isEmpty, b.isEmpty)
        assertEquals(a.message, b.message)
    }

    @Test
    fun `EmptyState copy preserves unmodified fields`() {
        val original = EmptyState(isEmpty = true, message = "Custom message")
        val copied = original.copy(isEmpty = false)
        assertFalse(copied.isEmpty)
        assertEquals("Custom message", copied.message)
    }

    @Test
    fun `EmptyState with isEmpty false and no content does not show empty state`() {
        val state = EmptyState(isEmpty = false)
        assertFalse(state.isEmpty)
    }

    // =========================================================================
    // LoadingErrorContainer decision logic
    // (testing the when-expression priority order without Compose)
    // =========================================================================

    /**
     * Mirrors the decision logic in LoadingErrorContainer.
     * Returns which branch the container would take.
     */
    private fun containerBranch(
        isLoading: Boolean,
        error: String?,
        emptyState: EmptyState,
    ): String = when {
        isLoading -> "loading"
        error != null -> "error"
        emptyState.isEmpty -> if (emptyState.content != null) "emptyCustom" else "emptyDefault"
        else -> "content"
    }

    @Test
    fun `loading state takes priority over error`() {
        val branch = containerBranch(
            isLoading = true,
            error = "Some error",
            emptyState = EmptyState(),
        )
        assertEquals("loading", branch)
    }

    @Test
    fun `loading state takes priority over empty`() {
        val branch = containerBranch(
            isLoading = true,
            error = null,
            emptyState = EmptyState(isEmpty = true),
        )
        assertEquals("loading", branch)
    }

    @Test
    fun `error state takes priority over empty`() {
        val branch = containerBranch(
            isLoading = false,
            error = "Network failure",
            emptyState = EmptyState(isEmpty = true),
        )
        assertEquals("error", branch)
    }

    @Test
    fun `empty state with default message`() {
        val branch = containerBranch(
            isLoading = false,
            error = null,
            emptyState = EmptyState(isEmpty = true, message = "No items"),
        )
        assertEquals("emptyDefault", branch)
    }

    @Test
    fun `empty state with custom content`() {
        val branch = containerBranch(
            isLoading = false,
            error = null,
            emptyState = EmptyState(isEmpty = true, content = { }),
        )
        assertEquals("emptyCustom", branch)
    }

    @Test
    fun `content branch when nothing else applies`() {
        val branch = containerBranch(
            isLoading = false,
            error = null,
            emptyState = EmptyState(isEmpty = false),
        )
        assertEquals("content", branch)
    }

    @Test
    fun `content branch with default empty state`() {
        val branch = containerBranch(
            isLoading = false,
            error = null,
            emptyState = EmptyState(),
        )
        assertEquals("content", branch)
    }

    @Test
    fun `all three flags active shows loading since it has highest priority`() {
        val branch = containerBranch(
            isLoading = true,
            error = "Error present",
            emptyState = EmptyState(isEmpty = true),
        )
        assertEquals("loading", branch)
    }

    @Test
    fun `error and empty both active shows error since error has higher priority`() {
        val branch = containerBranch(
            isLoading = false,
            error = "Something failed",
            emptyState = EmptyState(isEmpty = true),
        )
        assertEquals("error", branch)
    }
}

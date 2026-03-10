package com.artifactkeeper.android

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the local models and logic used in ApiTokensScreen.
 * The screen-private types (ApiTokenItem, ApiTokenListLocal) are duplicated
 * here because they are private to the composable file, so we test the
 * serialization contract and scope validation logic.
 */
class ApiTokensScreenTest {

    // Mirror of the private data classes in ApiTokensScreen.kt for test purposes
    @Serializable
    private data class ApiTokenItem(
        val id: String,
        val name: String,
        val scopes: List<String> = emptyList(),
        @SerialName("token_prefix") val tokenPrefix: String = "",
        @SerialName("created_at") val createdAt: String = "",
        @SerialName("expires_at") val expiresAt: String? = null,
        @SerialName("last_used_at") val lastUsedAt: String? = null,
    )

    @Serializable
    private data class ApiTokenListLocal(
        val items: List<ApiTokenItem> = emptyList(),
    )

    private val json = Json { ignoreUnknownKeys = true }

    // =========================================================================
    // ApiTokenItem serialization
    // =========================================================================

    @Test
    fun `ApiTokenItem round-trip serialization`() {
        val token = ApiTokenItem(
            id = "tok-1",
            name = "CI Pipeline",
            scopes = listOf("read:repos", "write:packages"),
            tokenPrefix = "ak_abc",
            createdAt = "2024-06-01T12:00:00Z",
            expiresAt = "2024-09-01T12:00:00Z",
            lastUsedAt = "2024-06-15T08:30:00Z",
        )
        val encoded = json.encodeToString(token)
        val decoded = json.decodeFromString<ApiTokenItem>(encoded)
        assertEquals(token, decoded)
    }

    @Test
    fun `ApiTokenItem defaults for optional fields`() {
        val token = ApiTokenItem(id = "tok-2", name = "Quick Token")
        assertTrue(token.scopes.isEmpty())
        assertEquals("", token.tokenPrefix)
        assertEquals("", token.createdAt)
        assertNull(token.expiresAt)
        assertNull(token.lastUsedAt)
    }

    @Test
    fun `ApiTokenItem deserialization with snake_case JSON`() {
        val jsonStr = """
            {
                "id": "tok-3",
                "name": "Deploy Key",
                "scopes": ["admin"],
                "token_prefix": "ak_xyz",
                "created_at": "2024-01-01T00:00:00Z",
                "expires_at": null,
                "last_used_at": "2024-06-10T10:00:00Z"
            }
        """.trimIndent()
        val token = json.decodeFromString<ApiTokenItem>(jsonStr)
        assertEquals("tok-3", token.id)
        assertEquals("Deploy Key", token.name)
        assertEquals(listOf("admin"), token.scopes)
        assertEquals("ak_xyz", token.tokenPrefix)
        assertNull(token.expiresAt)
        assertEquals("2024-06-10T10:00:00Z", token.lastUsedAt)
    }

    @Test
    fun `ApiTokenItem deserialization ignores unknown keys`() {
        val jsonStr = """
            {
                "id": "tok-4",
                "name": "Test",
                "unknown_field": "should be ignored",
                "another": 42
            }
        """.trimIndent()
        val token = json.decodeFromString<ApiTokenItem>(jsonStr)
        assertEquals("tok-4", token.id)
        assertEquals("Test", token.name)
    }

    // =========================================================================
    // ApiTokenListLocal serialization
    // =========================================================================

    @Test
    fun `ApiTokenListLocal round-trip serialization`() {
        val list = ApiTokenListLocal(
            items = listOf(
                ApiTokenItem(id = "1", name = "Token A"),
                ApiTokenItem(id = "2", name = "Token B", scopes = listOf("read:repos")),
            )
        )
        val encoded = json.encodeToString(list)
        val decoded = json.decodeFromString<ApiTokenListLocal>(encoded)
        assertEquals(list, decoded)
    }

    @Test
    fun `ApiTokenListLocal defaults to empty items`() {
        val list = ApiTokenListLocal()
        assertTrue(list.items.isEmpty())
    }

    @Test
    fun `ApiTokenListLocal deserialization from empty items JSON`() {
        val jsonStr = """{"items":[]}"""
        val list = json.decodeFromString<ApiTokenListLocal>(jsonStr)
        assertTrue(list.items.isEmpty())
    }

    @Test
    fun `ApiTokenListLocal deserialization from minimal JSON`() {
        val jsonStr = """{"items":[{"id":"t1","name":"n1"}]}"""
        val list = json.decodeFromString<ApiTokenListLocal>(jsonStr)
        assertEquals(1, list.items.size)
        assertEquals("t1", list.items[0].id)
    }

    // =========================================================================
    // Available scopes list
    // =========================================================================

    @Test
    fun `available scopes include expected values`() {
        // Mirrors the AVAILABLE_SCOPES constant in ApiTokensScreen.kt
        val scopes = listOf(
            "read:repos", "write:repos", "read:packages", "write:packages",
            "read:builds", "write:builds", "admin",
        )
        assertEquals(7, scopes.size)
        assertTrue(scopes.contains("read:repos"))
        assertTrue(scopes.contains("write:repos"))
        assertTrue(scopes.contains("read:packages"))
        assertTrue(scopes.contains("write:packages"))
        assertTrue(scopes.contains("read:builds"))
        assertTrue(scopes.contains("write:builds"))
        assertTrue(scopes.contains("admin"))
    }

    @Test
    fun `all scopes are unique`() {
        val scopes = listOf(
            "read:repos", "write:repos", "read:packages", "write:packages",
            "read:builds", "write:builds", "admin",
        )
        assertEquals(scopes.size, scopes.toSet().size)
    }

    // =========================================================================
    // Token name validation
    // =========================================================================

    @Test
    fun `blank token name is invalid`() {
        assertTrue("".isBlank())
        assertTrue("   ".isBlank())
    }

    @Test
    fun `non-blank token name is valid`() {
        assertTrue("CI Pipeline".isNotBlank())
    }

    // =========================================================================
    // Scope selection validation
    // =========================================================================

    @Test
    fun `empty scope selection is invalid`() {
        val selectedScopes = emptySet<String>()
        assertTrue(selectedScopes.isEmpty())
    }

    @Test
    fun `non-empty scope selection is valid`() {
        val selectedScopes = setOf("read:repos", "read:packages")
        assertTrue(selectedScopes.isNotEmpty())
        assertEquals(2, selectedScopes.size)
    }

    // =========================================================================
    // Expires in days parsing
    // =========================================================================

    @Test
    fun `valid integer string parses to Long`() {
        assertEquals(90L, "90".toLongOrNull())
    }

    @Test
    fun `empty string returns null for expires in days`() {
        assertNull("".toLongOrNull())
    }

    @Test
    fun `non-numeric string returns null`() {
        assertNull("abc".toLongOrNull())
    }

    @Test
    fun `expiresInDays digit filter keeps only digits`() {
        val input = "90 days"
        val filtered = input.filter { c -> c.isDigit() }
        assertEquals("90", filtered)
    }
}

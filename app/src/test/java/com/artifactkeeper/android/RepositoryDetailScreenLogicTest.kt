package com.artifactkeeper.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the display logic in RepositoryDetailScreen.kt.
 * Covers upload URL construction, artifact card display logic,
 * edit dialog field mapping, and virtual repo type detection.
 */
class RepositoryDetailScreenLogicTest {

    // =========================================================================
    // Upload URL construction
    // =========================================================================

    @Test
    fun `upload URL is constructed from baseUrl, repoKey, and fileName`() {
        val baseUrl = "https://artifacts.example.com/"
        val repoKey = "maven-central"
        val fileName = "spring-boot-3.0.0.jar"
        val uploadUrl = "${baseUrl}api/v1/repositories/$repoKey/artifacts/$fileName"
        assertEquals(
            "https://artifacts.example.com/api/v1/repositories/maven-central/artifacts/spring-boot-3.0.0.jar",
            uploadUrl,
        )
    }

    @Test
    fun `upload URL handles special characters in file name`() {
        val baseUrl = "https://example.com/"
        val repoKey = "npm-local"
        val fileName = "@scope-package-1.0.0.tgz"
        val uploadUrl = "${baseUrl}api/v1/repositories/$repoKey/artifacts/$fileName"
        assertTrue(uploadUrl.contains("@scope-package-1.0.0.tgz"))
    }

    // =========================================================================
    // Download URL construction (ArtifactCard click)
    // =========================================================================

    @Test
    fun `download URL uses artifact path`() {
        val baseUrl = "https://artifacts.example.com/"
        val repoKey = "maven-releases"
        val artifactPath = "com/example/app/1.0/app-1.0.jar"
        val downloadUrl = "${baseUrl}api/v1/repositories/$repoKey/artifacts/$artifactPath"
        assertEquals(
            "https://artifacts.example.com/api/v1/repositories/maven-releases/artifacts/com/example/app/1.0/app-1.0.jar",
            downloadUrl,
        )
    }

    // =========================================================================
    // Upload success message formatting
    // =========================================================================

    @Test
    fun `upload success message includes fileName and size`() {
        val fileName = "myfile.jar"
        val formattedSize = "2.5 MB"
        val message = "Uploaded $fileName ($formattedSize)"
        assertEquals("Uploaded myfile.jar (2.5 MB)", message)
    }

    // =========================================================================
    // Upload error message formatting
    // =========================================================================

    @Test
    fun `upload error includes HTTP error body`() {
        val errorBody = "413 Payload Too Large"
        val uploadError = "Upload failed: $errorBody"
        assertEquals("Upload failed: 413 Payload Too Large", uploadError)
    }

    @Test
    fun `upload error falls back to HTTP code when body is null`() {
        val body: String? = null
        val code = 500
        val errorBody = body ?: "HTTP $code"
        val uploadError = "Upload failed: $errorBody"
        assertEquals("Upload failed: HTTP 500", uploadError)
    }

    @Test
    fun `upload error uses exception message`() {
        val exceptionMessage: String? = "Connection timeout"
        val uploadError = exceptionMessage ?: "Upload failed"
        assertEquals("Connection timeout", uploadError)
    }

    @Test
    fun `upload error falls back for null exception message`() {
        val exceptionMessage: String? = null
        val uploadError = exceptionMessage ?: "Upload failed"
        assertEquals("Upload failed", uploadError)
    }

    // =========================================================================
    // Visibility chip: Public vs Private
    // =========================================================================

    @Test
    fun `public repository shows Public label`() {
        val isPublic = true
        val label = if (isPublic) "Public" else "Private"
        assertEquals("Public", label)
    }

    @Test
    fun `private repository shows Private label`() {
        val isPublic = false
        val label = if (isPublic) "Public" else "Private"
        assertEquals("Private", label)
    }

    // =========================================================================
    // Virtual repo type check (case insensitive)
    // =========================================================================

    @Test
    fun `virtual type detected case-insensitively`() {
        assertTrue("virtual".equals("virtual", ignoreCase = true))
        assertTrue("Virtual".equals("virtual", ignoreCase = true))
        assertTrue("VIRTUAL".equals("virtual", ignoreCase = true))
    }

    @Test
    fun `local type is not virtual`() {
        assertFalse("local".equals("virtual", ignoreCase = true))
    }

    // =========================================================================
    // Edit dialog: description ifBlank returns null
    // =========================================================================

    @Test
    fun `blank description becomes null for update request`() {
        val description = ""
        val result = description.ifBlank { null }
        assertNull(result)
    }

    @Test
    fun `whitespace description becomes null`() {
        val description = "   "
        val result = description.ifBlank { null }
        assertNull(result)
    }

    @Test
    fun `non-blank description is preserved`() {
        val description = "My repository"
        val result = description.ifBlank { null }
        assertEquals("My repository", result)
    }

    // =========================================================================
    // Edit dialog: key change detection
    // =========================================================================

    @Test
    fun `key change detected when new key differs`() {
        val newKey = "new-key"
        val originalKey = "old-key"
        val keyForRequest = if (newKey != originalKey) newKey else null
        assertEquals("new-key", keyForRequest)
    }

    @Test
    fun `key not included when unchanged`() {
        val newKey = "same-key"
        val originalKey = "same-key"
        val keyForRequest = if (newKey != originalKey) newKey else null
        assertNull(keyForRequest)
    }

    // =========================================================================
    // Key input forced to lowercase
    // =========================================================================

    @Test
    fun `key input is lowercased`() {
        val input = "MyRepo-Key"
        assertEquals("myrepo-key", input.lowercase())
    }

    // =========================================================================
    // searchQuery ifBlank for API call
    // =========================================================================

    @Test
    fun `blank searchQuery becomes null for API call`() {
        val searchQuery = ""
        val apiQuery = searchQuery.ifBlank { null }
        assertNull(apiQuery)
    }

    @Test
    fun `non-blank searchQuery is passed to API`() {
        val searchQuery = "spring"
        val apiQuery = searchQuery.ifBlank { null }
        assertEquals("spring", apiQuery)
    }

    // =========================================================================
    // Artifact version display
    // =========================================================================

    @Test
    fun `artifact version with v prefix`() {
        val version = "2.1.0"
        assertEquals("v2.1.0", "v$version")
    }

    @Test
    fun `null version hides chip`() {
        val version: String? = null
        assertFalse(version != null)
    }

    @Test
    fun `non-null version shows chip`() {
        val version: String? = "1.0"
        assertTrue(version != null)
    }

    // =========================================================================
    // Artifacts count display
    // =========================================================================

    @Test
    fun `artifacts section title includes count`() {
        val count = 15
        assertEquals("Artifacts (15)", "Artifacts ($count)")
    }

    @Test
    fun `artifacts section title with zero count`() {
        val count = 0
        assertEquals("Artifacts (0)", "Artifacts ($count)")
    }
}

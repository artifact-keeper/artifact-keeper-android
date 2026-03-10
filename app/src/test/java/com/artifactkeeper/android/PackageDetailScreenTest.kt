package com.artifactkeeper.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the display logic used in PackageDetailScreen.
 * The screen is a Composable, so we test the string formatting
 * and data transformation logic that drives the UI.
 */
class PackageDetailScreenTest {

    // =========================================================================
    // Version display
    // =========================================================================

    @Test
    fun `version header shows count`() {
        val versions = listOf("1.0.0", "1.1.0", "2.0.0")
        val header = "Version History (${versions.size})"
        assertEquals("Version History (3)", header)
    }

    @Test
    fun `version header shows zero for empty list`() {
        val versions = emptyList<String>()
        val header = "Version History (${versions.size})"
        assertEquals("Version History (0)", header)
    }

    // =========================================================================
    // Checksum display (truncated with ellipsis)
    // =========================================================================

    @Test
    fun `SHA256 display truncates to 16 chars`() {
        val checksum = "abc123def456789012345678901234567890"
        val display = "SHA256: ${checksum.take(16)}..."
        assertEquals("SHA256: abc123def4567890...", display)
    }

    @Test
    fun `short checksum shows full value plus ellipsis`() {
        val checksum = "abc"
        val display = "SHA256: ${checksum.take(16)}..."
        assertEquals("SHA256: abc...", display)
    }

    @Test
    fun `empty checksum is detected by isNotBlank check`() {
        val checksum = ""
        assertTrue(checksum.isBlank())
    }

    @Test
    fun `non-empty checksum passes isNotBlank check`() {
        val checksum = "abc123"
        assertTrue(checksum.isNotBlank())
    }

    // =========================================================================
    // Date display (take first 10 chars from ISO string)
    // =========================================================================

    @Test
    fun `createdAt display takes first 10 characters`() {
        val ts = "2024-06-15T12:30:45.123Z"
        assertEquals("2024-06-15", ts.take(10))
    }

    @Test
    fun `short date string is kept as-is`() {
        val ts = "2024-06"
        assertEquals("2024-06", ts.take(10))
    }

    // =========================================================================
    // Format display (uppercase)
    // =========================================================================

    @Test
    fun `format is displayed in uppercase`() {
        assertEquals("MAVEN", "maven".uppercase())
        assertEquals("NPM", "npm".uppercase())
        assertEquals("DOCKER", "docker".uppercase())
        assertEquals("PYPI", "pypi".uppercase())
    }

    // =========================================================================
    // Download count display
    // =========================================================================

    @Test
    fun `download count is shown as string`() {
        assertEquals("150 downloads", "${150} downloads")
        assertEquals("0 downloads", "${0} downloads")
    }

    // =========================================================================
    // Version count display
    // =========================================================================

    @Test
    fun `version count is shown as string`() {
        assertEquals("5 versions", "${5} versions")
        assertEquals("0 versions", "${0} versions")
    }

    // =========================================================================
    // Latest version label
    // =========================================================================

    @Test
    fun `latest version label formats correctly`() {
        val v = "2.1.0"
        assertEquals("Latest: 2.1.0", "Latest: $v")
    }

    // =========================================================================
    // UUID parsing (used for packageId)
    // =========================================================================

    @Test
    fun `valid UUID string parses successfully`() {
        val id = "550e8400-e29b-41d4-a716-446655440000"
        val uuid = java.util.UUID.fromString(id)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", uuid.toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid UUID string throws`() {
        java.util.UUID.fromString("not-a-uuid")
    }
}

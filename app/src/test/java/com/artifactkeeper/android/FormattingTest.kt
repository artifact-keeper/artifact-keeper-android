package com.artifactkeeper.android

import com.artifactkeeper.android.ui.util.formatBytes
import com.artifactkeeper.android.ui.util.formatDownloadCount
import com.artifactkeeper.android.ui.util.formatDuration
import com.artifactkeeper.android.ui.util.formatRelativeTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class FormattingTest {

    // =========================================================================
    // formatBytes
    // =========================================================================

    @Test
    fun `formatBytes returns bytes for values under 1024`() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("1 B", formatBytes(1))
        assertEquals("512 B", formatBytes(512))
        assertEquals("1023 B", formatBytes(1023))
    }

    @Test
    fun `formatBytes returns KB for values in kilobyte range`() {
        assertEquals("1.0 KB", formatBytes(1024))
        assertEquals("1.5 KB", formatBytes(1536))
        assertEquals("5.0 KB", formatBytes(5 * 1024))
    }

    @Test
    fun `formatBytes drops decimal for values 10 KB and above`() {
        assertEquals("10 KB", formatBytes(10 * 1024))
        assertEquals("100 KB", formatBytes(100 * 1024))
        assertEquals("999 KB", formatBytes(999 * 1024))
    }

    @Test
    fun `formatBytes returns MB for megabyte range`() {
        assertEquals("1.0 MB", formatBytes(1024L * 1024))
        assertEquals("5.0 MB", formatBytes(5L * 1024 * 1024))
        assertEquals("512 MB", formatBytes(512L * 1024 * 1024))
    }

    @Test
    fun `formatBytes returns GB for gigabyte range`() {
        assertEquals("1.0 GB", formatBytes(1024L * 1024 * 1024))
        assertEquals("2.5 GB", formatBytes((2.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `formatBytes returns TB for terabyte range`() {
        assertEquals("1.0 TB", formatBytes(1024L * 1024 * 1024 * 1024))
        assertEquals("5.0 TB", formatBytes(5L * 1024 * 1024 * 1024 * 1024))
    }

    @Test
    fun `formatBytes handles very large TB values`() {
        // 100 TB should drop the decimal
        assertEquals("100 TB", formatBytes(100L * 1024 * 1024 * 1024 * 1024))
    }

    // =========================================================================
    // formatDuration
    // =========================================================================

    @Test
    fun `formatDuration returns seconds only for short durations`() {
        assertEquals("0s", formatDuration(0))
        assertEquals("0s", formatDuration(999))
        assertEquals("1s", formatDuration(1000))
        assertEquals("59s", formatDuration(59_000))
    }

    @Test
    fun `formatDuration returns minutes and seconds`() {
        assertEquals("1m 0s", formatDuration(60_000))
        assertEquals("1m 30s", formatDuration(90_000))
        assertEquals("5m 15s", formatDuration(315_000))
    }

    @Test
    fun `formatDuration returns hours minutes and seconds`() {
        assertEquals("1h 0m 0s", formatDuration(3_600_000))
        assertEquals("2h 30m 45s", formatDuration(9_045_000))
    }

    // =========================================================================
    // formatRelativeTime (ISO string overload)
    // =========================================================================

    @Test
    fun `formatRelativeTime returns just now for recent timestamps`() {
        val now = Instant.now().toString()
        assertEquals("just now", formatRelativeTime(now))
    }

    @Test
    fun `formatRelativeTime returns minutes ago`() {
        val fiveMinAgo = Instant.now().minus(5, ChronoUnit.MINUTES).toString()
        assertEquals("5m ago", formatRelativeTime(fiveMinAgo))
    }

    @Test
    fun `formatRelativeTime returns hours ago`() {
        val threeHoursAgo = Instant.now().minus(3, ChronoUnit.HOURS).toString()
        assertEquals("3h ago", formatRelativeTime(threeHoursAgo))
    }

    @Test
    fun `formatRelativeTime returns days ago`() {
        val twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS).toString()
        assertEquals("2d ago", formatRelativeTime(twoDaysAgo))
    }

    @Test
    fun `formatRelativeTime returns formatted date for old timestamps`() {
        val oldDate = Instant.parse("2023-01-15T10:00:00Z")
        val result = formatRelativeTime(oldDate.toString())
        // Should contain "Jan 15, 2023" (depending on system timezone, exact day may vary)
        assertTrue(
            "Expected formatted date containing 'Jan' and '2023', got: $result",
            result.contains("2023") && result.contains("Jan")
        )
    }

    @Test
    fun `formatRelativeTime returns original string for invalid input`() {
        assertEquals("not-a-date", formatRelativeTime("not-a-date"))
        assertEquals("", formatRelativeTime(""))
    }

    // =========================================================================
    // formatRelativeTime (OffsetDateTime overload)
    // =========================================================================

    @Test
    fun `formatRelativeTime with OffsetDateTime returns just now`() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        assertEquals("just now", formatRelativeTime(now))
    }

    @Test
    fun `formatRelativeTime with OffsetDateTime returns minutes ago`() {
        val tenMinAgo = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10)
        assertEquals("10m ago", formatRelativeTime(tenMinAgo))
    }

    // =========================================================================
    // formatDownloadCount
    // =========================================================================

    @Test
    fun `formatDownloadCount returns raw number below 1000`() {
        assertEquals("0", formatDownloadCount(0))
        assertEquals("1", formatDownloadCount(1))
        assertEquals("999", formatDownloadCount(999))
    }

    @Test
    fun `formatDownloadCount returns K suffix for thousands`() {
        assertEquals("1.0K", formatDownloadCount(1_000))
        assertEquals("1.5K", formatDownloadCount(1_500))
        assertEquals("999.9K", formatDownloadCount(999_900))
    }

    @Test
    fun `formatDownloadCount returns M suffix for millions`() {
        assertEquals("1.0M", formatDownloadCount(1_000_000))
        assertEquals("2.5M", formatDownloadCount(2_500_000))
        assertEquals("100.0M", formatDownloadCount(100_000_000))
    }
}

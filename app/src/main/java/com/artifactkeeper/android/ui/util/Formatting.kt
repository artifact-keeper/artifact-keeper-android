package com.artifactkeeper.android.ui.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    for (unit in units) {
        value /= 1024.0
        if (value < 1024 || unit == "TB") {
            return if (value < 10) "%.1f %s".format(value, unit)
            else "%.0f %s".format(value, unit)
        }
    }
    return "$bytes B"
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return buildString {
        if (hours > 0) append("${hours}h ")
        if (hours > 0 || minutes > 0) append("${minutes}m ")
        append("${seconds}s")
    }.trim()
}

fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        formatRelativeTimeFromInstant(instant)
    } catch (_: Exception) {
        isoTimestamp
    }
}

fun formatRelativeTime(dateTime: OffsetDateTime): String {
    return formatRelativeTimeFromInstant(dateTime.toInstant())
}

private fun formatRelativeTimeFromInstant(instant: Instant): String {
    val now = Instant.now()
    val minutesAgo = ChronoUnit.MINUTES.between(instant, now)
    val hoursAgo = ChronoUnit.HOURS.between(instant, now)
    val daysAgo = ChronoUnit.DAYS.between(instant, now)

    return when {
        minutesAgo < 1 -> "just now"
        minutesAgo < 60 -> "${minutesAgo}m ago"
        hoursAgo < 24 -> "${hoursAgo}h ago"
        daysAgo < 7 -> "${daysAgo}d ago"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        }
    }
}

fun formatDownloadCount(count: Long): String {
    return when {
        count < 1_000 -> "$count"
        count < 1_000_000 -> "%.1fK".format(count / 1_000.0)
        else -> "%.1fM".format(count / 1_000_000.0)
    }
}

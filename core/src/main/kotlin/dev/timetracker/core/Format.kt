package dev.timetracker.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

/** `12s`, `35m`, `3h 10m`; truncates, clamps negatives to `0s`. */
fun formatDuration(ms: Long): String {
    val totalSeconds = maxOf(ms, 0L) / 1000
    val totalMinutes = totalSeconds / 60
    return when {
        totalSeconds < 60 -> "${totalSeconds}s"
        totalMinutes < 60 -> "${totalMinutes}m"
        else -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
    }
}

/** A live counter: [formatDuration] plus two-digit seconds once past a minute, e.g. `12m 08s`, `1h 5m 03s`. */
fun formatElapsed(ms: Long): String {
    val totalSeconds = maxOf(ms, 0L) / 1000
    return if (totalSeconds < 60) formatDuration(ms) else "${formatDuration(ms)} ${(totalSeconds % 60).toString().padStart(2, '0')}s"
}

fun formatTime(epochMs: Long, zone: ZoneId): String =
    TIME_FORMAT.format(Instant.ofEpochMilli(epochMs).atZone(zone))

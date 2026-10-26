package dev.timetracker.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Allowed new timestamps when moving [pointId]: `prev.at..next.at` (inclusive).
 * The first point has no lower bound; the last point is bounded above by [now].
 * Throws [NoSuchElementException] if [pointId] is not in [points].
 */
fun moveBounds(points: List<Point>, pointId: String, now: Long): LongRange {
    val sorted = points.sortedWith(pointOrder)
    val i = sorted.indexOfFirst { it.id == pointId }
    if (i < 0) throw NoSuchElementException("No point with id $pointId")
    val lower = sorted.getOrNull(i - 1)?.at ?: Long.MIN_VALUE
    val upper = sorted.getOrNull(i + 1)?.at ?: now
    return lower..upper
}

/** Timestamp at which a new point splits [block] into two halves. */
fun midpoint(block: Block): Long = block.start.at + block.durationMs / 2

/**
 * Resolves a picked [hour]:[minute] to a timestamp inside [bounds]. Tries the local date of [originalAt]
 * first, then whichever of the day before / day after is nearer to [originalAt], then the other one.
 * Returns null when no candidate fits (including empty [bounds]); the caller must not clamp.
 */
fun resolvePickedTime(originalAt: Long, hour: Int, minute: Int, bounds: LongRange, zone: ZoneId): Long? {
    if (bounds.isEmpty()) return null
    val date = Instant.ofEpochMilli(originalAt).atZone(zone).toLocalDate()
    fun on(d: LocalDate) = d.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
    val others = listOf(on(date.minusDays(1)), on(date.plusDays(1))).sortedBy { abs(it - originalAt) }
    return (listOf(on(date)) + others).firstOrNull { it in bounds }
}

package dev.timetracker.core

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

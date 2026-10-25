package dev.timetracker.core

/** Orders points by time, then by creation time, then by id, so equal timestamps are deterministic. */
val pointOrder: Comparator<Point> = compareBy<Point>({ it.at }, { it.createdAt }, { it.id })

/** Derives blocks from points. The last point opens the current block, which ends at [now]. Does not mutate [points]. */
fun deriveBlocks(points: List<Point>, now: Long): List<Block> {
    val sorted = points.sortedWith(pointOrder)
    return sorted.mapIndexed { i, start ->
        val end = sorted.getOrNull(i + 1)
        if (end == null) Block(start, null, now) else Block(start, end, end.at)
    }
}

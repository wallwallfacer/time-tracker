package dev.timetracker.core

/**
 * The newest candidate strictly before [now]: the start of the block a press at [now] closes.
 * Candidates at or after [now] (the press itself, the phone echoing it back, clock skew) are ignored.
 */
fun previousPoint(now: Long, vararg candidates: Long?): Long? = candidates.filterNotNull().filter { it < now }.maxOrNull()

package dev.timetracker.core

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class EditsTest {
    private fun p(id: String, at: Long) = Point(id, at, Source.WATCH, null, null, at)
    private val pts = listOf(p("a", 100), p("b", 200), p("c", 300))

    @Test fun `middle point bounded by neighbours inclusive`() = assertEquals(100L..300L, moveBounds(pts, "b", 1000))
    @Test fun `first point has no lower bound`() = assertEquals(Long.MIN_VALUE, moveBounds(pts, "a", 1000).first)
    @Test fun `first point upper bound is next point`() = assertEquals(200L, moveBounds(pts, "a", 1000).last)
    @Test fun `move_bounds_for_last_point_end_at_now`() = assertEquals(200L..1000L, moveBounds(pts, "c", 1000))
    @Test fun `bounds ignore input order`() = assertEquals(100L..300L, moveBounds(pts.reversed(), "b", 1000))
    @Test fun `unknown id throws`() {
        assertThrows(NoSuchElementException::class.java) { moveBounds(pts, "zzz", 1000) }
    }
    @Test fun `midpoint of block`() = assertEquals(150L, midpoint(Block(pts[0], pts[1], 200)))

    private val utc: ZoneId = ZoneOffset.UTC
    private fun at(day: Int, h: Int, m: Int) = LocalDateTime.of(2026, 10, day, h, m).atZone(utc).toInstant().toEpochMilli()

    @Test fun `picked time fits on same day`() =
        assertEquals(at(25, 10, 30), resolvePickedTime(at(25, 10, 0), 10, 30, at(25, 9, 0)..at(25, 11, 0), utc))

    @Test fun `picked time across midnight resolves to next day`() =
        assertEquals(at(26, 0, 10), resolvePickedTime(at(25, 23, 50), 0, 10, at(25, 22, 0)..at(26, 7, 0), utc))

    @Test fun `picked time across midnight resolves to previous day`() =
        assertEquals(at(25, 23, 50), resolvePickedTime(at(26, 0, 10), 23, 50, at(25, 22, 0)..at(26, 7, 0), utc))

    @Test fun `picked time outside bounds is null`() =
        assertNull(resolvePickedTime(at(25, 10, 0), 12, 0, at(25, 9, 0)..at(25, 11, 0), utc))

    @Test fun `empty bounds is null`() =
        assertNull(resolvePickedTime(at(25, 10, 0), 10, 0, at(25, 11, 0)..at(25, 9, 0), utc))
}

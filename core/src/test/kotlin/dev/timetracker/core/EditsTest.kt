package dev.timetracker.core

import org.junit.Assert.assertEquals
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
}

package dev.timetracker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DaysTest {
    private val zone = ZoneId.of("Asia/Singapore")
    private fun ms(day: Int, hour: Int) =
        ZonedDateTime.of(2026, 10, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli()

    private val day1Start = ms(25, 0)
    private val day2Start = ms(26, 0)
    private val day3Start = ms(27, 0)
    private val day4Start = ms(28, 0)

    private val sleepStart = Point("s", ms(25, 23), Source.WATCH, "sleep", null, 0)
    private val sleepEnd = Point("e", ms(26, 1), Source.WATCH, null, null, 0)
    private val block = Block(sleepStart, sleepEnd, sleepEnd.at)

    @Test fun `block_spanning_midnight_appears_clipped_on_both_days`() {
        val d1 = clipToDay(listOf(block), day1Start, day2Start); val d2 = clipToDay(listOf(block), day2Start, day3Start)
        assertEquals(sleepStart.at, d1.single().visibleStart); assertEquals(day2Start, d1.single().visibleEnd)
        assertEquals(day2Start, d2.single().visibleStart); assertEquals(sleepEnd.at, d2.single().visibleEnd)
        assertEquals("sleep", d2.single().block.start.tagId)
    }
    @Test fun `block outside day is excluded`() = assertTrue(clipToDay(listOf(block), day3Start, day4Start).isEmpty())
    @Test fun `block ending exactly at day start is excluded`() {
        val b = Block(sleepStart, sleepEnd, day2Start)
        assertTrue(clipToDay(listOf(b), day2Start, day3Start).isEmpty())
    }
    @Test fun `block starting exactly at day end is excluded`() {
        val b = Block(Point("x", day2Start, Source.WATCH, null, null, 0), null, ms(26, 5))
        assertTrue(clipToDay(listOf(b), day1Start, day2Start).isEmpty())
    }
}

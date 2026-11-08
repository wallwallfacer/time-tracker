package dev.timetracker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class OverviewTest {
    private val zone = ZoneId.of("Asia/Singapore")
    private val hour = 3_600_000L
    private fun ms(month: Int, day: Int, hour: Int, min: Int = 0) =
        ZonedDateTime.of(2026, month, day, hour, min, 0, 0, zone).toInstant().toEpochMilli()
    private fun date(month: Int, day: Int) = LocalDate.of(2026, month, day)
    private fun p(id: String, at: Long, tag: String?) = Point(id, at, Source.WATCH, tag, null, at)

    @Test fun `week starts monday`() {
        assertEquals(date(10, 19), periodStart(ViewMode.WEEK, date(10, 22)))
        assertEquals(date(10, 26), periodEnd(ViewMode.WEEK, date(10, 22)))
        assertEquals(date(10, 19), periodStart(ViewMode.WEEK, date(10, 19)))
    }

    @Test fun `month bounds`() {
        assertEquals(date(10, 1), periodStart(ViewMode.MONTH, date(10, 22)))
        assertEquals(date(11, 1), periodEnd(ViewMode.MONTH, date(10, 22)))
    }

    @Test fun `step month clamps day`() {
        assertEquals(date(2, 28), stepAnchor(ViewMode.MONTH, date(3, 31), -1))
        assertEquals(date(10, 29), stepAnchor(ViewMode.WEEK, date(10, 22), 1))
        assertEquals(date(10, 21), stepAnchor(ViewMode.DAY, date(10, 22), -1))
    }

    @Test fun `totals split block crossing period start`() {
        val points = listOf(p("s", ms(10, 18, 22), "sleep"), p("w", ms(10, 19, 8), "work"), p("u", ms(10, 19, 12), null))
        val o = overview(points, ViewMode.WEEK, date(10, 19), zone, ms(10, 19, 14))
        assertEquals(8 * hour, o.totals["sleep"])
        assertEquals(4 * hour, o.totals["work"])
        assertEquals(2 * hour, o.totals[null])
        assertEquals(14 * hour, o.elapsedMs)
        assertEquals(85, o.labeledPercent)
    }

    @Test fun `span starts at first point`() {
        val o = overview(listOf(p("w", ms(10, 21, 9), "work")), ViewMode.WEEK, date(10, 21), zone, ms(10, 21, 17))
        assertEquals(8 * hour, o.elapsedMs)
        assertEquals(100, o.labeledPercent)
        assertNull(o.previous)
    }

    @Test fun `previous same span week`() {
        val points = listOf(p("a1", ms(10, 12, 0), "a"), p("b", ms(10, 14, 10), "b"), p("a2", ms(10, 19, 0), "a"))
        val o = overview(points, ViewMode.WEEK, date(10, 21), zone, ms(10, 21, 10))
        assertEquals(58 * hour, o.totals["a"])
        assertEquals(mapOf<String?, Long>("a" to 58 * hour), o.previous)
    }

    @Test fun `previous same span clamps to previous month end`() {
        val o = overview(listOf(p("a", ms(9, 1, 0), "a")), ViewMode.MONTH, date(10, 31), zone, ms(10, 31, 14, 44))
        assertEquals(mapOf<String?, Long>("a" to 30 * 24 * hour), o.previous)
    }

    @Test fun `past period compares whole previous period`() {
        val o = overview(listOf(p("a", ms(9, 1, 0), "a")), ViewMode.WEEK, date(10, 5), zone, ms(10, 21, 0))
        assertEquals(7 * 24 * hour, o.elapsedMs)
        assertEquals(mapOf<String?, Long>("a" to 7 * 24 * hour), o.previous)
    }

    @Test fun `label target skips current block`() {
        val gap = listOf(p("a", ms(10, 19, 0), "a"), p("g", ms(10, 20, 9), null), p("a2", ms(10, 20, 10), "a"), p("c", ms(10, 21, 9), null))
        assertEquals(date(10, 20), overview(gap, ViewMode.WEEK, date(10, 21), zone, ms(10, 21, 10)).labelTarget)
        val onlyCurrent = listOf(p("a", ms(10, 19, 0), "a"), p("c", ms(10, 21, 9), null))
        assertEquals(date(10, 21), overview(onlyCurrent, ViewMode.WEEK, date(10, 21), zone, ms(10, 21, 10)).labelTarget)
        val labeled = listOf(p("a", ms(10, 19, 0), "a"))
        assertNull(overview(labeled, ViewMode.WEEK, date(10, 21), zone, ms(10, 21, 10)).labelTarget)
    }

    @Test fun `overview day columns use real day length across DST`() {
        val berlin = ZoneId.of("Europe/Berlin")
        val o = overview(emptyList(), ViewMode.WEEK, date(10, 25), berlin, ms(10, 27, 0))
        assertEquals(7, o.days.size)
        val sunday = o.days.single { it.date == date(10, 25) }
        assertEquals(25 * hour, sunday.end - sunday.start)
    }

    @Test fun `future days are empty columns`() {
        val o = overview(listOf(p("a", ms(10, 1, 0), "a")), ViewMode.MONTH, date(10, 26), zone, ms(10, 26, 12))
        assertEquals(31, o.days.size)
        assertTrue(o.days[30].blocks.isEmpty())
        assertFalse(o.days[25].blocks.isEmpty())
    }
}

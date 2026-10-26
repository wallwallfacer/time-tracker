package dev.timetracker.core

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun `durations`() {
        assertEquals("0s", formatDuration(0))
        assertEquals("12s", formatDuration(12_000))
        assertEquals("35m", formatDuration(35 * 60_000L))
        assertEquals("3h 10m", formatDuration(190 * 60_000L))
    }

    @Test fun `negative clamps to zero`() = assertEquals("0s", formatDuration(-5000))

    @Test fun `whole hours keep zero minutes`() = assertEquals("3h 0m", formatDuration(3 * 3_600_000L))

    @Test fun `truncates never rounds`() {
        assertEquals("59s", formatDuration(59_999))
        assertEquals("59m", formatDuration(3_599_999))
    }

    @Test fun `formatTime uses zone`() {
        // 2026-10-25T06:32:00Z is 14:32 in Singapore (UTC+8)
        assertEquals("14:32", formatTime(1_792_909_920_000L, ZoneId.of("Asia/Singapore")))
    }
}

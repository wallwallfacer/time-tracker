package dev.timetracker.phone.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class RailSizeTest {
    private fun min(m: Long) = m * 60_000

    @Test fun `height follows duration between floor and cap`() {
        assertEquals(RailSize(60.dp, false), railSize(min(50), isCurrent = false))
        assertEquals(RailSize(72.dp, false), railSize(min(60), isCurrent = false))
    }

    @Test fun `short blocks keep a tappable floor`() {
        assertEquals(RailSize(48.dp, false), railSize(12_000, isCurrent = false))
        assertEquals(RailSize(64.dp, false), railSize(min(12), isCurrent = true))
    }

    @Test fun `long blocks are capped and marked compressed`() {
        assertEquals(RailSize(140.dp, true), railSize(min(190), isCurrent = false))
        // 116m 40s is exactly 140dp: at the cap, not over it.
        assertEquals(RailSize(140.dp, false), railSize(min(116) + 40_000, isCurrent = false))
    }
}

package dev.timetracker.phone.ui.themes

import dev.timetracker.phone.ui.AppThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuskThemeTest {
    @Test fun registeredAsDarkOnlyDusk() {
        val t = AppThemes.byId("dusk")
        assertEquals("Dusk", t.name)
        assertTrue(t.darkOnly)
    }
}

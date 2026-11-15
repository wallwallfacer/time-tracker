package dev.timetracker.phone.ui.themes

import dev.timetracker.phone.ui.AppThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialThemeTest {
    @Test fun `dial is registered with its id and name`() {
        val t = AppThemes.byId("dial")
        assertEquals("Dial", t.name)
        assertTrue(!t.darkOnly)
        assertNotNull(t.dayHeader)
    }
}

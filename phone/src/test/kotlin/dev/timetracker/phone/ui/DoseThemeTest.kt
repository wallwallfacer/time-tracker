package dev.timetracker.phone.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DoseThemeTest {
    @Test fun isRegistered() {
        val t = AppThemes.byId("dose")
        assertEquals("dose", t.id)
        assertEquals("Dose", t.name)
        assertFalse(t.darkOnly)
    }
}

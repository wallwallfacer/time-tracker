package dev.timetracker.phone.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {
    @Test fun railIsTheDefault() = assertEquals("rail", AppThemes.byId(null).id)

    @Test fun unknownIdFallsBackToRail() = assertEquals("rail", AppThemes.byId("nope").id)

    @Test fun idsAreUnique() = assertEquals(AppThemes.all.size, AppThemes.all.map { it.id }.toSet().size)

    @Test fun systemModeFollowsTheSystem() {
        assertTrue(resolveDark(ThemeMode.SYSTEM, RailTheme, systemDark = true))
        assertFalse(resolveDark(ThemeMode.SYSTEM, RailTheme, systemDark = false))
    }

    @Test fun explicitModeBeatsTheSystem() {
        assertFalse(resolveDark(ThemeMode.LIGHT, RailTheme, systemDark = true))
        assertTrue(resolveDark(ThemeMode.DARK, RailTheme, systemDark = false))
    }

    @Test fun darkOnlyThemeIgnoresLightMode() {
        val dusk = RailTheme.copy(darkOnly = true)
        assertTrue(resolveDark(ThemeMode.LIGHT, dusk, systemDark = false))
        assertTrue(resolveDark(ThemeMode.SYSTEM, dusk, systemDark = false))
    }
}

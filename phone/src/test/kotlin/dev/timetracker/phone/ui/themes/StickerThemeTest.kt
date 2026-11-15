package dev.timetracker.phone.ui.themes

import dev.timetracker.phone.ui.AppThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class StickerThemeTest {
    @Test fun `sticker is registered with its id and name`() {
        assertSame(StickerTheme, AppThemes.byId("sticker"))
        assertEquals("Sticker", StickerTheme.name)
        assertFalse(StickerTheme.darkOnly)
    }
}

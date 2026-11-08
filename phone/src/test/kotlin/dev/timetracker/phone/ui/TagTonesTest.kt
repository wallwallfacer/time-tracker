package dev.timetracker.phone.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.timetracker.phone.data.TagPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagTonesTest {
    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }
    private val lightSurface = Color(0xFFFBF8FF)
    private val darkSurface = Color(0xFF121318)

    @Test fun `palette has sixteen distinct colors`() = assertEquals(16, TagPalette.colors.toSet().size)

    @Test fun `every palette color has tuned tones in both themes`() {
        TagPalette.colors.forEach {
            assertTrue("light ${Integer.toHexString(it)}", it in LightTones)
            assertTrue("dark ${Integer.toHexString(it)}", it in DarkTones)
        }
    }

    @Test fun `rails clear 3 to 1 on the surface and card text clears 4_5 to 1`() {
        for ((tones, surface) in listOf(LightTones to lightSurface, DarkTones to darkSurface)) {
            tones.forEach { (color, t) ->
                val hex = Integer.toHexString(color)
                assertTrue("rail $hex ${contrast(t.rail, surface)}", contrast(t.rail, surface) >= 3f)
                assertTrue("text $hex ${contrast(t.onContainer, t.container)}", contrast(t.onContainer, t.container) >= 4.5f)
            }
        }
    }
}

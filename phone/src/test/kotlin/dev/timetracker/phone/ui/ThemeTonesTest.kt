package dev.timetracker.phone.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.timetracker.phone.data.TagPalette
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every built-in theme, in both modes, must keep the 32 tag colours readable and apart. */
class ThemeTonesTest {
    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    private fun oklab(c: Color): FloatArray {
        fun lin(v: Float) = if (v <= 0.04045f) v / 12.92f else Math.pow((v + 0.055) / 1.055, 2.4).toFloat()
        val r = lin(c.red); val g = lin(c.green); val b = lin(c.blue)
        val l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return floatArrayOf(
            (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s).toFloat(),
            (1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s).toFloat(),
            (0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s).toFloat(),
        )
    }

    private fun distance(a: Color, b: Color) = oklab(a).zip(oklab(b)) { x, y -> (x - y) * (x - y) }.sum().let { Math.sqrt(it.toDouble()) }

    private val cases = AppThemes.all.flatMap { t -> listOf(false, true).map { dark -> Triple(t, dark, if (dark) t.dark else t.light) } }
        .filter { (t, dark, _) -> !t.darkOnly || dark }

    @Test fun `card text clears 4_5 to 1 on its container for every palette color`() {
        for ((theme, dark, scheme) in cases) for (color in TagPalette.colors) {
            val t = theme.tones(color, scheme, dark)
            val c = contrast(t.onContainer, t.container)
            assertTrue("${theme.id} dark=$dark ${Integer.toHexString(color)}: $c", c >= 4.5f)
        }
    }

    @Test fun `every pair of rails stays apart in every theme`() {
        for ((theme, dark, scheme) in cases) {
            val rails = TagPalette.colors.map { theme.tones(it, scheme, dark).rail }
            for (i in rails.indices) for (j in i + 1 until rails.size) {
                val d = distance(rails[i], rails[j])
                assertTrue("${theme.id} dark=$dark ${Integer.toHexString(TagPalette.colors[i])} vs ${Integer.toHexString(TagPalette.colors[j])}: $d", d >= 0.04)
            }
        }
    }
}

package dev.timetracker.phone.ui.themes

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.timetracker.phone.R

/** Bundled OFL faces for the built-in themes (licences in docs/fonts). Rail keeps Google Sans Flex from Theme.kt. */
object Fonts {
    private val weights = listOf(400, 500, 600, 700, 800)

    /** A variable font: one file, every weight drawn from its wght axis (API 26+, our minSdk). */
    @OptIn(ExperimentalTextApi::class)
    private fun variable(res: Int) = FontFamily(
        weights.map { w -> Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) },
    )

    val SchibstedGrotesk = variable(R.font.schibsted_grotesk_vf)
    val WixMadeforDisplay = variable(R.font.wix_madefor_display_vf)
    val WixMadeforText = variable(R.font.wix_madefor_text_vf)
    val SofiaSans = variable(R.font.sofia_sans_vf)

    // Static Regular and Bold only: Medium and SemiBold fall back to the nearest of the two.
    val InstrumentSans = FontFamily(
        Font(R.font.instrument_sans_regular, FontWeight.Normal),
        Font(R.font.instrument_sans_regular, FontWeight.Medium),
        Font(R.font.instrument_sans_bold, FontWeight.SemiBold),
        Font(R.font.instrument_sans_bold, FontWeight.Bold),
    )
    val BricolageGrotesque = FontFamily(
        Font(R.font.bricolage_grotesque_regular, FontWeight.Normal),
        Font(R.font.bricolage_grotesque_regular, FontWeight.Medium),
        Font(R.font.bricolage_grotesque_bold, FontWeight.SemiBold),
        Font(R.font.bricolage_grotesque_bold, FontWeight.Bold),
    )
    val DMMono = FontFamily(
        Font(R.font.dm_mono_regular, FontWeight.Normal),
        Font(R.font.dm_mono_medium, FontWeight.Medium),
    )
    val BowlbyOneSC = FontFamily(Font(R.font.bowlby_one_sc, FontWeight.Normal))
}

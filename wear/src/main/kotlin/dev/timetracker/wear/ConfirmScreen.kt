package dev.timetracker.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import dev.timetracker.core.formatDuration
import dev.timetracker.core.formatTime
import java.time.ZoneId

private val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex_500, FontWeight.Medium),
    Font(R.font.google_sans_flex_600, FontWeight.SemiBold),
)
private val Ink = Color(0xFFE4E1E9)
private val Muted = Color(0xFFC7C5D0)

/** [recorded] false means the write failed: say so and skip the previous-block line. */
@Composable
fun ConfirmScreen(now: Long, prev: Long?, recorded: Boolean) {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                formatTime(now, ZoneId.systemDefault()),
                color = Ink,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Medium,
                fontSize = 52.sp,
                letterSpacing = (-0.02).em,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
            // Success uses the phone's primary-container pill; failure the error container.
            val (bg, ink) = if (recorded) Color(0xFF293CA0) to Color(0xFFDEE0FF) else Color(0xFF93000A) to Color(0xFFFFDAD6)
            Row(
                Modifier.padding(top = 10.dp).height(28.dp).clip(CircleShape).background(bg).padding(start = 10.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(ink))
                Text(
                    stringResource(if (recorded) R.string.recorded else R.string.not_recorded),
                    color = ink, fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                )
            }
            if (recorded && prev != null) {
                val duration = formatDuration(now - prev)
                val line = stringResource(R.string.previous_block, duration)
                Text(
                    buildAnnotatedString {
                        append(line.removeSuffix(duration))
                        withStyle(SpanStyle(color = Ink, fontWeight = FontWeight.SemiBold)) { append(duration) }
                    },
                    Modifier.padding(top = 8.dp),
                    color = Muted, fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                )
            }
        }
    }
}

package dev.timetracker.phone.capture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.timetracker.phone.R
import dev.timetracker.phone.ui.TimeTrackerTheme
import dev.timetracker.phone.ui.tabular

/** What the overlay shows; [previousBlock] is null when there was no earlier point. */
data class OverlayState(val time: String, val previousBlock: String?)

private val Ink = Color(0xFFE4E1E9)
private val Accent = Color(0xFFBAC3FF)
private val PillBg = Color(0xFF293CA0)
private val PillInk = Color(0xFFDEE0FF)

/** Dark over any wallpaper: a point drops onto a rail, then the time, "Recorded" and the block it closed. */
@Composable
fun ConfirmOverlay(state: OverlayState?) {
    if (state == null) return
    TimeTrackerTheme(dark = true) {
        val type = MaterialTheme.typography
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xCC0E0F18)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.width(26.dp).height(132.dp).drawBehind {
                    val w = 10.dp.toPx()
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color.Transparent, Accent), 0f, size.height - 13.dp.toPx()),
                        Offset((size.width - w) / 2, 0f), Size(w, size.height - 13.dp.toPx()), CornerRadius(w / 2),
                    )
                    val dot = Offset(size.width / 2, size.height - 13.dp.toPx())
                    drawCircle(Accent.copy(alpha = 0.28f), 19.dp.toPx(), dot)
                    drawCircle(Ink, 13.dp.toPx(), dot)
                },
            )
            Text(state.time, Modifier.padding(top = 28.dp), style = type.displayLarge.tabular, color = Ink)
            Row(
                Modifier.padding(top = 18.dp).height(44.dp).clip(CircleShape).background(PillBg).padding(start = 16.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(PillInk))
                Text(stringResource(R.string.recorded), style = type.titleMedium, color = PillInk)
            }
            state.previousBlock?.let { prev ->
                // "Previous block 1h 47m" with the duration emphasised.
                val line = stringResource(R.string.previous_block, prev)
                Text(
                    buildAnnotatedString {
                        append(line.removeSuffix(prev))
                        withStyle(SpanStyle(color = Ink, fontWeight = FontWeight.SemiBold)) { append(prev) }
                    },
                    Modifier.padding(top = 14.dp),
                    style = type.bodyLarge,
                    color = Color(0xFFC7C5D0),
                )
            }
            DrainBar(state, Modifier.padding(top = 40.dp))
        }
    }
}

/** Empties over the 2 s the overlay stays up; a new press ([key]) refills it, matching the restarted finish timer. */
@Composable
private fun DrainBar(key: OverlayState, modifier: Modifier) {
    val left = remember { Animatable(1f) }
    LaunchedEffect(key) {
        left.snapTo(1f)
        left.animateTo(0f, tween(2000, easing = LinearEasing))
    }
    Box(modifier.width(64.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.18f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(left.value).background(Accent))
    }
}

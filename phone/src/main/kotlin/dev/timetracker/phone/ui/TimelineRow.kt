package dev.timetracker.phone.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.timetracker.core.formatDuration
import dev.timetracker.core.formatTime
import dev.timetracker.phone.R
import java.time.ZoneId

/** One block as seen on the selected day: visible start time, label chip and visible duration. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineRowItem(row: TimelineRow, zone: ZoneId, onClick: () -> Unit, onLongClick: () -> Unit) {
    val visible = row.block
    val duration = formatDuration(visible.visibleEnd - visible.visibleStart)
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(formatTime(visible.visibleStart, zone), style = MaterialTheme.typography.titleMedium)
        TagChip(row.tag?.name, row.tag?.let { Color(it.color) })
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (visible.block.isCurrent) {
                Text(stringResource(R.string.in_progress, duration), color = MaterialTheme.colorScheme.primary)
            } else {
                Text(duration)
            }
        }
    }
}

@Composable
private fun TagChip(name: String?, color: Color?) {
    val label = MaterialTheme.typography.labelLarge
    val padding = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    if (name == null || color == null) {
        val outline = MaterialTheme.colorScheme.outline
        Text(
            stringResource(R.string.unlabeled),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = label,
            modifier = Modifier
                .drawBehind {
                    drawRoundRect(
                        outline,
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                    )
                }
                .then(padding),
        )
    } else {
        Text(
            name,
            color = color,
            style = label,
            modifier = Modifier
                .drawBehind { drawRoundRect(color.copy(alpha = 0.2f), cornerRadius = CornerRadius(8.dp.toPx())) }
                .then(padding),
        )
    }
}

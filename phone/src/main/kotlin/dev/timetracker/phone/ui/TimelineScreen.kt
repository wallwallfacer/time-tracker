package dev.timetracker.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.timetracker.core.Block
import dev.timetracker.phone.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val OTHER_DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
private val SHORT_DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

@Composable
fun TimelineScreen(
    state: TimelineState,
    zone: ZoneId,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onMark: () -> Unit,
    onRowClick: (Block) -> Unit,
    onDeleteRow: (Block) -> Unit,
    onSettings: () -> Unit,
    snackbar: SnackbarHostState,
) {
    Scaffold(
        // Scaffold lifts the undo snackbar above the centered FAB instead of covering it.
        snackbarHost = {
            SnackbarHost(snackbar) {
                Snackbar(it, shape = RoundedCornerShape(16.dp), actionColor = MaterialTheme.colorScheme.inversePrimary)
            }
        },
        floatingActionButton = { MarkButton(onMark) },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DayHeader(state.day, zone, onPrev, onNext, onToday, onSettings)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.isEmpty) {
                    Text(
                        stringResource(R.string.empty_hint),
                        Modifier.align(Alignment.Center).padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    // Bottom padding keeps the last row clear of the Mark button.
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp)) {
                        items(state.rows, key = { it.block.block.start.id }) { row ->
                            TimelineRowItem(
                                row, zone,
                                onClick = { onRowClick(row.block.block) },
                                onLongClick = { onDeleteRow(row.block.block) },
                            )
                        }
                    }
                    BottomFade(Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

@Composable
private fun MarkButton(onMark: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    ExtendedFloatingActionButton(
        onClick = onMark,
        Modifier.height(72.dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer,
    ) {
        Box(
            Modifier.size(26.dp).drawBehind {
                drawCircle(scheme.onPrimaryContainer.copy(alpha = 0.22f))
                drawCircle(scheme.onPrimaryContainer, 9.dp.toPx())
            },
        )
        Spacer(Modifier.size(14.dp))
        Text(stringResource(R.string.mark), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun BottomFade(modifier: Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier.fillMaxWidth().height(110.dp)
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.55f to surface)),
    )
}

@Composable
private fun DayHeader(day: LocalDate, zone: ZoneId, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit, onSettings: () -> Unit) {
    val title = if (day == LocalDate.now(zone)) "${stringResource(R.string.today)} · ${day.format(SHORT_DAY)}" else day.format(OTHER_DAY)
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
        Text(
            title,
            Modifier.clip(CircleShape).clickable(onClick = onToday).padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
        Spacer(Modifier.weight(1f))
        FilledTonalIconButton(
            onClick = onSettings,
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) { Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings)) }
    }
}

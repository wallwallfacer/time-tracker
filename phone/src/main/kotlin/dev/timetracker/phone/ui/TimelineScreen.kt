package dev.timetracker.phone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
        snackbarHost = { SnackbarHost(snackbar) },
        // In the bottom bar the undo snackbar sits above the button instead of covering it.
        bottomBar = {
            Button(onClick = onMark, Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                Text(stringResource(R.string.mark), style = MaterialTheme.typography.titleLarge)
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DayHeader(state.day, zone, onPrev, onNext, onToday, onSettings)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.isEmpty) {
                    Text(
                        stringResource(R.string.empty_hint),
                        Modifier.align(Alignment.Center).padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.rows, key = { it.block.block.start.id }) { row ->
                            TimelineRowItem(
                                row, zone,
                                onClick = { onRowClick(row.block.block) },
                                onLongClick = { onDeleteRow(row.block.block) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, zone: ZoneId, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit, onSettings: () -> Unit) {
    val title = if (day == LocalDate.now(zone)) "${stringResource(R.string.today)} · ${day.format(SHORT_DAY)}" else day.format(OTHER_DAY)
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onPrev) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
        Text(title, Modifier.clickable(onClick = onToday).padding(8.dp), style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onNext) { Text("›", style = MaterialTheme.typography.headlineMedium) }
        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) }
    }
}

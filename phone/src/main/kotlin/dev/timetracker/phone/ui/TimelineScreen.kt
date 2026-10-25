package dev.timetracker.phone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.timetracker.core.Block
import dev.timetracker.core.Point
import dev.timetracker.phone.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

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
    onDeleteRow: (Block) -> Point,
    onUndo: (Point) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.deleted_point)
    val undoLabel = stringResource(R.string.undo)
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DayHeader(state.day, zone, onPrev, onNext, onToday)
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
                                onLongClick = {
                                    val removed = onDeleteRow(row.block.block)
                                    scope.launch {
                                        snackbar.currentSnackbarData?.dismiss()
                                        val result = snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Short)
                                        if (result == SnackbarResult.ActionPerformed) onUndo(removed)
                                    }
                                },
                            )
                        }
                    }
                }
            }
            Button(onClick = onMark, Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.mark), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, zone: ZoneId, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    val title = if (day == LocalDate.now(zone)) "${stringResource(R.string.today)} · ${day.format(SHORT_DAY)}" else day.format(OTHER_DAY)
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onPrev) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
        Text(title, Modifier.clickable(onClick = onToday).padding(8.dp), style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onNext) { Text("›", style = MaterialTheme.typography.headlineMedium) }
    }
}

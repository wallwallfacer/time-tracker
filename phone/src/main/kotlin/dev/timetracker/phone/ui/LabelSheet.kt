package dev.timetracker.phone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.timetracker.core.Block
import dev.timetracker.core.formatDuration
import dev.timetracker.core.formatTime
import dev.timetracker.phone.R
import java.time.ZoneId
import kotlinx.coroutines.launch

/** A pending time edit: the picker opens on [at]; [after] runs once the picker closes (picked or cancelled). */
private class TimeEdit(val at: Long, val onPick: (Long) -> Unit, val after: () -> Unit = {})

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LabelSheet(block: Block, vm: LabelSheetViewModel, zone: ZoneId, onDismiss: () -> Unit) {
    val tags by vm.tags.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var note by remember(block.start.id) { mutableStateOf(block.start.note ?: "") }
    var edit by remember { mutableStateOf<TimeEdit?>(null) }
    var newTagOpen by remember { mutableStateOf(false) }

    fun saveNote() {
        if (note.trim() != (block.start.note ?: "")) vm.setNote(block, note)
    }
    fun close() {
        saveNote()
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = ::close) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { edit = TimeEdit(block.start.at, { vm.moveStart(block, it) }) }) {
                    Text(formatTime(block.start.at, zone), style = MaterialTheme.typography.headlineSmall)
                }
                Text("→", style = MaterialTheme.typography.headlineSmall)
                if (block.end == null) {
                    Text(stringResource(R.string.now), Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.headlineSmall)
                } else {
                    TextButton(onClick = { edit = TimeEdit(block.endAt, { vm.moveEnd(block, it) }) }) {
                        Text(formatTime(block.endAt, zone), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
            Text(
                stringResource(R.string.tap_to_change, formatDuration(block.durationMs)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    FilterChip(
                        selected = tag.id == block.start.tagId,
                        onClick = { vm.applyTag(block, tag.id); close() },
                        label = { Text(tag.name) },
                    )
                }
                AssistChip(onClick = { newTagOpen = true }, label = { Text(stringResource(R.string.new_tag)) })
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val created = vm.insertMidpoint(block)
                        edit = TimeEdit(created.at, { vm.moveStart(block.copy(start = created), it) }, after = ::close)
                    }
                },
                Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.insert_point)) }
            OutlinedButton(
                onClick = { vm.deleteStart(block); onDismiss() },
                Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.delete_start)) }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) saveNote() },
                label = { Text(stringResource(R.string.note_hint)) },
                singleLine = true,
            )
        }
    }

    edit?.let { e ->
        TimePickerDialog(
            initialAt = e.at,
            zone = zone,
            onConfirm = { e.onPick(it); edit = null; e.after() },
            onDismiss = { edit = null; e.after() },
        )
    }
    if (newTagOpen) {
        NewTagDialog(onCreate = { vm.createAndApply(block, it) }, onCreated = ::close, onDismiss = { newTagOpen = false })
    }
}

@Composable
private fun NewTagDialog(onCreate: suspend (String) -> Boolean, onCreated: () -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var duplicate by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { scope.launch { if (onCreate(name)) onCreated() else duplicate = true } },
            ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; duplicate = false },
                label = { Text(stringResource(R.string.tag_name)) },
                singleLine = true,
                isError = duplicate,
                supportingText = { if (duplicate) Text(stringResource(R.string.tag_exists)) },
            )
        },
    )
}

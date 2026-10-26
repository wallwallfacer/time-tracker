package dev.timetracker.phone.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.timetracker.core.Block
import dev.timetracker.core.Tag
import dev.timetracker.core.formatDuration
import dev.timetracker.core.formatTime
import dev.timetracker.phone.R
import java.time.ZoneId
import kotlinx.coroutines.launch

/** A pending time edit: the picker opens on [at]; [onPick] gets the picked hour and minute. */
private class TimeEdit(val at: Long, val onPick: (hour: Int, minute: Int) -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSheet(block: Block, vm: LabelSheetViewModel, zone: ZoneId, onDeleteStart: (Block) -> Unit, onDismiss: () -> Unit) {
    val tags by vm.tags.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var note by remember(block.start.id) { mutableStateOf(block.start.note ?: "") }
    var edit by remember { mutableStateOf<TimeEdit?>(null) }
    var newTagOpen by remember { mutableStateOf(false) }
    var outOfRange by remember { mutableStateOf(false) }
    LaunchedEffect(vm) { vm.moveRejected.collect { outOfRange = true } }

    fun saveNote() {
        if (note.trim() != (block.start.note ?: "")) vm.setNote(block, note)
    }
    fun close() {
        saveNote()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = ::close,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        LabelSheetContent(
            block, tags, zone, note, outOfRange,
            onNoteChange = { note = it },
            onNoteBlur = ::saveNote,
            onEditStart = { edit = TimeEdit(block.start.at, { h, m -> vm.moveStart(block, h, m, zone) }) },
            onEditEnd = { edit = TimeEdit(block.endAt, { h, m -> vm.moveEnd(block, h, m, zone) }) },
            onTag = { vm.applyTag(block, it.id); close() },
            onNewTag = { newTagOpen = true },
            onInsert = {
                scope.launch {
                    val created = vm.insertMidpoint(block)
                    // The sheet stays open on the first half, so a rejected time can be reported.
                    edit = TimeEdit(created.at) { h, m -> vm.moveStart(block.copy(start = created), h, m, zone) }
                }
            },
            onDelete = { onDeleteStart(block) },
        )
    }

    edit?.let { e ->
        TimePickerDialog(
            initialAt = e.at,
            zone = zone,
            onConfirm = { h, m -> outOfRange = false; e.onPick(h, m); edit = null },
            onDismiss = { edit = null },
        )
    }
    if (newTagOpen) {
        NewTagDialog(onCreate = { vm.createAndApply(block, it) }, onCreated = ::close, onDismiss = { newTagOpen = false })
    }
}

/** The sheet's body, free of view models and windows so it can be rendered on its own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelSheetContent(
    block: Block,
    tags: List<Tag>,
    zone: ZoneId,
    note: String,
    outOfRange: Boolean,
    onNoteChange: (String) -> Unit,
    onNoteBlur: () -> Unit,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onTag: (Tag) -> Unit,
    onNewTag: () -> Unit,
    onInsert: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        Text(stringResource(R.string.label_block).uppercase(), style = type.labelSmall, color = scheme.onSurfaceVariant)
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeTile(formatTime(block.start.at, zone), onEditStart)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = scheme.onSurfaceVariant)
            if (block.end == null) TimeTile(stringResource(R.string.now), onClick = null)
            else TimeTile(formatTime(block.endAt, zone), onEditEnd)
        }
        if (outOfRange) {
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint = scheme.error)
                Text(stringResource(R.string.move_out_of_range), style = type.labelMedium, color = scheme.error)
            }
        }
        // "1h 47m · tap a time to change it" with the duration emphasised.
        val hint = stringResource(R.string.tap_to_change, formatDuration(block.durationMs))
        val duration = formatDuration(block.durationMs)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = scheme.onSurface, fontWeight = type.labelLarge.fontWeight)) { append(duration) }
                append(hint.removePrefix(duration))
            },
            Modifier.padding(top = 10.dp, start = 2.dp),
            style = type.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp, start = 2.dp, end = 2.dp)) {
            Text(stringResource(R.string.tags_label), Modifier.weight(1f), style = type.labelMedium, color = scheme.onSurfaceVariant)
            Text(stringResource(R.string.last_used_first), style = type.bodySmall, color = scheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { tag -> TagChip(tag, selected = tag.id == block.start.tagId) { onTag(tag) } }
            Row(
                Modifier.height(44.dp).clip(ChipShape).border(1.5.dp, scheme.outlineVariant, ChipShape)
                    .clickable(onClick = onNewTag).padding(start = 10.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Add, null, Modifier.size(20.dp), tint = scheme.primary)
                Text(stringResource(R.string.new_tag), style = type.labelLarge, color = scheme.primary)
            }
        }
        TextField(
            value = note,
            onValueChange = onNoteChange,
            Modifier.fillMaxWidth().padding(top = 18.dp).onFocusChanged { if (!it.isFocused) onNoteBlur() },
            label = { Text(stringResource(R.string.note_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        )
        Button(
            onClick = onInsert,
            Modifier.fillMaxWidth().padding(top = 18.dp).height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = scheme.primaryContainer, contentColor = scheme.onPrimaryContainer),
        ) {
            Icon(Icons.Filled.Add, null, Modifier.size(20.dp))
            Text(stringResource(R.string.insert_point), Modifier.padding(start = 8.dp), style = type.labelLarge)
        }
        OutlinedButton(
            onClick = onDelete,
            Modifier.fillMaxWidth().padding(top = 10.dp).height(52.dp),
            border = BorderStroke(1.5.dp, scheme.error.copy(alpha = 0.45f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error),
        ) {
            Icon(Icons.Outlined.Delete, null, Modifier.size(20.dp))
            Text(stringResource(R.string.delete_start), Modifier.padding(start = 8.dp), style = type.labelLarge)
        }
    }
}

private val ChipShape = RoundedCornerShape(15.dp)

/** A large tonal boundary-time button; [onClick] null shows a fixed value ("now"). */
@Composable
private fun RowScope.TimeTile(text: String, onClick: (() -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.weight(1f).height(68.dp).clip(RoundedCornerShape(22.dp)).background(scheme.secondaryContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.headlineMedium.tabular, color = scheme.onSecondaryContainer)
    }
}

@Composable
private fun TagChip(tag: Tag, selected: Boolean, onClick: () -> Unit) {
    val tones = tagTones(tag.color)
    Row(
        Modifier.height(44.dp).clip(ChipShape).background(tones.container)
            .then(if (selected) Modifier.border(2.dp, tones.onContainer, ChipShape) else Modifier)
            .clickable(onClick = onClick).padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (selected) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = tones.onContainer)
        else Box(Modifier.size(12.dp).clip(ChipShape).background(tones.rail))
        Text(tag.name, style = MaterialTheme.typography.labelLarge, color = tones.onContainer)
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

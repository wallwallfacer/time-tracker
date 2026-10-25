package dev.timetracker.phone.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.ZoneId

/**
 * 24-hour time picker seeded with [initialAt]. [onConfirm] gets the picked hour and minute; an unchanged
 * hour/minute just dismisses, so seconds are never silently dropped. Choosing the date is the ViewModel's job.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initialAt: Long, zone: ZoneId, onConfirm: (hour: Int, minute: Int) -> Unit, onDismiss: () -> Unit) {
    val initial = Instant.ofEpochMilli(initialAt).atZone(zone)
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (state.hour == initial.hour && state.minute == initial.minute) {
                    onDismiss()
                } else {
                    onConfirm(state.hour, state.minute)
                }
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        text = { TimePicker(state = state) },
    )
}

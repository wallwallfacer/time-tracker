package dev.timetracker.phone.ui

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.timetracker.core.Tag
import dev.timetracker.phone.R
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, zone: ZoneId, onBack: () -> Unit) {
    val tags by vm.tags.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val exportedMessage = stringResource(R.string.export_done)
    val failedMessage = stringResource(R.string.export_failed)
    var renaming by remember { mutableStateOf<Tag?>(null) }
    var deleting by remember { mutableStateOf<Pair<Tag, Int>?>(null) }

    // The CSV is built after the picker returns, so it reflects the data at write time.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            val csv = vm.csv()
            val ok = withContext(Dispatchers.IO) {
                try {
                    val out = context.contentResolver.openOutputStream(uri, "wt")
                    if (out == null) false else { out.use { it.write(csv.toByteArray()) }; true }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("SettingsScreen", "CSV export failed", e)
                    false
                }
            }
            snackbar.showSnackbar(if (ok) exportedMessage else failedMessage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(tags, key = { it.id }) { tag ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(tag.color)))
                        Text(tag.name, Modifier.weight(1f).padding(start = 12.dp))
                        TextButton(onClick = { renaming = tag }) { Text(stringResource(R.string.rename)) }
                        TextButton(onClick = { scope.launch { deleting = tag to vm.deleteCount(tag.id) } }) { Text(stringResource(R.string.delete)) }
                    }
                }
            }
            Button(
                onClick = { picker.launch("time-tracker-${LocalDate.now(zone)}.csv") },
                Modifier.fillMaxWidth().padding(16.dp),
            ) { Text(stringResource(R.string.export_csv)) }
        }
    }

    renaming?.let { tag ->
        RenameDialog(tag, onRename = { vm.rename(tag.id, it) }, onDismiss = { renaming = null })
    }
    deleting?.let { (tag, count) ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            text = { Text(stringResource(R.string.delete_tag_confirm, tag.name, count)) },
            confirmButton = {
                TextButton(onClick = { vm.deleteTag(tag.id); deleting = null }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}

@Composable
private fun RenameDialog(tag: Tag, onRename: suspend (String) -> Boolean, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(tag.name) }
    var duplicate by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { scope.launch { if (onRename(name)) onDismiss() else duplicate = true } },
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

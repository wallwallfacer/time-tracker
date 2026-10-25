package dev.timetracker.phone

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.wearable.Wearable
import dev.timetracker.core.Block
import dev.timetracker.core.deriveBlocks
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
import dev.timetracker.phone.sync.LatestPublisher
import dev.timetracker.phone.sync.PointItemConsumer
import dev.timetracker.phone.ui.LabelSheet
import dev.timetracker.phone.ui.LabelSheetViewModel
import dev.timetracker.phone.ui.SettingsScreen
import dev.timetracker.phone.ui.SettingsViewModel
import dev.timetracker.phone.ui.TimeTrackerTheme
import dev.timetracker.phone.ui.TimelineScreen
import dev.timetracker.phone.ui.TimelineViewModel
import java.time.ZoneId
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val zone = ZoneId.systemDefault()

    private val viewModel: TimelineViewModel by viewModels {
        viewModelFactory {
            initializer {
                TimelineViewModel(
                    PointRepository(AppDatabase.get(applicationContext)),
                    LatestPublisher(Wearable.getDataClient(applicationContext)),
                    zone,
                    System::currentTimeMillis,
                )
            }
        }
    }

    private val labelViewModel: LabelSheetViewModel by viewModels {
        viewModelFactory {
            initializer {
                LabelSheetViewModel(
                    PointRepository(AppDatabase.get(applicationContext)),
                    LatestPublisher(Wearable.getDataClient(applicationContext)),
                    System::currentTimeMillis,
                )
            }
        }
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        viewModelFactory {
            initializer {
                SettingsViewModel(PointRepository(AppDatabase.get(applicationContext)), zone, System::currentTimeMillis)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            runCatching { PointItemConsumer.sweep(applicationContext) }
                .onFailure { Log.w(TAG, "Failed to sweep leftover watch points", it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimeTrackerTheme {
                var screen by rememberSaveable { mutableStateOf("timeline") }
                if (screen == "settings") {
                    BackHandler { screen = "timeline" }
                    SettingsScreen(settingsViewModel, zone, onBack = { screen = "timeline" })
                    return@TimeTrackerTheme
                }
                val state by viewModel.state.collectAsStateWithLifecycle()
                val points by labelViewModel.points.collectAsStateWithLifecycle()
                var selectedId by remember { mutableStateOf<String?>(null) }
                // One snackbar for every delete (row long-press and the sheet), so both offer undo.
                val snackbar = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val deletedMessage = stringResource(R.string.deleted_point)
                val undoLabel = stringResource(R.string.undo)
                fun deleteWithUndo(block: Block) {
                    val removed = block.start
                    viewModel.deletePoint(removed)
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        val result = snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Short)
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(removed)
                    }
                }
                TimelineScreen(
                    state = state,
                    zone = zone,
                    onPrev = viewModel::previousDay,
                    onNext = viewModel::nextDay,
                    onToday = viewModel::today,
                    onMark = viewModel::mark,
                    onRowClick = { selectedId = it.start.id },
                    onDeleteRow = ::deleteWithUndo,
                    onSettings = { screen = "settings" },
                    snackbar = snackbar,
                )
                // Re-derive from the latest points so the sheet follows edits; dismiss if the block vanished.
                selectedId?.let { id ->
                    val block = deriveBlocks(points, System.currentTimeMillis()).firstOrNull { it.start.id == id }
                    if (block == null) {
                        LaunchedEffect(id) { selectedId = null }
                    } else {
                        LabelSheet(
                            block, labelViewModel, zone,
                            onDeleteStart = { deleteWithUndo(it); selectedId = null },
                            onDismiss = { selectedId = null },
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}

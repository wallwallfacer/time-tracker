package dev.timetracker.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.wearable.Wearable
import dev.timetracker.core.deriveBlocks
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
import dev.timetracker.phone.sync.LatestPublisher
import dev.timetracker.phone.ui.LabelSheet
import dev.timetracker.phone.ui.LabelSheetViewModel
import dev.timetracker.phone.ui.TimeTrackerTheme
import dev.timetracker.phone.ui.TimelineScreen
import dev.timetracker.phone.ui.TimelineViewModel
import java.time.ZoneId

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimeTrackerTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val points by labelViewModel.points.collectAsStateWithLifecycle()
                var selectedId by remember { mutableStateOf<String?>(null) }
                TimelineScreen(
                    state = state,
                    zone = zone,
                    onPrev = viewModel::previousDay,
                    onNext = viewModel::nextDay,
                    onToday = viewModel::today,
                    onMark = viewModel::mark,
                    onRowClick = { selectedId = it.start.id },
                    onDeleteRow = { viewModel.deletePoint(it.start.id) },
                    onUndo = viewModel::undoDelete,
                )
                // Re-derive from the latest points so the sheet follows edits; dismiss if the block vanished.
                selectedId?.let { id ->
                    val block = deriveBlocks(points, System.currentTimeMillis()).firstOrNull { it.start.id == id }
                    if (block == null) {
                        LaunchedEffect(id) { selectedId = null }
                    } else {
                        LabelSheet(block, labelViewModel, zone, onDismiss = { selectedId = null })
                    }
                }
            }
        }
    }
}

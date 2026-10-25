package dev.timetracker.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.wearable.Wearable
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
import dev.timetracker.phone.sync.LatestPublisher
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimeTrackerTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                TimelineScreen(
                    state = state,
                    zone = zone,
                    onPrev = viewModel::previousDay,
                    onNext = viewModel::nextDay,
                    onToday = viewModel::today,
                    onMark = viewModel::mark,
                    onRowClick = { /* Task 9: label sheet */ },
                    onDeleteRow = { viewModel.deletePoint(it.start.id) },
                    onUndo = viewModel::undoDelete,
                )
            }
        }
    }
}

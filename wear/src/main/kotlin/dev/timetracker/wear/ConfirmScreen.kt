package dev.timetracker.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import dev.timetracker.core.formatDuration
import dev.timetracker.core.formatTime
import java.time.ZoneId

@Composable
fun ConfirmScreen(now: Long, prev: Long?) {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(formatTime(now, ZoneId.systemDefault()), fontSize = 40.sp)
            Text("● " + stringResource(R.string.recorded))
            if (prev != null) Text(stringResource(R.string.previous_block, formatDuration(now - prev)))
        }
    }
}

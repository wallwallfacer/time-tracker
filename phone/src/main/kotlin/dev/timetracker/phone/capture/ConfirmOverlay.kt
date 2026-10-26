package dev.timetracker.phone.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.timetracker.phone.R

/** What the overlay shows; [previousBlock] is null when there was no earlier point. */
data class OverlayState(val time: String, val previousBlock: String?)

@Composable
fun ConfirmOverlay(state: OverlayState?) {
    if (state == null) return
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(state.time, style = MaterialTheme.typography.displayLarge, color = Color.White)
        Text("● " + stringResource(R.string.recorded), style = MaterialTheme.typography.headlineSmall, color = Color.White)
        state.previousBlock?.let {
            Text(stringResource(R.string.previous_block, it), style = MaterialTheme.typography.bodyLarge, color = Color.White)
        }
    }
}

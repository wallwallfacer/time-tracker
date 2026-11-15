package dev.timetracker.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.timetracker.phone.R

/** Theme cards (each painted in its own colours) and the System / Light / Dark switch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePicker(choice: ThemeChoice, systemDark: Boolean, onTheme: (String) -> Unit, onMode: (ThemeMode) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(AppThemes.all, key = { it.id }) { theme ->
                ThemeCard(theme, resolveDark(choice.mode, theme, systemDark), theme.id == choice.theme.id) { onTheme(theme.id) }
            }
        }
        val labels = listOf(R.string.mode_system, R.string.mode_light, R.string.mode_dark)
        val locked = choice.theme.darkOnly
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            ThemeMode.entries.forEachIndexed { i, m ->
                SegmentedButton(
                    selected = (if (locked) ThemeMode.DARK else choice.mode) == m,
                    onClick = { onMode(m) },
                    enabled = !locked,
                    shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                ) { Text(stringResource(labels[i])) }
            }
        }
        if (locked) {
            Text(
                stringResource(R.string.dark_only_note, choice.theme.name),
                Modifier.padding(start = 8.dp, top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeCard(theme: AppTheme, dark: Boolean, selected: Boolean, onClick: () -> Unit) {
    val own = if (dark) theme.dark else theme.light
    val shape = RoundedCornerShape(theme.radii.card)
    val ring = MaterialTheme.colorScheme.primary
    Column(
        Modifier.width(104.dp).clip(shape).clickable(onClick = onClick).semantics { this.selected = selected }
            .background(own.surface, shape)
            .border(if (selected) 2.5.dp else 1.dp, if (selected) ring else MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(own.primaryContainer))
        Box(Modifier.width(54.dp).height(10.dp).clip(CircleShape).background(own.surfaceVariant))
        Box(Modifier.size(width = 36.dp, height = 18.dp).clip(RoundedCornerShape(theme.radii.chip.coerceAtMost(9.dp))).background(own.primary))
        Text(theme.name, style = MaterialTheme.typography.labelMedium, color = own.onSurface, modifier = Modifier.align(Alignment.Start))
    }
}

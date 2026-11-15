package dev.timetracker.phone.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Corner radii the shared sheets, chips and cards read, so a theme can reshape them without touching the screens. */
@Immutable
data class Radii(val card: Dp, val chip: Dp, val sheet: Dp)

class TimelineRowArgs(val row: TimelineRow, val zone: ZoneId, val onClick: () -> Unit, val onLongClick: () -> Unit)

class WeekGridArgs(val state: OverviewState, val today: LocalDate, val onOpenDay: (LocalDate) -> Unit)

class DayHeaderArgs(val state: TimelineState, val zone: ZoneId)

/**
 * One built-in look. Colours, type and shapes feed Material; the four slots replace the parts whose layout differs
 * between themes. A theme that does not care about a slot passes the Rail renderer.
 */
@Immutable
data class AppTheme(
    val id: String,
    val name: String,
    val darkOnly: Boolean,
    val light: ColorScheme,
    val dark: ColorScheme,
    val typography: Typography,
    val shapes: Shapes,
    val radii: Radii,
    /** Full-screen dim behind the capture overlay. */
    val scrim: Color,
    /** Tag colour (the stored hex) to the tones this theme paints it with. */
    val tones: (color: Int, scheme: ColorScheme, dark: Boolean) -> TagTones,
    val timelineRow: @Composable (TimelineRowArgs) -> Unit,
    val markButton: @Composable (onMark: () -> Unit) -> Unit,
    val weekGrid: @Composable (WeekGridArgs) -> Unit,
    /** Optional block above the day list (the Dial ring); null for none. */
    val dayHeader: (@Composable (DayHeaderArgs) -> Unit)? = null,
)

val RailTheme = AppTheme(
    id = "rail",
    name = "Rail",
    darkOnly = false,
    light = RailLightColors,
    dark = RailDarkColors,
    typography = RailTypography,
    shapes = Shapes(),
    radii = Radii(card = 18.dp, chip = 15.dp, sheet = 30.dp),
    scrim = Color(0xCC0E0F18),
    tones = ::railTones,
    timelineRow = { TimelineRowItem(it.row, it.zone, it.onClick, it.onLongClick) },
    markButton = { RailMarkButton(it) },
    weekGrid = { RailWeekGrid(it.state, it.today, it.onOpenDay) },
)

/** Every built-in theme, in picker order. */
object AppThemes {
    val all: List<AppTheme> = listOf(
        RailTheme,
        dev.timetracker.phone.ui.themes.DoseTheme,
        dev.timetracker.phone.ui.themes.DuskTheme,
        dev.timetracker.phone.ui.themes.StickerTheme,
        dev.timetracker.phone.ui.themes.DialTheme,
    )

    fun byId(id: String?): AppTheme = all.firstOrNull { it.id == id } ?: RailTheme
}

val LocalAppTheme = staticCompositionLocalOf { RailTheme }

/** A dark-only theme stays dark whatever the mode says. */
fun resolveDark(mode: ThemeMode, theme: AppTheme, systemDark: Boolean): Boolean = when {
    theme.darkOnly -> true
    mode == ThemeMode.LIGHT -> false
    mode == ThemeMode.DARK -> true
    else -> systemDark
}

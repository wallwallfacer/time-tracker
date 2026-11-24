package dev.timetracker.phone.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.timetracker.core.Point
import dev.timetracker.core.Source
import dev.timetracker.core.Tag
import dev.timetracker.core.ViewMode
import dev.timetracker.core.clipToDay
import dev.timetracker.core.deriveBlocks
import dev.timetracker.core.formatDuration
import dev.timetracker.phone.capture.ConfirmOverlay
import dev.timetracker.phone.capture.OverlayState
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
import dev.timetracker.phone.ui.themes.DialTheme
import dev.timetracker.phone.ui.themes.DoseTheme
import dev.timetracker.phone.ui.themes.DuskTheme
import dev.timetracker.phone.ui.themes.StickerTheme
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders screens in each translated language (PNGs in phone/build/renders) to check that longer text and units still fit. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class LocalizedRenderTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val zone = ZoneOffset.UTC
    private val day = LocalDate.now(zone)
    private val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun at(h: Int, m: Int, s: Int = 0) = dayStart + ((h * 60L + m) * 60 + s) * 1000

    private val tags = listOf(
        Tag("work", "Work", 0xFF64B5F6.toInt(), 7), Tag("commute", "Commute", 0xFFFFB74D.toInt(), 6),
        Tag("breakfast", "Breakfast", 0xFFFFF176.toInt(), 5), Tag("lunch", "Lunch", 0xFF81C784.toInt(), 4),
        Tag("sleep", "Sleep", 0xFF9575CD.toInt(), 3), Tag("reading", "Reading", 0xFF4DB6AC.toInt(), 2),
        Tag("exercise", "Exercise", 0xFFE57373.toInt(), 1),
    )

    private fun point(id: String, at: Long, tag: String? = null) = Point(id, at, Source.WATCH, tag, createdAt = at)

    private val points = listOf(
        point("p1", at(7, 10), "breakfast"), point("p2", at(7, 45), "commute"), point("p3", at(8, 35), "work"),
        point("p4", at(11, 45)), point("p5", at(11, 45, 12), "lunch"), point("p6", at(12, 45, 12)), point("p7", at(14, 32)),
    )
    private val now = at(14, 44, 8)

    private fun sampleState(): TimelineState {
        val byId = tags.associateBy { it.id }
        val rows = clipToDay(deriveBlocks(points, now), dayStart, dayStart + 86_400_000)
            .map { TimelineRow(it, byId[it.block.start.tagId]) }
        return TimelineState(day, rows, isEmpty = false)
    }

    /** Freezes the clock so infinite animations (live pulse) cannot keep the test busy, then captures. */
    private fun render(name: String, theme: AppTheme = RailTheme, dark: Boolean = false, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { rule.activity.actionBar?.hide() } // the test activity's bar; the app's theme has none
        rule.setContent { TimeTrackerTheme(dark, theme) { Surface(Modifier.fillMaxSize()) { content() } } }
        rule.mainClock.advanceTimeBy(1_000)
        // Draw the window directly: captureToImage waits for a real frame, which a frozen clock never delivers.
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(Canvas(bitmap)) }
        File("build/renders").apply { mkdirs() }.resolve("$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun timeline() = @Composable { TimelineScreen(sampleState(), zone, {}, {}, {}, {}, {}, {}, {}, {}, SnackbarHostState()) }

    private fun settings(name: String) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val repo = PointRepository(db) { now }
        runBlocking { tags.reversed().forEach { repo.createTag(it.name) } }
        render(name) { SettingsScreen(SettingsViewModel(repo, zone) { now }, zone, onBack = {}, language = AppLanguage.KOREAN) }
        db.close()
    }

    private fun labelSheet() = @Composable {
        val block = deriveBlocks(points, now).first { it.start.id == "p6" }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Box(Modifier.padding(top = 24.dp)) {
                    LabelSheetContent(block, tags, zone, "", outOfRange = true, {}, {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
    }

    // ---- Week / Month: three prior weeks of a routine with a few unlabeled evenings, plus today's sample ----
    private val history: List<Point> = (1..24).flatMap { back ->
        val start = dayStart - back * 86_400_000L
        fun at(h: Int, m: Int) = start + (h * 60L + m) * 60_000
        val weekend = day.minusDays(back.toLong()).dayOfWeek.value >= 6
        val evening = if (back % 3 == 0 || back < 3) null else if (back % 2 == 0) "exercise" else "reading"
        if (weekend) listOf(
            point("b$back", at(9, 0), "breakfast"), point("r$back", at(9, 40), "reading"), point("l$back", at(13, 0), "lunch"),
            point("e$back", at(14, 0), evening), point("s$back", at(22, 30), "sleep"),
        ) else listOf(
            point("b$back", at(7, 10), "breakfast"), point("c$back", at(7, 45), "commute"), point("w$back", at(8, 35), "work"),
            point("l$back", at(12, 0), "lunch"), point("x$back", at(13, 0), "work"), point("d$back", at(18, 0), "commute"),
            point("e$back", at(18, 50), evening), point("s$back", at(23, 0), "sleep"),
        )
    } + points

    private fun overview(mode: ViewMode) =
        @Composable { OverviewScreen(overviewState(history, tags, mode, null, null, zone, now), zone, {}, {}, {}, {}, {}, {}, {}) }

    @Test @Config(qualifiers = "+ko") fun koTimeline() = render("i18n-ko-timeline", content = timeline())
    @Test @Config(qualifiers = "+ko") fun koWeek() = render("i18n-ko-week", content = overview(ViewMode.WEEK))
    @Test @Config(qualifiers = "+ko") fun koMonth() = render("i18n-ko-month", content = overview(ViewMode.MONTH))
    @Test @Config(qualifiers = "+ko") fun koSettings() = settings("i18n-ko-settings")
    @Test @Config(qualifiers = "+ko") fun koLabelSheet() = render("i18n-ko-label-sheet", content = labelSheet())
    @Test @Config(qualifiers = "+ko") fun koCapture() = render("i18n-ko-capture", dark = true) { ConfirmOverlay(OverlayState("14:32", formatDuration(107 * 60_000L, appLocale()))) }
    @Test @Config(qualifiers = "+ko") fun koStickerWeek() = render("i18n-ko-sticker-week", StickerTheme, content = overview(ViewMode.WEEK))
    @Test @Config(qualifiers = "+ko") fun koStickerTimeline() = render("i18n-ko-sticker-timeline", StickerTheme, content = timeline())
    @Test @Config(qualifiers = "+ko") fun koDialTimeline() = render("i18n-ko-dial-timeline", DialTheme, content = timeline())
    @Test @Config(qualifiers = "+ko") fun koDialWeek() = render("i18n-ko-dial-week", DialTheme, content = overview(ViewMode.WEEK))
    @Test @Config(qualifiers = "+ko") fun koDoseTimeline() = render("i18n-ko-dose-timeline", DoseTheme, content = timeline())
    @Test @Config(qualifiers = "+ko") fun koDuskWeek() = render("i18n-ko-dusk-week", DuskTheme, dark = true, content = overview(ViewMode.WEEK))

    @Test @Config(qualifiers = "+zh-rHK") fun zhHantTimeline() = render("i18n-zh-hant-timeline", content = timeline())
    @Test @Config(qualifiers = "+zh-rHK") fun zhHantWeek() = render("i18n-zh-hant-week", content = overview(ViewMode.WEEK))
    @Test @Config(qualifiers = "+zh-rHK") fun zhHantSettings() = settings("i18n-zh-hant-settings")
    @Test @Config(qualifiers = "+zh-rCN") fun zhHansTimeline() = render("i18n-zh-hans-timeline", content = timeline())
    @Test @Config(qualifiers = "+zh-rCN") fun zhHansWeek() = render("i18n-zh-hans-week", content = overview(ViewMode.WEEK))
    @Test @Config(qualifiers = "+zh-rCN") fun zhHansSettings() = settings("i18n-zh-hans-settings")
    @Test @Config(qualifiers = "+zh-rCN") fun zhHansLabelSheet() = render("i18n-zh-hans-label-sheet", content = labelSheet())
    @Test @Config(qualifiers = "+ja") fun jaTimeline() = render("i18n-ja-timeline", content = timeline())
    @Test @Config(qualifiers = "+ja") fun jaWeek() = render("i18n-ja-week", content = overview(ViewMode.WEEK))
    @Test @Config(qualifiers = "+ja") fun jaSettings() = settings("i18n-ja-settings")
    @Test @Config(qualifiers = "+ja") fun jaStickerWeek() = render("i18n-ja-sticker-week", StickerTheme, content = overview(ViewMode.WEEK))
    @Test @Config(qualifiers = "+ms") fun msTimeline() = render("i18n-ms-timeline", content = timeline())
    @Test @Config(qualifiers = "+ms") fun msWeek() = render("i18n-ms-week", content = overview(ViewMode.WEEK))
    @Test @Config(qualifiers = "+ms") fun msSettings() = settings("i18n-ms-settings")
    @Test @Config(qualifiers = "+ms") fun msStickerWeek() = render("i18n-ms-sticker-week", StickerTheme, content = overview(ViewMode.WEEK))
}

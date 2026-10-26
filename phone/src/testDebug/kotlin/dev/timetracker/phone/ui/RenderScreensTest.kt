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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.timetracker.core.Point
import dev.timetracker.core.Source
import dev.timetracker.core.Tag
import dev.timetracker.core.clipToDay
import dev.timetracker.core.deriveBlocks
import dev.timetracker.phone.R
import dev.timetracker.phone.capture.ConfirmOverlay
import dev.timetracker.phone.capture.OverlayState
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
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

/**
 * Renders each redesigned screen with the sample day from docs/ui-candidates/BRIEF.md as PNGs in phone/build/renders,
 * for visual comparison with the Rail mockup. Fails only if a screen cannot compose or draw.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class RenderScreensTest {
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
    private fun render(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { rule.activity.actionBar?.hide() } // the test activity's bar; the app's theme has none
        rule.setContent { TimeTrackerTheme(dark) { Surface(Modifier.fillMaxSize()) { content() } } }
        rule.mainClock.advanceTimeBy(1_000)
        // Draw the window directly: captureToImage waits for a real frame, which a frozen clock never delivers.
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(Canvas(bitmap)) }
        File("build/renders").apply { mkdirs() }.resolve("$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun timeline(snackbar: SnackbarHostState = SnackbarHostState(), state: TimelineState = sampleState()) =
        @Composable { TimelineScreen(state, zone, {}, {}, {}, {}, {}, {}, {}, snackbar) }

    @Test fun timelineLight() = render("01-timeline-light", content = timeline())

    @Test fun timelineDarkWithUndo() = render("02-timeline-dark-undo", dark = true) {
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { snackbar.showSnackbar("Point deleted", "Undo") }
        timeline(snackbar)()
    }

    @Test fun labelSheet() = render("03-label-sheet") {
        val block = deriveBlocks(points, now).first { it.start.id == "p6" }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Box(Modifier.padding(top = 24.dp)) {
                    LabelSheetContent(block, tags, zone, "", outOfRange = true, {}, {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
    }

    @Test fun settings() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val repo = PointRepository(db) { now }
        runBlocking { tags.reversed().forEach { repo.createTag(it.name) } }
        render("04-settings") { SettingsScreen(SettingsViewModel(repo, zone) { now }, zone, onBack = {}) }
        db.close()
    }

    @Test fun captureOverlay() = render("05-capture-overlay", dark = true) {
        Box(Modifier.fillMaxSize()) {
            timeline()()
            ConfirmOverlay(OverlayState("14:32", "1h 47m"))
        }
    }

    /** Draws each launcher icon through AdaptiveIconDrawable, so the system mask and safe zone are real. */
    @Test fun launcherIcons() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val size = 432
        val sheet = Bitmap.createBitmap(size * 2 + 48, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_mark).forEachIndexed { i, id ->
            context.getDrawable(id)!!.apply {
                setBounds(i * (size + 48), 0, i * (size + 48) + size, size)
                draw(canvas)
            }
        }
        File("build/renders").apply { mkdirs() }.resolve("07-launcher-icons.png").outputStream().use {
            sheet.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun emptyState() = render("06-empty", content = timeline(state = TimelineState(day, emptyList(), isEmpty = true)))
}

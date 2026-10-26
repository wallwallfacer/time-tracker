package dev.timetracker.wear

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the confirm screen, recorded and failed, as PNGs in wear/build/renders for visual review. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w227dp-h227dp-round-xhdpi")
class RenderConfirmTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val now = 1_792_917_120_000L // a fixed instant; the shown time follows the JVM's zone
    private val prev = now - (107 * 60_000L)

    private fun render(name: String, recorded: Boolean) {
        rule.runOnUiThread { rule.activity.actionBar?.hide() }
        rule.setContent { ConfirmScreen(now, prev, recorded) }
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(Canvas(bitmap)) }
        File("build/renders").apply { mkdirs() }.resolve("$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun recorded() = render("watch-recorded", recorded = true)

    @Test fun notRecorded() = render("watch-not-recorded", recorded = false)
}

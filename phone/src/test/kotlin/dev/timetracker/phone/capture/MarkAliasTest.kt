package dev.timetracker.phone.capture

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MarkAliasTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun launcherEntries(): List<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        return context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.name }
    }

    @Test fun shownByDefault() {
        assertTrue(MarkAlias.isShown(context))
        assertTrue("dev.timetracker.phone.Mark" in launcherEntries())
    }

    @Test fun hidingRemovesOnlyTheMarkLauncherEntry() {
        MarkAlias.setShown(context, false)
        assertFalse(MarkAlias.isShown(context))
        assertEquals(listOf("dev.timetracker.phone.MainActivity"), launcherEntries())

        MarkAlias.setShown(context, true)
        assertTrue(MarkAlias.isShown(context))
        assertTrue("dev.timetracker.phone.Mark" in launcherEntries())
    }
}

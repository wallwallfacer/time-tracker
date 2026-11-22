package dev.timetracker.phone.capture

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * The "Mark" home-screen icon: the `.Mark` activity-alias in AndroidManifest.xml, which the side button binds to.
 * Its enabled state in PackageManager is the only record of the setting, so there is nothing to keep in sync.
 */
object MarkAlias {
    private fun component(context: Context) = ComponentName(context, "dev.timetracker.phone.Mark")

    fun isShown(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(component(context)) !=
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    /** Hiding also drops "Mark" from the side-button app picker, which only lists launcher entries. */
    fun setShown(context: Context, shown: Boolean) {
        val state = if (shown) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        context.packageManager.setComponentEnabledSetting(component(context), state, PackageManager.DONT_KILL_APP)
    }
}

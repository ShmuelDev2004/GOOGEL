package app.sederyom.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.sederyom.SederYomApplication
import kotlinx.coroutines.launch

/** Alarms are lost on reboot, app update and clock or zone changes, so each of these triggers a reschedule. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val application = context.applicationContext as? SederYomApplication ?: return
        val pending = goAsync()
        application.container.applicationScope.launch {
            try {
                application.container.reminderRescheduler.reschedule()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}

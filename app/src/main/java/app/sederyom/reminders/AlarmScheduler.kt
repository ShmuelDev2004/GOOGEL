package app.sederyom.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.sederyom.MainActivity
import app.sederyom.domain.model.NotifyMode
import app.sederyom.domain.usecase.ReminderEntry
import app.sederyom.ui.components.formatHm
import java.time.ZoneId

/** Registers alarms with setAlarmClock, which fires on time even in Doze and needs no special permission. */
class AlarmScheduler(private val context: Context) {
    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    fun schedule(entry: ReminderEntry, mode: NotifyMode, zone: ZoneId) {
        val manager = alarmManager ?: return
        val triggerMillis = entry.triggerAt.atZone(zone).toInstant().toEpochMilli()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(EXTRA_NOTIFICATION_ID, entry.requestCode)
            .putExtra(EXTRA_TYPE, entry.type.name)
            .putExtra(EXTRA_TITLE, entry.title)
            .putExtra(EXTRA_START, entry.blockStart.toLocalTime().formatHm())
            .putExtra(EXTRA_LEAD, entry.leadMinutes)
            .putExtra(EXTRA_MODE, mode.name)
        val operation = PendingIntent.getBroadcast(
            context,
            entry.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMillis, openAppIntent()), operation)
    }

    fun cancel(requestCode: Int) {
        val manager = alarmManager ?: return
        val existing = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        manager.cancel(existing)
        existing.cancel()
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_TYPE = "type"
        const val EXTRA_TITLE = "title"
        const val EXTRA_START = "start"
        const val EXTRA_LEAD = "lead"
        const val EXTRA_MODE = "mode"
    }
}

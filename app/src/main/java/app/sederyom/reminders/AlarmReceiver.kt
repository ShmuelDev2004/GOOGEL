package app.sederyom.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.sederyom.R
import app.sederyom.domain.model.BlockType
import app.sederyom.domain.model.NotifyMode
import app.sederyom.ui.components.blockName
import app.sederyom.ui.components.isolateLtr

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(AlarmScheduler.EXTRA_NOTIFICATION_ID, 0)
        val type = BlockType.fromName(intent.getStringExtra(AlarmScheduler.EXTRA_TYPE))
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE).orEmpty()
        val start = isolateLtr(intent.getStringExtra(AlarmScheduler.EXTRA_START).orEmpty())
        val lead = intent.getIntExtra(AlarmScheduler.EXTRA_LEAD, 0)
        val mode = NotifyMode.fromName(intent.getStringExtra(AlarmScheduler.EXTRA_MODE))

        val text = if (lead <= 0) {
            context.getString(R.string.notification_text_now, start)
        } else {
            context.getString(R.string.notification_text_lead, lead, start)
        }
        NotificationHelper(context).showReminder(notificationId, blockName(context, type, title), text, mode)
    }
}

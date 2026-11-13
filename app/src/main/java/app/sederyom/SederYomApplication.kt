package app.sederyom

import android.app.Application
import app.sederyom.di.AppContainer
import app.sederyom.reminders.NotificationHelper
import app.sederyom.reminders.ReminderWorker
import kotlinx.coroutines.launch

class SederYomApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper(this).ensureChannels()
        ReminderWorker.enqueueDaily(this)
        container.applicationScope.launch { container.reminderRescheduler.reschedule() }
    }
}

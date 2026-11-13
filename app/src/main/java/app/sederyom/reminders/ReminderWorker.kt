package app.sederyom.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.sederyom.SederYomApplication
import java.util.concurrent.TimeUnit

/** Runs once a day so the rolling 7 day window always reaches a week ahead. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? SederYomApplication ?: return Result.failure()
        application.container.reminderRescheduler.reschedule()
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "daily_reminder_reschedule"

        fun enqueueDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

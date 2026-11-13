package app.sederyom.reminders

import app.sederyom.data.settings.SettingsRepository
import app.sederyom.domain.repository.ScheduleRepository
import app.sederyom.domain.usecase.ComputeReminderPlan
import app.sederyom.domain.usecase.ReminderRescheduler
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cancels the alarms registered last time and registers a fresh 7 day plan.
 * Safe to call from anywhere and as often as needed; calls are serialized.
 */
class ReminderCoordinator(
    private val scheduleRepository: ScheduleRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val computeReminderPlan: ComputeReminderPlan = ComputeReminderPlan(),
) : ReminderRescheduler {
    private val mutex = Mutex()

    override suspend fun reschedule() {
        mutex.withLock {
            // The zone is read on every run so a time zone change is picked up.
            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.now(zone)
            val today = now.toLocalDate()
            val settings = settingsRepository.settings.first()
            val template = scheduleRepository.getTemplate()
            val lastDate = today.plusDays(ComputeReminderPlan.PLAN_DAYS.toLong())
            val overrides = scheduleRepository.getOverrides(today, lastDate)

            val plan = computeReminderPlan(template, overrides, now, settings.leadTimeMinutes)

            settingsRepository.getScheduledCodes().forEach { alarmScheduler.cancel(it) }
            plan.forEach { alarmScheduler.schedule(it, settings.notifyMode, zone) }
            settingsRepository.setScheduledCodes(plan.map { it.requestCode }.toSet())
        }
    }
}

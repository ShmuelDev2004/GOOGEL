package app.sederyom.domain.usecase

import app.sederyom.domain.model.AppSettings
import app.sederyom.domain.model.NotifyMode
import app.sederyom.domain.repository.SettingsStore
import kotlinx.coroutines.flow.Flow

class ObserveSettings(private val store: SettingsStore) {
    operator fun invoke(): Flow<AppSettings> = store.settings
}

/** Changing a setting that affects reminders re-registers all alarms. */
class UpdateSettings(
    private val store: SettingsStore,
    private val rescheduler: ReminderRescheduler,
) {
    suspend fun setLeadTime(minutes: Int) {
        if (minutes !in AppSettings.LEAD_TIME_OPTIONS) return
        store.setLeadTimeMinutes(minutes)
        rescheduler.reschedule()
    }

    suspend fun setNotifyMode(mode: NotifyMode) {
        store.setNotifyMode(mode)
        rescheduler.reschedule()
    }
}

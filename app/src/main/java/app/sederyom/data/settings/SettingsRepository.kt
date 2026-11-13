package app.sederyom.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.sederyom.domain.model.AppSettings
import app.sederyom.domain.model.NotifyMode
import app.sederyom.domain.repository.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** DataStore wrapper for app settings and for the bookkeeping of which alarms are currently scheduled. */
class SettingsRepository(private val context: Context) : SettingsStore {
    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .map { prefs ->
            AppSettings(
                leadTimeMinutes = prefs[LEAD_TIME] ?: AppSettings.DEFAULT_LEAD_TIME_MINUTES,
                notifyMode = NotifyMode.fromName(prefs[NOTIFY_MODE]),
                onboardingDone = prefs[ONBOARDING_DONE] ?: false,
            )
        }
        .distinctUntilChanged()

    override suspend fun setLeadTimeMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[LEAD_TIME] = minutes }
    }

    override suspend fun setNotifyMode(mode: NotifyMode) {
        context.settingsDataStore.edit { it[NOTIFY_MODE] = mode.name }
    }

    override suspend fun setOnboardingDone() {
        context.settingsDataStore.edit { it[ONBOARDING_DONE] = true }
    }

    suspend fun getScheduledCodes(): Set<Int> {
        val stored = context.settingsDataStore.data.first()[SCHEDULED_CODES].orEmpty()
        return stored.mapNotNull { it.toIntOrNull() }.toSet()
    }

    suspend fun setScheduledCodes(codes: Set<Int>) {
        context.settingsDataStore.edit { prefs -> prefs[SCHEDULED_CODES] = codes.map { it.toString() }.toSet() }
    }

    private companion object {
        val LEAD_TIME = intPreferencesKey("lead_time_minutes")
        val NOTIFY_MODE = stringPreferencesKey("notify_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SCHEDULED_CODES = stringSetPreferencesKey("scheduled_alarm_codes")
    }
}

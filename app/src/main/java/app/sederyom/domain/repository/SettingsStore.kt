package app.sederyom.domain.repository

import app.sederyom.domain.model.AppSettings
import app.sederyom.domain.model.NotifyMode
import kotlinx.coroutines.flow.Flow

interface SettingsStore {
    val settings: Flow<AppSettings>

    suspend fun setLeadTimeMinutes(minutes: Int)

    suspend fun setNotifyMode(mode: NotifyMode)

    suspend fun setOnboardingDone()
}

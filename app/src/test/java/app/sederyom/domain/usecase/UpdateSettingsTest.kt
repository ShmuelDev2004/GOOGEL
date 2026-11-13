package app.sederyom.domain.usecase

import app.sederyom.domain.model.AppSettings
import app.sederyom.domain.model.NotifyMode
import app.sederyom.domain.repository.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateSettingsTest {
    private class FakeStore : SettingsStore {
        val state = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = state

        override suspend fun setLeadTimeMinutes(minutes: Int) {
            state.value = state.value.copy(leadTimeMinutes = minutes)
        }

        override suspend fun setNotifyMode(mode: NotifyMode) {
            state.value = state.value.copy(notifyMode = mode)
        }

        override suspend fun setOnboardingDone() {
            state.value = state.value.copy(onboardingDone = true)
        }
    }

    @Test
    fun leadTimeChangeIsStoredAndReschedules() = runBlocking {
        val store = FakeStore()
        var rescheduled = 0
        UpdateSettings(store, { rescheduled++ }).setLeadTime(30)

        assertEquals(30, store.state.value.leadTimeMinutes)
        assertEquals(1, rescheduled)
    }

    @Test
    fun leadTimeOutsideTheOptionsIsIgnored() = runBlocking {
        val store = FakeStore()
        var rescheduled = 0
        UpdateSettings(store, { rescheduled++ }).setLeadTime(7)

        assertEquals(AppSettings.DEFAULT_LEAD_TIME_MINUTES, store.state.value.leadTimeMinutes)
        assertEquals(0, rescheduled)
    }

    @Test
    fun notifyModeChangeIsStoredAndReschedules() = runBlocking {
        val store = FakeStore()
        var rescheduled = 0
        UpdateSettings(store, { rescheduled++ }).setNotifyMode(NotifyMode.VIBRATE)

        assertEquals(NotifyMode.VIBRATE, store.state.value.notifyMode)
        assertEquals(1, rescheduled)
    }
}

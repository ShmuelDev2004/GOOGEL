package app.sederyom.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sederyom.R
import app.sederyom.domain.model.AppSettings
import app.sederyom.domain.model.NotifyMode
import app.sederyom.domain.usecase.ApplyImport
import app.sederyom.domain.usecase.ExportBackup
import app.sederyom.domain.usecase.ImportError
import app.sederyom.domain.usecase.ImportPreview
import app.sederyom.domain.usecase.ObserveSettings
import app.sederyom.domain.usecase.PrepareImport
import app.sederyom.domain.usecase.PrepareImportResult
import app.sederyom.domain.usecase.UpdateSettings
import app.sederyom.ui.components.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the import dialog shows: nothing, a preview waiting for confirmation, or the reason a file was refused. */
sealed interface ImportDialog {
    data object None : ImportDialog

    data class Preview(val preview: ImportPreview) : ImportDialog

    data class Rejected(val error: ImportError) : ImportDialog
}

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val dialog: ImportDialog = ImportDialog.None,
    val busy: Boolean = false,
)

class SettingsViewModel(
    observeSettings: ObserveSettings,
    private val updateSettings: UpdateSettings,
    private val exportBackup: ExportBackup,
    private val prepareImport: PrepareImport,
    private val applyImport: ApplyImport,
) : ViewModel() {
    private val dialog = MutableStateFlow<ImportDialog>(ImportDialog.None)
    private val busy = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> = combine(observeSettings(), dialog, busy) { settings, dialog, busy ->
        SettingsUiState(settings, dialog, busy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = eventChannel.receiveAsFlow()

    fun onLeadTimeSelected(minutes: Int) {
        viewModelScope.launch { updateSettings.setLeadTime(minutes) }
    }

    fun onNotifyModeSelected(mode: NotifyMode) {
        viewModelScope.launch { updateSettings.setNotifyMode(mode) }
    }

    fun onExportTo(location: String) {
        viewModelScope.launch {
            busy.value = true
            val ok = exportBackup(location)
            busy.value = false
            eventChannel.send(UiEvent.Message(if (ok) R.string.export_done else R.string.export_failed))
        }
    }

    fun onImportFrom(location: String) {
        viewModelScope.launch {
            busy.value = true
            dialog.value = when (val result = prepareImport(location)) {
                is PrepareImportResult.Ready -> ImportDialog.Preview(result.preview)
                is PrepareImportResult.Rejected -> ImportDialog.Rejected(result.error)
            }
            busy.value = false
        }
    }

    fun onConfirmImport() {
        val current = dialog.value as? ImportDialog.Preview ?: return
        viewModelScope.launch {
            busy.value = true
            applyImport(current.preview.data)
            busy.value = false
            dialog.update { ImportDialog.None }
            eventChannel.send(UiEvent.Message(R.string.import_done))
        }
    }

    fun onDismissDialog() {
        dialog.value = ImportDialog.None
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

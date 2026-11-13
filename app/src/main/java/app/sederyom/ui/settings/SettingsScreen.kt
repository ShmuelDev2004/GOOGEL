package app.sederyom.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sederyom.R
import app.sederyom.domain.model.AppSettings
import app.sederyom.domain.model.NotifyMode
import app.sederyom.domain.usecase.ImportPreview
import app.sederyom.ui.components.SectionTitle
import app.sederyom.ui.components.UiEvent
import app.sederyom.ui.components.describeImportError
import app.sederyom.ui.components.formatDate

private const val BACKUP_FILE_NAME = "sederyom-backup.json"
private const val BACKUP_MIME_TYPE = "application/json"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is UiEvent.Message) snackbarHostState.showSnackbar(context.getString(event.resId))
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
    ) { uri -> if (uri != null) viewModel.onExportTo(uri.toString()) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.onImportFrom(uri.toString()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReminderSettings(
                settings = state.settings,
                onLeadTime = viewModel::onLeadTimeSelected,
                onNotifyMode = viewModel::onNotifyModeSelected,
            )
            HorizontalDivider()
            SectionTitle(stringResource(R.string.settings_backup))
            Text(stringResource(R.string.settings_backup_hint), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = { exportLauncher.launch(BACKUP_FILE_NAME) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_export)) }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_import)) }
        }
    }

    when (val dialog = state.dialog) {
        ImportDialog.None -> Unit
        is ImportDialog.Preview -> PreviewDialog(
            preview = dialog.preview,
            onConfirm = viewModel::onConfirmImport,
            onDismiss = viewModel::onDismissDialog,
        )

        is ImportDialog.Rejected -> AlertDialog(
            onDismissRequest = viewModel::onDismissDialog,
            title = { Text(stringResource(R.string.import_rejected_title)) },
            text = { Text(describeImportError(context, dialog.error)) },
            confirmButton = { TextButton(onClick = viewModel::onDismissDialog) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderSettings(
    settings: AppSettings,
    onLeadTime: (Int) -> Unit,
    onNotifyMode: (NotifyMode) -> Unit,
) {
    SectionTitle(stringResource(R.string.settings_lead_time))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppSettings.LEAD_TIME_OPTIONS.forEach { minutes ->
            FilterChip(
                selected = settings.leadTimeMinutes == minutes,
                onClick = { onLeadTime(minutes) },
                label = {
                    Text(
                        if (minutes == 0) {
                            stringResource(R.string.settings_lead_at_start)
                        } else {
                            stringResource(R.string.settings_lead_minutes, minutes)
                        },
                    )
                },
            )
        }
    }
    SectionTitle(stringResource(R.string.settings_notify_mode))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = settings.notifyMode == NotifyMode.SOUND,
            onClick = { onNotifyMode(NotifyMode.SOUND) },
            label = { Text(stringResource(R.string.settings_mode_sound)) },
        )
        FilterChip(
            selected = settings.notifyMode == NotifyMode.VIBRATE,
            onClick = { onNotifyMode(NotifyMode.VIBRATE) },
            label = { Text(stringResource(R.string.settings_mode_vibrate)) },
        )
    }
}

@Composable
private fun PreviewDialog(preview: ImportPreview, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val first = preview.firstOverride
    val last = preview.lastOverride
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_preview_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.import_preview_template, preview.templateCount))
                Text(stringResource(R.string.import_preview_overrides, preview.overrideCount))
                if (first != null && last != null) {
                    Text(stringResource(R.string.import_preview_range, formatDate(first), formatDate(last)))
                }
                Text(
                    text = stringResource(R.string.import_preview_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.import_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.sheet_cancel)) } },
    )
}

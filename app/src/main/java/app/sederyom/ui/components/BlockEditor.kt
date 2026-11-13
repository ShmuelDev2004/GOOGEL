package app.sederyom.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.sederyom.R
import app.sederyom.domain.model.BlockType
import app.sederyom.domain.model.MINUTES_PER_DAY
import app.sederyom.domain.model.TimeBlock
import app.sederyom.domain.usecase.ValidateBlock
import java.time.LocalTime
import kotlin.math.roundToInt

private enum class SheetKind { TIME, DURATION, DETAILS }

private data class SheetTarget(val blockId: Long, val kind: SheetKind)

private const val DURATION_STEP = 5
private fun snapToStep(value: Float): Int =
    ((value / DURATION_STEP).roundToInt() * DURATION_STEP).coerceIn(DURATION_STEP, MINUTES_PER_DAY)

private val DURATION_PRESETS = listOf(15, 30, 45, 60, 90, 120, 240, 480)

/**
 * A list of block cards. Every block is edited on its own: the time, the duration and the details each
 * open their own bottom sheet and report a single changed block through [onUpdate].
 */
@Composable
fun BlockListEditor(
    blocks: List<TimeBlock>,
    emptyText: String,
    onUpdate: (TimeBlock) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    header: (@Composable () -> Unit)? = null,
) {
    var target by remember { mutableStateOf<SheetTarget?>(null) }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (header != null) {
            item(key = "header") { header() }
        }
        if (blocks.isEmpty()) {
            item(key = "empty") {
                Text(text = emptyText, style = MaterialTheme.typography.bodyLarge)
            }
        }
        items(blocks, key = { it.id }) { block ->
            BlockCard(
                block = block,
                onEditTime = { target = SheetTarget(block.id, SheetKind.TIME) },
                onEditDuration = { target = SheetTarget(block.id, SheetKind.DURATION) },
                onEditDetails = { target = SheetTarget(block.id, SheetKind.DETAILS) },
                onToggleReminder = { onUpdate(block.copy(reminderEnabled = it)) },
                onDelete = { onDelete(block.id) },
            )
        }
    }

    val current = target
    val editing = blocks.firstOrNull { it.id == current?.blockId }
    if (current != null && editing != null) {
        val close = { target = null }
        when (current.kind) {
            SheetKind.TIME -> TimeSheet(
                initial = editing.start,
                onConfirm = {
                    onUpdate(editing.copy(start = it))
                    close()
                },
                onDismiss = close,
            )

            SheetKind.DURATION -> DurationSheet(
                initialMinutes = editing.durationMinutes,
                onConfirm = {
                    onUpdate(editing.copy(durationMinutes = it))
                    close()
                },
                onDismiss = close,
            )

            SheetKind.DETAILS -> DetailsSheet(
                initialType = editing.type,
                initialTitle = editing.title,
                onConfirm = { type, title ->
                    onUpdate(editing.copy(type = type, title = title))
                    close()
                },
                onDismiss = close,
            )
        }
    }
}

@Composable
private fun BlockCard(
    block: TimeBlock,
    onEditTime: () -> Unit,
    onEditDuration: () -> Unit,
    onEditDetails: () -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val name = blockNameText(block.type, block.title)
    val start = block.start.formatHm()
    val range = formatRange(start, formatMinuteOfDay(block.endMinuteOfDay))
    val duration = durationText(block.durationMinutes)
    val editTimeDescription = stringResource(R.string.block_edit_time) + " " + isolateLtr(start)
    val editDurationDescription = stringResource(R.string.block_edit_duration) + " " + duration
    val editDetailsDescription = stringResource(R.string.block_edit_details_named, name)
    val deleteDescription = stringResource(R.string.block_delete_named, name)
    val reminderDescription = stringResource(
        if (block.reminderEnabled) R.string.block_reminder_on else R.string.block_reminder_off,
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, style = MaterialTheme.typography.titleMedium)
                    Text(text = range, style = MaterialTheme.typography.bodyMedium)
                    if (block.crossesMidnight) {
                        Text(
                            text = stringResource(R.string.block_overnight),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                IconButton(onClick = onEditDetails) {
                    Icon(Icons.Default.Edit, contentDescription = editDetailsDescription)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = deleteDescription)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = onEditTime,
                    label = { Text(stringResource(R.string.block_start_value, isolateLtr(start))) },
                    modifier = Modifier.semantics { contentDescription = editTimeDescription },
                )
                AssistChip(
                    onClick = onEditDuration,
                    label = { Text(stringResource(R.string.block_duration_value, duration)) },
                    modifier = Modifier.semantics { contentDescription = editDurationDescription },
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = block.reminderEnabled,
                    onCheckedChange = onToggleReminder,
                    modifier = Modifier.semantics { contentDescription = reminderDescription },
                )
            }
        }
    }
}

@Composable
private fun SheetColumn(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionTitle(text = title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
private fun SheetButtons(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.sheet_cancel))
        }
        Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.sheet_confirm))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSheet(initial: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        SheetColumn(title = stringResource(R.string.sheet_time_title)) {
            TimePicker(state = pickerState)
            SheetButtons(
                onConfirm = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) },
                onCancel = onDismiss,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DurationSheet(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableIntStateOf(initialMinutes.coerceIn(DURATION_STEP, MINUTES_PER_DAY)) }
    val sliderDescription = stringResource(R.string.sheet_duration_title)
    val sliderState = durationText(minutes)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        SheetColumn(title = stringResource(R.string.sheet_duration_title)) {
            Text(text = durationText(minutes), style = MaterialTheme.typography.headlineMedium)
            Slider(
                value = minutes.toFloat(),
                onValueChange = { minutes = snapToStep(it) },
                valueRange = DURATION_STEP.toFloat()..MINUTES_PER_DAY.toFloat(),
                steps = MINUTES_PER_DAY / DURATION_STEP - 2,
                modifier = Modifier.semantics {
                    contentDescription = sliderDescription
                    stateDescription = sliderState
                },
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DURATION_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = minutes == preset,
                        onClick = { minutes = preset },
                        label = { Text(durationText(preset)) },
                    )
                }
            }
            SheetButtons(onConfirm = { onConfirm(minutes) }, onCancel = onDismiss)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailsSheet(
    initialType: BlockType,
    initialTitle: String,
    onConfirm: (BlockType, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by remember { mutableStateOf(initialType) }
    var title by remember { mutableStateOf(initialTitle) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        SheetColumn(title = stringResource(R.string.sheet_details_title)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BlockType.entries.forEach { entry ->
                    FilterChip(
                        selected = entry == type,
                        onClick = { type = entry },
                        label = { Text(stringResource(entry.labelRes())) },
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(ValidateBlock.MAX_TITLE_LENGTH) },
                label = { Text(stringResource(R.string.sheet_title_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SheetButtons(onConfirm = { onConfirm(type, title.trim()) }, onCancel = onDismiss)
        }
    }
}

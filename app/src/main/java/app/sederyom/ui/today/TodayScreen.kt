package app.sederyom.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sederyom.R
import app.sederyom.domain.model.DaySegment
import app.sederyom.ui.components.SectionTitle
import app.sederyom.ui.components.blockNameText
import app.sederyom.ui.components.durationText
import app.sederyom.ui.components.formatDate
import app.sederyom.ui.components.formatMinuteOfDay
import app.sederyom.ui.components.formatRange
import app.sederyom.ui.components.isolateLtr
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(viewModel: TodayViewModel, onEditDay: (LocalDate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (current != null) formatDate(current.date) else stringResource(R.string.app_name)) },
                actions = {
                    if (current != null) {
                        IconButton(onClick = { onEditDay(current.date) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.today_edit_day))
                        }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (current != null) {
            TodayContent(
                state = current,
                onPrevious = viewModel::onPreviousDay,
                onNext = viewModel::onNextDay,
                onToday = viewModel::onBackToToday,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun TodayContent(
    state: TodayUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "navigation") {
            DayNavigation(isToday = state.isToday, onPrevious = onPrevious, onNext = onNext, onToday = onToday)
        }
        if (state.isToday) {
            item(key = "summary") { SummaryCard(state.current, state.next) }
        }
        if (state.isOverride) {
            item(key = "override") {
                Text(
                    text = stringResource(R.string.today_overridden),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        if (state.segments.isEmpty()) {
            item(key = "empty") { Text(stringResource(R.string.today_empty)) }
        }
        itemsIndexed(state.segments, key = { index, _ -> index }) { _, segment ->
            SegmentRow(segment = segment, isCurrent = segment == state.current)
        }
    }
}

@Composable
private fun DayNavigation(isToday: Boolean, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.today_prev_day))
        }
        if (!isToday) {
            TextButton(onClick = onToday) { Text(stringResource(R.string.today_go_to_today)) }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.today_next_day))
        }
    }
}

@Composable
private fun SummaryCard(current: DaySegment?, next: DaySegment?) {
    val nowText = if (current != null) {
        stringResource(
            R.string.today_now_value,
            blockNameText(current.block.type, current.block.title),
            isolateLtr(formatMinuteOfDay(current.endMinute)),
        )
    } else {
        stringResource(R.string.today_nothing_now)
    }
    val nextText = if (next != null) {
        stringResource(
            R.string.today_next_value,
            blockNameText(next.block.type, next.block.title),
            isolateLtr(formatMinuteOfDay(next.startMinute)),
        )
    } else {
        stringResource(R.string.today_nothing_next)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(stringResource(R.string.today_now), style = MaterialTheme.typography.labelLarge)
            Text(nowText, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            SectionTitle(stringResource(R.string.today_next), style = MaterialTheme.typography.labelLarge)
            Text(nextText, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SegmentRow(segment: DaySegment, isCurrent: Boolean) {
    val name = blockNameText(segment.block.type, segment.block.title)
    val range = formatRange(formatMinuteOfDay(segment.startMinute), formatMinuteOfDay(segment.endMinute))
    val duration = durationText(segment.durationMinutes)
    val description = stringResource(R.string.segment_description, name, range, duration)
    val container = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = range, style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.bodyLarge)
                if (segment.continuesFromPrevious) {
                    Text(stringResource(R.string.segment_from_previous), style = MaterialTheme.typography.labelMedium)
                }
                if (segment.continuesToNext) {
                    Text(stringResource(R.string.segment_to_next), style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(text = duration, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

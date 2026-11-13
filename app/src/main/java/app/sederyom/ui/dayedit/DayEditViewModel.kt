package app.sederyom.ui.dayedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sederyom.R
import app.sederyom.domain.model.TimeBlock
import app.sederyom.domain.usecase.ObserveSchedule
import app.sederyom.domain.usecase.ResetDayOverride
import app.sederyom.domain.usecase.ResolveDayPlan
import app.sederyom.domain.usecase.SaveDayOverride
import app.sederyom.domain.usecase.SaveResult
import app.sederyom.domain.usecase.SuggestNewBlock
import app.sederyom.domain.usecase.ValidateBlock
import app.sederyom.domain.usecase.ValidationResult
import app.sederyom.ui.components.UiEvent
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DayEditUiState(
    val loaded: Boolean = false,
    val draft: List<TimeBlock> = emptyList(),
    val hasOverride: Boolean = false,
)

/** Edits a draft copy of one day. Nothing is stored until [onSave]; [onReset] deletes the override. */
class DayEditViewModel(
    private val date: LocalDate,
    observeSchedule: ObserveSchedule,
    resolveDayPlan: ResolveDayPlan,
    private val validateBlock: ValidateBlock,
    private val suggestNewBlock: SuggestNewBlock,
    private val saveDayOverride: SaveDayOverride,
    private val resetDayOverride: ResetDayOverride,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DayEditUiState())
    val state: StateFlow<DayEditUiState> = mutableState.asStateFlow()

    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = eventChannel.receiveAsFlow()

    private var initialDraft: List<TimeBlock> = emptyList()

    init {
        viewModelScope.launch {
            val snapshot = observeSchedule().first()
            val plan = resolveDayPlan.plan(date, snapshot.template, snapshot.overrides)
            initialDraft = plan.blocks
            mutableState.value = DayEditUiState(loaded = true, draft = plan.blocks, hasOverride = plan.isOverride)
        }
    }

    fun onUpdate(block: TimeBlock) {
        val draft = mutableState.value.draft
        val others = draft.filter { it.id != block.id }
        when (val result = validateBlock(block, others)) {
            ValidationResult.Valid -> updateDraft { list -> list.map { if (it.id == block.id) block else it } }
            is ValidationResult.Invalid -> viewModelScope.launch { eventChannel.send(UiEvent.Error(result.errors)) }
        }
    }

    fun onDelete(id: Long) = updateDraft { list -> list.filter { it.id != id } }

    fun onAdd() {
        val draft = mutableState.value.draft
        val newId = (draft.maxOfOrNull { it.id } ?: 0L) + 1
        val suggestion = suggestNewBlock(draft, newId)
        if (suggestion == null) {
            viewModelScope.launch { eventChannel.send(UiEvent.Message(R.string.template_no_room)) }
        } else {
            updateDraft { list -> list + suggestion }
        }
    }

    fun onSave() {
        val draft = mutableState.value.draft
        viewModelScope.launch {
            if (draft == initialDraft) {
                eventChannel.send(UiEvent.Done)
                return@launch
            }
            when (val result = saveDayOverride(date, draft)) {
                is SaveResult.Saved -> eventChannel.send(UiEvent.Done)
                is SaveResult.Rejected -> eventChannel.send(UiEvent.Error(result.errors))
            }
        }
    }

    fun onReset() {
        viewModelScope.launch {
            resetDayOverride(date)
            eventChannel.send(UiEvent.Done)
        }
    }

    private fun updateDraft(transform: (List<TimeBlock>) -> List<TimeBlock>) {
        mutableState.update { it.copy(draft = transform(it.draft).sortedBy { block -> block.startMinuteOfDay }) }
    }
}

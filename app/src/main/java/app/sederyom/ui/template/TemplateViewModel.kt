package app.sederyom.ui.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sederyom.R
import app.sederyom.domain.model.TimeBlock
import app.sederyom.domain.usecase.DeleteTemplateBlock
import app.sederyom.domain.usecase.ObserveSchedule
import app.sederyom.domain.usecase.SaveResult
import app.sederyom.domain.usecase.SaveTemplateBlock
import app.sederyom.domain.usecase.SuggestNewBlock
import app.sederyom.ui.components.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplateViewModel(
    observeSchedule: ObserveSchedule,
    private val saveTemplateBlock: SaveTemplateBlock,
    private val deleteTemplateBlock: DeleteTemplateBlock,
    private val suggestNewBlock: SuggestNewBlock,
) : ViewModel() {
    val blocks: StateFlow<List<TimeBlock>> = observeSchedule()
        .map { snapshot -> snapshot.template.sortedBy { it.startMinuteOfDay } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = eventChannel.receiveAsFlow()

    fun onUpdate(block: TimeBlock) {
        viewModelScope.launch { save(block) }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch { deleteTemplateBlock(id) }
    }

    fun onAdd() {
        viewModelScope.launch {
            val suggestion = suggestNewBlock(blocks.value, id = 0L)
            if (suggestion == null) {
                eventChannel.send(UiEvent.Message(R.string.template_no_room))
            } else {
                save(suggestion)
            }
        }
    }

    private suspend fun save(block: TimeBlock) {
        val result = saveTemplateBlock(block)
        if (result is SaveResult.Rejected) eventChannel.send(UiEvent.Error(result.errors))
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

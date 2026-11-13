package app.sederyom.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sederyom.domain.model.DaySegment
import app.sederyom.domain.usecase.FindNowAndNext
import app.sederyom.domain.usecase.NowAndNext
import app.sederyom.domain.usecase.ObserveSchedule
import app.sederyom.domain.usecase.ResolveDayPlan
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class TodayUiState(
    val date: LocalDate,
    val isToday: Boolean,
    val isOverride: Boolean,
    val segments: List<DaySegment>,
    val current: DaySegment?,
    val next: DaySegment?,
)

class TodayViewModel(
    observeSchedule: ObserveSchedule,
    private val resolveDayPlan: ResolveDayPlan,
    private val findNowAndNext: FindNowAndNext,
) : ViewModel() {
    private val dayOffset = MutableStateFlow(0)

    /** Emits the current time now and every 30 seconds so "now" and "next" stay fresh. */
    private val ticker: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(TICK_MILLIS)
        }
    }

    val state: StateFlow<TodayUiState?> = combine(observeSchedule(), dayOffset, ticker) { snapshot, offset, now ->
        val date = now.toLocalDate().plusDays(offset.toLong())
        val plan = resolveDayPlan.plan(date, snapshot.template, snapshot.overrides)
        val segments = resolveDayPlan.timeline(date, snapshot.template, snapshot.overrides)
        val isToday = offset == 0
        val nowAndNext = if (isToday) findNowAndNext(segments, now.toLocalTime()) else NowAndNext(null, null)
        TodayUiState(date, isToday, plan.isOverride, segments, nowAndNext.current, nowAndNext.next)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun onPreviousDay() = dayOffset.update { it - 1 }

    fun onNextDay() = dayOffset.update { it + 1 }

    fun onBackToToday() {
        dayOffset.value = 0
    }

    private companion object {
        const val TICK_MILLIS = 30_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

package app.sederyom.domain.usecase

import app.sederyom.domain.model.DaySegment
import java.time.LocalTime

data class NowAndNext(
    val current: DaySegment?,
    val next: DaySegment?,
)

class FindNowAndNext {
    /** [timeline] is one day's segments (see ResolveDayPlan.timeline). [now] is a time on that day. */
    operator fun invoke(timeline: List<DaySegment>, now: LocalTime): NowAndNext {
        val minute = now.hour * 60 + now.minute
        val current = timeline.firstOrNull { it.startMinute <= minute && minute < it.endMinute }
        val next = timeline.filter { it.startMinute > minute }.minByOrNull { it.startMinute }
        return NowAndNext(current, next)
    }
}

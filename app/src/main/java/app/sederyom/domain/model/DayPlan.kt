package app.sederyom.domain.model

import java.time.LocalDate

/** The slice of a [TimeBlock] that is visible on one calendar day. Minutes are 0..1440. */
data class DaySegment(
    val block: TimeBlock,
    val startMinute: Int,
    val endMinute: Int,
    val continuesFromPrevious: Boolean,
    val continuesToNext: Boolean,
) {
    val durationMinutes: Int
        get() = endMinute - startMinute
}

/** A full replacement of the template for one specific date. */
data class DayOverride(
    val date: LocalDate,
    val blocks: List<TimeBlock>,
)

/** The blocks that start on [date], sorted by start time. */
data class DayPlan(
    val date: LocalDate,
    val blocks: List<TimeBlock>,
    val isOverride: Boolean,
)

data class ScheduleSnapshot(
    val template: List<TimeBlock>,
    val overrides: Map<LocalDate, DayOverride>,
)

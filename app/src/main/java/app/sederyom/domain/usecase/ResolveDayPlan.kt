package app.sederyom.domain.usecase

import app.sederyom.domain.model.DayOverride
import app.sederyom.domain.model.DayPlan
import app.sederyom.domain.model.DaySegment
import app.sederyom.domain.model.TimeBlock
import java.time.LocalDate

class ResolveDayPlan {
    /** An override replaces the template completely for its date. */
    fun plan(date: LocalDate, template: List<TimeBlock>, overrides: Map<LocalDate, DayOverride>): DayPlan {
        val override = overrides[date]
        val blocks = override?.blocks ?: template
        return DayPlan(
            date = date,
            blocks = blocks.sortedBy { it.startMinuteOfDay },
            isOverride = override != null,
        )
    }

    /**
     * Everything visible on [date]: blocks that start on it plus the tails of the previous day's
     * overnight blocks, ordered by start minute.
     */
    fun timeline(date: LocalDate, template: List<TimeBlock>, overrides: Map<LocalDate, DayOverride>): List<DaySegment> {
        val today = plan(date, template, overrides)
        val yesterday = plan(date.minusDays(1), template, overrides)
        val tails = yesterday.blocks.mapNotNull { it.segmentOnNextDay() }
        val heads = today.blocks.map { it.segmentOnStartDay() }
        return (tails + heads).sortedWith(compareBy<DaySegment> { it.startMinute }.thenBy { it.endMinute })
    }
}

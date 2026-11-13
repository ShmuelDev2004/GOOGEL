package app.sederyom.domain.usecase

import app.sederyom.domain.model.BlockType
import app.sederyom.domain.model.DayOverride
import app.sederyom.domain.model.TimeBlock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** One reminder to schedule: it fires at [triggerAt] for the block that starts at [blockStart]. */
data class ReminderEntry(
    val date: LocalDate,
    val blockId: Long,
    val type: BlockType,
    val title: String,
    val blockStart: LocalDateTime,
    val triggerAt: LocalDateTime,
) {
    val key: String
        get() = "${date.toEpochDay()}:$blockId"

    /** Stable per day and block, so rescheduling replaces the same alarm instead of adding another. */
    val requestCode: Int
        get() = key.hashCode()

    val leadMinutes: Int
        get() = Duration.between(triggerAt, blockStart).toMinutes().toInt()
}

/** Computes the reminders for the next [PLAN_DAYS] days, starting with the date of `now`. */
class ComputeReminderPlan(private val resolveDayPlan: ResolveDayPlan = ResolveDayPlan()) {
    operator fun invoke(
        template: List<TimeBlock>,
        overrides: Map<LocalDate, DayOverride>,
        now: LocalDateTime,
        leadTimeMinutes: Int,
        days: Int = PLAN_DAYS,
    ): List<ReminderEntry> {
        val firstDate = now.toLocalDate()
        return (0 until days)
            .flatMap { offset ->
                val date = firstDate.plusDays(offset.toLong())
                resolveDayPlan.plan(date, template, overrides).blocks
                    .filter { it.reminderEnabled }
                    .map { block ->
                        val start = date.atTime(block.start)
                        ReminderEntry(
                            date = date,
                            blockId = block.id,
                            type = block.type,
                            title = block.title,
                            blockStart = start,
                            triggerAt = start.minusMinutes(leadTimeMinutes.toLong()),
                        )
                    }
            }
            .filter { it.triggerAt.isAfter(now) }
            .sortedWith(compareBy<ReminderEntry> { it.triggerAt }.thenBy { it.blockId })
    }

    companion object {
        const val PLAN_DAYS = 7
    }
}

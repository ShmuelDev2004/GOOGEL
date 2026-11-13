package app.sederyom.domain.usecase

import app.sederyom.domain.model.DayOverride
import app.sederyom.domain.model.TimeBlock
import app.sederyom.domain.repository.ScheduleRepository
import java.time.LocalDate

/** Writes a [DayOverride] holding the given blocks for one date, after validating all of them. */
class SaveDayOverride(
    private val repository: ScheduleRepository,
    private val validateBlock: ValidateBlock,
    private val rescheduler: ReminderRescheduler,
) {
    suspend operator fun invoke(date: LocalDate, blocks: List<TimeBlock>): SaveResult {
        val errors = validateBlock.validateEach(blocks).values.flatten().distinct()
        if (errors.isNotEmpty()) return SaveResult.Rejected(errors)
        repository.saveOverride(DayOverride(date, blocks))
        rescheduler.reschedule()
        return SaveResult.Saved(date.toEpochDay())
    }
}

/** "Reset to template": deleting the override makes the date follow the template again. */
class ResetDayOverride(
    private val repository: ScheduleRepository,
    private val rescheduler: ReminderRescheduler,
) {
    suspend operator fun invoke(date: LocalDate) {
        repository.deleteOverride(date)
        rescheduler.reschedule()
    }
}

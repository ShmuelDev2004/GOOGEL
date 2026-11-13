package app.sederyom.domain.usecase

import app.sederyom.domain.model.TimeBlock
import app.sederyom.domain.repository.ScheduleRepository

/** Validates one template block on its own and stores it. Other blocks are never touched. */
class SaveTemplateBlock(
    private val repository: ScheduleRepository,
    private val validateBlock: ValidateBlock,
    private val rescheduler: ReminderRescheduler,
) {
    suspend operator fun invoke(block: TimeBlock): SaveResult {
        val others = repository.getTemplate().filter { it.id != block.id }
        return when (val validation = validateBlock(block, others)) {
            is ValidationResult.Invalid -> SaveResult.Rejected(validation.errors)
            ValidationResult.Valid -> {
                val id = repository.upsertTemplateBlock(block)
                rescheduler.reschedule()
                SaveResult.Saved(id)
            }
        }
    }
}

class DeleteTemplateBlock(
    private val repository: ScheduleRepository,
    private val rescheduler: ReminderRescheduler,
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteTemplateBlock(id)
        rescheduler.reschedule()
    }
}

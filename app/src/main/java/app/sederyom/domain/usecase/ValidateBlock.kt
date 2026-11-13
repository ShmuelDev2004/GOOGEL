package app.sederyom.domain.usecase

import app.sederyom.domain.model.MINUTES_PER_DAY
import app.sederyom.domain.model.TimeBlock

sealed interface BlockError {
    data object DurationTooShort : BlockError

    data object DurationTooLong : BlockError

    data object TitleTooLong : BlockError

    data class Overlaps(val other: TimeBlock) : BlockError
}

sealed interface ValidationResult {
    data object Valid : ValidationResult

    data class Invalid(val errors: List<BlockError>) : ValidationResult
}

class ValidateBlock {
    /** Validates [block] against [others]. [others] must not contain [block] itself. */
    operator fun invoke(block: TimeBlock, others: List<TimeBlock>): ValidationResult {
        val errors = mutableListOf<BlockError>()
        if (block.durationMinutes < MIN_DURATION_MINUTES) errors += BlockError.DurationTooShort
        if (block.durationMinutes > MINUTES_PER_DAY) errors += BlockError.DurationTooLong
        if (block.title.length > MAX_TITLE_LENGTH) errors += BlockError.TitleTooLong
        if (errors.isEmpty()) {
            others.filter { overlaps(block, it) }.forEach { errors += BlockError.Overlaps(it) }
        }
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /** Validates every block against all the others. The result maps list index to its errors. */
    fun validateEach(blocks: List<TimeBlock>): Map<Int, List<BlockError>> {
        val result = mutableMapOf<Int, List<BlockError>>()
        blocks.forEachIndexed { index, block ->
            val others = blocks.filterIndexed { otherIndex, _ -> otherIndex != index }
            val validation = invoke(block, others)
            if (validation is ValidationResult.Invalid) result[index] = validation.errors
        }
        return result
    }

    /**
     * Two blocks overlap when their intervals intersect on the 24 hour circle.
     * The second block is also tested shifted by one day either way so overnight blocks are handled.
     * Blocks that merely touch (one ends exactly when the other starts) do not overlap.
     */
    private fun overlaps(a: TimeBlock, b: TimeBlock): Boolean {
        val aStart = a.startMinuteOfDay
        val aEnd = aStart + a.durationMinutes
        return DAY_SHIFTS.any { shift ->
            val bStart = b.startMinuteOfDay + shift
            val bEnd = bStart + b.durationMinutes
            aStart < bEnd && bStart < aEnd
        }
    }

    companion object {
        const val MIN_DURATION_MINUTES = 1
        const val MAX_TITLE_LENGTH = 60
        private val DAY_SHIFTS = listOf(-MINUTES_PER_DAY, 0, MINUTES_PER_DAY)
    }
}

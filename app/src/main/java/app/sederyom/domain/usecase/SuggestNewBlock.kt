package app.sederyom.domain.usecase

import app.sederyom.domain.model.BlockType
import app.sederyom.domain.model.TimeBlock
import java.time.LocalTime

/** Proposes a default 30 minute block in the first free slot, or null when the day is full. */
class SuggestNewBlock(private val validateBlock: ValidateBlock = ValidateBlock()) {
    operator fun invoke(existing: List<TimeBlock>, id: Long): TimeBlock? {
        val afterBlocks = existing.sortedBy { it.startMinuteOfDay }.map { it.end }
        val fullHours = (0 until HOURS_PER_DAY).map { LocalTime.of(it, 0) }
        val candidates = (afterBlocks + DEFAULT_START + fullHours).distinct()
        return candidates
            .map { start -> TimeBlock(id, BlockType.OTHER, "", start, DEFAULT_DURATION_MINUTES, true) }
            .firstOrNull { validateBlock(it, existing) is ValidationResult.Valid }
    }

    private companion object {
        const val DEFAULT_DURATION_MINUTES = 30
        const val HOURS_PER_DAY = 24
        val DEFAULT_START: LocalTime = LocalTime.of(8, 0)
    }
}

package app.sederyom.domain.usecase

import app.sederyom.domain.block
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateBlockTest {
    private val validate = ValidateBlock()

    @Test
    fun validBlockWithNoOthers() {
        assertEquals(ValidationResult.Valid, validate(block(1, "08:00", 60), emptyList()))
    }

    @Test
    fun zeroDurationIsRejected() {
        val result = validate(block(1, "08:00", 0), emptyList())
        assertEquals(ValidationResult.Invalid(listOf(BlockError.DurationTooShort)), result)
    }

    @Test
    fun durationOverOneDayIsRejected() {
        val result = validate(block(1, "08:00", 1441), emptyList())
        assertEquals(ValidationResult.Invalid(listOf(BlockError.DurationTooLong)), result)
    }

    @Test
    fun fullDayDurationIsAllowed() {
        assertEquals(ValidationResult.Valid, validate(block(1, "00:00", 1440), emptyList()))
    }

    @Test
    fun longTitleIsRejected() {
        val title = "x".repeat(ValidateBlock.MAX_TITLE_LENGTH + 1)
        val result = validate(block(1, "08:00", 30, title = title), emptyList())
        assertEquals(ValidationResult.Invalid(listOf(BlockError.TitleTooLong)), result)
    }

    @Test
    fun overlappingBlocksAreRejected() {
        val other = block(2, "08:30", 60)
        val result = validate(block(1, "08:00", 60), listOf(other))
        assertEquals(ValidationResult.Invalid(listOf(BlockError.Overlaps(other))), result)
    }

    @Test
    fun touchingBlocksAreAllowed() {
        val other = block(2, "09:00", 60)
        assertEquals(ValidationResult.Valid, validate(block(1, "08:00", 60), listOf(other)))
    }

    @Test
    fun overnightBlockOverlapsEarlyMorningBlock() {
        val sleep = block(1, "23:00", 420)
        val early = block(2, "05:00", 60)
        assertTrue(validate(sleep, listOf(early)) is ValidationResult.Invalid)
        assertTrue(validate(early, listOf(sleep)) is ValidationResult.Invalid)
    }

    @Test
    fun overnightBlockDoesNotOverlapBlockAfterItEnds() {
        val sleep = block(1, "23:00", 420)
        val later = block(2, "06:00", 60)
        assertEquals(ValidationResult.Valid, validate(sleep, listOf(later)))
        assertEquals(ValidationResult.Valid, validate(later, listOf(sleep)))
    }

    @Test
    fun overnightBlockOverlapsLateEveningBlock() {
        val sleep = block(1, "23:00", 420)
        val evening = block(2, "22:30", 60)
        assertTrue(validate(sleep, listOf(evening)) is ValidationResult.Invalid)
    }

    @Test
    fun twoOvernightBlocksOverlap() {
        val a = block(1, "22:00", 480)
        val b = block(2, "01:00", 600)
        assertTrue(validate(a, listOf(b)) is ValidationResult.Invalid)
    }

    @Test
    fun validateEachReportsIndexesOfBadBlocks() {
        val blocks = listOf(block(1, "08:00", 60), block(2, "08:30", 60), block(3, "12:00", 30))
        val result = validate.validateEach(blocks)
        assertEquals(setOf(0, 1), result.keys)
    }
}

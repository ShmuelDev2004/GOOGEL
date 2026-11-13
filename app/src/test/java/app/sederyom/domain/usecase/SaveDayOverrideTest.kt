package app.sederyom.domain.usecase

import app.sederyom.domain.FakeScheduleRepository
import app.sederyom.domain.block
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveDayOverrideTest {
    private val date = LocalDate.of(2026, 11, 13)

    @Test
    fun savingWritesAnOverrideWithAllBlocks() = runBlocking {
        val repository = FakeScheduleRepository(listOf(block(1, "08:00", 60)))
        var rescheduled = 0
        val save = SaveDayOverride(repository, ValidateBlock()) { rescheduled++ }

        val result = save(date, listOf(block(1, "09:00", 60), block(2, "12:00", 30)))

        assertTrue(result is SaveResult.Saved)
        assertEquals(2, repository.overrides.getValue(date).blocks.size)
        assertEquals(1, rescheduled)
        assertEquals(1, repository.blocks.size)
    }

    @Test
    fun overlappingBlocksAreNotSaved() = runBlocking {
        val repository = FakeScheduleRepository()
        val save = SaveDayOverride(repository, ValidateBlock()) { }

        val result = save(date, listOf(block(1, "09:00", 60), block(2, "09:30", 30)))

        assertTrue(result is SaveResult.Rejected)
        assertTrue(repository.overrides.isEmpty())
    }

    @Test
    fun resetDeletesTheOverrideOnly() = runBlocking {
        val repository = FakeScheduleRepository(listOf(block(1, "08:00", 60)))
        val save = SaveDayOverride(repository, ValidateBlock()) { }
        save(date, listOf(block(9, "10:00", 15)))

        var rescheduled = 0
        ResetDayOverride(repository) { rescheduled++ }(date)

        assertTrue(repository.overrides.isEmpty())
        assertEquals(1, repository.blocks.size)
        assertEquals(1, rescheduled)
    }
}

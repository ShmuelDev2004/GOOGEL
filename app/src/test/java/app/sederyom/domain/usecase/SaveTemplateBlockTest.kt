package app.sederyom.domain.usecase

import app.sederyom.domain.FakeScheduleRepository
import app.sederyom.domain.block
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveTemplateBlockTest {
    @Test
    fun savesValidBlockAndReschedules() = runBlocking {
        val repository = FakeScheduleRepository(listOf(block(1, "08:00", 60)))
        var rescheduled = 0
        val save = SaveTemplateBlock(repository, ValidateBlock()) { rescheduled++ }

        val result = save(block(0, "10:00", 30))

        assertEquals(SaveResult.Saved(100), result)
        assertEquals(2, repository.blocks.size)
        assertEquals(1, rescheduled)
    }

    @Test
    fun rejectsOverlapWithoutTouchingStorage() = runBlocking {
        val repository = FakeScheduleRepository(listOf(block(1, "08:00", 60)))
        var rescheduled = 0
        val save = SaveTemplateBlock(repository, ValidateBlock()) { rescheduled++ }

        val result = save(block(0, "08:30", 30))

        assertTrue(result is SaveResult.Rejected)
        assertEquals(1, repository.blocks.size)
        assertEquals(0, rescheduled)
    }

    @Test
    fun editingABlockDoesNotCollideWithItself() = runBlocking {
        val repository = FakeScheduleRepository(listOf(block(1, "08:00", 60)))
        val save = SaveTemplateBlock(repository, ValidateBlock()) { }

        assertEquals(SaveResult.Saved(1), save(block(1, "08:15", 60)))
    }

    @Test
    fun deleteRemovesBlockAndReschedules() = runBlocking {
        val repository = FakeScheduleRepository(listOf(block(1, "08:00", 60)))
        var rescheduled = 0
        DeleteTemplateBlock(repository) { rescheduled++ }(1)

        assertTrue(repository.blocks.isEmpty())
        assertEquals(1, rescheduled)
    }
}

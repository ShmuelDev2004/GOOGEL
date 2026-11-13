package app.sederyom.data.db

import app.sederyom.domain.model.BlockType
import app.sederyom.domain.model.DayOverride
import app.sederyom.domain.model.TimeBlock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomScheduleRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomScheduleRepository

    @Before
    fun setUp() {
        database = createInMemoryDatabase()
        repository = RoomScheduleRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun block(id: Long, start: String) = TimeBlock(id, BlockType.WORK, "", LocalTime.parse(start), 60, true)

    @Test
    fun upsertReturnsGeneratedIdForNewBlock() = runBlocking {
        val id = repository.upsertTemplateBlock(block(0, "08:00"))
        assertTrue(id > 0)
        assertEquals(id, repository.getTemplate().single().id)
    }

    @Test
    fun upsertKeepsIdForExistingBlock() = runBlocking {
        val id = repository.upsertTemplateBlock(block(0, "08:00"))
        assertEquals(id, repository.upsertTemplateBlock(block(id, "09:00")))
        assertEquals(LocalTime.of(9, 0), repository.getTemplate().single().start)
    }

    @Test
    fun overridesRoundTripThroughDomainModel() = runBlocking {
        val date = LocalDate.of(2026, 11, 13)
        val override = DayOverride(date, listOf(block(2, "10:00"), block(1, "08:00")))
        repository.saveOverride(override)

        val loaded = repository.observeOverrides().first().getValue(date)
        assertEquals(listOf(1L, 2L), loaded.blocks.map { it.id })
        assertEquals(date, loaded.date)
    }

    @Test
    fun deleteOverrideRemovesIt() = runBlocking {
        val date = LocalDate.of(2026, 11, 13)
        repository.saveOverride(DayOverride(date, listOf(block(1, "08:00"))))
        repository.deleteOverride(date)
        assertTrue(repository.getOverrides(date, date).isEmpty())
    }

    @Test
    fun replaceAllSwapsEverything() = runBlocking {
        val date = LocalDate.of(2026, 11, 13)
        repository.upsertTemplateBlock(block(0, "07:00"))
        repository.saveOverride(DayOverride(date, listOf(block(1, "08:00"))))

        val newDate = LocalDate.of(2026, 12, 1)
        repository.replaceAll(listOf(block(5, "12:00")), listOf(DayOverride(newDate, emptyList())))

        assertEquals(listOf(5L), repository.getTemplate().map { it.id })
        assertEquals(setOf(newDate), repository.observeOverrides().first().keys)
    }
}

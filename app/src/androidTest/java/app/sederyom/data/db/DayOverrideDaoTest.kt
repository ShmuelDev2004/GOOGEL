package app.sederyom.data.db

import app.sederyom.data.db.dao.DayOverrideDao
import app.sederyom.data.db.entity.DayOverrideBlockEntity
import app.sederyom.data.db.entity.DayOverrideEntity
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DayOverrideDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: DayOverrideDao

    @Before
    fun setUp() {
        database = createInMemoryDatabase()
        dao = database.dayOverrideDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun blockEntity(day: Long, id: Long, start: String) = DayOverrideBlockEntity(
        epochDay = day,
        blockId = id,
        type = "STUDY",
        title = "",
        startTime = LocalTime.parse(start),
        durationMinutes = 45,
        reminderEnabled = false,
    )

    @Test
    fun replaceDayStoresBlocksWithTheDay() = runBlocking {
        dao.replaceDay(DayOverrideEntity(100), listOf(blockEntity(100, 1, "08:00"), blockEntity(100, 2, "10:00")))
        val loaded = dao.getForDay(100)
        assertNotNull(loaded)
        assertEquals(2, loaded?.blocks?.size)
        assertEquals(false, loaded?.blocks?.first()?.reminderEnabled)
    }

    @Test
    fun replaceDayOverwritesPreviousBlocks() = runBlocking {
        dao.replaceDay(DayOverrideEntity(100), listOf(blockEntity(100, 1, "08:00")))
        dao.replaceDay(DayOverrideEntity(100), listOf(blockEntity(100, 5, "09:00")))
        val loaded = dao.getForDay(100)
        assertEquals(listOf(5L), loaded?.blocks?.map { it.blockId })
    }

    @Test
    fun deleteDayCascadesToBlocks() = runBlocking {
        dao.replaceDay(DayOverrideEntity(100), listOf(blockEntity(100, 1, "08:00")))
        dao.deleteDay(100)
        assertNull(dao.getForDay(100))
        assertEquals(0, database.query("SELECT * FROM day_override_blocks", null).count)
    }

    @Test
    fun getBetweenIsInclusive() = runBlocking {
        listOf(99L, 100L, 101L, 102L).forEach { dao.replaceDay(DayOverrideEntity(it), emptyList()) }
        assertEquals(listOf(100L, 101L), dao.getBetween(100, 101).map { it.day.epochDay }.sorted())
    }

    @Test
    fun emptyOverrideIsKept() = runBlocking {
        dao.replaceDay(DayOverrideEntity(7), emptyList())
        assertEquals(0, dao.getForDay(7)?.blocks?.size)
    }
}

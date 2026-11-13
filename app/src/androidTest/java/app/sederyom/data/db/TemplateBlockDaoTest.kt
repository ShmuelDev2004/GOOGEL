package app.sederyom.data.db

import app.sederyom.data.db.dao.TemplateBlockDao
import app.sederyom.data.db.entity.TemplateBlockEntity
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TemplateBlockDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: TemplateBlockDao

    @Before
    fun setUp() {
        database = createInMemoryDatabase()
        dao = database.templateBlockDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(start: String, id: Long = 0) = TemplateBlockEntity(
        id = id,
        type = "WORK",
        title = "t",
        startTime = LocalTime.parse(start),
        durationMinutes = 30,
        reminderEnabled = true,
    )

    @Test
    fun upsertInsertsAndReturnsGeneratedId() = runBlocking {
        val id = dao.upsert(entity("08:00"))
        assertEquals(1L, id)
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun upsertWithExistingIdUpdatesTheRow() = runBlocking {
        val id = dao.upsert(entity("08:00"))
        dao.upsert(entity("09:15", id))
        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals(LocalTime.of(9, 15), all.first().startTime)
    }

    @Test
    fun rowsAreOrderedByStartTime() = runBlocking {
        dao.upsert(entity("12:00"))
        dao.upsert(entity("07:30"))
        dao.upsert(entity("23:45"))
        val starts = dao.observeAll().first().map { it.startTime }
        assertEquals(listOf(LocalTime.of(7, 30), LocalTime.of(12, 0), LocalTime.of(23, 45)), starts)
    }

    @Test
    fun deleteByIdRemovesOnlyThatRow() = runBlocking {
        val first = dao.upsert(entity("08:00"))
        dao.upsert(entity("09:00"))
        dao.deleteById(first)
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun deleteAllClearsTable() = runBlocking {
        dao.upsert(entity("08:00"))
        dao.deleteAll()
        assertEquals(0, dao.getAll().size)
    }
}

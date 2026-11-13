package app.sederyom.data.db

import androidx.room.withTransaction
import app.sederyom.data.db.mapper.toDomain
import app.sederyom.data.db.mapper.toEntity
import app.sederyom.data.db.mapper.toOverrideEntity
import app.sederyom.data.db.mapper.toTemplateEntity
import app.sederyom.domain.model.DayOverride
import app.sederyom.domain.model.TimeBlock
import app.sederyom.domain.repository.ScheduleRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomScheduleRepository(private val database: AppDatabase) : ScheduleRepository {
    private val templateDao = database.templateBlockDao()
    private val overrideDao = database.dayOverrideDao()

    override fun observeTemplate(): Flow<List<TimeBlock>> =
        templateDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getTemplate(): List<TimeBlock> = templateDao.getAll().map { it.toDomain() }

    override suspend fun upsertTemplateBlock(block: TimeBlock): Long {
        val rowId = templateDao.upsert(block.toTemplateEntity())
        return if (block.id != 0L) block.id else rowId
    }

    override suspend fun deleteTemplateBlock(id: Long) = templateDao.deleteById(id)

    override fun observeOverrides(): Flow<Map<LocalDate, DayOverride>> =
        overrideDao.observeAll().map { list -> list.map { it.toDomain() }.associateBy { it.date } }

    override suspend fun getOverrides(from: LocalDate, to: LocalDate): Map<LocalDate, DayOverride> =
        overrideDao.getBetween(from.toEpochDay(), to.toEpochDay()).map { it.toDomain() }.associateBy { it.date }

    override suspend fun saveOverride(dayOverride: DayOverride) {
        val epochDay = dayOverride.date.toEpochDay()
        overrideDao.replaceDay(dayOverride.toEntity(), dayOverride.blocks.map { it.toOverrideEntity(epochDay) })
    }

    override suspend fun deleteOverride(date: LocalDate) = overrideDao.deleteDay(date.toEpochDay())

    override suspend fun replaceAll(template: List<TimeBlock>, overrides: List<DayOverride>) {
        database.withTransaction {
            templateDao.deleteAll()
            overrideDao.deleteAll()
            templateDao.insertAll(template.map { it.toTemplateEntity() })
            overrides.forEach { saveOverride(it) }
        }
    }
}

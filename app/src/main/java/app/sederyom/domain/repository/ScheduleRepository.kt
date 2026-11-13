package app.sederyom.domain.repository

import app.sederyom.domain.model.DayOverride
import app.sederyom.domain.model.TimeBlock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeTemplate(): Flow<List<TimeBlock>>

    suspend fun getTemplate(): List<TimeBlock>

    /** Inserts (id 0) or updates a template block and returns its id. */
    suspend fun upsertTemplateBlock(block: TimeBlock): Long

    suspend fun deleteTemplateBlock(id: Long)

    fun observeOverrides(): Flow<Map<LocalDate, DayOverride>>

    /** Overrides whose date lies in [from]..[to], both inclusive. */
    suspend fun getOverrides(from: LocalDate, to: LocalDate): Map<LocalDate, DayOverride>

    suspend fun saveOverride(dayOverride: DayOverride)

    suspend fun deleteOverride(date: LocalDate)

    /** Atomically replaces the whole template and all overrides. */
    suspend fun replaceAll(template: List<TimeBlock>, overrides: List<DayOverride>)
}

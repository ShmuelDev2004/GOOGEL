package app.sederyom.domain

import app.sederyom.domain.model.DayOverride
import app.sederyom.domain.model.TimeBlock
import app.sederyom.domain.repository.ScheduleRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeScheduleRepository(
    template: List<TimeBlock> = emptyList(),
    overrides: List<DayOverride> = emptyList(),
) : ScheduleRepository {
    private val templateState = MutableStateFlow(template)
    private val overrideState = MutableStateFlow(overrides.associateBy { it.date })
    private var nextId = 100L

    val blocks: List<TimeBlock>
        get() = templateState.value

    val overrides: Map<LocalDate, DayOverride>
        get() = overrideState.value

    override fun observeTemplate(): Flow<List<TimeBlock>> = templateState

    override suspend fun getTemplate(): List<TimeBlock> = templateState.value

    override suspend fun upsertTemplateBlock(block: TimeBlock): Long {
        val id = if (block.id == 0L) nextId++ else block.id
        templateState.value = templateState.value.filter { it.id != id } + block.copy(id = id)
        return id
    }

    override suspend fun deleteTemplateBlock(id: Long) {
        templateState.value = templateState.value.filter { it.id != id }
    }

    override fun observeOverrides(): Flow<Map<LocalDate, DayOverride>> = overrideState.map { it }

    override suspend fun getOverrides(from: LocalDate, to: LocalDate): Map<LocalDate, DayOverride> =
        overrideState.value.filterKeys { it >= from && it <= to }

    override suspend fun saveOverride(dayOverride: DayOverride) {
        overrideState.value = overrideState.value + (dayOverride.date to dayOverride)
    }

    override suspend fun deleteOverride(date: LocalDate) {
        overrideState.value = overrideState.value - date
    }

    override suspend fun replaceAll(template: List<TimeBlock>, overrides: List<DayOverride>) {
        templateState.value = template
        overrideState.value = overrides.associateBy { it.date }
    }
}

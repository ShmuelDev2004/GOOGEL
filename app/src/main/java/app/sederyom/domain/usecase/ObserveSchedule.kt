package app.sederyom.domain.usecase

import app.sederyom.domain.model.ScheduleSnapshot
import app.sederyom.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveSchedule(private val repository: ScheduleRepository) {
    operator fun invoke(): Flow<ScheduleSnapshot> =
        combine(repository.observeTemplate(), repository.observeOverrides()) { template, overrides ->
            ScheduleSnapshot(template, overrides)
        }
}

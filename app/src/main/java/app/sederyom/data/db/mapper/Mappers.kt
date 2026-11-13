package app.sederyom.data.db.mapper

import app.sederyom.data.db.entity.DayOverrideBlockEntity
import app.sederyom.data.db.entity.DayOverrideEntity
import app.sederyom.data.db.entity.DayOverrideWithBlocks
import app.sederyom.data.db.entity.TemplateBlockEntity
import app.sederyom.domain.model.BlockType
import app.sederyom.domain.model.DayOverride
import app.sederyom.domain.model.TimeBlock
import java.time.LocalDate

fun TemplateBlockEntity.toDomain(): TimeBlock = TimeBlock(
    id = id,
    type = BlockType.fromName(type),
    title = title,
    start = startTime,
    durationMinutes = durationMinutes,
    reminderEnabled = reminderEnabled,
)

fun TimeBlock.toTemplateEntity(): TemplateBlockEntity = TemplateBlockEntity(
    id = id,
    type = type.name,
    title = title,
    startTime = start,
    durationMinutes = durationMinutes,
    reminderEnabled = reminderEnabled,
)

fun DayOverrideBlockEntity.toDomain(): TimeBlock = TimeBlock(
    id = blockId,
    type = BlockType.fromName(type),
    title = title,
    start = startTime,
    durationMinutes = durationMinutes,
    reminderEnabled = reminderEnabled,
)

fun TimeBlock.toOverrideEntity(epochDay: Long): DayOverrideBlockEntity = DayOverrideBlockEntity(
    epochDay = epochDay,
    blockId = id,
    type = type.name,
    title = title,
    startTime = start,
    durationMinutes = durationMinutes,
    reminderEnabled = reminderEnabled,
)

fun DayOverrideWithBlocks.toDomain(): DayOverride = DayOverride(
    date = LocalDate.ofEpochDay(day.epochDay),
    blocks = blocks.map { it.toDomain() }.sortedBy { it.startMinuteOfDay },
)

fun DayOverride.toEntity(): DayOverrideEntity = DayOverrideEntity(epochDay = date.toEpochDay())

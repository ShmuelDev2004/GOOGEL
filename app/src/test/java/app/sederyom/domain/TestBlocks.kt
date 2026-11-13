package app.sederyom.domain

import app.sederyom.domain.model.BlockType
import app.sederyom.domain.model.TimeBlock
import java.time.LocalTime

fun block(
    id: Long,
    start: String,
    minutes: Int,
    type: BlockType = BlockType.OTHER,
    title: String = "",
    reminder: Boolean = true,
): TimeBlock = TimeBlock(id, type, title, LocalTime.parse(start), minutes, reminder)

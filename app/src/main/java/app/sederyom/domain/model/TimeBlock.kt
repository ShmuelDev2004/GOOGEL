package app.sederyom.domain.model

import java.time.LocalTime

const val MINUTES_PER_DAY = 24 * 60

/**
 * A block of time that starts at [start] and lasts [durationMinutes].
 * A block may run past midnight (for example sleep from 23:00 for 7 hours).
 * An id of 0 means the block has not been stored yet.
 */
data class TimeBlock(
    val id: Long,
    val type: BlockType,
    val title: String,
    val start: LocalTime,
    val durationMinutes: Int,
    val reminderEnabled: Boolean = true,
) {
    val startMinuteOfDay: Int
        get() = start.hour * 60 + start.minute

    val endMinuteOfDay: Int
        get() = Math.floorMod(startMinuteOfDay + durationMinutes, MINUTES_PER_DAY)

    val end: LocalTime
        get() = LocalTime.of(endMinuteOfDay / 60, endMinuteOfDay % 60)

    /** True when the block continues after midnight into the next day. */
    val crossesMidnight: Boolean
        get() = startMinuteOfDay + durationMinutes > MINUTES_PER_DAY

    /** The part of this block that lies on the day it starts. */
    fun segmentOnStartDay(): DaySegment = DaySegment(
        block = this,
        startMinute = startMinuteOfDay,
        endMinute = minOf(startMinuteOfDay + durationMinutes, MINUTES_PER_DAY),
        continuesFromPrevious = false,
        continuesToNext = crossesMidnight,
    )

    /** The part of this block that spills into the following day, or null if it ends before midnight. */
    fun segmentOnNextDay(): DaySegment? = if (crossesMidnight) {
        DaySegment(
            block = this,
            startMinute = 0,
            endMinute = startMinuteOfDay + durationMinutes - MINUTES_PER_DAY,
            continuesFromPrevious = true,
            continuesToNext = false,
        )
    } else {
        null
    }
}

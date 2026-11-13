package app.sederyom.domain.model

import app.sederyom.domain.block
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeBlockTest {
    @Test
    fun endIsStartPlusDuration() {
        val block = block(1, "08:00", 90)
        assertEquals(LocalTime.of(9, 30), block.end)
        assertFalse(block.crossesMidnight)
    }

    @Test
    fun blockEndingExactlyAtMidnightDoesNotCrossIt() {
        val block = block(1, "22:00", 120)
        assertEquals(LocalTime.MIDNIGHT, block.end)
        assertFalse(block.crossesMidnight)
        assertNull(block.segmentOnNextDay())
        assertEquals(MINUTES_PER_DAY, block.segmentOnStartDay().endMinute)
    }

    @Test
    fun overnightBlockIsSplitAtMidnight() {
        val block = block(1, "23:00", 420)
        assertTrue(block.crossesMidnight)
        assertEquals(LocalTime.of(6, 0), block.end)

        val head = block.segmentOnStartDay()
        assertEquals(23 * 60, head.startMinute)
        assertEquals(MINUTES_PER_DAY, head.endMinute)
        assertTrue(head.continuesToNext)
        assertFalse(head.continuesFromPrevious)

        val tail = block.segmentOnNextDay()
        assertEquals(0, tail?.startMinute)
        assertEquals(6 * 60, tail?.endMinute)
        assertEquals(true, tail?.continuesFromPrevious)
    }

    @Test
    fun fullDayBlockStartingMidDayWrapsAround() {
        val block = block(1, "08:00", MINUTES_PER_DAY)
        assertTrue(block.crossesMidnight)
        assertEquals(LocalTime.of(8, 0), block.end)
        assertEquals(8 * 60, block.segmentOnNextDay()?.endMinute)
    }
}

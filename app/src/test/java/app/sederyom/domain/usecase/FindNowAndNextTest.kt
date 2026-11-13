package app.sederyom.domain.usecase

import app.sederyom.domain.block
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FindNowAndNextTest {
    private val find = FindNowAndNext()
    private val resolve = ResolveDayPlan()
    private val date = LocalDate.of(2026, 11, 13)
    private val template = listOf(block(1, "08:00", 60), block(2, "12:00", 30), block(3, "23:00", 420))
    private val timeline = resolve.timeline(date, template, emptyMap())

    @Test
    fun beforeFirstBlockOfTheMorningTheNightTailIsCurrent() {
        val result = find(timeline, LocalTime.of(5, 0))
        assertEquals(3L, result.current?.block?.id)
        assertEquals(1L, result.next?.block?.id)
    }

    @Test
    fun betweenBlocksThereIsNoCurrentButANext() {
        val result = find(timeline, LocalTime.of(10, 0))
        assertNull(result.current)
        assertEquals(2L, result.next?.block?.id)
    }

    @Test
    fun startMinuteIsInclusiveAndEndMinuteIsExclusive() {
        assertEquals(1L, find(timeline, LocalTime.of(8, 0)).current?.block?.id)
        assertNull(find(timeline, LocalTime.of(9, 0)).current)
    }

    @Test
    fun lateEveningHasCurrentOvernightHeadAndNoNext() {
        val result = find(timeline, LocalTime.of(23, 30))
        assertEquals(3L, result.current?.block?.id)
        assertNull(result.next)
    }
}

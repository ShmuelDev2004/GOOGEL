package app.sederyom.domain.usecase

import app.sederyom.domain.block
import app.sederyom.domain.model.DayOverride
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveDayPlanTest {
    private val resolve = ResolveDayPlan()
    private val monday = LocalDate.of(2026, 11, 9)
    private val template = listOf(block(2, "12:00", 60), block(1, "08:00", 60))

    @Test
    fun usesTemplateSortedByStartWhenNoOverride() {
        val plan = resolve.plan(monday, template, emptyMap())
        assertFalse(plan.isOverride)
        assertEquals(listOf(1L, 2L), plan.blocks.map { it.id })
    }

    @Test
    fun overrideReplacesTemplateOnlyForItsDate() {
        val override = DayOverride(monday, listOf(block(9, "10:00", 30)))
        val overrides = mapOf(monday to override)

        val overridden = resolve.plan(monday, template, overrides)
        assertTrue(overridden.isOverride)
        assertEquals(listOf(9L), overridden.blocks.map { it.id })

        val nextDay = resolve.plan(monday.plusDays(1), template, overrides)
        assertFalse(nextDay.isOverride)
        assertEquals(2, nextDay.blocks.size)
    }

    @Test
    fun emptyOverrideMeansAnEmptyDay() {
        val overrides = mapOf(monday to DayOverride(monday, emptyList()))
        val plan = resolve.plan(monday, template, overrides)
        assertTrue(plan.isOverride)
        assertTrue(plan.blocks.isEmpty())
    }

    @Test
    fun timelineIncludesTailOfPreviousNightFirst() {
        val withSleep = template + block(3, "23:00", 420)
        val timeline = resolve.timeline(monday, withSleep, emptyMap())

        assertEquals(listOf(3L, 1L, 2L, 3L), timeline.map { it.block.id })
        val tail = timeline.first()
        assertEquals(0, tail.startMinute)
        assertEquals(6 * 60, tail.endMinute)
        assertTrue(tail.continuesFromPrevious)
        val head = timeline.last()
        assertEquals(23 * 60, head.startMinute)
        assertEquals(1440, head.endMinute)
        assertTrue(head.continuesToNext)
    }

    @Test
    fun timelineTailComesFromPreviousDaysOverride() {
        val sundayOverride = DayOverride(monday.minusDays(1), listOf(block(7, "22:00", 480)))
        val timeline = resolve.timeline(monday, template, mapOf(sundayOverride.date to sundayOverride))

        val tail = timeline.first()
        assertEquals(7L, tail.block.id)
        assertEquals(6 * 60, tail.endMinute)
    }

    @Test
    fun timelineWithoutOvernightBlocksHasNoTail() {
        val timeline = resolve.timeline(monday, template, emptyMap())
        assertEquals(listOf(1L, 2L), timeline.map { it.block.id })
        assertTrue(timeline.none { it.continuesFromPrevious })
    }
}

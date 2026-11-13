package app.sederyom.domain.usecase

import app.sederyom.domain.block
import app.sederyom.domain.model.DayOverride
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputeReminderPlanTest {
    private val compute = ComputeReminderPlan()
    private val friday = LocalDate.of(2026, 11, 13)
    private val template = listOf(block(1, "08:00", 60), block(2, "12:00", 30), block(3, "23:00", 420))

    private fun at(date: LocalDate, time: String): LocalDateTime = date.atTime(LocalTime.parse(time))

    @Test
    fun sevenDayPlanHasEveryBlockOnEveryDay() {
        val plan = compute(template, emptyMap(), at(friday, "07:00"), 10)

        assertEquals(7 * 3, plan.size)
        assertEquals(friday, plan.first().date)
        assertEquals(friday.plusDays(6), plan.maxOf { it.date })
    }

    @Test
    fun planIsSortedByTriggerTime() {
        val plan = compute(template, emptyMap(), at(friday, "07:00"), 10)

        assertEquals(plan.sortedBy { it.triggerAt }, plan)
        assertEquals(at(friday, "07:50"), plan.first().triggerAt)
        assertEquals(1L, plan.first().blockId)
    }

    @Test
    fun remindersAlreadyInThePastAreDropped() {
        val plan = compute(template, emptyMap(), at(friday, "09:00"), 10)

        assertEquals(7 * 3 - 1, plan.size)
        assertEquals(2L, plan.first().blockId)
    }

    @Test
    fun triggerIsStartMinusLeadTime() {
        val plan = compute(template, emptyMap(), at(friday, "07:00"), 30)

        val first = plan.first()
        assertEquals(at(friday, "08:00"), first.blockStart)
        assertEquals(at(friday, "07:30"), first.triggerAt)
        assertEquals(30, first.leadMinutes)
    }

    @Test
    fun zeroLeadTimeFiresAtTheStart() {
        val plan = compute(template, emptyMap(), at(friday, "07:00"), 0)
        assertEquals(at(friday, "08:00"), plan.first().triggerAt)
    }

    @Test
    fun leadTimeCanPushTheTriggerToThePreviousDay() {
        val early = listOf(block(5, "00:10", 30))
        val plan = compute(early, emptyMap(), at(friday, "12:00"), 30)

        val first = plan.first()
        assertEquals(friday.plusDays(1), first.date)
        assertEquals(at(friday, "23:40"), first.triggerAt)
    }

    @Test
    fun blocksWithRemindersOffAreSkipped() {
        val blocks = listOf(block(1, "08:00", 60, reminder = false), block(2, "12:00", 30))
        val plan = compute(blocks, emptyMap(), at(friday, "07:00"), 10)

        assertEquals(7, plan.size)
        assertTrue(plan.all { it.blockId == 2L })
    }

    @Test
    fun overrideReplacesTemplateForItsDateOnly() {
        val saturday = friday.plusDays(1)
        val overrides = mapOf(saturday to DayOverride(saturday, listOf(block(9, "10:00", 30))))
        val plan = compute(template, overrides, at(friday, "07:00"), 10)

        assertEquals(listOf(9L), plan.filter { it.date == saturday }.map { it.blockId })
        assertEquals(3, plan.count { it.date == friday })
        assertEquals(3, plan.count { it.date == saturday.plusDays(1) })
    }

    @Test
    fun overrideOutsideTheWindowIsIgnored() {
        val later = friday.plusDays(7)
        val overrides = mapOf(later to DayOverride(later, emptyList()))
        val plan = compute(template, overrides, at(friday, "07:00"), 10)
        assertEquals(7 * 3, plan.size)
    }

    @Test
    fun keysAndRequestCodesAreUniqueWithinThePlan() {
        val plan = compute(template, emptyMap(), at(friday, "07:00"), 10)

        assertEquals(plan.size, plan.map { it.key }.toSet().size)
        assertEquals(plan.size, plan.map { it.requestCode }.toSet().size)
    }

    @Test
    fun emptyTemplateGivesEmptyPlan() {
        assertTrue(compute(emptyList(), emptyMap(), at(friday, "07:00"), 10).isEmpty())
    }
}

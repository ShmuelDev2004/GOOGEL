package app.sederyom.ui.components

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattingTest {
    @Test
    fun endOfDayMinuteIsShownAsMidnight() {
        assertEquals("00:00", formatMinuteOfDay(1440))
        assertEquals("23:59", formatMinuteOfDay(1439))
        assertEquals("07:05", formatMinuteOfDay(7 * 60 + 5))
    }

    @Test
    fun timesAreZeroPadded() {
        assertEquals("08:05", LocalTime.of(8, 5).formatHm())
    }

    @Test
    fun isolateWrapsTextInLeftToRightIsolates() {
        val wrapped = isolateLtr("08:00")
        assertTrue(wrapped.startsWith("⁦"))
        assertTrue(wrapped.endsWith("⁩"))
        assertTrue(wrapped.contains("08:00"))
    }

    @Test
    fun rangeKeepsStartBeforeEndInsideOneIsolate() {
        val range = formatRange("23:00", "06:00")
        assertEquals("⁦23:00–06:00⁩", range)
    }
}

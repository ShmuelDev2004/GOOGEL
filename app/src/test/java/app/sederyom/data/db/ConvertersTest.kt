package app.sederyom.data.db

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun timeIsStoredAsHoursAndMinutesText() {
        assertEquals("07:05", converters.fromLocalTime(LocalTime.of(7, 5)))
        assertEquals("23:59", converters.fromLocalTime(LocalTime.of(23, 59)))
        assertEquals("00:00", converters.fromLocalTime(LocalTime.MIDNIGHT))
    }

    @Test
    fun textIsParsedBackToTime() {
        assertEquals(LocalTime.of(7, 5), converters.toLocalTime("07:05"))
    }

    @Test
    fun nullsPassThrough() {
        assertNull(converters.fromLocalTime(null))
        assertNull(converters.toLocalTime(null))
    }
}

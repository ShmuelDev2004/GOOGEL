package app.sederyom.domain.usecase

import app.sederyom.domain.block
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SuggestNewBlockTest {
    private val suggest = SuggestNewBlock()

    @Test
    fun emptyDayStartsAtEight() {
        assertEquals(LocalTime.of(8, 0), suggest(emptyList(), 1)?.start)
    }

    @Test
    fun startsRightAfterTheLastBlock() {
        val result = suggest(listOf(block(1, "08:00", 60)), 2)
        assertEquals(LocalTime.of(9, 0), result?.start)
    }

    @Test
    fun skipsOccupiedSlots() {
        val existing = listOf(block(1, "08:00", 60), block(2, "09:00", 45))
        val result = suggest(existing, 3)
        assertNotNull(result)
        assertEquals(LocalTime.of(9, 45), result?.start)
    }

    @Test
    fun returnsNullWhenDayIsFull() {
        assertNull(suggest(listOf(block(1, "00:00", 1440)), 2))
    }
}

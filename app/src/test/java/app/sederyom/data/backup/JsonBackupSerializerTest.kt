package app.sederyom.data.backup

import app.sederyom.domain.block
import app.sederyom.domain.model.BackupData
import app.sederyom.domain.model.BlockType
import app.sederyom.domain.model.DayOverride
import app.sederyom.domain.usecase.DecodeResult
import app.sederyom.domain.usecase.ImportError
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonBackupSerializerTest {
    private val serializer = JsonBackupSerializer()
    private val date = LocalDate.of(2026, 11, 13)

    private fun failure(text: String): ImportError = (serializer.decode(text) as DecodeResult.Failure).error

    @Test
    fun roundTripKeepsContentAndReissuesIds() {
        val data = BackupData(
            template = listOf(block(7, "07:30", 45, BlockType.WORK, "x", reminder = false), block(9, "23:00", 420)),
            overrides = listOf(DayOverride(date, listOf(block(4, "10:00", 30, BlockType.STUDY)))),
        )

        val decoded = (serializer.decode(serializer.encode(data)) as DecodeResult.Success).data

        assertEquals(listOf(1L, 2L), decoded.template.map { it.id })
        assertEquals(data.template.map { it.copy(id = 0) }, decoded.template.map { it.copy(id = 0) })
        assertEquals(date, decoded.overrides.single().date)
        assertEquals(BlockType.STUDY, decoded.overrides.single().blocks.single().type)
        assertEquals(1L, decoded.overrides.single().blocks.single().id)
    }

    @Test
    fun encodedFileIsReadableText() {
        val text = serializer.encode(BackupData(listOf(block(1, "08:05", 30)), emptyList()))
        assertTrue(text.contains("\"start\": \"08:05\""))
        assertTrue(text.contains("\"version\": 1"))
    }

    @Test
    fun garbageIsNotValidJson() {
        assertEquals(ImportError.NotValidJson, failure("not json"))
        assertEquals(ImportError.NotValidJson, failure("""{"template": []}"""))
    }

    @Test
    fun otherVersionIsRejected() {
        assertEquals(ImportError.UnsupportedVersion(2), failure("""{"version": 2}"""))
    }

    @Test
    fun badTimeNamesTheField() {
        val text = """{"version":1,"template":[{"type":"WORK","start":"25:00","durationMinutes":30}]}"""
        assertEquals(ImportError.BadValue("template[0].start"), failure(text))
    }

    @Test
    fun unknownTypeIsRejected() {
        val text = """{"version":1,"template":[{"type":"NAP","start":"08:00","durationMinutes":30}]}"""
        assertEquals(ImportError.BadValue("template[0].type"), failure(text))
    }

    @Test
    fun badDateAndDuplicateDateAreRejected() {
        assertEquals(
            ImportError.BadValue("overrides[0].date"),
            failure("""{"version":1,"overrides":[{"date":"13/11/2026"}]}"""),
        )
        assertEquals(
            ImportError.BadValue("overrides[1].date"),
            failure("""{"version":1,"overrides":[{"date":"2026-11-13"},{"date":"2026-11-13"}]}"""),
        )
    }

    @Test
    fun unknownKeysAreIgnoredAndDefaultsApplied() {
        val text = """{"version":1,"extra":5,"template":[{"type":"MEAL","start":"12:00","durationMinutes":30}]}"""
        val block = (serializer.decode(text) as DecodeResult.Success).data.template.single()
        assertEquals("", block.title)
        assertTrue(block.reminderEnabled)
    }
}

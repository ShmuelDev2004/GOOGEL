package app.sederyom.domain.usecase

import app.sederyom.data.backup.JsonBackupSerializer
import app.sederyom.domain.FakeBackupFiles
import app.sederyom.domain.FakeScheduleRepository
import app.sederyom.domain.block
import app.sederyom.domain.model.BackupData
import app.sederyom.domain.model.DayOverride
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupUseCasesTest {
    private val serializer = JsonBackupSerializer()
    private val date = LocalDate.of(2026, 11, 13)

    private fun prepare(files: FakeBackupFiles) = PrepareImport(serializer, ValidateBlock(), files)

    @Test
    fun exportThenPrepareGivesAPreviewWithCounts() = runBlocking {
        val repository = FakeScheduleRepository(
            template = listOf(block(1, "08:00", 60), block(2, "12:00", 30)),
            overrides = listOf(
                DayOverride(date, listOf(block(1, "09:00", 30))),
                DayOverride(date.plusDays(3), emptyList()),
            ),
        )
        val files = FakeBackupFiles()

        assertTrue(ExportBackup(repository, serializer, files)("file"))
        val result = prepare(files)("file") as PrepareImportResult.Ready

        assertEquals(2, result.preview.templateCount)
        assertEquals(2, result.preview.overrideCount)
        assertEquals(date, result.preview.firstOverride)
        assertEquals(date.plusDays(3), result.preview.lastOverride)
    }

    @Test
    fun failedWriteIsReported() = runBlocking {
        val files = FakeBackupFiles().apply { failWrites = true }
        assertFalse(ExportBackup(FakeScheduleRepository(), serializer, files)("file"))
    }

    @Test
    fun missingFileCannotBeRead() = runBlocking {
        val result = prepare(FakeBackupFiles())("nowhere")
        assertEquals(PrepareImportResult.Rejected(ImportError.CannotRead), result)
    }

    @Test
    fun overlappingTemplateBlocksAreRejectedBeforeAnythingIsChanged() = runBlocking {
        val data = BackupData(listOf(block(1, "08:00", 60), block(2, "08:30", 60)), emptyList())
        val files = FakeBackupFiles().apply { this["f"] = serializer.encode(data) }

        val result = prepare(files)("f") as PrepareImportResult.Rejected

        val error = result.error as ImportError.InvalidBlocks
        assertEquals("template", error.location)
        assertTrue(error.errors.isNotEmpty())
    }

    @Test
    fun overlapInsideAnOverrideNamesThatDate() = runBlocking {
        val bad = DayOverride(date, listOf(block(1, "08:00", 60), block(2, "08:30", 60)))
        val files = FakeBackupFiles().apply { this["f"] = serializer.encode(BackupData(emptyList(), listOf(bad))) }

        val error = (prepare(files)("f") as PrepareImportResult.Rejected).error as ImportError.InvalidBlocks

        assertEquals("overrides[2026-11-13]", error.location)
    }

    @Test
    fun applyReplacesEverythingAndReschedules() = runBlocking {
        val repository = FakeScheduleRepository(
            template = listOf(block(1, "07:00", 30)),
            overrides = listOf(DayOverride(date, emptyList())),
        )
        var rescheduled = 0
        val incoming = BackupData(listOf(block(5, "12:00", 30)), emptyList())

        ApplyImport(repository) { rescheduled++ }(incoming)

        assertEquals(listOf(5L), repository.blocks.map { it.id })
        assertTrue(repository.overrides.isEmpty())
        assertEquals(1, rescheduled)
    }
}

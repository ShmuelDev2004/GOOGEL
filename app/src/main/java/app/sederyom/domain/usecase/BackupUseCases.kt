package app.sederyom.domain.usecase

import app.sederyom.domain.model.BackupData
import app.sederyom.domain.model.DayOverride
import app.sederyom.domain.repository.BackupFileAccess
import app.sederyom.domain.repository.ScheduleRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.first

sealed interface ImportError {
    data object CannotRead : ImportError

    data object NotValidJson : ImportError

    data class UnsupportedVersion(val version: Int) : ImportError

    /** [location] is a technical path such as `template[2].start` or `overrides[2026-11-13]`. */
    data class BadValue(val location: String) : ImportError

    data class TooMany(val location: String) : ImportError

    data class InvalidBlocks(val location: String, val errors: List<BlockError>) : ImportError
}

sealed interface DecodeResult {
    data class Success(val data: BackupData) : DecodeResult

    data class Failure(val error: ImportError) : DecodeResult
}

interface BackupSerializer {
    fun encode(data: BackupData): String

    /** Checks the structure and every value, but not whether blocks overlap (see [PrepareImport]). */
    fun decode(text: String): DecodeResult
}

data class ImportPreview(
    val data: BackupData,
    val firstOverride: LocalDate?,
    val lastOverride: LocalDate?,
) {
    val templateCount: Int
        get() = data.template.size

    val overrideCount: Int
        get() = data.overrides.size
}

sealed interface PrepareImportResult {
    data class Ready(val preview: ImportPreview) : PrepareImportResult

    data class Rejected(val error: ImportError) : PrepareImportResult
}

class ExportBackup(
    private val repository: ScheduleRepository,
    private val serializer: BackupSerializer,
    private val files: BackupFileAccess,
) {
    /** Returns true when the file was written. */
    suspend operator fun invoke(location: String): Boolean {
        val overrides = repository.observeOverrides().first().values.sortedBy { it.date }
        val data = BackupData(repository.getTemplate(), overrides)
        return files.write(location, serializer.encode(data))
    }
}

/** Reads and fully validates a backup file without changing anything. */
class PrepareImport(
    private val serializer: BackupSerializer,
    private val validateBlock: ValidateBlock,
    private val files: BackupFileAccess,
) {
    suspend operator fun invoke(location: String): PrepareImportResult {
        val text = files.read(location) ?: return PrepareImportResult.Rejected(ImportError.CannotRead)
        return when (val decoded = serializer.decode(text)) {
            is DecodeResult.Failure -> PrepareImportResult.Rejected(decoded.error)
            is DecodeResult.Success -> validate(decoded.data)
        }
    }

    private fun validate(data: BackupData): PrepareImportResult {
        val templateErrors = validateBlock.validateEach(data.template).values.flatten().distinct()
        if (templateErrors.isNotEmpty()) {
            return PrepareImportResult.Rejected(ImportError.InvalidBlocks("template", templateErrors))
        }
        for (override in data.overrides) {
            val errors = validateBlock.validateEach(override.blocks).values.flatten().distinct()
            if (errors.isNotEmpty()) {
                return PrepareImportResult.Rejected(ImportError.InvalidBlocks("overrides[${override.date}]", errors))
            }
        }
        val dates = data.overrides.map(DayOverride::date)
        return PrepareImportResult.Ready(ImportPreview(data, dates.minOrNull(), dates.maxOrNull()))
    }
}

/** Replaces the whole template and all overrides in one transaction. */
class ApplyImport(
    private val repository: ScheduleRepository,
    private val rescheduler: ReminderRescheduler,
) {
    suspend operator fun invoke(data: BackupData) {
        repository.replaceAll(data.template, data.overrides)
        rescheduler.reschedule()
    }
}

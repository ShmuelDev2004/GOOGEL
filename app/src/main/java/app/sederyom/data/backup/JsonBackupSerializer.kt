package app.sederyom.data.backup

import app.sederyom.domain.model.BackupData
import app.sederyom.domain.model.BlockType
import app.sederyom.domain.model.DayOverride
import app.sederyom.domain.model.TimeBlock
import app.sederyom.domain.usecase.BackupSerializer
import app.sederyom.domain.usecase.DecodeResult
import app.sederyom.domain.usecase.ImportError
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.serialization.json.Json

/** Pure Kotlin (no android.* imports) so it can be tested on the JVM. Block ids are not exported; they are reissued. */
class JsonBackupSerializer : BackupSerializer {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun encode(data: BackupData): String {
        val file = BackupFileDto(
            version = CURRENT_VERSION,
            template = data.template.map { it.toDto() },
            overrides = data.overrides.map { OverrideDto(it.date.toString(), it.blocks.map { b -> b.toDto() }) },
        )
        return json.encodeToString(BackupFileDto.serializer(), file)
    }

    override fun decode(text: String): DecodeResult {
        val file = try {
            json.decodeFromString(BackupFileDto.serializer(), text)
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException.
            return DecodeResult.Failure(ImportError.NotValidJson)
        }
        return try {
            DecodeResult.Success(file.toDomain())
        } catch (e: BackupFormatException) {
            DecodeResult.Failure(e.error)
        }
    }

    private fun BackupFileDto.toDomain(): BackupData {
        if (version != CURRENT_VERSION) throw BackupFormatException(ImportError.UnsupportedVersion(version))
        if (template.size > MAX_BLOCKS_PER_DAY) throw BackupFormatException(ImportError.TooMany("template"))
        if (overrides.size > MAX_OVERRIDES) throw BackupFormatException(ImportError.TooMany("overrides"))

        val seenDates = mutableSetOf<LocalDate>()
        val domainOverrides = overrides.mapIndexed { index, dto ->
            val where = "overrides[$index]"
            val date = parseDate(dto.date, "$where.date")
            if (!seenDates.add(date)) throw BackupFormatException(ImportError.BadValue("$where.date"))
            if (dto.blocks.size > MAX_BLOCKS_PER_DAY) throw BackupFormatException(ImportError.TooMany("$where.blocks"))
            DayOverride(date, dto.blocks.mapIndexed { i, b -> b.toDomain(i + 1L, "$where.blocks[$i]") })
        }
        val domainTemplate = template.mapIndexed { i, b -> b.toDomain(i + 1L, "template[$i]") }
        return BackupData(domainTemplate, domainOverrides.sortedBy { it.date })
    }

    private fun BlockDto.toDomain(id: Long, where: String): TimeBlock {
        val blockType = BlockType.entries.firstOrNull { it.name == type }
            ?: throw BackupFormatException(ImportError.BadValue("$where.type"))
        val time = try {
            LocalTime.parse(start, TIME_FORMAT)
        } catch (e: DateTimeParseException) {
            throw BackupFormatException(ImportError.BadValue("$where.start"))
        }
        return TimeBlock(id, blockType, title.trim(), time, durationMinutes, reminderEnabled)
    }

    private fun parseDate(text: String, where: String): LocalDate = try {
        LocalDate.parse(text)
    } catch (e: DateTimeParseException) {
        throw BackupFormatException(ImportError.BadValue(where))
    }

    private fun TimeBlock.toDto() = BlockDto(
        type = type.name,
        title = title,
        start = start.format(TIME_FORMAT),
        durationMinutes = durationMinutes,
        reminderEnabled = reminderEnabled,
    )

    private class BackupFormatException(val error: ImportError) : Exception()

    companion object {
        const val CURRENT_VERSION = 1
        const val MAX_BLOCKS_PER_DAY = 200
        const val MAX_OVERRIDES = 4000
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    }
}

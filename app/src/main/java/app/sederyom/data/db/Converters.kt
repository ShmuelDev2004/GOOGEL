package app.sederyom.data.db

import androidx.room.TypeConverter
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** LocalTime is stored as "HH:mm" text. Dates are stored directly as epochDay Long values. */
class Converters {
    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.format(FORMATTER)

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it, FORMATTER) }

    private companion object {
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    }
}

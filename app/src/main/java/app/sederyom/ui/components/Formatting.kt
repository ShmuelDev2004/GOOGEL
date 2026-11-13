package app.sederyom.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.sederyom.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val LEFT_TO_RIGHT_ISOLATE = "\u2066"
private const val POP_DIRECTIONAL_ISOLATE = "\u2069"
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24
private val HEBREW_LOCALE: Locale = Locale.forLanguageTag("he-IL")
private val HM_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

fun LocalTime.formatHm(): String = format(HM_FORMATTER)

/** Minute 1440 (end of day) is shown as 00:00. */
fun formatMinuteOfDay(minute: Int): String =
    LocalTime.of((minute / MINUTES_PER_HOUR) % HOURS_PER_DAY, minute % MINUTES_PER_HOUR).formatHm()

/** Keeps digits and punctuation in their written order inside right-to-left text. */
fun isolateLtr(text: String): String = "$LEFT_TO_RIGHT_ISOLATE$text$POP_DIRECTIONAL_ISOLATE"

fun formatRange(start: String, end: String): String = isolateLtr("$start\u2013$end")

fun formatDuration(context: Context, minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> context.getString(R.string.duration_minutes_only, rest)
        rest == 0 -> context.getString(R.string.duration_hours_only, hours)
        else -> context.getString(R.string.duration_hours_minutes, hours, rest)
    }
}

@Composable
fun durationText(minutes: Int): String = formatDuration(LocalContext.current, minutes)

fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(HEBREW_LOCALE))

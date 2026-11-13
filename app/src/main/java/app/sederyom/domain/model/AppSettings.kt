package app.sederyom.domain.model

enum class NotifyMode {
    SOUND,
    VIBRATE,
    ;

    companion object {
        fun fromName(name: String?): NotifyMode = entries.firstOrNull { it.name == name } ?: SOUND
    }
}

data class AppSettings(
    val leadTimeMinutes: Int = DEFAULT_LEAD_TIME_MINUTES,
    val notifyMode: NotifyMode = NotifyMode.SOUND,
    val onboardingDone: Boolean = false,
) {
    companion object {
        const val DEFAULT_LEAD_TIME_MINUTES = 10
        val LEAD_TIME_OPTIONS = listOf(0, 5, 10, 15, 30, 60)
    }
}

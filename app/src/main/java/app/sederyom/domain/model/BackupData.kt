package app.sederyom.domain.model

/** Everything that is exported and imported: the template and all day overrides. Settings are not included. */
data class BackupData(
    val template: List<TimeBlock>,
    val overrides: List<DayOverride>,
)

package app.sederyom.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupFileDto(
    val version: Int,
    val template: List<BlockDto> = emptyList(),
    val overrides: List<OverrideDto> = emptyList(),
)

/** Times are "HH:mm" text and dates are ISO "yyyy-MM-dd" text, so the file is readable and editable. */
@Serializable
data class BlockDto(
    val type: String,
    val title: String = "",
    val start: String,
    val durationMinutes: Int,
    val reminderEnabled: Boolean = true,
)

@Serializable
data class OverrideDto(
    val date: String,
    val blocks: List<BlockDto> = emptyList(),
)

package app.sederyom.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(tableName = "template_blocks")
data class TemplateBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val reminderEnabled: Boolean,
)

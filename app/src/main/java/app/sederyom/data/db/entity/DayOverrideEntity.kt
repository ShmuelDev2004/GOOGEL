package app.sederyom.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalTime

@Entity(tableName = "day_overrides")
data class DayOverrideEntity(
    @PrimaryKey val epochDay: Long,
)

@Entity(
    tableName = "day_override_blocks",
    primaryKeys = ["epochDay", "blockId"],
    foreignKeys = [
        ForeignKey(
            entity = DayOverrideEntity::class,
            parentColumns = ["epochDay"],
            childColumns = ["epochDay"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DayOverrideBlockEntity(
    val epochDay: Long,
    val blockId: Long,
    val type: String,
    val title: String,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val reminderEnabled: Boolean,
)

data class DayOverrideWithBlocks(
    @Embedded val day: DayOverrideEntity,
    @Relation(parentColumn = "epochDay", entityColumn = "epochDay")
    val blocks: List<DayOverrideBlockEntity>,
)

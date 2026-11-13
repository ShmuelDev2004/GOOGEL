package app.sederyom.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.sederyom.data.db.dao.DayOverrideDao
import app.sederyom.data.db.dao.TemplateBlockDao
import app.sederyom.data.db.entity.DayOverrideBlockEntity
import app.sederyom.data.db.entity.DayOverrideEntity
import app.sederyom.data.db.entity.TemplateBlockEntity

@Database(
    entities = [
        TemplateBlockEntity::class,
        DayOverrideEntity::class,
        DayOverrideBlockEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun templateBlockDao(): TemplateBlockDao

    abstract fun dayOverrideDao(): DayOverrideDao
}

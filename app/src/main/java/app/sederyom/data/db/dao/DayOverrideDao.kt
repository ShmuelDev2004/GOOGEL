package app.sederyom.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import app.sederyom.data.db.entity.DayOverrideBlockEntity
import app.sederyom.data.db.entity.DayOverrideEntity
import app.sederyom.data.db.entity.DayOverrideWithBlocks
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DayOverrideDao {
    @Transaction
    @Query("SELECT * FROM day_overrides")
    abstract fun observeAll(): Flow<List<DayOverrideWithBlocks>>

    @Transaction
    @Query("SELECT * FROM day_overrides WHERE epochDay BETWEEN :from AND :to")
    abstract suspend fun getBetween(from: Long, to: Long): List<DayOverrideWithBlocks>

    @Transaction
    @Query("SELECT * FROM day_overrides WHERE epochDay = :epochDay")
    abstract suspend fun getForDay(epochDay: Long): DayOverrideWithBlocks?

    @Insert
    abstract suspend fun insertDay(day: DayOverrideEntity)

    @Insert
    abstract suspend fun insertBlocks(blocks: List<DayOverrideBlockEntity>)

    /** Child blocks are removed by the foreign key cascade. */
    @Query("DELETE FROM day_overrides WHERE epochDay = :epochDay")
    abstract suspend fun deleteDay(epochDay: Long)

    @Query("DELETE FROM day_overrides")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceDay(day: DayOverrideEntity, blocks: List<DayOverrideBlockEntity>) {
        deleteDay(day.epochDay)
        insertDay(day)
        insertBlocks(blocks)
    }
}

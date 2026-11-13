package app.sederyom.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import app.sederyom.data.db.entity.TemplateBlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateBlockDao {
    @Query("SELECT * FROM template_blocks ORDER BY startTime, id")
    fun observeAll(): Flow<List<TemplateBlockEntity>>

    @Query("SELECT * FROM template_blocks ORDER BY startTime, id")
    suspend fun getAll(): List<TemplateBlockEntity>

    /** Returns the new row id for an insert, or -1 when an existing row was updated. */
    @Upsert
    suspend fun upsert(entity: TemplateBlockEntity): Long

    @Insert
    suspend fun insertAll(entities: List<TemplateBlockEntity>)

    @Query("DELETE FROM template_blocks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM template_blocks")
    suspend fun deleteAll()
}

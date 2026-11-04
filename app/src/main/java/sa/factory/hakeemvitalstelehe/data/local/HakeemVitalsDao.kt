package sa.factory.hakeemvitalstelehe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HakeemVitalsDao {
    @Query("SELECT * FROM telehealth_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<HakeemVitalsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<HakeemVitalsEntity>)

    @Query("DELETE FROM telehealth_items_cache")
    suspend fun clearAll()
}

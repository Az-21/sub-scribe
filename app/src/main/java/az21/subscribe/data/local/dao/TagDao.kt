package az21.subscribe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import az21.subscribe.data.local.entity.SubscriptionTagRow
import az21.subscribe.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TagDao {
  @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
  fun observeAll(): Flow<List<TagEntity>>

  @Query(
    """
    SELECT st.subscription_id AS subscription_id, t.id AS tag_id, t.name AS name, t.color AS color
    FROM subscription_tags st
    INNER JOIN tags t ON t.id = st.tag_id
    ORDER BY t.name COLLATE NOCASE ASC
    """,
  )
  fun observeAssignments(): Flow<List<SubscriptionTagRow>>

  @Query(
    """
    SELECT t.* FROM tags t
    INNER JOIN subscription_tags st ON st.tag_id = t.id
    WHERE st.subscription_id = :subscriptionId
    ORDER BY t.name COLLATE NOCASE ASC
    """,
  )
  fun observeForSubscription(subscriptionId: UUID): Flow<List<TagEntity>>

  @Query("SELECT * FROM tags WHERE id = :id")
  suspend fun getById(id: UUID): TagEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: TagEntity)

  @Query("DELETE FROM tags WHERE id = :id")
  suspend fun deleteById(id: UUID)
}

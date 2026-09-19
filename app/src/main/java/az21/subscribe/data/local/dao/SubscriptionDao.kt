package az21.subscribe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.data.local.entity.SubscriptionTagEntity
import az21.subscribe.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SubscriptionDao {
  @Query("SELECT * FROM subscriptions ORDER BY name COLLATE NOCASE ASC")
  fun observeAll(): Flow<List<SubscriptionEntity>>

  @Query(
    "SELECT * FROM subscriptions WHERE status = :status ORDER BY name COLLATE NOCASE ASC",
  )
  fun observeByStatus(status: SubscriptionStatus): Flow<List<SubscriptionEntity>>

  @Query("SELECT * FROM subscriptions WHERE id = :id")
  fun observeById(id: UUID): Flow<SubscriptionEntity?>

  @Query("SELECT * FROM subscriptions WHERE id = :id")
  suspend fun getById(id: UUID): SubscriptionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: SubscriptionEntity)

  @Query("DELETE FROM subscriptions WHERE id = :id")
  suspend fun deleteById(id: UUID)

  @Query("SELECT tag_id FROM subscription_tags WHERE subscription_id = :subscriptionId")
  fun observeTagIds(subscriptionId: UUID): Flow<List<UUID>>

  @Query("DELETE FROM subscription_tags WHERE subscription_id = :subscriptionId")
  suspend fun clearTags(subscriptionId: UUID)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTags(rows: List<SubscriptionTagEntity>)

  @Transaction
  suspend fun setTags(
    subscriptionId: UUID,
    tagIds: Collection<UUID>,
  ) {
    clearTags(subscriptionId)
    insertTags(tagIds.map { SubscriptionTagEntity(subscriptionId = subscriptionId, tagId = it) })
  }
}

package az21.subscribe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
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

  @Query("SELECT * FROM subscriptions")
  suspend fun getAll(): List<SubscriptionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: SubscriptionEntity)

  @Upsert
  suspend fun upsertAll(items: List<SubscriptionEntity>)

  @Query("DELETE FROM subscriptions WHERE id = :id")
  suspend fun deleteById(id: UUID)

  @Query("SELECT tag_id FROM subscription_tags WHERE subscription_id = :subscriptionId")
  fun observeTagIds(subscriptionId: UUID): Flow<List<UUID>>

  @Query("SELECT * FROM subscription_tags")
  suspend fun getAllTagAssignments(): List<SubscriptionTagEntity>

  @Query("DELETE FROM subscription_tags WHERE subscription_id = :subscriptionId")
  suspend fun clearTags(subscriptionId: UUID)

  @Query("DELETE FROM subscription_tags WHERE subscription_id IN (:subscriptionIds)")
  suspend fun clearTagsFor(subscriptionIds: List<UUID>)

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

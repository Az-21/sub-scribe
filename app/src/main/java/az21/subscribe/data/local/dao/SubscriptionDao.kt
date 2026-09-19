package az21.subscribe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.data.local.entity.SubscriptionReminderEntity
import az21.subscribe.data.local.entity.SubscriptionTagEntity
import az21.subscribe.data.local.entity.SubscriptionWithReminders
import az21.subscribe.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SubscriptionDao {
  @Transaction
  @Query("SELECT * FROM subscriptions ORDER BY name COLLATE NOCASE ASC")
  fun observeAll(): Flow<List<SubscriptionWithReminders>>

  @Transaction
  @Query(
    "SELECT * FROM subscriptions WHERE status = :status ORDER BY name COLLATE NOCASE ASC",
  )
  fun observeByStatus(status: SubscriptionStatus): Flow<List<SubscriptionWithReminders>>

  @Transaction
  @Query("SELECT * FROM subscriptions WHERE id = :id")
  fun observeById(id: UUID): Flow<SubscriptionWithReminders?>

  @Transaction
  @Query("SELECT * FROM subscriptions WHERE id = :id")
  suspend fun getById(id: UUID): SubscriptionWithReminders?

  @Transaction
  @Query("SELECT * FROM subscriptions")
  suspend fun getAll(): List<SubscriptionWithReminders>

  @Upsert
  suspend fun upsert(entity: SubscriptionEntity)

  @Upsert
  suspend fun upsertAll(items: List<SubscriptionEntity>)

  @Query("DELETE FROM subscriptions WHERE id = :id")
  suspend fun deleteById(id: UUID)

  @Query("SELECT * FROM subscription_reminders")
  suspend fun getAllReminders(): List<SubscriptionReminderEntity>

  @Query("DELETE FROM subscription_reminders WHERE subscription_id = :subscriptionId")
  suspend fun clearReminders(subscriptionId: UUID)

  @Query("DELETE FROM subscription_reminders WHERE subscription_id IN (:subscriptionIds)")
  suspend fun clearRemindersFor(subscriptionIds: List<UUID>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReminders(rows: List<SubscriptionReminderEntity>)

  @Transaction
  suspend fun setReminders(
    subscriptionId: UUID,
    reminders: List<SubscriptionReminderEntity>,
  ) {
    clearReminders(subscriptionId)
    insertReminders(reminders)
  }

  @Transaction
  suspend fun upsertWithReminders(
    entity: SubscriptionEntity,
    reminders: List<SubscriptionReminderEntity>,
  ) {
    upsert(entity)
    setReminders(entity.id, reminders)
  }

  @Transaction
  suspend fun upsertAllWithReminders(items: List<SubscriptionWithReminders>) {
    upsertAll(items.map { it.subscription })
    val ids = items.map { it.subscription.id }
    if (ids.isNotEmpty()) clearRemindersFor(ids)
    insertReminders(items.flatMap { it.reminders })
  }

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

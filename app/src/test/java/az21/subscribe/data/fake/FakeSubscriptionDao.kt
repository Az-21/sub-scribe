package az21.subscribe.data.fake

import az21.subscribe.data.local.dao.SubscriptionDao
import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.data.local.entity.SubscriptionReminderEntity
import az21.subscribe.data.local.entity.SubscriptionTagEntity
import az21.subscribe.data.local.entity.SubscriptionWithReminders
import az21.subscribe.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** In-memory [SubscriptionDao] for JVM-only repository tests. */
class FakeSubscriptionDao : SubscriptionDao {
  private val entities = MutableStateFlow<Map<UUID, SubscriptionEntity>>(emptyMap())
  private val reminders = MutableStateFlow<Map<UUID, List<SubscriptionReminderEntity>>>(emptyMap())
  private val tagLinks = MutableStateFlow<Map<UUID, Set<UUID>>>(emptyMap())

  override fun observeAll(): Flow<List<SubscriptionWithReminders>> =
    entities.map { map ->
      map.values
        .sortedBy { entity -> entity.name.lowercase() }
        .map { entity -> entity.withReminders() }
    }

  override fun observeByStatus(status: SubscriptionStatus): Flow<List<SubscriptionWithReminders>> =
    entities.map { map ->
      map.values
        .filter { it.status == status }
        .sortedBy { entity -> entity.name.lowercase() }
        .map { entity -> entity.withReminders() }
    }

  override fun observeById(id: UUID): Flow<SubscriptionWithReminders?> =
    entities.map { map -> map[id]?.withReminders() }

  override suspend fun getById(id: UUID): SubscriptionWithReminders? = entities.value[id]?.withReminders()

  override suspend fun getAll(): List<SubscriptionWithReminders> = entities.value.values.map { it.withReminders() }

  override suspend fun upsert(entity: SubscriptionEntity) {
    entities.value = entities.value + (entity.id to entity)
  }

  override suspend fun upsertAll(items: List<SubscriptionEntity>) {
    entities.value = entities.value + items.associateBy { entity -> entity.id }
  }

  override suspend fun deleteById(id: UUID) {
    entities.value = entities.value - id
    reminders.value = reminders.value - id
    tagLinks.value = tagLinks.value - id
  }

  override suspend fun getAllReminders(): List<SubscriptionReminderEntity> = reminders.value.values.flatten()

  override suspend fun clearReminders(subscriptionId: UUID) {
    reminders.value = reminders.value - subscriptionId
  }

  override suspend fun clearRemindersFor(subscriptionIds: List<UUID>) {
    reminders.value = reminders.value - subscriptionIds.toSet()
  }

  override suspend fun insertReminders(rows: List<SubscriptionReminderEntity>) {
    val current = reminders.value.toMutableMap()
    rows.groupBy { it.subscriptionId }.forEach { (subscriptionId, group) ->
      current[subscriptionId] = (current[subscriptionId] ?: emptyList()) + group
    }
    reminders.value = current
  }

  override fun observeTagIds(subscriptionId: UUID): Flow<List<UUID>> =
    tagLinks.map { (it[subscriptionId] ?: emptySet()).toList() }

  override suspend fun getAllTagAssignments(): List<SubscriptionTagEntity> =
    tagLinks.value.flatMap { (subscriptionId, tagIds) ->
      tagIds.map { tagId -> SubscriptionTagEntity(subscriptionId = subscriptionId, tagId = tagId) }
    }

  override suspend fun clearTags(subscriptionId: UUID) {
    tagLinks.value = tagLinks.value - subscriptionId
  }

  override suspend fun clearTagsFor(subscriptionIds: List<UUID>) {
    tagLinks.value = tagLinks.value - subscriptionIds.toSet()
  }

  override suspend fun insertTags(rows: List<SubscriptionTagEntity>) {
    val links = tagLinks.value.toMutableMap()
    rows.forEach { row ->
      links[row.subscriptionId] = (links[row.subscriptionId] ?: emptySet()) + row.tagId
    }
    tagLinks.value = links
  }

  private fun SubscriptionEntity.withReminders(): SubscriptionWithReminders =
    SubscriptionWithReminders(subscription = this, reminders = reminders.value[this.id].orEmpty())
}

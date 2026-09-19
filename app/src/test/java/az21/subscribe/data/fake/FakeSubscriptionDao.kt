package az21.subscribe.data.fake

import az21.subscribe.data.local.dao.SubscriptionDao
import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.data.local.entity.SubscriptionTagEntity
import az21.subscribe.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** In-memory [SubscriptionDao] for JVM-only repository tests. */
class FakeSubscriptionDao : SubscriptionDao {
  private val entities = MutableStateFlow<Map<UUID, SubscriptionEntity>>(emptyMap())
  private val tagLinks = MutableStateFlow<Map<UUID, Set<UUID>>>(emptyMap())

  override fun observeAll(): Flow<List<SubscriptionEntity>> =
    entities.map { it.values.sortedBy { entity -> entity.name.lowercase() } }

  override fun observeByStatus(status: SubscriptionStatus): Flow<List<SubscriptionEntity>> =
    entities.map { map ->
      map.values.filter { it.status == status }.sortedBy { entity -> entity.name.lowercase() }
    }

  override fun observeById(id: UUID): Flow<SubscriptionEntity?> = entities.map { it[id] }

  override suspend fun getById(id: UUID): SubscriptionEntity? = entities.value[id]

  override suspend fun upsert(entity: SubscriptionEntity) {
    entities.value = entities.value + (entity.id to entity)
  }

  override suspend fun deleteById(id: UUID) {
    entities.value = entities.value - id
    tagLinks.value = tagLinks.value - id
  }

  override fun observeTagIds(subscriptionId: UUID): Flow<List<UUID>> =
    tagLinks.map { (it[subscriptionId] ?: emptySet()).toList() }

  override suspend fun clearTags(subscriptionId: UUID) {
    tagLinks.value = tagLinks.value - subscriptionId
  }

  override suspend fun insertTags(rows: List<SubscriptionTagEntity>) {
    val links = tagLinks.value.toMutableMap()
    rows.forEach { row ->
      links[row.subscriptionId] = (links[row.subscriptionId] ?: emptySet()) + row.tagId
    }
    tagLinks.value = links
  }
}

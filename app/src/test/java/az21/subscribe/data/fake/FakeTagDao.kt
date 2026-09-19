package az21.subscribe.data.fake

import az21.subscribe.data.local.dao.TagDao
import az21.subscribe.data.local.entity.SubscriptionTagRow
import az21.subscribe.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

/** In-memory [TagDao] for JVM-only repository and ViewModel tests. */
class FakeTagDao : TagDao {
  private val entities = MutableStateFlow<Map<UUID, TagEntity>>(emptyMap())

  /** subscriptionId -> set of tagIds. */
  val assignments = MutableStateFlow<Map<UUID, Set<UUID>>>(emptyMap())

  override fun observeAll(): Flow<List<TagEntity>> =
    entities.map { it.values.sortedBy { entity -> entity.name.lowercase() } }

  override fun observeAssignments(): Flow<List<SubscriptionTagRow>> =
    combine(entities, assignments) { tags, links ->
      links
        .flatMap { (subscriptionId, tagIds) ->
          tagIds.mapNotNull { tagId ->
            tags[tagId]?.let { tag ->
              SubscriptionTagRow(
                subscriptionId = subscriptionId,
                tagId = tag.id,
                name = tag.name,
                color = tag.color,
              )
            }
          }
        }.sortedBy { row -> row.name.lowercase() }
    }

  override fun observeForSubscription(subscriptionId: UUID): Flow<List<TagEntity>> =
    combine(entities, assignments) { tags, links ->
      (links[subscriptionId] ?: emptySet()).mapNotNull { tags[it] }.sortedBy { it.name.lowercase() }
    }

  override suspend fun getById(id: UUID): TagEntity? = entities.value[id]

  override suspend fun upsert(entity: TagEntity) {
    entities.value = entities.value + (entity.id to entity)
  }

  override suspend fun deleteById(id: UUID) {
    entities.value = entities.value - id
    assignments.value = assignments.value.mapValues { (_, tagIds) -> tagIds - id }
  }
}

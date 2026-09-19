package az21.subscribe.data.fake

import az21.subscribe.data.local.dao.PriceHistoryDao
import az21.subscribe.data.local.entity.PriceHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/** In-memory [PriceHistoryDao] for JVM-only repository tests. */
class FakePriceHistoryDao : PriceHistoryDao {
  private val entities = MutableStateFlow<Map<UUID, PriceHistoryEntity>>(emptyMap())

  override fun observeForSubscription(subscriptionId: UUID): Flow<List<PriceHistoryEntity>> =
    entities.map { timeline(it.values, subscriptionId) }

  override fun observeAll(): Flow<List<PriceHistoryEntity>> = entities.map { it.values.toList() }

  override suspend fun getAll(): List<PriceHistoryEntity> = entities.value.values.toList()

  override suspend fun getForSubscription(subscriptionId: UUID): List<PriceHistoryEntity> =
    timeline(entities.value.values, subscriptionId)

  override suspend fun getEffectiveOn(
    subscriptionId: UUID,
    date: LocalDate,
  ): PriceHistoryEntity? =
    timeline(entities.value.values, subscriptionId).firstOrNull { !it.effectiveFromDate.isAfter(date) }

  override suspend fun upsert(entity: PriceHistoryEntity) {
    entities.value = entities.value + (entity.id to entity)
  }

  override suspend fun upsertAll(items: List<PriceHistoryEntity>) {
    entities.value = entities.value + items.associateBy { entity -> entity.id }
  }

  override suspend fun deleteById(id: UUID) {
    entities.value = entities.value - id
  }

  override suspend fun deleteForSubscription(subscriptionId: UUID) {
    entities.value = entities.value.filterValues { it.subscriptionId != subscriptionId }
  }

  private fun timeline(
    all: Collection<PriceHistoryEntity>,
    subscriptionId: UUID,
  ): List<PriceHistoryEntity> =
    all
      .filter { it.subscriptionId == subscriptionId }
      .sortedWith(
        compareByDescending<PriceHistoryEntity> { it.effectiveFromDate }
          .thenByDescending { it.createdAt },
      )
}

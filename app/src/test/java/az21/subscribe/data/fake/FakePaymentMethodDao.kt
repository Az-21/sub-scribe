package az21.subscribe.data.fake

import az21.subscribe.data.local.dao.PaymentMethodDao
import az21.subscribe.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** In-memory [PaymentMethodDao] for JVM-only repository and ViewModel tests. */
class FakePaymentMethodDao : PaymentMethodDao {
  private val entities = MutableStateFlow<Map<UUID, PaymentMethodEntity>>(emptyMap())

  override fun observeAll(): Flow<List<PaymentMethodEntity>> =
    entities.map { it.values.sortedBy { entity -> entity.label.lowercase() } }

  override suspend fun getById(id: UUID): PaymentMethodEntity? = entities.value[id]

  override suspend fun upsert(entity: PaymentMethodEntity) {
    entities.value = entities.value + (entity.id to entity)
  }

  override suspend fun deleteById(id: UUID) {
    entities.value = entities.value - id
  }
}

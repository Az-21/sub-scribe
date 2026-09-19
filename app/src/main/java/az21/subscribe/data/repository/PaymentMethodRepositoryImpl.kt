package az21.subscribe.data.repository

import az21.subscribe.data.local.dao.PaymentMethodDao
import az21.subscribe.data.local.entity.PaymentMethodEntity
import az21.subscribe.data.mapper.toDomain
import az21.subscribe.data.mapper.toEntity
import az21.subscribe.domain.model.PaymentMethod
import az21.subscribe.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentMethodRepositoryImpl
  @Inject
  constructor(
    private val paymentMethodDao: PaymentMethodDao,
  ) : PaymentMethodRepository {
    override fun observePaymentMethods(): Flow<List<PaymentMethod>> =
      paymentMethodDao.observeAll().map { entities -> entities.map(PaymentMethodEntity::toDomain) }

    override suspend fun getPaymentMethod(id: UUID): PaymentMethod? = paymentMethodDao.getById(id)?.toDomain()

    override suspend fun createPaymentMethod(label: String): PaymentMethod {
      val entity = PaymentMethodEntity(id = UUID.randomUUID(), label = label)
      paymentMethodDao.upsert(entity)
      return entity.toDomain()
    }

    override suspend fun updatePaymentMethod(paymentMethod: PaymentMethod) {
      paymentMethodDao.upsert(paymentMethod.toEntity())
    }

    override suspend fun deletePaymentMethod(id: UUID) {
      paymentMethodDao.deleteById(id)
    }
  }

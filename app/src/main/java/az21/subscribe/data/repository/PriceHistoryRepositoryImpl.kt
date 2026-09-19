package az21.subscribe.data.repository

import az21.subscribe.data.local.dao.PriceHistoryDao
import az21.subscribe.data.local.entity.PriceHistoryEntity
import az21.subscribe.data.mapper.toDomain
import az21.subscribe.domain.model.PriceHistory
import az21.subscribe.domain.repository.PriceHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceHistoryRepositoryImpl
  @Inject
  constructor(
    private val priceHistoryDao: PriceHistoryDao,
    private val clock: Clock,
  ) : PriceHistoryRepository {
    override fun observeTimeline(subscriptionId: UUID): Flow<List<PriceHistory>> =
      priceHistoryDao.observeForSubscription(subscriptionId).map { entries ->
        entries.map(PriceHistoryEntity::toDomain)
      }

    override suspend fun getTimeline(subscriptionId: UUID): List<PriceHistory> =
      priceHistoryDao.getForSubscription(subscriptionId).map(PriceHistoryEntity::toDomain)

    override suspend fun getPriceEffectiveOn(
      subscriptionId: UUID,
      date: LocalDate,
    ): PriceHistory? = priceHistoryDao.getEffectiveOn(subscriptionId, date)?.toDomain()

    override suspend fun addPriceChange(
      subscriptionId: UUID,
      price: BigDecimal,
      effectiveFromDate: LocalDate,
    ): PriceHistory {
      val entity =
        PriceHistoryEntity(
          id = UUID.randomUUID(),
          subscriptionId = subscriptionId,
          price = price,
          effectiveFromDate = effectiveFromDate,
          createdAt = clock.instant(),
        )
      priceHistoryDao.upsert(entity)
      return entity.toDomain()
    }

    override suspend fun deleteEntry(id: UUID) {
      priceHistoryDao.deleteById(id)
    }
  }

package az21.subscribe.domain.repository

import az21.subscribe.domain.model.PriceHistory
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/** Reads and writes the price timeline of subscriptions. */
interface PriceHistoryRepository {
  /** The full timeline for a subscription, newest effective date first. */
  fun observeTimeline(subscriptionId: UUID): Flow<List<PriceHistory>>

  /** Every subscription's timeline, keyed by subscription id, for list screens. */
  fun observeAllTimelines(): Flow<Map<UUID, List<PriceHistory>>>

  suspend fun getTimeline(subscriptionId: UUID): List<PriceHistory>

  /** The price in force on [date], or null when no entry is effective yet. */
  suspend fun getPriceEffectiveOn(
    subscriptionId: UUID,
    date: LocalDate,
  ): PriceHistory?

  /** Adds a price entry, which may be backdated or future-dated. */
  suspend fun addPriceChange(
    subscriptionId: UUID,
    price: BigDecimal,
    effectiveFromDate: LocalDate,
  ): PriceHistory

  /** Updates an existing entry's price and effective date, preserving its creation time. */
  suspend fun updateEntry(
    id: UUID,
    price: BigDecimal,
    effectiveFromDate: LocalDate,
  )

  suspend fun deleteEntry(id: UUID)
}

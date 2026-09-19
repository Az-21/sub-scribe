package az21.subscribe.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * A single price point for a subscription.
 *
 * [effectiveFromDate] may be in the past (a backdated correction) or in the future (a scheduled
 * change). The price in force on any date is the entry with the latest [effectiveFromDate] that is
 * not after that date.
 */
data class PriceHistory(
  val id: UUID,
  val subscriptionId: UUID,
  val price: BigDecimal,
  val effectiveFromDate: LocalDate,
  val createdAt: Instant,
)

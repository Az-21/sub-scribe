package az21.subscribe.ui.subscription

import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.PriceHistory
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.Tag
import java.math.BigDecimal
import java.time.LocalDate

/** A price point with its change relative to the previous (older) price, when there is one. */
data class PriceHistoryItem(
  val entry: PriceHistory,
  val delta: BigDecimal?,
)

/**
 * Immutable state for the subscription detail screen, including the price timeline and the derived
 * next billing date and current price.
 */
data class SubscriptionDetailUiState(
  val isLoading: Boolean = true,
  val subscription: Subscription? = null,
  val currentPrice: BigDecimal? = null,
  val nextBillingDate: LocalDate? = null,
  val timeline: List<PriceHistoryItem> = emptyList(),
  val tags: List<Tag> = emptyList(),
  val paymentMethodLabel: String? = null,
  val currency: Currency = Currency.DEFAULT,
)

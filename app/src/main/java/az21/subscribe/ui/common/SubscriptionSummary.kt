package az21.subscribe.ui.common

import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.Tag
import java.math.BigDecimal
import java.time.LocalDate

/**
 * A subscription enriched with the values list screens show: the price in force today, the next
 * charge date, and attached tags.
 */
data class SubscriptionSummary(
  val subscription: Subscription,
  val price: BigDecimal?,
  val nextBillingDate: LocalDate?,
  val tags: List<Tag>,
)

package az21.subscribe.domain.billing

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import java.time.LocalDate

/**
 * Pure helpers for working out when a subscription is actually charged.
 *
 * The first charge lands at [Subscription.startDate]. Cancelled and archived subscriptions have no
 * next billing date.
 */
object BillingSchedule {
  /** The date of the first real charge. */
  fun billingStartDate(subscription: Subscription): LocalDate = subscription.startDate

  /**
   * The earliest charge date on or after [from], or null when the subscription is no longer active.
   */
  fun nextBillingDate(
    subscription: Subscription,
    from: LocalDate,
  ): LocalDate? {
    if (subscription.status != SubscriptionStatus.ACTIVE) return null
    return firstOnOrAfter(subscription, from)
  }

  /** All charge dates in the inclusive range [from]..[toInclusive]. */
  fun billingDatesBetween(
    subscription: Subscription,
    from: LocalDate,
    toInclusive: LocalDate,
  ): List<LocalDate> {
    if (toInclusive.isBefore(from)) return emptyList()

    val dates = mutableListOf<LocalDate>()
    var date = firstOnOrAfter(subscription, from)
    while (!date.isAfter(toInclusive)) {
      dates += date
      date = advance(date, subscription.billingCycle)
    }
    return dates
  }

  /** The date one billing cycle after [date]. */
  fun advance(
    date: LocalDate,
    cycle: BillingCycle,
  ): LocalDate =
    when (cycle) {
      BillingCycle.MONTHLY -> date.plusMonths(1)
      BillingCycle.ANNUAL -> date.plusYears(1)
    }

  private fun firstOnOrAfter(
    subscription: Subscription,
    from: LocalDate,
  ): LocalDate {
    var date = billingStartDate(subscription)
    while (date.isBefore(from)) {
      date = advance(date, subscription.billingCycle)
    }
    return date
  }
}

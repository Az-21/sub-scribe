package az21.subscribe.domain.metrics

import az21.subscribe.domain.billing.BillingSchedule
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.PriceHistory
import az21.subscribe.domain.model.Subscription
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.util.UUID

/**
 * Pure spend-metric arithmetic, kept free of Android and database concerns so it is trivial to test.
 *
 * Cancelled and archived subscriptions are included: a cancelled subscription still counts charges
 * that landed on or before its end date. Future charges are never assumed once a subscription has
 * ended. Deleted subscriptions are absent from the data and therefore never counted.
 */
object SpendCalculator {
  private const val MONTHS_PER_YEAR = 12
  private val MONTH_DIVISOR = BigDecimal(MONTHS_PER_YEAR)

  /** The price in force on [date]: the latest entry effective on or before it. */
  fun resolvePrice(
    timeline: List<PriceHistory>,
    date: LocalDate,
  ): BigDecimal? =
    timeline
      .filter { !it.effectiveFromDate.isAfter(date) }
      .maxWithOrNull(compareBy({ it.effectiveFromDate }, { it.createdAt }))
      ?.price

  /** Computes all three metrics for [year] across [subscriptions] and their price [timelines]. */
  fun yearly(
    subscriptions: List<Subscription>,
    timelines: Map<UUID, List<PriceHistory>>,
    year: Int,
  ): YearlySpend {
    val yearStart = LocalDate.of(year, 1, 1)
    val yearEnd = LocalDate.of(year, 12, 31)
    val trueByMonth = Array(MONTHS_PER_YEAR) { BigDecimal.ZERO }
    val divviedByMonth = Array(MONTHS_PER_YEAR) { BigDecimal.ZERO }

    subscriptions.forEach { subscription ->
      val timeline = timelines[subscription.id].orEmpty()
      accrueCharges(subscription, timeline, yearStart, yearEnd, trueByMonth)
      if (subscription.billingCycle == BillingCycle.ANNUAL) {
        accrueDivviedAnnual(subscription, timeline, yearStart, yearEnd, year, divviedByMonth)
      }
    }

    val monthly =
      (0 until MONTHS_PER_YEAR).map { index ->
        MonthlySpend(
          year = year,
          month = Month.of(index + 1),
          trueMonthlySpend = trueByMonth[index],
          divviedAnnualSpend = divviedByMonth[index],
        )
      }

    return YearlySpend(
      year = year,
      trueAnnualSpend = trueByMonth.fold(BigDecimal.ZERO) { total, value -> total + value },
      divviedAnnualSpend = divviedByMonth.fold(BigDecimal.ZERO) { total, value -> total + value },
      monthly = monthly,
    )
  }

  private fun accrueCharges(
    subscription: Subscription,
    timeline: List<PriceHistory>,
    yearStart: LocalDate,
    yearEnd: LocalDate,
    buckets: Array<BigDecimal>,
  ) {
    val chargeEnd = subscription.endDate?.let { minOf(it, yearEnd) } ?: yearEnd
    if (chargeEnd.isBefore(yearStart)) return

    BillingSchedule.billingDatesBetween(subscription, yearStart, chargeEnd).forEach { date ->
      val price = resolvePrice(timeline, date) ?: return@forEach
      val index = date.monthValue - 1
      buckets[index] = buckets[index] + price
    }
  }

  private fun accrueDivviedAnnual(
    subscription: Subscription,
    timeline: List<PriceHistory>,
    yearStart: LocalDate,
    yearEnd: LocalDate,
    year: Int,
    buckets: Array<BigDecimal>,
  ) {
    val renewals = BillingSchedule.billingDatesBetween(subscription, yearStart.minusYears(1), yearEnd)
    renewals.forEach { renewal ->
      val price = resolvePrice(timeline, renewal) ?: return@forEach
      val perMonth = price.divide(MONTH_DIVISOR, 2, RoundingMode.HALF_UP)
      repeat(MONTHS_PER_YEAR) { offset ->
        val month = renewal.plusMonths(offset.toLong())
        if (month.year == year) {
          val index = month.monthValue - 1
          buckets[index] = buckets[index] + perMonth
        }
      }
    }
  }
}

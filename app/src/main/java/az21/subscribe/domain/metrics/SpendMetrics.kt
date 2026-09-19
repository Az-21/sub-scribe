package az21.subscribe.domain.metrics

import java.math.BigDecimal
import java.time.Month

/** Spend for a single calendar month. */
data class MonthlySpend(
  val year: Int,
  val month: Month,
  /** Actual charges that landed in this month; an annual subscription only counts on its renewal. */
  val trueMonthlySpend: BigDecimal,
  /** Annual subscriptions' cost spread evenly across the twelve months of their billing year. */
  val divviedAnnualSpend: BigDecimal,
)

/**
 * Spend across a calendar year, broken down by month.
 *
 * [trueAnnualSpend] is the "what did I actually pay this year" figure: the sum of every real charge,
 * not amortized. [divviedAnnualSpend] is the sum of the per-month annual contributions.
 */
data class YearlySpend(
  val year: Int,
  val trueAnnualSpend: BigDecimal,
  val divviedAnnualSpend: BigDecimal,
  val monthly: List<MonthlySpend>,
)

package az21.subscribe.ui.metrics

import az21.subscribe.domain.metrics.MonthlySpend
import az21.subscribe.domain.model.Currency
import java.math.BigDecimal
import java.time.Month

/**
 * Immutable state rendered by the metrics screen.
 *
 * [totalSpend] is the "what did I actually pay this year" figure. [divviedAnnualSpend] spreads each
 * annual charge across its billing year, and [monthly] carries the true-monthly charges per month.
 */
data class MetricsUiState(
  val year: Int,
  val selectedMonth: Month,
  val isLoading: Boolean = true,
  val monthly: List<MonthlySpend> = emptyList(),
  val totalSpend: BigDecimal = BigDecimal.ZERO,
  val divviedAnnualSpend: BigDecimal = BigDecimal.ZERO,
  val currency: Currency = Currency.DEFAULT,
) {
  /** Spend for [selectedMonth], or null before the first computation completes. */
  val selectedMonthSpend: MonthlySpend?
    get() = monthly.firstOrNull { it.month == selectedMonth }
}

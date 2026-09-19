package az21.subscribe.ui.common

import az21.subscribe.domain.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private const val DECIMAL_PLACES = 2

/**
 * Renders a stored numeric amount with the app-wide display currency, using [locale] for digit
 * grouping and the decimal separator. Currency is cosmetic: amounts are stored as plain numerics.
 */
fun formatMoney(
  amount: BigDecimal,
  currency: Currency,
  locale: Locale,
): String {
  val numberFormat =
    NumberFormat.getNumberInstance(locale).apply {
      minimumFractionDigits = DECIMAL_PLACES
      maximumFractionDigits = DECIMAL_PLACES
      roundingMode = RoundingMode.HALF_UP
    }
  return "${currency.symbol}${numberFormat.format(amount)}"
}

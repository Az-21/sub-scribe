package az21.subscribe.ui.common

import az21.subscribe.domain.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class MoneyFormatterTest {
  @Test
  fun prefixesTheCurrencySymbolAndPadsToTwoDecimals() {
    assertEquals("\$10.00", formatMoney(BigDecimal("10"), Currency.USD, Locale.US))
    assertEquals("€10.50", formatMoney(BigDecimal("10.5"), Currency.EUR, Locale.US))
  }

  @Test
  fun groupsDigitsAndUsesTheLocaleDecimalSeparator() {
    assertEquals("\$1,234.50", formatMoney(BigDecimal("1234.5"), Currency.USD, Locale.US))
    assertEquals("\$1.234,50", formatMoney(BigDecimal("1234.5"), Currency.USD, Locale.GERMANY))
  }

  @Test
  fun roundsHalfUpToTwoDecimals() {
    assertEquals("\$10.00", formatMoney(BigDecimal("9.999"), Currency.USD, Locale.US))
    assertEquals("\$10.01", formatMoney(BigDecimal("10.005"), Currency.USD, Locale.US))
  }
}

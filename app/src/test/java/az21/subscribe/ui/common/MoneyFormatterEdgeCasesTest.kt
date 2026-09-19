package az21.subscribe.ui.common

import az21.subscribe.domain.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

/** Boundary and locale cases for [formatMoney]. */
class MoneyFormatterEdgeCasesTest {
  @Test
  fun zero_isPadded() {
    assertEquals("$0.00", formatMoney(BigDecimal.ZERO, Currency.USD, Locale.US))
  }

  @Test
  fun negativeAmount_keepsTheSignAfterTheSymbol() {
    assertEquals("$-1,234.50", formatMoney(BigDecimal("-1234.5"), Currency.USD, Locale.US))
  }

  @Test
  fun largeAmount_isGrouped() {
    assertEquals("$1,234,567.89", formatMoney(BigDecimal("1234567.891"), Currency.USD, Locale.US))
  }

  @Test
  fun roundsHalfUpOnExactDecimals() {
    assertEquals("$2.35", formatMoney(BigDecimal("2.345"), Currency.USD, Locale.US))
    assertEquals("$2.34", formatMoney(BigDecimal("2.344"), Currency.USD, Locale.US))
  }

  @Test
  fun negativeScaleBigDecimal_isHandled() {
    assertEquals("$1,000.00", formatMoney(BigDecimal("1E+3"), Currency.USD, Locale.US))
  }

  @Test
  fun multiCharacterAndNonAsciiSymbols_arePrefixed() {
    assertEquals("CHF1,234.50", formatMoney(BigDecimal("1234.5"), Currency.CHF, Locale.US))
    assertEquals("¥1,000.00", formatMoney(BigDecimal("1000"), Currency.JPY, Locale.US))
  }
}

package az21.subscribe.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Behavior of [Currency.fromCode], especially its fallback for unknown input. */
class CurrencyTest {
  @Test
  fun fromCode_isCaseInsensitive() {
    assertEquals(Currency.USD, Currency.fromCode("usd"))
    assertEquals(Currency.GBP, Currency.fromCode("Gbp"))
    assertEquals(Currency.INR, Currency.fromCode("INR"))
  }

  @Test
  fun fromCode_unknownBlankOrNull_fallsBackToDefault() {
    assertEquals(Currency.DEFAULT, Currency.fromCode("XYZ"))
    assertEquals(Currency.DEFAULT, Currency.fromCode(""))
    assertEquals(Currency.DEFAULT, Currency.fromCode(null))
  }

  @Test
  fun defaultIsUsd() {
    assertEquals(Currency.USD, Currency.DEFAULT)
  }

  @Test
  fun everyCurrencyHasANonBlankCodeAndSymbol() {
    Currency.entries.forEach { currency ->
      assertEquals(true, currency.code.isNotBlank())
      assertEquals(true, currency.symbol.isNotBlank())
    }
  }
}

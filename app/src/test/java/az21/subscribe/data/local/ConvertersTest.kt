package az21.subscribe.data.local

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class ConvertersTest {
  private val converters = Converters()

  @Test
  fun uuid_roundTrips() {
    val value = UUID.randomUUID()

    assertEquals(value, converters.toUuid(converters.fromUuid(value)))
  }

  @Test
  fun localDate_roundTripsUsingIsoFormat() {
    val value = LocalDate.of(2024, 2, 29)

    assertEquals("2024-02-29", converters.fromLocalDate(value))
    assertEquals(value, converters.toLocalDate(converters.fromLocalDate(value)))
  }

  @Test
  fun instant_roundTripsAsEpochMillis() {
    val value = Instant.parse("2024-01-01T12:34:56.789Z")

    assertEquals(value, converters.toInstant(converters.fromInstant(value)))
  }

  @Test
  fun bigDecimal_roundTripsWithoutScaleLoss() {
    val value = BigDecimal("12.990")

    assertEquals("12.990", converters.fromBigDecimal(value))
    assertEquals(value, converters.toBigDecimal(converters.fromBigDecimal(value)))
  }

  @Test
  fun enums_roundTripByName() {
    assertEquals(
      BillingCycle.ANNUAL,
      converters.toBillingCycle(converters.fromBillingCycle(BillingCycle.ANNUAL)),
    )
    assertEquals(
      SubscriptionStatus.ARCHIVED,
      converters.toSubscriptionStatus(converters.fromSubscriptionStatus(SubscriptionStatus.ARCHIVED)),
    )
  }

  @Test
  fun nullValues_convertToNull() {
    assertNull(converters.fromUuid(null))
    assertNull(converters.toLocalDate(null))
    assertNull(converters.toBigDecimal(null))
    assertNull(converters.toBillingCycle(null))
    assertNull(converters.toSubscriptionStatus(null))
  }
}

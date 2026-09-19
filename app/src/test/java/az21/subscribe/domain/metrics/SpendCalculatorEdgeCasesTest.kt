package az21.subscribe.domain.metrics

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.PriceHistory
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.util.UUID

/** Boundary and negative cases for [SpendCalculator], complementing the happy-path suite. */
class SpendCalculatorEdgeCasesTest {
  @Test
  fun resolvePrice_emptyTimeline_isNull() {
    assertNull(SpendCalculator.resolvePrice(emptyList(), LocalDate.of(2024, 1, 1)))
  }

  @Test
  fun resolvePrice_picksEntryEffectiveExactlyOnDate() {
    val id = UUID.randomUUID()
    val timeline =
      listOf(
        price(id, "9.99", LocalDate.of(2024, 1, 1)),
        price(id, "12.99", LocalDate.of(2024, 2, 1)),
      )

    assertEquals(BigDecimal("12.99"), SpendCalculator.resolvePrice(timeline, LocalDate.of(2024, 2, 1)))
    assertEquals(BigDecimal("9.99"), SpendCalculator.resolvePrice(timeline, LocalDate.of(2024, 1, 31)))
  }

  @Test
  fun resolvePrice_ignoresFutureEntries() {
    val id = UUID.randomUUID()
    val timeline =
      listOf(
        price(id, "9.99", LocalDate.of(2024, 1, 1)),
        price(id, "14.99", LocalDate.of(2024, 12, 1)),
      )

    assertEquals(BigDecimal("9.99"), SpendCalculator.resolvePrice(timeline, LocalDate.of(2024, 6, 1)))
  }

  @Test
  fun resolvePrice_breaksTieOnEffectiveDateByNewestCreation() {
    val id = UUID.randomUUID()
    val date = LocalDate.of(2024, 1, 1)
    val timeline =
      listOf(
        price(id, "9.99", date, createdAt = Instant.parse("2024-01-01T00:00:00Z")),
        price(id, "11.99", date, createdAt = Instant.parse("2024-03-01T00:00:00Z")),
      )

    assertEquals(BigDecimal("11.99"), SpendCalculator.resolvePrice(timeline, LocalDate.of(2024, 6, 1)))
  }

  @Test
  fun yearly_returnsZeroesWhenSubscriptionStartsAfterYear() {
    val subscription = subscription(startDate = LocalDate.of(2025, 1, 15))
    val timeline = mapOf(subscription.id to listOf(price(subscription.id, "10.00", LocalDate.of(2025, 1, 1))))

    val result = SpendCalculator.yearly(listOf(subscription), timeline, 2024)

    assertEquals(BigDecimal.ZERO, result.trueAnnualSpend)
    assertEquals(BigDecimal.ZERO, result.divviedAnnualSpend)
  }

  @Test
  fun yearly_returnsZeroesWhenCancelledBeforeYearStart() {
    val subscription =
      subscription(
        startDate = LocalDate.of(2023, 11, 15),
        status = SubscriptionStatus.CANCELLED,
        endDate = LocalDate.of(2023, 12, 31),
      )
    val timeline = mapOf(subscription.id to listOf(price(subscription.id, "10.00", LocalDate.of(2023, 11, 1))))

    assertEquals(BigDecimal.ZERO, SpendCalculator.yearly(listOf(subscription), timeline, 2024).trueAnnualSpend)
  }

  @Test
  fun yearly_returnsZeroesWhenEndDatePrecedesFirstCharge() {
    val subscription =
      subscription(
        startDate = LocalDate.of(2024, 1, 15),
        status = SubscriptionStatus.CANCELLED,
        endDate = LocalDate.of(2024, 1, 10),
      )
    val timeline = mapOf(subscription.id to listOf(price(subscription.id, "10.00", LocalDate.of(2024, 1, 1))))

    assertEquals(BigDecimal.ZERO, SpendCalculator.yearly(listOf(subscription), timeline, 2024).trueAnnualSpend)
  }

  @Test
  fun yearly_skipsChargesWhosePriceIsUnknown() {
    val subscription = subscription(startDate = LocalDate.of(2024, 1, 15))

    val result = SpendCalculator.yearly(listOf(subscription), emptyMap(), 2024)

    assertEquals(BigDecimal.ZERO, result.trueAnnualSpend)
  }

  @Test
  fun yearly_divviedAnnualRoundsHalfUpPerMonth() {
    val subscription =
      subscription(startDate = LocalDate.of(2024, 1, 1), billingCycle = BillingCycle.ANNUAL)
    val timeline = mapOf(subscription.id to listOf(price(subscription.id, "100.00", LocalDate.of(2024, 1, 1))))

    val result = SpendCalculator.yearly(listOf(subscription), timeline, 2024)

    assertEquals(BigDecimal("8.33"), result.monthly.first().divviedAnnualSpend)
    assertEquals(BigDecimal("99.96"), result.divviedAnnualSpend)
  }

  @Test
  fun yearly_annualRenewalStraddlingYearStartDivviesFullYear() {
    val subscription =
      subscription(startDate = LocalDate.of(2023, 12, 15), billingCycle = BillingCycle.ANNUAL)
    val timeline = mapOf(subscription.id to listOf(price(subscription.id, "120.00", LocalDate.of(2023, 12, 1))))

    val result = SpendCalculator.yearly(listOf(subscription), timeline, 2024)

    assertEquals(BigDecimal("120.00"), result.trueAnnualSpend)
    assertEquals(BigDecimal("120.00"), result.divviedAnnualSpend)
    assertEquals(BigDecimal("10.00"), result.monthly[Month.JANUARY.value - 1].divviedAnnualSpend)
  }

  @Test
  fun yearly_annualUsesPriceEffectiveAtRenewal() {
    val subscription =
      subscription(startDate = LocalDate.of(2024, 1, 1), billingCycle = BillingCycle.ANNUAL)
    val timeline =
      mapOf(
        subscription.id to
          listOf(
            price(subscription.id, "120.00", LocalDate.of(2024, 1, 1)),
            price(subscription.id, "240.00", LocalDate.of(2024, 6, 1)),
          ),
      )

    val result = SpendCalculator.yearly(listOf(subscription), timeline, 2024)

    assertEquals(BigDecimal("120.00"), result.trueAnnualSpend)
    assertEquals(BigDecimal("120.00"), result.divviedAnnualSpend)
  }

  @Test
  fun yearly_sumsMultipleSubscriptions() {
    val first = subscription(startDate = LocalDate.of(2024, 1, 15))
    val second = subscription(startDate = LocalDate.of(2024, 1, 15))
    val timeline =
      mapOf(
        first.id to listOf(price(first.id, "10.00", LocalDate.of(2024, 1, 1))),
        second.id to listOf(price(second.id, "5.00", LocalDate.of(2024, 1, 1))),
      )

    val result = SpendCalculator.yearly(listOf(first, second), timeline, 2024)

    assertEquals(BigDecimal("180.00"), result.trueAnnualSpend)
    assertEquals(BigDecimal("15.00"), result.monthly.first().trueMonthlySpend)
  }

  @Test
  fun yearly_leapDayChargeLandsInFebruary() {
    val subscription = subscription(startDate = LocalDate.of(2024, 2, 29))
    val timeline = mapOf(subscription.id to listOf(price(subscription.id, "5.00", LocalDate.of(2024, 1, 1))))

    val result = SpendCalculator.yearly(listOf(subscription), timeline, 2024)

    assertEquals(BigDecimal("5.00"), result.monthly[Month.FEBRUARY.value - 1].trueMonthlySpend)
    assertEquals(BigDecimal("55.00"), result.trueAnnualSpend)
  }

  private fun subscription(
    startDate: LocalDate,
    billingCycle: BillingCycle = BillingCycle.MONTHLY,
    status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    endDate: LocalDate? = null,
  ): Subscription =
    Subscription(
      id = UUID.randomUUID(),
      name = "Test",
      iconId = "test",
      startDate = startDate,
      billingCycle = billingCycle,
      status = status,
      endDate = endDate,
      paymentMethodId = null,
      notes = null,
      createdAt = Instant.EPOCH,
      updatedAt = Instant.EPOCH,
    )

  private fun price(
    subscriptionId: UUID,
    amount: String,
    effectiveFrom: LocalDate,
    createdAt: Instant = Instant.EPOCH,
  ): PriceHistory =
    PriceHistory(
      id = UUID.randomUUID(),
      subscriptionId = subscriptionId,
      price = BigDecimal(amount),
      effectiveFromDate = effectiveFrom,
      createdAt = createdAt,
    )
}

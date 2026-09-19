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

class SpendCalculatorTest {
  @Test
  fun monthlySubscription_countsOneChargePerMonth() {
    val subscription = subscription(startDate = LocalDate.of(2024, 1, 15))
    val timeline = mapOf(subscription.id to listOf(price(subscription.id, "9.99", LocalDate.of(2024, 1, 1))))

    val result = SpendCalculator.yearly(listOf(subscription), timeline, 2024)

    assertEquals(BigDecimal("119.88"), result.trueAnnualSpend)
    assertEquals(BigDecimal.ZERO, result.divviedAnnualSpend)
    assertEquals(BigDecimal("9.99"), result.monthly.first().trueMonthlySpend)
    assertEquals(BigDecimal("9.99"), result.monthly.last().trueMonthlySpend)
  }

  @Test
  fun annualSubscription_countsRenewalAndDivviesAcrossTheYear() {
    val subscription =
      subscription(startDate = LocalDate.of(2024, 1, 15), billingCycle = BillingCycle.ANNUAL)
    val timeline = mapOf(subscription.id to listOf(price(subscription.id, "120.00", LocalDate.of(2024, 1, 1))))

    val result = SpendCalculator.yearly(listOf(subscription), timeline, 2024)

    assertEquals(BigDecimal("120.00"), result.trueAnnualSpend)
    assertEquals(BigDecimal("120.00"), result.divviedAnnualSpend)
    assertEquals(BigDecimal("120.00"), result.monthly[Month.JANUARY.value - 1].trueMonthlySpend)
    assertEquals(BigDecimal.ZERO, result.monthly[Month.FEBRUARY.value - 1].trueMonthlySpend)
    assertEquals(BigDecimal("10.00"), result.monthly[Month.FEBRUARY.value - 1].divviedAnnualSpend)
  }

  @Test
  fun annualSubscriptionStartedMidYear_divviesOnlyRemainingMonths() {
    val subscription =
      subscription(startDate = LocalDate.of(2024, 3, 10), billingCycle = BillingCycle.ANNUAL)
    val timeline = mapOf(subscription.id to listOf(price(subscription.id, "120.00", LocalDate.of(2024, 3, 10))))

    val result = SpendCalculator.yearly(listOf(subscription), timeline, 2024)

    assertEquals(BigDecimal("120.00"), result.trueAnnualSpend)
    assertEquals(BigDecimal("100.00"), result.divviedAnnualSpend)
    assertEquals(BigDecimal.ZERO, result.monthly[Month.FEBRUARY.value - 1].divviedAnnualSpend)
    assertEquals(BigDecimal("10.00"), result.monthly[Month.MARCH.value - 1].divviedAnnualSpend)
    assertEquals(BigDecimal("10.00"), result.monthly[Month.DECEMBER.value - 1].divviedAnnualSpend)
  }

  @Test
  fun usesPriceEffectiveAtEachChargeDate() {
    val subscription = subscription(startDate = LocalDate.of(2024, 1, 15))
    val timeline =
      mapOf(
        subscription.id to
          listOf(
            price(subscription.id, "9.99", LocalDate.of(2024, 1, 1)),
            price(subscription.id, "14.99", LocalDate.of(2024, 7, 1)),
          ),
      )

    val result = SpendCalculator.yearly(listOf(subscription), timeline, 2024)

    assertEquals(BigDecimal("9.99"), result.monthly[Month.JUNE.value - 1].trueMonthlySpend)
    assertEquals(BigDecimal("14.99"), result.monthly[Month.JULY.value - 1].trueMonthlySpend)
    assertEquals(BigDecimal("149.88"), result.trueAnnualSpend)
  }

  @Test
  fun stopsChargingAfterEndDate() {
    val subscription =
      subscription(
        startDate = LocalDate.of(2024, 1, 15),
        status = SubscriptionStatus.CANCELLED,
        endDate = LocalDate.of(2024, 3, 31),
      )
    val timeline = mapOf(subscription.id to listOf(price(subscription.id, "10.00", LocalDate.of(2024, 1, 1))))

    val result = SpendCalculator.yearly(listOf(subscription), timeline, 2024)

    assertEquals(BigDecimal("30.00"), result.trueAnnualSpend)
    assertEquals(BigDecimal.ZERO, result.monthly[Month.APRIL.value - 1].trueMonthlySpend)
  }

  @Test
  fun includesArchivedSubscriptionHistory() {
    val subscription =
      subscription(
        startDate = LocalDate.of(2023, 11, 15),
        status = SubscriptionStatus.ARCHIVED,
        endDate = LocalDate.of(2024, 1, 31),
      )
    val timeline = mapOf(subscription.id to listOf(price(subscription.id, "10.00", LocalDate.of(2023, 11, 1))))

    val result = SpendCalculator.yearly(listOf(subscription), timeline, 2024)

    assertEquals(BigDecimal("10.00"), result.trueAnnualSpend)
  }

  @Test
  fun freeTrialDelaysFirstCharge() {
    val subscription =
      subscription(startDate = LocalDate.of(2024, 1, 15), freeTrialMonths = 2)
    val timeline = mapOf(subscription.id to listOf(price(subscription.id, "10.00", LocalDate.of(2024, 1, 15))))

    val result = SpendCalculator.yearly(listOf(subscription), timeline, 2024)

    assertEquals(BigDecimal("100.00"), result.trueAnnualSpend)
    assertEquals(BigDecimal.ZERO, result.monthly[Month.JANUARY.value - 1].trueMonthlySpend)
    assertEquals(BigDecimal("10.00"), result.monthly[Month.MARCH.value - 1].trueMonthlySpend)
  }

  @Test
  fun noSubscriptions_returnsZeroes() {
    val result = SpendCalculator.yearly(emptyList(), emptyMap(), 2024)

    assertEquals(BigDecimal.ZERO, result.trueAnnualSpend)
    assertEquals(BigDecimal.ZERO, result.divviedAnnualSpend)
    assertEquals(12, result.monthly.size)
  }

  @Test
  fun resolvePrice_returnsNullBeforeFirstEntry() {
    val id = UUID.randomUUID()
    val timeline = listOf(price(id, "9.99", LocalDate.of(2024, 1, 1)))

    assertNull(SpendCalculator.resolvePrice(timeline, LocalDate.of(2023, 12, 31)))
    assertEquals(BigDecimal("9.99"), SpendCalculator.resolvePrice(timeline, LocalDate.of(2024, 1, 1)))
  }

  private fun subscription(
    startDate: LocalDate,
    billingCycle: BillingCycle = BillingCycle.MONTHLY,
    freeTrialMonths: Int? = null,
    status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    endDate: LocalDate? = null,
  ): Subscription =
    Subscription(
      id = UUID.randomUUID(),
      name = "Test",
      iconId = "test",
      startDate = startDate,
      billingCycle = billingCycle,
      freeTrialMonths = freeTrialMonths,
      status = status,
      endDate = endDate,
      reminderDaysBefore = null,
      trialReminderEnabled = false,
      paymentMethodId = null,
      notes = null,
      createdAt = Instant.EPOCH,
      updatedAt = Instant.EPOCH,
    )

  private fun price(
    subscriptionId: UUID,
    amount: String,
    effectiveFrom: LocalDate,
  ): PriceHistory =
    PriceHistory(
      id = UUID.randomUUID(),
      subscriptionId = subscriptionId,
      price = BigDecimal(amount),
      effectiveFromDate = effectiveFrom,
      createdAt = Instant.EPOCH,
    )
}

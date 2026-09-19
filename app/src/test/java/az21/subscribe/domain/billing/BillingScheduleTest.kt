package az21.subscribe.domain.billing

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class BillingScheduleTest {
  private val start = LocalDate.of(2024, 1, 15)

  @Test
  fun billingStartDate_addsFreeTrialMonths() {
    val subscription = subscription(startDate = start, freeTrialMonths = 3)

    assertEquals(LocalDate.of(2024, 4, 15), BillingSchedule.billingStartDate(subscription))
  }

  @Test
  fun billingStartDate_withoutFreeTrial_isStartDate() {
    val subscription = subscription(startDate = start, freeTrialMonths = null)

    assertEquals(start, BillingSchedule.billingStartDate(subscription))
  }

  @Test
  fun nextBillingDate_monthly_rollsForwardFromBillingStart() {
    val subscription = subscription(startDate = start, billingCycle = BillingCycle.MONTHLY)

    assertEquals(LocalDate.of(2024, 1, 15), BillingSchedule.nextBillingDate(subscription, start))
    assertEquals(
      LocalDate.of(2024, 3, 15),
      BillingSchedule.nextBillingDate(subscription, LocalDate.of(2024, 2, 20)),
    )
  }

  @Test
  fun nextBillingDate_annual_rollsForwardByYear() {
    val subscription = subscription(startDate = start, billingCycle = BillingCycle.ANNUAL)

    assertEquals(
      LocalDate.of(2025, 1, 15),
      BillingSchedule.nextBillingDate(subscription, LocalDate.of(2024, 6, 1)),
    )
  }

  @Test
  fun nextBillingDate_includesFreeTrialOffset() {
    val subscription = subscription(startDate = start, freeTrialMonths = 2, billingCycle = BillingCycle.MONTHLY)

    assertEquals(
      LocalDate.of(2024, 3, 15),
      BillingSchedule.nextBillingDate(subscription, LocalDate.of(2024, 2, 1)),
    )
  }

  @Test
  fun nextBillingDate_isNullWhenNotActive() {
    val cancelled = subscription(status = SubscriptionStatus.CANCELLED)
    val archived = subscription(status = SubscriptionStatus.ARCHIVED)

    assertNull(BillingSchedule.nextBillingDate(cancelled, start))
    assertNull(BillingSchedule.nextBillingDate(archived, start))
  }

  @Test
  fun billingDatesBetween_returnsInclusiveRange() {
    val subscription = subscription(startDate = start, billingCycle = BillingCycle.MONTHLY)

    val dates =
      BillingSchedule.billingDatesBetween(
        subscription,
        LocalDate.of(2024, 2, 1),
        LocalDate.of(2024, 4, 30),
      )

    assertEquals(
      listOf(
        LocalDate.of(2024, 2, 15),
        LocalDate.of(2024, 3, 15),
        LocalDate.of(2024, 4, 15),
      ),
      dates,
    )
  }

  @Test
  fun billingDatesBetween_emptyWhenRangeIsInverted() {
    val subscription = subscription()

    assertEquals(
      emptyList<LocalDate>(),
      BillingSchedule.billingDatesBetween(subscription, start, start.minusDays(1)),
    )
  }

  private fun subscription(
    startDate: LocalDate = start,
    billingCycle: BillingCycle = BillingCycle.MONTHLY,
    freeTrialMonths: Int? = null,
    status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
  ): Subscription =
    Subscription(
      id = UUID.randomUUID(),
      name = "Test",
      iconId = "test",
      startDate = startDate,
      billingCycle = billingCycle,
      freeTrialMonths = freeTrialMonths,
      status = status,
      endDate = null,
      reminderDaysBefore = null,
      trialReminderEnabled = false,
      paymentMethodId = null,
      notes = null,
      createdAt = Instant.EPOCH,
      updatedAt = Instant.EPOCH,
    )
}

package az21.subscribe.domain.billing

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Boundary and negative cases for [BillingSchedule], complementing the happy-path suite. */
class BillingScheduleEdgeCasesTest {
  @Test
  fun billingStartDate_zeroFreeTrialMonths_isStartDate() {
    val subscription = subscription(startDate = LocalDate.of(2024, 1, 15), freeTrialMonths = 0)

    assertEquals(LocalDate.of(2024, 1, 15), BillingSchedule.billingStartDate(subscription))
  }

  @Test
  fun billingStartDate_monthEndClampsToLastValidDay() {
    val subscription = subscription(startDate = LocalDate.of(2024, 1, 31), freeTrialMonths = 1)

    assertEquals(LocalDate.of(2024, 2, 29), BillingSchedule.billingStartDate(subscription))
  }

  @Test
  fun advance_monthEndClampsThenDrifts() {
    assertEquals(LocalDate.of(2024, 2, 29), BillingSchedule.advance(LocalDate.of(2024, 1, 31), BillingCycle.MONTHLY))
    assertEquals(LocalDate.of(2024, 3, 29), BillingSchedule.advance(LocalDate.of(2024, 2, 29), BillingCycle.MONTHLY))
  }

  @Test
  fun advance_annualClampsLeapDay() {
    assertEquals(LocalDate.of(2025, 2, 28), BillingSchedule.advance(LocalDate.of(2024, 2, 29), BillingCycle.ANNUAL))
  }

  @Test
  fun nextBillingDate_returnsBillingStartWhenFromIsBeforeIt() {
    val subscription = subscription(startDate = LocalDate.of(2024, 6, 15), billingCycle = BillingCycle.ANNUAL)

    assertEquals(
      LocalDate.of(2024, 6, 15),
      BillingSchedule.nextBillingDate(subscription, LocalDate.of(2024, 1, 1)),
    )
  }

  @Test
  fun nextBillingDate_monthEndChargeRollsForward() {
    val subscription = subscription(startDate = LocalDate.of(2024, 1, 31))

    assertEquals(
      LocalDate.of(2024, 2, 29),
      BillingSchedule.nextBillingDate(subscription, LocalDate.of(2024, 2, 1)),
    )
  }

  @Test
  fun billingDatesBetween_isEmptyWhenRangeEndsBeforeBillingStart() {
    val subscription = subscription(startDate = LocalDate.of(2024, 6, 15))

    assertTrue(
      BillingSchedule
        .billingDatesBetween(subscription, LocalDate.of(2023, 1, 1), LocalDate.of(2024, 6, 14))
        .isEmpty(),
    )
  }

  @Test
  fun billingDatesBetween_singleDayRangeMatchesCharge() {
    val subscription = subscription(startDate = LocalDate.of(2024, 1, 15))

    assertEquals(
      listOf(LocalDate.of(2024, 1, 15)),
      BillingSchedule.billingDatesBetween(subscription, LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 15)),
    )
  }

  @Test
  fun billingDatesBetween_annualReturnsEachAnniversary() {
    val subscription = subscription(startDate = LocalDate.of(2024, 1, 15), billingCycle = BillingCycle.ANNUAL)

    assertEquals(
      listOf(LocalDate.of(2024, 1, 15), LocalDate.of(2025, 1, 15), LocalDate.of(2026, 1, 15)),
      BillingSchedule.billingDatesBetween(subscription, LocalDate.of(2024, 1, 1), LocalDate.of(2026, 12, 31)),
    )
  }

  @Test
  fun billingDatesBetween_honoursFreeTrialOffset() {
    val subscription = subscription(startDate = LocalDate.of(2024, 1, 15), freeTrialMonths = 2)

    assertEquals(
      listOf(LocalDate.of(2024, 3, 15), LocalDate.of(2024, 4, 15)),
      BillingSchedule.billingDatesBetween(subscription, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 4, 30)),
    )
  }

  @Test
  fun nextBillingDate_inactiveIsNullEvenBeforeBillingStart() {
    val cancelled = subscription(startDate = LocalDate.of(2024, 6, 15), status = SubscriptionStatus.CANCELLED)

    assertNull(BillingSchedule.nextBillingDate(cancelled, LocalDate.of(2024, 1, 1)))
  }

  private fun subscription(
    startDate: LocalDate,
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

package az21.subscribe.domain.reminder

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/** Boundary and negative cases for [ReminderPlanner] and [ReminderPlan.triggerAt]. */
class ReminderPlannerEdgeCasesTest {
  private val zone = ZoneOffset.UTC

  @Test
  fun triggerAt_zeroDaysBefore_isTheDayAtNineLocal() {
    assertEquals(
      Instant.parse("2024-05-20T09:00:00Z"),
      ReminderPlan.triggerAt(LocalDate.of(2024, 5, 20), daysBefore = 0, zone = zone),
    )
  }

  @Test
  fun triggerAt_usesTheProvidedZone() {
    val tokyo = ZoneId.of("Asia/Tokyo")

    assertEquals(
      Instant.parse("2024-05-20T00:00:00Z"),
      ReminderPlan.triggerAt(LocalDate.of(2024, 5, 20), daysBefore = 0, zone = tokyo),
    )
  }

  @Test
  fun billingPlan_skipsTriggerThatIsExactlyNow() {
    val now = Instant.parse("2024-05-29T09:00:00Z")

    val plan = ReminderPlanner.billingPlan(subscription(reminderDaysBefore = 3), now, zone)

    assertEquals(LocalDate.of(2024, 7, 1), plan?.targetDate)
    assertEquals(Instant.parse("2024-06-28T09:00:00Z"), plan?.triggerAt)
  }

  @Test
  fun billingPlan_advancesAnnualCycle() {
    val now = Instant.parse("2024-02-01T09:00:00Z")
    val subscription =
      subscription(
        startDate = LocalDate.of(2024, 1, 1),
        billingCycle = BillingCycle.ANNUAL,
        reminderDaysBefore = 7,
      )

    val plan = ReminderPlanner.billingPlan(subscription, now, zone)

    assertEquals(LocalDate.of(2025, 1, 1), plan?.targetDate)
    assertEquals(Instant.parse("2024-12-25T09:00:00Z"), plan?.triggerAt)
  }

  @Test
  fun trialPlan_skipsTriggerThatIsExactlyNow() {
    val now = Instant.parse("2024-01-29T09:00:00Z")
    val subscription =
      subscription(freeTrialMonths = 1, trialReminderEnabled = true, reminderDaysBefore = 3)

    assertNull(ReminderPlanner.trialPlan(subscription, now, zone))
  }

  @Test
  fun plans_isEmptyForInactiveSubscription() {
    val subscription =
      subscription(
        status = SubscriptionStatus.CANCELLED,
        freeTrialMonths = 1,
        trialReminderEnabled = true,
        reminderDaysBefore = 3,
      )

    assertTrue(ReminderPlanner.plans(subscription, Instant.parse("2024-01-01T00:00:00Z"), zone).isEmpty())
  }

  @Test
  fun trialPlan_isNullWhenReminderDaysUnsetEvenWithTrialEnabled() {
    val subscription =
      subscription(freeTrialMonths = 3, trialReminderEnabled = true, reminderDaysBefore = null)

    assertNull(ReminderPlanner.trialPlan(subscription, Instant.parse("2024-01-01T00:00:00Z"), zone))
  }

  @Test
  fun billingPlan_whenTrialEndAlreadyPassed_doesNotSkipTheNextCharge() {
    val now = Instant.parse("2024-03-15T09:00:00Z")
    val subscription =
      subscription(
        startDate = LocalDate.of(2024, 1, 1),
        freeTrialMonths = 1,
        trialReminderEnabled = true,
        reminderDaysBefore = 5,
      )

    val plan = ReminderPlanner.billingPlan(subscription, now, zone)

    assertEquals(LocalDate.of(2024, 4, 1), plan?.targetDate)
  }

  private fun subscription(
    startDate: LocalDate = LocalDate.of(2024, 1, 1),
    billingCycle: BillingCycle = BillingCycle.MONTHLY,
    freeTrialMonths: Int? = null,
    status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    reminderDaysBefore: Int? = 3,
    trialReminderEnabled: Boolean = false,
  ): Subscription =
    Subscription(
      id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
      name = "Netflix",
      iconId = "netflix",
      startDate = startDate,
      billingCycle = billingCycle,
      freeTrialMonths = freeTrialMonths,
      status = status,
      endDate = null,
      reminderDaysBefore = reminderDaysBefore,
      trialReminderEnabled = trialReminderEnabled,
      paymentMethodId = null,
      notes = null,
      createdAt = Instant.EPOCH,
      updatedAt = Instant.EPOCH,
    )
}

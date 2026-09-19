package az21.subscribe.domain.reminder

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.ReminderSpec
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/** Boundary and negative cases for [ReminderPlanner] and [ReminderPlan.triggerAt]. */
class ReminderPlannerEdgeCasesTest {
  private val zone = ZoneOffset.UTC

  @Test
  fun triggerAt_zeroDaysBefore_usesProvidedTime() {
    assertEquals(
      Instant.parse("2024-05-20T09:00:00Z"),
      ReminderPlan.triggerAt(
        LocalDate.of(2024, 5, 20),
        daysBefore = 0,
        time = LocalTime.of(9, 0),
        zone = zone,
      ),
    )
  }

  @Test
  fun triggerAt_respectsCustomTime() {
    assertEquals(
      Instant.parse("2024-05-20T18:45:00Z"),
      ReminderPlan.triggerAt(
        LocalDate.of(2024, 5, 20),
        daysBefore = 0,
        time = LocalTime.of(18, 45),
        zone = zone,
      ),
    )
  }

  @Test
  fun triggerAt_usesTheProvidedZone() {
    val tokyo = ZoneId.of("Asia/Tokyo")

    assertEquals(
      Instant.parse("2024-05-20T00:00:00Z"),
      ReminderPlan.triggerAt(
        LocalDate.of(2024, 5, 20),
        daysBefore = 0,
        time = LocalTime.of(9, 0),
        zone = tokyo,
      ),
    )
  }

  @Test
  fun billingPlan_skipsTriggerThatIsExactlyNow() {
    val now = Instant.parse("2024-05-29T09:00:00Z")
    val spec = ReminderSpec(daysBefore = 3, time = LocalTime.of(9, 0))

    val plan = ReminderPlanner.billingPlan(subscription(reminders = listOf(spec)), spec, now, zone)

    assertEquals(LocalDate.of(2024, 7, 1), plan?.targetDate)
    assertEquals(Instant.parse("2024-06-28T09:00:00Z"), plan?.triggerAt)
  }

  @Test
  fun billingPlan_advancesAnnualCycle() {
    val now = Instant.parse("2024-02-01T09:00:00Z")
    val spec = ReminderSpec(daysBefore = 7, time = LocalTime.of(9, 0))
    val subscription =
      subscription(
        startDate = LocalDate.of(2024, 1, 1),
        billingCycle = BillingCycle.ANNUAL,
        reminders = listOf(spec),
      )

    val plan = ReminderPlanner.billingPlan(subscription, spec, now, zone)

    assertEquals(LocalDate.of(2025, 1, 1), plan?.targetDate)
    assertEquals(Instant.parse("2024-12-25T09:00:00Z"), plan?.triggerAt)
  }

  @Test
  fun plans_isEmptyForInactiveSubscription() {
    val spec = ReminderSpec(daysBefore = 3, time = LocalTime.of(9, 0))
    val subscription =
      subscription(
        status = SubscriptionStatus.CANCELLED,
        reminders = listOf(spec),
      )

    assertTrue(ReminderPlanner.plans(subscription, Instant.parse("2024-01-01T00:00:00Z"), zone).isEmpty())
  }

  private fun subscription(
    startDate: LocalDate = LocalDate.of(2024, 1, 1),
    billingCycle: BillingCycle = BillingCycle.MONTHLY,
    status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    reminders: List<ReminderSpec> = listOf(ReminderSpec(3, LocalTime.of(9, 0))),
  ): Subscription =
    Subscription(
      id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
      name = "Netflix",
      iconId = "netflix",
      startDate = startDate,
      billingCycle = billingCycle,
      status = status,
      endDate = null,
      reminders = reminders,
      paymentMethodId = null,
      notes = null,
      createdAt = Instant.EPOCH,
      updatedAt = Instant.EPOCH,
    )
}

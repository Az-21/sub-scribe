package az21.subscribe.domain.reminder

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.ReminderSpec
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID

class ReminderPlannerTest {
  private val zone = ZoneOffset.UTC
  private val now = Instant.parse("2024-05-20T09:00:00Z")
  private val spec = ReminderSpec(daysBefore = 3, time = LocalTime.of(9, 0))

  @Test
  fun billingPlan_targetsNextChargeWhenTriggerIsInFuture() {
    val plan = ReminderPlanner.billingPlan(subscription(), spec, now, zone)

    assertEquals(LocalDate.of(2024, 6, 1), plan?.targetDate)
    assertEquals(Instant.parse("2024-05-29T09:00:00Z"), plan?.triggerAt)
  }

  @Test
  fun billingPlan_advancesPastElapsedReminders() {
    val farSpec = ReminderSpec(daysBefore = 30, time = LocalTime.of(9, 0))
    val plan = ReminderPlanner.billingPlan(subscription(reminders = listOf(farSpec)), farSpec, now, zone)

    assertEquals(LocalDate.of(2024, 7, 1), plan?.targetDate)
    assertEquals(Instant.parse("2024-06-01T09:00:00Z"), plan?.triggerAt)
  }

  @Test
  fun plans_isEmptyWhenNoReminders() {
    assertTrue(ReminderPlanner.plans(subscription(reminders = emptyList()), now, zone).isEmpty())
  }

  @Test
  fun plans_isEmptyWhenSubscriptionInactive() {
    val subscription = subscription(status = SubscriptionStatus.CANCELLED)

    assertTrue(ReminderPlanner.plans(subscription, now, zone).isEmpty())
  }

  @Test
  fun plans_includesOnePlanPerReminder() {
    val second = ReminderSpec(daysBefore = 1, time = LocalTime.of(8, 30))

    val plans = ReminderPlanner.plans(subscription(reminders = listOf(spec, second)), now, zone)

    assertEquals(2, plans.size)
    assertEquals(
      listOf(Instant.parse("2024-05-29T09:00:00Z"), Instant.parse("2024-05-31T08:30:00Z")),
      plans.map { it.triggerAt },
    )
    assertTrue(plans.all { it.targetDate == LocalDate.of(2024, 6, 1) })
  }

  @Test
  fun billingPlan_isNullWhenTriggerWouldNeverBeReached() {
    // A cancelled subscription has no upcoming charge to base a reminder on.
    assertNull(ReminderPlanner.billingPlan(subscription(status = SubscriptionStatus.CANCELLED), spec, now, zone))
  }

  private fun subscription(
    startDate: LocalDate = LocalDate.of(2024, 1, 1),
    billingCycle: BillingCycle = BillingCycle.MONTHLY,
    status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    reminders: List<ReminderSpec> = listOf(spec),
  ) = Subscription(
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

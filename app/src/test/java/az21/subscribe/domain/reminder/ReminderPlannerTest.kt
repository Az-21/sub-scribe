package az21.subscribe.domain.reminder

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class ReminderPlannerTest {
  private val zone = ZoneOffset.UTC
  private val now = Instant.parse("2024-05-20T09:00:00Z")

  @Test
  fun billingPlan_targetsNextChargeWhenTriggerIsInFuture() {
    val plan = ReminderPlanner.billingPlan(subscription(), now, zone)

    assertEquals(ReminderType.BILLING, plan?.type)
    assertEquals(LocalDate.of(2024, 6, 1), plan?.targetDate)
    assertEquals(Instant.parse("2024-05-29T09:00:00Z"), plan?.triggerAt)
  }

  @Test
  fun billingPlan_advancesPastElapsedReminders() {
    val plan = ReminderPlanner.billingPlan(subscription(reminderDaysBefore = 30), now, zone)

    assertEquals(LocalDate.of(2024, 7, 1), plan?.targetDate)
    assertEquals(Instant.parse("2024-06-01T09:00:00Z"), plan?.triggerAt)
  }

  @Test
  fun billingPlan_isNullWhenRemindersDisabled() {
    assertNull(ReminderPlanner.billingPlan(subscription(reminderDaysBefore = null), now, zone))
  }

  @Test
  fun billingPlan_isNullWhenSubscriptionInactive() {
    assertNull(ReminderPlanner.billingPlan(subscription(status = SubscriptionStatus.CANCELLED), now, zone))
  }

  @Test
  fun plans_includesBillingPlan() {
    val types = ReminderPlanner.plans(subscription(), now, zone).map { it.type }

    assertEquals(listOf(ReminderType.BILLING), types)
  }

  private fun subscription(
    startDate: LocalDate = LocalDate.of(2024, 1, 1),
    billingCycle: BillingCycle = BillingCycle.MONTHLY,
    status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    reminderDaysBefore: Int? = 3,
  ) = Subscription(
    id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    name = "Netflix",
    iconId = "netflix",
    startDate = startDate,
    billingCycle = billingCycle,
    status = status,
    endDate = null,
    reminderDaysBefore = reminderDaysBefore,
    paymentMethodId = null,
    notes = null,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH,
  )
}

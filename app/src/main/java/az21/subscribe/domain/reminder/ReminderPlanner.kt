package az21.subscribe.domain.reminder

import az21.subscribe.domain.billing.BillingSchedule
import az21.subscribe.domain.model.ReminderSpec
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import java.time.Instant
import java.time.ZoneId

/**
 * Pure planning logic that turns a subscription and its reminder rules into the reminders that
 * should currently be scheduled.
 *
 * Reminders whose trigger instant has already passed are skipped; each rule advances through the
 * subscription's cycles until a future trigger is found.
 */
object ReminderPlanner {
  /** Guards the billing-cycle sequence; far beyond any realistic subscription lifetime. */
  private const val MAX_CYCLES = 1_200

  fun plans(
    subscription: Subscription,
    now: Instant,
    zone: ZoneId,
  ): List<ReminderPlan> {
    if (subscription.status != SubscriptionStatus.ACTIVE) return emptyList()
    return subscription.reminders
      .sortedWith(compareBy({ it.daysBefore }, { it.time }))
      .mapNotNull { spec -> billingPlan(subscription, spec, now, zone) }
      .distinctBy { it.triggerAt }
      .sortedBy { it.triggerAt }
  }

  fun billingPlan(
    subscription: Subscription,
    spec: ReminderSpec,
    now: Instant,
    zone: ZoneId,
  ): ReminderPlan? =
    billingCandidates(subscription, now, zone, spec).firstOrNull { plan -> plan.triggerAt.isAfter(now) }

  private fun billingCandidates(
    subscription: Subscription,
    now: Instant,
    zone: ZoneId,
    spec: ReminderSpec,
  ): Sequence<ReminderPlan> {
    val today = now.atZone(zone).toLocalDate()
    return BillingSchedule
      .nextBillingDate(subscription, today)
      ?.let { charge -> generateSequence(charge) { date -> BillingSchedule.advance(date, subscription.billingCycle) } }
      ?.take(MAX_CYCLES)
      ?.map { charge ->
        ReminderPlan(
          triggerAt = ReminderPlan.triggerAt(charge, spec.daysBefore, spec.time, zone),
          targetDate = charge,
          daysBefore = spec.daysBefore,
          time = spec.time,
        )
      }.orEmpty()
  }
}

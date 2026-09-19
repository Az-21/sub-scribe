package az21.subscribe.domain.reminder

import az21.subscribe.domain.billing.BillingSchedule
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure planning logic that turns a subscription into the reminders that should currently be
 * scheduled.
 *
 * Reminders whose trigger instant has already passed are skipped; billing reminders advance through
 * the subscription's cycles until a future trigger is found. The trial reminder is a one-off, so it
 * is skipped (not advanced) once its trigger has passed.
 */
object ReminderPlanner {
  /** Guards the billing-cycle sequence; far beyond any realistic subscription lifetime. */
  private const val MAX_CYCLES = 1_200

  fun plans(
    subscription: Subscription,
    now: Instant,
    zone: ZoneId,
  ): List<ReminderPlan> = listOfNotNull(billingPlan(subscription, now, zone), trialPlan(subscription, now, zone))

  fun billingPlan(
    subscription: Subscription,
    now: Instant,
    zone: ZoneId,
  ): ReminderPlan? {
    val daysBefore = subscription.reminderDaysBefore
    if (daysBefore == null || subscription.status != SubscriptionStatus.ACTIVE) return null
    return billingCandidates(subscription, now, zone, daysBefore).firstOrNull { plan -> plan.triggerAt.isAfter(now) }
  }

  fun trialPlan(
    subscription: Subscription,
    now: Instant,
    zone: ZoneId,
  ): ReminderPlan? {
    val daysBefore = subscription.reminderDaysBefore
    val hasTrial =
      subscription.trialReminderEnabled &&
        subscription.status == SubscriptionStatus.ACTIVE &&
        (subscription.freeTrialMonths ?: 0) > 0
    if (daysBefore == null || !hasTrial) return null

    val trialEndDate = BillingSchedule.billingStartDate(subscription)
    return ReminderPlan(
      type = ReminderType.TRIAL_ENDING,
      triggerAt = ReminderPlan.triggerAt(trialEndDate, daysBefore, zone),
      targetDate = trialEndDate,
    ).takeIf { plan -> plan.triggerAt.isAfter(now) }
  }

  private fun billingCandidates(
    subscription: Subscription,
    now: Instant,
    zone: ZoneId,
    daysBefore: Int,
  ): Sequence<ReminderPlan> {
    val today = now.atZone(zone).toLocalDate()
    return firstChargeDate(subscription, today)
      ?.let { charge -> generateSequence(charge) { date -> BillingSchedule.advance(date, subscription.billingCycle) } }
      ?.take(MAX_CYCLES)
      ?.map { charge ->
        ReminderPlan(ReminderType.BILLING, ReminderPlan.triggerAt(charge, daysBefore, zone), charge)
      }.orEmpty()
  }

  private fun firstChargeDate(
    subscription: Subscription,
    today: LocalDate,
  ): LocalDate? {
    val charge = BillingSchedule.nextBillingDate(subscription, today) ?: return null
    return if (subscription.trialReminderEnabled && BillingSchedule.billingStartDate(subscription) == charge) {
      BillingSchedule.advance(charge, subscription.billingCycle)
    } else {
      charge
    }
  }
}

package az21.subscribe.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * A tracked subscription.
 *
 * [startDate] is the date the subscription was taken out. If it included a free trial, the first
 * real charge lands [freeTrialMonths] later; see `billingStartDate` in `domain/billing`.
 */
data class Subscription(
  val id: UUID,
  val name: String,
  val iconId: String,
  val startDate: LocalDate,
  val billingCycle: BillingCycle,
  val freeTrialMonths: Int?,
  val status: SubscriptionStatus,
  val endDate: LocalDate?,
  val reminderDaysBefore: Int?,
  val trialReminderEnabled: Boolean,
  val paymentMethodId: UUID?,
  val notes: String?,
  val createdAt: Instant,
  val updatedAt: Instant,
)

/**
 * Fields supplied when creating a subscription. The repository fills in the generated [id], status,
 * and timestamps, and defaults a missing start date to "today".
 */
data class SubscriptionDraft(
  val name: String,
  val iconId: String,
  val startDate: LocalDate? = null,
  val billingCycle: BillingCycle,
  val freeTrialMonths: Int? = null,
  val reminderDaysBefore: Int? = null,
  val trialReminderEnabled: Boolean = false,
  val paymentMethodId: UUID? = null,
  val notes: String? = null,
)

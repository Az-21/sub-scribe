package az21.subscribe.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * A tracked subscription.
 *
 * [startDate] is the date the subscription was taken out and the date of its first charge.
 */
data class Subscription(
  val id: UUID,
  val name: String,
  val iconId: String,
  val startDate: LocalDate,
  val billingCycle: BillingCycle,
  val status: SubscriptionStatus,
  val endDate: LocalDate?,
  val reminderDaysBefore: Int?,
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
  val reminderDaysBefore: Int? = null,
  val paymentMethodId: UUID? = null,
  val notes: String? = null,
)

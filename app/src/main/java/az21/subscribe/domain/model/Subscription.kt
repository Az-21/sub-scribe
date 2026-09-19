package az21.subscribe.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * A tracked subscription.
 *
 * [startDate] is the date the subscription was taken out and the date of its first charge.
 * [iconColor] is an ARGB color overriding the theme's default icon tint; null keeps the theme color.
 */
data class Subscription(
  val id: UUID,
  val name: String,
  val iconId: String,
  val startDate: LocalDate,
  val billingCycle: BillingCycle,
  val status: SubscriptionStatus,
  val endDate: LocalDate?,
  val reminders: List<ReminderSpec> = emptyList(),
  val paymentMethodId: UUID?,
  val notes: String?,
  val createdAt: Instant,
  val updatedAt: Instant,
  val iconColor: Int? = null,
)

/**
 * Fields supplied when creating a subscription. The repository fills in the generated [id], status,
 * and timestamps, and defaults a missing start date to "today".
 */
data class SubscriptionDraft(
  val name: String,
  val iconId: String,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val billingCycle: BillingCycle,
  val reminders: List<ReminderSpec> = emptyList(),
  val paymentMethodId: UUID? = null,
  val notes: String? = null,
  val iconColor: Int? = null,
)

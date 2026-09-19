package az21.subscribe.ui.subscription

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.PaymentMethod
import az21.subscribe.domain.model.Tag
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** Per-field validation flags rendered as supporting text. */
data class FormErrors(
  val name: Boolean = false,
  val price: Boolean = false,
) {
  val hasErrors: Boolean
    get() = name || price
}

/**
 * One editable reminder rule. [daysBefore] is held as text so the user can clear it; [isError] marks
 * an entry that failed validation on save.
 */
data class ReminderEntry(
  val daysBefore: String = "",
  val time: LocalTime = DEFAULT_TIME,
  val isError: Boolean = false,
) {
  companion object {
    val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)
  }
}

/**
 * One editable price point. [price] is held as text so the user can clear it; [id] is null for an
 * entry the user just added and non-null for one loaded from the existing timeline. [isError] marks
 * an entry that failed validation on save.
 */
data class PriceEntry(
  val id: UUID? = null,
  val price: String = "",
  val effectiveFromDate: LocalDate,
  val isError: Boolean = false,
)

/**
 * Editable state of the add/edit form. Numeric fields are held as text so the user can clear them;
 * they are parsed on save.
 */
data class SubscriptionFormUiState(
  val isEditing: Boolean = false,
  val isLoading: Boolean = false,
  val name: String = "",
  val iconId: String = "",
  val iconColor: Int? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val billingCycle: BillingCycle = BillingCycle.MONTHLY,
  val priceEntries: List<PriceEntry> = emptyList(),
  val reminders: List<ReminderEntry> = emptyList(),
  val selectedTagIds: Set<UUID> = emptySet(),
  val paymentMethodId: UUID? = null,
  val notes: String = "",
  val availableTags: List<Tag> = emptyList(),
  val paymentMethods: List<PaymentMethod> = emptyList(),
  val errors: FormErrors = FormErrors(),
  val saved: Boolean = false,
)

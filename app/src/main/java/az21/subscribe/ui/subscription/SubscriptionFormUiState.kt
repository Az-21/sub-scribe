package az21.subscribe.ui.subscription

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.PaymentMethod
import az21.subscribe.domain.model.Tag
import java.time.LocalDate
import java.util.UUID

/** Per-field validation flags rendered as supporting text. */
data class FormErrors(
  val name: Boolean = false,
  val price: Boolean = false,
  val reminderDays: Boolean = false,
) {
  val hasErrors: Boolean
    get() = name || price || reminderDays
}

/**
 * Editable state of the add/edit form. Numeric fields are held as text so the user can clear them;
 * they are parsed on save.
 */
data class SubscriptionFormUiState(
  val isEditing: Boolean = false,
  val isLoading: Boolean = false,
  val name: String = "",
  val iconId: String = "",
  val startDate: LocalDate? = null,
  val billingCycle: BillingCycle = BillingCycle.MONTHLY,
  val price: String = "",
  val reminderDaysBefore: String = "",
  val selectedTagIds: Set<UUID> = emptySet(),
  val paymentMethodId: UUID? = null,
  val notes: String = "",
  val availableTags: List<Tag> = emptyList(),
  val paymentMethods: List<PaymentMethod> = emptyList(),
  val errors: FormErrors = FormErrors(),
  val saved: Boolean = false,
)

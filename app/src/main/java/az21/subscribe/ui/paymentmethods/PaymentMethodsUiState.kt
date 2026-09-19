package az21.subscribe.ui.paymentmethods

import az21.subscribe.domain.model.PaymentMethod
import java.util.UUID

/** State of the payment method create/edit dialog. [id] is null when creating. */
data class PaymentMethodEditorState(
  val id: UUID? = null,
  val label: String = "",
  val color: Int? = null,
)

/** Immutable state for the payment method management screen. */
data class PaymentMethodsUiState(
  val isLoading: Boolean = true,
  val paymentMethods: List<PaymentMethod> = emptyList(),
  val editor: PaymentMethodEditorState? = null,
  val pendingDelete: PaymentMethod? = null,
)

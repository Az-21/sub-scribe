package az21.subscribe.domain.model

import java.util.UUID

/** Where a subscription is billed, e.g. "Visa ...1234" or "PayPal". */
data class PaymentMethod(
  val id: UUID,
  val label: String,
)

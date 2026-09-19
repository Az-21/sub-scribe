package az21.subscribe.domain.model

import java.util.UUID

/** Raised when a subscription lifecycle transition is not allowed. */
sealed class SubscriptionTransitionException(
  message: String,
) : IllegalStateException(message) {
  class NotFound(
    val id: UUID,
  ) : SubscriptionTransitionException("Subscription $id was not found")

  class CannotArchive(
    val status: SubscriptionStatus,
  ) : SubscriptionTransitionException("Cannot archive a subscription with status $status; cancel it first")

  class CannotDelete(
    val status: SubscriptionStatus,
  ) : SubscriptionTransitionException("Cannot delete a subscription with status $status; archive it first")

  class CannotCancel(
    val status: SubscriptionStatus,
  ) : SubscriptionTransitionException("Cannot cancel a subscription with status $status")
}

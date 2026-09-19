package az21.subscribe.ui.archive

import az21.subscribe.domain.model.Currency
import az21.subscribe.ui.common.SubscriptionSummary

/**
 * Immutable state for the archive screen: cancelled and archived subscriptions, plus the item
 * awaiting delete confirmation.
 */
data class ArchiveUiState(
  val isLoading: Boolean = true,
  val items: List<SubscriptionSummary> = emptyList(),
  val currency: Currency = Currency.DEFAULT,
  val pendingDelete: SubscriptionSummary? = null,
)

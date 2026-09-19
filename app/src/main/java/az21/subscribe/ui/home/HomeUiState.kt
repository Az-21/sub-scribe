package az21.subscribe.ui.home

import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.PaymentMethod
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.model.Tag
import az21.subscribe.ui.common.SubscriptionSummary
import java.util.UUID

/** How the subscription list is ordered. */
enum class SubscriptionSort {
  NAME,
  PRICE,
  NEXT_BILLING,
}

/**
 * Immutable state for the subscription list. [items] is already filtered and sorted; [allTags] and
 * [paymentMethods] power the filter controls.
 */
data class HomeUiState(
  val isLoading: Boolean = true,
  val items: List<SubscriptionSummary> = emptyList(),
  val allTags: List<Tag> = emptyList(),
  val paymentMethods: List<PaymentMethod> = emptyList(),
  val currency: Currency = Currency.DEFAULT,
  val query: String = "",
  val status: SubscriptionStatus? = SubscriptionStatus.ACTIVE,
  val tagId: UUID? = null,
  val paymentMethodId: UUID? = null,
  val sort: SubscriptionSort = SubscriptionSort.NAME,
  val hasSubscriptions: Boolean = false,
) {
  /** True when any filter differs from the default active-only view. */
  val hasActiveFilters: Boolean
    get() = query.isNotBlank() || tagId != null || paymentMethodId != null || status != SubscriptionStatus.ACTIVE
}

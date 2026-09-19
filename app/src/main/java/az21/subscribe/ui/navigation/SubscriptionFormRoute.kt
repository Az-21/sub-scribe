package az21.subscribe.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Add/edit destination. [subscriptionId] is null when adding and the id being edited otherwise.
 */
@Serializable
data class SubscriptionFormRoute(
  val subscriptionId: String? = null,
) : NavKey
